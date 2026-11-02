#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Mission Planner & DAG Scheduler
Constructs and evaluates Directed Acyclic Graph (DAG) for mission tasks.
"""

from typing import Dict, List, Optional, Any, Set
from dataclasses import dataclass, field

@dataclass
class DagTask:
    id: str
    mission_id: str
    agent_id: str
    cli_id: str
    title: str
    description: str
    prompt: str
    dependencies: List[str] = field(default_factory=list)
    status: str = "PENDING"  # PENDING, READY, RUNNING, COMPLETED, FAILED, BLOCKED
    weight: int = 25
    acceptance_criteria: str = ""
    assigned_file: Optional[str] = None
    exit_code: Optional[int] = None
    result_summary: str = ""

class DagPlanner:
    def __init__(self):
        pass

    def build_dag_for_mission(
        self,
        mission_id: str,
        title: str,
        objective: str,
        agents: List[Dict[str, Any]],
        workspace_dir: str
    ) -> List[DagTask]:
        """
        Builds a robust, multi-phase execution DAG matching the assigned agents.
        Phases:
        1. Architecture & Specification (Sequential)
        2. Implementation & Engineering (Parallel or Handover from Architecture)
        3. Quality Audit & Automated Testing (Depends on all Implementation tasks)
        4. Mission Report & Final Acceptance (Depends on QA)
        """
        tasks: List[DagTask] = []
        if not agents:
            return tasks

        arch_agent = next((a for a in agents if "architect" in a["role"].lower()), agents[0])
        dev_agents = [a for a in agents if a["id"] != arch_agent["id"] and "audit" not in a["role"].lower() and "qualité" not in a["role"].lower()]
        qa_agent = next((a for a in agents if "audit" in a["role"].lower() or "qualité" in a["role"].lower()), agents[-1])

        # Task 1: Architecture Specification
        t1_id = f"task-{mission_id[:6]}-spec"
        t1 = DagTask(
            id=t1_id,
            mission_id=mission_id,
            agent_id=arch_agent["id"],
            cli_id=arch_agent["cli"],
            title=f"Spécification technique — {title}",
            description="Analyser l'objectif et formaliser les contraintes et modules attendus.",
            prompt=f"Rédige une spécification technique concise (architecture, contraintes, modules) pour : {objective}",
            dependencies=[],
            weight=25,
            acceptance_criteria="Spécification technique rédigée avec succès et sans erreur.",
            assigned_file=f"{workspace_dir}/docs/spec_technique.md"
        )
        tasks.append(t1)

        # Task 2+: Implementation Tasks (depend on Task 1)
        impl_task_ids = []
        if not dev_agents:
            dev_agents = [arch_agent]

        for idx, dev in enumerate(dev_agents):
            t_id = f"task-{mission_id[:6]}-impl-{idx+1}"
            impl_task_ids.append(t_id)
            t_impl = DagTask(
                id=t_id,
                mission_id=mission_id,
                agent_id=dev["id"],
                cli_id=dev["cli"],
                title=f"Implémentation {dev['name']} — {title}",
                description=f"Produire le code source ou la documentation selon le rôle {dev['role']}.",
                prompt=f"En te basant sur la spécification technique, génère la solution vérifiable pour l'objectif: {objective}",
                dependencies=[t1_id],
                weight=35 // len(dev_agents),
                acceptance_criteria="Fichier de code source ou livrable produit avec intégrité SHA-256.",
                assigned_file=f"{workspace_dir}/src/solution_part_{idx+1}.py"
            )
            tasks.append(t_impl)

        # Task 3: Quality Audit & Independent Verification (depends on all impl tasks)
        t_qa_id = f"task-{mission_id[:6]}-audit"
        t_qa = DagTask(
            id=t_qa_id,
            mission_id=mission_id,
            agent_id=qa_agent["id"],
            cli_id=qa_agent["cli"],
            title=f"Audit qualité & Tests unitaires — {qa_agent['name']}",
            description="Exécuter les vérifications de conformité et valider les critères d'acceptation.",
            prompt=f"Vérifie la conformité technique, l'intégrité et l'absence d'erreurs pour les livrables de: {objective}",
            dependencies=impl_task_ids,
            weight=25,
            acceptance_criteria="Validation indépendante réussie et rapport d'audit conforme.",
            assigned_file=f"{workspace_dir}/tests/audit_verdict.json"
        )
        tasks.append(t_qa)

        # Task 4: Final Mission Report & Sealing
        t_rep_id = f"task-{mission_id[:6]}-report"
        t_rep = DagTask(
            id=t_rep_id,
            mission_id=mission_id,
            agent_id=arch_agent["id"],
            cli_id=arch_agent["cli"],
            title="Consolidation du rapport de mission et archivage mémoire",
            description="Sceller les preuves, vérifier les empreintes SHA-256 et enregistrer l'expérience.",
            prompt=f"Établis le bilan d'exécution de la mission {title} et récapitule les livrables vérifiés.",
            dependencies=[t_qa_id],
            weight=15,
            acceptance_criteria="Rapport final complet avec preuve cryptographique de tous les livrables.",
            assigned_file=f"{workspace_dir}/report.md"
        )
        tasks.append(t_rep)

        return tasks

    def get_ready_tasks(self, tasks: List[DagTask], completed_ids: Set[str]) -> List[DagTask]:
        """Returns all tasks whose dependencies are satisfied and not yet started."""
        ready = []
        for t in tasks:
            if t.status == "PENDING" and all(dep in completed_ids for dep in t.dependencies):
                ready.append(t)
        return ready

    def is_dag_completed(self, tasks: List[DagTask]) -> bool:
        return all(t.status in ("COMPLETED", "FAILED") for t in tasks)
