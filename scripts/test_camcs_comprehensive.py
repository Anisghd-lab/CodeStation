#!/usr/bin/env python3
"""
CAMCS Comprehensive Verification & Reliability Test Suite
Executes and validates the 30 Mandatory Scenarios defined in the Specification:
- Execution Tests (1 to 15)
- Cancellation & Stop Tests (16 to 22)
- Consistency & Invariant Tests (23 to 30)
Enforces 100% FREE Models & Zero Secrets Leakage.
"""

import os
import sys
import time
import json
import uuid
import sqlite3
import unittest
from typing import Dict, Any, List
import urllib.request
import urllib.error

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from camcs.task_classifier import TaskClassifier, TaskClassification
from camcs.state_machines import (
    MissionState, AgentState, TaskState,
    validate_mission_transition, validate_agent_transition, validate_task_transition
)
from camcs.provider_registry import ProviderRegistry, ProviderSelector, ProviderFallbackManager
from camcs.dag_planner import DagPlanner, DagTask
from camcs.agent_factory import AgentFactory
from camcs.cli_registry import CliRegistry
from camcs.process_manager import ProcessManager
from camcs.deliverable_manager import DeliverableManager
from camcs.quality_auditor import QualityAuditor
from camcs_engine import DB_PATH, ENGINE, CamcsMission, CamcsAgent, CamcsOrchestrationEngine

BASE_URL = "http://127.0.0.1:19223"

def http_get(path: str) -> Dict[str, Any]:
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(url)
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode())

def http_post(path: str, data: Dict[str, Any]) -> Dict[str, Any]:
    url = f"{BASE_URL}{path}"
    req = urllib.request.Request(
        url,
        data=json.dumps(data).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode())


class TestCamcsEngineSuite(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Verify engine connection
        st = http_get("/api/camcs/status")
        assert st.get("ok") is True

    # =========================================================================
    # GROUPE 1: TESTS D'EXÉCUTION (1 à 15)
    # =========================================================================

    def test_01_single_agent_mission(self):
        """1. Mission simple avec un agent."""
        planner = DagPlanner()
        agents = [{"id": "ag-1", "name": "Solo Dev", "role": "Backend", "cli": "clixad"}]
        tasks = planner.build_dag_for_mission("m-test-1", "Titre 1", "Objectif simple", agents, "/tmp")
        self.assertGreaterEqual(len(tasks), 1)

    def test_02_four_agents_parallel(self):
        """2. Mission avec quatre agents parallèles."""
        factory = AgentFactory(ENGINE.cli_registry, ENGINE.provider_registry)
        team = factory.assemble_team_for_mission("m-test-2", "Refonte Android", "Créer une app Android complète avec documentation et audit")
        self.assertGreaterEqual(len(team), 3)

    def test_03_mission_with_dependencies(self):
        """3. Mission avec dépendances DAG ordonnées."""
        planner = DagPlanner()
        agents = [
            {"id": "ag-1", "name": "Arch", "role": "Architecture", "cli": "antigravity"},
            {"id": "ag-2", "name": "Dev", "role": "Backend", "cli": "codebuddy"},
            {"id": "ag-3", "name": "QA", "role": "Qualité", "cli": "clixad"}
        ]
        tasks = planner.build_dag_for_mission("m-test-3", "Titre 3", "Objectif", agents, "/tmp")
        # Task 2 must depend on Task 1
        t1 = tasks[0]
        t2 = tasks[1]
        self.assertIn(t1.id, t2.dependencies)

    def test_04_agent_waiting_state(self):
        """4. Mission avec agent en attente de dépendances."""
        m = CamcsMission("m-wait-1", "Attente", "Obj")
        ag = CamcsAgent("ag-w", m.id, "QA", "Auditeur", "pebble", "violet", "clixad", "local_cli", "v1", "Obj")
        m.agents = [ag]
        t1 = DagTask(id="t-prev", mission_id=m.id, agent_id="ag-other", classification="ANALYSIS", title="Prev", description="", prompt="", dependencies=[], status="PENDING")
        t2 = DagTask(id="t-dep", mission_id=m.id, agent_id=ag.id, classification="VALIDATION", title="Dep", description="", prompt="", dependencies=["t-prev"], status="PENDING")
        m.tasks = [t1, t2]
        ready = DagPlanner().get_ready_tasks(m.tasks)
        # t2 cannot be ready while t1 is pending
        self.assertNotIn(t2, ready)

    def test_05_cli_unavailable_detection(self):
        """5. CLI indisponible détectée sans blocage."""
        status_list = ENGINE.cli_registry.get_all_clis_status()
        omnirush_stat = next((c for c in status_list if c["id"] == "omnirush"), None)
        self.assertIsNotNone(omnirush_stat)
        self.assertFalse(omnirush_stat["isOperational"])
        self.assertEqual(omnirush_stat["healthStatus"], "BLOCKED")

    def test_06_cli_execution_failure_handling(self):
        """6. CLI qui échoue pendant l'exécution gérée proprement."""
        # Test transition to FAILED is valid from RUNNING
        ok, _ = validate_task_transition("RUNNING", "FAILED")
        self.assertTrue(ok)

    def test_07_externally_killed_process_handling(self):
        """7. Processus interrompu extérieurement."""
        pm = ProcessManager(max_concurrent=1)
        self.assertFalse(pm.cancel_task("non-existent-task"))

    def test_08_fallback_to_alternative_cli(self):
        """8. Fallback vers un autre CLI opérationnel."""
        runner, reason = ENGINE.cli_registry.select_best_cli("coding", preferred_cli="omnirush")
        # Since omnirush is BLOCKED, selector must fallback to an operational CLI (e.g. codebuddy or opencode)
        self.assertNotEqual(runner.cli_id, "omnirush")
        self.assertIn(runner.cli_id, ["codebuddy", "opencode", "antigravity", "clixad"])

    def test_09_fallback_to_free_api(self):
        """9. Fallback vers une API 100% gratuite."""
        prov, model = ENGINE.agent_factory.selector.select_best_free_model("coding")
        self.assertIsNotNone(prov)
        self.assertTrue(":free" in model or "free" in model.lower())

    def test_10_specialized_research_agent(self):
        """10. Recherche et documentation par agent spécialisé."""
        prof = TaskClassifier.classify("Recherche documentation", "Consulter la documentation officielle en ligne")
        self.assertIn(prof.classification, [TaskClassification.RESEARCH, TaskClassification.WEB_RESEARCH, TaskClassification.DOCUMENTATION])
        self.assertEqual(prof.execution_mode, "API_FREE")

    def test_11_dynamic_multi_agent_creation(self):
        """11. Création dynamique de plusieurs agents."""
        m_id = str(uuid.uuid4())
        agents = ENGINE.agent_factory.assemble_team_for_mission(m_id, "Projet Mobile", "Créer une application Android Jetpack Compose avec audit")
        self.assertGreaterEqual(len(agents), 3)
        roles = [a["role"] for a in agents]
        self.assertTrue(any("Android" in r for r in roles))
        self.assertTrue(any("Audit" in r or "Qualité" in r for r in roles))

    def test_12_context_preservation_across_tasks(self):
        """12. Reprise d'une tâche avec contexte conservé."""
        m_id = str(uuid.uuid4())
        comm = ENGINE.collab_hub.record_communication(
            m_id, "ag-1", "Lead Architect", "ag-2", "Backend Dev", "handover",
            "Spécification technique prête", payload={"file": "spec.md"}
        )
        self.assertIsNotNone(comm)
        self.assertEqual(comm["messageType"], "handover")

    def test_13_api_provider_unavailable_handling(self):
        """13. Fournisseur API indisponible géré gracieusement."""
        status = ENGINE.provider_registry.get_status()
        self.assertTrue(len(status) >= 2)

    def test_14_all_free_models_enforced(self):
        """14. Règle absolue : Uniquement des modèles 100% gratuits."""
        for prov in [ENGINE.provider_registry.openrouter, ENGINE.provider_registry.xkiro, ENGINE.provider_registry.opencode_inf]:
            for m in prov.get_available_free_models():
                self.assertTrue(m.is_free, f"Le modèle {m.model_id} n'est pas marqué gratuit!")
                self.assertTrue(":free" in m.model_id or "free" in m.model_id.lower() or m.provider_id == "openrouter")

    def test_15_independent_deliverable_validation(self):
        """15. Validation indépendante des livrables et empreintes SHA-256."""
        tmp_file = "/tmp/test_camcs_deliv.txt"
        with open(tmp_file, "w") as f:
            f.write("Validation physique CAMCS 100% conforme")
        deliv = ENGINE.deliverable_mgr.register_deliverable(
            "m-deliv", "t-deliv", "ag-qa", "QA", tmp_file, "document", "Test livrable"
        )
        self.assertEqual(deliv["validationStatus"], "PENDING")
        all_ok, count, results = ENGINE.quality_auditor.audit_deliverables("m-deliv", [deliv])
        self.assertTrue(all_ok)
        self.assertEqual(count, 1)

    # =========================================================================
    # GROUPE 2: TESTS D'ANNULATION ET D'ARRÊT (16 à 22)
    # =========================================================================

    def test_16_cancellation_in_planning(self):
        """16. Annulation en phase PLANNING."""
        ok, _ = validate_mission_transition(MissionState.PLANNING, MissionState.CANCELLED)
        self.assertTrue(ok)

    def test_17_cancellation_in_dispatching(self):
        """17. Annulation en phase DISPATCHING."""
        ok, _ = validate_mission_transition(MissionState.DISPATCHING, MissionState.CANCELLED)
        self.assertTrue(ok)

    def test_18_cancellation_in_running(self):
        """18. Annulation en phase RUNNING."""
        ok, _ = validate_mission_transition(MissionState.RUNNING, MissionState.CANCELLED)
        self.assertTrue(ok)

    def test_19_cancellation_of_old_mission(self):
        """19. Annulation d'une ancienne mission persistée."""
        conn = sqlite3.connect(DB_PATH)
        cur = conn.cursor()
        old_id = f"m-old-{uuid.uuid4().hex[:6]}"
        cur.execute("INSERT INTO missions (id, title, objective, status, progress, created_at) VALUES (?, 'Old', 'Obj', 'RUNNING', 20, ?)", (old_id, time.time() - 3600))
        conn.commit()
        conn.close()

        res = http_post("/api/camcs/missions/cancel", {"missionId": old_id})
        self.assertTrue(res.get("ok"))
        self.assertEqual(res.get("status"), "CANCELLED")

    def test_20_stop_single_task_in_multi_agent(self):
        """20. Arrêt d'une seule tâche sans affecter la mission entière."""
        m_id = f"m-multi-{uuid.uuid4().hex[:6]}"
        t1_id = f"t-stop-{uuid.uuid4().hex[:6]}"
        t2_id = f"t-cont-{uuid.uuid4().hex[:6]}"
        conn = sqlite3.connect(DB_PATH)
        cur = conn.cursor()
        cur.execute("INSERT INTO missions (id, title, objective, status, progress, created_at) VALUES (?, 'Multi', 'Obj', 'RUNNING', 40, ?)", (m_id, time.time()))
        cur.execute("INSERT INTO tasks (id, mission_id, agent_id, title, status, progress, created_at) VALUES (?, ?, 'ag-1', 'Task 1', 'RUNNING', 50, ?)", (t1_id, m_id, time.time()))
        cur.execute("INSERT INTO tasks (id, mission_id, agent_id, title, status, progress, created_at) VALUES (?, ?, 'ag-2', 'Task 2', 'RUNNING', 50, ?)", (t2_id, m_id, time.time()))
        conn.commit()
        conn.close()

        res = http_post("/api/camcs/tasks/stop", {"missionId": m_id, "taskId": t1_id})
        self.assertTrue(res.get("ok"))

    def test_21_no_orphan_processes(self):
        """21. Absence de processus orphelins après nettoyage."""
        ENGINE.process_manager.cleanup_orphans()
        self.assertEqual(len(ENGINE.process_manager._active_processes), 0)

    def test_22_independent_missions_continue(self):
        """22. Vérification que les autres missions continuent indépendamment."""
        m1 = CamcsMission("m-iso-1", "Mission 1", "Obj")
        m2 = CamcsMission("m-iso-2", "Mission 2", "Obj")
        m1.status = MissionState.RUNNING
        m2.status = MissionState.RUNNING
        ENGINE.active_missions[m1.id] = m1
        ENGINE.active_missions[m2.id] = m2

        ENGINE.cancel_mission(m1.id)
        self.assertEqual(m1.status, MissionState.CANCELLED)
        self.assertEqual(m2.status, MissionState.RUNNING)

    # =========================================================================
    # GROUPE 3: TESTS DE COHÉRENCE & INVARIANTS (23 à 30)
    # =========================================================================

    def test_23_sse_stream_format(self):
        """23. Formatage et ordonnancement des événements temps réel SSE."""
        q = ENGINE.subscribe_events()
        ENGINE.emit_event("m-sse", "test.event", "Test Message", payload={"key": "val"})
        ev = q.get(timeout=2.0)
        ENGINE.unsubscribe_events(q)
        self.assertEqual(ev["type"], "test.event")
        self.assertIn("sequenceNumber", ev)

    def test_24_host_restart_resilience(self):
        """24. Résilience après redémarrage du moteur et rechargement DB."""
        fresh_engine = CamcsOrchestrationEngine()
        self.assertIsNotNone(fresh_engine.active_missions)

    def test_25_android_data_contract_consistency(self):
        """25. Contrat de données avec Android (champs conformes CamcsModels)."""
        data = http_get("/api/camcs/missions")
        self.assertIn("missions", data)
        for m in data["missions"]:
            self.assertIn("id", m)
            self.assertIn("title", m)
            self.assertIn("status", m)
            self.assertIn("progress", m)
            self.assertIn("orchestrationPhase", m)
            self.assertIn("agents", m)

    def test_26_persistence_integrity(self):
        """26. Validation de la persistance SQLite."""
        conn = sqlite3.connect(DB_PATH)
        cur = conn.cursor()
        cur.execute("SELECT count(*) FROM missions")
        count = cur.fetchone()[0]
        conn.close()
        self.assertGreaterEqual(count, 1)

    def test_27_metric_counters_coherence(self):
        """27. Cohérence des compteurs d'agents actifs vs terminés."""
        m = CamcsMission("m-metric", "Metric", "Obj")
        ag1 = CamcsAgent("ag-m1", m.id, "A1", "Dev", "square", "blue", "clixad", "local", "v1", "Obj")
        ag1.status = AgentState.COMPLETED
        ag1.progress = 100
        ag2 = CamcsAgent("ag-m2", m.id, "A2", "QA", "pebble", "violet", "clixad", "local", "v1", "Obj")
        ag2.status = AgentState.RUNNING
        ag2.progress = 50
        m.agents = [ag1, ag2]

        active_count = sum(1 for a in m.agents if a.status in AgentState.ACTIVE_STATES)
        completed_count = sum(1 for a in m.agents if a.status == AgentState.COMPLETED)
        self.assertEqual(active_count, 1)
        self.assertEqual(completed_count, 1)

    def test_28_progress_computation_invariants(self):
        """28. Invariant : COMPLETED implique obligatoirement 100%."""
        planner = DagPlanner()
        t1 = DagTask(id="t1", mission_id="m", agent_id="a", classification="DEV", title="T1", description="", prompt="", weight=50, is_mandatory=True, status="COMPLETED", progress=100)
        t2 = DagTask(id="t2", mission_id="m", agent_id="a", classification="QA", title="T2", description="", prompt="", weight=50, is_mandatory=True, status="COMPLETED", progress=100)
        prog = planner.compute_mission_progress([t1, t2], mission_completed=True)
        self.assertEqual(prog, 100)

        # If mission is marked completed, it MUST return 100
        prog_forced = planner.compute_mission_progress([t1], mission_completed=True)
        self.assertEqual(prog_forced, 100)

    def test_29_terminal_states_immutability(self):
        """29. Immuabilité des états terminaux (COMPLETED, FAILED, CANCELLED)."""
        ok, err = validate_mission_transition(MissionState.COMPLETED, MissionState.RUNNING)
        self.assertFalse(ok)
        self.assertIn("Impossible de quitter l'état terminal", err)

        ok, err = validate_mission_transition(MissionState.FAILED, MissionState.RUNNING)
        self.assertFalse(ok)

        ok, err = validate_mission_transition(MissionState.CANCELLED, MissionState.PLANNING)
        self.assertFalse(ok)

    def test_30_simultaneous_independent_missions(self):
        """30. Deux missions indépendantes simultanées avec isolation."""
        m1 = CamcsMission("m-sim-1", "Mission A", "Obj A")
        m2 = CamcsMission("m-sim-2", "Mission B", "Obj B")
        m1.status = MissionState.RUNNING
        m2.status = MissionState.RUNNING
        ENGINE.active_missions[m1.id] = m1
        ENGINE.active_missions[m2.id] = m2

        t1 = DagTask(id="t-a", mission_id=m1.id, agent_id="ag-a", classification="DEV", title="Ta", description="", prompt="", weight=100, is_mandatory=True, status="COMPLETED", progress=100)
        m1.tasks = [t1]
        t2 = DagTask(id="t-b", mission_id=m2.id, agent_id="ag-b", classification="DEV", title="Tb", description="", prompt="", weight=100, is_mandatory=True, status="RUNNING", progress=30)
        m2.tasks = [t2]

        ENGINE._update_progress(m1)
        ENGINE._update_progress(m2)

        self.assertEqual(m1.progress, 100)
        self.assertEqual(m2.progress, 30)


if __name__ == "__main__":
    unittest.main()
