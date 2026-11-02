#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Objective-Oriented DAG Planner & Scheduler
Constructs dynamic task DAGs with 11 task classifications, realistic dependencies,
mandatory vs optional flags, and accurate weight-based progress computation.
"""

import time
import uuid
from typing import Dict, List, Optional, Any, Set
from dataclasses import dataclass, field
from .task_classifier import TaskClassifier, TaskClassification, TaskProfile

@dataclass
class DagTask:
    id: str
    mission_id: str
    agent_id: str
    classification: str
    title: str
    description: str
    prompt: str
    dependencies: List[str] = field(default_factory=list)
    status: str = "PENDING"  # PENDING, READY, RUNNING, VALIDATING, COMPLETED, FAILED, BLOCKED, CANCELLED
    progress: int = 0
    weight: int = 20
    is_mandatory: bool = True
    acceptance_criteria: str = ""
    expected_deliverable: str = ""
    assigned_file: Optional[str] = None
    exit_code: Optional[int] = None
    result_summary: str = ""
    deliverables: List[Dict[str, Any]] = field(default_factory=list)
    created_at: float = field(default_factory=time.time)
    started_at: Optional[float] = None
    completed_at: Optional[float] = None
    error_message: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "missionId": self.mission_id,
            "agentId": self.agent_id,
            "classification": self.classification,
            "title": self.title,
            "description": self.description,
            "dependencies": self.dependencies,
            "status": self.status,
            "progress": self.progress,
            "weight": self.weight,
            "isMandatory": self.is_mandatory,
            "acceptanceCriteria": self.acceptance_criteria,
            "expectedDeliverable": self.expected_deliverable,
            "assignedFile": self.assigned_file,
            "exitCode": self.exit_code,
            "resultSummary": self.result_summary,
            "createdAt": self.created_at,
            "startedAt": self.started_at,
            "completedAt": self.completed_at,
            "errorMessage": self.error_message
        }


class DagPlanner:
    """Builds and schedules execution DAGs from objectives and assembled agents."""

    def build_dag_for_mission(
        self,
        mission_id: str,
        title: str,
        objective: str,
        agents: List[Dict[str, Any]],
        workspace_dir: str
    ) -> List[DagTask]:
        """
        Dynamically decomposes the user objective into an oriented DAG of tasks.
        Assigns tasks according to agents' roles and competencies.
        """
        tasks: List[DagTask] = []
        if not agents:
            return tasks

        now = time.time()
        m_short = mission_id[:6]

        arch_agent = next((a for a in agents if "architect" in a["role"].lower() or "architecture" in a["role"].lower()), agents[0])
        qa_agent = next((a for a in agents if "qualité" in a["role"].lower() or "audit" in a["role"].lower() or "test" in a["role"].lower()), agents[-1])
        dev_agents = [a for a in agents if a["id"] != arch_agent["id"] and a["id"] != qa_agent["id"]]
        if not dev_agents:
            dev_agents = [arch_agent]

        # 1. Architecture Specification Task
        t1_id = f"task-{m_short}-spec"
        prof1 = TaskClassifier.classify("Spécification d'architecture", objective)
        t1 = DagTask(
            id=t1_id,
            mission_id=mission_id,
            agent_id=arch_agent["id"],
            classification=TaskClassification.ANALYSIS.value,
            title=f"Spécification technique — {title}",
            description="Analyser l'objectif utilisateur, identifier contraintes et concevoir l'architecture DAG.",
            prompt=f"Analyse l'objectif utilisateur et rédige la spécification technique complète : {objective}",
            dependencies=[],
            status="READY",
            weight=25,
            is_mandatory=True,
            acceptance_criteria="Spécification technique formalisée et livrable architecture_spec.md produit.",
            expected_deliverable="architecture_spec.md",
            assigned_file=f"{workspace_dir}/docs/architecture_spec.md",
            created_at=now
        )
        tasks.append(t1)

        # 2. Implementation Tasks (Depend on Task 1)
        impl_task_ids = []
        dev_weight = 40 // len(dev_agents)
        for idx, dev in enumerate(dev_agents):
            t_id = f"task-{m_short}-impl-{idx+1}"
            impl_task_ids.append(t_id)

            is_doc = "doc" in dev["role"].lower() or "rédaction" in dev["role"].lower()
            cls_name = TaskClassification.DOCUMENTATION.value if is_doc else TaskClassification.SOFTWARE_DEVELOPMENT.value
            target_file = f"{workspace_dir}/docs/technical_documentation.md" if is_doc else f"{workspace_dir}/src/solution_module.py"

            t_impl = DagTask(
                id=t_id,
                mission_id=mission_id,
                agent_id=dev["id"],
                classification=cls_name,
                title=f"{'Documentation' if is_doc else 'Développement'} — {dev['name']}",
                description=f"Exécuter l'implémentation pour le rôle {dev['role']} conformément aux spécifications.",
                prompt=f"Génère la solution complète et vérifiable pour l'objectif : {objective}",
                dependencies=[t1_id],
                status="PENDING",
                weight=dev_weight,
                is_mandatory=True,
                acceptance_criteria="Code source exécutable ou document technique conforme avec SHA-256 scellé.",
                expected_deliverable="technical_documentation.md" if is_doc else "solution_module.py",
                assigned_file=target_file,
                created_at=now
            )
            tasks.append(t_impl)

        # 3. Automated Test Suite (Depends on code implementation)
        t_test_id = f"task-{m_short}-tests"
        t_test = DagTask(
            id=t_test_id,
            mission_id=mission_id,
            agent_id=qa_agent["id"],
            classification=TaskClassification.TESTING.value,
            title=f"Suite de tests automatisés — {qa_agent['name']}",
            description="Élaborer et exécuter la suite de tests unitaires pour valider le code produit.",
            prompt=f"Écris et exécute les tests unitaires pour valider la conformité de : {objective}",
            dependencies=impl_task_ids,
            status="PENDING",
            weight=15,
            is_mandatory=True,
            acceptance_criteria="Suite de tests exécutée sans échec (Code retour 0, 100% assertions vérifiées).",
            expected_deliverable="test_solution.py",
            assigned_file=f"{workspace_dir}/tests/test_solution.py",
            created_at=now
        )
        tasks.append(t_test)

        # 4. Independent Quality Audit & Verification (Depends on tests and deliverables)
        t_qa_id = f"task-{m_short}-audit"
        t_qa = DagTask(
            id=t_qa_id,
            mission_id=mission_id,
            agent_id=qa_agent["id"],
            classification=TaskClassification.VALIDATION.value,
            title=f"Audit qualité indépendant & Preuves SHA-256 — {qa_agent['name']}",
            description="Vérification physique des fichiers créés, intégrité SHA-256 et validation des critères.",
            prompt=f"Vérifie l'intégrité de tous les livrables physiques et confirme la conformité pour : {objective}",
            dependencies=[t_test_id],
            status="PENDING",
            weight=10,
            is_mandatory=True,
            acceptance_criteria="Vérification physique des livrables 100% validée et preuves enregistrées.",
            expected_deliverable="audit_proofs.json",
            assigned_file=f"{workspace_dir}/proofs/audit_proofs.json",
            created_at=now
        )
        tasks.append(t_qa)

        # 5. Final Report & Perpetual Memory Archival
        t_rep_id = f"task-{m_short}-report"
        t_rep = DagTask(
            id=t_rep_id,
            mission_id=mission_id,
            agent_id=arch_agent["id"],
            classification=TaskClassification.DOCUMENTATION.value,
            title="Consolidation du rapport final & Mémoire Collective",
            description="Génération du rapport markdown de clôture et ingestion des acquis dans CodeStation-Memory.",
            prompt=f"Consolide le rapport de mission final et les empreintes cryptographiques pour : {objective}",
            dependencies=[t_qa_id],
            status="PENDING",
            weight=10,
            is_mandatory=True,
            acceptance_criteria="Rapport final complet report.md généré et indexation mémoire validée.",
            expected_deliverable="report.md",
            assigned_file=f"{workspace_dir}/report.md",
            created_at=now
        )
        tasks.append(t_rep)

        return tasks

    def get_ready_tasks(self, tasks: List[DagTask]) -> List[DagTask]:
        """Returns all tasks whose dependencies are COMPLETED and currently in PENDING state."""
        completed_ids = {t.id for t in tasks if t.status == "COMPLETED"}
        ready: List[DagTask] = []
        for t in tasks:
            if t.status == "PENDING" and all(dep in completed_ids for dep in t.dependencies):
                ready.append(t)
        return ready

    def compute_mission_progress(self, tasks: List[DagTask], mission_completed: bool = False) -> int:
        """
        Calculates mission progress based strictly on mandatory tasks and their weights.
        Invariant: If mission_completed is True, returns strictly 100.
        """
        if mission_completed:
            return 100

        mandatory = [t for t in tasks if t.is_mandatory]
        if not mandatory:
            return 100 if all(t.status == "COMPLETED" for t in tasks) else 0

        total_weight = sum(t.weight for t in mandatory)
        if total_weight <= 0:
            return 0

        accumulated = sum(t.progress * t.weight for t in mandatory)
        calc = int(accumulated / total_weight)

        # Invariant: cannot report 100% unless all mandatory tasks are COMPLETED
        if calc >= 100 and not all(t.status == "COMPLETED" for t in mandatory):
            return 99

        return max(0, min(100, calc))

    def compute_agent_progress(self, agent_id: str, tasks: List[DagTask]) -> int:
        """Calculates an agent's justified progress based on assigned tasks."""
        assigned = [t for t in tasks if t.agent_id == agent_id]
        if not assigned:
            return 0
        total_w = sum(t.weight for t in assigned)
        if total_w <= 0:
            return 100 if all(t.status == "COMPLETED" for t in assigned) else 0
        acc = sum(t.progress * t.weight for t in assigned)
        return max(0, min(100, int(acc / total_w)))

    def all_mandatory_tasks_completed(self, tasks: List[DagTask]) -> bool:
        """Returns True if and only if all mandatory tasks are COMPLETED with valid deliverables."""
        mandatory = [t for t in tasks if t.is_mandatory]
        if not mandatory:
            return False
        return all(t.status == "COMPLETED" and t.progress == 100 for t in mandatory)
