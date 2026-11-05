#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Collective Memory Client
Bridges CAMCS missions with the Continuous Memory Engine (port 19224 / /root/CodeStation-Memory).
Provides high-fidelity synchronization of architectural decisions, quality gate validations,
learned error avoidance rules, and verified mission syntheses.
"""

import os
import time
import json
import uuid
import urllib.request
import urllib.error
import urllib.parse
from typing import Dict, List, Optional, Any

MEMORY_REPO_PATH = "/root/CodeStation-Memory"


class CollectiveMemoryClient:
    def __init__(self, memory_port: int = 19224, repo_path: str = MEMORY_REPO_PATH):
        self.base_url = f"http://127.0.0.1:{memory_port}"
        self.repo_path = repo_path

    def query_knowledge(self, query_text: str, limit: int = 3) -> List[Dict[str, Any]]:
        """Queries continuous memory repository for relevant validated solutions."""
        url = f"{self.base_url}/api/memory/search"
        payload = json.dumps({"query": query_text, "limit": limit}).encode("utf-8")
        try:
            req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                return data.get("results", [])
        except Exception:
            # Fallback to GET query
            try:
                get_url = f"{self.base_url}/api/memory/search?q={urllib.parse.quote(query_text)}"
                with urllib.request.urlopen(get_url, timeout=3.0) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    return data.get("results", [])
            except Exception:
                # Disk fallback directly into knowledge directory
                return self._search_disk_fallback(query_text, limit)

    def _search_disk_fallback(self, query_text: str, limit: int = 3) -> List[Dict[str, Any]]:
        results = []
        knw_dir = os.path.join(self.repo_path, "knowledge")
        if not os.path.isdir(knw_dir):
            return []
        q_words = set(query_text.lower().split())
        for f in os.listdir(knw_dir):
            if f.endswith(".json"):
                fpath = os.path.join(knw_dir, f)
                try:
                    with open(fpath, "r", encoding="utf-8") as kf:
                        item = json.load(kf)
                        text_corpus = f"{item.get('title', '')} {item.get('problem', '')} {item.get('solution', '')} {' '.join(item.get('tags', []))}".lower()
                        score = sum(1 for w in q_words if w in text_corpus)
                        if score > 0:
                            results.append((score, item))
                except Exception:
                    continue
        results.sort(key=lambda x: x[0], reverse=True)
        return [r[1] for r in results[:limit]]

    def record_learning(
        self,
        title: str,
        problem: str,
        solution: str,
        error_avoidance: str = "",
        context: str = "",
        tags: Optional[List[str]] = None,
        author: str = "camcs-agent",
        category: str = "solution"
    ) -> bool:
        """Records an immediately learned rule/solution into perpetual memory."""
        url = f"{self.base_url}/api/memory/learn"
        payload = json.dumps({
            "title": title,
            "problem": problem,
            "solution": solution,
            "error_avoidance": error_avoidance,
            "context": context,
            "tags": tags or ["camcs", "learned-rule"],
            "author": author,
            "type": category
        }).encode("utf-8")
        try:
            req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=4.0) as resp:
                return resp.status == 200
        except Exception:
            # Fallback to direct file emission
            return self._record_disk_fallback(title, problem, solution, error_avoidance, context, tags or ["camcs"], author)

    def record_decision(
        self,
        title: str,
        decision: str,
        rationale: str,
        context: str = "",
        consequences: str = "",
        error_avoidance: str = "",
        tags: Optional[List[str]] = None,
        author: str = "camcs-architect"
    ) -> bool:
        """
        Records an Architectural Decision (ADR) into perpetual memory.
        Inspired by Festival camp/internal/concept.
        """
        full_title = f"Décision d'Architecture : {title}"
        full_problem = f"Contexte :\n{context}\n\nMotivation & Rationale :\n{rationale}"
        full_solution = f"Décision adoptée :\n{decision}\n\nConséquences & Invariants :\n{consequences}"
        adr_tags = list(set((tags or []) + ["architecture-decision", "adr", "camcs"]))
        return self.record_learning(
            title=full_title,
            problem=full_problem,
            solution=full_solution,
            error_avoidance=error_avoidance,
            context=context,
            tags=adr_tags,
            author=author,
            category="decision"
        )

    def record_repair_experience(
        self,
        task_title: str,
        error_msg: str,
        remediation_applied: str,
        iteration_count: int,
        context: str = ""
    ) -> bool:
        """
        Records a successfully remediated error into perpetual memory
        so future agent executions inherit the avoidance pattern.
        """
        title = f"Règle d'Évitement : {task_title}"
        problem = f"Erreur rencontrée : {error_msg}"
        solution = f"Remédiation validée en {iteration_count} tentative(s) : {remediation_applied}"
        avoidance = f"Ne pas reproduire : {error_msg}. Appliquer la solution : {remediation_applied}"
        tags = ["auto-repair", "error-avoidance", "quality-gate", "camcs"]
        return self.record_learning(
            title=title,
            problem=problem,
            solution=solution,
            error_avoidance=avoidance,
            context=context,
            tags=tags,
            author="camcs-repair-engine",
            category="error_avoidance"
        )

    def record_mission_synthesis(
        self,
        mission_id: str,
        title: str,
        objective: str,
        dag_phases: List[str],
        gates_passed: int,
        repairs_resolved: int,
        deliverables_count: int
    ) -> bool:
        """
        Synthesizes mission lessons and architecture decisions for perpetual retrieval.
        """
        syn_title = f"Synthèse de Mission Validée : {title}"
        problem = f"Objectif initial :\n{objective}\n\nPhases DAG exécutées : {', '.join(dag_phases)}"
        solution = (
            f"Mission menée à terme avec succès.\n"
            f"- Livrables certifiés conformes : {deliverables_count}\n"
            f"- Quality Gates passées : {gates_passed}\n"
            f"- Réparations autonomes résolues : {repairs_resolved}"
        )
        avoidance = "Réutiliser les patterns validés pour toute mission de complexité équivalente."
        tags = ["mission-synthesis", "verified", "camcs", "continuous-memory"]
        return self.record_learning(
            title=syn_title,
            problem=problem,
            solution=solution,
            error_avoidance=avoidance,
            context=f"CAMCS Mission ID: {mission_id}",
            tags=tags,
            author="camcs-orchestrator",
            category="synthesis"
        )

    def ingest_mission_experience(
        self,
        mission_id: str,
        title: str,
        objective: str,
        deliverables: List[Dict[str, Any]],
        proofs: List[Dict[str, Any]],
        success: bool = True
    ) -> bool:
        """Submits verified mission outcomes to collective memory."""
        url = f"{self.base_url}/api/memory/collect"
        payload = json.dumps({
            "missionId": mission_id,
            "title": title,
            "objective": objective,
            "deliverablesCount": len(deliverables),
            "proofsCount": len(proofs),
            "verified": success
        }).encode("utf-8")
        try:
            req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=4.0) as resp:
                return resp.status == 200
        except Exception:
            return False

    def _record_disk_fallback(
        self,
        title: str,
        problem: str,
        solution: str,
        error_avoidance: str,
        context: str,
        tags: List[str],
        author: str
    ) -> bool:
        """Fallback direct disk write if server on port 19224 is temporarily offline."""
        try:
            knw_dir = os.path.join(self.repo_path, "knowledge")
            exp_dir = os.path.join(self.repo_path, "experiences")
            os.makedirs(knw_dir, exist_ok=True)
            os.makedirs(exp_dir, exist_ok=True)
            uid = str(uuid.uuid4())[:8]
            now_iso = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())

            knw_item = {
                "id": f"knw-cs-{uid}",
                "title": title,
                "problem": problem,
                "solution": solution,
                "error_avoidance": error_avoidance,
                "context": context,
                "tags": tags,
                "author": author,
                "created_at": now_iso
            }
            with open(os.path.join(knw_dir, f"knw-cs-{uid}.json"), "w", encoding="utf-8") as f:
                json.dump(knw_item, f, indent=2, ensure_ascii=False)

            exp_item = {
                "id": f"exp-cs-{uid}",
                "title": title,
                "problem": problem,
                "solution_proposed": solution,
                "context": context,
                "validation_status": "validated_solution",
                "tags": tags,
                "author_agent": author,
                "created_at": now_iso
            }
            with open(os.path.join(exp_dir, f"exp-cs-{uid}.json"), "w", encoding="utf-8") as f:
                json.dump(exp_item, f, indent=2, ensure_ascii=False)
            return True
        except Exception:
            return False
