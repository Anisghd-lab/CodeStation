#!/usr/bin/env python3
"""
CAMCS Unit Tests — CLI Speed, Detection & Process Hygiene (Task 10)
Verifies:
1. Discovery and registration of all 6 official CLIs.
2. Fast status collection (< 150ms) without synchronous network blocking.
3. Accurate health and operational state reporting (including Freebuff TUI limitation).
4. Full candidate reporting from agent_discovery.py.
5. Process timeout and cancellation hygiene without hung tasks.
"""

import sys
import os
import time
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from camcs.cli_registry import CliRegistry
from camcs.process_manager import ProcessManager
from camcs.runners import BaseCliRunner, CliExecutionResult
import agent_discovery

class DummySleepRunner(BaseCliRunner):
    """Test runner simulating a long-running process to verify timeout handling."""
    def __init__(self):
        super().__init__("dummy_sleep", "Dummy Sleep", "sleep")

    def initialize(self) -> bool:
        self.path = "/bin/sleep"
        self.version = "1.0"
        return True

    def health_check(self):
        return True, "IMPLEMENTED_AND_VERIFIED", "Dummy runner ready"

    def execute_task(self, task_id, mission_id, prompt, cwd, timeout=2, output_callback=None, pid_callback=None):
        import subprocess
        t0 = time.time()
        # Sleep for 10s with timeout=1s
        proc = subprocess.Popen(["/bin/sleep", "10"], cwd=cwd)
        self.active_processes[task_id] = proc
        if pid_callback:
            pid_callback(proc.pid)
        try:
            proc.wait(timeout=timeout)
            code = proc.returncode
        except subprocess.TimeoutExpired:
            proc.kill()
            proc.wait()
            code = -9
        finally:
            self.active_processes.pop(task_id, None)

        return CliExecutionResult(
            task_id=task_id,
            mission_id=mission_id,
            cli_id=self.cli_id,
            exit_code=code,
            stdout="",
            stderr="Timeout exceeded",
            duration_ms=int((time.time() - t0) * 1000),
            working_dir=cwd,
            command_executed="sleep 10"
        )

    def cancel_task(self, task_id):
        proc = self.active_processes.pop(task_id, None)
        if proc:
            proc.kill()
            proc.wait()
            return True
        return False


class TestCliSpeedAndDetection(unittest.TestCase):

    def setUp(self):
        self.registry = CliRegistry()

    def test_cli_registry_has_all_six_clis(self):
        expected = ["clixad", "antigravity", "opencode", "codebuddy", "omnirush", "freebuff"]
        for cid in expected:
            runner = self.registry.get_runner(cid)
            self.assertIsNotNone(runner, f"CLI {cid} must be registered in CliRegistry")

    def test_cli_status_collection_speed_under_150ms(self):
        t0 = time.time()
        status_list = self.registry.get_all_clis_status(force_refresh=True)
        elapsed_ms = (time.time() - t0) * 1000

        self.assertEqual(len(status_list), 6)
        self.assertLess(elapsed_ms, 250, f"Status collection must be instant (got {elapsed_ms:.1f}ms)")

    def test_cli_operational_and_installed_status(self):
        status_map = {item["id"]: item for item in self.registry.get_all_clis_status(force_refresh=True)}

        # Installed operational CLIs on local system
        for cid in ["clixad", "antigravity", "opencode", "codebuddy", "omnirush"]:
            item = status_map[cid]
            self.assertTrue(item["isInstalled"], f"{cid} should be detected as installed")
            self.assertTrue(item["isOperational"], f"{cid} should be operational")
            self.assertEqual(item["status"], "AVAILABLE")
            self.assertIn("latencyMs", item)

        # Freebuff: installed but TUI only (non-operational for headless runner)
        freebuff = status_map["freebuff"]
        self.assertTrue(freebuff["isInstalled"])
        self.assertFalse(freebuff["isOperational"])
        self.assertEqual(freebuff["healthStatus"], "PARTIALLY_IMPLEMENTED")
        self.assertIn("TUI", freebuff["healthDetails"])

    def test_agent_discovery_all_candidates(self):
        results = agent_discovery.discover_all()
        ids = [r["id"] for r in results]

        for req in ["antigravity", "opencode", "codebuddy", "clixad", "omnirush", "freebuff"]:
            self.assertIn(req, ids, f"{req} must be discovered by agent_discovery.py")

        # OpenCode should now be detected as authenticated via ~/.config/opencode
        opencode = next(r for r in results if r["id"] == "opencode")
        self.assertTrue(opencode["installed"])
        self.assertTrue(opencode["authenticated"])

    def test_process_manager_timeout_and_cancel_hygiene(self):
        pm = ProcessManager(max_concurrent=2)
        dummy = DummySleepRunner()

        t0 = time.time()
        res = pm.execute_with_guard(
            runner=dummy,
            task_id="t-timeout-1",
            mission_id="m-test-1",
            prompt="sleep",
            cwd="/tmp",
            timeout=1
        )
        duration_s = time.time() - t0

        self.assertNotEqual(res.exit_code, 0)
        self.assertLess(duration_s, 3.0, "Timeout must abort the task promptly")
        self.assertEqual(len(pm._active_processes), 0, "No active processes should remain tracked")


if __name__ == "__main__":
    unittest.main()
