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
import re

active_procs: dict[str, subprocess.Popen] = {}
active_procs_lock = threading.Lock()
session_conversations: dict[str, str] = {}
session_conversations_lock = threading.Lock()

SETTINGS_FILE_PATH = "/root/.gemini/antigravity-cli/settings.json"
AUTH_TOKEN_FILE = "/root/.gemini/antigravity-cli/antigravity-oauth-token"
LOG_FILE_PATH = "/tmp/codestation-agy-bridge.log"

# The 6 REAL officially available models in Google Antigravity CLI
DEFAULT_AGY_MODELS = [
    {"value": "gemini-3.8-flash-medium", "name": "Gemini 3.8 Flash (Medium)"},
    {"value": "gemini-3.8-flash-high", "name": "Gemini 3.8 Flash (High)"},
    {"value": "gemini-3.8-flash-low", "name": "Gemini 3.8 Flash (Low)"},
    {"value": "claude-sonnet-4-6", "name": "Claude Sonnet 4.6 (Thinking)"},
    {"value": "claude-opus-4-6-thinking", "name": "Claude Opus 4.6 (Thinking)"},
    {"value": "gpt-oss-120b-medium", "name": "GPT-OSS 120B (Medium)"},
]

CANONICAL_MODELS = {
    "gemini-3.8-flash-medium": {"id": "gemini-3.8-flash-medium", "name": "Gemini 3.8 Flash (Medium)", "effort": "medium"},
    "gemini-3.8-flash-high": {"id": "gemini-3.8-flash-high", "name": "Gemini 3.8 Flash (High)", "effort": "high"},
    "gemini-3.8-flash-low": {"id": "gemini-3.8-flash-low", "name": "Gemini 3.8 Flash (Low)", "effort": "low"},
    "claude-sonnet-4-6": {"id": "claude-sonnet-4-6", "name": "Claude Sonnet 4.6 (Thinking)", "effort": None},
    "claude-opus-4-6-thinking": {"id": "claude-opus-4-6-thinking", "name": "Claude Opus 4.6 (Thinking)", "effort": None},
    "gpt-oss-120b-medium": {"id": "gpt-oss-120b-medium", "name": "GPT-OSS 120B (Medium)", "effort": "medium"},
}

ALIASES = {
    "gemini-3.8-flash": "gemini-3.8-flash-medium",
    "gemini 3.8 flash": "gemini-3.8-flash-medium",
    "gemini-3.8-flash-medium": "gemini-3.8-flash-medium",
    "gemini 3.8 flash (medium)": "gemini-3.8-flash-medium",
    "gemini-3.8-flash-high": "gemini-3.8-flash-high",
    "gemini 3.8 flash (high)": "gemini-3.8-flash-high",
    "gemini-3.8-flash-low": "gemini-3.8-flash-low",
    "gemini 3.8 flash (low)": "gemini-3.8-flash-low",
    "claude-sonnet": "claude-sonnet-4-6",
    "claude sonnet": "claude-sonnet-4-6",
    "claude-sonnet-4.6": "claude-sonnet-4-6",
    "claude-sonnet-4-6": "claude-sonnet-4-6",
    "claude sonnet 4.6 (thinking)": "claude-sonnet-4-6",
    "claude-opus": "claude-opus-4-6-thinking",
    "claude opus": "claude-opus-4-6-thinking",
    "claude-opus-4-6": "claude-opus-4-6-thinking",
    "claude-opus-4.6": "claude-opus-4-6-thinking",
    "claude-opus-4-6-thinking": "claude-opus-4-6-thinking",
    "claude opus 4.6 (thinking)": "claude-opus-4-6-thinking",
    "gpt-oss": "gpt-oss-120b-medium",
    "gpt-oss-120b": "gpt-oss-120b-medium",
    "gpt oss 120b": "gpt-oss-120b-medium",
    "gpt-oss-120b-medium": "gpt-oss-120b-medium",
    "gpt-oss 120b (medium)": "gpt-oss-120b-medium",
    # Legacy fallbacks
    "gemini-4-argon-medium": "gemini-3.8-flash-medium",
    "gemini-4-argon-high": "gemini-3.8-flash-high",
    "gemini-4-argon-low": "gemini-3.8-flash-low",
    "gemini-4-argon": "gemini-3.8-flash-medium",
    "claude-opus-5-5-high": "claude-opus-4-6-thinking",
    "claude-opus-5-5-medium": "claude-opus-4-6-thinking",
    "claude-opus-5-5-low": "claude-opus-4-6-thinking",
    "claude-sonnet-5-5-high": "claude-sonnet-4-6",
    "claude-sonnet-5-5-medium": "claude-sonnet-4-6",
    "claude-sonnet-5-5-low": "claude-sonnet-4-6",
}

DEFAULT_MODEL_ID = "gemini-3.8-flash-medium"

def resolve_model(input_name, effort=None):
    raw = (input_name or "").strip().lower()
    clean = raw.replace("_", "-")
    target_id = ALIASES.get(clean) or clean

    if target_id == "gemini-3.8-flash" or clean.startswith("gemini-3.8-flash"):
        eff = (effort or "").lower()
        if eff in ("high", "xhigh", "max"):
            target_id = "gemini-3.8-flash-high"
        elif eff == "low":
            target_id = "gemini-3.8-flash-low"
        else:
            target_id = target_id if target_id in CANONICAL_MODELS else "gemini-3.8-flash-medium"

    if target_id not in CANONICAL_MODELS:
        for cid, meta in CANONICAL_MODELS.items():
            if raw == meta["name"].lower() or clean == meta["name"].lower().replace(" ", "-"):
                target_id = cid
                break

    if target_id not in CANONICAL_MODELS:
        log(f"Warning: Unknown model '{input_name}', defaulting to validated '{DEFAULT_MODEL_ID}'")
        target_id = DEFAULT_MODEL_ID

    return CANONICAL_MODELS[target_id]

def load_persisted_model():
    try:
        if os.path.exists(SETTINGS_FILE_PATH):
            with open(SETTINGS_FILE_PATH, "r", encoding="utf-8") as f:
                d = json.load(f)
                m = d.get("model")
                if m:
                    res = resolve_model(m)
                    return res["id"]
    except Exception:
        pass
    return DEFAULT_MODEL_ID

def persist_selected_model(model_id):
    try:
        data = {}
        if os.path.exists(SETTINGS_FILE_PATH):
            with open(SETTINGS_FILE_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)
        meta = resolve_model(model_id)
        data["model"] = meta["name"]
        with open(SETTINGS_FILE_PATH, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
        log(f"Persisted model '{meta['name']}' to {SETTINGS_FILE_PATH}")
    except Exception as e:
        log(f"Failed to persist model to settings.json: {e}")

def execute_slash_command(prompt_text, current_model=None, target_effort=None):
    """
    Executes or inspects slash commands (/signin, /model, /model <name>).
    Returns string response if it is a slash command, or None if it's a regular prompt.
    """
    global current_selected_model
    ptrim = (prompt_text or "").strip()
    plower = ptrim.lower()

    if plower in ["/signin", "/login", "signin", "login"]:
        user_email = ""
        has_auth = False
        if os.path.exists(AUTH_TOKEN_FILE) and os.path.getsize(AUTH_TOKEN_FILE) > 0:
            has_auth = True
            try:
                with open(AUTH_TOKEN_FILE, "r", encoding="utf-8") as f:
                    tok_data = json.load(f)
                    user_email = tok_data.get("id_token_email", "")
            except Exception:
                pass
        if has_auth:
            msg = "✓ Session Google Antigravity active et authentifiée."
            if user_email:
                msg += f"\nCompte associé : {user_email}."
            msg += "\nLa session OAuth est persistée dans ~/.gemini/antigravity-cli/antigravity-oauth-token et conservée après reconnexion."
            return msg
        else:
            return "Session Google Antigravity non authentifiée. Veuillez vous connecter via `agy` dans le terminal."

    if plower == "/model" or plower == "model":
        model_to_query = current_model or current_selected_model or DEFAULT_MODEL_ID
        meta = resolve_model(model_to_query, effort=target_effort)
        target_model = meta["id"]
        effort = target_effort or meta.get("effort")

        cmd = ["agy", "-p", "/model", "--dangerously-skip-permissions", "--model", target_model]
        if effort:
            cmd.extend(["--effort", effort])
        cmd.extend(["--output-format", "stream-json"])
        env = os.environ.copy()
        env["PATH"] = "/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:" + env.get("PATH", "")
        
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, env=env, timeout=15)
            active_info = None
            for l in res.stdout.splitlines():
                if l.startswith("{") and "command_result" in l:
                    try:
                        cj = json.loads(l)
                        active_info = cj.get("command", {}).get("data", {})
                    except Exception:
                        pass
            if active_info:
                act_label = active_info.get("label", target_model)
                act_id = active_info.get("id", target_model)
                act_eff = active_info.get("effort")
                msg = f"Modèle Google Antigravity actif : {act_label} (`{act_id}`)"
                if act_eff:
                    msg += f"\nNiveau d'effort de réflexion : {act_eff}"
                return msg
        except Exception as e:
            log(f"Error querying /model: {e}")
        return f"Modèle Google Antigravity actif : {meta['name']} (`{target_model}`)"

    if plower.startswith("/model "):
        requested_model = ptrim[7:].strip()
        new_meta = resolve_model(requested_model)
        current_selected_model = new_meta["id"]
        persist_selected_model(current_selected_model)
        return f"✓ Modèle changé pour : {new_meta['name']} (`{current_selected_model}`)."

    return None

current_selected_model = load_persisted_model()

_models_cache = DEFAULT_AGY_MODELS
_models_cache_time = time.time()
CACHE_TTL_SECONDS = 600

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
        out = subprocess.check_output(["agy", "models"], text=True, timeout=25, env=env)
        models = []
        for line in out.splitlines():
            line = line.strip()
            if not line or "Fetching available models" in line:
                continue
            m = re.match(r"^([a-z0-9\.\-_]+)\s+([A-Za-z0-9\.\-\_\s\(\)]+)$", line)
            if m:
                m_id, m_name = m.group(1), m.group(2).strip()
                if m_id in CANONICAL_MODELS:
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
    global current_selected_model
    proc = None
    try:
        env = os.environ.copy()
        env["PATH"] = "/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:" + env.get("PATH", "")

        resolved_meta = resolve_model(model_id or current_selected_model)
        target_model = resolved_meta["id"]
        target_effort = resolved_meta.get("effort")

        # Handle Slash Commands (/signin, /model, /model <name>)
        slash_res = execute_slash_command(prompt_text, current_model=target_model, target_effort=target_effort)
        if slash_res is not None:
            send_notification("session/update", {
                "sessionId": session_id,
                "update": {
                    "sessionUpdate": "agent_message_chunk",
                    "content": {"type": "text", "text": slash_res}
                }
            })
            send_response(req_id, {"stopReason": "end_turn"})
            return

        # 4. Handle Quota query
        if prompt_text.strip().lower() in ["/quota", "/usage", "quota", "usage"]:
            cmd = ["agy", "-p", "/quota", "--dangerously-skip-permissions"]
            log(f"Executing quota query directly: {' '.join(cmd)}")
        else:
            cmd = ["agy", "-p", prompt_text, "--dangerously-skip-permissions", "--model", target_model]
            if target_effort:
                cmd.extend(["--effort", target_effort])
            cmd.extend(["--output-format", "stream-json"])

            with session_conversations_lock:
                existing_conv_id = session_conversations.get(session_id)
            if existing_conv_id:
                cmd.extend(["--conversation", existing_conv_id])
                log(f"Continuing existing conversation {existing_conv_id} for session {session_id}")
            log(f"Executing: {' '.join(cmd[:3])} ... --model {target_model} (cwd={cwd})")

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

        def read_stderr():
            if proc.stderr:
                for sline in proc.stderr:
                    stderr_chunks.append(sline)
                    log(f"[agy stderr] {sline.strip()}")

        stderr_thread = threading.Thread(target=read_stderr, daemon=True)
        stderr_thread.start()

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
                    "version": "1.5.1"
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
                resolved = resolve_model(value)
                current_selected_model = resolved["id"]
                persist_selected_model(current_selected_model)
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
                resolved = resolve_model(model_id)
                current_selected_model = resolved["id"]
                persist_selected_model(current_selected_model)
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
