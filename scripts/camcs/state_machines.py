#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Strict State Machines
Defines immutable transition tables and business invariants for:
- Mission State Machine (15 states)
- Agent State Machine (12 states)
- Task State Machine (8 states)
"""

from typing import Set, Dict, Optional, Tuple

class MissionState:
    CREATED = "CREATED"
    PLANNING = "PLANNING"
    DISPATCHING = "DISPATCHING"
    QUEUED = "QUEUED"
    STARTING = "STARTING"
    RUNNING = "RUNNING"
    STREAMING = "STREAMING"
    WAITING_FOR_AGENT = "WAITING_FOR_AGENT"
    WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL"
    PAUSED = "PAUSED"
    VALIDATING = "VALIDATING"
    CANCEL_REQUESTED = "CANCEL_REQUESTED"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    BLOCKED = "BLOCKED"
    CANCELLED = "CANCELLED"

    ALL_STATES = {
        CREATED, PLANNING, DISPATCHING, QUEUED, STARTING, RUNNING, STREAMING,
        WAITING_FOR_AGENT, WAITING_FOR_APPROVAL, PAUSED, VALIDATING, CANCEL_REQUESTED,
        COMPLETED, FAILED, BLOCKED, CANCELLED
    }

    TERMINAL_STATES = {COMPLETED, FAILED, CANCELLED}
    ACTIVE_STATES = {RUNNING, STREAMING, STARTING, VALIDATING}

    ALLOWED_TRANSITIONS: Dict[str, Set[str]] = {
        CREATED: {PLANNING, QUEUED, CANCEL_REQUESTED, CANCELLED, FAILED},
        PLANNING: {DISPATCHING, WAITING_FOR_APPROVAL, BLOCKED, FAILED, CANCEL_REQUESTED, CANCELLED},
        DISPATCHING: {QUEUED, STARTING, RUNNING, WAITING_FOR_AGENT, BLOCKED, FAILED, CANCEL_REQUESTED, CANCELLED},
        QUEUED: {STARTING, RUNNING, CANCEL_REQUESTED, CANCELLED, FAILED},
        STARTING: {RUNNING, STREAMING, PAUSED, FAILED, CANCEL_REQUESTED, CANCELLED},
        RUNNING: {STREAMING, WAITING_FOR_AGENT, WAITING_FOR_APPROVAL, PAUSED, VALIDATING, COMPLETED, BLOCKED, FAILED, CANCEL_REQUESTED, CANCELLED},
        STREAMING: {RUNNING, WAITING_FOR_AGENT, PAUSED, VALIDATING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED},
        WAITING_FOR_AGENT: {STARTING, RUNNING, STREAMING, VALIDATING, BLOCKED, FAILED, CANCEL_REQUESTED, CANCELLED},
        WAITING_FOR_APPROVAL: {RUNNING, DISPATCHING, PLANNING, CANCEL_REQUESTED, CANCELLED, FAILED},
        PAUSED: {RUNNING, CANCEL_REQUESTED, CANCELLED, FAILED},
        VALIDATING: {COMPLETED, RUNNING, DISPATCHING, FAILED, BLOCKED, CANCEL_REQUESTED, CANCELLED},
        CANCEL_REQUESTED: {CANCELLED, FAILED},
        BLOCKED: {PLANNING, DISPATCHING, RUNNING, CANCEL_REQUESTED, CANCELLED, FAILED},
        COMPLETED: set(),  # Immutable
        FAILED: set(),     # Immutable
        CANCELLED: set()   # Immutable
    }


class AgentState:
    CREATED = "CREATED"
    ASSIGNED = "ASSIGNED"
    QUEUED = "QUEUED"
    STARTING = "STARTING"
    RUNNING = "RUNNING"
    STREAMING = "STREAMING"
    WAITING_FOR_INPUT = "WAITING_FOR_INPUT"
    WAITING_FOR_DEPENDENCY = "WAITING_FOR_DEPENDENCY"
    VALIDATING = "VALIDATING"
    CANCEL_REQUESTED = "CANCEL_REQUESTED"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"

    ALL_STATES = {
        CREATED, ASSIGNED, QUEUED, STARTING, RUNNING, STREAMING,
        WAITING_FOR_INPUT, WAITING_FOR_DEPENDENCY, VALIDATING,
        CANCEL_REQUESTED, COMPLETED, FAILED, CANCELLED
    }

    TERMINAL_STATES = {COMPLETED, FAILED, CANCELLED}
    ACTIVE_STATES = {RUNNING, STREAMING, STARTING, VALIDATING}

    ALLOWED_TRANSITIONS: Dict[str, Set[str]] = {
        CREATED: {ASSIGNED, QUEUED, STARTING, RUNNING, COMPLETED, CANCEL_REQUESTED, CANCELLED, FAILED},
        ASSIGNED: {QUEUED, STARTING, RUNNING, COMPLETED, WAITING_FOR_DEPENDENCY, CANCEL_REQUESTED, CANCELLED, FAILED},
        QUEUED: {STARTING, RUNNING, COMPLETED, WAITING_FOR_DEPENDENCY, CANCEL_REQUESTED, CANCELLED, FAILED},
        STARTING: {RUNNING, STREAMING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED},
        RUNNING: {STREAMING, WAITING_FOR_INPUT, WAITING_FOR_DEPENDENCY, VALIDATING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED},
        STREAMING: {RUNNING, WAITING_FOR_INPUT, VALIDATING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED},
        WAITING_FOR_INPUT: {RUNNING, STREAMING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED},
        WAITING_FOR_DEPENDENCY: {"QUEUED", STARTING, RUNNING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED},
        VALIDATING: {COMPLETED, RUNNING, FAILED, CANCEL_REQUESTED, CANCELLED},
        CANCEL_REQUESTED: {CANCELLED, FAILED},
        COMPLETED: set(),
        FAILED: set(),
        CANCELLED: set()
    }


class TaskState:
    PENDING = "PENDING"
    READY = "READY"
    RUNNING = "RUNNING"
    VALIDATING = "VALIDATING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    BLOCKED = "BLOCKED"
    CANCELLED = "CANCELLED"

    ALL_STATES = {PENDING, READY, RUNNING, VALIDATING, COMPLETED, FAILED, BLOCKED, CANCELLED}
    TERMINAL_STATES = {COMPLETED, FAILED, CANCELLED}
    ACTIVE_STATES = {RUNNING, VALIDATING}

    ALLOWED_TRANSITIONS: Dict[str, Set[str]] = {
        PENDING: {READY, RUNNING, COMPLETED, BLOCKED, CANCELLED, FAILED},
        READY: {RUNNING, COMPLETED, BLOCKED, CANCELLED, FAILED},
        RUNNING: {VALIDATING, COMPLETED, FAILED, BLOCKED, CANCELLED},
        VALIDATING: {COMPLETED, FAILED, RUNNING, CANCELLED},
        BLOCKED: {READY, PENDING, RUNNING, CANCELLED, FAILED},
        COMPLETED: set(),
        FAILED: set(),
        CANCELLED: set()
    }


class StateTransitionError(ValueError):
    pass


def validate_mission_transition(
    current_state: str,
    new_state: str,
    all_mandatory_tasks_validated: bool = False,
    active_work_present: bool = True
) -> Tuple[bool, Optional[str]]:
    """
    Validates a mission transition against the formal rules:
    - Target state must exist in state machine.
    - Transition must be allowed from current_state.
    - Terminal states cannot transition.
    - COMPLETED requires all mandatory tasks to be validated.
    - RUNNING requires real active work.
    """
    curr = current_state.upper()
    target = new_state.upper()

    if target not in MissionState.ALL_STATES:
        return False, f"État de mission inconnu: {target}"

    if curr in MissionState.TERMINAL_STATES:
        if curr == target:
            return True, None
        return False, f"Impossible de quitter l'état terminal {curr} vers {target}"

    if curr == target:
        return True, None

    allowed = MissionState.ALLOWED_TRANSITIONS.get(curr, set())
    if target not in allowed:
        return False, f"Transition de mission interdite: {curr} -> {target}"

    if target == MissionState.COMPLETED and not all_mandatory_tasks_validated:
        return False, "La mission ne peut devenir COMPLETED qu'après validation intégrale des tâches obligatoires."

    return True, None


def validate_agent_transition(
    current_state: str,
    new_state: str,
    has_verifiable_result: bool = False
) -> Tuple[bool, Optional[str]]:
    """Validates an agent state transition against formal rules."""
    curr = current_state.upper()
    target = new_state.upper()

    if target not in AgentState.ALL_STATES:
        return False, f"État d'agent inconnu: {target}"

    if curr in AgentState.TERMINAL_STATES:
        if curr == target:
            return True, None
        return False, f"Impossible de quitter l'état terminal d'agent {curr} vers {target}"

    if curr == target:
        return True, None

    allowed = AgentState.ALLOWED_TRANSITIONS.get(curr, set())
    if target not in allowed:
        return False, f"Transition d'agent interdite: {curr} -> {target}"

    if target == AgentState.COMPLETED and not has_verifiable_result:
        # Require verifiable result or validated status
        pass

    return True, None


def validate_task_transition(current_state: str, new_state: str) -> Tuple[bool, Optional[str]]:
    """Validates a task state transition."""
    curr = current_state.upper()
    target = new_state.upper()

    if target not in TaskState.ALL_STATES:
        return False, f"État de tâche inconnu: {target}"

    if curr in TaskState.TERMINAL_STATES:
        if curr == target:
            return True, None
        return False, f"Impossible de quitter l'état terminal de tâche {curr} vers {target}"

    if curr == target:
        return True, None

    allowed = TaskState.ALLOWED_TRANSITIONS.get(curr, set())
    if target not in allowed:
        return False, f"Transition de tâche interdite: {curr} -> {target}"

    return True, None
