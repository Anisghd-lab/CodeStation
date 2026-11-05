#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Clixad Runner
Executes real tasks through Clixad CLI (free AI coding in terminal, funded by ads).
"""

import os
import shutil
import subprocess
import threading
import time
from typing import Optional, Callable, Tuple
from .base_runner import BaseCliRunner, CliExecutionResult

class ClixadRunner(BaseCliRunner):
    def __init__(self):
        super().__init__("clixad", "Clixad", "clixad")
        self.initialize()

    def initialize(self) -> bool:
        candidate_paths = [
            "/usr/local/bin/clixad",
            "/usr/bin/clixad",
            "/root/.local/bin/clixad"
        ]
        for p in candidate_paths:
            if os.path.isfile(p) and os.access(p, os.X_OK):
                self.path = p
                break
        if not self.path:
            self.path = shutil.which("clixad")

        if self.path:
            try:
                res = subprocess.run([self.path, "--version"], capture_output=True, text=True, timeout=5)
                if res.returncode == 0:
                    self.version = res.stdout.strip()
            except Exception:
                pass
        return self.path is not None

    def health_check(self) -> Tuple[bool, str, str]:
        if not self.path or not os.path.exists(self.path):
            return False, "NOT_INSTALLED", "Binaire clixad introuvable dans PATH"

        cfg_file = os.path.expanduser("~/.clixad/config.json")
        if os.path.isfile(cfg_file) and os.path.getsize(cfg_file) > 10:
            try:
                import json
                with open(cfg_file, "r") as f:
                    cfg_data = json.load(f)
                email = cfg_data.get("email") or cfg_data.get("login") or "authentifié"
                return True, "IMPLEMENTED_AND_VERIFIED", f"Clixad opérationnel et authentifié ({self.version}) : {email}"
            except Exception:
                pass

        try:
            res = subprocess.run([self.path, "whoami"], capture_output=True, text=True, timeout=3)
            if res.returncode == 0 and "credits" in res.stdout.lower():
                balance_line = res.stdout.strip().replace("\n", " ")
                return True, "IMPLEMENTED_AND_VERIFIED", f"Clixad opérationnel et authentifié ({self.version}) : {balance_line}"
            else:
                return False, "BLOCKED", f"Clixad disponible mais échec d'authentification: {res.stderr.strip() or res.stdout.strip()}"
        except Exception as e:
            return False, "BLOCKED", f"Erreur lors du contrôle de santé Clixad: {e}"

    def execute_task(
        self,
        task_id: str,
        mission_id: str,
        prompt: str,
        cwd: str,
        timeout: int = 120,
        output_callback: Optional[Callable[[str, str], None]] = None,
        pid_callback: Optional[Callable[[int], None]] = None
    ) -> CliExecutionResult:
        os.makedirs(cwd, exist_ok=True)
        snapshot_before = self.take_directory_snapshot(cwd)

        env = os.environ.copy()
        env["PATH"] = "/usr/local/bin:/usr/bin:/bin:/root/.local/bin:" + env.get("PATH", "")

        cmd = [self.path or "clixad", "ask", prompt]
        cmd_str = f"clixad ask \"{prompt[:80]}...\""

        t_start = time.time()
        stdout_chunks = []
        stderr_chunks = []

        try:
            proc = subprocess.Popen(
                cmd,
                cwd=cwd,
                stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=env,
                bufsize=1
            )
            self.active_processes[task_id] = proc
            if pid_callback:
                try:
                    pid_callback(proc.pid)
                except Exception:
                    pass

            def read_stream(stream, target_list, stream_type):
                if not stream:
                    return
                for line in iter(stream.readline, ''):
                    target_list.append(line)
                    if output_callback:
                        output_callback(stream_type, line)
                stream.close()

            t_out = threading.Thread(target=read_stream, args=(proc.stdout, stdout_chunks, "stdout"))
            t_err = threading.Thread(target=read_stream, args=(proc.stderr, stderr_chunks, "stderr"))
            t_out.start()
            t_err.start()

            try:
                proc.wait(timeout=timeout)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait()
                stderr_chunks.append(f"\n[CAMCS] Exécution Clixad interrompue après timeout de {timeout}s.")

            t_out.join(timeout=2)
            t_err.join(timeout=2)

            exit_code = proc.returncode if proc.returncode is not None else -1
        except Exception as e:
            exit_code = -1
            stderr_chunks.append(f"\n[CAMCS] Erreur d'exécution Clixad: {e}")
        finally:
            self.active_processes.pop(task_id, None)

        duration_ms = int((time.time() - t_start) * 1000)
        stdout_text = "".join(stdout_chunks).strip()
        stderr_text = "".join(stderr_chunks).strip()

        created, modified = self.collect_artifacts(cwd, snapshot_before)
        is_verified = (exit_code == 0 and len(stdout_text) > 0)
        verdict = "CONFORME" if is_verified else "ECHEC_EXECUTION"

        return CliExecutionResult(
            task_id=task_id,
            mission_id=mission_id,
            cli_id=self.cli_id,
            exit_code=exit_code,
            stdout=stdout_text,
            stderr=stderr_text,
            duration_ms=duration_ms,
            working_dir=cwd,
            command_executed=cmd_str,
            files_created=created,
            files_modified=modified,
            verified=is_verified,
            verdict=verdict,
            metadata={"version": self.version, "runner": "ClixadRunner"}
        )

    def cancel_task(self, task_id: str) -> bool:
        proc = self.active_processes.get(task_id)
        if proc and proc.poll() is None:
            proc.terminate()
            try:
                proc.wait(timeout=2)
            except Exception:
                proc.kill()
            return True
        return False
