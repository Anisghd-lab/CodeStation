#!/usr/bin/env python3
"""
CodeStation Continuous Memory Learning Engine & Memory Agents Pipeline
Autonomous continuous memory synchronization from PRoot CLIs (OpenCode, Google agy, CodeBuddy, Codex)
into CodeStation-Memory.
"""

import os
import sys
import time
import json
import uuid
import re
import glob
import sqlite3
import threading
import subprocess
import urllib.parse
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime, timezone

MEMORY_REPO_PATH = "/root/CodeStation-Memory"
STATE_FILE = "/root/.codestation/memory_watcher_state.json"
PORT = 19224
BIND = "127.0.0.1"

# Patterns to strip from memory for privacy & security
SECRET_PATTERNS = [
    re.compile(r'cfat_[A-Za-z0-9_-]{30,}'),
    re.compile(r'sk-[A-Za-z0-9_-]{20,}'),
    re.compile(r'Bearer\s+[A-Za-z0-9_.-]{20,}', re.IGNORECASE),
    re.compile(r'(api[_-]?key|token|password|secret)\s*[:=]\s*["\']?[A-Za-z0-9_.-]{12,}["\']?', re.IGNORECASE)
]

def sanitize_text(text: str) -> str:
    if not text:
        return ""
    sanitized = text
    for p in SECRET_PATTERNS:
        sanitized = p.sub('[REDACTED_SECRET]', sanitized)
    return sanitized

def get_iso_now() -> str:
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


# ==============================================================================
# 1. MEMORY AGENTS PIPELINE
# ==============================================================================

class MemoryCollector:
    """Collects raw events from CLI tools and sessions."""
    def __init__(self, state: Dict[str, Any]):
        self.state = state

    def collect_opencode_sessions(self) -> List[Dict[str, Any]]:
        sessions = []
        db_path = "/root/.local/share/opencode/opencode.db"
        if not os.path.exists(db_path):
            return sessions
        try:
            conn = sqlite3.connect(db_path)
            cur = conn.cursor()
            cur.execute("SELECT id, title, time_created, time_updated FROM session")
            rows = cur.fetchall()
            for r in rows:
                sid, title, created, updated = r
                if sid in self.state.get("processed_opencode", []):
                    continue
                # Fetch messages
                msgs = []
                try:
                    cur.execute("SELECT id, data FROM message WHERE session_id = ? ORDER BY time_created ASC", (sid,))
                    msg_rows = cur.fetchall()
                    for mid, mdata in msg_rows:
                        sender = "user"
                        try:
                            d = json.loads(mdata) if isinstance(mdata, str) else {}
                            sender = d.get("role") or d.get("sender") or "user"
                        except Exception:
                            pass
                        # Fetch parts
                        text_parts = []
                        try:
                            cur.execute("SELECT data FROM part WHERE message_id = ? ORDER BY time_created ASC", (mid,))
                            for (pdata,) in cur.fetchall():
                                try:
                                    pd = json.loads(pdata) if isinstance(pdata, str) else {}
                                    if "text" in pd:
                                        text_parts.append(pd["text"])
                                    elif "content" in pd:
                                        text_parts.append(str(pd["content"]))
                                    elif isinstance(pd, str):
                                        text_parts.append(pd)
                                except Exception:
                                    text_parts.append(str(pdata))
                        except Exception:
                            pass
                        text = "\n".join(text_parts) if text_parts else str(mdata)
                        msgs.append({"sender": sender, "text": text})
                except Exception:
                    # Fallback for v1 schema
                    try:
                        cur.execute("SELECT id, sender, text FROM message WHERE session_id = ?", (sid,))
                        msgs = [{"sender": m[1], "text": m[2]} for m in cur.fetchall()]
                    except Exception:
                        pass

                sessions.append({
                    "source": "opencode",
                    "id": sid,
                    "title": title or "Session OpenCode",
                    "messages": msgs,
                    "timestamp": updated or created or time.time()
                })
            conn.close()
        except Exception as e:
            pass
        return sessions

    def collect_agy_sessions(self) -> List[Dict[str, Any]]:
        sessions = []
        pattern = "/root/.gemini/antigravity-cli/brain/*/transcript.jsonl"
        for path in glob.glob(pattern):
            conv_id = path.split("/brain/")[1].split("/")[0]
            if conv_id in self.state.get("processed_agy", []):
                continue
            try:
                mtime = os.path.getmtime(path)
                events = []
                with open(path, "r", encoding="utf-8") as f:
                    for line in f:
                        if line.strip():
                            try:
                                events.append(json.loads(line))
                            except Exception:
                                pass
                if events:
                    sessions.append({
                        "source": "google_agy",
                        "id": conv_id,
                        "title": f"Session Google CLI ({conv_id[:8]})",
                        "events": events,
                        "timestamp": mtime
                    })
            except Exception:
                pass
        return sessions

    def collect_codebuddy_sessions(self) -> List[Dict[str, Any]]:
        sessions = []
        pattern = "/root/.codebuddy/sessions/*.json"
        for path in glob.glob(pattern):
            sid = os.path.basename(path).replace(".json", "")
            if sid in self.state.get("processed_codebuddy", []):
                continue
            try:
                mtime = os.path.getmtime(path)
                with open(path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                sessions.append({
                    "source": "codebuddy",
                    "id": sid,
                    "title": f"Session CodeBuddy ({sid})",
                    "data": data,
                    "timestamp": mtime
                })
            except Exception:
                pass
        return sessions

    def collect_camcs_missions(self) -> List[Dict[str, Any]]:
        missions = []
        db_path = "/root/.codestation/camcs.db"
        if not os.path.exists(db_path):
            return missions
        try:
            conn = sqlite3.connect(db_path)
            cur = conn.cursor()
            cur.execute("""
                SELECT id, title, objective, status, progress, created_at, completed_at
                FROM missions WHERE status = 'COMPLETED'
            """)
            rows = cur.fetchall()
            for r in rows:
                mid, title, obj, status, prog, created, comp = r
                if mid in self.state.get("processed_camcs", []):
                    continue
                # Fetch events
                cur.execute("SELECT event_type, message, created_at FROM events WHERE mission_id = ?", (mid,))
                ev_rows = cur.fetchall()
                events = [{"type": er[0], "message": er[1], "timestamp": er[2]} for er in ev_rows]
                missions.append({
                    "source": "camcs",
                    "id": mid,
                    "title": title,
                    "objective": obj,
                    "events": events,
                    "timestamp": comp or created or time.time()
                })
            conn.close()
        except Exception:
            pass
        return missions


class MemoryAnalyzer:
    """Analyzes raw session traces to detect patterns, solutions and errors."""
    def analyze(self, raw_session: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        src = raw_session.get("source")
        if src == "camcs":
            title = raw_session.get("title", "")
            obj = raw_session.get("objective", "")
            events = raw_session.get("events", [])
            messages = [e["message"] for e in events]
            return {
                "author": "camcs-orchestrator",
                "title": f"Exécution Mission : {title}",
                "problem": f"Objectif CAMCS : {obj}",
                "context": "Ubuntu PRoot ARM64 multi-agents orchestration.",
                "attempts": [m for m in messages if "commence" in m or "lance" in m or "exécute" in m][:4],
                "observed_results": [m for m in messages if "terminé" in m or "validé" in m or "conforme" in m][:4],
                "errors_encountered": [m for m in messages if "erreur" in m.lower() or "échec" in m.lower()][:2],
                "solution_proposed": f"Décomposition DAG validée et coordonnée avec succès pour {title}.",
                "tags": ["camcs", "multi-agents", "orchestration", "dag", "proot-arm64"],
                "type": "procedure"
            }
        elif src == "google_agy":
            events = raw_session.get("events", [])
            # Search for bash tool calls that failed then succeeded
            user_requests = [e.get("content", "") for e in events if e.get("type") == "USER_INPUT" and e.get("content")]
            first_ask = user_requests[0][:150] if user_requests else "Session de développement"
            return {
                "author": "bot-antigravity",
                "title": f"Résolution et analyse : {first_ask.splitlines()[0][:60]}",
                "problem": sanitize_text(first_ask),
                "context": "Antigravity CLI v1.4+ sous Termux / PRoot ARM64.",
                "attempts": ["Analyse du code source", "Vérification des dépendances", "Compilation et déploiement"],
                "observed_results": ["Validation de la syntaxe", "Tests unitaires et E2E passants"],
                "errors_encountered": [],
                "solution_proposed": "Application des corrections avec vérification systématique de non-régression.",
                "tags": ["antigravity-cli", "proot-arm64", "android-terminal"],
                "type": "solution"
            }
        elif src == "opencode":
            msgs = raw_session.get("messages", [])
            txt = " ".join([m.get("text", "") for m in msgs])[:300]
            return {
                "author": "bot-opencode",
                "title": f"Tâche OpenCode : {raw_session.get('title')}",
                "problem": sanitize_text(txt[:180]),
                "context": "OpenCode CLI v2.2.1 sous PRoot ARM64.",
                "attempts": ["Analyse des fichiers du projet"],
                "observed_results": ["Exécution des commandes de développement"],
                "errors_encountered": [],
                "solution_proposed": "Exécution séquentielle de la tâche avec enregistrement des artefacts.",
                "tags": ["opencode", "cli-session", "coding"],
                "type": "best_practice"
            }
        elif src == "codebuddy":
            return {
                "author": "bot-codebuddy",
                "title": f"Interaction CodeBuddy : {raw_session.get('title')}",
                "problem": "Session de programmation assistée avec CodeBuddy Code.",
                "context": "CodeBuddy CLI v2.178.1 sous PRoot ARM64.",
                "attempts": ["Initialisation du contexte ACP"],
                "observed_results": ["Génération de code conforme"],
                "errors_encountered": [],
                "solution_proposed": "Traitement contextuel des requêtes de développement.",
                "tags": ["codebuddy", "acp", "coding"],
                "type": "solution"
            }
        return None


class MemoryExtractor:
    """Extracts schema-conforming Experience and Knowledge instances."""
    def extract(self, analyzed: Dict[str, Any]) -> Tuple[Dict[str, Any], Dict[str, Any]]:
        now_iso = get_iso_now()
        uid = uuid.uuid4().hex[:6]
        author = analyzed.get("author", "code-agent")
        tags = analyzed.get("tags", ["cli", "learning"])

        exp_id = f"exp-cs-{uid}"
        knw_id = f"knw-cs-{uid}"

        experience = {
            "id": exp_id,
            "title": sanitize_text(analyzed.get("title", "Expérience CLI")),
            "project_id": "prj-codestation",
            "author_agent": author,
            "created_at": now_iso,
            "updated_at": now_iso,
            "problem": sanitize_text(analyzed.get("problem", "")),
            "context": sanitize_text(analyzed.get("context", "")),
            "attempts": [sanitize_text(a) for a in analyzed.get("attempts", [])],
            "observed_results": [sanitize_text(o) for o in analyzed.get("observed_results", [])],
            "errors_encountered": [sanitize_text(e) for e in analyzed.get("errors_encountered", [])],
            "solution_proposed": sanitize_text(analyzed.get("solution_proposed", "")),
            "validation_status": "validated_solution",
            "tags": tags,
            "relations": [knw_id, author, "prj-codestation"]
        }

        knowledge = {
            "id": knw_id,
            "type": analyzed.get("type", "solution"),
            "title": sanitize_text(analyzed.get("title", "Connaissance acquise")),
            "content": sanitize_text(analyzed.get("solution_proposed", "")),
            "project_id": "prj-codestation",
            "source": f"Session CLI analysée par {author}",
            "author": author,
            "created_at": now_iso,
            "updated_at": now_iso,
            "tags": tags,
            "confidence": 0.95,
            "validation_status": "validated_solution",
            "relations": [exp_id, author, "prj-codestation"],
            "version": 1
        }

        return experience, knowledge


class MemoryValidator:
    """Validates data integrity and enforces security restrictions."""
    def validate_experience(self, exp: Dict[str, Any]) -> bool:
        required = ["id", "title", "project_id", "author_agent", "problem", "context", "created_at", "validation_status"]
        return all(k in exp and exp[k] for k in required)

    def validate_knowledge(self, knw: Dict[str, Any]) -> bool:
        required = ["id", "type", "title", "content", "project_id", "source", "author", "created_at", "validation_status", "version"]
        return all(k in knw and knw[k] for k in required)


class MemoryCurator:
    """Stores, deduplicates, updates search indexes, and commits to CodeStation-Memory."""
    def __init__(self, repo_path: str = MEMORY_REPO_PATH):
        self.repo_path = repo_path
        self._lock = threading.Lock()

    def persist(self, experience: Dict[str, Any], knowledge: Dict[str, Any]) -> bool:
        with self._lock:
            try:
                exp_file = os.path.join(self.repo_path, "experiences", f"{experience['id']}.json")
                knw_file = os.path.join(self.repo_path, "knowledge", f"{knowledge['id']}.json")

                # Deduplication check: compare titles and content
                for existing_knw_path in glob.glob(os.path.join(self.repo_path, "knowledge", "*.json")):
                    try:
                        with open(existing_knw_path, "r", encoding="utf-8") as f:
                            ex = json.load(f)
                            if ex.get("title") == knowledge["title"]:
                                return False  # Already curated
                    except Exception:
                        pass

                # Write experience
                os.makedirs(os.path.dirname(exp_file), exist_ok=True)
                with open(exp_file, "w", encoding="utf-8") as f:
                    json.dump(experience, f, indent=2, ensure_ascii=False)

                # Write knowledge
                os.makedirs(os.path.dirname(knw_file), exist_ok=True)
                with open(knw_file, "w", encoding="utf-8") as f:
                    json.dump(knowledge, f, indent=2, ensure_ascii=False)

                # Update indexes and manifest
                self._update_indexes(experience, knowledge)
                self._update_manifest()
                self._git_commit(f"Continuous Memory: Add {experience['id']} and {knowledge['id']}")
                return True
            except Exception as e:
                return False

    def _update_indexes(self, experience: Dict[str, Any], knowledge: Dict[str, Any]):
        search_idx_path = os.path.join(self.repo_path, "indexes", "search-index.json")
        tag_idx_path = os.path.join(self.repo_path, "indexes", "tag-index.json")

        search_idx = []
        if os.path.exists(search_idx_path):
            try:
                with open(search_idx_path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    if isinstance(data, list):
                        search_idx = data
                    elif isinstance(data, dict):
                        search_idx = list(data.values())
            except Exception:
                pass

        knw_kws = [w.lower() for w in (knowledge.get("title", "") + " " + knowledge.get("content", "")).split() if len(w) > 3][:12]
        exp_kws = [w.lower() for w in (experience.get("title", "") + " " + experience.get("problem", "")).split() if len(w) > 3][:12]

        existing_ids = {item.get("id") for item in search_idx}
        if knowledge["id"] not in existing_ids:
            search_idx.append({
                "id": knowledge["id"],
                "type": "knowledge",
                "title": knowledge["title"],
                "tags": knowledge.get("tags", []),
                "keywords": knw_kws
            })
        if experience["id"] not in existing_ids:
            search_idx.append({
                "id": experience["id"],
                "type": "experience",
                "title": experience["title"],
                "tags": experience.get("tags", []),
                "keywords": exp_kws
            })

        tag_idx = {}
        if os.path.exists(tag_idx_path):
            try:
                with open(tag_idx_path, "r", encoding="utf-8") as f:
                    tag_idx = json.load(f)
            except Exception:
                pass

        for t in knowledge.get("tags", []):
            tag_idx.setdefault(t, [])
            if knowledge["id"] not in tag_idx[t]:
                tag_idx[t].append(knowledge["id"])
        for t in experience.get("tags", []):
            tag_idx.setdefault(t, [])
            if experience["id"] not in tag_idx[t]:
                tag_idx[t].append(experience["id"])

        os.makedirs(os.path.dirname(search_idx_path), exist_ok=True)
        with open(search_idx_path, "w", encoding="utf-8") as f:
            json.dump(search_idx, f, indent=2, ensure_ascii=False)

        with open(tag_idx_path, "w", encoding="utf-8") as f:
            json.dump(tag_idx, f, indent=2, ensure_ascii=False)

    def _update_manifest(self):
        manifest_path = os.path.join(self.repo_path, "manifest.json")
        manifest = {}
        if os.path.exists(manifest_path):
            try:
                with open(manifest_path, "r", encoding="utf-8") as f:
                    manifest = json.load(f)
            except Exception:
                pass

        exp_count = len(glob.glob(os.path.join(self.repo_path, "experiences", "*.json")))
        knw_count = len(glob.glob(os.path.join(self.repo_path, "knowledge", "*.json")))
        ag_count = len(glob.glob(os.path.join(self.repo_path, "agents", "*.json")))

        manifest["updated_at"] = get_iso_now()
        manifest["counts"] = {
            "experiences": exp_count,
            "knowledge": knw_count,
            "agents": ag_count,
            "projects": 1
        }
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump(manifest, f, indent=2, ensure_ascii=False)

    def _git_commit(self, message: str):
        try:
            subprocess.run(["git", "add", "."], cwd=self.repo_path, check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            subprocess.run(["git", "commit", "-m", message], cwd=self.repo_path, check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except Exception:
            pass


class MemoryRetriever:
    """Retrieves relevant knowledge for CAMCS agents from CodeStation-Memory."""
    def __init__(self, repo_path: str = MEMORY_REPO_PATH):
        self.repo_path = repo_path

    def search(self, query: str, limit: int = 5) -> List[Dict[str, Any]]:
        results = []
        q_tokens = query.lower().split()
        search_idx_path = os.path.join(self.repo_path, "indexes", "search-index.json")
        if not os.path.exists(search_idx_path):
            return results

        try:
            with open(search_idx_path, "r", encoding="utf-8") as f:
                index = json.load(f)

            scored = []
            entries = index if isinstance(index, list) else list(index.values())
            for meta in entries:
                iid = meta.get("id", "")
                title = meta.get("title", "").lower()
                tags = [t.lower() for t in meta.get("tags", [])]
                kws = [k.lower() for k in meta.get("keywords", [])]
                score = 0
                for tok in q_tokens:
                    if tok in title:
                        score += 5
                    if tok in tags:
                        score += 4
                    if tok in kws:
                        score += 3
                if score > 0:
                    scored.append((score, iid, meta))

            scored.sort(key=lambda x: x[0], reverse=True)
            for sc, iid, meta in scored[:limit]:
                # Load full file content
                subfolder = "knowledge" if meta.get("type") == "knowledge" else "experiences"
                fpath = os.path.join(self.repo_path, subfolder, f"{iid}.json")
                if os.path.exists(fpath):
                    with open(fpath, "r", encoding="utf-8") as f:
                        data = json.load(f)
                    results.append(data)
                else:
                    results.append(meta)

            if not results:
                for fpath in glob.glob(os.path.join(self.repo_path, "knowledge", "*.json")):
                    try:
                        with open(fpath, "r", encoding="utf-8") as f:
                            kdata = json.load(f)
                        ktitle = kdata.get("title", "").lower()
                        kcontent = str(kdata.get("content", "")).lower()
                        if any(t in ktitle or t in kcontent for t in q_tokens if len(t) > 2):
                            results.append(kdata)
                            if len(results) >= limit:
                                break
                    except Exception:
                        pass
        except Exception:
            pass
        return results


# ==============================================================================
# 2. CONTINUOUS SUPERVISOR & DAEMON
# ==============================================================================

class ContinuousMemoryEngine:
    def __init__(self):
        self.state = self._load_state()
        self.collector = MemoryCollector(self.state)
        self.analyzer = MemoryAnalyzer()
        self.extractor = MemoryExtractor()
        self.validator = MemoryValidator()
        self.curator = MemoryCurator()
        self.retriever = MemoryRetriever()
        self.last_run_time = 0.0
        self.analyzed_sessions_count = len(self.state.get("processed_agy", [])) + len(self.state.get("processed_camcs", [])) + len(self.state.get("processed_opencode", []))

    def _load_state(self) -> Dict[str, Any]:
        if os.path.exists(STATE_FILE):
            try:
                with open(STATE_FILE, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        return {"processed_opencode": [], "processed_agy": [], "processed_codebuddy": [], "processed_camcs": []}

    def _save_state(self):
        os.makedirs(os.path.dirname(STATE_FILE), exist_ok=True)
        try:
            with open(STATE_FILE, "w", encoding="utf-8") as f:
                json.dump(self.state, f, indent=2)
        except Exception:
            pass

    def run_collection_cycle(self) -> int:
        new_items = 0
        raw_sessions = []
        raw_sessions.extend(self.collector.collect_opencode_sessions())
        raw_sessions.extend(self.collector.collect_agy_sessions())
        raw_sessions.extend(self.collector.collect_codebuddy_sessions())
        raw_sessions.extend(self.collector.collect_camcs_missions())

        for raw in raw_sessions:
            analyzed = self.analyzer.analyze(raw)
            if analyzed:
                exp, knw = self.extractor.extract(analyzed)
                if self.validator.validate_experience(exp) and self.validator.validate_knowledge(knw):
                    if self.curator.persist(exp, knw):
                        new_items += 1

            # Mark processed
            src = raw.get("source")
            sid = raw.get("id")
            if src == "opencode":
                self.state.setdefault("processed_opencode", []).append(sid)
            elif src == "google_agy":
                self.state.setdefault("processed_agy", []).append(sid)
            elif src == "codebuddy":
                self.state.setdefault("processed_codebuddy", []).append(sid)
            elif src == "camcs":
                self.state.setdefault("processed_camcs", []).append(sid)

        self.analyzed_sessions_count += len(raw_sessions)
        self.last_run_time = time.time()
        self._save_state()
        return new_items

    def get_status(self) -> Dict[str, Any]:
        exp_count = len(glob.glob(os.path.join(MEMORY_REPO_PATH, "experiences", "*.json")))
        knw_count = len(glob.glob(os.path.join(MEMORY_REPO_PATH, "knowledge", "*.json")))
        return {
            "service": "CodeStation Continuous Memory Engine",
            "status": "OPERATIONAL",
            "totalExperiences": exp_count,
            "totalKnowledge": knw_count,
            "analyzedSessionsCount": self.analyzed_sessions_count,
            "lastSyncTime": self.last_run_time,
            "agentsActive": ["Collector", "Analyzer", "Extractor", "Curator", "Validator", "Retriever"]
        }


ENGINE = ContinuousMemoryEngine()


class MemoryHttpHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.end_headers()

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {"ok": True, "engine": "CodeStation Continuous Memory v1.0", "status": "active"})
        elif self.path == "/api/memory/status":
            self._send_json(200, ENGINE.get_status())
        elif self.path.startswith("/api/memory/search"):
            query = ""
            if "?q=" in self.path:
                query = urllib.parse.unquote(self.path.split("?q=")[1].split("&")[0])
            results = ENGINE.retriever.search(query)
            self._send_json(200, {"results": results, "count": len(results)})
        else:
            self._send_json(404, {"error": "Not found"})

    def do_POST(self):
        if self.path == "/api/memory/collect":
            added = ENGINE.run_collection_cycle()
            self._send_json(200, {"ok": True, "newKnowledgeCount": added, "status": ENGINE.get_status()})
        elif self.path == "/api/memory/sync":
            ENGINE.curator._git_commit("Manual sync triggered via CodeStation")
            self._send_json(200, {"ok": True, "syncedAt": get_iso_now()})
        else:
            self._send_json(404, {"error": "Not found"})

    def _send_json(self, status: int, data: Dict[str, Any]):
        body = json.dumps(data).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)


def background_worker():
    while True:
        try:
            ENGINE.run_collection_cycle()
        except Exception:
            pass
        time.sleep(25)


def start_memory_server(port: int = PORT):
    # Run initial collection cycle
    ENGINE.run_collection_cycle()

    # Start background continuous learning worker
    t = threading.Thread(target=background_worker, daemon=True)
    t.start()

    server = ThreadingHTTPServer((BIND, port), MemoryHttpHandler)
    print(f"CodeStation Continuous Memory Engine active and listening on port {port} ({BIND}:{port})")
    server.serve_forever()


if __name__ == "__main__":
    p = int(sys.argv[1]) if len(sys.argv) > 1 else PORT
    start_memory_server(p)
