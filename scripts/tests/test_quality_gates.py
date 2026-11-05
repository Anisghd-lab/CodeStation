#!/usr/bin/env python3
"""
Unit tests for CAMCS Quality Gates Engine
Inspired by Festival fest/internal/gates/ and fest/internal/loop/.
"""

import unittest
import sys
import os
import tempfile
import sqlite3

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from camcs.quality_gates import QualityGateEngine, QualityGate, GateType, GateStatus

class TestQualityGates(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = os.path.join(self.temp_dir.name, "test_camcs.db")
        self.engine = QualityGateEngine(self.db_path)

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_testing_gate_success(self):
        gate = self.engine.evaluate_command_gate(
            mission_id="m1",
            task_id="t1",
            gate_name="Unit Tests Pass",
            command="python3 -c 'import sys; sys.exit(0)'",
            cwd=self.temp_dir.name
        )
        self.assertEqual(gate.status, GateStatus.PASSED)
        self.assertEqual(gate.gate_type, GateType.TESTING)
        self.assertTrue(gate.duration_ms >= 0)

    def test_testing_gate_failure(self):
        gate = self.engine.evaluate_command_gate(
            mission_id="m1",
            task_id="t1",
            gate_name="Failing Test Gate",
            command="python3 -c 'import sys; sys.stderr.write(\"AssertionError\"); sys.exit(1)'",
            cwd=self.temp_dir.name
        )
        self.assertEqual(gate.status, GateStatus.FAILED)
        self.assertIn("AssertionError", gate.output_summary)

    def test_security_gate_clean(self):
        clean_file = os.path.join(self.temp_dir.name, "clean.py")
        with open(clean_file, "w") as f:
            f.write("# Clean code without any secrets\ndef hello(): return 'world'\n")

        gate = self.engine.evaluate_security_gate(
            mission_id="m1",
            task_id="t1",
            file_paths=[clean_file]
        )
        self.assertEqual(gate.status, GateStatus.PASSED)
        self.assertEqual(gate.gate_type, GateType.SECURITY)

    def test_security_gate_secret_detected(self):
        leak_file = os.path.join(self.temp_dir.name, "leak.py")
        with open(leak_file, "w") as f:
            f.write("GITHUB_TOKEN = 'ghp_ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890'\n")

        gate = self.engine.evaluate_security_gate(
            mission_id="m1",
            task_id="t1",
            file_paths=[leak_file]
        )
        self.assertEqual(gate.status, GateStatus.FAILED)
        self.assertIn("ghp_", gate.details)

    def test_deliverable_integrity_gate(self):
        fpath = os.path.join(self.temp_dir.name, "deliverable.txt")
        with open(fpath, "w") as f:
            f.write("Important verified deliverable\n")

        gate = self.engine.evaluate_deliverable_gate(
            mission_id="m1",
            task_id="t1",
            file_path=fpath
        )
        self.assertEqual(gate.status, GateStatus.PASSED)
        self.assertEqual(gate.gate_type, GateType.DELIVERABLES)

    def test_persistence_and_query(self):
        g1 = self.engine.evaluate_command_gate(
            mission_id="m_query",
            task_id="t_query",
            gate_name="Query Test Gate",
            command="python3 -c 'print(\"ok\")'",
            cwd=self.temp_dir.name
        )
        gates = self.engine.get_gates_for_mission("m_query")
        self.assertEqual(len(gates), 1)
        self.assertEqual(gates[0].id, g1.id)
        self.assertEqual(gates[0].status, GateStatus.PASSED)

if __name__ == "__main__":
    unittest.main()
