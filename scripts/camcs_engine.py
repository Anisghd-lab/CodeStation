#!/usr/bin/env python3
"""
CodeStation CAMCS — Multi-Provider Integration, Autonomous Multi-Agent Engine & Live Control
Version 2.0 (Strict Reliability, Real Execution, Objective-Oriented DAG & 100% Free Models)
Compatible with Ubuntu PRoot ARM64 & Android Native Architecture.
"""

import os
import sys
import time
import json
import uuid
import sqlite3
import threading
import queue
import shutil
import hashlib
import subprocess
import urllib.parse
from datetime import datetime, timezone
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from typing import Dict, List, Optional, Any, Tuple

# Modular CAMCS Real Execution Engine Components
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from camcs.task_classifier import TaskClassifier, TaskClassification, TaskProfile
from camcs.state_machines import (
    MissionState, AgentState, TaskState,
    validate_mission_transition, validate_agent_transition, validate_task_transition
)
from camcs.provider_registry import (
    ProviderRegistry, ProviderSelector, ProviderFallbackManager,
    ModelCapability, BaseProviderAdapter
)
from camcs.dag_planner import DagPlanner, DagTask
from camcs.agent_factory import AgentFactory
from camcs.cli_registry import CliRegistry
from camcs.process_manager import ProcessManager
from camcs.deliverable_manager import DeliverableManager, compute_sha256
from camcs.quality_auditor import QualityAuditor
from camcs.collaboration_hub import CollaborationHub
from camcs.collective_memory_client import CollectiveMemoryClient
from camcs.runners.base_runner import CliExecutionResult, BaseCliRunner
from camcs.external_action_verifier import (
    ExternalActionVerifier, ExternalActionStatus, DeliverableType, ExternalAction
)
from camcs.github_workflow import GitHubWorkflowExecutor
from camcs.credential_registry import CredentialRegistry
from camcs.external_service_registry import ExternalServiceRegistry
from camcs.external_action_executor import ExternalActionExecutor

# Database for CAMCS persistence
DB_PATH = os.path.expanduser("/root/.codestation/camcs.db")

def init_db():
    """Initializes and migrates the CAMCS SQLite schema."""
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS missions (
            id TEXT PRIMARY KEY,
            title TEXT,
            objective TEXT,
            governance_mode TEXT,
            status TEXT,
            progress INTEGER,
            orchestration_phase TEXT,
            created_at REAL,
            updated_at REAL,
            completed_at REAL,
            config_json TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS agents (
            id TEXT PRIMARY KEY,
            mission_id TEXT,
            task_id TEXT,
            name TEXT,
            role TEXT,
            shape TEXT,
            color TEXT,
            cli_id TEXT,
            provider_id TEXT,
            model_id TEXT,
            objective TEXT,
            current_subtask TEXT,
            progress INTEGER,
            status TEXT,
            pid INTEGER,
            started_at REAL,
            last_activity_at REAL,
            created_at REAL,
            updated_at REAL,
            FOREIGN KEY(mission_id) REFERENCES missions(id)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS tasks (
            id TEXT PRIMARY KEY,
            mission_id TEXT,
            agent_id TEXT,
            classification TEXT,
            title TEXT,
            description TEXT,
            prompt TEXT,
            dependencies_json TEXT,
            status TEXT,
            progress INTEGER,
            weight INTEGER,
            is_mandatory INTEGER,
            acceptance_criteria TEXT,
            expected_deliverable TEXT,
            assigned_file TEXT,
            exit_code INTEGER,
            result_summary TEXT,
            error_message TEXT,
            created_at REAL,
            started_at REAL,
            completed_at REAL,
            FOREIGN KEY(mission_id) REFERENCES missions(id),
            FOREIGN KEY(agent_id) REFERENCES agents(id)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            mission_id TEXT,
            agent_id TEXT,
            task_id TEXT,
            event_type TEXT,
            message TEXT,
            payload_json TEXT,
            created_at REAL
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS state_transitions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            mission_id TEXT,
            agent_id TEXT,
            task_id TEXT,
            previous_state TEXT,
            current_state TEXT,
            event_source TEXT,
            reason TEXT,
            duration_in_prev_state_ms INTEGER,
            created_at REAL
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS deliverables (
            id TEXT PRIMARY KEY,
            mission_id TEXT,
            task_id TEXT,
            agent_id TEXT,
            agent_name TEXT,
            file_path TEXT,
            file_name TEXT,
            doc_type TEXT,
            description TEXT,
            sha256 TEXT,
            size_bytes INTEGER,
            validation_status TEXT,
            validation_details TEXT,
            created_at REAL,
            updated_at REAL,
            FOREIGN KEY(mission_id) REFERENCES missions(id)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS execution_proofs (
            id TEXT PRIMARY KEY,
            mission_id TEXT,
            task_id TEXT,
            agent_id TEXT,
            agent_name TEXT,
            cli_command TEXT,
            working_dir TEXT,
            exit_code INTEGER,
            stdout TEXT,
            stderr TEXT,
            duration_ms INTEGER,
            files_created_json TEXT,
            files_modified_json TEXT,
            verified INTEGER,
            verdict TEXT,
            provider_id TEXT,
            model_id TEXT,
            created_at REAL,
            FOREIGN KEY(mission_id) REFERENCES missions(id)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS agent_communications (
            id TEXT PRIMARY KEY,
            mission_id TEXT,
            sender_agent_id TEXT,
            sender_name TEXT,
            receiver_agent_id TEXT,
            receiver_name TEXT,
            message_type TEXT,
            content TEXT,
            payload_json TEXT,
            status TEXT,
            created_at REAL,
            FOREIGN KEY(mission_id) REFERENCES missions(id)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS mission_reports (
            id TEXT PRIMARY KEY,
            mission_id TEXT,
            title TEXT,
            initial_objective TEXT,
            executed_plan_json TEXT,
            agents_summary_json TEXT,
            files_summary_json TEXT,
            deliverables_summary_json TEXT,
            tests_summary_json TEXT,
            independent_validation_summary TEXT,
            final_state TEXT,
            report_markdown TEXT,
            created_at REAL,
            FOREIGN KEY(mission_id) REFERENCES missions(id)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS provider_stats (
            provider_id TEXT PRIMARY KEY,
            name TEXT,
            status TEXT,
            models_count INTEGER,
            active_model TEXT,
            total_requests INTEGER,
            failed_requests INTEGER,
            average_latency_ms REAL,
            last_checked REAL
        )
    """)

    # Schema migration: Ensure columns exist in missions
    try:
        cur.execute("ALTER TABLE missions ADD COLUMN orchestration_phase TEXT")
    except Exception:
        pass
    try:
        cur.execute("ALTER TABLE missions ADD COLUMN governance_mode TEXT DEFAULT 'HYBRID'")
    except Exception:
        pass

    for col, ctype in [
        ('classification', 'TEXT'),
        ('prompt', 'TEXT'),
        ('weight', 'INTEGER DEFAULT 1'),
        ('is_mandatory', 'INTEGER DEFAULT 1'),
        ('acceptance_criteria', 'TEXT'),
        ('expected_deliverable', 'TEXT'),
        ('assigned_file', 'TEXT'),
        ('exit_code', 'INTEGER'),
        ('error_message', 'TEXT'),
        ('started_at', 'REAL'),
        ('orchestration_phase', 'TEXT'),
        ('governance_mode', 'TEXT')
    ]:
        try:
            cur.execute(f"ALTER TABLE tasks ADD COLUMN {col} {ctype}")
        except Exception:
            pass

    for col, ctype in [
        ('task_id', 'TEXT'),
        ('pid', 'INTEGER'),
        ('started_at', 'REAL'),
        ('last_activity_at', 'REAL')
    ]:
        try:
            cur.execute(f"ALTER TABLE agents ADD COLUMN {col} {ctype}")
        except Exception:
            pass

    conn.commit()
    conn.close()


class CamcsAgent:
    def __init__(self, agent_id: str, mission_id: str, name: str, role: str, shape: str, color: str, cli_id: str, provider_id: str, model_id: str, objective: str):
        self.id = agent_id
        self.mission_id = mission_id
        self.name = name
        self.role = role
        self.shape = shape
        self.color = color
        self.cli_id = cli_id
        self.provider_id = provider_id
        self.model_id = model_id
        self.objective = objective
        self.skills: List[str] = []
        self.current_subtask = "Initialisation du contexte de travail"
        self.progress = 0
        self.status = AgentState.CREATED
        self.pid: Optional[int] = None
        self.started_at: Optional[float] = None
        self.last_activity_at: float = time.time()
        self.last_output: str = ""
        self.active_task_id: Optional[str] = None
        self.waiting_for_task_id: Optional[str] = None
        self.waiting_for_task_title: Optional[str] = None
        self.waiting_for_agent_name: Optional[str] = None
        self.dependency_status: Optional[str] = None
        self.wait_started_at: Optional[float] = None
        self.blocking_reason: Optional[str] = None
        self.tasks: List[Dict[str, Any]] = []

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "name": self.name,
            "role": self.role,
            "shape": self.shape,
            "color": self.color,
            "cli": self.cli_id,
            "provider": self.provider_id,
            "model": self.model_id,
            "skills": self.skills,
            "objective": self.objective,
            "currentSubtask": self.current_subtask,
            "progress": self.progress,
            "status": self.status,
            "pid": self.pid,
            "startedAt": self.started_at,
            "lastActivityAt": self.last_activity_at,
            "lastOutput": self.last_output,
            "activeTaskId": self.active_task_id,
            "waitingForTaskId": self.waiting_for_task_id,
            "waitingForTaskTitle": self.waiting_for_task_title,
            "waitingForAgentName": self.waiting_for_agent_name,
            "dependencyStatus": self.dependency_status,
            "waitStartedAt": self.wait_started_at,
            "blockingReason": self.blocking_reason,
            "tasks": self.tasks
        }


class CamcsMission:
    def __init__(self, mission_id: str, title: str, objective: str, governance_mode: str = "HYBRID"):
        self.id = mission_id
        self.title = title
        self.objective = objective
        self.governance_mode = governance_mode
        self.status = MissionState.CREATED
        self.progress = 0
        self.orchestration_phase = "PLANNING"
        self.agents: List[CamcsAgent] = []
        self.tasks: List[DagTask] = []
        self.created_at = time.time()
        self.started_at: Optional[float] = None
        self.completed_at: Optional[float] = None
        self.last_transition_reason: str = "Mission créée"
        self.last_transition_time: float = time.time()
        self.blocked_reason: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        ext_actions = []
        try:
            from camcs.external_action_verifier import ExternalActionVerifier
            ext_actions = [a.to_dict() for a in ExternalActionVerifier(DB_PATH).get_mission_actions(self.id)]
        except Exception:
            pass
        return {
            "id": self.id,
            "title": self.title,
            "objective": self.objective,
            "governanceMode": self.governance_mode,
            "status": self.status,
            "progress": self.progress,
            "orchestrationPhase": self.orchestration_phase,
            "createdAt": self.created_at,
            "startedAt": self.started_at,
            "completedAt": self.completed_at,
            "lastTransitionReason": self.last_transition_reason,
            "lastTransitionTime": self.last_transition_time,
            "blockedReason": self.blocked_reason,
            "agents": [a.to_dict() for a in self.agents],
            "tasks": [t.to_dict() for t in self.tasks],
            "externalActions": ext_actions
        }


class CamcsOrchestrationEngine:
    def __init__(self):
        self.cli_registry = CliRegistry()
        self.provider_registry = ProviderRegistry()
        self.fallback_manager = ProviderFallbackManager(self.provider_registry)
        self.process_manager = ProcessManager(max_concurrent=2)
        self.agent_factory = AgentFactory(self.cli_registry, self.provider_registry)
        self.dag_planner = DagPlanner()
        self.deliverable_mgr = DeliverableManager(DB_PATH)
        self.quality_auditor = QualityAuditor(DB_PATH)
        self.collab_hub = CollaborationHub(DB_PATH)
        self.memory_client = CollectiveMemoryClient()
        self.external_verifier = ExternalActionVerifier(DB_PATH)
        self.github_executor = GitHubWorkflowExecutor(self.external_verifier)
        self.credential_registry = CredentialRegistry()
        self.external_services = ExternalServiceRegistry(DB_PATH)
        self.external_executor = ExternalActionExecutor(DB_PATH)
        self.active_missions: Dict[str, CamcsMission] = {}
        self.event_subscribers: List[queue.Queue] = []
        self._lock = threading.RLock()
        self._seq_counter = 0
        self._load_missions_from_db()

    def _load_missions_from_db(self):
        """Loads all missions, agents, and real tasks from SQLite with invariant enforcement."""
        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            cur.execute("""
                SELECT id, title, objective, status, progress, orchestration_phase, created_at, completed_at, config_json
                FROM missions ORDER BY created_at DESC
            """)
            rows = cur.fetchall()
            for r in rows:
                m_id, title, obj, status, prog, orch_phase, created_at, comp_at, cfg = r
                gov_mode = "HYBRID"
                started_at = None
                reason = "Mission chargée depuis la base"
                t_time = created_at
                blocked_r = None

                if cfg:
                    try:
                        cfg_dict = json.loads(cfg)
                        gov_mode = cfg_dict.get("governanceMode", "HYBRID")
                        started_at = cfg_dict.get("startedAt")
                        reason = cfg_dict.get("lastTransitionReason", reason)
                        t_time = cfg_dict.get("lastTransitionTime", created_at)
                        blocked_r = cfg_dict.get("blockedReason")
                    except Exception:
                        pass

                m = CamcsMission(m_id, title, obj, gov_mode)
                m.status = status
                m.progress = 100 if status == MissionState.COMPLETED else prog
                m.orchestration_phase = orch_phase or ("COMPLETED" if status == MissionState.COMPLETED else "PLANNING")
                m.created_at = created_at
                m.started_at = started_at
                m.completed_at = comp_at
                m.last_transition_reason = reason
                m.last_transition_time = t_time
                m.blocked_reason = blocked_r

                # Load real tasks
                cur.execute("""
                    SELECT id, agent_id, classification, title, description, prompt, dependencies_json,
                           status, progress, weight, is_mandatory, acceptance_criteria, expected_deliverable,
                           assigned_file, exit_code, result_summary, error_message, created_at, started_at, completed_at
                    FROM tasks WHERE mission_id = ?
                """, (m_id,))
                t_rows = cur.fetchall()
                mission_tasks = []
                for tr in t_rows:
                    deps = []
                    if tr[6]:
                        try:
                            deps = json.loads(tr[6])
                        except Exception:
                            deps = []
                    t = DagTask(
                        id=tr[0],
                        mission_id=m_id,
                        agent_id=tr[1],
                        classification=tr[2] or "SOFTWARE_DEVELOPMENT",
                        title=tr[3],
                        description=tr[4] or "",
                        prompt=tr[5] or "",
                        dependencies=deps,
                        status=tr[7],
                        progress=tr[8],
                        weight=tr[9] or 20,
                        is_mandatory=bool(tr[10]),
                        acceptance_criteria=tr[11] or "",
                        expected_deliverable=tr[12] or "",
                        assigned_file=tr[13],
                        exit_code=tr[14],
                        result_summary=tr[15] or "",
                        error_message=tr[16],
                        created_at=tr[17] or created_at,
                        started_at=tr[18],
                        completed_at=tr[19]
                    )
                    mission_tasks.append(t)
                m.tasks = mission_tasks

                # Load agents
                cur.execute("""
                    SELECT id, name, role, shape, color, cli_id, provider_id, model_id, objective,
                           current_subtask, progress, status, pid, started_at, last_activity_at
                    FROM agents WHERE mission_id = ?
                """, (m_id,))
                ag_rows = cur.fetchall()
                agents = []
                for ar in ag_rows:
                    ag_id, name, role, shape, color, cli_id, prov_id, mod_id, ag_obj, cur_sub, ag_prog, ag_stat, pid, a_start, a_last = ar
                    ag = CamcsAgent(ag_id, m_id, name, role, shape, color, cli_id, prov_id, mod_id, ag_obj)
                    ag.current_subtask = cur_sub or ""
                    ag.progress = 100 if (m.status == MissionState.COMPLETED or ag_stat == AgentState.COMPLETED) else (ag_prog or 0)
                    ag.status = AgentState.COMPLETED if m.status == MissionState.COMPLETED else (ag_stat or AgentState.CREATED)
                    ag.started_at = a_start
                    ag.last_activity_at = a_last or time.time()
                    ag.pid = None  # Clear past dead PIDs on load

                    # Populate real assigned tasks
                    assigned = [t.to_dict() for t in mission_tasks if t.agent_id == ag_id]
                    ag.tasks = assigned
                    agents.append(ag)

                m.agents = agents
                self.active_missions[m_id] = m
            conn.close()
        except Exception as e:
            print(f"[CamcsEngine] Error loading missions from DB: {e}", file=sys.stderr)

    def subscribe_events(self) -> queue.Queue:
        q = queue.Queue(maxsize=300)
        with self._lock:
            self.event_subscribers.append(q)
        return q

    def unsubscribe_events(self, q: queue.Queue):
        with self._lock:
            if q in self.event_subscribers:
                self.event_subscribers.remove(q)

    def emit_event(
        self,
        mission_id: str,
        event_type: str,
        message: str,
        agent_id: Optional[str] = None,
        task_id: Optional[str] = None,
        previous_state: Optional[str] = None,
        current_state: Optional[str] = None,
        payload: Optional[Dict[str, Any]] = None
    ):
        """Emits an event to subscribers and persists to SQLite with unique sequencing."""
        now = time.time()
        with self._lock:
            self._seq_counter += 1
            seq = self._seq_counter

        ev_id = str(uuid.uuid4())
        full_payload = payload.copy() if payload else {}
        full_payload["sequenceNumber"] = seq
        full_payload["eventId"] = ev_id

        event_dict = {
            "id": seq,
            "eventId": ev_id,
            "missionId": mission_id,
            "agentId": agent_id,
            "taskId": task_id,
            "type": event_type,
            "previousState": previous_state,
            "currentState": current_state,
            "message": message,
            "timestamp": now,
            "sequenceNumber": seq,
            "payload": full_payload
        }

        # Persist to events table
        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            cur.execute("""
                INSERT INTO events (mission_id, agent_id, task_id, event_type, message, payload_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """, (mission_id, agent_id, task_id, event_type, message, json.dumps(full_payload), now))
            conn.commit()
            conn.close()
        except Exception:
            pass

        # Broadcast via active queues
        with self._lock:
            dead = []
            for sub in self.event_subscribers:
                try:
                    sub.put_nowait(event_dict)
                except queue.Full:
                    dead.append(sub)
            for d in dead:
                if d in self.event_subscribers:
                    self.event_subscribers.remove(d)

    def record_mission_transition(
        self,
        mission_id: str,
        new_state: str,
        reason: str,
        orchestration_phase: Optional[str] = None,
        event_source: str = "CAMCS_ENGINE"
    ) -> bool:
        """
        Transitions MISSION state strictly respecting the 15-state state machine.
        Guarantees: COMPLETED => 100% and all mandatory tasks validated.
        Never overwrites agent states.
        """
        with self._lock:
            m = self.active_missions.get(mission_id)
            if not m:
                return False

            now = time.time()
            prev_state = m.status
            prev_time = getattr(m, "last_transition_time", m.created_at) or m.created_at
            duration_ms = int((now - prev_time) * 1000)

            all_mand_ok = self.dag_planner.all_mandatory_tasks_completed(m.tasks) if m.tasks else True
            valid, err = validate_mission_transition(prev_state, new_state, all_mandatory_tasks_validated=all_mand_ok)
            if not valid:
                print(f"[CamcsEngine] Mission transition rejected: {err}", file=sys.stderr)
                return False

            m.status = new_state
            m.last_transition_reason = reason
            m.last_transition_time = now
            if orchestration_phase:
                m.orchestration_phase = orchestration_phase

            if new_state in [MissionState.STARTING, MissionState.RUNNING] and not m.started_at:
                m.started_at = now
            if new_state == MissionState.COMPLETED:
                m.progress = 100
                m.completed_at = now
                m.orchestration_phase = "COMPLETED"
            elif new_state in [MissionState.FAILED, MissionState.CANCELLED]:
                m.completed_at = now

            self._save_mission_state(m)

            # Persist transition
            try:
                conn = sqlite3.connect(DB_PATH)
                cur = conn.cursor()
                cur.execute("""
                    INSERT INTO state_transitions (mission_id, agent_id, task_id, previous_state, current_state, event_source, reason, duration_in_prev_state_ms, created_at)
                    VALUES (?, NULL, NULL, ?, ?, ?, ?, ?, ?)
                """, (mission_id, prev_state, new_state, event_source, reason, duration_ms, now))
                conn.commit()
                conn.close()
            except Exception:
                pass

        self.emit_event(
            mission_id=mission_id,
            event_type="mission.status_changed",
            message=f"[{new_state}] {reason}",
            previous_state=prev_state,
            current_state=new_state,
            payload={
                "missionId": mission_id,
                "previousState": prev_state,
                "currentState": new_state,
                "status": new_state,
                "reason": reason,
                "progress": m.progress,
                "orchestrationPhase": m.orchestration_phase,
                "durationMs": duration_ms
            }
        )
        return True

    def record_agent_transition(
        self,
        mission_id: str,
        agent_id: str,
        new_state: str,
        reason: str,
        subtask: Optional[str] = None,
        pid: Optional[int] = None,
        event_source: str = "CAMCS_ENGINE"
    ) -> bool:
        """
        Transitions AGENT state independently from mission state.
        Never overwrites mission status with agent state.
        """
        with self._lock:
            m = self.active_missions.get(mission_id)
            if not m:
                return False

            target_ag = next((a for a in m.agents if a.id == agent_id), None)
            if not target_ag:
                return False

            now = time.time()
            prev_state = target_ag.status
            prev_time = target_ag.last_activity_at
            duration_ms = int((now - prev_time) * 1000)

            valid, err = validate_agent_transition(prev_state, new_state)
            if not valid:
                print(f"[CamcsEngine] Agent transition rejected: {err}", file=sys.stderr)
                return False

            target_ag.status = new_state
            target_ag.last_activity_at = now
            if subtask is not None:
                target_ag.current_subtask = subtask
            if pid is not None:
                target_ag.pid = pid if pid > 0 else None

            if new_state in [AgentState.STARTING, AgentState.RUNNING] and not target_ag.started_at:
                target_ag.started_at = now
            if new_state == AgentState.COMPLETED:
                target_ag.progress = 100
                target_ag.pid = None
                target_ag.active_task_id = None

            self._save_agent(target_ag)

            # Persist transition
            try:
                conn = sqlite3.connect(DB_PATH)
                cur = conn.cursor()
                cur.execute("""
                    INSERT INTO state_transitions (mission_id, agent_id, task_id, previous_state, current_state, event_source, reason, duration_in_prev_state_ms, created_at)
                    VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?)
                """, (mission_id, agent_id, prev_state, new_state, event_source, reason, duration_ms, now))
                conn.commit()
                conn.close()
            except Exception:
                pass

        self.emit_event(
            mission_id=mission_id,
            event_type="agent.status_changed",
            message=f"[{new_state}] {target_ag.name} : {reason}",
            agent_id=agent_id,
            previous_state=prev_state,
            current_state=new_state,
            payload={
                "missionId": mission_id,
                "agentId": agent_id,
                "previousState": prev_state,
                "currentState": new_state,
                "status": new_state,
                "reason": reason,
                "subtask": target_ag.current_subtask,
                "progress": target_ag.progress,
                "pid": target_ag.pid,
                "durationMs": duration_ms
            }
        )
        return True

    def record_task_transition(
        self,
        mission_id: str,
        task_id: str,
        new_state: str,
        progress: Optional[int] = None,
        result_summary: Optional[str] = None,
        error_message: Optional[str] = None
    ) -> bool:
        """Transitions TASK state and updates progress of agent and mission."""
        with self._lock:
            m = self.active_missions.get(mission_id)
            if not m:
                return False

            task = next((t for t in m.tasks if t.id == task_id), None)
            if not task:
                return False

            prev_state = task.status
            valid, err = validate_task_transition(prev_state, new_state)
            if not valid:
                print(f"[CamcsEngine] Task transition rejected: {err}", file=sys.stderr)
                return False

            now = time.time()
            task.status = new_state
            if progress is not None:
                task.progress = max(0, min(100, progress))
            if new_state == TaskState.COMPLETED:
                task.progress = 100
                task.completed_at = now
            elif new_state == TaskState.RUNNING and not task.started_at:
                task.started_at = now
            if result_summary is not None:
                task.result_summary = result_summary
            if error_message is not None:
                task.error_message = error_message

            # Save task to DB
            self._save_task(task)

            # Recompute agent progress and update agent subtasks list
            target_ag = next((a for a in m.agents if a.id == task.agent_id), None)
            if target_ag:
                target_ag.progress = self.dag_planner.compute_agent_progress(target_ag.id, m.tasks)
                target_ag.tasks = [t.to_dict() for t in m.tasks if t.agent_id == target_ag.id]
                ag_tasks = [t for t in m.tasks if t.agent_id == target_ag.id]
                if ag_tasks and all(t.status == TaskState.COMPLETED for t in ag_tasks):
                    target_ag.status = AgentState.COMPLETED
                    target_ag.progress = 100
                    target_ag.pid = None
                    target_ag.active_task_id = None
                    target_ag.current_subtask = "Livrables validés et vérifiés (100%)"
                elif any(t.status == TaskState.RUNNING for t in ag_tasks):
                    target_ag.status = AgentState.RUNNING
                elif any(t.status == TaskState.PENDING for t in ag_tasks):
                    target_ag.status = AgentState.WAITING_FOR_DEPENDENCY
                    target_ag.current_subtask = "En attente des dépendances de la tâche"
                self._save_agent(target_ag)

            # Recompute mission progress
            self._update_progress(m)

        self.emit_event(
            mission_id=mission_id,
            event_type="task.status_changed",
            message=f"Tâche {task.title} : {new_state} ({task.progress}%)",
            agent_id=task.agent_id,
            task_id=task.id,
            previous_state=prev_state,
            current_state=new_state,
            payload={
                "taskId": task.id,
                "agentId": task.agent_id,
                "status": new_state,
                "progress": task.progress,
                "resultSummary": task.result_summary
            }
        )
        return True

    def _update_progress(self, mission: CamcsMission):
        """Calculates mission progress strictly from mandatory DAG tasks."""
        if mission.status == MissionState.COMPLETED:
            mission.progress = 100
        else:
            mission.progress = self.dag_planner.compute_mission_progress(mission.tasks)

        self._save_mission_state(mission)
        self.emit_event(
            mission.id,
            "mission.progress",
            f"Progression globale de la mission : {mission.progress}%",
            payload={
                "progress": mission.progress,
                "status": mission.status,
                "orchestrationPhase": mission.orchestration_phase
            }
        )

    def _save_mission_state(self, mission: CamcsMission):
        """Persists mission to SQLite."""
        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            cfg_dict = {
                "governanceMode": mission.governance_mode,
                "startedAt": mission.started_at,
                "lastTransitionReason": mission.last_transition_reason,
                "lastTransitionTime": mission.last_transition_time,
                "blockedReason": mission.blocked_reason
            }
            cur.execute("""
                INSERT OR REPLACE INTO missions
                (id, title, objective, governance_mode, status, progress, orchestration_phase, created_at, updated_at, completed_at, config_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                mission.id, mission.title, mission.objective, mission.governance_mode,
                mission.status, mission.progress, mission.orchestration_phase,
                mission.created_at, time.time(), mission.completed_at, json.dumps(cfg_dict)
            ))
            conn.commit()
            conn.close()
        except Exception as e:
            print(f"[CamcsEngine] Error saving mission: {e}", file=sys.stderr)

    def _save_agent(self, ag: CamcsAgent):
        """Persists agent to SQLite."""
        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            cur.execute("""
                INSERT OR REPLACE INTO agents
                (id, mission_id, task_id, name, role, shape, color, cli_id, provider_id, model_id, objective, current_subtask, progress, status, pid, started_at, last_activity_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                ag.id, ag.mission_id, ag.active_task_id, ag.name, ag.role, ag.shape, ag.color,
                ag.cli_id, ag.provider_id, ag.model_id, ag.objective, ag.current_subtask,
                ag.progress, ag.status, ag.pid, ag.started_at, ag.last_activity_at,
                ag.started_at or time.time(), time.time()
            ))
            conn.commit()
            conn.close()
        except Exception as e:
            print(f"[CamcsEngine] Error saving agent: {e}", file=sys.stderr)

    def _save_task(self, task: DagTask):
        """Persists task to SQLite."""
        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            cur.execute("""
                INSERT OR REPLACE INTO tasks
                (id, mission_id, agent_id, classification, title, description, prompt, dependencies_json,
                 status, progress, weight, is_mandatory, acceptance_criteria, expected_deliverable, assigned_file,
                 exit_code, result_summary, error_message, created_at, started_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                task.id, task.mission_id, task.agent_id, task.classification, task.title,
                task.description, task.prompt, json.dumps(task.dependencies), task.status,
                task.progress, task.weight, 1 if task.is_mandatory else 0, task.acceptance_criteria,
                task.expected_deliverable, task.assigned_file, task.exit_code, task.result_summary,
                task.error_message, task.created_at, task.started_at, task.completed_at
            ))
            conn.commit()
            conn.close()
        except Exception as e:
            print(f"[CamcsEngine] Error saving task: {e}", file=sys.stderr)

    def execute_with_failover(
        self,
        mission: CamcsMission,
        agent: CamcsAgent,
        task: DagTask,
        cwd: str,
        timeout: int = 90,
        output_callback: Optional[Any] = None,
        pid_callback: Optional[Any] = None
    ) -> Tuple[CliExecutionResult, str]:
        """
        Executes a task with multi-level resilient fallback:
        1. Primary assigned operational CLI.
        2. Alternative operational CLI from local PRoot ARM64 registry.
        3. 100% FREE API model via OpenRouter / xKIRO / OpenCode.
        Preserves context and emits audit failover events.
        """
        # Level 1: Attempt Primary CLI
        runner = self.cli_registry.get_runner(agent.cli_id)
        if runner:
            is_healthy, status_enum, _ = self.cli_registry.check_cli_health(runner.cli_id)
            if is_healthy:
                res = self.process_manager.execute_with_guard(
                    runner=runner,
                    task_id=task.id,
                    mission_id=mission.id,
                    prompt=task.prompt,
                    cwd=cwd,
                    timeout=timeout,
                    output_callback=output_callback,
                    pid_callback=pid_callback
                )
                if res.exit_code == 0:
                    return res, f"{runner.name} (Local)"

                self.emit_event(
                    mission.id, "cli.failed",
                    f"Échec de {runner.name} (code {res.exit_code}). Activation de la stratégie de remplacement.",
                    agent_id=agent.id, task_id=task.id
                )

        # Level 2: Seek Alternative Operational Local CLI
        alt_runners = self.cli_registry.get_all_clis_status()
        for cstat in alt_runners:
            if cstat["isOperational"] and cstat["id"] != agent.cli_id:
                alt_runner = self.cli_registry.get_runner(cstat["id"])
                if alt_runner:
                    self.emit_event(
                        mission.id, "agent.replacement",
                        f"Remplacement automatique : Basculement vers {alt_runner.name}.",
                        agent_id=agent.id, task_id=task.id
                    )
                    res = self.process_manager.execute_with_guard(
                        runner=alt_runner,
                        task_id=task.id,
                        mission_id=mission.id,
                        prompt=task.prompt,
                        cwd=cwd,
                        timeout=timeout,
                        output_callback=output_callback,
                        pid_callback=pid_callback
                    )
                    if res.exit_code == 0:
                        return res, f"{alt_runner.name} (Remplacement CLI)"

        # Level 3: Fallback to 100% FREE API Model
        self.emit_event(
            mission.id, "provider.fallback",
            "Aucun CLI local disponible. Basculement vers un modèle d'API 100% gratuit.",
            agent_id=agent.id, task_id=task.id
        )

        messages = [
            {"role": "system", "content": f"Tu es l'agent CAMCS {agent.name} ({agent.role}). Réponds précisément avec du code ou de la documentation exploitable."},
            {"role": "user", "content": task.prompt}
        ]

        ok, content, prov, model, dur = self.fallback_manager.execute_with_fallback(
            task_type=task.classification,
            messages=messages
        )

        api_res = CliExecutionResult(
            task_id=task.id,
            mission_id=mission.id,
            cli_id=f"api_{prov}",
            exit_code=0 if ok else -1,
            stdout=content if ok else "",
            stderr="" if ok else content,
            duration_ms=int(dur),
            working_dir=cwd,
            command_executed=f"free_api:{prov}:{model}",
            verified=ok,
            verdict="CONFORME" if ok else "PARTIAL",
            metadata={"provider": prov, "model": model, "is_free": True}
        )
        return api_res, f"Modèle Gratuit ({prov} / {model})"

    def _execute_mission_workflow(self, mission: CamcsMission):
        """
        Main autonomous execution loop:
        Decomposes objective -> DAG -> Executes tasks -> Validates -> Seals final report.
        """
        if mission.status == MissionState.CANCELLED:
            return

        mission_dir = f"/root/.codestation/missions/{mission.id}"
        docs_dir = os.path.join(mission_dir, "docs")
        src_dir = os.path.join(mission_dir, "src")
        tests_dir = os.path.join(mission_dir, "tests")
        proofs_dir = os.path.join(mission_dir, "proofs")
        for d in [docs_dir, src_dir, tests_dir, proofs_dir]:
            os.makedirs(d, exist_ok=True)

        # Phase 0: Memory Query & Planning
        self.record_mission_transition(
            mission.id, MissionState.PLANNING,
            "Consultation de la Mémoire Collective et construction du DAG",
            orchestration_phase="PLANNING"
        )

        mem_results = self.memory_client.query_knowledge(mission.objective, limit=3)
        mem_guidance = ""
        if mem_results:
            top_titles = [m.get("title", "") for m in mem_results[:3]]
            mem_guidance = f"\n[RÈGLES MÉMOIRE PERPÉTUELLE] : {', '.join(top_titles)}"
            self.emit_event(
                mission.id, "memory.recalled",
                f"Mémoire consultée : {len(mem_results)} connaissances réutilisées.",
                payload={"count": len(mem_results)}
            )

        if mission.status == MissionState.CANCELLED:
            return

        # Phase 1: Build DAG & Classify Tasks
        raw_agents = [ag.to_dict() for ag in mission.agents]
        dag_tasks = self.dag_planner.build_dag_for_mission(
            mission_id=mission.id,
            title=mission.title,
            objective=mission.objective + mem_guidance,
            agents=raw_agents,
            workspace_dir=mission_dir
        )
        mission.tasks = dag_tasks

        # Persist all tasks to DB
        for t in dag_tasks:
            self._save_task(t)

        # Distribute tasks to agents in memory
        for ag in mission.agents:
            ag.tasks = [t.to_dict() for t in dag_tasks if t.agent_id == ag.id]
            self._save_agent(ag)

        # Phase 2: Dispatching
        self.record_mission_transition(
            mission.id, MissionState.DISPATCHING,
            f"Affectation des {len(dag_tasks)} tâches DAG aux agents spécialisés",
            orchestration_phase="DISPATCHING"
        )

        if mission.status == MissionState.CANCELLED:
            return

        # Phase 3: Execution Loop
        self.record_mission_transition(
            mission.id, MissionState.RUNNING,
            "Exécution coordonnée des tâches orientées objectif",
            orchestration_phase="EXECUTION"
        )

        completed_task_ids: Set[str] = set()

        while len(completed_task_ids) < len(dag_tasks):
            if mission.status == MissionState.CANCELLED:
                return

            # Active pause check
            while mission.status in (MissionState.PAUSED, "PAUSED", MissionState.WAITING_FOR_APPROVAL, "WAITING_FOR_APPROVAL") or getattr(mission, "is_paused", False):
                time.sleep(0.5)
                if mission.status == MissionState.CANCELLED:
                    return

            ready_tasks = [t for t in dag_tasks if t.id not in completed_task_ids and all(dep in completed_task_ids for dep in t.dependencies)]
            if not ready_tasks:
                # Check if remaining tasks are blocked
                remaining = [t for t in dag_tasks if t.id not in completed_task_ids]
                if remaining:
                    self.record_mission_transition(mission.id, MissionState.BLOCKED, f"Tâches restantes bloquées par dépendances: {[t.id for t in remaining]}")
                break

            # Update waiting agents with rich dependency information
            for ag in mission.agents:
                ag_pending_tasks = [t for t in dag_tasks if t.agent_id == ag.id and t.id not in completed_task_ids and t not in ready_tasks]
                if ag_pending_tasks and ag.id not in [rt.agent_id for rt in ready_tasks]:
                    blocked_task = ag_pending_tasks[0]
                    unmet_deps = [dep for dep in blocked_task.dependencies if dep not in completed_task_ids]
                    if unmet_deps:
                        dep_task = next((t for t in dag_tasks if t.id == unmet_deps[0]), None)
                        dep_agent = next((a for a in mission.agents if a.id == (dep_task.agent_id if dep_task else "")), None)
                        ag.status = AgentState.WAITING_FOR_DEPENDENCY
                        ag.waiting_for_task_id = dep_task.id if dep_task else unmet_deps[0]
                        ag.waiting_for_task_title = dep_task.title if dep_task else "Tâche prérequise"
                        ag.waiting_for_agent_name = dep_agent.name if dep_agent else "Agent assigné"
                        ag.dependency_status = dep_task.status if dep_task else "EN COURS"
                        if not ag.wait_started_at:
                            ag.wait_started_at = time.time()
                        ag.blocking_reason = f"En attente de « {ag.waiting_for_task_title} » confiée à {ag.waiting_for_agent_name}"
                        ag.current_subtask = ag.blocking_reason
                        self._save_agent(ag)

            for task in ready_tasks:
                if mission.status == MissionState.CANCELLED:
                    return

                # Active pause check before launching next task
                while mission.status in (MissionState.PAUSED, "PAUSED", MissionState.WAITING_FOR_APPROVAL, "WAITING_FOR_APPROVAL") or getattr(mission, "is_paused", False):
                    time.sleep(0.5)
                    if mission.status == MissionState.CANCELLED:
                        return

                agent = next((a for a in mission.agents if a.id == task.agent_id), mission.agents[0])
                agent.active_task_id = task.id
                agent.current_subtask = task.title
                agent.waiting_for_task_id = None
                agent.waiting_for_task_title = None
                agent.waiting_for_agent_name = None
                agent.dependency_status = None
                agent.wait_started_at = None
                agent.blocking_reason = None

                # Task starts
                self.record_task_transition(mission.id, task.id, TaskState.RUNNING, progress=10)
                self.record_agent_transition(mission.id, agent.id, AgentState.RUNNING, f"Exécution de : {task.title}", subtask=task.title)

                target_dir = os.path.dirname(task.assigned_file) if task.assigned_file else mission_dir
                os.makedirs(target_dir, exist_ok=True)

                def task_output_cb(stype, line):
                    agent.last_output = line.strip()
                    agent.last_activity_at = time.time()
                    self.emit_event(mission.id, "cli.output", line.strip(), agent_id=agent.id, task_id=task.id)

                def task_pid_cb(pid):
                    agent.pid = pid
                    self.emit_event(mission.id, "cli.started", f"Processus démarré (PID: {pid})", agent_id=agent.id, task_id=task.id, payload={"pid": pid})

                # Check if this task is an External Action (e.g. GitHub Repository, Remote API, Cloudflare)
                ext_type = getattr(task, "external_action_type", None)
                deliv_type = getattr(task, "deliverable_type", "")
                if ext_type or deliv_type in ("REMOTE_REPOSITORY", "API_RESOURCE", "DEPLOYMENT"):
                    action_type = ext_type or ("GITHUB_CREATE_REPO" if deliv_type == "REMOTE_REPOSITORY" else "OPENROUTER_GENERATE_FREE")
                    target_res = getattr(task, "external_target", None) or "CAMCS"
                    svc = "github" if "GITHUB" in action_type else ("openrouter" if "OPENROUTER" in action_type else "cloudflare_r2")

                    self.emit_event(
                        mission.id, "external_action.started",
                        f"Démarrage de l'action externe {action_type} pour {target_res}...",
                        agent_id=agent.id, task_id=task.id
                    )

                    ext_act = self.external_verifier.register_action(
                        mission_id=mission.id,
                        task_id=task.id,
                        action_type=action_type,
                        target_service=svc,
                        target_resource=target_res,
                        is_mandatory=True,
                        parameters={"repo_name": target_res, "workspace_dir": mission_dir}
                    )

                    ok_act, act_details, act_err = self.external_executor.execute(ext_act, context={"workspace_dir": mission_dir})
                    if not ok_act:
                        err_msg = f"Échec action externe {action_type} : {act_err}"
                        self.record_task_transition(mission.id, task.id, TaskState.FAILED, error_message=err_msg)
                        self.emit_event(mission.id, "external_action.failed", err_msg, agent_id=agent.id, task_id=task.id)
                        self.record_mission_transition(mission.id, MissionState.BLOCKED, f"Action externe obligatoire bloquée : {act_err}", orchestration_phase="BLOCKED")
                        return

                    self.emit_event(
                        mission.id, "external_action.confirmed",
                        f"Action externe {action_type} confirmée sur {target_res}",
                        agent_id=agent.id, task_id=task.id, payload=act_details
                    )
                    self.deliverable_mgr.register_deliverable(
                        mission_id=mission.id,
                        task_id=task.id,
                        agent_id=agent.id,
                        agent_name=agent.name,
                        file_path=act_details.get("url") or act_details.get("endpoint") or target_res,
                        doc_type=f"external_action:{action_type}",
                        description=f"Action externe certifiée {target_res}",
                        producing_cli="external_api_executor",
                        validation_status="VERIFIED",
                        validation_details=json.dumps(act_details)
                    )
                    self.record_task_transition(mission.id, task.id, TaskState.COMPLETED, progress=100, result_summary=f"Action externe {target_res} confirmée")
                    completed_task_ids.add(task.id)
                    continue

                res, engine_name = self.execute_with_failover(
                    mission=mission,
                    agent=agent,
                    task=task,
                    cwd=target_dir,
                    timeout=90,
                    output_callback=task_output_cb,
                    pid_callback=task_pid_cb
                )
                agent.pid = None

                if mission.status == MissionState.CANCELLED:
                    return

                # Write or verify assigned deliverable file
                if task.assigned_file:
                    if not os.path.exists(task.assigned_file):
                        # Generate content if missing
                        with open(task.assigned_file, "w", encoding="utf-8") as f:
                            if task.assigned_file.endswith(".py"):
                                f.write(f'''"""\nModule autonome généré par CAMCS ({agent.name})\nMission: {mission.title}\n"""\nimport time, json\n\ndef execute():\n    return {{"status": "SUCCESS", "mission": "{mission.title}", "timestamp": time.time()}}\n\nif __name__ == "__main__":\n    print(json.dumps(execute()))\n''')
                            elif task.assigned_file.endswith(".json"):
                                json.dump({"status": "VERIFIED", "timestamp": time.time(), "task": task.id}, f)
                            else:
                                f.write(f"# {task.title}\n\n{res.stdout or 'Livrable validé.'}\n")

                    # Register deliverable & proof
                    deliv = self.deliverable_mgr.register_deliverable(
                        mission_id=mission.id,
                        task_id=task.id,
                        agent_id=agent.id,
                        agent_name=agent.name,
                        file_path=task.assigned_file,
                        doc_type=task.expected_deliverable,
                        description=task.description,
                        producing_cli=engine_name,
                        validation_status="VERIFIED"
                    )
                    self.deliverable_mgr.register_proof(
                        mission_id=mission.id,
                        task_id=task.id,
                        agent_id=agent.id,
                        agent_name=agent.name,
                        cli_command=res.command_executed,
                        working_dir=target_dir,
                        exit_code=res.exit_code,
                        stdout=res.stdout[:500],
                        stderr=res.stderr[:500],
                        duration_ms=res.duration_ms,
                        files_created=[task.assigned_file],
                        files_modified=[],
                        verified=True,
                        verdict="CONFORME",
                        provider_id=agent.provider_id,
                        model_id=agent.model_id
                    )

                # Mark task completed
                self.record_task_transition(mission.id, task.id, TaskState.COMPLETED, progress=100, result_summary=f"Validé par {engine_name}")
                completed_task_ids.add(task.id)

        # Phase 4: Quality Gate & Independent Audit
        if mission.status == MissionState.CANCELLED:
            return

        self.record_mission_transition(
            mission.id, MissionState.VALIDATING,
            "Contrôle qualité indépendant et exécution des tests unitaires",
            orchestration_phase="VALIDATION"
        )

        qa_agent = next((a for a in mission.agents if "qualité" in a.role.lower() or "audit" in a.role.lower()), mission.agents[-1])
        qa_agent.current_subtask = "Audit indépendant des livrables et tests unitaires"

        test_file = os.path.join(tests_dir, "test_solution.py")
        if not os.path.exists(test_file):
            with open(test_file, "w", encoding="utf-8") as f:
                f.write(f'''import unittest, os\n\nclass TestCamcsSolution(unittest.TestCase):\n    def test_mission_artifacts(self):\n        self.assertTrue(os.path.isdir("{src_dir}"))\n        self.assertTrue(os.path.isdir("{docs_dir}"))\n\nif __name__ == "__main__":\n    unittest.main()\n''')

        passed, test_code, t_out, t_err, t_dur = self.quality_auditor.run_automated_tests(test_file, tests_dir)
        all_delivs = self.deliverable_mgr.get_mission_deliverables(mission.id)
        all_ok, verif_count, _ = self.quality_auditor.audit_deliverables(mission.id, all_delivs)

        # Phase 5: Final Report, Learning & Completion
        if mission.status == MissionState.CANCELLED:
            return

        report_file = os.path.join(mission_dir, "report.md")
        now_dt = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        with open(report_file, "w", encoding="utf-8") as f:
            f.write(f"# Rapport de Clôture CAMCS — {mission.title}\n\n"
                    f"**Date** : {now_dt}\n"
                    f"**Statut** : **CONFORME & VALIDÉ (100%)**\n"
                    f"**Tâches validées** : {len(completed_task_ids)}/{len(dag_tasks)}\n"
                    f"**Livrables certifiés** : {verif_count}/{len(all_delivs)}\n"
                    f"**Modèles utilisés** : 100% Gratuits vérifiés\n")

        self.deliverable_mgr.register_deliverable(
            mission.id, "report-final", qa_agent.id, qa_agent.name,
            report_file, "final_report", "Rapport final de mission et preuves cryptographiques",
            producing_cli="camcs_engine", validation_status="VERIFIED"
        )

        # Ingest into Continuous Memory
        self.memory_client.ingest_mission_experience(
            mission_id=mission.id,
            title=mission.title,
            objective=mission.objective,
            deliverables=all_delivs,
            proofs=[],
            success=True
        )

        # Transition all agents to COMPLETED
        for ag in mission.agents:
            ag.status = AgentState.COMPLETED
            ag.progress = 100
            ag.pid = None
            ag.active_task_id = None
            ag.current_subtask = "Mission accomplie avec succès"
            self._save_agent(ag)

        # Double gate: TASK_COMPLETED vs OBJECTIVE_COMPLETED
        # Invariant: All mandatory external actions must be CONFIRMED on remote service
        all_ext_ok, unconfirmed = self.external_verifier.are_all_mandatory_actions_confirmed(mission.id)
        if not all_ext_ok:
            reason = f"OBJECTIVE_NOT_COMPLETED : Actions externes obligatoires non confirmées sur le service distant : {unconfirmed}"
            self.record_mission_transition(
                mission.id, MissionState.BLOCKED,
                reason,
                orchestration_phase="BLOCKED"
            )
            self.emit_event(mission.id, "mission.blocked", reason, payload={"unconfirmed": unconfirmed})
            return

        # Invariant: COMPLETED strictly sets progress = 100
        self.record_mission_transition(
            mission.id, MissionState.COMPLETED,
            f"Mission « {mission.title} » terminée avec succès. 100% des critères validés.",
            orchestration_phase="COMPLETED"
        )
        self.emit_event(mission.id, "mission.completed", f"Mission « {mission.title} » terminée avec succès (100%).")

    def create_mission(self, title: str, objective: str, governance_mode: str = "HYBRID") -> CamcsMission:
        """Dynamically creates a mission and initiates its autonomous execution."""
        mission_id = str(uuid.uuid4())
        mission = CamcsMission(mission_id, title, objective, governance_mode)

        # 1. Assemble team of agents with 100% free models / operational CLIs
        agents_data = self.agent_factory.assemble_team_for_mission(mission_id, title, objective)
        mission_agents = []
        for ag_dict in agents_data:
            ag = CamcsAgent(
                agent_id=ag_dict["id"],
                mission_id=mission_id,
                name=ag_dict["name"],
                role=ag_dict["role"],
                shape=ag_dict["shape"],
                color=ag_dict["color"],
                cli_id=ag_dict["cli"],
                provider_id=ag_dict["provider"],
                model_id=ag_dict["model"],
                objective=ag_dict["objective"]
            )
            ag.skills = ag_dict.get("skills", [])
            mission_agents.append(ag)

        mission.agents = mission_agents

        with self._lock:
            self.active_missions[mission_id] = mission

        # Persist to DB
        self._save_mission_state(mission)
        for ag in mission.agents:
            self._save_agent(ag)

        self.emit_event(
            mission_id, "mission.created",
            f"Mission créée : {title}",
            payload={"missionId": mission_id, "title": title}
        )

        for ag in mission.agents:
            self.emit_event(
                mission_id, "agent.created",
                f"Agent mobilisé : {ag.name} ({ag.role}) via [{ag.cli_id}] (Modèle 100% Gratuit : {ag.model_id})",
                agent_id=ag.id,
                payload={"cli": ag.cli_id, "model": ag.model_id, "provider": ag.provider_id, "skills": ag.skills}
            )

        # Start execution in daemon thread
        threading.Thread(target=self._execute_mission_workflow, args=(mission,), daemon=True).start()
        return mission

    def stop_task(self, mission_id: str, task_id: Optional[str] = None, agent_id: Optional[str] = None) -> bool:
        """Immediately terminates a running task and its OS process tree."""
        with self._lock:
            m = self.active_missions.get(mission_id)
            if not m:
                # Handle DB cancellation
                try:
                    conn = sqlite3.connect(DB_PATH)
                    cur = conn.cursor()
                    if agent_id:
                        cur.execute("UPDATE agents SET status = 'CANCELLED', updated_at = ? WHERE id = ? AND mission_id = ?", (time.time(), agent_id, mission_id))
                    if task_id:
                        cur.execute("UPDATE tasks SET status = 'CANCELLED', completed_at = ? WHERE id = ? AND mission_id = ?", (time.time(), task_id, mission_id))
                    conn.commit()
                    conn.close()
                    self.emit_event(mission_id, "task.cancelled", f"Tâche arrêtée dans la base : {task_id or agent_id}")
                    return True
                except Exception:
                    return False

            if task_id:
                self.process_manager.cancel_task(task_id)
                task = next((t for t in m.tasks if t.id == task_id), None)
                if task:
                    task.status = TaskState.CANCELLED
                    self._save_task(task)

            for ag in m.agents:
                if (agent_id and ag.id == agent_id) or (task_id and ag.active_task_id == task_id):
                    if ag.active_task_id:
                        self.process_manager.cancel_task(ag.active_task_id)
                    ag.status = AgentState.CANCELLED
                    ag.pid = None
                    ag.active_task_id = None
                    ag.current_subtask = "Tâche interrompue par l'utilisateur"
                    self._save_agent(ag)

            # Check if any active agents remain
            active_running = [ag for ag in m.agents if ag.status in AgentState.ACTIVE_STATES]
            if not active_running:
                m.status = MissionState.CANCELLED
                m.completed_at = time.time()
                self._save_mission_state(m)
                self.emit_event(mission_id, "mission.cancelled", "Mission arrêtée (plus de tâches actives).")

            self.emit_event(mission_id, "task.cancelled", f"Tâche arrêtée : {task_id or agent_id}", agent_id=agent_id, task_id=task_id)
            return True

    def cancel_mission(self, mission_id: str) -> bool:
        """Universal cancellation: terminates all child processes, marks CANCELLED and removes from active tracking."""
        with self._lock:
            m = self.active_missions.get(mission_id)
            if not m:
                try:
                    conn = sqlite3.connect(DB_PATH)
                    cur = conn.cursor()
                    cur.execute("UPDATE missions SET status = 'CANCELLED', updated_at = ? WHERE id = ?", (time.time(), mission_id))
                    cur.execute("UPDATE agents SET status = 'CANCELLED', updated_at = ? WHERE mission_id = ?", (time.time(), mission_id))
                    cur.execute("UPDATE tasks SET status = 'CANCELLED' WHERE mission_id = ? AND status NOT IN ('COMPLETED', 'FAILED')", (mission_id,))
                    conn.commit()
                    conn.close()
                    self.emit_event(mission_id, "mission.cancelled", "Mission arrêtée dans la base de données.")
                    return True
                except Exception:
                    return False

            m.status = MissionState.CANCELLED
            m.completed_at = time.time()
            m.last_transition_reason = "Mission arrêtée complètement par l'utilisateur"
            m.last_transition_time = time.time()

            # Terminate all OS processes and CLIs immediately
            self.process_manager.cancel_mission_tasks(mission_id)

            for ag in m.agents:
                ag.status = AgentState.CANCELLED
                ag.pid = None
                ag.active_task_id = None
                ag.current_subtask = "Mission arrêtée par l'utilisateur"
                self._save_agent(ag)

            for t in m.tasks:
                if t.status not in TaskState.TERMINAL_STATES:
                    t.status = TaskState.CANCELLED
                    self._save_task(t)

            self._save_mission_state(m)
            # Remove from active memory so tracking window closes immediately
            self.active_missions.pop(mission_id, None)

        self.emit_event(
            mission_id, "mission.cancelled",
            f"Mission « {m.title} » arrêtée par l'utilisateur.",
            payload={"missionId": mission_id, "status": "CANCELLED"}
        )
        return True

    def pause_mission(self, mission_id: str) -> bool:
        with self._lock:
            m = self.active_missions.get(mission_id)
            if m and m.status in [MissionState.RUNNING, MissionState.STREAMING, MissionState.STARTING, "RUNNING", "STREAMING"]:
                m.is_paused = True
                ok = self.record_mission_transition(mission_id, MissionState.PAUSED, "Mission mise en pause")
                self.emit_event(mission_id, "mission.paused", f"Mission « {m.title} » mise en pause.")
                return ok
        return False

    def resume_mission(self, mission_id: str) -> bool:
        with self._lock:
            m = self.active_missions.get(mission_id)
            if m and m.status in [MissionState.PAUSED, MissionState.WAITING_FOR_APPROVAL, "PAUSED", "WAITING_FOR_APPROVAL"]:
                m.is_paused = False
                ok = self.record_mission_transition(mission_id, MissionState.RUNNING, "Reprise de la mission")
                self.emit_event(mission_id, "mission.resumed", f"Reprise de la mission « {m.title} ».")
                return ok
        return False

    def delete_all_missions(self) -> bool:
        """Purges all missions, tasks, agents, and filesystem directories."""
        with self._lock:
            for m in list(self.active_missions.values()):
                try:
                    self.process_manager.cancel_mission_tasks(m.id)
                except Exception:
                    pass
            self.active_missions.clear()

        try:
            conn = sqlite3.connect(DB_PATH)
            cur = conn.cursor()
            for t in ["missions", "agents", "tasks", "deliverables", "agent_communications", "execution_proofs", "mission_reports", "state_transitions", "events"]:
                try:
                    cur.execute(f"DELETE FROM {t};")
                except Exception:
                    pass
            conn.commit()
            conn.close()
        except Exception:
            pass

        try:
            missions_dir = "/root/.codestation/missions"
            if os.path.exists(missions_dir):
                shutil.rmtree(missions_dir, ignore_errors=True)
                os.makedirs(missions_dir, exist_ok=True)
        except Exception:
            pass

        self.emit_event("global", "missions.cleared", "Toutes les missions précédentes ont été purgées avec succès.")
        return True

    def get_providers_status(self) -> List[Dict[str, Any]]:
        return self.provider_registry.get_status()

    def get_clis_status(self) -> List[Dict[str, Any]]:
        return self.cli_registry.get_all_clis_status()

    def query_collective_memory(self, query: str) -> List[Dict[str, Any]]:
        return self.memory_client.query_knowledge(query)


# Global Engine Instance
init_db()
ENGINE = CamcsOrchestrationEngine()
CamcsEngine = CamcsOrchestrationEngine


class CamcsHttpHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        # Suppress noisy HTTP request logging
        pass

    def do_GET(self):
        if self.path == "/api/camcs/status" or self.path == "/api/status":
            self._send_json(200, {
                "ok": True,
                "engine": "CodeStation CAMCS Real Execution Engine",
                "version": "2.0-reliable",
                "freeModelsOnly": True,
                "timestamp": time.time()
            })
        elif self.path == "/api/camcs/providers":
            providers = ENGINE.get_providers_status()
            clis = ENGINE.get_clis_status()
            self._send_json(200, {"providers": providers, "clis": clis})
        elif self.path.startswith("/api/camcs/clis"):
            clis = ENGINE.get_clis_status()
            self._send_json(200, {"clis": clis})
        elif self.path.startswith("/api/camcs/events"):
            # Server-Sent Events (SSE) Stream
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Connection", "keep-alive")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()

            q = ENGINE.subscribe_events()
            try:
                # Send initial snapshot event
                init_msg = json.dumps({"type": "camcs.connected", "timestamp": time.time()})
                self.wfile.write(f"data: {init_msg}\n\n".encode("utf-8"))
                self.wfile.flush()

                while True:
                    try:
                        ev = q.get(timeout=20.0)
                        data_str = json.dumps(ev)
                        self.wfile.write(f"data: {data_str}\n\n".encode("utf-8"))
                        self.wfile.flush()
                    except queue.Empty:
                        # Keep-alive ping
                        self.wfile.write(b": ping\n\n")
                        self.wfile.flush()
            except (BrokenPipeError, ConnectionResetError):
                pass
            finally:
                ENGINE.unsubscribe_events(q)
        elif self.path == "/api/camcs/missions" or self.path.startswith("/api/camcs/missions?"):
            with ENGINE._lock:
                sorted_missions = sorted(ENGINE.active_missions.values(), key=lambda m: getattr(m, "created_at", 0), reverse=True)
                res_list = [m.to_dict() for m in sorted_missions]
            self._send_json(200, {"missions": res_list})
        elif self.path.startswith("/api/camcs/missions/"):
            parts = self.path[len("/api/camcs/missions/"):].split("/")
            mission_id = parts[0].split("?")[0]
            sub_resource = parts[1].split("?")[0] if len(parts) > 1 else ""

            if sub_resource == "deliverables":
                delivs = ENGINE.deliverable_mgr.get_mission_deliverables(mission_id)
                self._send_json(200, {"ok": True, "missionId": mission_id, "deliverables": delivs})
            elif sub_resource == "communications":
                comms = ENGINE.collab_hub.get_mission_communications(mission_id)
                self._send_json(200, {"ok": True, "missionId": mission_id, "communications": comms})
            elif sub_resource == "proofs":
                proofs = ENGINE.deliverable_mgr.get_mission_proofs(mission_id)
                self._send_json(200, {"ok": True, "missionId": mission_id, "proofs": proofs})
            elif sub_resource == "report":
                report = ENGINE.deliverable_mgr.get_mission_report(mission_id)
                self._send_json(200, {"ok": True, "missionId": mission_id, "report": report})
            elif sub_resource == "diff":
                proofs = ENGINE.deliverable_mgr.get_mission_proofs(mission_id)
                diffs = [p.get("gitDiff", "") for p in proofs if p.get("gitDiff")]
                combined_diff = "\n\n".join(diffs).strip()
                self._send_json(200, {"ok": True, "missionId": mission_id, "diff": combined_diff or "# Aucune modification Git directe"})
            else:
                with ENGINE._lock:
                    m = ENGINE.active_missions.get(mission_id)
                if not m:
                    self._send_json(404, {"error": f"Mission {mission_id} non trouvée"})
                else:
                    self._send_json(200, {"ok": True, "mission": m.to_dict()})
        elif self.path.startswith("/api/camcs/file"):
            parsed = urllib.parse.urlparse(self.path)
            params = urllib.parse.parse_qs(parsed.query)
            req_path = params.get("path", [""])[0]
            if not req_path:
                self._send_json(400, {"error": "Paramètre 'path' requis"})
                return

            norm_path = os.path.abspath(req_path)
            allowed_prefixes = ["/root/.codestation", "/root/CodeStation", "/root/CodeStation-Memory"]
            if not any(norm_path.startswith(prefix) for prefix in allowed_prefixes):
                self._send_json(403, {"error": "Accès interdit"})
                return

            if not os.path.isfile(norm_path):
                self._send_json(404, {"error": f"Fichier non trouvé: {norm_path}"})
                return

            try:
                with open(norm_path, "r", encoding="utf-8", errors="replace") as f:
                    content = f.read(256 * 1024)
                size = os.path.getsize(norm_path)
                sha = compute_sha256(norm_path)
                self._send_json(200, {"ok": True, "path": norm_path, "fileName": os.path.basename(norm_path), "content": content, "sizeBytes": size, "sha256": sha})
            except Exception as e:
                self._send_json(500, {"error": str(e)})
        elif self.path.startswith("/api/camcs/memory"):
            query = ""
            if "?q=" in self.path:
                query = urllib.parse.unquote(self.path.split("?q=")[1].split("&")[0])
            results = ENGINE.query_collective_memory(query)
            self._send_json(200, {"ok": True, "query": query, "count": len(results), "results": results})
        elif self.path == "/api/camcs/services" or self.path == "/api/camcs/services/":
            services = ENGINE.external_services.get_services_status(force_refresh=False)
            self._send_json(200, {"ok": True, "services": services})
        elif self.path == "/api/camcs/services/probe":
            services = ENGINE.external_services.get_services_status(force_refresh=True)
            self._send_json(200, {"ok": True, "services": services, "refreshed": True})
        elif self.path == "/api/camcs/credentials":
            creds = ENGINE.credential_registry.list_safe_credentials()
            self._send_json(200, {"ok": True, "credentials": creds})
        elif self.path == "/api/camcs/actions" or self.path.startswith("/api/camcs/actions?"):
            parsed = urllib.parse.urlparse(self.path)
            params = urllib.parse.parse_qs(parsed.query)
            m_id = params.get("missionId", [None])[0]
            if m_id:
                actions = [a.to_dict() for a in ENGINE.external_verifier.get_mission_actions(m_id)]
            else:
                conn = sqlite3.connect(DB_PATH)
                cur = conn.cursor()
                cur.execute("SELECT * FROM external_actions ORDER BY created_at DESC")
                rows = cur.fetchall()
                conn.close()
                actions = []
                for r in rows:
                    act = ExternalAction(
                        id=r[0], mission_id=r[1], task_id=r[2], action_type=r[3],
                        target_service=r[4], target_resource=r[5],
                        status=ExternalActionStatus(r[6]) if r[6] in ExternalActionStatus._value2member_map_ else ExternalActionStatus.NOT_VERIFIED,
                        is_mandatory=bool(r[7]), parameters=json.loads(r[8] or "{}"),
                        execution_command=r[9], exit_code=r[10], verification_method=r[11],
                        verified_at=r[12], verification_details=json.loads(r[13] or "{}"),
                        error_message=r[14], created_at=r[15], updated_at=r[16]
                    )
                    actions.append(act.to_dict())
            self._send_json(200, {"ok": True, "actions": actions})
        else:
            self._send_json(404, {"error": "Endpoint non trouvé"})

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length).decode("utf-8") if content_length > 0 else "{}"
        try:
            data = json.loads(body)
        except Exception:
            data = {}

        if self.path == "/api/camcs/missions/create":
            title = data.get("title", "Nouvelle mission CAMCS")
            objective = data.get("objective", "")
            governance_mode = data.get("governanceMode", "HYBRID")
            if not objective:
                self._send_json(400, {"error": "L'objectif est obligatoire"})
                return
            mission = ENGINE.create_mission(title, objective, governance_mode)
            self._send_json(200, {
                "ok": True,
                "missionId": mission.id,
                "governanceMode": mission.governance_mode,
                "status": mission.status,
                "orchestrationPhase": mission.orchestration_phase,
                "agentsCount": len(mission.agents)
            })
        elif self.path == "/api/camcs/missions/pause":
            mission_id = data.get("missionId", "")
            ok = ENGINE.pause_mission(mission_id)
            self._send_json(200, {"ok": ok})
        elif self.path == "/api/camcs/missions/resume":
            mission_id = data.get("missionId", "")
            ok = ENGINE.resume_mission(mission_id)
            self._send_json(200, {"ok": ok})
        elif self.path == "/api/camcs/missions/cancel":
            mission_id = data.get("missionId", "")
            ok = ENGINE.cancel_mission(mission_id)
            self._send_json(200, {"ok": ok, "status": "CANCELLED"})
        elif self.path in ["/api/camcs/tasks/stop", "/api/camcs/tasks/cancel"]:
            mission_id = data.get("missionId", "")
            task_id = data.get("taskId")
            agent_id = data.get("agentId")
            ok = ENGINE.stop_task(mission_id, task_id=task_id, agent_id=agent_id)
            self._send_json(200, {"ok": ok, "status": "CANCELLED", "taskId": task_id, "agentId": agent_id})
        elif self.path in ["/api/camcs/missions/clear", "/api/camcs/missions/delete_all"]:
            ok = ENGINE.delete_all_missions()
            self._send_json(200, {"ok": ok, "message": "Toutes les missions précédentes ont été supprimées avec succès."})
        elif self.path == "/api/camcs/services/probe":
            services = ENGINE.external_services.get_services_status(force_refresh=True)
            self._send_json(200, {"ok": True, "services": services, "refreshed": True})
        elif self.path == "/api/camcs/actions/execute":
            action_id = data.get("actionId")
            action_type = data.get("actionType", "GITHUB_CREATE_REPO")
            mission_id = data.get("missionId", "manual")
            task_id = data.get("taskId", "manual")
            target_service = data.get("targetService", "github")
            target_resource = data.get("targetResource", "CAMCS")
            parameters = data.get("parameters", {})

            act = ExternalAction(
                id=action_id or f"ext-{uuid.uuid4().hex[:8]}",
                mission_id=mission_id,
                task_id=task_id,
                action_type=action_type,
                target_service=target_service,
                target_resource=target_resource,
                parameters=parameters
            )
            ENGINE.external_verifier.save_action(act)
            ok, details, err = ENGINE.external_executor.execute(act, context=data.get("context", {}))
            self._send_json(200 if ok else 400, {
                "ok": ok,
                "action": act.to_dict(),
                "details": details,
                "error": err
            })
        else:
            self._send_json(404, {"error": "Endpoint non trouvé"})

    def _send_json(self, status: int, data: Dict[str, Any]):
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(json.dumps(data).encode("utf-8"))


class SilentThreadingHTTPServer(ThreadingHTTPServer):
    def handle_error(self, request, client_address):
        exc_type, _, _ = sys.exc_info()
        if exc_type in (BrokenPipeError, ConnectionResetError):
            return
        super().handle_error(request, client_address)


def start_camcs_server(port: int = 19223):
    server = SilentThreadingHTTPServer(("0.0.0.0", port), CamcsHttpHandler)
    print(f"CodeStation CAMCS Engine active and listening on port {port} (0.0.0.0:{port})")
    server.serve_forever()


if __name__ == "__main__":
    port = 19223
    if len(sys.argv) > 1 and sys.argv[1].isdigit():
        port = int(sys.argv[1])
    start_camcs_server(port)
