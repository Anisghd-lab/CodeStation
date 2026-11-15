#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Dynamic Agent Factory & Agency 283 Integrator
Creates specialized autonomous agents on demand for real tasks with:
- Native Integration of the 283 autonomous Agency agents (20 divisions)
- Automatic skills assignment from installed environment skills (~/.agents/skills)
- 100% FREE model and operational CLI assignment
- Strict state initialization (CREATED / ASSIGNED)
"""

import os
import re
import glob
import uuid
from typing import Dict, List, Optional, Any
from .cli_registry import CliRegistry
from .provider_registry import ProviderRegistry, ProviderSelector
from .task_classifier import TaskClassification

ROLE_PROFILES = {
    "architect": {
        "name": "Lead Architect",
        "role": "Architecture & Planification",
        "shape": "squircle",
        "color": "green",
        "preferred_cli": "antigravity",
        "task_type": "architecture",
        "skills": ["codebase-onboarding", "backend-patterns", "coding-standards"],
        "default_free_model": "dots-studio/dots-3-note-preview:free"
    },
    "researcher": {
        "name": "Research Agent",
        "role": "Recherche & Veille Technique",
        "shape": "circle",
        "color": "blue",
        "preferred_cli": "clixad",
        "task_type": "research",
        "skills": ["research-ops", "documentation-lookup"],
        "default_free_model": "liquid/lfm-2.5-2.6b:free"
    },
    "web_researcher": {
        "name": "Web Researcher",
        "role": "Recherche Web & Documentation",
        "shape": "circle",
        "color": "cyan",
        "preferred_cli": "opencode",
        "task_type": "research",
        "skills": ["documentation-lookup", "research-ops"],
        "default_free_model": "qwen/qwen3.5-plus:free"
    },
    "backend": {
        "name": "Backend Developer",
        "role": "Ingénierie Système & Backend",
        "shape": "tablet",
        "color": "blue",
        "preferred_cli": "codebuddy",
        "task_type": "coding",
        "skills": ["python-patterns", "backend-patterns", "error-handling"],
        "default_free_model": "qwen/qwen3-coder-plus:free"
    },
    "android": {
        "name": "Android Developer",
        "role": "Développement Mobile Android Kotlin & Compose",
        "shape": "squircle",
        "color": "emerald",
        "preferred_cli": "antigravity",
        "task_type": "coding",
        "skills": ["android-clean-architecture", "compose-multiplatform-patterns"],
        "default_free_model": "nvidia/nemotron-3.5-lightning:free"
    },
    "rust": {
        "name": "Rust Developer",
        "role": "Ingénierie Native & Performance Rust",
        "shape": "hexagon",
        "color": "orange",
        "preferred_cli": "codebuddy",
        "task_type": "coding",
        "skills": ["rust-patterns", "rust-testing"],
        "default_free_model": "qwen/qwen3-coder-plus:free"
    },
    "analyst": {
        "name": "Code Analyst",
        "role": "Analyse Statique & Audit de Code",
        "shape": "pebble",
        "color": "indigo",
        "preferred_cli": "antigravity",
        "task_type": "verification",
        "skills": ["codebase-onboarding", "coding-standards"],
        "default_free_model": "dots-studio/dots-3-note-preview:free"
    },
    "qa": {
        "name": "Quality Validator",
        "role": "Audit Indépendant & Validation",
        "shape": "pebble",
        "color": "violet",
        "preferred_cli": "clixad",
        "task_type": "qa",
        "skills": ["production-audit", "security-review"],
        "default_free_model": "dots-studio/dots-3-note-preview:free"
    },
    "tester": {
        "name": "Test Engineer",
        "role": "Ingénierie des Tests & Fiabilité",
        "shape": "shield",
        "color": "purple",
        "preferred_cli": "clixad",
        "task_type": "qa",
        "skills": ["tdd-workflow", "python-testing"],
        "default_free_model": "liquid/lfm-2.5-2.6b:free"
    },
    "documentation": {
        "name": "Documentation Specialist",
        "role": "Documentation & Rapports",
        "shape": "circle",
        "color": "amber",
        "preferred_cli": "clixad",
        "task_type": "documentation",
        "skills": ["content-engine", "brand-voice"],
        "default_free_model": "dots-studio/dots-3-note-preview:free"
    },
    "security": {
        "name": "Security Auditor",
        "role": "Audit de Sécurité & Confidentialité",
        "shape": "shield",
        "color": "red",
        "preferred_cli": "clixad",
        "task_type": "qa",
        "skills": ["security-review", "production-audit"],
        "default_free_model": "dots-studio/dots-3-note-preview:free"
    },
    "debugger": {
        "name": "Debugging Agent",
        "role": "Diagnostic & Résolution de Pannes",
        "shape": "hexagon",
        "color": "rose",
        "preferred_cli": "opencode",
        "task_type": "coding",
        "skills": ["agent-introspection-debugging", "error-handling"],
        "default_free_model": "qwen/qwen3-coder-plus:free"
    },
    "api_integrator": {
        "name": "API Integrator",
        "role": "Intégration d'API & Connecteurs",
        "shape": "tablet",
        "color": "teal",
        "preferred_cli": "opencode",
        "task_type": "backend",
        "skills": ["api-connector-builder", "backend-patterns"],
        "default_free_model": "liquid/lfm-2.5-2.6b:free"
    }
}

class AgentFactory:
    def __init__(self, cli_registry: CliRegistry, provider_registry: Optional[ProviderRegistry] = None):
        self.cli_registry = cli_registry
        self.provider_registry = provider_registry or ProviderRegistry()
        self.selector = ProviderSelector(self.provider_registry)
        self.available_skills = self._detect_installed_skills()
        self.agency_catalog = self._load_agency_catalog()

    def _detect_installed_skills(self) -> List[str]:
        """Detects available skills installed in the environment."""
        paths = [
            "/root/.gemini/config/plugins/ecc/skills",
            "/root/.agents/skills",
            "/root/.gemini/config/skills"
        ]
        found = set()
        for p in paths:
            if os.path.isdir(p):
                try:
                    for d in os.listdir(p):
                        full = os.path.join(p, d)
                        if os.path.isdir(full) and os.path.exists(os.path.join(full, "SKILL.md")):
                            found.add(d)
                except Exception:
                    pass
        return sorted(list(found))

    def _load_agency_catalog(self) -> Dict[str, Dict[str, Any]]:
        """Parses and indexes all 283 Agency autonomous agents from ~/.agents/skills."""
        catalog: Dict[str, Dict[str, Any]] = {}
        paths = [
            "/root/.agents/skills",
            "/root/.gemini/config/skills",
            "/root/.kimi-code/skills"
        ]
        for base_p in paths:
            if not os.path.isdir(base_p):
                continue
            skill_files = glob.glob(os.path.join(base_p, "agency-*/SKILL.md"))
            for s_path in skill_files:
                try:
                    with open(s_path, "r", encoding="utf-8", errors="replace") as f:
                        content = f.read()
                    name_m = re.search(r"^name:\s*['\"]?([^'\"\n]+)['\"]?", content, re.MULTILINE)
                    desc_m = re.search(r"^description:\s*['\"]?([^'\"\n]+)['\"]?", content, re.MULTILINE)
                    title_m = re.search(r"^#\s+([^\n]+)", content, re.MULTILINE)

                    slug = name_m.group(1).strip() if name_m else os.path.basename(os.path.dirname(s_path))
                    desc = desc_m.group(1).strip() if desc_m else ""
                    raw_title = title_m.group(1).strip() if title_m else slug
                    clean_title = re.sub(r"\s+Agent\s+Personality", "", raw_title, flags=re.IGNORECASE).strip()

                    # Infer task type and preferred CLI
                    s_lower = slug.lower() + " " + desc.lower()
                    clean_role = desc.split(".")[0].strip() if desc else clean_title
                    if any(w in s_lower for w in ["android", "mobile", "ios", "visionos", "swift", "flutter"]):
                        pref_cli = "antigravity"
                        task_type = "coding"
                    elif any(w in s_lower for w in ["backend", "api", "database", "sql", "rust", "system"]):
                        pref_cli = "codebuddy"
                        task_type = "coding"
                    elif any(w in s_lower for w in ["security", "audit", "pentest", "qa", "test", "checker", "collector"]):
                        pref_cli = "clixad"
                        task_type = "qa"
                        if "audit" not in clean_role.lower() and "qualit" not in clean_role.lower():
                            clean_role = f"Audit Qualité & {clean_role}"
                    elif any(w in s_lower for w in ["research", "doc", "writer", "historian", "psychologist", "translator"]):
                        pref_cli = "kimi"
                        task_type = "documentation"
                    elif any(w in s_lower for w in ["architecture", "orchestrator", "planner", "rag", "ai", "model"]):
                        pref_cli = "antigravity"
                        task_type = "architecture"
                    else:
                        pref_cli = "opencode"
                        task_type = "coding"

                    agent_info = {
                        "slug": slug,
                        "name": clean_title,
                        "role": clean_role,
                        "description": desc,
                        "shape": "squircle" if "android" in s_lower or "architect" in s_lower else ("shield" if "security" in s_lower or "qa" in s_lower else "tablet"),
                        "color": "emerald" if "android" in s_lower else ("purple" if "security" in s_lower or "qa" in s_lower else "blue"),
                        "preferred_cli": pref_cli,
                        "task_type": task_type,
                        "skill_path": s_path,
                        "system_prompt": content
                    }
                    catalog[slug] = agent_info
                    # Index without "agency-" prefix as well
                    if slug.startswith("agency-"):
                        catalog[slug[7:]] = agent_info
                except Exception:
                    pass
        return catalog

    def create_agent(
        self,
        mission_id: str,
        role_key: str,
        task_id: Optional[str] = None,
        custom_name: Optional[str] = None,
        custom_objective: Optional[str] = None,
        preferred_cli: Optional[str] = None,
        force_api_mode: bool = False
    ) -> Dict[str, Any]:
        """Creates an agent instance strictly matching user constraints and 100% free resources."""
        agent_id = f"ag-{uuid.uuid4().hex[:8]}"

        # Check if requested role_key matches an Agency agent
        agency_info = self.agency_catalog.get(role_key)
        profile = agency_info if agency_info else ROLE_PROFILES.get(role_key, ROLE_PROFILES["backend"])

        cli_runner = None
        provider_id = "local_cli"
        model_id = "default"

        if not force_api_mode:
            cli_choice = preferred_cli or profile["preferred_cli"]
            cli_runner, router_reason = self.cli_registry.select_best_cli(profile["task_type"], cli_choice)
            if cli_runner:
                provider_id = "local_cli"
                model_id = cli_runner.version or "local_v1"

        if force_api_mode or not cli_runner:
            # Select 100% free API model
            api_adapter, free_model = self.selector.select_best_free_model(profile["task_type"])
            provider_id = api_adapter.provider_id
            model_id = free_model

        assigned_skills = [s for s in profile.get("skills", []) if s in self.available_skills]
        if not assigned_skills and agency_info:
            assigned_skills = [agency_info["slug"]]
        if not assigned_skills:
            assigned_skills = ["coding-standards"]

        return {
            "id": agent_id,
            "mission_id": mission_id,
            "task_id": task_id,
            "name": custom_name or profile["name"],
            "role": profile["role"],
            "shape": profile["shape"],
            "color": profile["color"],
            "cli": cli_runner.cli_id if cli_runner else "api_fallback",
            "provider": provider_id,
            "model": model_id,
            "skills": assigned_skills,
            "agency_slug": agency_info.get("slug") if agency_info else None,
            "agency_skill_path": agency_info.get("skill_path") if agency_info else None,
            "agency_system_prompt": agency_info.get("system_prompt") if agency_info else None,
            "objective": custom_objective or f"Accomplir avec rigueur les tâches de {profile['role']}",
            "currentSubtask": "Initialisation du contexte",
            "progress": 0,
            "status": "CREATED",
            "budget": 8192,
            "tasks": []
        }

    def assemble_team_for_mission(
        self,
        mission_id: str,
        title: str,
        objective: str
    ) -> List[Dict[str, Any]]:
        """
        Dynamically analyzes the mission and selects specialized Agency agents from the 283 catalog.
        Ensures diverse operational CLIs or free API models are assigned.
        """
        obj_lower = (title + " " + objective).lower()
        team_role_keys: List[str] = []

        # 1. Lead Orchestrator / Architect Selection
        if any(w in obj_lower for w in ["pipeline", "multi-agent", "orchestr", "workflow", "grand"]):
            team_role_keys.append("agency-agents-orchestrator" if "agency-agents-orchestrator" in self.agency_catalog else "architect")
        else:
            team_role_keys.append("agency-software-architect" if "agency-software-architect" in self.agency_catalog else "architect")

        # 2. Domain Specialists Selection from the 283 Catalog
        if any(w in obj_lower for w in ["android", "compose", "mobile", "apk", "kotlin"]):
            team_role_keys.append("agency-mobile-app-builder" if "agency-mobile-app-builder" in self.agency_catalog else "android")
        elif any(w in obj_lower for w in ["rust", "cargo"]):
            team_role_keys.append("agency-rust-refactoring-specialist" if "agency-rust-refactoring-specialist" in self.agency_catalog else "rust")
        elif any(w in obj_lower for w in ["debug", "bug", "erreur", "crash"]):
            team_role_keys.append("debugger")
        elif any(w in obj_lower for w in ["api", "connecteur", "webhook", "microservice", "rest"]):
            team_role_keys.append("agency-api-platform-engineer" if "agency-api-platform-engineer" in self.agency_catalog else "api_integrator")
        elif any(w in obj_lower for w in ["database", "sql", "postgres", "sqlite", "query"]):
            team_role_keys.append("agency-database-optimizer" if "agency-database-optimizer" in self.agency_catalog else "backend")
        elif any(w in obj_lower for w in ["rag", "embedding", "vector", "ia", "intelligence"]):
            team_role_keys.append("agency-rag-pipeline-engineer" if "agency-rag-pipeline-engineer" in self.agency_catalog else "backend")
        else:
            team_role_keys.append("agency-backend-architect" if "agency-backend-architect" in self.agency_catalog else "backend")

        # 3. Research & Documentation Specialists
        if any(w in obj_lower for w in ["recherche", "web", "internet", "veille"]):
            team_role_keys.append("web_researcher")
        elif any(w in obj_lower for w in ["doc", "guide", "manuel", "rapport"]):
            team_role_keys.append("agency-technical-writer" if "agency-technical-writer" in self.agency_catalog else "documentation")

        # 4. Independent Security & Quality Auditor
        if any(w in obj_lower for w in ["securite", "sécurité", "security", "pentest", "vulnerab"]):
            team_role_keys.append("agency-application-security-engineer" if "agency-application-security-engineer" in self.agency_catalog else "security")
        elif any(w in obj_lower for w in ["test", "unitaire", "qualite", "qualité", "audit", "verifier"]):
            team_role_keys.append("agency-evidence-collector" if "agency-evidence-collector" in self.agency_catalog else "qa")
        else:
            team_role_keys.append("qa")

        # Deduplicate
        unique_keys = list(dict.fromkeys(team_role_keys))

        agents = []
        assigned_clis = set()

        for rk in unique_keys:
            ag = self.create_agent(mission_id, rk)
            # CLI diversity enforcement: If CLI is already assigned, attempt alternative operational CLI
            if len(agents) > 0 and ag["cli"] in assigned_clis and ag["provider"] == "local_cli":
                all_status = self.cli_registry.get_all_clis_status()
                for cli_stat in all_status:
                    if cli_stat["isOperational"] and cli_stat["id"] not in assigned_clis:
                        ag["cli"] = cli_stat["id"]
                        ag["model"] = cli_stat["version"]
                        break
            assigned_clis.add(ag["cli"])
            agents.append(ag)

        return agents

