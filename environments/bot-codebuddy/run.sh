#!/usr/bin/env bash
# ==============================================================================
# CodeStation Bot Environment Runner - CodeBuddy Code
# Handles explicit actions: server, cli, shell, run, stop, status, models
# Works both inside Ubuntu PRoot and from Termux without nested PRoot hangs.
# ==============================================================================

set -e

BOT_ID="bot-codebuddy"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE="${CODESTATION_WORKSPACE:-/root/CodeStation}"
mkdir -p "$WORKSPACE" "$SCRIPT_DIR/workspace"

export PATH="/usr/local/bin:/usr/bin:/bin:/root/.local/bin:$PATH"
export HOME="/root"

# Detect if already executing inside PRoot
if [ -n "$PROOT_L2S_DIR" ] || uname -r | grep -qi "proot" || [ -f "/proc/sys/kernel/cap_last_cap" ]; then
    IN_PROOT=1
else
    IN_PROOT=0
fi

# If called from Termux (outside Ubuntu PRoot), enter PRoot first
if [ "$IN_PROOT" -eq 0 ]; then
    if command -v proot-distro >/dev/null 2>&1; then
        exec proot-distro login ubuntu -- bash "$SCRIPT_DIR/run.sh" "$@"
    else
        echo "[ERREUR] proot-distro introuvable sous Termux." >&2
        exit 1
    fi
fi

CB_BIN=""
if [ -x "/usr/local/bin/codebuddy" ]; then
    CB_BIN="/usr/local/bin/codebuddy"
elif command -v codebuddy >/dev/null 2>&1; then
    CB_BIN="$(command -v codebuddy)"
elif command -v cbc >/dev/null 2>&1; then
    CB_BIN="$(command -v cbc)"
fi

ACTION="${1:-cli}"
shift 2>/dev/null || true

cd "$WORKSPACE"

case "$ACTION" in
    server|acp)
        if [ -z "$CB_BIN" ]; then
            echo "[ERREUR] Binaire codebuddy introuvable." >&2
            exit 1
        fi
        exec "$CB_BIN" --acp "$@"
        ;;

    cli|interactive)
        if [ -z "$CB_BIN" ]; then
            echo "[ERREUR] Binaire codebuddy introuvable." >&2
            exit 1
        fi
        echo -e "\n=== Lancement de la session interactive CodeBuddy Code ==="
        echo "Workspace : $WORKSPACE"
        exec "$CB_BIN" "$@"
        ;;

    shell|bash)
        echo -e "\n=== Shell d'environnement CodeStation ($BOT_ID) ==="
        echo "Workspace : $WORKSPACE"
        exec /bin/bash "$@"
        ;;

    run|query|ask)
        if [ -z "$CB_BIN" ]; then
            echo "[ERREUR] Binaire codebuddy introuvable." >&2
            exit 1
        fi
        PROMPT="$*"
        if [ -z "$PROMPT" ]; then
            exec "$CB_BIN" -p -y
        else
            exec "$CB_BIN" -p -y "$PROMPT"
        fi
        ;;

    stop)
        echo "Arrêt des processus $BOT_ID..."
        pkill -TERM -f "codebuddy.*--acp" 2>/dev/null || true
        echo "OK"
        exit 0
        ;;

    status)
        echo "=== État du Bot $BOT_ID ==="
        echo "Binaire codebuddy : ${CB_BIN:-Non trouvé}"
        if [ -n "$CB_BIN" ]; then
            echo "Version : $("$CB_BIN" --version 2>/dev/null || echo 'inconnue')"
        fi
        echo "Workspace : $WORKSPACE"
        ;;

    models)
        if [ -z "$CB_BIN" ]; then
            echo "[ERREUR] Binaire codebuddy introuvable." >&2
            exit 1
        fi
        echo "Modèles supportés par CodeBuddy Code : hy3, hy4-preview, deepseek-v4.1-flash, fast-model, balanced-model, primary-model"
        ;;

    *)
        if command -v "$ACTION" >/dev/null 2>&1 || [ -x "$ACTION" ]; then
            exec "$ACTION" "$@"
        else
            echo "Usage: $0 {server|acp|cli|shell|run <prompt>|stop|status|models} [args...]" >&2
            exit 1
        fi
        ;;
esac
