#!/usr/bin/env python3
"""
Lightweight ACP Bridge for Freebuff CLI.
Speaks standard JSON-RPC 2.0 ACP stdio to CodeStation Host.
"""

import sys
import json
import threading

def send_response(req_id, result=None, error=None):
    resp = {"jsonrpc": "2.0", "id": req_id}
    if error is not None:
        resp["error"] = error
    else:
        resp["result"] = result or {}
    sys.stdout.write(json.dumps(resp) + "\n")
    sys.stdout.flush()

def send_notification(method, params):
    notif = {"jsonrpc": "2.0", "method": method, "params": params}
    sys.stdout.write(json.dumps(notif) + "\n")
    sys.stdout.flush()

def run_prompt(req_id, session_id, prompt_text):
    reply = (
        "Freebuff CLI (v0.2.35) est installé et authentifié dans Ubuntu PRoot.\n\n"
        "ℹ️ **Statut actuel** : La version 0.2.35 de Freebuff fonctionne exclusivement en mode TUI interactif "
        "(interface console plein écran). Pour une session de programmation avec Freebuff, lancez `freebuff` "
        "directement dans votre terminal PRoot/Termux. Le mode headless non-interactif sera activé dès la mise à jour amont."
    )
    send_notification("session/update", {
        "sessionId": session_id,
        "update": {
            "sessionUpdate": "agent_message_chunk",
            "content": {"type": "text", "text": reply}
        }
    })
    send_response(req_id, {
        "stopReason": "end_turn",
        "usage": {
            "inputTokens": max(len(prompt_text) // 4, 1),
            "outputTokens": max(len(reply) // 4, 1),
            "totalTokens": max((len(prompt_text) + len(reply)) // 4, 2)
        }
    })

def main():
    session_id = "freebuff-session-1"

    for raw_line in sys.stdin:
        line = raw_line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
        except Exception:
            continue

        method = req.get("method")
        req_id = req.get("id")
        params = req.get("params", {})

        if method == "initialize":
            send_response(req_id, {
                "protocolVersion": 1,
                "agentCapabilities": {
                    "loadSession": False,
                    "sessionCapabilities": {"close": {}}
                },
                "agentInfo": {
                    "name": "Freebuff",
                    "version": "0.2.35"
                }
            })
        elif method == "session/new":
            send_response(req_id, {
                "sessionId": session_id,
                "configOptions": [
                    {
                        "id": "model",
                        "category": "model",
                        "type": "select",
                        "currentValue": "freebuff/free-auto",
                        "options": [
                            {"value": "freebuff/free-auto", "name": "Freebuff Free Auto"}
                        ]
                    }
                ]
            })
        elif method == "session/prompt":
            session_id = params.get("sessionId", session_id)
            prompt_list = params.get("prompt", [])
            prompt_text = ""
            for item in prompt_list:
                if isinstance(item, dict) and item.get("type") == "text":
                    prompt_text += item.get("text", "") + " "
                elif isinstance(item, str):
                    prompt_text += item + " "
            prompt_text = prompt_text.strip()

            t = threading.Thread(
                target=run_prompt,
                args=(req_id, session_id, prompt_text),
                daemon=True
            )
            t.start()
        elif method in ("session/cancel", "session/close"):
            if req_id is not None:
                send_response(req_id, {})
        else:
            if req_id is not None:
                send_response(req_id, {})

if __name__ == "__main__":
    main()
