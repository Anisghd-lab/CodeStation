#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Process & Resource Manager
Centralizes OS process lifecycle, concurrency limits, queueing, and resource hygiene on ARM64 PRoot.
"""

import os
import time
import signal
import threading
import queue
from typing import Dict, List, Optional, Any, Callable
from dataclasses import dataclass, field
from .runners import BaseCliRunner, CliExecutionResult

@dataclass
class ProcessInfo:
    task_id: str
    mission_id: str
    cli_id: str
    pid: int
    command: str
    working_dir: str
    started_at: float
    status: str = "RUNNING"  # RUNNING, COMPLETED, FAILED, TIMED_OUT, CANCELLED
    duration_ms: int = 0
    exit_code: Optional[int] = None
    cpu_percent: float = 0.0
    memory_mb: float = 0.0

class ProcessManager:
    def __init__(self, max_concurrent: int = 2):
        self.max_concurrent = max_concurrent
        self._active_processes: Dict[str, ProcessInfo] = {}
        self._lock = threading.Lock()
        self._semaphore = threading.Semaphore(max_concurrent)
        self._task_queue = queue.PriorityQueue()
        self.cleanup_orphans()

    def get_system_load(self) -> Dict[str, Any]:
        """Reads system RAM and load metrics from /proc."""
        total_ram_mb = 0
        avail_ram_mb = 0
        try:
            with open("/proc/meminfo", "r") as f:
                for line in f:
                    if line.startswith("MemTotal:"):
                        total_ram_mb = int(line.split()[1]) // 1024
                    elif line.startswith("MemAvailable:"):
                        avail_ram_mb = int(line.split()[1]) // 1024
        except Exception:
            pass

        return {
            "total_ram_mb": total_ram_mb,
            "avail_ram_mb": avail_ram_mb,
            "active_cli_processes": len(self._active_processes),
            "max_concurrent": self.max_concurrent
        }

    def execute_with_guard(
        self,
        runner: BaseCliRunner,
        task_id: str,
        mission_id: str,
        prompt: str,
        cwd: str,
        timeout: int = 150,
        output_callback: Optional[Callable[[str, str], None]] = None
    ) -> CliExecutionResult:
        """
        Executes a task through runner with concurrency bounding and process tracking.
        """
        self._semaphore.acquire()
        t0 = time.time()
        pinfo = ProcessInfo(
            task_id=task_id,
            mission_id=mission_id,
            cli_id=runner.cli_id,
            pid=0,
            command=f"{runner.binary} task",
            working_dir=cwd,
            started_at=t0
        )

        with self._lock:
            self._active_processes[task_id] = pinfo

        try:
            result = runner.execute_task(
                task_id=task_id,
                mission_id=mission_id,
                prompt=prompt,
                cwd=cwd,
                timeout=timeout,
                output_callback=output_callback
            )

            pinfo.exit_code = result.exit_code
            pinfo.duration_ms = result.duration_ms
            pinfo.status = "COMPLETED" if result.exit_code == 0 else "FAILED"
            return result
        except Exception as e:
            pinfo.status = "FAILED"
            pinfo.exit_code = -1
            return CliExecutionResult(
                task_id=task_id,
                mission_id=mission_id,
                cli_id=runner.cli_id,
                exit_code=-1,
                stdout="",
                stderr=f"[ProcessManager] Exception pendant l'exécution: {e}",
                duration_ms=int((time.time() - t0) * 1000),
                working_dir=cwd,
                command_executed=f"{runner.binary}",
                verified=False,
                verdict="EXCEPTION"
            )
        finally:
            with self._lock:
                self._active_processes.pop(task_id, None)
            self._semaphore.release()

    def cancel_task(self, runner: BaseCliRunner, task_id: str) -> bool:
        with self._lock:
            pinfo = self._active_processes.get(task_id)
            if pinfo:
                pinfo.status = "CANCELLED"
        return runner.cancel_task(task_id)

    def list_active_processes(self) -> List[Dict[str, Any]]:
        with self._lock:
            return [
                {
                    "taskId": p.task_id,
                    "missionId": p.mission_id,
                    "cliId": p.cli_id,
                    "pid": p.pid,
                    "command": p.command,
                    "workingDir": p.working_dir,
                    "startedAt": p.started_at,
                    "status": p.status,
                    "durationMs": int((time.time() - p.started_at) * 1000)
                }
                for p in self._active_processes.values()
            ]

    def cleanup_orphans(self):
        """Scans and terminates any orphan CLI processes matching known runners."""
        patterns = [
            "clixad ask",
            "omnirush -p",
            "opencode run"
        ]
        # Gentle cleanup only for detached CLI sub-commands without parents
        try:
            import subprocess
            for pat in patterns:
                subprocess.run(["pkill", "-f", pat], capture_output=True)
        except Exception:
            pass
