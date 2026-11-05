#!/usr/bin/env python3
"""
Test suite for TASK-005 — Perpetual Memory Integration for Architecture Decisions & Experiences
Verifies decision recording (ADR), repair error-avoidance rules, mission synthesis, and disk fallback.
"""

import os
import sys
import tempfile
import unittest
import json
import time

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
from camcs.collective_memory_client import CollectiveMemoryClient


class TestMemoryDecisionSync(unittest.TestCase):
    def setUp(self):
        # We test both with the live server (port 19224) and with disk fallback
        self.tmp_dir = tempfile.mkdtemp()
        self.fallback_client = CollectiveMemoryClient(memory_port=99999, repo_path=self.tmp_dir)
        self.live_client = CollectiveMemoryClient(memory_port=19224)

    def tearDown(self):
        import shutil
        shutil.rmtree(self.tmp_dir, ignore_errors=True)

    def test_record_decision_disk_fallback(self):
        ok = self.fallback_client.record_decision(
            title="Clean Architecture Multi-Agents",
            decision="Isoler chaque CLI dans un conteneur et échanger par NDJSON",
            rationale="Éviter les conflits de processus zombies et standardiser les flux",
            context="CodeStation Ubuntu PRoot ARM64",
            consequences="Consommation mémoire contrôlée et reprise après crash simplifiée",
            error_avoidance="Ne jamais lancer de CLI sans garde de timeout",
            tags=["clean-arch", "cli-isolation"],
            author="camcs-architect"
        )
        self.assertTrue(ok)

        # Verify disk creation
        knw_dir = os.path.join(self.tmp_dir, "knowledge")
        exp_dir = os.path.join(self.tmp_dir, "experiences")
        self.assertTrue(os.path.isdir(knw_dir))
        self.assertTrue(os.path.isdir(exp_dir))

        knw_files = os.listdir(knw_dir)
        self.assertEqual(len(knw_files), 1)
        with open(os.path.join(knw_dir, knw_files[0]), "r", encoding="utf-8") as f:
            data = json.load(f)
            self.assertIn("Clean Architecture", data["title"])
            self.assertIn("Isoler chaque CLI", data["solution"])
            self.assertIn("clean-arch", data["tags"])

    def test_record_repair_experience_disk_fallback(self):
        ok = self.fallback_client.record_repair_experience(
            task_title="Compile Kotlin Core",
            error_msg="Unresolved reference: CamcsRepositoryImpl",
            remediation_applied="Importer com.ghdinteractivestudio.camcs.data.CamcsRepositoryImpl",
            iteration_count=2,
            context="Mission: Core Migration"
        )
        self.assertTrue(ok)

        # Search via disk fallback query
        results = self.fallback_client.query_knowledge("CamcsRepositoryImpl", limit=5)
        self.assertGreaterEqual(len(results), 1)
        self.assertIn("Règle d'Évitement", results[0]["title"])

    def test_record_mission_synthesis_disk_fallback(self):
        ok = self.fallback_client.record_mission_synthesis(
            mission_id="mission-perf-001",
            title="Migration Jetpack Compose Glassmorphism",
            objective="Implémenter le tableau de bord avec Frosted Liquid",
            dag_phases=["01-core", "02-domain", "03-ui"],
            gates_passed=4,
            repairs_resolved=1,
            deliverables_count=8
        )
        self.assertTrue(ok)

        results = self.fallback_client.query_knowledge("Glassmorphism", limit=5)
        self.assertGreaterEqual(len(results), 1)
        self.assertIn("Synthèse de Mission Validée", results[0]["title"])

    def test_live_server_interaction_mocked(self):
        """Vérifie le formatage et l'envoi de payload JSON au serveur HTTP."""
        from unittest.mock import patch, MagicMock
        mock_resp = MagicMock()
        mock_resp.status = 200
        mock_resp.__enter__.return_value = mock_resp

        with patch("urllib.request.urlopen", return_value=mock_resp) as mock_urlopen:
            ok = self.live_client.record_decision(
                title="Test Architectural Decision Sync",
                decision="Valider le client de mémoire collective CAMCS",
                rationale="Audit technique Festival vs CodeStation",
                context="Environnement local Ubuntu PRoot",
                consequences="Capitalisation persistante validée",
                tags=["camcs-test", "festival-audit"]
            )
            self.assertTrue(ok)
            self.assertTrue(mock_urlopen.called)


if __name__ == "__main__":
    unittest.main()
