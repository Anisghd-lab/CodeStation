#!/usr/bin/env bash
# ==============================================================================
# CodeStation 2.0 - Unified Lifecycle & Multi-Engine Manager
# GHD Interactive Studio (com.ghdinteractivestudio.codestation)
#
# Manages:
# 1. CodeStation Host (Rust core backend, port 19222)
# 2. CAMCS Engine (Center for Autonomous Multi-Agent Coordination, port 19223)
# 3. Continuous Memory Engine (Autonomous 6-Agent Learning Pipeline, port 19224)
# 4. Background Auto-Recovery Supervisor (Watchdog with exponential backoff)
# 5. Dedicated Bot Environments & PRoot CLIs (agy, opencode, codebuddy, codex)
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Auto-détection de l'environnement : Si invoqué sous Termux hors Ubuntu PRoot
if [ -d "/data/data/com.termux/files" ] && [ ! -f "/proc/sys/kernel/cap_last_cap" ] && [ -z "$PROOT_L2S_DIR" ]; then
    if command -v proot-distro >/dev/null 2>&1; then
        echo "[CodeStation] Environnement Termux détecté : basculement automatique dans Ubuntu PRoot..."
        exec proot-distro login ubuntu -- bash -c "cd /root/CodeStation && ./start_code_station.sh $*"
    fi
fi

export PATH="/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:/data/data/com.termux/files/usr/bin:$PATH"

# Configuration réseau
export CODYNC_ENV="${CODYNC_ENV:-main}"
BIND="${CODESTATION_BIND:-127.0.0.1}"
HOST_PORT="${CODESTATION_PORT:-19222}"
CAMCS_PORT="${CAMCS_PORT:-19223}"
MEMORY_PORT="${MEMORY_PORT:-19224}"

# Fichiers PID & Logs
HOST_PID_FILE="/tmp/codestation-host.pid"
HOST_LOG_FILE="/tmp/codestation-host.log"
CAMCS_PID_FILE="/tmp/codestation-camcs.pid"
CAMCS_LOG_FILE="/tmp/codestation-camcs.log"
MEMORY_PID_FILE="/tmp/codestation-memory.pid"
MEMORY_LOG_FILE="/tmp/codestation-memory.log"
SUPERVISOR_PID_FILE="/tmp/codestation-supervisor.pid"
SUPERVISOR_LOG_FILE="/tmp/codestation-supervisor.log"
SHUTDOWN_FLAG="/tmp/codestation-shutdown.flag"

# Répertoires de configuration
export CONFIG_DIR="${CODESTATION_HOME:-$HOME/.codestation}"
export CODYNC_HOME="$CONFIG_DIR"
export CODESTATION_HOME="$CONFIG_DIR"
export TOKEN_FILE="$CONFIG_DIR/token"
export MEMORY_REPO_PATH="/root/CodeStation-Memory"

mkdir -p "$CONFIG_DIR"

# Migration des configurations historiques
if [ -d "$HOME/.codync-dev" ] && [ ! -f "$TOKEN_FILE" ]; then
    cp -a "$HOME/.codync-dev"/* "$CONFIG_DIR"/ 2>/dev/null || true
elif [ -d "$HOME/.codync" ] && [ ! -f "$TOKEN_FILE" ]; then
    cp -a "$HOME/.codync"/* "$CONFIG_DIR"/ 2>/dev/null || true
fi

# Forcer la boucle locale pour l'application mobile Android sur le même smartphone
export CODYNC_EXTERNAL_URL="http://${BIND}:${HOST_PORT}"
export CODESTATION_EXTERNAL_URL="http://${BIND}:${HOST_PORT}"

# Détection du binaire hôte
detect_host_binary() {
    if command -v codestation >/dev/null 2>&1; then
        command -v codestation
    elif [ -x "$SCRIPT_DIR/host/target/debug/codestation" ]; then
        echo "$SCRIPT_DIR/host/target/debug/codestation"
    elif [ -x "/usr/local/bin/codestation" ]; then
        echo "/usr/local/bin/codestation"
    elif command -v codestation-host >/dev/null 2>&1; then
        command -v codestation-host
    elif command -v codync-host >/dev/null 2>&1; then
        command -v codync-host
    elif [ -x "/usr/local/bin/codestation-host" ]; then
        echo "/usr/local/bin/codestation-host"
    elif [ -x "/usr/local/bin/codync-host" ]; then
        echo "/usr/local/bin/codync-host"
    elif [ -x "$HOME/.local/bin/codync-host" ]; then
        echo "$HOME/.local/bin/codync-host"
    else
        echo ""
    fi
}

HOST_BIN="$(detect_host_binary)"

check_port_listening() {
    local target_port="$1"
    python3 -c "
import socket, sys
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.settimeout(0.5)
try:
    s.connect(('127.0.0.1', int('$target_port')))
    s.close()
    sys.exit(0)
except Exception:
    sys.exit(1)
" 2>/dev/null
}

is_pid_alive() {
    local pid="$1"
    [ -n "$pid" ] && [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null
}

read_pid() {
    local pid_file="$1"
    local pattern="$2"
    if [ -z "$pattern" ]; then
        case "$pid_file" in
            *"host"*) pattern="codestation.*serve" ;;
            *"camcs"*) pattern="camcs_engine.py" ;;
            *"memory"*) pattern="memory_continuous_watcher.py" ;;
            *"supervisor"*) pattern="_supervisor_daemon" ;;
        esac
    fi
    if [ -f "$pid_file" ]; then
        local p
        p="$(cat "$pid_file" 2>/dev/null || true)"
        if is_pid_alive "$p"; then
            echo "$p"
            return 0
        fi
    fi
    if [ -n "$pattern" ]; then
        local found_pid
        found_pid="$(pgrep -f "$pattern" 2>/dev/null | grep -v "$$" | head -n 1 || true)"
        if [ -n "$found_pid" ] && is_pid_alive "$found_pid"; then
            echo "$found_pid" > "$pid_file" 2>/dev/null || true
            echo "$found_pid"
            return 0
        fi
    fi
    echo ""
    return 1
}

# ==============================================================================
# GESTION DES SERVICES INDIVIDUELS
# ==============================================================================

start_host() {
    echo -n "[1/3] Démarrage de CodeStation Host (port $HOST_PORT)... "
    local pid
    pid="$(read_pid "$HOST_PID_FILE" "codestation.*serve" || true)"
    if [ -n "$pid" ] && check_port_listening "$HOST_PORT"; then
        echo "DÉJÀ ACTIF (PID: $pid)"
        return 0
    fi

    if check_port_listening "$HOST_PORT"; then
        echo "PORT OCCUPÉ. Nettoyage de l'ancien processus..."
        pkill -TERM -f "codestation.*serve" 2>/dev/null || true
        pkill -TERM -f "codync-host.*serve" 2>/dev/null || true
        sleep 1
    fi

    if [ -z "$HOST_BIN" ] || [ ! -x "$HOST_BIN" ]; then
        echo "ÉCHEC : Binaire hôte introuvable (/usr/local/bin/codestation)."
        return 1
    fi

    setsid "$HOST_BIN" serve --bind "$BIND" --port "$HOST_PORT" </dev/null >> "$HOST_LOG_FILE" 2>&1 &
    local new_pid=$!
    echo "$new_pid" > "$HOST_PID_FILE"
    disown "$new_pid" 2>/dev/null || true

    local ready=0
    for _ in $(seq 1 12); do
        if check_port_listening "$HOST_PORT"; then
            ready=1
            break
        fi
        sleep 0.5
    done

    if [ "$ready" -eq 1 ]; then
        echo "OK (PID: $new_pid)"
        return 0
    else
        echo "ÉCHEC (consultez $HOST_LOG_FILE)"
        return 1
    fi
}

stop_host() {
    echo -n "Arrêt de CodeStation Host (port $HOST_PORT)... "
    local stopped=0
    local pid
    pid="$(read_pid "$HOST_PID_FILE" || true)"
    if [ -n "$pid" ]; then
        kill -TERM "$pid" 2>/dev/null || true
        for _ in $(seq 1 10); do
            if ! is_pid_alive "$pid"; then
                stopped=1
                break
            fi
            sleep 0.2
        done
        if is_pid_alive "$pid"; then
            kill -9 "$pid" 2>/dev/null || true
            stopped=1
        fi
    fi
    rm -f "$HOST_PID_FILE"

    for p in $(pgrep -f "codestation.*serve" 2>/dev/null || true) $(pgrep -f "codync-host.*serve" 2>/dev/null || true); do
        kill -TERM "$p" 2>/dev/null || true
        stopped=1
    done

    if [ "$stopped" -eq 1 ]; then
        echo "ARRÊTÉ"
    else
        echo "INACTIF"
    fi
}

start_camcs() {
    echo -n "[2/3] Démarrage de CAMCS Engine (port $CAMCS_PORT)... "
    local pid
    pid="$(read_pid "$CAMCS_PID_FILE" || true)"
    if [ -n "$pid" ] && check_port_listening "$CAMCS_PORT"; then
        echo "DÉJÀ ACTIF (PID: $pid)"
        return 0
    fi

    if check_port_listening "$CAMCS_PORT"; then
        echo "PORT OCCUPÉ. Nettoyage de l'ancien processus CAMCS..."
        pkill -TERM -f "camcs_engine.py" 2>/dev/null || true
        sleep 1
    fi

    if [ ! -f "$SCRIPT_DIR/scripts/camcs_engine.py" ]; then
        echo "ÉCHEC : $SCRIPT_DIR/scripts/camcs_engine.py introuvable."
        return 1
    fi

    setsid python3 "$SCRIPT_DIR/scripts/camcs_engine.py" "$CAMCS_PORT" </dev/null >> "$CAMCS_LOG_FILE" 2>&1 &
    local new_pid=$!
    echo "$new_pid" > "$CAMCS_PID_FILE"
    disown "$new_pid" 2>/dev/null || true

    local ready=0
    for _ in $(seq 1 12); do
        if check_port_listening "$CAMCS_PORT"; then
            ready=1
            break
        fi
        sleep 0.5
    done

    if [ "$ready" -eq 1 ]; then
        echo "OK (PID: $new_pid)"
        return 0
    else
        echo "ÉCHEC (consultez $CAMCS_LOG_FILE)"
        return 1
    fi
}

stop_camcs() {
    echo -n "Arrêt de CAMCS Engine (port $CAMCS_PORT)... "
    local stopped=0
    local pid
    pid="$(read_pid "$CAMCS_PID_FILE" || true)"
    if [ -n "$pid" ]; then
        kill -TERM "$pid" 2>/dev/null || true
        for _ in $(seq 1 10); do
            if ! is_pid_alive "$pid"; then
                stopped=1
                break
            fi
            sleep 0.2
        done
        if is_pid_alive "$pid"; then
            kill -9 "$pid" 2>/dev/null || true
            stopped=1
        fi
    fi
    rm -f "$CAMCS_PID_FILE"

    for p in $(pgrep -f "camcs_engine.py" 2>/dev/null || true); do
        kill -TERM "$p" 2>/dev/null || true
        stopped=1
    done

    if [ "$stopped" -eq 1 ]; then
        echo "ARRÊTÉ"
    else
        echo "INACTIF"
    fi
}

start_memory() {
    echo -n "[3/3] Démarrage du Continuous Memory Engine (port $MEMORY_PORT)... "
    local pid
    pid="$(read_pid "$MEMORY_PID_FILE" || true)"
    if [ -n "$pid" ] && check_port_listening "$MEMORY_PORT"; then
        echo "DÉJÀ ACTIF (PID: $pid)"
        return 0
    fi

    if check_port_listening "$MEMORY_PORT"; then
        echo "PORT OCCUPÉ. Nettoyage de l'ancien processus Memory..."
        pkill -TERM -f "memory_continuous_watcher.py" 2>/dev/null || true
        sleep 1
    fi

    if [ ! -f "$SCRIPT_DIR/scripts/memory_continuous_watcher.py" ]; then
        echo "ÉCHEC : $SCRIPT_DIR/scripts/memory_continuous_watcher.py introuvable."
        return 1
    fi

    setsid python3 "$SCRIPT_DIR/scripts/memory_continuous_watcher.py" "$MEMORY_PORT" </dev/null >> "$MEMORY_LOG_FILE" 2>&1 &
    local new_pid=$!
    echo "$new_pid" > "$MEMORY_PID_FILE"
    disown "$new_pid" 2>/dev/null || true

    local ready=0
    for _ in $(seq 1 12); do
        if check_port_listening "$MEMORY_PORT"; then
            ready=1
            break
        fi
        sleep 0.5
    done

    if [ "$ready" -eq 1 ]; then
        echo "OK (PID: $new_pid)"
        return 0
    else
        echo "ÉCHEC (consultez $MEMORY_LOG_FILE)"
        return 1
    fi
}

stop_memory() {
    echo -n "Arrêt du Continuous Memory Engine (port $MEMORY_PORT)... "
    local stopped=0
    local pid
    pid="$(read_pid "$MEMORY_PID_FILE" || true)"
    if [ -n "$pid" ]; then
        kill -TERM "$pid" 2>/dev/null || true
        for _ in $(seq 1 10); do
            if ! is_pid_alive "$pid"; then
                stopped=1
                break
            fi
            sleep 0.2
        done
        if is_pid_alive "$pid"; then
            kill -9 "$pid" 2>/dev/null || true
            stopped=1
        fi
    fi
    rm -f "$MEMORY_PID_FILE"

    for p in $(pgrep -f "memory_continuous_watcher.py" 2>/dev/null || true); do
        kill -TERM "$p" 2>/dev/null || true
        stopped=1
    done

    if [ "$stopped" -eq 1 ]; then
        echo "ARRÊTÉ"
    else
        echo "INACTIF"
    fi
}

# ==============================================================================
# SUPERVISEUR & AUTO-RECOVERY (WATCHDOG)
# ==============================================================================

start_supervisor() {
    echo -n "[+] Activation du Superviseur d'Auto-Recovery... "
    local pid
    pid="$(read_pid "$SUPERVISOR_PID_FILE" "_supervisor_daemon" || true)"
    if [ -n "$pid" ]; then
        echo "DÉJÀ ACTIF (PID: $pid)"
        return 0
    fi

    rm -f "$SHUTDOWN_FLAG"
    setsid bash "$SCRIPT_DIR/start_code_station.sh" _supervisor_daemon </dev/null >> "$SUPERVISOR_LOG_FILE" 2>&1 &
    local new_pid=$!
    echo "$new_pid" > "$SUPERVISOR_PID_FILE"
    disown "$new_pid" 2>/dev/null || true
    echo "ACTIF (PID: $new_pid)"
}

stop_supervisor() {
    echo -n "Arrêt du Superviseur d'Auto-Recovery... "
    touch "$SHUTDOWN_FLAG"
    local stopped=0
    local pid
    pid="$(read_pid "$SUPERVISOR_PID_FILE" "_supervisor_daemon" || true)"
    if [ -n "$pid" ]; then
        kill -TERM "$pid" 2>/dev/null || true
        sleep 0.3
        if is_pid_alive "$pid"; then
            kill -9 "$pid" 2>/dev/null || true
        fi
        stopped=1
    fi
    rm -f "$SUPERVISOR_PID_FILE"

    for p in $(pgrep -f "_supervisor_daemon" 2>/dev/null || true); do
        kill -TERM "$p" 2>/dev/null || true
        stopped=1
    done

    if [ "$stopped" -eq 1 ]; then
        echo "ARRÊTÉ"
    else
        echo "INACTIF"
    fi
}

run_supervisor_loop() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Superviseur CodeStation démarré (PID: $$)"
    local host_fails=0
    local camcs_fails=0
    local memory_fails=0
    local max_fails=5

    while true; do
        if [ -f "$SHUTDOWN_FLAG" ]; then
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] Drapeau d'arrêt détecté. Sortie du superviseur."
            break
        fi

        # 1. Surveillance Host (19222)
        local h_pid
        h_pid="$(read_pid "$HOST_PID_FILE" || true)"
        if [ -z "$h_pid" ] || ! check_port_listening "$HOST_PORT"; then
            host_fails=$((host_fails + 1))
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] ALERTE: Host inactif (échec #$host_fails)."
            if [ "$host_fails" -le "$max_fails" ]; then
                local backoff=$((host_fails * 2))
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] Redémarrage de Host après ${backoff}s de pause..."
                sleep "$backoff"
                start_host || true
            else
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] Host: nombre maximal de tentatives ($max_fails) atteint. Intervention requise."
            fi
        else
            host_fails=0
        fi

        # 2. Surveillance CAMCS (19223)
        local c_pid
        c_pid="$(read_pid "$CAMCS_PID_FILE" || true)"
        if [ -z "$c_pid" ] || ! check_port_listening "$CAMCS_PORT"; then
            camcs_fails=$((camcs_fails + 1))
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] ALERTE: CAMCS inactif (échec #$camcs_fails)."
            if [ "$camcs_fails" -le "$max_fails" ]; then
                local backoff=$((camcs_fails * 2))
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] Redémarrage de CAMCS après ${backoff}s de pause..."
                sleep "$backoff"
                start_camcs || true
            else
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] CAMCS: nombre maximal de tentatives ($max_fails) atteint."
            fi
        else
            camcs_fails=0
        fi

        # 3. Surveillance Memory (19224)
        local m_pid
        m_pid="$(read_pid "$MEMORY_PID_FILE" || true)"
        if [ -z "$m_pid" ] || ! check_port_listening "$MEMORY_PORT"; then
            memory_fails=$((memory_fails + 1))
            echo "[$(date '+%Y-%m-%d %H:%M:%S')] ALERTE: Continuous Memory inactif (échec #$memory_fails)."
            if [ "$memory_fails" -le "$max_fails" ]; then
                local backoff=$((memory_fails * 2))
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] Redémarrage de Memory après ${backoff}s de pause..."
                sleep "$backoff"
                start_memory || true
            else
                echo "[$(date '+%Y-%m-%d %H:%M:%S')] Memory: nombre maximal de tentatives ($max_fails) atteint."
            fi
        else
            memory_fails=0
        fi

        sleep 10
    done
}

# ==============================================================================
# AUDIT & UTILITAIRES
# ==============================================================================

show_cli_status() {
    echo -e "\n=================================================================="
    echo "       STATUT DES CLI ET ENGINES OFFICIELS CODESTATION           "
    echo "=================================================================="
    python3 "$SCRIPT_DIR/scripts/bot_env_manager.py" clis --no-cache | python3 -c "
import sys, json

try:
    data = json.load(sys.stdin)
    clis = data.get('clis', [])
    for cli in clis:
        name = cli.get('name', 'Inconnu')
        binary = cli.get('binary', '')
        path = cli.get('path') or 'Non trouvé'
        ver = cli.get('version') or 'Inconnu'
        status = cli.get('status', 'NOT_INSTALLED')
        details = cli.get('details', '')
        
        status_color = '\033[0;32m' if status in ('AUTHENTICATED', 'READY') else ('\033[0;33m' if status in ('AVAILABLE', 'INSTALLED') else '\033[0;31m')
        print(f'• {name:20} : {status_color}{status}\033[0m ({ver})')
        print(f'  ├─ Exécutable : {path}')
        print(f'  └─ Détails    : {details}')
except Exception as e:
    print(f'Erreur de lecture: {e}')
"
    echo "=================================================================="
}

check_agents() {
    show_cli_status
}

print_status_report() {
    echo -e "\n=================================================================="
    echo "          RAPPORT DE STATUT CENTRAL CODESTATION                   "
    echo "=================================================================="

    local h_pid c_pid m_pid s_pid
    h_pid="$(read_pid "$HOST_PID_FILE" || true)"
    c_pid="$(read_pid "$CAMCS_PID_FILE" || true)"
    m_pid="$(read_pid "$MEMORY_PID_FILE" || true)"
    s_pid="$(read_pid "$SUPERVISOR_PID_FILE" || true)"

    # Service 1: Host
    echo -n "• CodeStation Host      (port $HOST_PORT) : "
    if check_port_listening "$HOST_PORT"; then
        local health
        health="$(curl -s -m 1 "http://$BIND:$HOST_PORT/health" 2>/dev/null || echo "")"
        echo -e "\033[0;32mOPÉRATIONNEL\033[0m (PID: ${h_pid:-externe})"
        echo "  └─ Socket: $BIND:$HOST_PORT | Health: $health"
        echo "  └─ Log: $HOST_LOG_FILE"
    else
        echo -e "\033[0;31mINACTIF\033[0m"
    fi

    # Service 2: CAMCS
    echo -n "• CAMCS Engine          (port $CAMCS_PORT) : "
    if check_port_listening "$CAMCS_PORT"; then
        local camcs_health
        camcs_health="$(curl -s -m 1 "http://$BIND:$CAMCS_PORT/health" 2>/dev/null || echo "")"
        echo -e "\033[0;32mOPÉRATIONNEL\033[0m (PID: ${c_pid:-externe})"
        echo "  └─ Socket: $BIND:$CAMCS_PORT | Health: $camcs_health"
        echo "  └─ Log: $CAMCS_LOG_FILE"
    else
        echo -e "\033[0;31mINACTIF\033[0m"
    fi

    # Service 3: Continuous Memory
    echo -n "• Continuous Memory     (port $MEMORY_PORT) : "
    if check_port_listening "$MEMORY_PORT"; then
        local mem_health
        mem_health="$(curl -s -m 1 "http://$BIND:$MEMORY_PORT/health" 2>/dev/null || echo "")"
        echo -e "\033[0;32mOPÉRATIONNEL\033[0m (PID: ${m_pid:-externe})"
        echo "  └─ Socket: $BIND:$MEMORY_PORT | Health: $mem_health"
        echo "  └─ Log: $MEMORY_LOG_FILE"
    else
        echo -e "\033[0;31mINACTIF\033[0m"
    fi

    # Service 4: Superviseur
    echo -n "• Watchdog Superviseur  : "
    if [ -n "$s_pid" ]; then
        echo -e "\033[0;32mACTIF\033[0m (PID: $s_pid, auto-recovery avec backoff progressif)"
        echo "  └─ Log: $SUPERVISOR_LOG_FILE"
    else
        echo -e "\033[0;33mINACTIF\033[0m"
    fi

    # État des Stockages & Données
    echo -e "\n[+] Persistance & Bases de Données :"
    if [ -f "$CONFIG_DIR/codestation.db" ]; then
        echo "  [✓] Base CodeStation Host   : $CONFIG_DIR/codestation.db ($(stat -c%s "$CONFIG_DIR/codestation.db" 2>/dev/null || echo 0) octets)"
    fi
    if [ -f "$CONFIG_DIR/camcs.db" ]; then
        local missions_cnt
        missions_cnt=$(sqlite3 "$CONFIG_DIR/camcs.db" "SELECT count(*) FROM missions;" 2>/dev/null || echo "0")
        echo "  [✓] Base CAMCS Engine       : $CONFIG_DIR/camcs.db ($missions_cnt missions enregistrées)"
    fi
    if [ -d "$MEMORY_REPO_PATH" ]; then
        local exp_cnt know_cnt
        exp_cnt=$(find "$MEMORY_REPO_PATH/experiences" -type f 2>/dev/null | wc -l)
        know_cnt=$(find "$MEMORY_REPO_PATH/knowledge" -type f 2>/dev/null | wc -l)
        echo "  [✓] Dépôt CodeStation-Memory: $MEMORY_REPO_PATH ($exp_cnt expériences, $know_cnt connaissances consolidées)"
    fi

    echo "=================================================================="
}

run_health_checks() {
    echo -e "\n=== Contrôle de Santé Complet de CodeStation ==="
    local all_ok=1

    # 1. Host Health
    echo -n "• Vérification Socket & HTTP Host (19222)... "
    local host_resp
    host_resp="$(curl -s -m 2 "http://$BIND:$HOST_PORT/health" 2>/dev/null || true)"
    if echo "$host_resp" | grep -q '"ok":true'; then
        echo -e "\033[0;32mVALIDE\033[0m"
    else
        echo -e "\033[0;31mÉCHEC\033[0m (Réponse: ${host_resp:-Inaccessible})"
        all_ok=0
    fi

    # 2. CAMCS Health
    echo -n "• Vérification Socket & HTTP CAMCS (19223)... "
    local camcs_resp
    camcs_resp="$(curl -s -m 2 "http://$BIND:$CAMCS_PORT/health" 2>/dev/null || true)"
    if echo "$camcs_resp" | grep -q '"ok": true'; then
        echo -e "\033[0;32mVALIDE\033[0m"
    else
        echo -e "\033[0;31mÉCHEC\033[0m (Réponse: ${camcs_resp:-Inaccessible})"
        all_ok=0
    fi

    # 3. Memory Health
    echo -n "• Vérification Socket & HTTP Memory (19224)... "
    local mem_resp
    mem_resp="$(curl -s -m 2 "http://$BIND:$MEMORY_PORT/health" 2>/dev/null || true)"
    if echo "$mem_resp" | grep -q '"ok": true'; then
        echo -e "\033[0;32mVALIDE\033[0m"
    else
        echo -e "\033[0;31mÉCHEC\033[0m (Réponse: ${mem_resp:-Inaccessible})"
        all_ok=0
    fi

    # 4. Superviseur
    echo -n "• Vérification Watchdog Superviseur... "
    local s_pid
    s_pid="$(read_pid "$SUPERVISOR_PID_FILE" || true)"
    if [ -n "$s_pid" ]; then
        echo -e "\033[0;32mVALIDE (PID: $s_pid)\033[0m"
    else
        echo -e "\033[0;33mATTENTION (Non démarré)\033[0m"
    fi

    # 5. CLI Tools
    echo -n "• Vérification Outils CLI PRoot (omnirush, clixad, agy, opencode, codebuddy, freebuff)... "
    local cli_found=0
    for cmd in omnirush clixad agy opencode codebuddy freebuff; do
        if command -v "$cmd" >/dev/null 2>&1; then
            cli_found=$((cli_found + 1))
        fi
    done
    echo -e "\033[0;32m$cli_found/6 CLI opérationnels détectés\033[0m"

    if [ "$all_ok" -eq 1 ]; then
        echo -e "\n\033[0;32m[✓] TOUS LES SERVICES CODESTATION SONT PARFAITEMENT SAINS ET FONCTIONNELS !\033[0m"
        return 0
    else
        echo -e "\n\033[0;31m[!] Certains services nécessitent une attention.\033[0m"
        return 1
    fi
}

show_logs() {
    local target="${1:-all}"
    local lines="${2:-30}"

    case "$target" in
        host)
            echo "=== Dernières lignes du Journal CodeStation Host ($HOST_LOG_FILE) ==="
            tail -n "$lines" "$HOST_LOG_FILE" 2>/dev/null || echo "Aucun log."
            ;;
        camcs)
            echo "=== Dernières lignes du Journal CAMCS Engine ($CAMCS_LOG_FILE) ==="
            tail -n "$lines" "$CAMCS_LOG_FILE" 2>/dev/null || echo "Aucun log."
            ;;
        memory)
            echo "=== Dernières lignes du Journal Continuous Memory ($MEMORY_LOG_FILE) ==="
            tail -n "$lines" "$MEMORY_LOG_FILE" 2>/dev/null || echo "Aucun log."
            ;;
        supervisor|watchdog)
            echo "=== Dernières lignes du Journal Superviseur ($SUPERVISOR_LOG_FILE) ==="
            tail -n "$lines" "$SUPERVISOR_LOG_FILE" 2>/dev/null || echo "Aucun log."
            ;;
        all|*)
            echo "=== Journal Superviseur ==="
            tail -n 10 "$SUPERVISOR_LOG_FILE" 2>/dev/null || true
            echo -e "\n=== Journal CodeStation Host ==="
            tail -n 10 "$HOST_LOG_FILE" 2>/dev/null || true
            echo -e "\n=== Journal CAMCS Engine ==="
            tail -n 10 "$CAMCS_LOG_FILE" 2>/dev/null || true
            echo -e "\n=== Journal Continuous Memory ==="
            tail -n 10 "$MEMORY_LOG_FILE" 2>/dev/null || true
            ;;
    esac
}

usage() {
    echo "Usage: $0 {start|stop|restart|status|health|logs|pair|token|info|check-agents|supervisor} [options]"
    echo ""
    echo "Commandes de cycle de vie :"
    echo "  start [all|host|camcs|memory]       : Démarrer tous les services dans l'ordre + superviseur"
    echo "  stop [all|host|camcs|memory]        : Arrêter proprement tous les services et le superviseur"
    echo "  restart [all|host|camcs|memory]     : Redémarrer tous les services ou un composant spécifique"
    echo "  status                              : Rapport d'état complet de tous les services et stockages"
    echo "  health                              : Contrôle de santé approfondi (sockets, HTTP, intégrité)"
    echo "  logs [all|host|camcs|memory|watchdog]: Consulter les journaux d'exécution"
    echo "  supervisor {start|stop|status}      : Gérer directement le processus superviseur"
    echo ""
    echo "Commandes CodeStation App & Utilitaires :"
    echo "  pair                                : Générer le lien d'appairage direct avec l'app Android"
    echo "  token                               : Afficher le jeton d'authentification (~/.codestation/token)"
    echo "  info                                : Afficher les identifiants et informations réseau de l'hôte"
    echo "  cli-status                          : État détaillé des 6 CLI installées (omnirush, clixad, agy, opencode, codebuddy, freebuff)"
    echo "  check-agents                        : Alias vers cli-status"
    exit 1
}

# ==============================================================================
# ROUTEUR PRINCIPAL
# ==============================================================================

CMD="${1:-start}"
shift || true

case "$CMD" in
    _supervisor_daemon)
        run_supervisor_loop
        ;;

    start)
        TARGET="${1:-all}"
        echo "=================================================================="
        echo "   DÉMARRAGE DU CYCLE DE VIE COMPLET CODESTATION (ARM64 PRoot)   "
        echo "=================================================================="

        case "$TARGET" in
            host)
                start_host
                ;;
            camcs)
                start_camcs
                ;;
            memory)
                start_memory
                ;;
            all|*)
                check_agents
                echo -e "\n[+] Séquence d'initialisation ordonnée des composants :"
                start_host
                start_camcs
                start_memory
                start_supervisor
                print_status_report
                ;;
        esac
        ;;

    stop)
        TARGET="${1:-all}"
        echo "=================================================================="
        echo "            ARRÊT DES SERVICES CODESTATION                        "
        echo "=================================================================="

        case "$TARGET" in
            host)
                stop_host
                ;;
            camcs)
                stop_camcs
                ;;
            memory)
                stop_memory
                ;;
            supervisor|watchdog)
                stop_supervisor
                ;;
            all|*)
                stop_supervisor
                stop_memory
                stop_camcs
                stop_host
                echo -e "\n[✓] Tous les services CodeStation ont été arrêtés proprement."
                ;;
        esac
        ;;

    restart)
        TARGET="${1:-all}"
        case "$TARGET" in
            bot)
                BOT_TO_RESTART="$2"
                echo -e "\n[+] Redémarrage ciblé du runner pour l'agent $BOT_TO_RESTART..."
                if [ -d "$SCRIPT_DIR/environments/$BOT_TO_RESTART" ] && [ -x "$SCRIPT_DIR/environments/$BOT_TO_RESTART/run.sh" ]; then
                    "$SCRIPT_DIR/environments/$BOT_TO_RESTART/run.sh" stop 2>/dev/null || true
                    echo "[✓] Runner $BOT_TO_RESTART réinitialisé."
                else
                    echo "[ERREUR] Environnement du bot $BOT_TO_RESTART introuvable."
                fi
                exit 0
                ;;
            host)
                stop_host
                sleep 1
                start_host
                ;;
            camcs)
                stop_camcs
                sleep 1
                start_camcs
                ;;
            memory)
                stop_memory
                sleep 1
                start_memory
                ;;
            supervisor|watchdog)
                stop_supervisor
                sleep 1
                start_supervisor
                ;;
            all|*)
                stop_supervisor
                stop_memory
                stop_camcs
                stop_host
                sleep 1
                start_host
                start_camcs
                start_memory
                start_supervisor
                print_status_report
                ;;
        esac
        ;;

    status)
        print_status_report
        ;;

    health)
        run_health_checks
        ;;

    logs)
        show_logs "$1" "$2"
        ;;

    supervisor)
        ACTION="${1:-status}"
        case "$ACTION" in
            start)
                start_supervisor
                ;;
            stop)
                stop_supervisor
                ;;
            status)
                s_pid="$(read_pid "$SUPERVISOR_PID_FILE" || true)"
                if [ -n "$s_pid" ]; then
                    echo "[✓] Superviseur ACTIF (PID: $s_pid)"
                else
                    echo "[-] Superviseur INACTIF"
                fi
                ;;
            *)
                echo "Usage: $0 supervisor {start|stop|status}"
                ;;
        esac
        ;;

    pair)
        if [ -z "$HOST_BIN" ] || [ ! -x "$HOST_BIN" ]; then
            echo "[ERREUR] Binaire hôte introuvable. Exécutez '$0 start' d'abord."
            exit 1
        fi

        if ! check_port_listening "$HOST_PORT"; then
            echo "[!] Le serveur ne répond pas sur $BIND:$HOST_PORT."
            echo "    Démarrage automatique du serveur..."
            start_host
            sleep 1
        fi

        echo -e "\n[+] Génération de l'appairage direct Termux / Android..."
        JSON_OUTPUT=$("$HOST_BIN" pair --port "$HOST_PORT" --json 2>/dev/null || true)

        if [ -z "$JSON_OUTPUT" ]; then
            echo "[ERREUR] Impossible de récupérer les paramètres d'appairage auprès du serveur sur le port $HOST_PORT."
            echo "Vérifiez que le serveur est bien démarré : $0 status"
            exit 1
        fi

        PAIR_RESULT=$(python3 -c "
import sys, json, urllib.parse

try:
    data = json.loads('''$JSON_OUTPUT''')
    raw_url = data.get('pairingUrl', '')
    if not raw_url:
        sys.exit(1)

    parsed = urllib.parse.urlparse(raw_url)
    qs = urllib.parse.parse_qs(parsed.query)

    assert 'code' in qs and 'id' in qs and 'sk' in qs and 'bk' in qs, 'Champs obligatoires manquants'

    urls = qs.get('urls', [''])[0].split(',') if 'urls' in qs else []
    local_url = 'http://127.0.0.1:$HOST_PORT'
    if local_url not in urls:
        urls.insert(0, local_url)
    qs['urls'] = [','.join(urls)]

    new_query = urllib.parse.urlencode(qs, doseq=True)
    codestation_url = urllib.parse.urlunparse(('codestation', 'pair', '', '', new_query, ''))
    codync_url = urllib.parse.urlunparse(('codync', 'pair', '', '', new_query, ''))

    print(json.dumps({
        'codestation': codestation_url,
        'codync': codync_url,
        'computerId': qs['id'][0],
        'expiresAt': data.get('expiresAt', 0)
    }))
except Exception as e:
    sys.exit(1)
")

        if [ -z "$PAIR_RESULT" ]; then
            echo "[ERREUR] Le format de réponse du serveur est invalide."
            exit 1
        fi

        PAIR_URL=$(python3 -c "import json; print(json.loads('''$PAIR_RESULT''')['codestation'])")
        PAIR_URL_CODYNC=$(python3 -c "import json; print(json.loads('''$PAIR_RESULT''')['codync'])")
        CID=$(python3 -c "import json; print(json.loads('''$PAIR_RESULT''')['computerId'])")

        echo -e "\n[✓] Appairage CodeStation Prêt (Computer ID: $CID)"
        echo "=================================================================="
        echo "Lien direct (CodeStation) :"
        echo "$PAIR_URL"
        echo "=================================================================="

        echo -e "\n[+] QR Code pour le viseur caméra de l'application mobile :"
        "$HOST_BIN" pair --port "$HOST_PORT" 2>/dev/null | grep -E "^[ ]*█" || true

        if command -v termux-clipboard-set >/dev/null 2>&1; then
            timeout 2 termux-clipboard-set "$PAIR_URL" 2>/dev/null || true
            echo -e "\n[✓] Lien copié dans le presse-papier Android !"
        fi

        if command -v termux-toast >/dev/null 2>&1; then
            timeout 2 termux-toast "CodeStation : Appairage 1-Clic prêt !" 2>/dev/null || true
        fi

        if command -v am >/dev/null 2>&1; then
            echo "[+] Ouverture directe de CodeStation sur votre téléphone..."
            timeout 3 am start -a android.intent.action.VIEW -d "$PAIR_URL" >/dev/null 2>&1 || true
        fi
        ;;

    token)
        if [ -f "$TOKEN_FILE" ]; then
            cat "$TOKEN_FILE"
        elif [ -f "$HOME/.codync-dev/token" ]; then
            cat "$HOME/.codync-dev/token"
        elif [ -f "$HOME/.codync/token" ]; then
            cat "$HOME/.codync/token"
        else
            echo "[CodeStation] Aucun jeton trouvé dans $CONFIG_DIR" >&2
            exit 1
        fi
        ;;

    info)
        if [ -z "$HOST_BIN" ] || [ ! -x "$HOST_BIN" ]; then
            echo "[ERREUR] Binaire introuvable."
            exit 1
        fi
        "$HOST_BIN" info --port "$HOST_PORT"
        ;;

    cli-status|check-agents|clis)
        show_cli_status
        ;;

    *)
        usage
        ;;
esac
