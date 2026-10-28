#!/usr/bin/env bash
# ==============================================================================
# CodeStation Bot Environment Runner - Google Antigravity
# Handles explicit actions: server, cli, shell, run, stop, status
# Works both inside Ubuntu PRoot and from Termux without nested PRoot hangs.
# ==============================================================================

set -e

BOT_ID="bot-antigravity"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE="${CODESTATION_WORKSPACE:-/root/workspace}"
mkdir -p "$WORKSPACE" "$SCRIPT_DIR/workspace"

# Environment variables
export PATH="/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:/data/data/com.termux/files/usr/bin:$PATH"
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

# Locate agy binary
AGY_BIN=""
if [ -x "/root/.local/bin/agy" ]; then
    AGY_BIN="/root/.local/bin/agy"
elif [ -x "/root/.gemini/antigravity-cli/bin/agy" ]; then
    AGY_BIN="/root/.gemini/antigravity-cli/bin/agy"
elif command -v agy >/dev/null 2>&1; then
    AGY_BIN="$(command -v agy)"
fi

ACTION="${1:-cli}"
shift 2>/dev/null || true

cd "$WORKSPACE"

case "$ACTION" in
    server|acp)
        # Mode serveur / ACP utilisé par CodeStation Host
        exec python3 "$SCRIPT_DIR/agy_acp_bridge.py" "$@"
        ;;

    cli|interactive)
        # Session interactive Antigravity CLI dans le terminal Termux
        if [ -z "$AGY_BIN" ]; then
            echo "[ERREUR] Binaire agy introuvable." >&2
            exit 1
        fi
        echo -e "\n=== Lancement de la session interactive Google Antigravity ==="
        echo "Workspace : $WORKSPACE"
        exec "$AGY_BIN" "$@"
        ;;

    shell|bash)
        # Shell interactif bash dans l'environnement du bot
        echo -e "\n=== Shell d'environnement CodeStation ($BOT_ID) ==="
        echo "Workspace : $WORKSPACE"
        exec /bin/bash "$@"
        ;;

    run|query|ask)
        # Requête non interactive avec agy (supporte --model <id> et stdin sans limite de longueur)
        if [ -z "$AGY_BIN" ]; then
            echo "[ERREUR] Binaire agy introuvable." >&2
            exit 1
        fi
        MODEL_ARGS=()
        if [ "$1" = "--model" ] || [ "$1" = "-m" ]; then
            MODEL_ARGS=("--model" "$2")
            shift 2 2>/dev/null || true
        fi
        PROMPT="$*"
        if [ -z "$PROMPT" ]; then
            exec "$AGY_BIN" -p - "${MODEL_ARGS[@]}" --dangerously-skip-permissions
        else
            printf "%s" "$PROMPT" | exec "$AGY_BIN" -p - "${MODEL_ARGS[@]}" --dangerously-skip-permissions
        fi
        ;;

    stop)
        # Arrêt des processus orphelins liés à agy
        echo "Arrêt des processus $BOT_ID..."
        pkill -TERM -f "agy_acp_bridge.py" 2>/dev/null || true
        pkill -TERM -f "bin/agy" 2>/dev/null || true
        echo "OK"
        exit 0
        ;;

    status)
        echo "=== État du Bot $BOT_ID ==="
        echo "Binaire agy : ${AGY_BIN:-Non trouvé}"
        if [ -n "$AGY_BIN" ]; then
            echo "Version : $("$AGY_BIN" --version 2>/dev/null || echo 'inconnue')"
        fi
        echo "Workspace : $WORKSPACE"
        echo "ACP Bridge : $SCRIPT_DIR/agy_acp_bridge.py"
        ;;

    models)
        # Liste les modèles officiellement pris en charge par le binaire agy
        if [ -z "$AGY_BIN" ]; then
            echo "[ERREUR] Binaire agy introuvable." >&2
            exit 1
        fi
        "$AGY_BIN" models
        ;;

    *)
        # Si le premier argument est un binaire ou une commande explicite (ex: host l'invoque directement)
        if command -v "$ACTION" >/dev/null 2>&1 || [ -x "$ACTION" ]; then
            exec "$ACTION" "$@"
        else
            echo "Usage: $0 {server|acp|cli|shell|run <prompt>|stop|status} [args...]" >&2
            exit 1
        fi
        ;;
esac
