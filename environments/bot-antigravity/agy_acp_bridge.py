#!/usr/bin/env python3
"""
Lightweight ACP (Agent Client Protocol) Bridge for Google Antigravity (agy).
Enables CodeStation Host to communicate with agy over standard JSON-RPC 2.0 stdio,
including dynamic model discovery via `agy models`, multi-turn conversation tracking,
and per-session process lifecycle management to prevent timeouts and orphan processes.
"""

import sys
import json
import subprocess
import threading
import os
import time

active_procs: dict[str, subprocess.Popen] = {}
active_procs_lock = threading.Lock()
session_conversations: dict[str, str] = {}
session_conversations_lock = threading.Lock()

current_selected_model = "gemini-4-argon-medium"

DEFAULT_AGY_MODELS = [
    {"value": "gemini-4-argon-medium", "name": "Gemini 4 Argon (Medium)"},
    {"value": "gemini-4-argon-high", "name": "Gemini 4 Argon (High)"},
    {"value": "gemini-4-argon-low", "name": "Gemini 4 Argon (Low)"},
    {"value": "gemini-3.8-flash-high", "name": "Gemini 3.8 Flash (High)"},
    {"value": "gemini-3.8-flash-medium", "name": "Gemini 3.8 Flash (Medium)"},
    {"value": "gemini-3.8-flash-low", "name": "Gemini 3.8 Flash (Low)"},
    {"value": "claude-opus-5-5-high", "name": "Claude Opus 5.5 (High)"},
    {"value": "claude-opus-5-5-medium", "name": "Claude Opus 5.5 (Medium)"},
    {"value": "claude-opus-5-5-low", "name": "Claude Opus 5.5 (Low)"},
    {"value": "claude-sonnet-5-5-high", "name": "Claude Sonnet 5.5 (High)"},
    {"value": "claude-sonnet-5-5-medium", "name": "Claude Sonnet 5.5 (Medium)"},
    {"value": "claude-sonnet-5-5-low", "name": "Claude Sonnet 5.5 (Low)"},
    {"value": "gpt-oss-120b-medium", "name": "GPT-OSS 120B (Medium)"}
]

_models_cache = DEFAULT_AGY_MODELS
_models_cache_time = time.time()
CACHE_TTL_SECONDS = 600

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

def _background_refresh_models():
    global _models_cache, _models_cache_time
    try:
        env = os.environ.copy()
        env["PATH"] = "/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:" + env.get("PATH", "")
        out = subprocess.check_output(["agy", "models"], text=True, timeout=10, env=env)
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
            _models_cache_time = time.time()
            log(f"Dynamic model list refreshed ({len(models)} models)")
    except Exception as e:
        log(f"Background models refresh skipped: {e}")

def fetch_agy_models(force_refresh=False):
    global _models_cache, _models_cache_time
    now = time.time()
    if force_refresh or (now - _models_cache_time > CACHE_TTL_SECONDS):
        threading.Thread(target=_background_refresh_models, daemon=True).start()
    return _models_cache

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
    proc = None
    try:
        env = os.environ.copy()
        env["PATH"] = "/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:" + env.get("PATH", "")
        
        # Validate model against available models
        avail_models = [m["value"] for m in fetch_agy_models()]
        if not model_id or model_id not in avail_models:
            model_id = "gemini-4-argon-medium"
            log(f"Model {model_id} not in cache, using default: gemini-4-argon-medium")

        # Check existing conversation ID for multi-turn continuity
        with session_conversations_lock:
            existing_conv_id = session_conversations.get(session_id)

        if prompt_text.strip().lower() in ["/quota", "/usage", "quota", "usage"]:
            cmd = ["agy", "-p", "/quota", "--dangerously-skip-permissions"]
            log(f"Executing quota query directly: {' '.join(cmd)}")
        else:
            cmd = ["agy", "-p", prompt_text, "--dangerously-skip-permissions", "--model", model_id, "--output-format", "stream-json"]
            if existing_conv_id:
                cmd.extend(["--conversation", existing_conv_id])
                log(f"Continuing existing conversation {existing_conv_id} for session {session_id}")
            log(f"Executing: {' '.join(cmd[:3])} ... --model {model_id} --output-format stream-json (cwd={cwd})")

        proc = subprocess.Popen(
            cmd,
            cwd=cwd if os.path.isdir(cwd) else "/root/workspace",
            stdin=subprocess.DEVNULL,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env=env,
            bufsize=1
        )

        with active_procs_lock:
            active_procs[session_id] = proc
        
        output_chunks = []
        stderr_chunks = []
        last_stream_time = time.time()
        heartbeat_stop = threading.Event()

        # Stream stderr chunks
        def read_stderr():
            if proc.stderr:
                for sline in proc.stderr:
                    stderr_chunks.append(sline)
                    log(f"[agy stderr] {sline.strip()}")

        stderr_thread = threading.Thread(target=read_stderr, daemon=True)
        stderr_thread.start()

        # Heartbeat to keep connection alive during lengthy thinking or tool execution
        def heartbeat_worker():
            while not heartbeat_stop.wait(6.0):
                with active_procs_lock:
                    p = active_procs.get(session_id)
                    proc_alive = (p is not None and p.poll() is None)
                if proc_alive and (time.time() - last_stream_time >= 6.0):
                    send_notification("session/update", {
                        "sessionId": session_id,
                        "update": {
                            "sessionUpdate": "agent_thought_chunk",
                            "content": {"type": "text", "text": " "}
                        }
                    })

        heartbeat_thread = threading.Thread(target=heartbeat_worker, daemon=True)
        heartbeat_thread.start()

        for raw_line in proc.stdout:
            last_stream_time = time.time()
            line = raw_line.strip()
            if not line:
                continue

            parsed = None
            if line.startswith("{") and line.endswith("}"):
                try:
                    parsed = json.loads(line)
                except Exception:
                    parsed = None

            if parsed and isinstance(parsed, dict) and "event" in parsed:
                event = parsed.get("event")
                # Capture conversation_id for multi-turn continuity
                conv_id = parsed.get("conversation_id")
                if not conv_id and isinstance(parsed.get("result"), dict):
                    conv_id = parsed["result"].get("conversation_id")
                if not conv_id and isinstance(parsed.get("step_update"), dict):
                    conv_id = parsed["step_update"].get("conversation_id")
                if conv_id:
                    with session_conversations_lock:
                        session_conversations[session_id] = conv_id

                if event == "step_update":
                    step = parsed.get("step_update", {})
                    step_type = step.get("step_type")
                    text_delta = step.get("text_delta")
                    if text_delta:
                        output_chunks.append(text_delta)
                        send_notification("session/update", {
                            "sessionId": session_id,
                            "update": {
                                "sessionUpdate": "agent_message_chunk",
                                "content": {"type": "text", "text": text_delta}
                            }
                        })
                    elif step_type == "tool":
                        tool_name = step.get("tool_name", "tool")
                        state = step.get("state", "ACTIVE")
                        if state == "ACTIVE":
                            send_notification("session/update", {
                                "sessionId": session_id,
                                "update": {
                                    "sessionUpdate": "agent_thought_chunk",
                                    "content": {"type": "text", "text": f"[{tool_name}] "}
                                }
                            })
                    elif step_type in ("thought", "agent_response"):
                        send_notification("session/update", {
                            "sessionId": session_id,
                            "update": {
                                "sessionUpdate": "agent_thought_chunk",
                                "content": {"type": "text", "text": " "}
                            }
                        })
                elif event == "result":
                    res_obj = parsed.get("result", {})
                    resp_text = res_obj.get("response", "")
                    if resp_text and not output_chunks:
                        output_chunks.append(resp_text)
                        send_notification("session/update", {
                            "sessionId": session_id,
                            "update": {
                                "sessionUpdate": "agent_message_chunk",
                                "content": {"type": "text", "text": resp_text}
                            }
                        })
            else:
                # Plain text line
                output_chunks.append(raw_line)
                send_notification("session/update", {
                    "sessionId": session_id,
                    "update": {
                        "sessionUpdate": "agent_message_chunk",
                        "content": {"type": "text", "text": raw_line}
                    }
                })
            
        heartbeat_stop.set()
        heartbeat_thread.join(timeout=1.0)
        try:
            proc.wait(timeout=300.0)
        except subprocess.TimeoutExpired:
            log("agy execution timed out after 300s, killing process")
            with active_procs_lock:
                p = active_procs.pop(session_id, None)
                if p is not None:
                    try:
                        p.terminate()
                        time.sleep(1.0)
                        if p.poll() is None:
                            p.kill()
                    except Exception:
                        pass
            send_response(req_id, error={"code": -32000, "message": "Google Antigravity process timed out after 300s"})
            return

        stderr_thread.join(timeout=2.0)
        
        exit_code = proc.returncode if proc else -1
        with active_procs_lock:
            active_procs.pop(session_id, None)

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
        with active_procs_lock:
            active_procs.pop(session_id, None)
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
                    "version": "1.4.12"
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
                if isinstance(item, dict):
                    if item.get("type") == "text" and "text" in item:
                        prompt_text += item["text"] + " "
                    elif item.get("type") == "image":
                        mime = item.get("mimeType", "image/png")
                        prompt_text += f"\n[Attached image file ({mime})]\n"
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
            target_sid = params.get("sessionId", session_id)
            with active_procs_lock:
                p = active_procs.pop(target_sid, None)
                if p is not None:
                    try:
                        p.terminate()
                        time.sleep(0.5)
                        if p.poll() is None:
                            p.kill()
                    except Exception:
                        pass
            if req_id is not None:
                send_response(req_id, {})
        elif method == "session/close":
            target_sid = params.get("sessionId", session_id)
            with active_procs_lock:
                p = active_procs.pop(target_sid, None)
                if p is not None:
                    try:
                        p.terminate()
                    except Exception:
                        pass
            with session_conversations_lock:
                session_conversations.pop(target_sid, None)
            if req_id is not None:
                send_response(req_id, {})
        else:
            if req_id is not None:
                send_response(req_id, {})

if __name__ == "__main__":
    main()
