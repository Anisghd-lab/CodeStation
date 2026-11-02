#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Process & Resource Manager
Centralizes OS process lifecycle, concurrency limits, queueing, and resource hygiene on ARM64 PRoot.
Tracks real OS PIDs, live activity, cancellation, and stuck process detection.
"""

import os
import time
import signal
import subprocess
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
    status: str = "RUNNING"  # RUNNING, STREAMING, COMPLETED, FAILED, TIMED_OUT, CANCELLED, BLOCKED
    duration_ms: int = 0
    exit_code: Optional[int] = None
    cpu_percent: float = 0.0
    memory_mb: float = 0.0
    last_activity_at: float = field(default_factory=time.time)
    last_output: str = ""
    runner: Optional[BaseCliRunner] = None

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

        with self._lock:
            active_count = len(self._active_processes)

        return {
            "total_ram_mb": total_ram_mb,
            "avail_ram_mb": avail_ram_mb,
            "active_cli_processes": active_count,
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
        output_callback: Optional[Callable[[str, str], None]] = None,
        pid_callback: Optional[Callable[[int], None]] = None
    ) -> CliExecutionResult:
        """
        Executes a task through runner with concurrency bounding and process tracking.
        Captures real OS PID and streaming activity.
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
            started_at=t0,
            last_activity_at=t0,
            runner=runner
        )

        with self._lock:
            self._active_processes[task_id] = pinfo

        def internal_pid_cb(pid: int):
            pinfo.pid = pid
            if pid_callback:
                try:
                    pid_callback(pid)
                except Exception:
                    pass

        def internal_output_cb(stream_type: str, line: str):
            pinfo.last_activity_at = time.time()
            clean_line = line.strip()
            if clean_line:
                pinfo.last_output = clean_line[:200]
                pinfo.status = "STREAMING"
            if output_callback:
                try:
                    output_callback(stream_type, line)
                except Exception:
                    pass

        try:
            result = runner.execute_task(
                task_id=task_id,
                mission_id=mission_id,
                prompt=prompt,
                cwd=cwd,
                timeout=timeout,
                output_callback=internal_output_cb,
                pid_callback=internal_pid_cb
            )

            with self._lock:
                pinfo.exit_code = result.exit_code
                pinfo.duration_ms = result.duration_ms
                if pinfo.status != "CANCELLED":
                    pinfo.status = "COMPLETED" if result.exit_code == 0 else "FAILED"
            return result
        except Exception as e:
            with self._lock:
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

    def cancel_task(self, task_id: str) -> bool:
        """
        Immediately stops and terminates a running task and its associated OS process tree.
        """
        with self._lock:
            pinfo = self._active_processes.get(task_id)

        if not pinfo:
            return False

        pinfo.status = "CANCELLED"
        cancelled = False

        # 1. Ask runner to cancel
        if pinfo.runner:
            try:
                cancelled = pinfo.runner.cancel_task(task_id)
            except Exception:
                pass

        # 2. Kill by OS PID and process group if still active
        if pinfo.pid > 0:
            try:
                # Terminate children first
                subprocess.run(["pkill", "-TERM", "-P", str(pinfo.pid)], capture_output=True, timeout=2)
                os.kill(pinfo.pid, signal.SIGTERM)
                time.sleep(0.15)
                # Force kill if still responding
                try:
                    os.kill(pinfo.pid, 0)
                    subprocess.run(["pkill", "-KILL", "-P", str(pinfo.pid)], capture_output=True, timeout=2)
                    os.kill(pinfo.pid, signal.SIGKILL)
                except OSError:
                    pass
                cancelled = True
            except Exception:
                pass

        return cancelled

    def cancel_mission_tasks(self, mission_id: str) -> int:
        """
        Cancels all active tasks associated with a given mission.
        """
        with self._lock:
            target_task_ids = [t_id for t_id, p in self._active_processes.items() if p.mission_id == mission_id]

        count = 0
        for t_id in target_task_ids:
            if self.cancel_task(t_id):
                count += 1
        return count

    def get_task_process_info(self, task_id: str) -> Optional[Dict[str, Any]]:
        with self._lock:
            p = self._active_processes.get(task_id)
            if not p:
                return None
            return {
                "taskId": p.task_id,
                "missionId": p.mission_id,
                "cliId": p.cli_id,
                "pid": p.pid,
                "command": p.command,
                "workingDir": p.working_dir,
                "startedAt": p.started_at,
                "status": p.status,
                "lastActivityAt": p.last_activity_at,
                "lastOutput": p.last_output,
                "durationMs": int((time.time() - p.started_at) * 1000)
            }

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
                    "lastActivityAt": p.last_activity_at,
                    "lastOutput": p.last_output,
                    "durationMs": int((time.time() - p.started_at) * 1000)
                }
                for p in self._active_processes.values()
            ]

    def check_stuck_processes(self, timeout_sec: int = 180) -> List[Dict[str, Any]]:
        """
        Audits active processes for stuck or dead states.
        Differentiates between silent execution and hung/dead processes.
        """
        now = time.time()
        stuck_reports = []
        with self._lock:
            for t_id, p in list(self._active_processes.items()):
                inactive_sec = now - p.last_activity_at
                if inactive_sec > timeout_sec:
                    is_alive = False
                    if p.pid > 0:
                        try:
                            os.kill(p.pid, 0)
                            is_alive = True
                        except OSError:
                            is_alive = False
                    stuck_reports.append({
                        "taskId": t_id,
                        "missionId": p.mission_id,
                        "cliId": p.cli_id,
                        "pid": p.pid,
                        "inactiveSeconds": int(inactive_sec),
                        "isAlive": is_alive,
                        "status": "BLOCKED" if is_alive else "FAILED"
                    })
        return stuck_reports

    def cleanup_orphans(self):
        """Scans and terminates any orphan CLI processes matching known runners."""
        patterns = [
            "clixad ask",
            "omnirush -p",
            "opencode run"
        ]
        try:
            for pat in patterns:
                subprocess.run(["pkill", "-f", pat], capture_output=True)
        except Exception:
            pass
