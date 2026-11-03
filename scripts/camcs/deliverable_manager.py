#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Deliverable & Artifact Manager
Tracks, verifies, hashes, and persists all generated files and execution proofs.
Implements the strict deliverable lifecycle:
  CREATED → WRITING → FINALIZED → HASHED → REGISTERED → VERIFIED
"""

import os
import time
import uuid
import sqlite3
import hashlib
from typing import Dict, List, Optional, Any

DB_PATH = os.path.expanduser("/root/.codestation/camcs.db")

# Deliverable lifecycle states
class DeliverableState:
    CREATED = "CREATED"
    WRITING = "WRITING"
    FINALIZED = "FINALIZED"
    HASHED = "HASHED"
    REGISTERED = "REGISTERED"
    VERIFIED = "VERIFIED"
    SHA_MISMATCH = "SHA_MISMATCH"
    PENDING = "PENDING"


def compute_sha256(path: str) -> str:
    if not os.path.isfile(path):
        return ""
    h = hashlib.sha256()
    try:
        with open(path, "rb") as f:
            while chunk := f.read(65536):
                h.update(chunk)
        return h.hexdigest()
    except Exception:
        return ""


class DeliverableManager:
    def __init__(self, db_path: str = DB_PATH):
        self.db_path = db_path
        self._ensure_lifecycle_column()

    def _ensure_lifecycle_column(self):
        """Add lifecycle_state column if it doesn't exist."""
        try:
            conn = sqlite3.connect(self.db_path)
            cur = conn.cursor()
            existing_cols = [c[1] for c in cur.execute("PRAGMA table_info(deliverables)").fetchall()]
            if "lifecycle_state" not in existing_cols:
                cur.execute("ALTER TABLE deliverables ADD COLUMN lifecycle_state TEXT DEFAULT 'REGISTERED'")
            if "version" not in existing_cols:
                cur.execute("ALTER TABLE deliverables ADD COLUMN version INTEGER DEFAULT 1")
            if "previous_sha256" not in existing_cols:
                cur.execute("ALTER TABLE deliverables ADD COLUMN previous_sha256 TEXT")
            conn.commit()
            conn.close()
        except Exception:
            pass

    def register_deliverable(
        self,
        mission_id: str,
        task_id: str,
        agent_id: str,
        agent_name: str,
        file_path: str,
        doc_type: str,
        description: str,
        producing_cli: str = "unknown",
        validation_status: str = "PENDING",
        validation_details: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        Registers a physical file deliverable in the database.
        Implements transactional lifecycle: only hashes a FINALIZED file.
        If a deliverable for the same mission+task+file already exists, creates a new version.
        """
        file_name = os.path.basename(file_path)
        now = time.time()

        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()

        # Check for existing deliverable for same file in same task
        cur.execute("""
            SELECT id, sha256, version FROM deliverables
            WHERE mission_id = ? AND task_id = ? AND file_path = ?
            ORDER BY version DESC LIMIT 1
        """, (mission_id, task_id, file_path))
        existing = cur.fetchone()

        previous_sha = None
        version = 1
        if existing:
            previous_sha = existing[1]
            version = (existing[2] or 1) + 1

        # Only compute SHA if the file exists and is non-empty (FINALIZED state)
        sha256 = ""
        lifecycle_state = DeliverableState.CREATED
        size_bytes = 0

        if os.path.isfile(file_path):
            size_bytes = os.path.getsize(file_path)
            if size_bytes > 0:
                lifecycle_state = DeliverableState.FINALIZED
                sha256 = compute_sha256(file_path)
                if sha256:
                    lifecycle_state = DeliverableState.HASHED

                    # If there's a previous SHA and it differs, note the divergence
                    if previous_sha and previous_sha != sha256:
                        validation_details = (validation_details or "") + f" [SHA-256 changed: prev={previous_sha[:16]}... new={sha256[:16]}...]"

                    lifecycle_state = DeliverableState.REGISTERED

        deliv_id = f"deliv-{uuid.uuid4().hex[:8]}"

        cur.execute("""
            INSERT OR REPLACE INTO deliverables
            (id, mission_id, task_id, agent_id, agent_name, file_path, file_name, doc_type, description, sha256, size_bytes, validation_status, validation_details, created_at, updated_at, lifecycle_state, version, previous_sha256)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            deliv_id, mission_id, task_id, agent_id, agent_name, file_path, file_name,
            doc_type, description, sha256, size_bytes, validation_status, validation_details, now, now,
            lifecycle_state, version, previous_sha
        ))
        conn.commit()
        conn.close()

        return {
            "id": deliv_id,
            "missionId": mission_id,
            "taskId": task_id,
            "agentId": agent_id,
            "agentName": agent_name,
            "filePath": file_path,
            "fileName": file_name,
            "docType": doc_type,
            "description": description,
            "producingCli": producing_cli,
            "sha256": sha256,
            "sizeBytes": size_bytes,
            "validationStatus": validation_status,
            "validationDetails": validation_details,
            "lifecycleState": lifecycle_state,
            "version": version,
            "previousSha256": previous_sha,
            "createdAt": now,
            "updatedAt": now
        }

    def verify_and_finalize_deliverable(self, deliv_id: str) -> Dict[str, Any]:
        """
        Re-hashes the file and transitions to VERIFIED if SHA matches,
        or SHA_MISMATCH if it has changed since registration.
        """
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()
        cur.execute("SELECT * FROM deliverables WHERE id = ?", (deliv_id,))
        row = cur.fetchone()
        if not row:
            conn.close()
            return {"error": f"Deliverable {deliv_id} not found"}

        fpath = row["file_path"]
        registered_sha = row["sha256"]

        if not os.path.isfile(fpath):
            cur.execute("""
                UPDATE deliverables SET validation_status = 'REJECTED', lifecycle_state = 'SHA_MISMATCH',
                validation_details = 'File not found on disk during verification', updated_at = ?
                WHERE id = ?
            """, (time.time(), deliv_id))
            conn.commit()
            conn.close()
            return {"status": "REJECTED", "reason": "File not found"}

        current_sha = compute_sha256(fpath)

        if registered_sha and current_sha != registered_sha:
            cur.execute("""
                UPDATE deliverables SET validation_status = 'SHA_MISMATCH', lifecycle_state = 'SHA_MISMATCH',
                validation_details = ?, previous_sha256 = ?, sha256 = ?, updated_at = ?
                WHERE id = ?
            """, (
                f"SHA divergence detected: registered={registered_sha[:16]}... current={current_sha[:16]}...",
                registered_sha, current_sha, time.time(), deliv_id
            ))
            conn.commit()
            conn.close()
            return {"status": "SHA_MISMATCH", "registered": registered_sha, "current": current_sha}

        cur.execute("""
            UPDATE deliverables SET validation_status = 'VERIFIED', lifecycle_state = 'VERIFIED',
            validation_details = 'SHA-256 integrity confirmed', updated_at = ?
            WHERE id = ?
        """, (time.time(), deliv_id))
        conn.commit()
        conn.close()
        return {"status": "VERIFIED", "sha256": current_sha}

    def register_proof(
        self,
        mission_id: str,
        task_id: str,
        agent_id: str,
        agent_name: str,
        cli_command: str,
        working_dir: str,
        exit_code: int,
        stdout: str,
        stderr: str,
        duration_ms: int,
        files_created: List[str],
        files_modified: List[str],
        verified: bool = True,
        verdict: str = "CONFORME",
        provider_id: str = "local_cli",
        model_id: str = "default"
    ) -> Dict[str, Any]:
        """Registers a verified execution proof in the database."""
        import json
        proof_id = f"proof-{uuid.uuid4().hex[:8]}"
        now = time.time()

        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT OR REPLACE INTO execution_proofs
            (id, mission_id, task_id, agent_id, agent_name, cli_command, working_dir, exit_code, stdout_summary, stderr_summary, execution_type, provider_id, model_id, duration_ms, files_created_json, files_modified_json, git_diff, verified_by_auditor, auditor_verdict, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            proof_id, mission_id, task_id, agent_id, agent_name, cli_command, working_dir,
            exit_code, stdout[:2000], stderr[:2000], "cli_process", provider_id, model_id,
            duration_ms, json.dumps(files_created), json.dumps(files_modified), None,
            1 if verified else 0, verdict, now
        ))
        conn.commit()
        conn.close()

        return {
            "id": proof_id,
            "missionId": mission_id,
            "taskId": task_id,
            "agentId": agent_id,
            "agentName": agent_name,
            "cliCommand": cli_command,
            "workingDir": working_dir,
            "exitCode": exit_code,
            "durationMs": duration_ms,
            "filesCreated": files_created,
            "filesModified": files_modified,
            "verified": verified,
            "verdict": verdict,
            "createdAt": now
        }

    def get_mission_deliverables(self, mission_id: str) -> List[Dict[str, Any]]:
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()
        cur.execute("""
            SELECT id, mission_id, task_id, agent_id, agent_name, file_path, file_name, doc_type, description, sha256, size_bytes, validation_status, validation_details, created_at, updated_at
            FROM deliverables WHERE mission_id = ? ORDER BY created_at ASC
        """, (mission_id,))
        rows = cur.fetchall()
        conn.close()

        return [
            {
                "id": r["id"],
                "missionId": r["mission_id"],
                "taskId": r["task_id"],
                "agentId": r["agent_id"],
                "agentName": r["agent_name"],
                "filePath": r["file_path"],
                "fileName": r["file_name"],
                "docType": r["doc_type"],
                "description": r["description"],
                "sha256": r["sha256"],
                "sizeBytes": r["size_bytes"],
                "validationStatus": r["validation_status"],
                "validationDetails": r["validation_details"],
                "createdAt": r["created_at"],
                "updatedAt": r["updated_at"]
            }
            for r in rows
        ]

    def get_mission_proofs(self, mission_id: str) -> List[Dict[str, Any]]:
        import json
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()
        cur.execute("""
            SELECT id, mission_id, task_id, agent_id, agent_name, cli_command, working_dir, exit_code, stdout_summary, stderr_summary, execution_type, provider_id, model_id, duration_ms, files_created_json, files_modified_json, git_diff, verified_by_auditor, auditor_verdict, created_at
            FROM execution_proofs WHERE mission_id = ? ORDER BY created_at ASC
        """, (mission_id,))
        rows = cur.fetchall()
        conn.close()

        res = []
        for r in rows:
            files_c = []
            files_m = []
            try:
                files_c = json.loads(r["files_created_json"]) if r["files_created_json"] else []
                files_m = json.loads(r["files_modified_json"]) if r["files_modified_json"] else []
            except Exception:
                pass

            res.append({
                "id": r["id"],
                "missionId": r["mission_id"],
                "taskId": r["task_id"],
                "agentId": r["agent_id"],
                "agentName": r["agent_name"],
                "cliCommand": r["cli_command"],
                "workingDir": r["working_dir"],
                "exitCode": r["exit_code"],
                "stdoutSummary": r["stdout_summary"],
                "stderrSummary": r["stderr_summary"],
                "executionType": r["execution_type"],
                "providerId": r["provider_id"],
                "modelId": r["model_id"],
                "durationMs": r["duration_ms"],
                "filesCreated": files_c,
                "filesModified": files_m,
                "gitDiff": r["git_diff"],
                "verifiedByAuditor": r["verified_by_auditor"],
                "auditorVerdict": r["auditor_verdict"],
                "createdAt": r["created_at"]
            })
        return res

    def save_mission_report(
        self,
        mission_id: str,
        title: str,
        initial_objective: str,
        report_markdown: str,
        independent_validation_summary: str = "Validé par CAMCS Quality Auditor",
        final_state: str = "COMPLETED"
    ) -> Dict[str, Any]:
        """Saves a formal sealed mission report in SQLite."""
        report_id = f"rep-{uuid.uuid4().hex[:8]}"
        now = time.time()
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT OR REPLACE INTO mission_reports
            (id, mission_id, title, initial_objective, executed_plan_json, agents_summary_json, files_summary_json, deliverables_summary_json, tests_summary_json, independent_validation_summary, final_state, report_markdown, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            report_id, mission_id, title, initial_objective, "[]", "[]", "[]", "[]", "[]",
            independent_validation_summary, final_state, report_markdown, now
        ))
        conn.commit()
        conn.close()
        return {
            "id": report_id,
            "missionId": mission_id,
            "title": title,
            "initialObjective": initial_objective,
            "independentValidationSummary": independent_validation_summary,
            "finalState": final_state,
            "reportMarkdown": report_markdown,
            "createdAt": now
        }

    def get_mission_report(self, mission_id: str) -> Optional[Dict[str, Any]]:
        """Retrieves the sealed report for a mission."""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()
        cur.execute("SELECT * FROM mission_reports WHERE mission_id = ? ORDER BY created_at DESC LIMIT 1", (mission_id,))
        row = cur.fetchone()
        conn.close()

        if row:
            return {
                "id": row["id"],
                "missionId": row["mission_id"],
                "title": row["title"],
                "initialObjective": row["initial_objective"],
                "independentValidationSummary": row["independent_validation_summary"],
                "finalState": row["final_state"],
                "reportMarkdown": row["report_markdown"],
                "createdAt": row["created_at"]
            }

        report_file = f"/root/.codestation/missions/{mission_id}/report.md"
        if os.path.isfile(report_file):
            with open(report_file, "r", encoding="utf-8", errors="replace") as f:
                content = f.read()
            return {
                "id": f"rep-{mission_id[:8]}",
                "missionId": mission_id,
                "title": f"Rapport de mission {mission_id[:8]}",
                "initialObjective": "",
                "independentValidationSummary": "Validé par CAMCS Quality Auditor",
                "finalState": "COMPLETED",
                "reportMarkdown": content,
                "createdAt": os.path.getmtime(report_file)
            }
        return None
