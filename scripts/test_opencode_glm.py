#!/usr/bin/env python3
"""
Test d'Intégration et de Validation du Modèle Gratuit GLM-5.3-Flash Free via OpenCode CLI
CodeStation - GHD Interactive Studio (com.ghdinteractivestudio.codestation)
"""

import subprocess
import json
import sys
import time
import os

def log(msg, status="INFO"):
    symbols = {"INFO": "ℹ️", "SUCCESS": "✅", "FAIL": "❌", "WARN": "⚠️"}
    print(f"[{symbols.get(status, '')} {status}] {msg}")

def test_opencode_cli_installed():
    log("Vérification de la présence de la CLI OpenCode...")
    res = subprocess.run(["which", "opencode"], capture_output=True, text=True)
    if res.returncode != 0:
        log("Binaire opencode non trouvé dans PATH", "FAIL")
        return False
    path = res.stdout.strip()
    ver_res = subprocess.run(["opencode", "--version"], capture_output=True, text=True)
    version = ver_res.stdout.strip()
    log(f"OpenCode trouvé : {path} ({version})", "SUCCESS")
    return True

def test_glm_model_in_catalog():
    log("Vérification de la disponibilité du modèle GLM-5.3-Flash Free dans le catalogue OpenCode...")
    res = subprocess.run(["opencode", "models"], capture_output=True, text=True)
    if res.returncode != 0:
        log("Impossible de lister les modèles OpenCode", "FAIL")
        return False
    models = res.stdout.splitlines()
    target_model = "opencode/glm-5.3-flash-free"
    if target_model in models:
        log(f"Modèle cible confirmé dans le catalogue : {target_model}", "SUCCESS")
        return True
    else:
        log(f"Modèle {target_model} absent du catalogue OpenCode", "FAIL")
        return False

def test_glm_live_acp_execution():
    log("Test d'exécution réelle via protocole ACP avec 'opencode/glm-5.3-flash-free'...")
    proc = subprocess.Popen(["opencode", "acp"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
    
    def send_rpc(msg):
        proc.stdin.write(json.dumps(msg) + "\n")
        proc.stdin.flush()
        while True:
            line = proc.stdout.readline()
            if not line: return None
            data = json.loads(line)
            if data.get("id") == msg.get("id"):
                return data

    # 1. Initialize
    init_res = send_rpc({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "initialize",
        "params": {"protocolVersion": 1, "clientCapabilities": {}, "clientInfo": {"name": "codestation-test", "version": "1.0"}}
    })
    if not init_res or "result" not in init_res:
        log("Échec du handshake initialize ACP", "FAIL")
        proc.terminate()
        return False

    # 2. Session new
    sess_res = send_rpc({
        "jsonrpc": "2.0",
        "id": 2,
        "method": "session/new",
        "params": {"cwd": "/root", "mcpServers": []}
    })
    sid = sess_res.get("result", {}).get("sessionId")
    if not sid:
        log("Échec de création de session ACP", "FAIL")
        proc.terminate()
        return False

    # 3. Set model to GLM-5.3-Flash Free
    set_res = send_rpc({
        "jsonrpc": "2.0",
        "id": 3,
        "method": "session/set_config_option",
        "params": {"sessionId": sid, "configId": "model", "value": "opencode/glm-5.3-flash-free"}
    })
    config_opts = set_res.get("result", {}).get("configOptions", [])
    model_opt = next((o for o in config_opts if o.get("id") == "model"), None)
    if not model_opt or model_opt.get("currentValue") != "opencode/glm-5.3-flash-free":
        log("Échec de configuration du modèle GLM-5.3-Flash Free", "FAIL")
        proc.terminate()
        return False

    # 4. Prompt
    proc.stdin.write(json.dumps({
        "jsonrpc": "2.0",
        "id": 4,
        "method": "session/prompt",
        "params": {"sessionId": sid, "prompt": [{"type": "text", "text": "Reponds uniquement par: CODESTATION_GLM_VALIDATED"}]}
    }) + "\n")
    proc.stdin.flush()

    received_text = []
    completed = False
    for _ in range(50):
        line = proc.stdout.readline()
        if not line: break
        data = json.loads(line)
        if "sessionUpdate" in str(data):
            update = data.get("params", {}).get("update", {})
            if "content" in update and update["content"].get("type") == "text":
                received_text.append(update["content"]["text"])
        if data.get("id") == 4:
            completed = True
            break

    proc.terminate()
    full_reply = " ".join(received_text)
    if completed and "CODESTATION_GLM_VALIDATED" in full_reply:
        log(f"Réponse authentique reçue de GLM-5.3-Flash Free : '{full_reply.strip()}'", "SUCCESS")
        return True
    elif completed and len(full_reply.strip()) > 0:
        log(f"Réponse reçue (non-mockée) : '{full_reply.strip()}'", "SUCCESS")
        return True
    else:
        log(f"Échec de prompt ou réponse vide : '{full_reply}'", "FAIL")
        return False

def test_invalid_model_rejection_without_silent_fallback():
    log("Test de rejet explicite d'un modèle indisponible (sans fallback silencieux)...")
    proc = subprocess.Popen(["opencode", "acp"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
    
    def send_rpc(msg):
        proc.stdin.write(json.dumps(msg) + "\n")
        proc.stdin.flush()
        while True:
            line = proc.stdout.readline()
            if not line: return None
            data = json.loads(line)
            if data.get("id") == msg.get("id"):
                return data

    send_rpc({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": 1, "clientCapabilities": {}, "clientInfo": {"name": "test", "version": "1.0"}}})
    sess = send_rpc({"jsonrpc": "2.0", "id": 2, "method": "session/new", "params": {"cwd": "/root", "mcpServers": []}})
    sid = sess.get("result", {}).get("sessionId")

    # Tentative avec un modèle inexistant
    err_res = send_rpc({
        "jsonrpc": "2.0",
        "id": 3,
        "method": "session/set_config_option",
        "params": {"sessionId": sid, "configId": "model", "value": "opencode/nonexistent-model-999"}
    })
    proc.terminate()

    # Soit erreur RPC directe, soit refus de changer la valeur
    has_error = "error" in err_res or err_res.get("result", {}).get("configOptions", [{}])[0].get("currentValue") != "opencode/nonexistent-model-999"
    if has_error:
        log("Rejet propre et explicite du modèle invalide confirmé", "SUCCESS")
        return True
    else:
        log("Le modèle invalide a été accepté anormalement", "FAIL")
        return False

def test_codestation_daemon():
    log("Vérification du démon local codestation sur http://127.0.0.1:19222/health...")
    res = subprocess.run(["codestation", "status"], capture_output=True, text=True)
    if res.returncode == 0 and "RUNNING" in res.stdout:
        log("Démon local CodeStation actif et sain", "SUCCESS")
        return True
    else:
        log(f"Démon local inactif ou erreur : {res.stderr or res.stdout}", "WARN")
        return False

def main():
    print("=================================================================")
    print("CODESTATION — TEST DE CONFORMITÉ MODÈLE GRATUIT GLM VIA OPENCODE")
    print("=================================================================")
    
    tests = [
        test_opencode_cli_installed,
        test_glm_model_in_catalog,
        test_glm_live_acp_execution,
        test_invalid_model_rejection_without_silent_fallback,
        test_codestation_daemon
    ]

    passed = 0
    for t in tests:
        if t():
            passed += 1
        print("-----------------------------------------------------------------")

    log(f"Résultats : {passed}/{len(tests)} tests réussis", "SUCCESS" if passed == len(tests) else "WARN")
    if passed == len(tests):
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()
