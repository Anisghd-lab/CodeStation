#!/usr/bin/env bash
# ==============================================================================
# CodeStation Bot Environment Runner - OmniRush
# ==============================================================================
set -e

BOT_ID="bot-omnirush"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE="${CODESTATION_WORKSPACE:-/root/CodeStation}"
mkdir -p "$WORKSPACE" "$SCRIPT_DIR/workspace"

export PATH="/usr/local/bin:/usr/bin:/bin:/root/.local/bin:$PATH"
export HOME="/root"

ACTION="${1:-cli}"
shift 2>/dev/null || true

cd "$WORKSPACE"

case "$ACTION" in
    server|acp)
        exec python3 "$SCRIPT_DIR/omnirush_acp_bridge.py" "$@"
        ;;
    cli|interactive)
        exec omnirush "$@"
        ;;
    *)
        exec python3 "$SCRIPT_DIR/omnirush_acp_bridge.py" "$@"
        ;;
esac
