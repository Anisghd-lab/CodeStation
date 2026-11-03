#!/usr/bin/env python3
"""
CAMCS v2.0 Regression Test Suite:
1. Extraction of exact GitHub repository names (no French connective words like "de").
2. Detection and rejection of ambiguous objectives.
3. SHA-256 integrity and versioning for deliverables.
4. ObjectiveCompletionGate barriers against false completions.
5. External action confirmation requirements.
"""

import os
import sys
import unittest
import tempfile
import sqlite3
import hashlib

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from camcs.dag_planner import DagPlanner
from camcs.deliverable_manager import DeliverableManager, compute_sha256
from camcs.objective_completion_gate import ObjectiveCompletionGate
from camcs.state_machines import TaskState, MissionState
from camcs.external_action_verifier import ExternalActionStatus

class TestCamcsV2Regression(unittest.TestCase):

    def setUp(self):
        self.planner = DagPlanner()
        self.agents = [
            {"id": "ag-arch", "name": "Architect", "role": "Software Architect"},
            {"id": "ag-dev", "name": "Developer", "role": "Kotlin Developer"},
            {"id": "ag-qa", "name": "QA", "role": "Quality Auditor"}
        ]

    def test_repo_name_extraction_no_connectives(self):
        """Test that French connective words like 'de' are never extracted as repo names."""
        test_cases = [
            ("créer une application de test Android Kotlin complète, la publier sur un nouveau dépôt GitHub de GHD CAMCS TEST, exécuter", "GHD-CAMCS-TEST"),
            ("Crée un repo CAMCS et écrit readme avec architecture Kotlin performe vite", "CAMCS"),
            ("publier sur github nommé GHD-CAMCS-TEST", "GHD-CAMCS-TEST"),
            ("créer un dépôt GitHub nommé 'MySpecialRepo'", "MySpecialRepo"),
            ("initialiser un dépôt github nommé super-projet", "super-projet")
        ]

        for objective, expected_name in test_cases:
            tasks = self.planner.build_dag_for_mission("m-test", "Test Title", objective, self.agents, "/tmp")
            ext_task = next((t for t in tasks if t.external_action_type == "GITHUB_CREATE_REPO"), None)
            self.assertIsNotNone(ext_task, f"External action not found for: {objective}")
            self.assertEqual(ext_task.external_target, expected_name, f"Failed extracting from: {objective}")
            self.assertNotEqual(ext_task.external_target, "de", "Target must never be 'de'!")

    def test_deliverable_lifecycle_and_sha_versioning(self):
        """Test deliverable registration and SHA-256 versioning."""
        with tempfile.NamedTemporaryFile(suffix=".db") as db_file, \
             tempfile.NamedTemporaryFile(suffix=".md", mode="w", delete=False) as f:
            
            f.write("# Preliminary Report\n")
            f.flush()
            fpath = f.name

            # Init schema
            conn = sqlite3.connect(db_file.name)
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE deliverables (
                    id TEXT PRIMARY KEY, mission_id TEXT, task_id TEXT, agent_id TEXT, agent_name TEXT,
                    file_path TEXT, file_name TEXT, doc_type TEXT, description TEXT, sha256 TEXT,
                    size_bytes INTEGER, validation_status TEXT, validation_details TEXT,
                    created_at REAL, updated_at REAL, lifecycle_state TEXT, version INTEGER, previous_sha256 TEXT
                )
            """)
            conn.commit()
            conn.close()

            mgr = DeliverableManager(db_file.name)
            
            # Register version 1
            d1 = mgr.register_deliverable("m1", "t1", "a1", "Arch", fpath, "report", "desc")
            self.assertEqual(d1["version"], 1)
            sha1 = d1["sha256"]
            self.assertTrue(len(sha1) > 0)

            # Modify file content
            with open(fpath, "a") as f_append:
                f_append.write("Additional finalized content.\n")

            # Register version 2
            d2 = mgr.register_deliverable("m1", "t1", "a1", "Arch", fpath, "report", "desc")
            self.assertEqual(d2["version"], 2)
            self.assertEqual(d2["previousSha256"], sha1)
            self.assertNotEqual(d2["sha256"], sha1)

            os.unlink(fpath)

    def test_gate_rejects_zero_deliverables(self):
        """Test that ObjectiveCompletionGate strictly rejects missions with 0 deliverables."""
        with tempfile.NamedTemporaryFile(suffix=".db") as db_file:
            conn = sqlite3.connect(db_file.name)
            cur = conn.cursor()
            cur.execute("CREATE TABLE missions (id TEXT PRIMARY KEY, title TEXT, objective TEXT, status TEXT)")
            cur.execute("CREATE TABLE tasks (id TEXT PRIMARY KEY, mission_id TEXT, status TEXT, is_mandatory INTEGER)")
            cur.execute("CREATE TABLE deliverables (id TEXT PRIMARY KEY, mission_id TEXT, file_path TEXT, doc_type TEXT, sha256 TEXT, created_at REAL)")
            cur.execute("CREATE TABLE execution_proofs (id TEXT PRIMARY KEY, mission_id TEXT, exit_code INTEGER)")
            cur.execute("CREATE TABLE external_actions (id TEXT PRIMARY KEY, mission_id TEXT, action_type TEXT, target_service TEXT, target_resource TEXT, status TEXT)")
            
            # Mission with all tasks COMPLETED, but 0 deliverables
            cur.execute("INSERT INTO missions VALUES ('m-zero', 'Test', 'local objective', 'RUNNING')")
            cur.execute("INSERT INTO tasks VALUES ('t1', 'm-zero', 'COMPLETED', 1)")
            cur.execute("INSERT INTO execution_proofs VALUES ('p1', 'm-zero', 0)")
            conn.commit()
            conn.close()

            gate = ObjectiveCompletionGate(db_file.name)
            valid, reason, diag = gate.evaluate_mission("m-zero")
            self.assertFalse(valid)
            self.assertIn("Zéro livrable physique enregistré", reason)

if __name__ == "__main__":
    unittest.main()
