#!/usr/bin/env python3
"""
Test suite for TASK-004 — Auto-Repair Loop & Human Escalation
Verifies iterative correction, context injection, threshold escalation, and operator approval/rejection.
"""

import os
import sys
import tempfile
import unittest
import time
from dataclasses import dataclass

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from camcs.auto_repair_loop import AutoRepairLoop, RepairAttempt
from camcs.state_machines import TaskState, MissionState


@dataclass
class MockTask:
    id: str = "task-repair-001"
    mission_id: str = "mission-mock-123"
    title: str = "Générer Repository Architecture"
    prompt: str = "Créer la classe Repository dans domain/"
    iteration_count: int = 0
    max_iterations: int = 3
    status: str = TaskState.READY
    last_error: str = None
    error_message: str = None
    escalation_reason: str = None


class TestAutoRepairLoop(unittest.TestCase):
    def setUp(self):
        self.tmp_dir = tempfile.mkdtemp()
        self.db_path = os.path.join(self.tmp_dir, "test_camcs.db")
        self.repair_loop = AutoRepairLoop(default_max_iterations=3, db_path=self.db_path)

    def tearDown(self):
        import shutil
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def test_format_error_context(self):
        ctx = self.repair_loop.format_error_context(
            task_title="Compile App",
            error_msg="Compilation failure",
            stderr="error: cannot find symbol Foo",
            exit_code=1,
            gate_failures=[{"name": "Security Gate", "type": "SECURITY", "details": "Token leak detected"}]
        )
        self.assertIn("Compile App", ctx)
        self.assertIn("Compilation failure", ctx)
        self.assertIn("error: cannot find symbol Foo", ctx)
        self.assertIn("Code de retour processus : 1", ctx)
        self.assertIn("Security Gate", ctx)

    def test_build_retry_prompt(self):
        ctx = "Error details"
        prompt = self.repair_loop.build_retry_prompt(
            base_prompt="Ecrire le code",
            iteration=2,
            max_iterations=3,
            error_context=ctx
        )
        self.assertIn("TENTATIVE 2/3", prompt)
        self.assertIn("Error details", prompt)
        self.assertIn("Ecrire le code", prompt)

    def test_repair_attempts_and_escalation(self):
        task = MockTask()

        # Attempt 1 -> can retry
        can_retry, prompt1 = self.repair_loop.attempt_repair(
            task=task,
            error_msg="Syntax error on line 42",
            stderr="line 42: invalid syntax",
            exit_code=1
        )
        self.assertTrue(can_retry)
        self.assertEqual(task.iteration_count, 1)
        self.assertIn("TENTATIVE 1/3", prompt1)
        self.assertEqual(task.last_error, "Syntax error on line 42")

        # Attempt 2 -> can retry
        can_retry, prompt2 = self.repair_loop.attempt_repair(
            task=task,
            error_msg="NullPointerException",
            stderr="java.lang.NullPointerException",
            exit_code=2
        )
        self.assertTrue(can_retry)
        self.assertEqual(task.iteration_count, 2)
        self.assertIn("TENTATIVE 2/3", prompt2)

        # Attempt 3 -> can retry (at max_iterations)
        can_retry, prompt3 = self.repair_loop.attempt_repair(
            task=task,
            error_msg="Import error",
            stderr="ImportError: module not found",
            exit_code=1
        )
        self.assertTrue(can_retry)
        self.assertEqual(task.iteration_count, 3)
        self.assertIn("TENTATIVE 3/3", prompt3)

        # Attempt 4 -> EXCEEDED -> must escalate!
        can_retry, escalation_msg = self.repair_loop.attempt_repair(
            task=task,
            error_msg="Crash persistently",
            stderr="Fatal error",
            exit_code=139
        )
        self.assertFalse(can_retry)
        self.assertEqual(task.iteration_count, 4)
        self.assertEqual(task.status, TaskState.NEEDS_APPROVAL)
        self.assertIn("SEUIL D'AUTO-RÉPARATION DÉPASSÉ", escalation_msg)
        self.assertIn("Escalade opérateur requise", escalation_msg)
        self.assertEqual(task.escalation_reason, escalation_msg)

    def test_human_escalation_approval(self):
        task = MockTask(iteration_count=4, max_iterations=3, status=TaskState.NEEDS_APPROVAL)
        task.escalation_reason = "Loop limit reached"

        # Operator approves with 2 extra attempts and refined prompt
        ok = self.repair_loop.resolve_escalation(
            task=task,
            approved=True,
            extra_attempts=2,
            override_prompt="Utiliser mock plutôt que dépendance réelle"
        )
        self.assertTrue(ok)
        self.assertEqual(task.status, TaskState.READY)
        self.assertEqual(task.max_iterations, 5)
        self.assertIsNone(task.escalation_reason)
        self.assertEqual(task.prompt, "Utiliser mock plutôt que dépendance réelle")

    def test_human_escalation_rejection(self):
        task = MockTask(iteration_count=4, max_iterations=3, status=TaskState.NEEDS_APPROVAL)

        # Operator rejects
        ok = self.repair_loop.resolve_escalation(task=task, approved=False)
        self.assertFalse(ok)
        self.assertEqual(task.status, TaskState.FAILED)
        self.assertIn("rejetée par l'opérateur", task.escalation_reason)

    def test_history_persistence_in_db(self):
        task = MockTask()
        self.repair_loop.attempt_repair(task, "Error 1")
        self.repair_loop.attempt_repair(task, "Error 2")
        self.repair_loop.attempt_repair(task, "Error 3")
        self.repair_loop.attempt_repair(task, "Error 4 (escalate)")
        self.repair_loop.resolve_escalation(task, approved=True, extra_attempts=1)

        history = self.repair_loop.get_task_repair_history(task.id)
        self.assertEqual(len(history), 5)
        self.assertEqual(history[0]["actionTaken"], "retry")
        self.assertEqual(history[1]["actionTaken"], "retry")
        self.assertEqual(history[2]["actionTaken"], "retry")
        self.assertEqual(history[3]["actionTaken"], "escalate")
        self.assertEqual(history[4]["actionTaken"], "approved")


if __name__ == "__main__":
    unittest.main()
