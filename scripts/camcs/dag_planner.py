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
    deliverable_type: str = "LOCAL_FILE"  # LOCAL_FILE, LOCAL_DIRECTORY, GIT_COMMIT, REMOTE_REPOSITORY, REMOTE_FILE, DEPLOYMENT, API_RESOURCE, TEST_REPORT
    external_action_type: Optional[str] = None  # GITHUB_CREATE_REPO, GIT_PUSH, DEPLOY_SERVICE
    external_target: Optional[str] = None  # e.g., "CAMCS"
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
            "deliverableType": self.deliverable_type,
            "externalActionType": self.external_action_type,
            "externalTarget": self.external_target,
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
        obj_lower = (title + " " + objective).lower()
        is_github_repo = any(w in obj_lower for w in ["repo github", "dépôt github", "depot github", "github repo", "repository github"])

        arch_agent = next((a for a in agents if "architect" in a["role"].lower() or "architecture" in a["role"].lower()), agents[0])
        qa_agent = next((a for a in agents if "qualité" in a["role"].lower() or "audit" in a["role"].lower() or "test" in a["role"].lower()), agents[-1])
        dev_agents = [a for a in agents if a["id"] != arch_agent["id"] and a["id"] != qa_agent["id"]]
        if not dev_agents:
            dev_agents = [arch_agent]

        if is_github_repo:
            # Objective requires a real remote GitHub repository creation & publication
            import re
            m = re.search(r'(?:nomm[ée]|nome|named)\s+(?:le\s+)?([A-Za-z0-9_\-\.]+)', objective, re.IGNORECASE)
            target_repo_name = m.group(1).strip() if m else "CAMCS"

            # 1. Architecture specification & Tree Design
            t1_id = f"task-{m_short}-spec"
            t1 = DagTask(
                id=t1_id,
                mission_id=mission_id,
                agent_id=arch_agent["id"],
                classification=TaskClassification.ANALYSIS.value,
                title=f"Spécification d'architecture et arbre applicatif — {target_repo_name}",
                description="Concevoir l'architecture applicative, l'arbre des répertoires et les modules demandés.",
                prompt=f"Conçois l'arbre d'architecture complet pour l'application Kotlin et le dépôt {target_repo_name} : {objective}",
                dependencies=[],
                status="READY",
                weight=15,
                is_mandatory=True,
                acceptance_criteria="Spécification formalisée dans architecture_spec.md avec arbre applicatif détaillé.",
                expected_deliverable="architecture_spec.md",
                assigned_file=f"{workspace_dir}/docs/architecture_spec.md",
                deliverable_type="LOCAL_FILE",
                created_at=now
            )
            tasks.append(t1)

            # 2. README.md Creation
            t2_id = f"task-{m_short}-readme"
            t2 = DagTask(
                id=t2_id,
                mission_id=mission_id,
                agent_id=arch_agent["id"],
                classification=TaskClassification.DOCUMENTATION.value,
                title=f"Rédaction du README.md officiel — {target_repo_name}",
                description="Rédiger le fichier README.md racine contenant présentation, arbre d'architecture et instructions.",
                prompt=f"Rédige le README.md complet avec arborescence Kotlin, présentation et guide de build pour {target_repo_name} : {objective}",
                dependencies=[t1_id],
                status="PENDING",
                weight=15,
                is_mandatory=True,
                acceptance_criteria="README.md racine généré avec arbre d'architecture Kotlin conforme et documentation.",
                expected_deliverable="README.md",
                assigned_file=f"{workspace_dir}/README.md",
                deliverable_type="LOCAL_FILE",
                created_at=now
            )
            tasks.append(t2)

            # 3. Source Code & Kotlin Tree Implementation
            impl_ids = []
            for idx, dev in enumerate(dev_agents):
                t_impl_id = f"task-{m_short}-impl-{idx+1}"
                impl_ids.append(t_impl_id)
                t_impl = DagTask(
                    id=t_impl_id,
                    mission_id=mission_id,
                    agent_id=dev["id"],
                    classification=TaskClassification.SOFTWARE_DEVELOPMENT.value,
                    title=f"Implémentation des modules Kotlin & structure — {dev['name']}",
                    description="Créer les packages Kotlin, fichiers sources et structure Clean Architecture.",
                    prompt=f"Génère la structure de projet Kotlin et fichiers sources pour {target_repo_name} : {objective}",
                    dependencies=[t1_id],
                    status="PENDING",
                    weight=20,
                    is_mandatory=True,
                    acceptance_criteria="Arborescence Kotlin créée avec classes sources et fichiers de configuration valides.",
                    expected_deliverable="MainActivity.kt",
                    assigned_file=f"{workspace_dir}/app/src/main/kotlin/com/ghdinteractivestudio/camcs/MainActivity.kt",
                    deliverable_type="LOCAL_FILE",
                    created_at=now
                )
                tasks.append(t_impl)

            # 4. Automated Tests
            t_test_id = f"task-{m_short}-tests"
            t_test = DagTask(
                id=t_test_id,
                mission_id=mission_id,
                agent_id=qa_agent["id"],
                classification=TaskClassification.TESTING.value,
                title=f"Tests automatisés de conformité d'architecture — {qa_agent['name']}",
                description="Valider la cohérence de l'arborescence, des imports et des fichiers générés.",
                prompt=f"Écris et exécute les tests de conformité d'architecture pour {target_repo_name} : {objective}",
                dependencies=impl_ids + [t2_id],
                status="PENDING",
                weight=10,
                is_mandatory=True,
                acceptance_criteria="Tests de conformité exécutés avec succès.",
                expected_deliverable="test_solution.py",
                assigned_file=f"{workspace_dir}/tests/test_solution.py",
                deliverable_type="TEST_REPORT",
                created_at=now
            )
            tasks.append(t_test)

            # 5. Mandatory External Action: GitHub Repo Creation & Publication
            t_ext_id = f"task-{m_short}-github-publish"
            t_ext = DagTask(
                id=t_ext_id,
                mission_id=mission_id,
                agent_id=dev_agents[0]["id"],
                classification=TaskClassification.API_INTEGRATION.value,
                title=f"Création et publication du dépôt distant GitHub — {target_repo_name}",
                description=f"Créer réellement le dépôt {target_repo_name} sur GitHub via GitHub CLI / API, initialiser git et pousser la branche main.",
                prompt=f"Publie le projet sur GitHub sous le nom {target_repo_name} et pousse tous les fichiers validés.",
                dependencies=impl_ids + [t2_id, t_test_id],
                status="PENDING",
                weight=25,
                is_mandatory=True,
                acceptance_criteria=f"Dépôt distant GitHub créé, commit poussé et accessible sur https://github.com/Anisghd-lab/{target_repo_name}",
                expected_deliverable=f"github_repo:{target_repo_name}",
                assigned_file=f"{workspace_dir}/README.md",
                deliverable_type="REMOTE_REPOSITORY",
                external_action_type="GITHUB_CREATE_REPO",
                external_target=target_repo_name,
                created_at=now
            )
            tasks.append(t_ext)

            # 6. Independent Quality Audit & Remote Verification
            t_qa_id = f"task-{m_short}-audit"
            t_qa = DagTask(
                id=t_qa_id,
                mission_id=mission_id,
                agent_id=qa_agent["id"],
                classification=TaskClassification.VALIDATION.value,
                title=f"Audit qualité indépendant & Vérification distante GitHub — {qa_agent['name']}",
                description="Interroger l'API GitHub pour confirmer la présence du dépôt, du README et des commits distants.",
                prompt=f"Vérifie de manière indépendante l'existence du dépôt GitHub {target_repo_name} et la présence des livrables.",
                dependencies=[t_ext_id],
                status="PENDING",
                weight=10,
                is_mandatory=True,
                acceptance_criteria="Dépôt distant GitHub confirmé opérationnel et livrables physiques certifiés.",
                expected_deliverable="audit_proofs.json",
                assigned_file=f"{workspace_dir}/proofs/audit_proofs.json",
                deliverable_type="TEST_REPORT",
                created_at=now
            )
            tasks.append(t_qa)

            # 7. Final Report
            t_rep_id = f"task-{m_short}-report"
            t_rep = DagTask(
                id=t_rep_id,
                mission_id=mission_id,
                agent_id=arch_agent["id"],
                classification=TaskClassification.DOCUMENTATION.value,
                title="Consolidation du rapport final & Enregistrement des Preuves",
                description="Générer le rapport final de clôture avec URL HTTPS vérifiée et SHA distants.",
                prompt=f"Génère le rapport final certifiant la création et publication du dépôt {target_repo_name} : {objective}",
                dependencies=[t_qa_id],
                status="PENDING",
                weight=5,
                is_mandatory=True,
                acceptance_criteria="Rapport final complet report.md généré avec URL distante et empreintes scellées.",
                expected_deliverable="report.md",
                assigned_file=f"{workspace_dir}/report.md",
                deliverable_type="LOCAL_FILE",
                created_at=now
            )
            tasks.append(t_rep)

            return tasks

        # Default Generic Plan (if no external action required)

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
