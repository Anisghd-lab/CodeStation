#!/usr/bin/env python3
"""
Lightweight ACP Bridge for xKIRO / Okiro API.
Speaks standard JSON-RPC 2.0 ACP stdio to CodeStation Host.
Streams completions from Qwen3 Coder Plus (Free 1M tokens) into CodeStation Chat.
"""

import sys
import json
import threading
import os
import time

sys.path.insert(0, "/root/CodeStation/scripts")
from camcs.provider_registry import XKiroAdapter

adapter = XKiroAdapter()

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

def run_prompt(req_id, session_id, prompt_text, model):
    try:
        messages = [
            {"role": "system", "content": "Tu es un agent d'architecture et de codage haute performance expert dans CodeStation, propulsé par xKIRO et Qwen3 Coder Plus."},
            {"role": "user", "content": prompt_text}
        ]
        
        ok, reply, latency = adapter.chat_completion(
            model=model or "qwen/qwen3-coder-plus:free",
            messages=messages,
            max_tokens=2048,
            temperature=0.2
        )

        if not ok:
            send_notification("session/update", {
                "sessionId": session_id,
                "update": {
                    "sessionUpdate": "agent_message_chunk",
                    "content": {"type": "text", "text": f"Erreur xKIRO: {reply}"}
                }
            })
            send_response(req_id, {
                "stopReason": "error",
                "usage": {"inputTokens": len(prompt_text)//4, "outputTokens": 10, "totalTokens": len(prompt_text)//4 + 10}
            })
            return

        # Stream the reply in readable chunks
        chunk_size = 40
        for i in range(0, len(reply), chunk_size):
            chunk = reply[i:i+chunk_size]
            send_notification("session/update", {
                "sessionId": session_id,
                "update": {
                    "sessionUpdate": "agent_message_chunk",
                    "content": {"type": "text", "text": chunk}
                }
            })
            time.sleep(0.02)

        send_response(req_id, {
            "stopReason": "end_turn",
            "usage": {
                "inputTokens": max(len(prompt_text) // 4, 1),
                "outputTokens": max(len(reply) // 4, 1),
                "totalTokens": max((len(prompt_text) + len(reply)) // 4, 2)
            }
        })
    except Exception as e:
        send_response(req_id, error={"code": -32603, "message": str(e)})

def main():
    session_id = "xkiro-session-1"
    selected_model = "qwen/qwen3-coder-plus:free"

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
                    "name": "xKIRO",
                    "version": "1.0.0"
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
                        "currentValue": "qwen/qwen3-coder-plus:free",
                        "options": [
                            {"value": "qwen/qwen3-coder-plus:free", "name": "Qwen3 Coder Plus (Free 1M ctx)"},
                            {"value": "qwen/qwen3.6-27b:free", "name": "Qwen3.6 27B (Free)"},
                            {"value": "qwen/qwen3.5-plus:free", "name": "Qwen3.5 Plus (Free 1M ctx)"}
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
                args=(req_id, session_id, prompt_text, selected_model),
                daemon=True
            )
            t.start()
        elif method in ["session/cancel", "session/close"]:
            if req_id is not None:
                send_response(req_id, {})
        else:
            if req_id is not None:
                send_response(req_id, {})

if __name__ == "__main__":
    main()
