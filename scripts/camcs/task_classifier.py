#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Task Classification System
Classifies tasks into 11 distinct operational categories and determines required skills,
complexity, execution mode, and 100% free resource selection.
"""

from enum import Enum
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, field

class TaskClassification(str, Enum):
    SOFTWARE_DEVELOPMENT = "SOFTWARE_DEVELOPMENT"
    RESEARCH = "RESEARCH"
    WEB_RESEARCH = "WEB_RESEARCH"
    ANALYSIS = "ANALYSIS"
    DOCUMENTATION = "DOCUMENTATION"
    TESTING = "TESTING"
    LIGHTWEIGHT = "LIGHTWEIGHT"
    ORCHESTRATION = "ORCHESTRATION"
    VALIDATION = "VALIDATION"
    DEBUGGING = "DEBUGGING"
    API_INTEGRATION = "API_INTEGRATION"

@dataclass
class TaskProfile:
    classification: TaskClassification
    required_skills: List[str]
    complexity: str  # LOW, MEDIUM, HIGH
    priority: str    # LOW, NORMAL, HIGH, CRITICAL
    execution_mode: str  # CLI, API_FREE, LIGHTWEIGHT
    preferred_cli: Optional[str] = None
    free_models_only: bool = True
    budget_tokens: int = 4096
    success_criteria: str = ""
    expected_deliverable_type: str = "document"  # source_code, test_suite, documentation, report, audit_verdict

class TaskClassifier:
    """Classifies tasks based on objectives, prompt content, and requirements."""

    KEYWORDS = {
        TaskClassification.SOFTWARE_DEVELOPMENT: [
            "code", "développement", "implémentation", "module", "fonction", "classe",
            "kotlin", "python", "rust", "backend", "frontend", "compose", "android", "build"
        ],
        TaskClassification.RESEARCH: [
            "recherche", "étude", "comparatif", "état de l'art", "benchmark", "exploration"
        ],
        TaskClassification.WEB_RESEARCH: [
            "web", "internet", "en ligne", "site", "url", "documentation officielle", "github repo"
        ],
        TaskClassification.ANALYSIS: [
            "analyse", "architecture", "audit", "cartographie", "diagnostic", "structure", "spécification"
        ],
        TaskClassification.DOCUMENTATION: [
            "documentation", "doc", "rapport", "guide", "manuel", "readme", "synthèse", "compte-rendu"
        ],
        TaskClassification.TESTING: [
            "test", "tests", "unitaire", "pytest", "junit", "e2e", "couverture", "validation tests"
        ],
        TaskClassification.LIGHTWEIGHT: [
            "rapide", "format", "json", "tri", "simple", "vérification mineure", "lookup"
        ],
        TaskClassification.ORCHESTRATION: [
            "planification", "dag", "dispatching", "coordination", "gouvernance", "consensus"
        ],
        TaskClassification.VALIDATION: [
            "validation", "conformité", "contrôle qualité", "auditeur", "critères", "sha256", "intégrité"
        ],
        TaskClassification.DEBUGGING: [
            "debug", "correction", "bug", "erreur", "exception", "trace", "crash", "fix"
        ],
        TaskClassification.API_INTEGRATION: [
            "api", "endpoint", "rest", "connecteur", "http", "openrouter", "xkiro", "webhook"
        ]
    }

    @classmethod
    def classify(cls, title: str, description: str, prompt: str = "") -> TaskProfile:
        text = f"{title} {description} {prompt}".lower()

        scores: Dict[TaskClassification, int] = {cat: 0 for cat in TaskClassification}
        for cat, kw_list in cls.KEYWORDS.items():
            for kw in kw_list:
                if kw in text:
                    scores[cat] += 1

        # Priority rules
        if scores[TaskClassification.DEBUGGING] > 0 and ("fix" in text or "erreur" in text or "crash" in text):
            chosen = TaskClassification.DEBUGGING
        elif scores[TaskClassification.TESTING] > 1 or ("unittest" in text or "pytest" in text):
            chosen = TaskClassification.TESTING
        elif scores[TaskClassification.VALIDATION] > 1 or ("qualité" in text and "audit" in text):
            chosen = TaskClassification.VALIDATION
        elif scores[TaskClassification.WEB_RESEARCH] > 0:
            chosen = TaskClassification.WEB_RESEARCH
        elif scores[TaskClassification.SOFTWARE_DEVELOPMENT] > 0:
            chosen = TaskClassification.SOFTWARE_DEVELOPMENT
        elif scores[TaskClassification.ANALYSIS] > 0:
            chosen = TaskClassification.ANALYSIS
        elif scores[TaskClassification.DOCUMENTATION] > 0:
            chosen = TaskClassification.DOCUMENTATION
        else:
            # Pick highest score, or fallback to LIGHTWEIGHT or SOFTWARE_DEVELOPMENT
            best = max(scores.items(), key=lambda item: item[1])
            chosen = best[0] if best[1] > 0 else TaskClassification.SOFTWARE_DEVELOPMENT

        # Build profile
        skills = cls._determine_skills(chosen, text)
        complexity = cls._determine_complexity(text)
        mode = cls._determine_mode(chosen, complexity)
        deliv_type = cls._determine_deliverable_type(chosen)

        return TaskProfile(
            classification=chosen,
            required_skills=skills,
            complexity=complexity,
            priority="HIGH" if complexity == "HIGH" else "NORMAL",
            execution_mode=mode,
            preferred_cli=cls._preferred_cli_for(chosen),
            free_models_only=True,
            budget_tokens=8192 if complexity == "HIGH" else 4096,
            success_criteria=f"Validation réussie selon la classification {chosen.value}",
            expected_deliverable_type=deliv_type
        )

    @staticmethod
    def _determine_skills(cat: TaskClassification, text: str) -> List[str]:
        skills = []
        if cat in [TaskClassification.SOFTWARE_DEVELOPMENT, TaskClassification.DEBUGGING]:
            skills.extend(["coding-standards", "error-handling"])
            if "python" in text:
                skills.append("python-patterns")
            if "android" in text or "compose" in text or "kotlin" in text:
                skills.append("android-clean-architecture")
        elif cat == TaskClassification.TESTING:
            skills.extend(["tdd-workflow", "python-testing"])
        elif cat in [TaskClassification.RESEARCH, TaskClassification.WEB_RESEARCH]:
            skills.extend(["research-ops", "documentation-lookup"])
        elif cat == TaskClassification.ANALYSIS:
            skills.extend(["codebase-onboarding", "backend-patterns"])
        elif cat == TaskClassification.DOCUMENTATION:
            skills.extend(["content-engine", "brand-voice"])
        elif cat == TaskClassification.VALIDATION:
            skills.extend(["production-audit", "security-review"])
        return skills or ["coding-standards"]

    @staticmethod
    def _determine_complexity(text: str) -> str:
        high_indicators = ["refonte", "complet", "multi-agent", "complexe", "architecture", "sécurité", "haute"]
        low_indicators = ["simple", "léger", "rapide", "format", "lookup", "petit"]
        if any(w in text for w in high_indicators):
            return "HIGH"
        if any(w in text for w in low_indicators):
            return "LOW"
        return "MEDIUM"

    @staticmethod
    def _determine_mode(cat: TaskClassification, complexity: str) -> str:
        # Lightweight tasks and fast research do not need heavy CLI
        if cat in [TaskClassification.LIGHTWEIGHT, TaskClassification.WEB_RESEARCH] or complexity == "LOW":
            return "API_FREE"
        return "CLI"

    @staticmethod
    def _preferred_cli_for(cat: TaskClassification) -> str:
        mapping = {
            TaskClassification.ANALYSIS: "antigravity",
            TaskClassification.ORCHESTRATION: "antigravity",
            TaskClassification.SOFTWARE_DEVELOPMENT: "codebuddy",
            TaskClassification.DEBUGGING: "opencode",
            TaskClassification.TESTING: "clixad",
            TaskClassification.VALIDATION: "clixad",
            TaskClassification.DOCUMENTATION: "clixad",
            TaskClassification.RESEARCH: "clixad",
            TaskClassification.WEB_RESEARCH: "opencode",
            TaskClassification.LIGHTWEIGHT: "clixad",
            TaskClassification.API_INTEGRATION: "opencode"
        }
        return mapping.get(cat, "codebuddy")

    @staticmethod
    def _determine_deliverable_type(cat: TaskClassification) -> str:
        mapping = {
            TaskClassification.SOFTWARE_DEVELOPMENT: "source_code",
            TaskClassification.DEBUGGING: "source_code",
            TaskClassification.TESTING: "test_suite",
            TaskClassification.DOCUMENTATION: "documentation",
            TaskClassification.ANALYSIS: "architecture",
            TaskClassification.VALIDATION: "audit_verdict",
            TaskClassification.RESEARCH: "research_notes",
            TaskClassification.WEB_RESEARCH: "research_notes",
            TaskClassification.ORCHESTRATION: "dag_specification",
            TaskClassification.LIGHTWEIGHT: "json_payload",
            TaskClassification.API_INTEGRATION: "source_code"
        }
        return mapping.get(cat, "document")
