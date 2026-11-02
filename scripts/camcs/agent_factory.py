#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Dynamic Agent Factory
Dynamically creates specialized autonomous agents suited to each mission.
"""

import uuid
from typing import Dict, List, Optional, Any
from .cli_registry import CliRegistry

ROLE_TEMPLATES = {
    "architect": {
        "name": "Lead Architect",
        "role": "Architecture & Planification",
        "shape": "squircle",
        "color": "green",
        "preferred_cli": "antigravity",
        "task_type": "architecture",
        "default_tasks": [
            ("t1-1", "Analyse de la demande utilisateur et contraintes", 30),
            ("t1-2", "Conception de l'architecture et structure des modules", 35),
            ("t1-3", "Élaboration de la spécification technique et du DAG", 35)
        ]
    },
    "backend": {
        "name": "Backend Engineer",
        "role": "Ingénierie Système & API",
        "shape": "tablet",
        "color": "blue",
        "preferred_cli": "codebuddy",
        "task_type": "backend",
        "default_tasks": [
            ("t2-1", "Développement des modules métier et connecteurs", 40),
            ("t2-2", "Implémentation de la persistance et des interfaces", 40),
            ("t2-3", "Optimisation des performances et gestion des erreurs", 20)
        ]
    },
    "android": {
        "name": "Android Developer",
        "role": "Développement Mobile Kotlin & Compose",
        "shape": "squircle",
        "color": "emerald",
        "preferred_cli": "antigravity",
        "task_type": "coding",
        "default_tasks": [
            ("t-and-1", "Architecture des écrans Jetpack Compose et Glassmorphism", 40),
            ("t-and-2", "Intégration du state management et flux asynchrones", 40),
            ("t-and-3", "Validation de la réactivité et des contraintes Android 16", 20)
        ]
    },
    "qa": {
        "name": "Auditeur Qualité & Tests",
        "role": "Audit Indépendant & Validation",
        "shape": "pebble",
        "color": "violet",
        "preferred_cli": "clixad",
        "task_type": "qa",
        "default_tasks": [
            ("t3-1", "Vérification physique des livrables et empreintes SHA-256", 50),
            ("t3-2", "Exécution de la suite de tests automatisée et validation", 50)
        ]
    },
    "documentation": {
        "name": "Documentation Specialist",
        "role": "Rédaction & Rapport Technique",
        "shape": "circle",
        "color": "amber",
        "preferred_cli": "clixad",
        "task_type": "documentation",
        "default_tasks": [
            ("t-doc-1", "Synthèse des fonctionnalités et guides d'utilisation", 50),
            ("t-doc-2", "Génération du rapport de clôture et scellement des preuves", 50)
        ]
    },
    "devops": {
        "name": "DevOps Engineer",
        "role": "Automatisation & Déploiement",
        "shape": "hexagon",
        "color": "cyan",
        "preferred_cli": "codebuddy",
        "task_type": "backend",
        "default_tasks": [
            ("t-devops-1", "Création des scripts d'automatisation et de validation", 50),
            ("t-devops-2", "Vérification de l'intégrité de l'environnement PRoot", 50)
        ]
    },
    "security": {
        "name": "Security Auditor",
        "role": "Audit de Sécurité & Conformité",
        "shape": "shield",
        "color": "red",
        "preferred_cli": "clixad",
        "task_type": "qa",
        "default_tasks": [
            ("t-sec-1", "Contrôle d'absence de fuite de secrets et permissions", 50),
            ("t-sec-2", "Vérification de l'isolation des processus et sandboxing", 50)
        ]
    }
}

class AgentFactory:
    def __init__(self, cli_registry: CliRegistry):
        self.cli_registry = cli_registry

    def create_agent(
        self,
        mission_id: str,
        role_key: str,
        custom_name: Optional[str] = None,
        custom_objective: Optional[str] = None,
        preferred_cli: Optional[str] = None
    ) -> Dict[str, Any]:
        template = ROLE_TEMPLATES.get(role_key, ROLE_TEMPLATES["backend"])
        agent_id = f"ag-{uuid.uuid4().hex[:8]}"

        cli_choice = preferred_cli or template["preferred_cli"]
        runner, router_reason = self.cli_registry.select_best_cli(template["task_type"], cli_choice)

        tasks = [
            {"id": tid, "title": title, "status": "PENDING", "weight": w}
            for tid, title, w in template["default_tasks"]
        ]

        return {
            "id": agent_id,
            "mission_id": mission_id,
            "name": custom_name or template["name"],
            "role": template["role"],
            "shape": template["shape"],
            "color": template["color"],
            "cli": runner.cli_id,
            "provider": "local_cli",
            "model": runner.version or "default",
            "objective": custom_objective or f"Exécuter avec rigueur les tâches de {template['role']}",
            "currentSubtask": "Initialisation de l'agent",
            "progress": 0,
            "status": "INITIALIZING",
            "tasks": tasks,
            "router_reason": router_reason
        }

    def assemble_team_for_mission(
        self,
        mission_id: str,
        title: str,
        objective: str
    ) -> List[Dict[str, Any]]:
        """
        Dynamically analyzes the mission and selects the right team of specialized agents.
        Ensures at least 2 distinct operational CLIs are selected for real collaboration.
        """
        obj_lower = (title + " " + objective).lower()
        team_roles = ["architect"]

        if any(w in obj_lower for w in ["android", "compose", "mobile", "apk"]):
            team_roles.append("android")
        else:
            team_roles.append("backend")

        if any(w in obj_lower for w in ["doc", "guide", "manuel", "rapport"]):
            team_roles.append("documentation")

        if any(w in obj_lower for w in ["deploy", "ci", "docker", "script", "automatisation"]):
            team_roles.append("devops")

        if any(w in obj_lower for w in ["securite", "security", "audit", "secret"]):
            team_roles.append("security")

        # Always include QA Engineer for independent quality auditing
        team_roles.append("qa")

        # Deduplicate while preserving order
        unique_roles = list(dict.fromkeys(team_roles))

        # Distinct CLI diversity enforcement:
        # Guarantee that at least 2 different CLIs are assigned across the agents
        assigned_clis = set()
        agents = []

        for role_key in unique_roles:
            agent = self.create_agent(mission_id, role_key)
            # If all assigned so far are same CLI, try assigning a different available CLI for subsequent agents
            if len(agents) > 0 and agent["cli"] in assigned_clis:
                # Seek alternative operational CLI
                all_status = self.cli_registry.get_all_clis_status()
                for cli_stat in all_status:
                    if cli_stat["isOperational"] and cli_stat["id"] not in assigned_clis:
                        agent["cli"] = cli_stat["id"]
                        agent["model"] = cli_stat["version"]
                        break

            assigned_clis.add(agent["cli"])
            agents.append(agent)

        return agents
