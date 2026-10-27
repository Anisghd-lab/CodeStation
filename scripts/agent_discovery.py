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
        elif candidate["id"] == "opencode":
            available = True
            auth_file = os.path.expanduser("~/.opencode/auth.json")
            if os.path.exists(auth_file):
                authenticated = True
            else:
                authenticated = False
                unavailability_reason = "Credits épuisés ou compte non authentifié (run 'opencode auth login')"
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
