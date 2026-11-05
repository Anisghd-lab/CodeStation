#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Official CLI Registry
Maintains the official registry of the 6 CLIs and provides intelligent routing and fallback.
"""

import time
from typing import Dict, List, Optional, Tuple, Any
from .runners import (
    BaseCliRunner,
    ClixadRunner,
    AntigravityRunner,
    OpenCodeRunner,
    CodeBuddyRunner,
    OmniRushRunner,
    FreebuffRunner
)

class CliRegistry:
    def __init__(self):
        self._runners: Dict[str, BaseCliRunner] = {
            "clixad": ClixadRunner(),
            "antigravity": AntigravityRunner(),
            "opencode": OpenCodeRunner(),
            "codebuddy": CodeBuddyRunner(),
            "omnirush": OmniRushRunner(),
            "freebuff": FreebuffRunner()
        }
        self._health_cache: Dict[str, Tuple[bool, str, str, float]] = {}
        self._cache_ttl = 30.0  # seconds

    def get_runner(self, cli_id: str) -> Optional[BaseCliRunner]:
        normalized = cli_id.lower().strip()
        if normalized in ("agy", "google-antigravity", "antigravity"):
            return self._runners.get("antigravity")
        elif normalized in ("cbc", "codebuddy", "codebuddy-code"):
            return self._runners.get("codebuddy")
        elif normalized in ("opencode", "opencode-glm"):
            return self._runners.get("opencode")
        elif normalized in ("clixad",):
            return self._runners.get("clixad")
        elif normalized in ("omnirush",):
            return self._runners.get("omnirush")
        elif normalized in ("freebuff",):
            return self._runners.get("freebuff")
        return self._runners.get(normalized, None)

    def check_cli_health(self, cli_id: str, force_refresh: bool = False) -> Tuple[bool, str, str]:
        now = time.time()
        runner = self.get_runner(cli_id)
        if not runner:
            return False, "NOT_INSTALLED", f"CLI {cli_id} inconnue dans le registre"

        if not force_refresh and cli_id in self._health_cache:
            is_healthy, status_enum, details, timestamp = self._health_cache[cli_id]
            if now - timestamp < self._cache_ttl:
                return is_healthy, status_enum, details

        is_healthy, status_enum, details = runner.health_check()
        self._health_cache[cli_id] = (is_healthy, status_enum, details, now)
        return is_healthy, status_enum, details

    def get_all_clis_status(self, force_refresh: bool = False) -> List[Dict[str, Any]]:
        results = []
        official_order = ["clixad", "antigravity", "opencode", "codebuddy", "omnirush", "freebuff"]

        for cid in official_order:
            t_check = time.time()
            runner = self._runners[cid]
            is_healthy, status_enum, details = self.check_cli_health(cid, force_refresh)
            latency_ms = max(1, int((time.time() - t_check) * 1000))

            # Map to Android CamcsCliStatus format
            if is_healthy:
                android_status = "AVAILABLE"
            elif status_enum == "NOT_INSTALLED":
                android_status = "NOT_INSTALLED"
            elif status_enum in ("BLOCKED", "PARTIALLY_IMPLEMENTED"):
                android_status = "BLOCKED"
            else:
                android_status = "NOT_INSTALLED" if not runner.path else "AVAILABLE"

            models_map = {
                "clixad": ["gpt-5-nano", "gpt-5-pro"],
                "antigravity": ["gemini-3.8-flash-medium", "gemini-3.8-flash-high", "gemini-3.8-flash-low", "claude-sonnet-4-6", "claude-opus-4-6-thinking", "gpt-oss-120b-medium"],
                "opencode": ["xkiro/qwen3-coder-plus", "opencode/glm-5.3-flash-free"],
                "codebuddy": ["Hy3", "deepseek-v4.1-flash", "kimi-k3"],
                "omnirush": ["gpt-6-astra", "claude-opus-5-5"],
                "freebuff": ["freebuff/free-auto"]
            }

            capabilities_map = {
                "clixad": ["code_generation", "technical_explanation", "qa_testing", "documentation"],
                "antigravity": ["architecture", "code_generation", "refactoring", "tool_use", "terminal_ops"],
                "opencode": ["coding", "acp_stdio", "autonomous_edits", "multi_model"],
                "codebuddy": ["full_project_edits", "headless_print", "fast_coding", "unit_tests"],
                "omnirush": ["high_complexity_coding", "one_shot_prompts", "doctor_audit"],
                "freebuff": ["tui_interactive_coding", "tree_sitter_ast"]
            }

            results.append({
                "id": cid,
                "name": runner.name,
                "binary": runner.binary,
                "path": runner.path or "Introuvable",
                "version": runner.version or "Inconnue",
                "status": android_status,
                "isInstalled": runner.path is not None,
                "activeAgent": None,
                "accessibleModels": models_map.get(cid, []),
                "activeSessions": len(runner.active_processes),
                "capabilities": capabilities_map.get(cid, []),
                "healthStatus": status_enum,
                "healthDetails": details,
                "isOperational": is_healthy,
                "latencyMs": latency_ms
            })

        return results

    def select_best_cli(
        self,
        task_type: str,
        preferred_cli: Optional[str] = None
    ) -> Tuple[BaseCliRunner, str]:
        """
        Intelligent CLI Router:
        Selects the best available, verified CLI based on task requirements,
        with seamless fallback preserving context.
        """
        # If preferred CLI is operational, use it
        if preferred_cli:
            runner = self.get_runner(preferred_cli)
            if runner:
                is_healthy, status_enum, _ = self.check_cli_health(runner.cli_id)
                if is_healthy:
                    return runner, f"Choix préféré : {runner.name} opérationnel"

        # Routing priorities based on task type
        task_routing_priority = {
            "architecture": ["antigravity", "codebuddy", "clixad", "opencode"],
            "coding": ["codebuddy", "opencode", "antigravity", "clixad"],
            "backend": ["codebuddy", "antigravity", "clixad", "opencode"],
            "qa": ["clixad", "codebuddy", "antigravity", "opencode"],
            "verification": ["antigravity", "codebuddy", "clixad", "opencode"],
            "documentation": ["clixad", "antigravity", "codebuddy", "opencode"]
        }

        priorities = task_routing_priority.get(task_type.lower(), ["codebuddy", "antigravity", "clixad", "opencode"])

        for cid in priorities:
            runner = self._runners[cid]
            is_healthy, _, _ = self.check_cli_health(cid)
            if is_healthy:
                return runner, f"Sélection automatique : {runner.name} pour tâche '{task_type}'"

        # Emergency fallback: any operational runner
        for cid, runner in self._runners.items():
            is_healthy, _, _ = self.check_cli_health(cid)
            if is_healthy:
                return runner, f"Repli d'urgence : {runner.name}"

        # If none healthy, default to codebuddy or antigravity
        fallback = self._runners.get("codebuddy") or self._runners.get("antigravity")
        return fallback, "Aucune CLI pleinement opérationnelle, utilisation du runner de secours"
