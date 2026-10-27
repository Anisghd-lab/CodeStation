# Détection et Registre des Agents — CodeStation (Ubuntu PRoot / Termux)

**Date d'audit :** 27 Octobre 2026  
**Environnement d'exécution :** Android 16 (OP5E03L1) / Termux / Ubuntu PRoot aarch64  
**Utilisateur d'exécution :** `root`  
**PATH inspecté :** `/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:/data/data/com.termux/files/usr/bin`  

---

## 1. Tableau Synthétique des Agents

| Identifiant | Nom d'affichage | Installé | Chemin d'exécutable | Version détectée | Méthode d'intégration | État Auth | Disponibilité |
|---|---|---|---|---|---|---|---|
| `antigravity` | **Google Antigravity** | **OUI** | `/root/.local/bin/agy` | `1.4.8` | CLI Print (`--print`) / ACP stdio | **Authentifié** | **OPÉRATIONNEL** |
| `opencode` | **OpenCode** | **OUI** | `/usr/local/bin/opencode` | `v2.2.0` | ACP stdio (`opencode acp`) | Non authentifié (Crédits requis) | **DISPONIBLE (Auth requise)** |
| `codex` | **Codex CLI** | **OUI** | `/data/data/com.termux/files/usr/bin/codex` | `0.153.3` | App Server (`app-server --stdio`) / `exec` | Non authentifié (Login requis) | **DISPONIBLE (Auth requise)** |
| `claude` | **Claude Code** | NON | `None` | `None` | ACP Wrapper (`claude-acp`) | Absent | **INDISPONIBLE** |
| `gemini` | **Gemini CLI** | NON | `None` | `None` | ACP (`gemini --acp`) | Absent | **INDISPONIBLE** |

---

## 2. Analyse Détaillée par Agent

### 1. Google Antigravity (`agy`)
- **Chemin binaire :** `/root/.local/bin/agy` (symlink / wrapper vers `/root/.gemini/antigravity-cli/bin/agy`).
- **Version :** `1.4.8`.
- **Méthode d'intégration :**
  - Mode synchrone non-interactif : `agy --print "<prompt>" --output-format text`.
  - Mode streaming NDJSON : `agy --print "<prompt>" --output-format stream-json --input-format stream-json`.
  - Intégration subagent / ACP.
- **État d'authentification :** Authentifié via le compte actif Google de la session.
- **Précautions de sécurité :** Exécuté sans élévation de privilèges non sollicitée, respectant le bac à sable local.

### 2. OpenCode (`opencode`)
- **Chemin binaire :** `/usr/local/bin/opencode`.
- **Version :** `opencode v2.2.0` (Bun standalone runtime).
- **Méthode d'intégration :**
  - Serveur ACP natif standardisé : `opencode acp` (écoute sur stdin/stdout en protocole JSON-RPC 2.0).
  - Mode direct : `opencode run "<message>"`.
- **État d'authentification :** Non configuré. Erreur constatée lors des tests de tâche : `Insufficient credits. This account never purchased credits.`.
- **Résolution :** L'utilisateur doit exécuter `opencode auth login` ou configurer un provider local/custom dans `~/.opencode/config.json`.

### 3. OpenAI Codex CLI (`codex`)
- **Chemin binaire :** `/data/data/com.termux/files/usr/bin/codex` (installé sous l'environnement Termux aarch64).
- **Version :** `codex-cli 0.153.3`.
- **Méthode d'intégration :**
  - Serveur d'application : `codex app-server --stdio`.
  - Exécution non-interactive : `codex exec "<prompt>"`.
- **État d'authentification :** `~/.codex/auth.json` absent ou non renseigné (`no Codex credentials were found`).
- **Résolution :** L'utilisateur doit exécuter `codex login` ou exporter `OPENAI_API_KEY`.

---

## 3. Contrat de Découverte Dynamique

Le script de découverte `/root/CodeStation/scripts/agent_discovery.py` est exécutable et fournit un export JSON structuré respectant le format :
```json
{
  "id": "antigravity",
  "name": "Google Antigravity",
  "installed": true,
  "available": true,
  "path": "/root/.local/bin/agy",
  "version": "1.4.8",
  "integrationMethod": "CLI Print (--print) / ACP",
  "authenticated": true,
  "unavailabilityReason": null,
  "setupHint": "Install Antigravity CLI and sign in with Google."
}
```
Ce contrat est consommé par les tests automatisés et expose à l'application l'état réel du système sans déclarations fictives.
