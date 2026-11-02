#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Freebuff Runner
Interfaces with Freebuff CLI (freebuff).
Empirically handles the upstream TUI-only limitation of v0.2.35.
"""

import os
import shutil
import subprocess
import time
from typing import Optional, Callable, Tuple
from .base_runner import BaseCliRunner, CliExecutionResult

class FreebuffRunner(BaseCliRunner):
    def __init__(self):
        super().__init__("freebuff", "Freebuff", "freebuff")
        self.native_binary_path = "/root/.config/manicode/freebuff"
        self.initialize()

    def initialize(self) -> bool:
        self.path = shutil.which("freebuff") or "/usr/local/bin/freebuff"
        if not os.path.isfile(self.path):
            self.path = None

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
            return False, "NOT_INSTALLED", "Binaire freebuff introuvable"

        # Check native binary & credentials in ~/.config/manicode
        has_native = os.path.isfile(self.native_binary_path) and os.access(self.native_binary_path, os.X_OK)
        cred_path = "/root/.config/manicode/credentials.json"
        has_creds = os.path.isfile(cred_path) and os.path.getsize(cred_path) > 0

        if not has_native or not has_creds:
            return False, "BLOCKED", "Freebuff non configuré ou binaire natif manquant dans ~/.config/manicode"

        # Upstream limitation: Freebuff v0.2.35 only supports interactive TUI mode (no -p flag)
        return False, "PARTIALLY_IMPLEMENTED", (
            f"Freebuff v{self.version} installé et authentifié, mais limité au mode TUI interactif (curses). "
            "Exécution non-interactive en attente de support upstream."
        )

    def execute_task(
        self,
        task_id: str,
        mission_id: str,
        prompt: str,
        cwd: str,
        timeout: int = 30,
        output_callback: Optional[Callable[[str, str], None]] = None
    ) -> CliExecutionResult:
        # Freebuff v0.2.35 lacks non-interactive execution flag
        msg = (
            f"Freebuff CLI (v{self.version}) est installé et authentifié dans Ubuntu PRoot.\n"
            "Limite technique observée : La version v0.2.35 s'exécute exclusivement en mode console interactif (TUI).\n"
            "Le routeur CAMCS active le repli automatique vers une CLI compatible."
        )
        if output_callback:
            output_callback("stderr", msg + "\n")

        return CliExecutionResult(
            task_id=task_id,
            mission_id=mission_id,
            cli_id=self.cli_id,
            exit_code=1,
            stdout="",
            stderr=msg,
            duration_ms=10,
            working_dir=cwd,
            command_executed="freebuff (mode interactif uniquement)",
            files_created=[],
            files_modified=[],
            verified=False,
            verdict="TUI_ONLY_REQUIRES_FALLBACK",
            metadata={"version": self.version, "runner": "FreebuffRunner", "limitation": "TUI_ONLY"}
        )

    def cancel_task(self, task_id: str) -> bool:
        return True
