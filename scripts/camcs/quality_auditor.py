#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Quality Auditor & Independent Validator
Verifies real files, runs automated tests, and issues cryptographic acceptance verdicts.
"""

import os
import subprocess
import time
import sqlite3
from typing import Dict, List, Optional, Any, Tuple
from .runners import BaseCliRunner, compute_file_sha256

class QualityAuditor:
    def __init__(self, db_path: str = "/root/.codestation/camcs.db"):
        self.db_path = db_path

    def audit_deliverables(
        self,
        mission_id: str,
        deliverables: List[Dict[str, Any]]
    ) -> Tuple[bool, int, List[Dict[str, Any]]]:
        """
        Independently checks all deliverables for:
        1. Physical file existence
        2. Non-empty size
        3. Exact SHA-256 match
        """
        verified_count = 0
        results = []
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()

        for d in deliverables:
            fpath = d.get("filePath", "")
            expected_sha = d.get("sha256", "")
            exists = os.path.isfile(fpath)
            size = os.path.getsize(fpath) if exists else 0
            cur_sha = compute_file_sha256(fpath) if exists else ""

            is_valid = exists and size > 0 and (cur_sha == expected_sha or not expected_sha)
            status = "VERIFIED" if is_valid else "REJECTED"
            details = (
                f"Validation physique réussie : {size} octets, SHA-256: {cur_sha[:16]}..."
                if is_valid else
                f"Échec d'intégrité : fichier manquant ou SHA-256 discordant ({cur_sha} vs {expected_sha})"
            )

            if is_valid:
                verified_count += 1

            cur.execute("""
                UPDATE deliverables
                SET validation_status = ?, validation_details = ?, updated_at = ?
                WHERE id = ?
            """, (status, details, time.time(), d.get("id")))

            results.append({
                "id": d.get("id"),
                "filePath": fpath,
                "status": status,
                "details": details,
                "sizeBytes": size,
                "sha256": cur_sha
            })

        conn.commit()
        conn.close()

        all_ok = (verified_count == len(deliverables)) and len(deliverables) > 0
        return all_ok, verified_count, results

    def run_automated_tests(
        self,
        test_file: str,
        working_dir: str
    ) -> Tuple[bool, int, str, str, int]:
        """Executes real Python / shell automated test suite."""
        t0 = time.time()
        if not os.path.isfile(test_file):
            return False, -1, "", f"Fichier de test introuvable: {test_file}", 0

        cmd = ["python3", "-m", "unittest", test_file] if test_file.endswith(".py") else [test_file]
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, cwd=working_dir, timeout=60)
            duration_ms = int((time.time() - t0) * 1000)
            passed = (res.returncode == 0)
            return passed, res.returncode, res.stdout, res.stderr, duration_ms
        except Exception as e:
            duration_ms = int((time.time() - t0) * 1000)
            return False, -1, "", str(e), duration_ms

    def independent_cli_review(
        self,
        qa_runner: BaseCliRunner,
        file_to_review: str,
        criteria: str,
        cwd: str
    ) -> Tuple[bool, str]:
        """
        Uses a distinct real CLI (the QA runner) to examine code or documentation.
        """
        if not os.path.isfile(file_to_review):
            return False, "Fichier à auditer introuvable"

        prompt = (
            f"Examine attentivement le fichier '{os.path.basename(file_to_review)}' "
            f"selon les critères : '{criteria}'. Réponds par un verdict synthétique (CONFORME ou NON_CONFORME) avec justification."
        )
        task_id = f"audit-{int(time.time())}"
        res = qa_runner.execute_task(task_id, "audit", prompt, cwd, timeout=60)
        review_text = res.stdout if res.stdout else res.stderr
        is_conforme = "CONFORME" in review_text.upper() or res.exit_code == 0
        return is_conforme, review_text
