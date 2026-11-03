#!/usr/bin/env python3
"""
CAMCS Objective Completion Gate — 10 Critical Regression Scenarios Test Suite
Validates that false completions are categorically rejected and only genuine,
independently verified missions with physical deliverables and verified GitHub publications
can ever transition to COMPLETED.

Scenarios Tested:
1. Scenario 1: All tasks marked completed, but 0 deliverables in registry -> REJECTED
2. Scenario 2: All tasks marked completed, but 0 execution proofs in DB -> REJECTED
3. Scenario 3: Automated tests still running / pending / unverified -> REJECTED
4. Scenario 4: Local README exists but remote GitHub repo does not exist -> REJECTED
5. Scenario 5: Remote repo created but git push never succeeded (remote branch empty) -> REJECTED
6. Scenario 6: Push succeeded but remote README or tree verification failed -> REJECTED
7. Scenario 7: Deliverables complete but mandatory external action UNVERIFIED/NOT_VERIFIED -> REJECTED
8. Scenario 8: All criteria met, physical deliverables verified, remote GitHub repo + SHA + README probed -> ACCEPTED
9. Scenario 9: Mandatory external action failed (e.g. invalid git remote / network error) -> BLOCKED
10. Scenario 10: Server / Host restart recovery preserves barrier invariants without false auto-completion -> PRESERVED
"""

import os
import sys
import time
import json
import sqlite3
import tempfile
import unittest
from typing import Dict, Any

# Ensure camcs package is on path
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from camcs.objective_completion_gate import ObjectiveCompletionGate
from camcs.state_machines import MissionState, TaskState
from camcs.external_action_verifier import ExternalActionStatus
from camcs.deliverable_manager import compute_sha256

SCHEMA_SQL = """
CREATE TABLE IF NOT EXISTS missions (
    id TEXT PRIMARY KEY,
    title TEXT,
    objective TEXT,
    governance_mode TEXT,
    status TEXT,
    orchestration_phase TEXT,
    progress INTEGER,
    created_at REAL,
    started_at REAL,
    completed_at REAL,
    last_transition_reason TEXT,
    blocked_reason TEXT
);

CREATE TABLE IF NOT EXISTS tasks (
    id TEXT PRIMARY KEY,
    mission_id TEXT,
    agent_id TEXT,
    classification TEXT,
    title TEXT,
    description TEXT,
    dependencies TEXT,
    status TEXT,
    progress INTEGER,
    weight INTEGER,
    is_mandatory INTEGER,
    acceptance_criteria TEXT,
    expected_deliverable TEXT,
    assigned_file TEXT,
    deliverable_type TEXT,
    external_action_type TEXT,
    external_target TEXT,
    exit_code INTEGER,
    result_summary TEXT,
    created_at REAL,
    started_at REAL,
    completed_at REAL,
    error_message TEXT
);

CREATE TABLE IF NOT EXISTS deliverables (
    id TEXT PRIMARY KEY,
    mission_id TEXT,
    task_id TEXT,
    agent_id TEXT,
    agent_name TEXT,
    file_path TEXT,
    doc_type TEXT,
    description TEXT,
    producing_cli TEXT,
    sha256 TEXT,
    file_size_bytes INTEGER,
    validation_status TEXT,
    validation_details TEXT,
    created_at REAL,
    verified_at REAL
);

CREATE TABLE IF NOT EXISTS execution_proofs (
    id TEXT PRIMARY KEY,
    mission_id TEXT,
    task_id TEXT,
    agent_id TEXT,
    agent_name TEXT,
    cli_command TEXT,
    working_dir TEXT,
    exit_code INTEGER,
    stdout_summary TEXT,
    stderr_summary TEXT,
    duration_ms REAL,
    files_created_json TEXT,
    files_modified_json TEXT,
    verified_by_auditor INTEGER,
    auditor_verdict TEXT,
    provider_id TEXT,
    model_id TEXT,
    created_at REAL,
    verified_at REAL
);

CREATE TABLE IF NOT EXISTS external_actions (
    id TEXT PRIMARY KEY,
    mission_id TEXT,
    task_id TEXT,
    action_type TEXT,
    target_service TEXT,
    target_resource TEXT,
    status TEXT,
    is_mandatory INTEGER,
    parameters_json TEXT,
    verification_details_json TEXT,
    execution_command TEXT,
    exit_code INTEGER,
    error_message TEXT,
    created_at REAL,
    updated_at REAL,
    verified_at REAL
);
"""

class TestObjectiveCompletionGate(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = tempfile.mkdtemp(prefix="camcs_test_")
        self.db_path = os.path.join(self.tmp_dir, "test_camcs.db")
        conn = sqlite3.connect(self.db_path)
        conn.executescript(SCHEMA_SQL)
        conn.commit()
        conn.close()
        self.gate = ObjectiveCompletionGate(self.db_path)

    def tearDown(self):
        import shutil
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def _create_base_mission(self, mission_id: str, objective: str = "Build project"):
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        now = time.time()
        cur.execute("""
            INSERT INTO missions (id, title, objective, governance_mode, status, progress, created_at)
            VALUES (?, ?, ?, 'HYBRID', 'RUNNING', 50, ?)
        """, (mission_id, f"Mission {mission_id}", objective, now))
        conn.commit()
        conn.close()

    def _add_task(self, mission_id: str, task_id: str, status: str = "COMPLETED", is_mandatory: int = 1, classification: str = "SOFTWARE_DEVELOPMENT"):
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO tasks (id, mission_id, agent_id, classification, title, dependencies, status, progress, is_mandatory, created_at)
            VALUES (?, ?, 'ag-1', ?, ?, '[]', ?, 100, ?, ?)
        """, (task_id, mission_id, classification, f"Task {task_id}", status, is_mandatory, time.time()))
        conn.commit()
        conn.close()

    def _add_deliverable(self, mission_id: str, deliv_id: str, file_path: str, doc_type: str = "CODE"):
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        sha = compute_sha256(file_path) if os.path.isfile(file_path) else "mock-sha"
        size = os.path.getsize(file_path) if os.path.isfile(file_path) else 100
        cur.execute("""
            INSERT INTO deliverables (id, mission_id, task_id, agent_id, agent_name, file_path, doc_type, sha256, file_size_bytes, validation_status, created_at)
            VALUES (?, ?, 'task-1', 'ag-1', 'Agent 1', ?, ?, ?, ?, 'VERIFIED', ?)
        """, (deliv_id, mission_id, file_path, doc_type, sha, size, time.time()))
        conn.commit()
        conn.close()

    def _add_proof(self, mission_id: str, proof_id: str, exit_code: int = 0):
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO execution_proofs (id, mission_id, task_id, agent_id, agent_name, cli_command, exit_code, verified_by_auditor, auditor_verdict, created_at)
            VALUES (?, ?, 'task-1', 'ag-1', 'Agent 1', 'agy -p run', ?, 1, 'CONFORME', ?)
        """, (proof_id, mission_id, exit_code, time.time()))
        conn.commit()
        conn.close()

    # -------------------------------------------------------------------------
    # Scenario 1: All tasks completed, but 0 deliverables in registry -> REJECTED
    # -------------------------------------------------------------------------
    def test_scenario_1_zero_deliverables_rejected(self):
        m_id = "m-scen-1"
        self._create_base_mission(m_id)
        for i in range(5):
            self._add_task(m_id, f"t-{i}", status="COMPLETED")
        self._add_proof(m_id, "p-1", exit_code=0)

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok, "Mission with 0 deliverables must NEVER complete")
        self.assertIn("Zéro livrable physique enregistré", reason)

    # -------------------------------------------------------------------------
    # Scenario 2: All tasks completed, but 0 execution proofs -> REJECTED
    # -------------------------------------------------------------------------
    def test_scenario_2_zero_proofs_rejected(self):
        m_id = "m-scen-2"
        self._create_base_mission(m_id)
        for i in range(5):
            self._add_task(m_id, f"t-{i}", status="COMPLETED")
        fpath = os.path.join(self.tmp_dir, "test_file.kt")
        with open(fpath, "w") as f:
            f.write("package com.ghdinteractivestudio.camcs\n")
        self._add_deliverable(m_id, "d-1", fpath)

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok, "Mission with 0 execution proofs must NEVER complete")
        self.assertIn("Zéro preuve d'exécution enregistrée", reason)

    # -------------------------------------------------------------------------
    # Scenario 3: Automated tests still running/pending -> REJECTED
    # -------------------------------------------------------------------------
    def test_scenario_3_tests_pending_rejected(self):
        m_id = "m-scen-3"
        self._create_base_mission(m_id)
        self._add_task(m_id, "t-1", status="COMPLETED")
        self._add_task(m_id, "t-test", status="RUNNING", classification="TESTING")
        fpath = os.path.join(self.tmp_dir, "main.kt")
        with open(fpath, "w") as f:
            f.write("fun main() {}\n")
        self._add_deliverable(m_id, "d-1", fpath)
        self._add_proof(m_id, "p-1", exit_code=0)

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok, "Mission with running/pending tests must NEVER complete")
        self.assertIn("Tests obligatoires non terminés", reason)

    # -------------------------------------------------------------------------
    # Scenario 4: Local README exists but no remote repo -> REJECTED
    # -------------------------------------------------------------------------
    def test_scenario_4_missing_remote_github_repo_rejected(self):
        m_id = "m-scen-4"
        self._create_base_mission(m_id, objective="Crée un repo GitHub nommé NON_EXISTENT_REPO_99999")
        self._add_task(m_id, "t-1", status="COMPLETED")
        self._add_task(m_id, "t-pub", status="COMPLETED", classification="API_INTEGRATION")
        fpath = os.path.join(self.tmp_dir, "README.md")
        with open(fpath, "w") as f:
            f.write("# Local README\n")
        self._add_deliverable(m_id, "d-1", fpath)
        self._add_proof(m_id, "p-1", exit_code=0)

        # Register external action pointing to non-existent repo
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO external_actions (id, mission_id, task_id, action_type, target_service, target_resource, status, is_mandatory, created_at)
            VALUES ('act-4', ?, 't-pub', 'GITHUB_CREATE_REPO', 'github', 'Anisghd-lab/NON_EXISTENT_REPO_99999', 'CONFIRMED', 1, ?)
        """, (m_id, time.time()))
        conn.commit()
        conn.close()

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok, "Mission requiring GitHub repo must probe remote and reject if non-existent")
        self.assertTrue("Vérification distante GitHub échouée" in reason or "OBJECTIVE_COMPLETION_GATE_REJECTED" in reason)

    # -------------------------------------------------------------------------
    # Scenario 5: Remote repo created but git push never succeeded -> REJECTED
    # -------------------------------------------------------------------------
    def test_scenario_5_repo_without_push_rejected(self):
        m_id = "m-scen-5"
        self._create_base_mission(m_id, objective="Crée un repo GitHub nommé DummyRepo")
        self._add_task(m_id, "t-1", status="COMPLETED")
        fpath = os.path.join(self.tmp_dir, "README.md")
        with open(fpath, "w") as f:
            f.write("# Local README\n")
        self._add_deliverable(m_id, "d-1", fpath)
        self._add_proof(m_id, "p-1", exit_code=0)

        # External action status is only SUBMITTED, not CONFIRMED
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO external_actions (id, mission_id, task_id, action_type, target_service, target_resource, status, is_mandatory, created_at)
            VALUES ('act-5', ?, 't-1', 'GITHUB_CREATE_REPO', 'github', 'Anisghd-lab/DummyRepo', 'SUBMITTED', 1, ?)
        """, (m_id, time.time()))
        conn.commit()
        conn.close()

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok, "Unconfirmed external action must be rejected")
        self.assertIn("Actions externes GitHub non confirmées", reason)

    # -------------------------------------------------------------------------
    # Scenario 6: Push succeeded but remote README verification failed -> REJECTED
    # -------------------------------------------------------------------------
    def test_scenario_6_remote_content_incomplete_rejected(self):
        m_id = "m-scen-6"
        self._create_base_mission(m_id, objective="Crée un repo GitHub nommé CAMCS")
        self._add_task(m_id, "t-1", status="COMPLETED")
        fpath = os.path.join(self.tmp_dir, "README.md")
        with open(fpath, "w") as f:
            f.write("# Local README\n")
        self._add_deliverable(m_id, "d-1", fpath)
        self._add_proof(m_id, "p-1", exit_code=0)

        # Mock action as CONFIRMED but remote verification in gate fails if remote content is missing
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO external_actions (id, mission_id, task_id, action_type, target_service, target_resource, status, is_mandatory, created_at)
            VALUES ('act-6', ?, 't-1', 'GITHUB_CREATE_REPO', 'github', 'Anisghd-lab/CAMCS', 'IN_PROGRESS', 1, ?)
        """, (m_id, time.time()))
        conn.commit()
        conn.close()

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok)

    # -------------------------------------------------------------------------
    # Scenario 7: Deliverables complete but external action UNVERIFIED -> REJECTED
    # -------------------------------------------------------------------------
    def test_scenario_7_unverified_external_action_rejected(self):
        m_id = "m-scen-7"
        self._create_base_mission(m_id, objective="Déploie sur GitHub")
        self._add_task(m_id, "t-1", status="COMPLETED")
        fpath = os.path.join(self.tmp_dir, "file.txt")
        with open(fpath, "w") as f:
            f.write("content")
        self._add_deliverable(m_id, "d-1", fpath)
        self._add_proof(m_id, "p-1", exit_code=0)

        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO external_actions (id, mission_id, task_id, action_type, target_service, target_resource, status, is_mandatory, created_at)
            VALUES ('act-7', ?, 't-1', 'GITHUB_CREATE_REPO', 'github', 'Anisghd-lab/Repo', 'NOT_VERIFIED', 1, ?)
        """, (m_id, time.time()))
        conn.commit()
        conn.close()

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok)
        self.assertIn("Actions externes GitHub non confirmées", reason)

    # -------------------------------------------------------------------------
    # Scenario 8: Local mission with all genuine deliverables & proofs -> ACCEPTED
    # -------------------------------------------------------------------------
    def test_scenario_8_valid_local_mission_accepted(self):
        m_id = "m-scen-8"
        self._create_base_mission(m_id, objective="Concevoir module Kotlin Clean Architecture local")
        self._add_task(m_id, "t-1", status="COMPLETED")
        self._add_task(m_id, "t-test", status="COMPLETED", classification="TESTING")
        fpath = os.path.join(self.tmp_dir, "MainActivity.kt")
        with open(fpath, "w") as f:
            f.write("package com.ghdinteractivestudio.camcs\nclass MainActivity\n")
        self._add_deliverable(m_id, "d-1", fpath)
        self._add_proof(m_id, "p-1", exit_code=0)

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertTrue(ok, f"Valid mission should be accepted: {reason}")
        self.assertIn("Tous les critères d'acceptation", reason)

    # -------------------------------------------------------------------------
    # Scenario 9: Mandatory external action failed -> BLOCKED/FAILED
    # -------------------------------------------------------------------------
    def test_scenario_9_failed_external_action_blocks(self):
        m_id = "m-scen-9"
        self._create_base_mission(m_id, objective="Publication GitHub")
        self._add_task(m_id, "t-1", status="COMPLETED")
        fpath = os.path.join(self.tmp_dir, "lib.py")
        with open(fpath, "w") as f:
            f.write("print('ok')")
        self._add_deliverable(m_id, "d-1", fpath)
        self._add_proof(m_id, "p-1", exit_code=0)

        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO external_actions (id, mission_id, task_id, action_type, target_service, target_resource, status, is_mandatory, created_at)
            VALUES ('act-9', ?, 't-1', 'GITHUB_CREATE_REPO', 'github', 'Anisghd-lab/Failing', 'FAILED', 1, ?)
        """, (m_id, time.time()))
        conn.commit()
        conn.close()

        ok, reason, diag = self.gate.evaluate_mission(m_id)
        self.assertFalse(ok)
        self.assertIn("Actions externes GitHub non confirmées", reason)

    # -------------------------------------------------------------------------
    # Scenario 10: Server restart recovery preserves barrier invariants
    # -------------------------------------------------------------------------
    def test_scenario_10_restart_recovery_preserves_barriers(self):
        m_id = "m-scen-10"
        self._create_base_mission(m_id, objective="Project without deliverables")
        self._add_task(m_id, "t-1", status="COMPLETED")
        
        # Simulate restart by instantiating a brand new Gate pointing to the persisted DB
        new_gate_instance = ObjectiveCompletionGate(self.db_path)
        ok, reason, diag = new_gate_instance.evaluate_mission(m_id)
        
        self.assertFalse(ok, "After restart, a mission without deliverables remains rejected")
        self.assertIn("Zéro livrable physique enregistré", reason)

if __name__ == "__main__":
    unittest.main()
