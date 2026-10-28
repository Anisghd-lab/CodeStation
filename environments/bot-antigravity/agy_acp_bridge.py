#!/usr/bin/env python3
"""
Lightweight ACP (Agent Client Protocol) Bridge for Google Antigravity (agy).
Enables CodeStation Host to communicate with agy over standard JSON-RPC 2.0 stdio,
including dynamic model discovery via `agy models`.
"""

import sys
import json
import subprocess
import threading
import os
import time

active_proc = None
active_proc_lock = threading.Lock()
current_selected_model = "gemini-4-argon-medium"

_models_cache = None
_models_cache_time = 0
CACHE_TTL_SECONDS = 300  # 5 minutes cache

def log(msg):
    sys.stderr.write(f"[agy_acp_bridge] {msg}\n")
    sys.stderr.flush()

def fetch_agy_models(force_refresh=False):
    global _models_cache, _models_cache_time
    now = time.time()
    if not force_refresh and _models_cache and (now - _models_cache_time < CACHE_TTL_SECONDS):
        return _models_cache

    try:
        env = os.environ.copy()
        env["PATH"] = "/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:" + env.get("PATH", "")
        out = subprocess.check_output(["agy", "models"], text=True, timeout=12, env=env)
        models = []
        for line in out.splitlines():
            line = line.strip()
            if not line or "Fetching available models" in line or line.startswith("⠋") or line.startswith("⠙"):
                continue
            parts = line.split(None, 1)
            if len(parts) == 2:
                m_id = parts[0].strip()
                m_name = parts[1].strip()
                models.append({
                    "value": m_id,
                    "name": m_name
                })
        if models:
            _models_cache = models
            _models_cache_time = now
            return models
    except Exception as e:
        log(f"Failed to discover agy models dynamically: {e}")

    # Fallback to verified models for agy CLI 1.4.9 if CLI query fails
    fallback = [
        {"value": "gemini-4-argon-medium", "name": "Gemini 4 Argon (Medium)"},
        {"value": "gemini-4-argon-high", "name": "Gemini 4 Argon (High)"},
        {"value": "gemini-4-argon-low", "name": "Gemini 4 Argon (Low)"},
        {"value": "gemini-3.8-flash-high", "name": "Gemini 3.8 Flash (High)"},
        {"value": "gemini-3.8-flash-medium", "name": "Gemini 3.8 Flash (Medium)"},
        {"value": "gemini-3.8-flash-low", "name": "Gemini 3.8 Flash (Low)"},
        {"value": "claude-opus-5-5-low", "name": "Claude Opus 5.5 (Low)"},
        {"value": "claude-opus-5-5-medium", "name": "Claude Opus 5.5 (Medium)"},
        {"value": "claude-opus-5-5-high", "name": "Claude Opus 5.5 (High)"},
        {"value": "claude-sonnet-5-5-low", "name": "Claude Sonnet 5.5 (Low)"},
        {"value": "claude-sonnet-5-5-medium", "name": "Claude Sonnet 5.5 (Medium)"},
        {"value": "claude-sonnet-5-5-high", "name": "Claude Sonnet 5.5 (High)"},
        {"value": "gpt-oss-120b-medium", "name": "GPT-OSS 120B (Medium)"}
    ]
    return fallback

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

def run_prompt(req_id, session_id, prompt_text, cwd, model_id):
    global active_proc
    try:
        env = os.environ.copy()
        env["PATH"] = "/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:" + env.get("PATH", "")
        
        cmd = ["agy", "-p", prompt_text, "--dangerously-skip-permissions"]
        if model_id:
            cmd.extend(["--model", model_id])

        with active_proc_lock:
            active_proc = subprocess.Popen(
                cmd,
                cwd=cwd if os.path.isdir(cwd) else "/root/workspace",
                stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                env=env,
                bufsize=1
            )
        
        output_chunks = []
        for line in active_proc.stdout:
            output_chunks.append(line)
            # Use ACP standard snake_case "agent_message_chunk"
            send_notification("session/update", {
                "sessionId": session_id,
                "update": {
                    "sessionUpdate": "agent_message_chunk",
                    "content": {"type": "text", "text": line}
                }
            })
            
        active_proc.wait()
        
        with active_proc_lock:
            active_proc = None
            
        send_response(req_id, {
            "stopReason": "end_turn",
            "usage": {
                "inputTokens": max(len(prompt_text) // 4, 1),
                "outputTokens": max(sum(len(c) for c in output_chunks) // 4, 1),
                "totalTokens": max((len(prompt_text) + sum(len(c) for c in output_chunks)) // 4, 2)
            }
        })
    except Exception as e:
        log(f"Error executing agy prompt with model {model_id}: {e}")
        with active_proc_lock:
            active_proc = None
        send_response(req_id, error={"code": -32603, "message": str(e)})

def main():
    global current_selected_model
    session_cwd = "/root/workspace"
    session_id = "agy-session-1"

    for raw_line in sys.stdin:
        line = raw_line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
        except Exception as e:
            log(f"JSON parse error: {e}")
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
                    "name": "Google Antigravity",
                    "version": "1.4.3"
                }
            })
        elif method == "session/new":
            session_cwd = params.get("cwd", "/root/workspace")
            models = fetch_agy_models()
            send_response(req_id, {
                "sessionId": session_id,
                "configOptions": [
                    {
                        "id": "model",
                        "category": "model",
                        "type": "select",
                        "currentValue": current_selected_model,
                        "options": models
                    }
                ]
            })
        elif method == "session/set_config_option":
            config_id = params.get("configId")
            value = params.get("value")
            if config_id == "model" and value:
                current_selected_model = value
                log(f"Model switched to: {current_selected_model}")
            send_response(req_id, {
                "sessionId": session_id,
                "configOptions": [
                    {
                        "id": "model",
                        "category": "model",
                        "type": "select",
                        "currentValue": current_selected_model,
                        "options": fetch_agy_models()
                    }
                ]
            })
        elif method == "session/set_model":
            model_id = params.get("modelId")
            if model_id:
                current_selected_model = model_id
                log(f"Model set to: {current_selected_model}")
            send_response(req_id, {})
        elif method == "session/prompt":
            session_id = params.get("sessionId", session_id)
            prompt_list = params.get("prompt", [])
            prompt_text = ""
            for item in prompt_list:
                if isinstance(item, dict) and "text" in item:
                    prompt_text += item["text"] + " "
                elif isinstance(item, str):
                    prompt_text += item + " "
            prompt_text = prompt_text.strip()
            
            t = threading.Thread(
                target=run_prompt,
                args=(req_id, session_id, prompt_text, session_cwd, current_selected_model),
                daemon=True
            )
            t.start()
        elif method == "session/cancel":
            with active_proc_lock:
                if active_proc is not None:
                    try:
                        active_proc.terminate()
                    except Exception:
                        pass
            if req_id is not None:
                send_response(req_id, {})
        elif method == "session/close":
            send_response(req_id, {})
        else:
            if req_id is not None:
                send_response(req_id, {})

if __name__ == "__main__":
    main()
