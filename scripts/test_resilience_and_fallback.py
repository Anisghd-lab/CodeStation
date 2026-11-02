#!/usr/bin/env python3
"""
Test de Résilience & Fallback CAMCS v2.0
- Rendre temporairement indisponible un CLI
- Vérifier la détection automatique de panne
- Vérifier la cascade de remplacement (CLI alternatif -> API gratuite)
- Vérifier le fallback multi-providers 100% gratuits
"""

import os
import sys
import time
import json
import uuid

sys.path.insert(0, "/root/CodeStation/scripts")
from camcs_engine import ENGINE, CamcsMission, CamcsAgent
from camcs.dag_planner import DagTask
from camcs.task_classifier import TaskClassification
from camcs.runners.base_runner import BaseCliRunner, CliExecutionResult

class BrokenTestRunner(BaseCliRunner):
    """Simulates a failed/crashed CLI runner."""
    def __init__(self):
        super().__init__("broken_cli", "Broken CLI", "/bin/false")
        self.is_installed = True
        self.is_operational = False
        self.version = "0.0.0-broken"

    def initialize(self) -> bool:
        return True

    def health_check(self):
        return False, "BLOCKED", "Simulated crashed CLI"

    def cancel_task(self, task_id: str) -> bool:
        return True

    def execute_task(self, task_id, mission_id, prompt, cwd, timeout=30, output_callback=None, pid_callback=None):
        return CliExecutionResult(
            task_id=task_id,
            mission_id=mission_id,
            cli_id="broken_cli",
            exit_code=127,
            stdout="",
            stderr="Error: CLI execution crashed (Simulated Hardware/Network Failure)",
            duration_ms=15,
            working_dir=cwd,
            command_executed="/bin/false"
        )

def run_resilience_test():
    print("=" * 70)
    print("TEST DE RÉSILIENCE & CASCADE DE FALLBACK CAMCS v2.0")
    print("=" * 70)

    mission_id = f"m-resilience-{uuid.uuid4().hex[:6]}"
    mission = CamcsMission(mission_id, "Test Résilience", "Objectif sous contrainte de panne")
    
    # 1. Agent avec CLI défaillant
    broken_agent = CamcsAgent(
        agent_id="ag-broken",
        mission_id=mission_id,
        name="Fragile Worker",
        role="Ingénierie Système",
        shape="hexagon",
        color="rose",
        cli_id="broken_cli",
        provider_id="local_cli",
        model_id="default",
        objective="Tâche critique de génération"
    )

    task = DagTask(
        id="task-failover",
        mission_id=mission_id,
        agent_id="ag-broken",
        classification=TaskClassification.SOFTWARE_DEVELOPMENT.value,
        title="Génération avec CLI défaillant",
        description="Cette tâche doit automatiquement basculer sur un moteur sain",
        prompt="Génère une fonction Python simple calculate_crc32(data: bytes) -> int",
        assigned_file="/tmp/test_failover_solution.py"
    )

    # Injecter le faux runner cassé dans le registre
    broken_runner = BrokenTestRunner()
    ENGINE.cli_registry._runners["broken_cli"] = broken_runner

    print("\n[*] Étape 1 : Exécution d'une tâche avec CLI défaillant...")
    t0 = time.time()
    res, engine_name = ENGINE.execute_with_failover(
        mission=mission,
        agent=broken_agent,
        task=task,
        cwd="/tmp",
        timeout=15
    )
    elapsed = (time.time() - t0) * 1000

    print(f"    - Statut d'exécution : Exit Code = {res.exit_code}")
    print(f"    - Moteur de substitution retenu : {engine_name}")
    print(f"    - Commande ou Modèle utilisé : {res.command_executed}")
    print(f"    - Durée de basculement : {elapsed:.2f} ms")
    assert res.exit_code == 0, "Le fallback n'a pas réussi à produire un résultat valide"
    assert "broken" not in engine_name.lower(), "Le runner défaillant a été utilisé à tort"
    print("    -> Succès : Panne détectée et contournée automatiquement via substitution !")

    # 2. Test Fallback API Multi-Fournisseurs (100% Gratuits)
    print("\n[*] Étape 2 : Test du fallback multi-fournisseurs (xKIRO -> OpenRouter free)...")
    fb_manager = ENGINE.fallback_manager

    # Test requête normale avec routage automatique vers modèle gratuit
    messages = [
        {"role": "user", "content": "Renvoie uniquement 'OK' si tu es opérationnel."}
    ]
    ok, content, prov, model, dur = fb_manager.execute_with_fallback(
        task_type="research",
        messages=messages
    )
    print(f"    - Résultat appel API : Succès = {ok}")
    print(f"    - Fournisseur activé : {prov}")
    print(f"    - Modèle 100% gratuit utilisé : {model}")
    print(f"    - Temps de réponse : {dur:.2f} ms")
    assert ok, f"L'appel API gratuit a échoué : {content}"
    assert ":free" in model or "free" in model.lower(), f"Modèle non gratuit utilisé : {model}"

    print("\n" + "=" * 70)
    print("VERDICT DU TEST DE RÉSILIENCE : 100% SUCCÈS (VERIFIED)")
    print("=" * 70)
    return True

if __name__ == "__main__":
    success = run_resilience_test()
    sys.exit(0 if success else 1)
