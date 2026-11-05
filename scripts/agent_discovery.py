#!/usr/bin/env python3
import json
import os
import shutil
import subprocess
import sys

CANDIDATES = [
    {
        "id": "antigravity",
        "name": "Google Antigravity",
        "bins": ["agy"],
        "custom_paths": ["/root/.local/bin/agy", "/root/.gemini/antigravity-cli/bin/agy"],
        "integration": "CLI Print (--print) / ACP",
        "doc_hint": "Install Antigravity CLI and sign in with Google."
    },
    {
        "id": "opencode",
        "name": "OpenCode",
        "bins": ["opencode"],
        "custom_paths": ["/usr/local/bin/opencode", "/root/.opencode/bin/opencode"],
        "integration": "Agent Client Protocol (acp) stdio",
        "doc_hint": "Install OpenCode and run opencode auth login."
    },
    {
        "id": "codex",
        "name": "Codex CLI",
        "bins": ["codex"],
        "custom_paths": ["/data/data/com.termux/files/usr/bin/codex", "/usr/bin/codex"],
        "integration": "Codex app-server (--stdio) / exec",
        "doc_hint": "Install Codex CLI and run codex login."
    },
    {
        "id": "codebuddy",
        "name": "CodeBuddy Code",
        "bins": ["codebuddy", "cbc"],
        "custom_paths": ["/usr/local/bin/codebuddy", "/root/.local/bin/codebuddy"],
        "integration": "Agent Client Protocol (acp) stdio / Print (-p)",
        "doc_hint": "Install CodeBuddy Code and run codebuddy login."
    },
    {
        "id": "claude",
        "name": "Claude Code",
        "bins": ["claude"],
        "custom_paths": ["/usr/local/bin/claude", "/root/.local/bin/claude"],
        "integration": "Claude ACP wrapper",
        "doc_hint": "Install Claude Code (npm install -g @anthropic-ai/claude-code) and run claude auth login."
    },
    {
        "id": "gemini",
        "name": "Gemini CLI",
        "bins": ["gemini"],
        "custom_paths": ["/usr/local/bin/gemini", "/root/.npm-global/bin/gemini"],
        "integration": "Gemini ACP (--acp)",
        "doc_hint": "Install Gemini CLI (npm install -g @google/gemini-cli) and sign in."
    },
    {
        "id": "clixad",
        "name": "Clixad",
        "bins": ["clixad"],
        "custom_paths": ["/usr/bin/clixad", "/usr/local/bin/clixad"],
        "integration": "Terminal Free Coding (-p) / Ad-supported",
        "doc_hint": "Install Clixad (npm i -g clixad) and authenticate with clixad login."
    },
    {
        "id": "omnirush",
        "name": "OmniRush",
        "bins": ["omnirush"],
        "custom_paths": ["/usr/bin/omnirush", "/usr/local/bin/omnirush"],
        "integration": "OmniRush AI Engine / One-shot (-p)",
        "doc_hint": "Install OmniRush and run omnirush login."
    },
    {
        "id": "freebuff",
        "name": "Freebuff",
        "bins": ["freebuff"],
        "custom_paths": ["/usr/bin/freebuff", "/usr/local/bin/freebuff", "/root/.config/manicode/freebuff"],
        "integration": "Interactive TUI (Curses) / Tree-sitter AST",
        "doc_hint": "Install Freebuff and run freebuff login."
    }
]

def check_agent(candidate):
    path = None
    # 1. Search in PATH
    for b in candidate["bins"]:
        found = shutil.which(b)
        if found:
            path = found
            break
    # 2. Search in custom paths
    if not path:
        for cp in candidate.get("custom_paths", []):
            if os.path.exists(cp) and os.access(cp, os.X_OK):
                path = cp
                break

    installed = path is not None
    version = None
    authenticated = False
    available = False
    unavailability_reason = None

    if installed:
        # Determine version
        try:
            res = subprocess.run([path, "--version"], capture_output=True, text=True, timeout=5)
            output = (res.stdout + " " + res.stderr).strip().splitlines()
            if output:
                version = output[0].strip()
        except Exception as e:
            version = f"Error reading version: {e}"

        # Determine authentication & operational readiness without leaking secrets
        if candidate["id"] == "antigravity":
            available = True
            authenticated = True
        elif candidate["id"] == "codebuddy":
            available = True
            authenticated = True
        elif candidate["id"] == "opencode":
            available = True
            cfg_paths = [
                os.path.expanduser("~/.config/opencode/opencode.json"),
                os.path.expanduser("~/.config/opencode/cli.json"),
                os.path.expanduser("~/.opencode/auth.json")
            ]
            if any(os.path.exists(p) for p in cfg_paths):
                authenticated = True
            else:
                authenticated = False
                unavailability_reason = "Configuration introuvable (run 'opencode auth login')"
        elif candidate["id"] == "clixad":
            available = True
            clixad_cfg = os.path.expanduser("~/.clixad/config.json")
            if os.path.isfile(clixad_cfg) and os.path.getsize(clixad_cfg) > 10:
                authenticated = True
            else:
                authenticated = False
                unavailability_reason = "Identifiants Clixad manquants (run 'clixad login')"
        elif candidate["id"] == "omnirush":
            available = True
            omni_auth = os.path.expanduser("~/.omnirush/auth.json")
            omni_acc = os.path.expanduser("~/.omnirush/account-id.json")
            if (os.path.isfile(omni_auth) and os.path.getsize(omni_auth) > 10) or (os.path.isfile(omni_acc) and os.path.getsize(omni_acc) > 10):
                authenticated = True
            else:
                authenticated = False
                unavailability_reason = "Identifiants OmniRush manquants (run 'omnirush login')"
        elif candidate["id"] == "freebuff":
            freebuff_cred = os.path.expanduser("~/.config/manicode/credentials.json")
            if os.path.isfile(freebuff_cred) and os.path.getsize(freebuff_cred) > 0:
                authenticated = True
                available = False  # Limited to interactive TUI
                unavailability_reason = "Mode TUI interactif uniquement (en attente de mode non-interactif upstream)"
            else:
                authenticated = False
                available = False
                unavailability_reason = "Identifiants Freebuff manquants (~/.config/manicode/credentials.json)"
        elif candidate["id"] == "codex":
            codex_auth = os.path.expanduser("~/.codex/auth.json")
            if os.path.exists(codex_auth) and os.path.getsize(codex_auth) > 5:
                authenticated = True
                available = True
            else:
                authenticated = False
                available = True  # Can run exec/sandbox or doctor
                unavailability_reason = "Identifiants manquants (run 'codex login')"
    else:
        unavailability_reason = "Exécutable non trouvé dans le PATH ni dans les répertoires standards"

    return {
        "id": candidate["id"],
        "name": candidate["name"],
        "installed": installed,
        "available": available,
        "path": path,
        "version": version,
        "integrationMethod": candidate["integration"],
        "authenticated": authenticated,
        "unavailabilityReason": unavailability_reason,
        "setupHint": candidate["doc_hint"]
    }

def discover_all():
    results = []
    for c in CANDIDATES:
        results.append(check_agent(c))
    return results

if __name__ == "__main__":
    report = discover_all()
    print(json.dumps(report, indent=2))
