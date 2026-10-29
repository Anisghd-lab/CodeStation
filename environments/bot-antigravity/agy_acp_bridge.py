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
current_selected_model = "gemini-3.8-flash-medium"

_models_cache = None
_models_cache_time = 0
CACHE_TTL_SECONDS = 300  # 5 minutes cache

LOG_FILE_PATH = "/tmp/codestation-agy-bridge.log"

def log(msg):
    ts = time.strftime("%Y-%m-%d %H:%M:%S")
    formatted = f"[{ts}] [agy_acp_bridge] {msg}\n"
    sys.stderr.write(formatted)
    sys.stderr.flush()
    try:
        with open(LOG_FILE_PATH, "a", encoding="utf-8") as f:
            f.write(formatted)
    except Exception:
        pass

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

    # Fallback to verified models for agy CLI 1.4.10 if CLI query fails
    fallback = [
        {"value": "gemini-3.8-flash-high", "name": "Gemini 3.8 Flash (High)"},
        {"value": "gemini-3.8-flash-medium", "name": "Gemini 3.8 Flash (Medium)"},
        {"value": "gemini-3.8-flash-low", "name": "Gemini 3.8 Flash (Low)"},
        {"value": "claude-sonnet-4-6", "name": "Claude Sonnet 4.6 (Thinking)"},
        {"value": "claude-opus-4-6-thinking", "name": "Claude Opus 4.6 (Thinking)"},
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
        
        # Validate model against available models
        avail_models = [m["value"] for m in fetch_agy_models()]
        if not model_id or model_id not in avail_models:
            model_id = "gemini-3.8-flash-medium"
            log(f"Falling back to default model: {model_id}")

        cmd = ["agy", "-p", prompt_text, "--dangerously-skip-permissions", "--model", model_id]
        log(f"Executing: {' '.join(cmd)} (cwd={cwd})")

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
        stderr_chunks = []

        # Stream stdout chunks
        def read_stderr():
            for sline in active_proc.stderr:
                stderr_chunks.append(sline)
                log(f"[agy stderr] {sline.strip()}")

        stderr_thread = threading.Thread(target=read_stderr, daemon=True)
        stderr_thread.start()

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
        stderr_thread.join(timeout=2.0)
        
        exit_code = active_proc.returncode
        with active_proc_lock:
            active_proc = None

        full_stdout = "".join(output_chunks).strip()
        full_stderr = "".join(stderr_chunks).strip()

        if exit_code != 0 and not full_stdout:
            err_msg = full_stderr or f"agy exited with code {exit_code}"
            log(f"agy execution failed: {err_msg}")
            send_response(req_id, error={"code": -32603, "message": err_msg})
            return
            
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
