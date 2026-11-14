#!/usr/bin/env bash
# ==============================================================================
# CodeStation Bot Environment Runner - Kimi Code CLI
# Handles explicit actions: server, acp, cli, shell, run, stop, status, doctor
# Works both inside Ubuntu PRoot and from Termux without nested PRoot hangs.
# ==============================================================================

set -e

BOT_ID="bot-kimi"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE="${CODESTATION_WORKSPACE:-/root/workspace}"
mkdir -p "$WORKSPACE" "$SCRIPT_DIR/workspace"

# Environment variables
export PATH="/root/.kimi-code/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:/data/data/com.termux/files/usr/bin:$PATH"
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

# Locate kimi binary
KIMI_BIN=""
if [ -x "/root/.kimi-code/bin/kimi" ]; then
    KIMI_BIN="/root/.kimi-code/bin/kimi"
elif [ -x "/usr/local/bin/kimi" ]; then
    KIMI_BIN="/usr/local/bin/kimi"
elif [ -x "/root/.local/bin/kimi" ]; then
    KIMI_BIN="/root/.local/bin/kimi"
elif command -v kimi >/dev/null 2>&1; then
    KIMI_BIN="$(command -v kimi)"
fi

ACTION="${1:-cli}"
shift 2>/dev/null || true

cd "$WORKSPACE"

case "$ACTION" in
    server|acp)
        # Mode serveur ACP stdio natif utilisé par CodeStation Host
        if [ -z "$KIMI_BIN" ]; then
            echo "[ERREUR] Binaire kimi introuvable." >&2
            exit 1
        fi
        exec "$KIMI_BIN" acp "$@"
        ;;

    cli|interactive)
        # Session interactive Kimi Code CLI dans le terminal
        if [ -z "$KIMI_BIN" ]; then
            echo "[ERREUR] Binaire kimi introuvable." >&2
            exit 1
        fi
        echo -e "\n=== Lancement de la session interactive Kimi Code CLI ==="
        echo "Workspace : $WORKSPACE"
        exec "$KIMI_BIN" "$@"
        ;;

    shell|bash)
        # Shell interactif bash dans l'environnement du bot
        echo -e "\n=== Shell d'environnement CodeStation ($BOT_ID) ==="
        echo "Workspace : $WORKSPACE"
        exec /bin/bash "$@"
        ;;

    run|query|ask)
        # Requête non interactive avec kimi (supporte --model <id> et prompt direct)
        if [ -z "$KIMI_BIN" ]; then
            echo "[ERREUR] Binaire kimi introuvable." >&2
            exit 1
        fi
        MODEL_ARGS=()
        if [ "$1" = "--model" ] || [ "$1" = "-m" ]; then
            MODEL_ARGS=("--model" "$2")
            shift 2 2>/dev/null || true
        fi
        PROMPT="$*"
        if [ -z "$PROMPT" ]; then
            exec "$KIMI_BIN" "${MODEL_ARGS[@]}"
        else
            exec "$KIMI_BIN" -p "$PROMPT" "${MODEL_ARGS[@]}"
        fi
        ;;

    stop)
        # Arrêt des processus orphelins liés à kimi
        echo "Arrêt des processus $BOT_ID..."
        pkill -TERM -f "/root/.kimi-code/bin/kimi" 2>/dev/null || true
        pkill -TERM -f "bin/kimi" 2>/dev/null || true
        echo "OK"
        exit 0
        ;;

    status)
        echo "=== État du Bot $BOT_ID ==="
        echo "Binaire kimi : ${KIMI_BIN:-Non trouvé}"
        if [ -n "$KIMI_BIN" ]; then
            echo "Version : $("$KIMI_BIN" --version 2>/dev/null || echo 'inconnue')"
        fi
        echo "Workspace : $WORKSPACE"
        echo "Configuration : /root/.kimi-code/config.toml"
        echo "Mode ACP : $KIMI_BIN acp (natif stdio)"
        ;;

    doctor)
        if [ -z "$KIMI_BIN" ]; then
            echo "[ERREUR] Binaire kimi introuvable." >&2
            exit 1
        fi
        exec "$KIMI_BIN" doctor "$@"
        ;;

    models|providers)
        if [ -z "$KIMI_BIN" ]; then
            echo "[ERREUR] Binaire kimi introuvable." >&2
            exit 1
        fi
        exec "$KIMI_BIN" provider "$@"
        ;;

    *)
        # Si le premier argument est un binaire ou une commande explicite
        if command -v "$ACTION" >/dev/null 2>&1 || [ -x "$ACTION" ]; then
            exec "$ACTION" "$@"
        else
            echo "Usage: $0 {server|acp|cli|shell|run <prompt>|stop|status|doctor} [args...]" >&2
            exit 1
        fi
        ;;
esac
