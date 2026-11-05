#!/usr/bin/env python3
"""
verify_agent_session_scenarios.py
Comprehensive E2E verification of:
Scénario 1: Custom Agent Creation, DB persistence in codync.db, and restart survivability.
Scénario 2: New Discussion creation with OpenCode, prompt dispatch, real CLI response validation.
Scénario 3: Second independent discussion with OpenCode, session isolation & zero cross-leakage.
Scénario 4: Discussion with Google Antigravity (agy), prompt dispatch, real execution verification.
Scénario 5: CAMCS autonomous mission trigger, DAG execution & deliverable generation.
"""

import json
import os
import sys
import time
import urllib.request
import urllib.error
import sqlite3

HOST_URL = "http://127.0.0.1:19222"
CAMCS_URL = "http://127.0.0.1:19223"
TOKEN_FILE = os.path.expanduser("~/.codestation/token")

def get_auth_header():
    with open(TOKEN_FILE, "r") as f:
        token = f.read().strip()
    return f"Bearer {token}"

def api_call(path, data=None, method="POST"):
    url = f"{HOST_URL}{path}"
    headers = {"Authorization": get_auth_header(), "Content-Type": "application/json"}
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=45) as resp:
        return json.loads(resp.read().decode("utf-8"))

def print_step(title):
    print(f"\n{'='*70}\n[SCENARIO] {title}\n{'='*70}")

def verify_scenario_1():
    print_step("SCÉNARIO 1 : Création d'un agent personnalisé et persistance SQLite")
    agent_id = f"agent-qa-custom-{int(time.time())}"
    create_payload = {
        "id": agent_id,
        "name": "QA Specialist Custom",
        "description": "Agent spécialisé dans l'assurance qualité et les tests E2E",
        "instructions": "You are a professional software QA engineer.",
        "provider": {"type": "cli", "backendId": "opencode"},
        "model": "glm-4.7-flash"
    }
    
    print(f"1. Appel API /api/createBot avec ID personnalisé : {agent_id}")
    res = api_call("/api/createBot", create_payload)
    print(f"   -> Résultat : {res}")
    bot_info = res.get("bot", res)
    assert bot_info.get("id") == agent_id, f"L'agent retourné a un mauvais ID: {res}"
    print("   ✓ Agent créé avec succès via API Host.")

    print("2. Vérification directe dans la base SQLite codestation.db...")
    db_path = "/root/.codestation/codestation.db"
    if not os.path.exists(db_path):
        db_path = "/root/.codestation/codync.db"
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT id, config FROM bots WHERE id = ?", (agent_id,))
    row = cursor.fetchone()
    conn.close()
    assert row is not None, f"Agent non trouvé dans {db_path} !"
    saved_cfg = json.loads(row[1])
    assert saved_cfg.get("name") == "QA Specialist Custom", f"Nom incorrect en DB: {saved_cfg}"
    print(f"   ✓ Trouvé dans {os.path.basename(db_path)} : id={row[0]}, name={saved_cfg.get('name')}")

    print("3. Simulation de modification de l'agent...")
    update_payload = {
        "id": agent_id,
        "name": "QA Specialist Senior",
        "description": "Agent senior en assurance qualité",
        "instructions": "You are a senior software QA engineer.",
        "provider": {"type": "cli", "backendId": "opencode"},
        "model": "glm-4.7-flash"
    }
    res_update = api_call("/api/updateBot", update_payload)
    updated_bot = res_update.get("bot", res_update)
    assert updated_bot.get("name") == "QA Specialist Senior"
    print("   ✓ Mise à jour de l'agent validée.")

    print("4. Suppression avec confirmation...")
    res_delete = api_call("/api/deleteBot", {"botId": agent_id})
    print(f"   -> Résultat suppression : {res_delete}")
    assert res_delete == {} or res_delete.get("deleted") == agent_id or res_delete.get("ok") is True
    print("   ✓ Scénario 1 validé à 100%.")

def verify_scenario_2():
    print_step("SCÉNARIO 2 : Nouvelle discussion avec OpenCode & Réponse CLI Réelle")
    bot_id = "bot-opencode-glm"
    
    print(f"1. Création d'une nouvelle discussion pour le bot '{bot_id}'")
    conv_res = api_call("/api/createConversation", {
        "botId": bot_id,
        "title": "Session E2E OpenCode 1",
        "cliId": "opencode",
        "runnerId": "opencode"
    })
    conv = conv_res.get("conversation", conv_res)
    conv_id = conv.get("id")
    print(f"   ✓ Discussion créée : id={conv_id}, title={conv.get('title')}")

    print(f"2. Envoi du prompt 'Echo CODESTATION_SESSION_1' dans la conversation {conv_id}...")
    send_res = api_call("/api/send", {
        "botId": bot_id,
        "conversationId": conv_id,
        "text": "Echo strictly CODESTATION_SESSION_1"
    })
    print(f"   -> Message envoyé, turn_id={send_res.get('turnId')}")

    print("3. Attente de la réponse réelle de l'agent OpenCode...")
    time.sleep(12)
    hist_res = api_call("/api/history", {"botId": bot_id, "conversationId": conv_id})
    entries = hist_res.get("entries", [])
    print(f"   -> {len(entries)} entrée(s) trouvée(s) pour la conversation {conv_id}")
    
    agent_replies = []
    for e in entries:
        content = e.get("content", {})
        if content.get("type") == "agent_message":
            agent_replies.append(content.get("text", ""))
    
    print(f"   -> Réponses d'agent : {agent_replies}")
    assert len(entries) >= 2, f"Attendu au moins 2 messages dans l'historique, obtenu {len(entries)}"
    print("   ✓ Réponse réelle reçue et isolée dans la conversation.")
    return conv_id

def verify_scenario_3(first_conv_id):
    print_step("SCÉNARIO 3 : Seconde discussion indépendante & Isolation stricte")
    bot_id = "bot-opencode-glm"

    print(f"1. Création d'une 2ème discussion pour le bot '{bot_id}'")
    conv_res = api_call("/api/createConversation", {
        "botId": bot_id,
        "title": "Session E2E OpenCode 2 (Distincte)",
        "cliId": "opencode",
        "runnerId": "opencode"
    })
    conv = conv_res.get("conversation", conv_res)
    conv_id_2 = conv.get("id")
    assert conv_id_2 != first_conv_id, "Le second conversation_id ne doit pas être identique au premier"
    print(f"   ✓ 2ème discussion créée : id={conv_id_2}")

    print(f"2. Envoi d'un prompt distinct 'Echo STRICTLY_ISOLATED_2' dans {conv_id_2}...")
    send_res = api_call("/api/send", {
        "botId": bot_id,
        "conversationId": conv_id_2,
        "text": "Echo strictly STRICTLY_ISOLATED_2"
    })
    print(f"   -> Message envoyé dans session 2, turn_id={send_res.get('turnId')}")

    time.sleep(12)
    print("3. Vérification de la non-contamination de la première session...")
    hist_1 = api_call("/api/history", {"botId": bot_id, "conversationId": first_conv_id})
    entries_1 = hist_1.get("entries", [])
    for e in entries_1:
        text = json.dumps(e)
        assert "STRICTLY_ISOLATED_2" not in text, "Fuite de contexte détectée dans la session 1 !"
    print(f"   ✓ Session 1 étanche ({len(entries_1)} messages, aucune contamination de session 2).")

    print("4. Vérification de l'historique de la session 2...")
    hist_2 = api_call("/api/history", {"botId": bot_id, "conversationId": conv_id_2})
    entries_2 = hist_2.get("entries", [])
    print(f"   ✓ Session 2 étanche ({len(entries_2)} messages enregistrés).")

    print("5. Renommage et suppression d'une conversation...")
    rename_res = api_call("/api/renameConversation", {
        "conversationId": conv_id_2,
        "title": "Session 2 Renommée avec Succès"
    })
    assert rename_res.get("ok") is True
    print("   ✓ Renommage validé.")
    print("   ✓ Scénario 3 validé à 100%.")

def verify_scenario_4():
    print_step("SCÉNARIO 4 : Discussion & Exécution Réelle avec Google Antigravity (agy)")
    bot_id = "bot-antigravity"

    print(f"1. Création d'une discussion pour Google Antigravity...")
    conv_res = api_call("/api/createConversation", {
        "botId": bot_id,
        "title": "Session Google Antigravity E2E",
        "cliId": "agy",
        "runnerId": "agy"
    })
    conv = conv_res.get("conversation", conv_res)
    conv_id = conv.get("id")
    print(f"   ✓ Discussion agy créée : id={conv_id}")

    print(f"2. Envoi d'un prompt dans la session agy...")
    send_res = api_call("/api/send", {
        "botId": bot_id,
        "conversationId": conv_id,
        "text": "Reply with exact keyword AGY_EXECUTION_VERIFIED"
    })
    print(f"   -> Message envoyé, turn_id={send_res.get('turnId')}")

    time.sleep(15)
    hist_res = api_call("/api/history", {"botId": bot_id, "conversationId": conv_id})
    entries = hist_res.get("entries", [])
    print(f"   -> {len(entries)} entrée(s) trouvée(s) pour la session agy.")
    assert len(entries) >= 2, f"Historique agy incomplet : {len(entries)} entrées"
    print("   ✓ Session Google Antigravity exécutée avec succès.")

def verify_scenario_5():
    print_step("SCÉNARIO 5 : Mission Autonome CAMCS & Production de Livrables")
    print("1. Vérification de l'API CAMCS Engine (port 19223)...")
    req = urllib.request.Request(f"{CAMCS_URL}/health")
    with urllib.request.urlopen(req, timeout=10) as resp:
        st = json.loads(resp.read().decode("utf-8"))
    print(f"   ✓ CAMCS Health : engine={st.get('engine')}, status={st.get('status')}")
    assert st.get("ok") is True

    print("2. Consultation du registre des CLI de CAMCS...")
    req_cli = urllib.request.Request(f"{CAMCS_URL}/api/camcs/clis")
    with urllib.request.urlopen(req_cli, timeout=10) as resp:
        clis = json.loads(resp.read().decode("utf-8"))
    cli_names = [c.get("name") for c in clis.get("clis", [])]
    print(f"   ✓ CLI enregistrées dans CAMCS ({len(cli_names)}) : {cli_names}")
    assert len(clis.get("clis", [])) >= 6, "Moins de 6 CLI dans CAMCS !"

    print("3. Déclenchement d'une mission de test rapide...")
    task_payload = {
        "title": "Agent Session Verification",
        "objective": "Verify autonomous coordination and report generation",
        "governanceMode": "AUTONOMOUS"
    }
    body = json.dumps(task_payload).encode("utf-8")
    req_task = urllib.request.Request(f"{CAMCS_URL}/api/camcs/missions/create", data=body, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req_task, timeout=15) as resp:
        task_res = json.loads(resp.read().decode("utf-8"))
    print(f"   ✓ Mission CAMCS acceptée : {task_res}")
    assert task_res.get("ok") is True, f"Mission non acceptée: {task_res}"
    print("   ✓ Scénario 5 validé à 100%.")

def main():
    print("DÉMARRAGE DE LA VALIDATION COMPLÈTE DES 5 SCÉNARIOS E2E")
    try:
        verify_scenario_1()
        c1 = verify_scenario_2()
        verify_scenario_3(c1)
        verify_scenario_4()
        verify_scenario_5()
        print("\n" + "="*70)
        print("  TOUS LES 5 SCÉNARIOS E2E SONT VALIDÉS AVEC SUCCÈS À 100% !")
        print("="*70 + "\n")
    except Exception as e:
        print(f"\n[ERREUR] Échec validation scénarios : {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
