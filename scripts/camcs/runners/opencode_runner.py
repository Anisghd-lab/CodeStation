#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — OpenCode Runner
Executes real tasks through OpenCode CLI (opencode).
"""

import os
import shutil
import subprocess
import threading
import time
from typing import Optional, Callable, Tuple
from .base_runner import BaseCliRunner, CliExecutionResult

class OpenCodeRunner(BaseCliRunner):
    def __init__(self, default_model: str = "xkiro/qwen3-coder-plus"):
        super().__init__("opencode", "OpenCode", "opencode")
        self.default_model = default_model
        self.initialize()

    def initialize(self) -> bool:
        candidate_paths = [
            "/usr/local/bin/opencode",
            "/root/.opencode/bin/opencode",
            "/usr/bin/opencode"
        ]
        for p in candidate_paths:
            if os.path.isfile(p) and os.access(p, os.X_OK):
                self.path = p
                break
        if not self.path:
            self.path = shutil.which("opencode")

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
            return False, "NOT_INSTALLED", "Binaire opencode introuvable dans PATH"

        cfg_paths = ["/root/.config/opencode/opencode.json", "/root/.secrets/xkiro-key", "/root/.secrets/opencode-key"]
        has_cfg = any(os.path.exists(p) for p in cfg_paths)

        if not has_cfg:
            return False, "BLOCKED", "OpenCode non configuré (manque ~/.config/opencode ou secrets)"

        return True, "IMPLEMENTED_AND_VERIFIED", f"OpenCode opérationnel ({self.version}) avec modèle {self.default_model}"

    def execute_task(
        self,
        task_id: str,
        mission_id: str,
        prompt: str,
        cwd: str,
        timeout: int = 150,
        output_callback: Optional[Callable[[str, str], None]] = None,
        pid_callback: Optional[Callable[[int], None]] = None
    ) -> CliExecutionResult:
        os.makedirs(cwd, exist_ok=True)
        # Prevent OpenCode from traversing upwards to /root/.git by ensuring cwd is its own git root
        if not os.path.exists(os.path.join(cwd, ".git")):
            try:
                subprocess.run(["git", "init", "-q", cwd], check=False, timeout=5)
            except Exception:
                pass
        snapshot_before = self.take_directory_snapshot(cwd)

        env = os.environ.copy()
        env["PATH"] = "/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:" + env.get("PATH", "")

        cmd = [
            self.path or "opencode",
            "run",
            "--standalone",
            "--auto",
            "--format", "default",
            "-m", self.default_model,
            prompt
        ]
        cmd_str = f"opencode run -m {self.default_model} \"{prompt[:80]}...\""

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
                stderr_chunks.append(f"\n[CAMCS] Exécution OpenCode interrompue après timeout de {timeout}s.")

            t_out.join(timeout=2)
            t_err.join(timeout=2)

            exit_code = proc.returncode if proc.returncode is not None else -1
        except Exception as e:
            exit_code = -1
            stderr_chunks.append(f"\n[CAMCS] Erreur d'exécution OpenCode: {e}")
        finally:
            self.active_processes.pop(task_id, None)

        duration_ms = int((time.time() - t_start) * 1000)
        stdout_text = "".join(stdout_chunks).strip()
        stderr_text = "".join(stderr_chunks).strip()

        created, modified = self.collect_artifacts(cwd, snapshot_before)
        is_verified = (exit_code == 0 and (len(stdout_text) > 0 or len(created) > 0))
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
            metadata={"version": self.version, "model": self.default_model, "runner": "OpenCodeRunner"}
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
