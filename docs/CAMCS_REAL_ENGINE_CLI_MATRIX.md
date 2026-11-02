# Matrice Complète des CLI — CAMCS Real Engine (v2.0)

**Date d'Audit** : Novembre 2026  
**Environnement** : Android Termux / Ubuntu 24.04 PRoot ARM64  
**Registre Attendu** : 6 CLI Officielles  

---

## 1. Tableau Synthétique de la Matrice des CLI

| CLI | Commande | Chemin Réel | Version | Authentification | Mode Non-Interactif | Statut Normalisé | Opérationnelle |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **Clixad** | `clixad` | `/usr/local/bin/clixad` | `0.0.1-beta.41` | Validée (25 200 crédits) | `clixad ask "<prompt>"` | `IMPLEMENTED_AND_VERIFIED` | **OUI** |
| **Google Antigravity** | `agy` | `/root/.local/bin/agy` | `1.4.12` | Validée (`~/.gemini`) | `agy -p "<prompt>" --dangerously-skip-permissions` | `IMPLEMENTED_AND_VERIFIED` | **OUI** |
| **OpenCode** | `opencode` | `/usr/local/bin/opencode` | `opencode v2.2.1` | Validée (xKiro, OpenCode) | `opencode run --standalone --auto -m "xkiro/qwen3-coder-plus" --format default "<prompt>"` | `IMPLEMENTED_AND_VERIFIED` | **OUI** |
| **CodeBuddy Code** | `codebuddy` | `/usr/local/bin/codebuddy` | `2.178.1` | Validée (`~/.codebuddy`) | `codebuddy -p -y "<prompt>"` | `IMPLEMENTED_AND_VERIFIED` | **OUI** |
| **OmniRush** | `omnirush` | `/usr/local/bin/omnirush` | `omnirush 2.7.8` | Validée (`Anisghd-lab`) | `omnirush -p "<prompt>"` | `BLOCKED` | **NON** (503 Upstream) |
| **Freebuff** | `freebuff` | `/usr/local/bin/freebuff` | `0.2.35` | Validée (`credentials.json`) | N/A (Mode interactif Curses TUI seul) | `PARTIALLY_IMPLEMENTED` | **NON** (TUI seul) |

---

## 2. Fiches Techniques Détaillées par CLI

### 2.1 Clixad (`clixad`)
- **Adaptateur** : `camcs/runners/clixad_runner.py`
- **Chemin Binaire** : `/usr/local/bin/clixad` (symlink vers installation Node/npm)
- **Authentification** : Compte vérifié (`M_ghediri@yahoo.com`), modèle par défaut `gpt-5-nano`, solde disponible : 25 200 crédits.
- **Protocole & Invocation** : `clixad ask "<prompt>"`
- **Capacités Déclarées & Testées** :
  - Revue de code, questions/réponses techniques, audit léger.
- **Performances Observées** : Réponse quasi instantanée (~3 000 ms) pour les revues de code.
- **Gestion des Erreurs** : En cas de charge serveur élevée (« *Clixad is currently down due to high demand* »), retourne le code 0 avec le message d'indisponibilité, permettant au Quality Auditor de requérir une seconde CLI.

### 2.2 Google Antigravity (`agy`)
- **Adaptateur** : `camcs/runners/antigravity_runner.py`
- **Chemin Binaire** : `/root/.local/bin/agy` (binaire Go natif ARM64)
- **Authentification** : Fichier de session et jetons dans `/root/.gemini/` et `~/.config/antigravity/`.
- **Protocole & Invocation** : `agy -p "<prompt>" --dangerously-skip-permissions`
- **Capacités Déclarées & Testées** :
  - Architecture logicielle de haut niveau, planification de DAG, décomposition de spécifications, refactorings complexes.
- **Performances Observées** : Temps d'exécution moyen de 60 à 95 secondes sur ARM64 pour des analyses approfondies.
- **Isolation** : Exécuté dans le sous-répertoire de la mission pour éviter l'indexation de `/root`.

### 2.3 OpenCode (`opencode`)
- **Adaptateur** : `camcs/runners/opencode_runner.py`
- **Chemin Binaire** : `/usr/local/bin/opencode` (binaire Rust/Node v2.2.1)
- **Authentification** : Fournisseurs xKiro et clés configurées dans `/root/.secrets/` et `/root/.config/opencode/`.
- **Modèle Retenu** : `xkiro/qwen3-coder-plus` (opérationnel avec code retour 0).
- **Protocole & Invocation** : `opencode run --standalone --auto -m xkiro/qwen3-coder-plus --format default "<prompt>"`
- **Particularité Découverte & Résolue** : L'option `--standalone` est **obligatoire** sous PRoot pour éviter la connexion au daemon d'arrière-plan démarré dans `/root`, qui provoquait des balayages git de 2 minutes sur tout le système. L'initialisation automatique d'un dépôt git local (`git init -q`) dans le répertoire de tâche garantit un confinement strict.
- **Capacités Déclarées & Testées** :
  - Génération de code Python/Rust/Kotlin, implémentation modulaire, corrections de bugs.
- **Performances Observées** : Temps de génération complet de 90 à 145 secondes sur ARM64.

### 2.4 CodeBuddy Code (`codebuddy`)
- **Adaptateur** : `camcs/runners/codebuddy_runner.py`
- **Chemin Binaire** : `/usr/local/bin/codebuddy` (binaire JS v2.178.1)
- **Authentification** : Profil utilisateur authentifié dans `/root/.codebuddy/`.
- **Protocole & Invocation** : `codebuddy -p -y "<prompt>"` (`-p` / `--print`, `-y` / `--dangerously-skip-permissions`).
- **Capacités Déclarées & Testées** :
  - Audit d'acceptation indépendant, vérification de sécurité (détection de failles path traversal), génération de tests unitaires.
- **Performances Observées** : Durée moyenne de 90 à 140 secondes pour un audit de conformité complet.
- **Comportement Remarquable** : Analyse critique rigoureuse capable de rejeter des livrables non conformes.

### 2.5 OmniRush (`omnirush`)
- **Adaptateur** : `camcs/runners/omnirush_runner.py`
- **Chemin Binaire** : `/usr/local/bin/omnirush` (v2.7.8)
- **Authentification** : Compte `Anisghd-lab`, statut `approved`, modèle activé `true`.
- **Statut Opérationnel** : `BLOCKED` (Indisponibilité temporaire amont).
- **Justification Empirique** : Le serveur cloud d'OmniRush renvoie une page de maintenance HTTP 503 : *« scaling up until 12:00 UTC 02 Nov »*.
- **Comportement du Routeur** : Détecté automatiquement comme non opérationnel. Toutes les requêtes vers OmniRush basculent de manière transparente vers `codebuddy` ou `antigravity`.

### 2.6 Freebuff (`freebuff`)
- **Adaptateur** : `camcs/runners/freebuff_runner.py`
- **Chemin Binaire** : `/usr/local/bin/freebuff` (binaire compilé natif 136 Mo à `/root/.config/manicode/freebuff`)
- **Authentification** : Fichier `credentials.json` présent et valide.
- **Statut Opérationnel** : `PARTIALLY_IMPLEMENTED` (Mode Curses TUI seul).
- **Justification Empirique** : La version v0.2.35 ne supporte pas d'argument non interactif (`-p` ou `--headless`). Son exécution sans terminal TTY lance une interface interactive plein écran impossible à piloter de façon déterministe en daemon.
- **Comportement du Routeur** : Freebuff est réservé aux interactions manuelles de l'utilisateur. Le routeur CAMCS transfère automatiquement toute tâche automatisée vers `opencode` ou `codebuddy`.

---

## 3. Matrice de Routage & Fallback

| Rôle Demandé | CLI Principale Sélectionnée | 1er Fallback | 2nd Fallback |
| :--- | :--- | :--- | :--- |
| **Lead Architect** | `antigravity` | `codebuddy` | `clixad` |
| **Backend Engineer** | `opencode` | `codebuddy` | `antigravity` |
| **Android Developer** | `codebuddy` | `opencode` | `antigravity` |
| **QA Engineer / Tests** | `codebuddy` | `antigravity` | `opencode` |
| **Peer Reviewer** | `clixad` | `codebuddy` | `antigravity` |
| **Security Auditor** | `codebuddy` | `antigravity` | `opencode` |
| **Documentation Specialist** | `clixad` | `antigravity` | `opencode` |
