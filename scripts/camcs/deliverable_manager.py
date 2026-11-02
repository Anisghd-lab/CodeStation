#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Deliverable & Artifact Manager
Tracks, verifies, hashes, and persists all generated files and execution proofs.
"""

import os
import time
import uuid
import sqlite3
import hashlib
from typing import Dict, List, Optional, Any

DB_PATH = os.path.expanduser("/root/.codestation/camcs.db")

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
        """Registers a physical file deliverable in the database."""
        deliv_id = f"deliv-{uuid.uuid4().hex[:8]}"
        file_name = os.path.basename(file_path)
        sha256 = compute_sha256(file_path)
        size_bytes = os.path.getsize(file_path) if os.path.isfile(file_path) else 0
        now = time.time()

        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT OR REPLACE INTO deliverables
            (id, mission_id, task_id, agent_id, agent_name, file_path, file_name, doc_type, description, sha256, size_bytes, validation_status, validation_details, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            deliv_id, mission_id, task_id, agent_id, agent_name, file_path, file_name,
            doc_type, description, sha256, size_bytes, validation_status, validation_details, now, now
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
            "createdAt": now,
            "updatedAt": now
        }

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
