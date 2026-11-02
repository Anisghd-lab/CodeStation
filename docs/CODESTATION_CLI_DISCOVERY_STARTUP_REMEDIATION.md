# CODESTATION — RAPPORT TECHNIQUE DE DÉCOUVERTE CLI & GESTIONNAIRE DE DÉMARRAGE

**Date** : 1er Novembre 2026  
**Auteur** : Antigravity (Google DeepMind Team)  
**Package d'application** : `com.ghdinteractivestudio.codestation` (`.debug`)  
**Cible d'exécution** : Ubuntu PRoot ARM64 (Linux Android aarch64)  
**Statut Global** : **VALIDÉ & OPÉRATIONNEL (6/6 CLI AUTHENTIFIÉES)**

---

## 1. Contexte & Objectif

L'objectif de cette mission critique était de corriger définitivement le Roster de CodeStation, la détection des CLI et le gestionnaire central `/root/CodeStation/start_code_station.sh` :
1. **Élimination définitive de Codex** (`codex`) de la liste active et du Roster.
2. **Conservation et détection de CodeBuddy Code** (`codebuddy`).
3. **Détection, intégration et affichage de la nouvelle CLI Freebuff** (`freebuff` v0.2.35).
4. **Intégration complète des 6 CLI réellement installées et authentifiées** :
   - **OmniRush** (`omnirush` 2.7.8)
   - **Clixad** (`clixad` 0.0.1-beta.41)
   - **Google Antigravity** (`agy` 1.4.12)
   - **OpenCode GLM** (`opencode` v2.2.1)
   - **CodeBuddy Code** (`codebuddy` 2.178.1)
   - **Freebuff** (`freebuff` 0.2.35)
5. **Nettoyage non destructif du Roster** : exclusion des bots obsolètes ou non installés (`OpenAI Codex`, `Claude Architect`, `CodeStation Core`, `Codync`) du Roster actif, tout en préservant l'intégralité des historiques de conversations dans Room et SQLite.
6. **Commandes de cycle de vie unifiées** : `start_code_station.sh cli-status`, `health`, `status`, `start`, `stop`, `restart`.

---

## 2. État Réel des 6 CLI Installées (Vérification Système)

| Nom de la CLI | Binaire Exécutable | Version Détectée | Modèle Associé | Fichier de Configuration / Auth | Statut |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **OmniRush** | `/usr/local/bin/omnirush` | `2.7.8` | `gpt-6-astra` | `/root/.omnirush/auth.json` | `AUTHENTICATED` |
| **Clixad** | `/usr/local/bin/clixad` | `0.0.1-beta.41` | `gpt-5-nano` | `/root/.clixad/config.json` | `AUTHENTICATED` |
| **Google Antigravity** | `/root/.local/bin/agy` | `1.4.12` | `gemini-4-argon-medium` | `/root/.gemini/antigravity-cli` | `AUTHENTICATED` |
| **OpenCode GLM** | `/usr/local/bin/opencode` | `v2.2.1` | `opencode/glm-5.3-flash-free` | `/root/.config/opencode/opencode.json` | `AUTHENTICATED` |
| **CodeBuddy Code** | `/usr/local/bin/codebuddy` | `2.178.1` | `Hy3` | `/root/.codebuddy/settings.json` | `AUTHENTICATED` |
| **Freebuff** | `/usr/local/bin/freebuff` | `0.2.35` | `freebuff/free-auto` | `/root/.freebuff/project-id` | `AUTHENTICATED` |

> [!NOTE]
> **OpenAI Codex (`codex`)** a été formellement retiré du système et de toutes les configurations de détection. Son exécutable n'est plus présent dans le PATH et le système affiche `command not found`.

---

## 3. Travaux de Remédiation Réalisés

### A. Moteur de Détection Python (`scripts/bot_env_manager.py`)
- Refonte complète de la fonction `detect_clis(use_cache: bool = True)`.
- Implémentation du contrôle en profondeur pour les 6 CLI :
  1. Résolution du chemin absolu (`candidate_paths` + `shutil.which`).
  2. Contrôle des droits d'exécution (`os.access(p, os.X_OK)`).
  3. Extraction directe et résiliente de la version (avec fallback direct sur `package.json` pour les packages Node).
  4. Vérification physique des identifiants et jetons d'authentification (`auth_paths`).
  5. Attribution des statuts normalisés : `AUTHENTICATED`, `AVAILABLE`, `NOT_INSTALLED`, `ERROR`.
  6. Support des drapeaux `--no-cache` et `force` pour rafraîchissement immédiat.

### B. Gestionnaire Central (`start_code_station.sh`)
- Ajout de la commande officielle dédiée `cli-status` (et alias `check-agents`, `clis`) affichant l'état des 6 CLI au format clair et colorisé.
- Mise à jour du contrôle de santé `health` : vérification stricte des 6 CLI opérationnelles (`6/6 CLI opérationnels détectés`).
- Export propre du PATH pour PRoot ARM64 :
  `export PATH="/root/.gemini/antigravity-cli/bin:/root/.local/bin:/usr/local/bin:/usr/bin:/bin:$PATH"`
- Idempotence garantie sur `start`, `stop`, `restart`, `status`.

### C. Hôte Rust & Persistance Centrale (`host/src/hub.rs` & `codync.db`)
- Mise à jour de `ensure_default_bots()` pour initialiser exclusivement les 6 bots officiels :
  - `bot-antigravity` (Position 1, lead)
  - `bot-opencode-glm` (Position 2)
  - `bot-omnirush` (Position 3)
  - `bot-clixad` (Position 4)
  - `bot-codebuddy` (Position 5)
  - `bot-freebuff` (Position 6)
- Dans la base SQLite `codync.db` :
  - Marquage `deleted = 1` pour les bots historiques et obsolètes (`bot-codex-terminal`, `bot-claude-architect`, `bot-lead-codestation`, `8731af25-da7f-4955-9b37-247f45c60912`), garantissant que le serveur ne les instancie pas tout en conservant leurs messages en base.

### D. Application Android Native Kotlin (`app/`)
- `domain/model/Bot.kt` :
  - Ajout des valeurs `OMNIRUSH`, `CLIXAD`, `FREEBUFF` dans l'énumération `ProviderType` avec leurs modèles par défaut respectifs.
- `domain/model/CliDetection.kt` :
  - Ajout des statuts `AUTHENTICATED`, `AVAILABLE`, `NOT_AUTHENTICATED`, `NOT_INSTALLED` dans `CliHealthStatus`.
- `data/local/BotRepositoryImpl.kt` :
  - Amorçage initial (`getInitialBotsList()`) des 6 bots réels.
  - Filtrage non destructif dans `getBotsStream()` et `refreshBots()` : `OBSOLETE_BOT_IDS` est filtré du flux Compose, garantissant que seuls les 6 bots actifs sont affichés dans le Roster, sans supprimer les tables de messages Room.

---

## 4. Résultats des Vérifications et Tests

### 1. Test CLI Status (`start_code_station.sh cli-status`)
```text
==================================================================
       STATUT DES CLI ET ENGINES OFFICIELS CODESTATION           
==================================================================
• OmniRush             : AUTHENTICATED (omnirush 2.7.8)
  ├─ Exécutable : /usr/local/bin/omnirush
  └─ Détails    : CLI omnirush (omnirush 2.7.8) authentifié et opérationnel avec modèle gpt-6-astra
• Clixad               : AUTHENTICATED (0.0.1-beta.41)
  ├─ Exécutable : /usr/local/bin/clixad
  └─ Détails    : CLI clixad (0.0.1-beta.41) authentifié et opérationnel avec modèle gpt-5-nano
• Google Antigravity   : AUTHENTICATED (1.4.12)
  ├─ Exécutable : /root/.local/bin/agy
  └─ Détails    : CLI agy (1.4.12) authentifié et opérationnel avec modèle gemini-4-argon-medium
• OpenCode GLM         : AUTHENTICATED (opencode v2.2.1)
  ├─ Exécutable : /usr/local/bin/opencode
  └─ Détails    : CLI opencode (opencode v2.2.1) authentifié et opérationnel avec modèle opencode/glm-5.3-flash-free
• CodeBuddy Code       : AUTHENTICATED (2.178.1)
  ├─ Exécutable : /usr/local/bin/codebuddy
  └─ Détails    : CLI codebuddy (2.178.1) authentifié et opérationnel avec modèle Hy3
• Freebuff             : AUTHENTICATED (0.2.35)
  ├─ Exécutable : /usr/local/bin/freebuff
  └─ Détails    : CLI freebuff (0.2.35) authentifié et opérationnel avec modèle freebuff/free-auto
==================================================================
```

### 2. Contrôle de Santé (`start_code_station.sh health`)
```text
=== Contrôle de Santé Complet de CodeStation ===
• Vérification Socket & HTTP Host (19222)... VALIDE
• Vérification Socket & HTTP CAMCS (19223)... VALIDE
• Vérification Socket & HTTP Memory (19224)... VALIDE
• Vérification Watchdog Superviseur... VALIDE (PID: 22082)
• Vérification Outils CLI PRoot (omnirush, clixad, agy, opencode, codebuddy, freebuff)... 6/6 CLI opérationnels détectés

[✓] TOUS LES SERVICES CODESTATION SONT PARFAITEMENT SAINS ET FONCTIONNELS !
```

### 3. Tests Unitaires Android (`./gradlew testDebugUnitTest`)
- **75/75 tests réussis** (`BUILD SUCCESSFUL in 1m 11s`).
- Tests de vérification de Roster :
  - `Roster filters out obsolete bots and exposes only installed CLIs` : **SUCCÈS**.
  - `P1-02 getBotsStream seeds initial 6 bots when Room database is completely empty` : **SUCCÈS**.
  - `P1-01 testConnection calculates activeBotCount from Room BotDao` : **SUCCÈS**.

### 4. Compilation et Déploiement APK
- Binaire APK généré : `/root/CodeStation/app/build/outputs/apk/debug/app-debug.apk` (22 MB).
- Installation sur l'appareil connecté (`192.168.1.13:37537` / PLE110) : `Success`.
- L'application se lance et expose les 6 bots configurés en base Room.
