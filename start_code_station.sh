#!/usr/bin/env bash
# ==============================================================================
# CodeStation 1.0.1 - Termux & Ubuntu PRoot Host & Agent Hub Manager
# GHD Interactive Studio (com.ghdinteractivestudio.codestation)
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export PATH="/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:/data/data/com.termux/files/usr/bin:$PATH"

# Configuration réseau et environnement (personnalisable par variables d'environnement)
export CODYNC_ENV="${CODYNC_ENV:-main}"
PORT="${CODESTATION_PORT:-19222}"
BIND="${CODESTATION_BIND:-127.0.0.1}"
PID_FILE="/tmp/codestation-host.pid"
LOG_FILE="/tmp/codestation-host.log"

export CONFIG_DIR="${CODESTATION_HOME:-$HOME/.codestation}"
export CODYNC_HOME="$CONFIG_DIR"
export CODESTATION_HOME="$CONFIG_DIR"
export TOKEN_FILE="$CONFIG_DIR/token"

# Migrate token/secrets from legacy directory if not present
if [ ! -d "$CONFIG_DIR" ]; then
    mkdir -p "$CONFIG_DIR"
    if [ -d "$HOME/.codync-dev" ]; then
        cp -a "$HOME/.codync-dev"/* "$CONFIG_DIR"/ 2>/dev/null || true
    elif [ -d "$HOME/.codync" ]; then
        cp -a "$HOME/.codync"/* "$CONFIG_DIR"/ 2>/dev/null || true
    fi
fi

# Forcer la boucle locale pour l'application mobile Android sur le même smartphone
export CODYNC_EXTERNAL_URL="http://${BIND}:${PORT}"
export CODESTATION_EXTERNAL_URL="http://${BIND}:${PORT}"

# Détection explicite du binaire hôte
detect_host_binary() {
    if [ -x "$SCRIPT_DIR/host/target/release/codync-host" ]; then
        echo "$SCRIPT_DIR/host/target/release/codync-host"
    elif [ -x "$SCRIPT_DIR/host/target/debug/codync-host" ]; then
        echo "$SCRIPT_DIR/host/target/debug/codync-host"
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
s.settimeout(0.6)
try:
    s.connect(('127.0.0.1', int('$target_port')))
    s.close()
    sys.exit(0)
except Exception:
    sys.exit(1)
" 2>/dev/null
}

check_agents() {
    echo -e "\n[+] Vérification des Agents de Code installés (Termux / Ubuntu) :"
    if command -v agy >/dev/null 2>&1; then
        echo "  [✓] Google Antigravity (agy) : $(which agy) ($(agy --version 2>/dev/null | head -n 1 || echo 'ok'))"
    else
        echo "  [-] Google Antigravity (agy) : Non trouvé"
    fi

    if command -v codex >/dev/null 2>&1; then
        echo "  [✓] OpenAI Codex (codex)    : $(which codex) ($(codex --version 2>/dev/null | head -n 1 || echo 'ok'))"
    else
        echo "  [-] OpenAI Codex (codex)    : Non trouvé"
    fi

    if command -v opencode >/dev/null 2>&1; then
        echo "  [✓] OpenCode (opencode)     : $(which opencode) ($(opencode --version 2>/dev/null | head -n 1 || echo 'ok'))"
    else
        echo "  [-] OpenCode (opencode)     : Non trouvé"
    fi
}

is_running() {
    if check_port_listening "$PORT"; then
        return 0
    fi
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE" 2>/dev/null || true)
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            return 0
        fi
    fi
    if pgrep -f "codync-host.*serve" >/dev/null 2>&1 || pgrep -f "codestation-host.*serve" >/dev/null 2>&1; then
        return 0
    fi
    return 1
}

usage() {
    echo "Usage: $0 {start|stop|restart|pair|token|info|status|check-agents}"
    echo "  start        : Démarrer le serveur hôte CodeStation dans Termux/Ubuntu ($BIND:$PORT)"
    echo "  stop         : Arrêter proprement le serveur CodeStation"
    echo "  restart      : Redémarrer le serveur CodeStation"
    echo "  pair         : Appairer directement avec l'application CodeStation sur ce téléphone"
    echo "  token        : Afficher le jeton d'authentification local (~/.codestation/token)"
    echo "  info         : Afficher les informations d'identité et réseau local"
    echo "  status       : État du service CodeStation"
    echo "  check-agents : Liste des agents IA installés et opérationnels"
    exit 1
}

CMD="${1:-start}"

case "$CMD" in
    check-agents)
        check_agents
        ;;
    start)
        check_agents

        if [ -z "$HOST_BIN" ] || [ ! -x "$HOST_BIN" ]; then
            echo -e "\n[ERREUR] Aucun binaire backend exécutable trouvé."
            echo "Vérifiez que /usr/local/bin/codestation-host ou host/target/debug/codync-host existe."
            exit 1
        fi

        if check_port_listening "$PORT"; then
            echo -e "\n[!] CodeStation Host est DÉJÀ actif et répond sur $BIND:$PORT."
            echo "    Pour appairer votre téléphone : $0 pair"
            echo "    Pour redémarrer le serveur   : $0 restart"
            exit 0
        fi

        # Si un processus tourne sur un autre port, l'arrêter d'abord pour libérer ~/.codync/host.lock
        if pgrep -f "codync-host.*serve" >/dev/null 2>&1 || pgrep -f "codestation-host.*serve" >/dev/null 2>&1; then
            echo "[!] Un processus hôte résiduel a été détecté. Arrêt propre pour libérer le verrou..."
            pkill -TERM -f "codync-host.*serve" 2>/dev/null || true
            pkill -TERM -f "codestation-host.*serve" 2>/dev/null || true
            sleep 1
        fi

        echo -e "\n[+] Lancement du serveur CodeStation sur $BIND:$PORT en arrière-plan..."
        nohup "$HOST_BIN" serve --bind "$BIND" --port "$PORT" > "$LOG_FILE" 2>&1 &
        HOST_PID=$!
        echo "$HOST_PID" > "$PID_FILE"
        disown "$HOST_PID" 2>/dev/null || true

        # Attente active de disponibilité du port
        READY=0
        for _ in $(seq 1 10); do
            if check_port_listening "$PORT"; then
                READY=1
                break
            fi
            sleep 0.5
        done

        if [ "$READY" -eq 1 ]; then
            echo "[✓] CodeStation Host démarré avec succès (PID: $HOST_PID)."
            echo "    Écoute locale active sur $BIND:$PORT"
            echo "    Journal : $LOG_FILE"
        else
            echo "[ERREUR] Échec du démarrage du serveur sur $BIND:$PORT. Consultez $LOG_FILE :"
            tail -n 20 "$LOG_FILE" 2>/dev/null || true
            exit 1
        fi
        ;;

    stop)
        echo -e "\n[+] Arrêt de CodeStation Host..."
        STOPPED=0

        if [ -f "$PID_FILE" ]; then
            PID=$(cat "$PID_FILE" 2>/dev/null || true)
            if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
                kill -TERM "$PID" 2>/dev/null || true
                for _ in $(seq 1 10); do
                    if ! kill -0 "$PID" 2>/dev/null; then
                        STOPPED=1
                        break
                    fi
                    sleep 0.3
                done
                if kill -0 "$PID" 2>/dev/null; then
                    kill -9 "$PID" 2>/dev/null || true
                    STOPPED=1
                fi
            fi
            rm -f "$PID_FILE"
        fi

        for p in $(pgrep -f "codestation-host.*serve" 2>/dev/null || true) $(pgrep -f "codync-host.*serve" 2>/dev/null || true); do
            kill -TERM "$p" 2>/dev/null || true
            sleep 0.3
            if kill -0 "$p" 2>/dev/null; then
                kill -9 "$p" 2>/dev/null || true
            fi
            STOPPED=1
        done

        if [ "$STOPPED" -eq 1 ]; then
            echo "[✓] CodeStation Host arrêté proprement."
        else
            echo "[-] Aucun serveur CodeStation en cours d'exécution."
        fi
        ;;

    restart)
        $0 stop
        sleep 1
        $0 start
        ;;

    pair)
        if [ -z "$HOST_BIN" ] || [ ! -x "$HOST_BIN" ]; then
            echo "[ERREUR] Binaire hôte introuvable. Exécutez '$0 start' d'abord."
            exit 1
        fi

        # Si le serveur n'est pas encore actif, le démarrer automatiquement
        if ! check_port_listening "$PORT"; then
            echo "[!] Le serveur ne répond pas sur $BIND:$PORT."
            echo "    Démarrage automatique du serveur..."
            $0 start
            sleep 1
        fi

        echo -e "\n[+] Génération de l'appairage direct Termux / Android..."
        JSON_OUTPUT=$("$HOST_BIN" pair --port "$PORT" --json 2>/dev/null || true)

        if [ -z "$JSON_OUTPUT" ]; then
            echo "[ERREUR] Impossible de récupérer les paramètres d'appairage auprès du serveur sur le port $PORT."
            echo "Vérifiez que le serveur est bien démarré : $0 status"
            exit 1
        fi

        # Traitement sécurisé et garanti du lien d'appairage en Python
        PAIR_RESULT=$(python3 -c "
import sys, json, urllib.parse

try:
    data = json.loads('''$JSON_OUTPUT''')
    raw_url = data.get('pairingUrl', '')
    if not raw_url:
        sys.exit(1)

    parsed = urllib.parse.urlparse(raw_url)
    qs = urllib.parse.parse_qs(parsed.query)

    # Vérification des champs requis par Android
    assert 'code' in qs and 'id' in qs and 'sk' in qs and 'bk' in qs, 'Champs obligatoires manquants'

    # S'assurer que l'adresse de bouclage local 127.0.0.1 est présente en première position
    urls = qs.get('urls', [''])[0].split(',') if 'urls' in qs else []
    local_url = 'http://127.0.0.1:$PORT'
    if local_url not in urls:
        urls.insert(0, local_url)
    qs['urls'] = [','.join(urls)]

    # Schéma codestation://pair
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

        # Affichage du QR code ASCII dans le terminal pour scan par caméra si souhaité
        echo -e "\n[+] QR Code pour le viseur caméra de l'application mobile :"
        "$HOST_BIN" pair --port "$PORT" 2>/dev/null | grep -E "^[ ]*█" || true

        # Action 1 : Copie dans le presse-papier Android
        if command -v termux-clipboard-set >/dev/null 2>&1; then
            timeout 2 termux-clipboard-set "$PAIR_URL" 2>/dev/null || true
            echo -e "\n[✓] Lien copié dans le presse-papier Android !"
        fi

        # Action 2 : Notification Toast Android
        if command -v termux-toast >/dev/null 2>&1; then
            timeout 2 termux-toast "CodeStation : Appairage 1-Clic prêt !" 2>/dev/null || true
        fi

        # Action 3 : Lancement direct de l'application via Android Intent
        if command -v am >/dev/null 2>&1; then
            echo "[+] Ouverture directe de CodeStation sur votre téléphone..."
            timeout 3 am start -a android.intent.action.VIEW -d "$PAIR_URL" >/dev/null 2>&1 || true
        fi
        ;;

    info)
        if [ -z "$HOST_BIN" ] || [ ! -x "$HOST_BIN" ]; then
            echo "[ERREUR] Binaire introuvable."
            exit 1
        fi
        "$HOST_BIN" info --port "$PORT"
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

    status)
        check_agents
        if is_running; then
            PID=$(cat "$PID_FILE" 2>/dev/null || pgrep -f "codync-host.*serve" | head -n 1 || echo "actif")
            echo -e "\n[✓] CodeStation Host est en cours d'exécution sur $BIND:$PORT (PID: $PID)."
            if check_port_listening "$PORT"; then
                echo "    Statut socket : Écoute active et réactive sur $BIND:$PORT."
            fi
        else
            echo -e "\n[-] CodeStation Host n'est pas actif."
        fi
        ;;

    *)
        usage
        ;;
esac
