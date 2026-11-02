#!/usr/bin/env python3
"""
Lightweight ACP Bridge for Clixad CLI.
Speaks standard JSON-RPC 2.0 ACP stdio to CodeStation Host.
Runs `clixad ask "<prompt>"` non-interactively.
"""

import sys
import json
import subprocess
import threading
import os
import time

active_procs = {}
active_procs_lock = threading.Lock()

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

def run_prompt(req_id, session_id, prompt_text, cwd):
    try:
        env = os.environ.copy()
        env["PATH"] = "/usr/local/bin:/usr/bin:/bin:/root/.local/bin:" + env.get("PATH", "")
        
        cmd = ["clixad", "ask", prompt_text]
        proc = subprocess.Popen(
            cmd,
            cwd=cwd if os.path.isdir(cwd) else "/root/CodeStation",
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env=env
        )

        with active_procs_lock:
            active_procs[session_id] = proc

        stdout_chunks = []
        stderr_chunks = []

        def read_stderr():
            if proc.stderr:
                for line in proc.stderr:
                    stderr_chunks.append(line)

        err_thread = threading.Thread(target=read_stderr, daemon=True)
        err_thread.start()

        for raw_line in proc.stdout:
            clean_line = raw_line
            # Strip leading "assistant " prefix if present
            if clean_line.startswith("assistant "):
                clean_line = clean_line[len("assistant "):]
            
            stdout_chunks.append(clean_line)
            send_notification("session/update", {
                "sessionId": session_id,
                "update": {
                    "sessionUpdate": "agent_message_chunk",
                    "content": {"type": "text", "text": clean_line}
                }
            })

        proc.wait(timeout=180.0)
        err_thread.join(timeout=1.0)

        with active_procs_lock:
            active_procs.pop(session_id, None)

        send_response(req_id, {
            "stopReason": "end_turn",
            "usage": {
                "inputTokens": max(len(prompt_text) // 4, 1),
                "outputTokens": max(sum(len(c) for c in stdout_chunks) // 4, 1),
                "totalTokens": max((len(prompt_text) + sum(len(c) for c in stdout_chunks)) // 4, 2)
            }
        })
    except Exception as e:
        with active_procs_lock:
            active_procs.pop(session_id, None)
        send_response(req_id, error={"code": -32603, "message": str(e)})

def main():
    session_cwd = "/root/CodeStation"
    session_id = "clixad-session-1"

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
                    "name": "Clixad",
                    "version": "0.0.1-beta.41"
                }
            })
        elif method == "session/new":
            session_cwd = params.get("cwd", "/root/CodeStation")
            send_response(req_id, {
                "sessionId": session_id,
                "configOptions": [
                    {
                        "id": "model",
                        "category": "model",
                        "type": "select",
                        "currentValue": "gpt-5-nano",
                        "options": [
                            {"value": "gpt-5-nano", "name": "GPT-5 Nano (Free)"},
                            {"value": "deepseek-v4.1-flash", "name": "DeepSeek V4.1 Flash"},
                            {"value": "gemini-3-flash", "name": "Gemini 3 Flash"}
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
                args=(req_id, session_id, prompt_text, session_cwd),
                daemon=True
            )
            t.start()
        elif method == "session/cancel":
            with active_procs_lock:
                p = active_procs.pop(session_id, None)
                if p:
                    try:
                        p.terminate()
                    except Exception:
                        pass
            if req_id is not None:
                send_response(req_id, {})
        elif method == "session/close":
            with active_procs_lock:
                p = active_procs.pop(session_id, None)
                if p:
                    try:
                        p.terminate()
                    except Exception:
                        pass
            if req_id is not None:
                send_response(req_id, {})
        else:
            if req_id is not None:
                send_response(req_id, {})

if __name__ == "__main__":
    main()
