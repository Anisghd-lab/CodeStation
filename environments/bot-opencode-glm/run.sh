#!/usr/bin/env bash
# ==============================================================================
# CodeStation Bot Environment Runner - OpenCode GLM
# Handles explicit actions: server, cli, shell, run, stop, status
# Works both inside Ubuntu PRoot and from Termux without nested PRoot hangs.
# ==============================================================================

set -e

BOT_ID="bot-opencode-glm"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE="${CODESTATION_WORKSPACE:-/root/workspace}"
mkdir -p "$WORKSPACE" "$SCRIPT_DIR/workspace"

# Environment variables
export PATH="/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:/data/data/com.termux/files/usr/bin:$PATH"
export HOME="/root"
export OPENCODE_CONFIG_DIR="/root/.config/opencode"

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

# Locate opencode binary
OPENCODE_BIN=""
if [ -x "/usr/local/bin/opencode" ]; then
    OPENCODE_BIN="/usr/local/bin/opencode"
elif [ -x "/root/.opencode/bin/opencode" ]; then
    OPENCODE_BIN="/root/.opencode/bin/opencode"
elif command -v opencode >/dev/null 2>&1; then
    OPENCODE_BIN="$(command -v opencode)"
fi

ACTION="${1:-cli}"
shift 2>/dev/null || true

cd "$WORKSPACE"

case "$ACTION" in
    server|acp)
        # Mode serveur / ACP utilisé par CodeStation Host
        if [ -z "$OPENCODE_BIN" ]; then
            echo "[ERREUR] Binaire opencode introuvable." >&2
            exit 1
        fi
        exec "$OPENCODE_BIN" acp "$@"
        ;;

    cli|interactive)
        # Session TUI / interactive OpenCode dans le terminal Termux
        if [ -z "$OPENCODE_BIN" ]; then
            echo "[ERREUR] Binaire opencode introuvable." >&2
            exit 1
        fi
        echo -e "\n=== Lancement de la session interactive OpenCode GLM ==="
        echo "Workspace : $WORKSPACE"
        exec "$OPENCODE_BIN" -m "opencode/glm-5.3-flash-free" "$@"
        ;;

    shell|bash)
        # Shell interactif bash dans l'environnement du bot
        echo -e "\n=== Shell d'environnement CodeStation ($BOT_ID) ==="
        echo "Workspace : $WORKSPACE"
        exec /bin/bash "$@"
        ;;

    run|query|ask)
        # Requête non interactive avec le modèle GLM-5.3-Flash Free (supporte stdin sans limite de longueur)
        if [ -z "$OPENCODE_BIN" ]; then
            echo "[ERREUR] Binaire opencode introuvable." >&2
            exit 1
        fi
        PROMPT="$*"
        if [ -z "$PROMPT" ]; then
            exec "$OPENCODE_BIN" run -m "opencode/glm-5.3-flash-free" --format default
        else
            printf "%s" "$PROMPT" | exec "$OPENCODE_BIN" run -m "opencode/glm-5.3-flash-free" --format default
        fi
        ;;

    stop)
        # Arrêt des processus orphelins liés à opencode
        echo "Arrêt des processus $BOT_ID..."
        pkill -TERM -f "bin/opencode.*serve" 2>/dev/null || true
        pkill -TERM -f "bin/opencode.*acp" 2>/dev/null || true
        echo "OK"
        exit 0
        ;;

    status)
        echo "=== État du Bot $BOT_ID ==="
        echo "Binaire opencode : ${OPENCODE_BIN:-Non trouvé}"
        if [ -n "$OPENCODE_BIN" ]; then
            echo "Version : $("$OPENCODE_BIN" --version 2>/dev/null || echo 'inconnue')"
        fi
        echo "Workspace : $WORKSPACE"
        echo "Modèle cible : opencode/glm-5.3-flash-free"
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
