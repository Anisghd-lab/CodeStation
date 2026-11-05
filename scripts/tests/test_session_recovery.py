#!/usr/bin/env python3
"""
Unit tests for CAMCS Session Recovery & Auto-Reconciliation Engine
Inspired by Festival continuation.go and obey bridge reconcile.
"""

import unittest
import sys
import os
import json
import time
import tempfile
import sqlite3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from camcs_engine import CamcsOrchestrationEngine, CamcsMission, CamcsAgent, DB_PATH
from camcs.dag_planner import DagTask
from camcs.state_machines import MissionState, TaskState

class TestSessionRecovery(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.test_db = os.path.join(self.temp_dir.name, "camcs_test.db")

        # Create schema in test_db
        conn = sqlite3.connect(self.test_db)
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS missions (
                id TEXT PRIMARY KEY, title TEXT, objective TEXT, governance_mode TEXT,
                status TEXT, progress INTEGER, orchestration_phase TEXT,
                created_at REAL, updated_at REAL, completed_at REAL, config_json TEXT
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT PRIMARY KEY, mission_id TEXT, agent_id TEXT, classification TEXT,
                title TEXT, description TEXT, prompt TEXT, dependencies_json TEXT,
                status TEXT, progress INTEGER, weight INTEGER, is_mandatory INTEGER,
                acceptance_criteria TEXT, expected_deliverable TEXT, assigned_file TEXT,
                exit_code INTEGER, result_summary TEXT, error_message TEXT,
                created_at REAL, started_at REAL, completed_at REAL
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS agents (
                id TEXT PRIMARY KEY, mission_id TEXT, name TEXT, role TEXT, shape TEXT,
                color TEXT, cli_id TEXT, provider_id TEXT, model_id TEXT, objective TEXT, status TEXT,
                progress INTEGER, current_subtask TEXT, pid INTEGER, started_at REAL, last_activity_at REAL,
                created_at REAL, updated_at REAL
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS mission_journal (
                id TEXT PRIMARY KEY, mission_id TEXT, event_type TEXT, message TEXT,
                payload_json TEXT, created_at REAL
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS quality_gates (
                id TEXT PRIMARY KEY, mission_id TEXT, task_id TEXT, gate_type TEXT,
                name TEXT, status TEXT, evaluator TEXT, details TEXT, output_summary TEXT,
                created_at REAL, evaluated_at REAL, duration_ms INTEGER
            )
        """)
        conn.commit()
        conn.close()

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_reconcile_interrupted_tasks(self):
        m_id = "mission-recon-1"
        mission_dir = os.path.join(self.temp_dir.name, "workspace")
        os.makedirs(mission_dir, exist_ok=True)

        # File for task 1 was written before crash
        t1_file = os.path.join(mission_dir, "t1_done.py")
        with open(t1_file, "w") as f:
            f.write("# Task 1 complete deliverable\n")

        # File for task 2 does NOT exist (interrupted mid-execution)
        t2_file = os.path.join(mission_dir, "t2_missing.py")

        conn = sqlite3.connect(self.test_db)
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO missions (id, title, objective, governance_mode, status, progress, orchestration_phase, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (m_id, "Interrupted Mission", "Test Objective", "HYBRID", MissionState.RUNNING, 30, "EXECUTION", time.time(), time.time()))

        cur.execute("""
            INSERT INTO agents (id, mission_id, name, role, shape, color, cli_id, provider_id, model_id, status, progress, current_subtask, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, ("ag-1", m_id, "DevAgent", "Codeur", "hexagon", "#00D2FF", "agy", "google", "gemini-flash", "RUNNING", 30, "Working", time.time(), time.time()))

        cur.execute("""
            INSERT INTO tasks (id, mission_id, agent_id, classification, title, description, prompt, dependencies_json, status, progress, weight, is_mandatory, assigned_file, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, ("t1", m_id, "ag-1", "CODE_GENERATION", "Task 1", "D1", "P1", "[]", "RUNNING", 50, 50, 1, t1_file, time.time()))

        cur.execute("""
            INSERT INTO tasks (id, mission_id, agent_id, classification, title, description, prompt, dependencies_json, status, progress, weight, is_mandatory, assigned_file, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, ("t2", m_id, "ag-1", "VERIFICATION", "Task 2", "D2", "P2", "[\"t1\"]", "RUNNING", 10, 50, 1, t2_file, time.time()))
        conn.commit()
        conn.close()

        # Instantiate engine pointing to test_db
        engine = CamcsOrchestrationEngine(self.test_db)

        reconciled = engine.reconcile_interrupted_missions(auto_resume=False)
        self.assertEqual(len(reconciled), 1)
        self.assertEqual(reconciled[0]["missionId"], m_id)

        # Check DB states
        conn = sqlite3.connect(self.test_db)
        cur = conn.cursor()
        cur.execute("SELECT status, progress FROM tasks WHERE id = 't1'")
        t1_status, t1_prog = cur.fetchone()
        self.assertEqual(t1_status, "COMPLETED")
        self.assertEqual(t1_prog, 100)

        cur.execute("SELECT status, progress FROM tasks WHERE id = 't2'")
        t2_status, t2_prog = cur.fetchone()
        self.assertEqual(t2_status, "READY")
        self.assertEqual(t2_prog, 0)

        # Check that reconciliation journal entry was made
        cur.execute("SELECT event_type, message FROM mission_journal WHERE mission_id = ?", (m_id,))
        journals = cur.fetchall()
        self.assertTrue(any("reconciliation" in j[0].lower() or "reprise" in j[1].lower() for j in journals))
        conn.close()

if __name__ == "__main__":
    unittest.main()
