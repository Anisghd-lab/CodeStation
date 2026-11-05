#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Auto-Repair Loop & Human Escalation
Inspired by Festival's fest/internal/loop/checker.go and state.go.
Provides structured iterative error remediation with:
- Error trace formatting (stderr, exit code, gate failures)
- Contextual corrective prompt generation (RETRY x/max)
- Iteration tracking (max_iterations, default 3)
- Automatic escalation to NEEDS_APPROVAL / WAITING_FOR_APPROVAL when loop limit is reached
- Resumption and human approval integration
"""

import os
import sys
import time
import json
import sqlite3
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field

from .state_machines import TaskState, MissionState

DB_PATH = "/root/.codestation/camcs.db"


@dataclass
class RepairAttempt:
    task_id: str
    mission_id: str
    iteration: int
    max_iterations: int
    error_summary: str
    failure_context: str
    action_taken: str  # "retry", "escalate", "approved", "rejected"
    timestamp: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "taskId": self.task_id,
            "missionId": self.mission_id,
            "iteration": self.iteration,
            "maxIterations": self.max_iterations,
            "errorSummary": self.error_summary,
            "failureContext": self.failure_context,
            "actionTaken": self.action_taken,
            "timestamp": self.timestamp
        }


class AutoRepairLoop:
    def __init__(self, default_max_iterations: int = 3, db_path: str = DB_PATH):
        self.default_max_iterations = default_max_iterations
        self.db_path = db_path
        self._init_db()

    def _init_db(self):
        try:
            os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
            conn = sqlite3.connect(self.db_path)
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE IF NOT EXISTS repair_attempts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id TEXT NOT NULL,
                    mission_id TEXT NOT NULL,
                    iteration INTEGER NOT NULL,
                    max_iterations INTEGER NOT NULL,
                    error_summary TEXT,
                    failure_context TEXT,
                    action_taken TEXT,
                    timestamp REAL NOT NULL
                )
            """)
            conn.commit()
            conn.close()
        except Exception as e:
            print(f"[AutoRepairLoop] DB init error: {e}", file=sys.stderr)

    def format_error_context(
        self,
        task_title: str,
        error_msg: str,
        stdout: str = "",
        stderr: str = "",
        exit_code: Optional[int] = None,
        gate_failures: Optional[List[Dict[str, Any]]] = None
    ) -> str:
        """
        Formats technical failure details into an actionable context block.
        """
        lines = [
            f"[CONTEXTE D'ÉCHEC POUR : {task_title}]",
            f"- Erreur principale : {error_msg}"
        ]
        if exit_code is not None:
            lines.append(f"- Code de retour processus : {exit_code}")

        if stderr and stderr.strip():
            lines.append(f"- Trace standard d'erreur (stderr) :\n{stderr.strip()[:1500]}")
        elif stdout and stdout.strip() and exit_code != 0:
            lines.append(f"- Sortie standard (stdout) :\n{stdout.strip()[:1000]}")

        if gate_failures:
            lines.append("- Contrôles Quality Gate en échec :")
            for gf in gate_failures:
                lines.append(f"  * Gate '{gf.get('name', 'Inconnue')}' ({gf.get('type', 'N/A')}) : {gf.get('details', '')}")

        return "\n".join(lines)

    def build_retry_prompt(
        self,
        base_prompt: str,
        iteration: int,
        max_iterations: int,
        error_context: str
    ) -> str:
        """
        Constructs the corrective prompt instructing the agent to fix the identified issue.
        """
        return (
            f"[CAMCS AUTO-REPAIR — TENTATIVE {iteration}/{max_iterations}]\n"
            f"La précédente tentative a échoué. Analyse attentivement les erreurs ci-dessous et produis une version corrigée et complète.\n\n"
            f"{error_context}\n\n"
            f"DIRECTIVES DE REMÉDIATION :\n"
            f"1. Corrige la cause racine de l'erreur sans dégrader les fonctionnalités existantes.\n"
            f"2. Assure-toi que les fichiers produits sont syntaxiquement valides et exécutables.\n"
            f"3. Réponds avec l'intégralité du code ou des livrables corrigés.\n\n"
            f"[OBJECTIF DE LA TÂCHE INITIALE] :\n{base_prompt}"
        )

    def should_escalate(self, task: Any) -> bool:
        """
        Checks if task iteration count has reached or exceeded max iterations.
        """
        current_iter = getattr(task, "iteration_count", 0)
        max_iter = getattr(task, "max_iterations", self.default_max_iterations)
        return current_iter >= max_iter

    def record_attempt(
        self,
        mission_id: str,
        task_id: str,
        iteration: int,
        max_iterations: int,
        error_summary: str,
        failure_context: str,
        action_taken: str
    ) -> RepairAttempt:
        attempt = RepairAttempt(
            task_id=task_id,
            mission_id=mission_id,
            iteration=iteration,
            max_iterations=max_iterations,
            error_summary=error_summary,
            failure_context=failure_context,
            action_taken=action_taken
        )
        try:
            conn = sqlite3.connect(self.db_path)
            cur = conn.cursor()
            cur.execute("""
                INSERT INTO repair_attempts
                (task_id, mission_id, iteration, max_iterations, error_summary, failure_context, action_taken, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                attempt.task_id, attempt.mission_id, attempt.iteration,
                attempt.max_iterations, attempt.error_summary, attempt.failure_context,
                attempt.action_taken, attempt.timestamp
            ))
            conn.commit()
            conn.close()
        except Exception as e:
            print(f"[AutoRepairLoop] Error recording attempt: {e}", file=sys.stderr)
        return attempt

    def attempt_repair(
        self,
        task: Any,
        error_msg: str,
        stdout: str = "",
        stderr: str = "",
        exit_code: Optional[int] = None,
        gate_failures: Optional[List[Dict[str, Any]]] = None
    ) -> Tuple[bool, str]:
        """
        Evaluates failure, increments iteration, formats prompt or triggers escalation.
        Returns (can_retry: bool, prompt_or_escalation_message: str)
        """
        if not hasattr(task, "iteration_count"):
            task.iteration_count = 0
        if not hasattr(task, "max_iterations"):
            task.max_iterations = self.default_max_iterations

        task.iteration_count += 1
        task.last_error = error_msg
        task.error_message = error_msg

        failure_ctx = self.format_error_context(
            task_title=getattr(task, "title", "Tâche"),
            error_msg=error_msg,
            stdout=stdout,
            stderr=stderr,
            exit_code=exit_code,
            gate_failures=gate_failures
        )

        if task.iteration_count > task.max_iterations:
            # Loop limit reached -> Escalate to human operator
            escalation_msg = (
                f"SEUIL D'AUTO-RÉPARATION DÉPASSÉ ({task.iteration_count - 1}/{task.max_iterations} tentatives échouées). "
                f"Escalade opérateur requise. Dernière erreur : {error_msg}"
            )
            task.status = TaskState.NEEDS_APPROVAL
            task.escalation_reason = escalation_msg

            self.record_attempt(
                mission_id=getattr(task, "mission_id", ""),
                task_id=getattr(task, "id", ""),
                iteration=task.iteration_count,
                max_iterations=task.max_iterations,
                error_summary=error_msg,
                failure_context=failure_ctx,
                action_taken="escalate"
            )
            return False, escalation_msg

        # Under limit -> retry with enriched prompt
        retry_prompt = self.build_retry_prompt(
            base_prompt=getattr(task, "prompt", ""),
            iteration=task.iteration_count,
            max_iterations=task.max_iterations,
            error_context=failure_ctx
        )

        self.record_attempt(
            mission_id=getattr(task, "mission_id", ""),
            task_id=getattr(task, "id", ""),
            iteration=task.iteration_count,
            max_iterations=task.max_iterations,
            error_summary=error_msg,
            failure_context=failure_ctx,
            action_taken="retry"
        )
        return True, retry_prompt

    def resolve_escalation(
        self,
        task: Any,
        approved: bool,
        extra_attempts: int = 1,
        override_prompt: Optional[str] = None
    ) -> bool:
        """
        Resolves human escalation: either authorizes extra retry attempts or marks failed.
        """
        if approved:
            task.status = TaskState.READY
            task.max_iterations = getattr(task, "max_iterations", self.default_max_iterations) + extra_attempts
            task.escalation_reason = None
            if override_prompt:
                task.prompt = override_prompt
            self.record_attempt(
                mission_id=getattr(task, "mission_id", ""),
                task_id=getattr(task, "id", ""),
                iteration=getattr(task, "iteration_count", 0),
                max_iterations=task.max_iterations,
                error_summary="Human approved continuation",
                failure_context=f"Extra attempts granted: +{extra_attempts}",
                action_taken="approved"
            )
            return True
        else:
            task.status = TaskState.FAILED
            task.escalation_reason = "Escalade rejetée par l'opérateur humain."
            self.record_attempt(
                mission_id=getattr(task, "mission_id", ""),
                task_id=getattr(task, "id", ""),
                iteration=getattr(task, "iteration_count", 0),
                max_iterations=getattr(task, "max_iterations", self.default_max_iterations),
                error_summary="Human rejected continuation",
                failure_context="Task marked FAILED by human decision",
                action_taken="rejected"
            )
            return False

    def get_task_repair_history(self, task_id: str) -> List[Dict[str, Any]]:
        results = []
        try:
            conn = sqlite3.connect(self.db_path)
            cur = conn.cursor()
            cur.execute("""
                SELECT task_id, mission_id, iteration, max_iterations, error_summary, failure_context, action_taken, timestamp
                FROM repair_attempts WHERE task_id = ? ORDER BY timestamp ASC
            """, (task_id,))
            for r in cur.fetchall():
                results.append({
                    "taskId": r[0],
                    "missionId": r[1],
                    "iteration": r[2],
                    "maxIterations": r[3],
                    "errorSummary": r[4],
                    "failureContext": r[5],
                    "actionTaken": r[6],
                    "timestamp": r[7]
                })
            conn.close()
        except Exception as e:
            print(f"[AutoRepairLoop] Error querying history: {e}", file=sys.stderr)
        return results
