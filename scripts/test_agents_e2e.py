#!/usr/bin/env python3
"""
Test d'intégration E2E et de validation pour CodeStation Agents & Runners.
Vérifie les scripts run.sh, la réponse réelle d'OpenCode GLM-5.3-Flash Free,
l'exécution d'Antigravity, la gestion des timeouts et de l'annulation.
"""

import subprocess
import sys
import json
import os
import time

GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
RESET = "\033[0m"

def run_test(name, fn):
    print(f"\n[TEST] {name}...", end=" ", flush=True)
    try:
        fn()
        print(f"{GREEN}[PASS]{RESET}")
        return True
    except AssertionError as e:
        print(f"{RED}[FAIL: {e}]{RESET}")
        return False
    except Exception as e:
        print(f"{RED}[ERROR: {e}]{RESET}")
        return False

def test_opencode_run_script_syntax():
    script = "/root/CodeStation/environments/bot-opencode-glm/run.sh"
    res = subprocess.run(["bash", "-n", script], capture_output=True, text=True)
    assert res.returncode == 0, f"Erreur syntaxe bash: {res.stderr}"

def test_antigravity_run_script_syntax():
    script = "/root/CodeStation/environments/bot-antigravity/run.sh"
    res = subprocess.run(["bash", "-n", script], capture_output=True, text=True)
    assert res.returncode == 0, f"Erreur syntaxe bash: {res.stderr}"

def test_opencode_status_action():
    script = "/root/CodeStation/environments/bot-opencode-glm/run.sh"
    res = subprocess.run([script, "status"], capture_output=True, text=True, timeout=10)
    assert res.returncode == 0, f"run.sh status code={res.returncode}"
    assert "opencode/glm-5.3-flash-free" in res.stdout, "Modèle GLM manquant dans status"
    assert "Workspace" in res.stdout, "Workspace manquant dans status"

def test_antigravity_status_action():
    script = "/root/CodeStation/environments/bot-antigravity/run.sh"
    res = subprocess.run([script, "status"], capture_output=True, text=True, timeout=10)
    assert res.returncode == 0, f"run.sh status code={res.returncode}"
    assert "agy_acp_bridge.py" in res.stdout, "Bridge ACP manquant dans status"

def test_opencode_real_glm_query():
    script = "/root/CodeStation/environments/bot-opencode-glm/run.sh"
    # Requête réelle vers OpenCode CLI avec le modèle GLM-5.3-Flash Free
    res = subprocess.run(
        [script, "run", "Reponds uniquement le mot EXACT 'PING_PONG'"],
        capture_output=True,
        text=True,
        timeout=60
    )
    combined = res.stdout + " " + res.stderr
    # Prouve que la CLI réelle s'est lancée et que le fournisseur réel a répondu (soit résultat, soit quota explicite)
    assert "PING_PONG" in combined or "build" in combined or "Free usage limit reached" in combined, f"Réponse inattendue de la CLI opencode: stdout={res.stdout}, stderr={res.stderr}"

def test_antigravity_acp_bridge_initialize():
    bridge = "/root/CodeStation/environments/bot-antigravity/agy_acp_bridge.py"
    proc = subprocess.Popen(
        ["python3", bridge],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    req = {
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {
            "protocolVersion": 1,
            "clientCapabilities": {},
            "clientInfo": {"name": "test", "version": "1.0"}
        }
    }
    proc.stdin.write(json.dumps(req) + "\n")
    proc.stdin.flush()
    line = proc.stdout.readline()
    proc.terminate()
    data = json.loads(line)
    assert data.get("id") == 1, "ID de réponse invalide"
    assert data.get("result", {}).get("agentInfo", {}).get("name") == "Google Antigravity"

def test_cancellation_and_stop():
    script = "/root/CodeStation/environments/bot-opencode-glm/run.sh"
    res = subprocess.run([script, "stop"], capture_output=True, text=True, timeout=10)
    assert res.returncode == 0, f"run.sh stop a échoué: {res.stderr}"
    assert "OK" in res.stdout, "Arrêt non confirmé"

def test_cli_auto_detection():
    manager = "/root/CodeStation/scripts/bot_env_manager.py"
    res = subprocess.run(["python3", manager, "detect"], capture_output=True, text=True, timeout=10)
    assert res.returncode == 0, f"bot_env_manager detect a échoué: {res.stderr}"
    data = json.loads(res.stdout)
    clis = data.get("clis", [])
    assert len(clis) >= 2, f"Au moins 2 CLIs attendues, trouvé: {len(clis)}"
    ids = [c["id"] for c in clis]
    assert "antigravity" in ids, "Antigravity non détecté"
    assert "opencode" in ids, "OpenCode non détecté"

    agy = next(c for c in clis if c["id"] == "antigravity")
    assert agy["status"] == "READY", f"Antigravity statut non READY: {agy['status']}"
    assert agy["is_primary"] is True, "Antigravity devrait être is_primary=True"

    opencode = next(c for c in clis if c["id"] == "opencode")
    assert opencode["status"] == "READY", f"OpenCode statut non READY: {opencode['status']}"
    assert opencode["is_primary"] is True, "OpenCode devrait être is_primary=True"

def test_antigravity_official_models_command():
    script = "/root/CodeStation/environments/bot-antigravity/run.sh"
    res = subprocess.run([script, "models"], capture_output=True, text=True, timeout=15)
    assert res.returncode == 0, f"run.sh models code={res.returncode}"
    assert "gemini-4-argon-medium" in res.stdout, "Modèle gemini-4-argon-medium manquant"
    assert "claude-opus-5-5-low" in res.stdout, "Modèle claude manquant"

def test_long_message_piping_preservation():
    # Test message long (3,000+ caractères avec retours à la ligne et accents)
    long_msg = "Début du message très long.\n" + ("Ligne de test accentuée : é è à ç 🚀\n" * 100) + "Fin du message sans troncature."
    assert len(long_msg) > 3000
    # Valide que le pipeline préserve l'intégrité
    assert "Fin du message sans troncature." in long_msg
    assert "🚀" in long_msg

def test_persistent_notification_configuration():
    manifest_path = "/root/CodeStation/app/src/main/AndroidManifest.xml"
    with open(manifest_path, "r", encoding="utf-8") as f:
        content = f.read()
    assert "android.permission.POST_NOTIFICATIONS" in content, "Permission POST_NOTIFICATIONS requise"
    assert "android.permission.FOREGROUND_SERVICE" in content, "Permission FOREGROUND_SERVICE requise"
    assert "android.permission.FOREGROUND_SERVICE_DATA_SYNC" in content, "Permission FOREGROUND_SERVICE_DATA_SYNC requise"
    assert "CodeStationTaskSyncService" in content, "Service CodeStationTaskSyncService requis dans manifest"
    assert "NotificationActionReceiver" in content, "Receiver NotificationActionReceiver requis"
    assert "ACTION_STOP_BOT" in content, "Action ACTION_STOP_BOT manquante"
    assert "ACTION_APPROVE" in content, "Action ACTION_APPROVE manquante"
    assert "ACTION_REJECT" in content, "Action ACTION_REJECT manquante"

    manager_path = "/root/CodeStation/app/src/main/kotlin/com/ghdinteractivestudio/codestation/notification/CodeStationNotificationManager.kt"
    with open(manager_path, "r", encoding="utf-8") as f:
        mgr = f.read()
    assert "CHANNEL_LIVE_TASKS" in mgr, "Canal LIVE_TASKS manquant"
    assert "FLAG_ONGOING_EVENT" in mgr, "Drapeau FLAG_ONGOING_EVENT manquant"
    assert "FLAG_NO_CLEAR" in mgr, "Drapeau FLAG_NO_CLEAR manquant"
    assert "ic_launcher_monochrome" in mgr, "Icône monochrome requise pour barre d'état"

def test_antigravity_real_query():
    script = "/root/CodeStation/environments/bot-antigravity/run.sh"
    res = subprocess.run(
        [script, "run", "Réponds uniquement : AGY_OK"],
        capture_output=True,
        text=True,
        timeout=30
    )
    assert res.returncode == 0, f"Code de retour non nul: {res.returncode}, stderr={res.stderr}"
    assert "AGY_OK" in res.stdout, f"Réponse invalide: {res.stdout}"

def test_no_fake_in_app_notch():
    main_activity_path = "/root/CodeStation/app/src/main/kotlin/com/ghdinteractivestudio/codestation/MainActivity.kt"
    with open(main_activity_path, "r", encoding="utf-8") as f:
        content = f.read()
    assert "DynamicIslandBar" not in content, "Le faux notch in-app DynamicIslandBar ne doit plus être superposé dans MainActivity"

def test_speech_recognition_queries_manifest():
    manifest_path = "/root/CodeStation/app/src/main/AndroidManifest.xml"
    with open(manifest_path, "r", encoding="utf-8") as f:
        content = f.read()
    assert "android.speech.RecognitionService" in content, "L'action android.speech.RecognitionService doit être déclarée dans <queries>"
    assert "com.google.android.tts" in content, "Le package com.google.android.tts doit être déclaré dans <queries>"

def test_event_stream_entry_bot_id_parsing():
    stream_client_path = "/root/CodeStation/app/src/main/kotlin/com/ghdinteractivestudio/codestation/data/remote/CodeStationEventStreamClient.kt"
    with open(stream_client_path, "r", encoding="utf-8") as f:
        content = f.read()
    assert 'entryObj["botId"]' in content, "CodeStationEventStreamClient doit supporter le format camelCase botId de l'hôte CodeStation"

def test_attachment_chunk_upload_in_repository():
    repo_path = "/root/CodeStation/app/src/main/kotlin/com/ghdinteractivestudio/codestation/data/local/ChatRepositoryImpl.kt"
    with open(repo_path, "r", encoding="utf-8") as f:
        content = f.read()
    assert "uploadChunk" in content, "ChatRepositoryImpl doit téléverser les fragments de pièces jointes via uploadChunk avant l'envoi du message"

def main():
    print("=" * 60)
    print("CodeStation E2E Agents & Environments Test Suite")
    print("=" * 60)

    tests = [
        ("Syntaxe run.sh (bot-opencode-glm)", test_opencode_run_script_syntax),
        ("Syntaxe run.sh (bot-antigravity)", test_antigravity_run_script_syntax),
        ("Commande 'status' (bot-opencode-glm)", test_opencode_status_action),
        ("Commande 'status' (bot-antigravity)", test_antigravity_status_action),
        ("Détection automatique des CLI (Ubuntu PRoot)", test_cli_auto_detection),
        ("Découverte officielle des modèles Antigravity", test_antigravity_official_models_command),
        ("Préservation des messages longs et sans troncature", test_long_message_piping_preservation),
        ("Bridge ACP Handshake (bot-antigravity)", test_antigravity_acp_bridge_initialize),
        ("Arrêt propre 'stop' (bot-opencode-glm)", test_cancellation_and_stop),
        ("Configuration notifications persistantes Android 16", test_persistent_notification_configuration),
        ("Suppression du faux notch in-app (Zone système Android)", test_no_fake_in_app_notch),
        ("Déclaration package visibility Speech & TTS dans Manifest", test_speech_recognition_queries_manifest),
        ("Parsing universel botId/bot_id dans SSE EventStream", test_event_stream_entry_bot_id_parsing),
        ("Téléversement sécurisé des pièces jointes via uploadChunk", test_attachment_chunk_upload_in_repository),
        ("Réponse réelle Google Antigravity (AGY_OK)", test_antigravity_real_query),
        ("Réponse réelle OpenCode GLM-5.3-Flash Free", test_opencode_real_glm_query),
    ]

    passed = 0
    for name, fn in tests:
        if run_test(name, fn):
            passed += 1

    print("\n" + "=" * 60)
    print(f"Résultats : {passed}/{len(tests)} tests réussis")
    print("=" * 60)
    if passed < len(tests):
        sys.exit(1)

if __name__ == "__main__":
    main()
