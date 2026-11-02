# Architecture Système — CAMCS Real Execution Engine (v2.0)

**Version** : 2.0.0-PROD  
**Date** : Novembre 2026  
**Auteur** : Équipe d'Ingénierie CodeStation & Google Antigravity  
**Statut Global** : `IMPLEMENTED_AND_VERIFIED`  

---

## 1. Vision et Objectifs Fondamentaux

Le moteur **CAMCS** (*CodeStation Autonomous Multi-Agent Collaboration System*) v2.0 marque la transition d'un système d'affichage et de communications simulées vers un **véritable moteur d'orchestration multi-agents autonome pour la production logicielle**.

Dans cette architecture :
- **CAMCS** est le cerveau central et le planificateur d'orchestration (Python 3.14 asynchrone sur port `19223`).
- **Les Agents** sont des entités logiques dynamiques créées à la demande par l'**Agent Factory**, dotées d'objectifs, compétences, responsabilités et directives spécifiques.
- **Les CLI** constituent les moteurs réels d'exécution du système d'exploitation. Aucun code ni rapport n'est simulé ; les commandes sont exécutées dans des processus OS isolés avec capture des PID réels, des codes de retour et des flux standard.
- **Le Host Rust** (`codestation_host` sur port `19222`) gère l'infrastructure d'exécution bas niveau, le pontage de fichiers et les moniteurs système.
- **Continuous Memory** (`CodeStation-Memory` sur port `19224`) garantit la persistance de la mémoire collective, l'indexation sémantique et la capitalisation des missions passées.
- **CodeStation Android** (100% natif Kotlin / Jetpack Compose) agit comme centre de commande, supervision temps réel et approbation de gouvernance.

---

## 2. Diagramme d'Architecture Globale

```
                         +----------------------------------+
                         |       CODESTATION ANDROID        |
                         |  (Native Kotlin / Jetpack Compose)|
                         +-----------------+----------------+
                                           | HTTP REST / SSE Stream
                                           v
                         +----------------------------------+
                         |        CAMCS REST/SSE API        |
                         |       (Port 19223 / HTTP)        |
                         +-----------------+----------------+
                                           |
                                           v
                         +----------------------------------+
                         |        CAMCS ORCHESTRATOR        |
                         +--------+--------+--------+-------+
                                  |        |        |
            +---------------------+        |        +---------------------+
            v                              v                              v
+-----------------------+      +-----------------------+      +-----------------------+
|    MISSION PLANNER    |      |     AGENT FACTORY     |      |   GOVERNANCE ENGINE   |
| (DAG Task Generation) |      |   (Dynamic Roles)     |      | (Autonomous / Hybrid) |
+-----------+-----------+      +-----------+-----------+      +-----------+-----------+
            |                              |                              |
            +------------------------------+------------------------------+
                                           |
                                           v
                         +----------------------------------+
                         |          TASK SCHEDULER          |
                         |     (DAG Dependency Engine)      |
                         +-----------------+----------------+
                                           |
                                           v
                         +----------------------------------+
                         |         TASK DISPATCHER          |
                         |     (Intelligent CLI Router)     |
                         +-----------------+----------------+
                                           |
                                           v
                         +----------------------------------+
                         |           CLI REGISTRY           |
                         |     (Health Cache TTL 30s)       |
                         +-----------------+----------------+
                                           |
       +---------------+---------------+---+-----------+---------------+---------------+
       |               |               |               |               |               |
       v               v               v               v               v               v
+--------------+ +-------------+ +-------------+ +-------------+ +-------------+ +-------------+
|    CLIXAD    | | ANTIGRAVITY | |  OPENCODE   | |  FREEBUFF   | |  CODEBUDDY  | |  OMNIRUSH   |
|    RUNNER    | |   RUNNER    | |   RUNNER    | |   RUNNER    | |   RUNNER    | |   RUNNER    |
+-------+------+ +------+------+ +------+------+ +------+------+ +------+------+ +------+------+
        |               |               |               |               |               |
        +---------------+---------------+-------+-------+---------------+---------------+
                                                |
                                                v
                         +----------------------------------+
                         |         PROCESS MANAGER          |
                         |  (Max 2 Concurrency, PRoot ARM64)|
                         +-----------------+----------------+
                                           |
                                           v
                         +----------------------------------+
                         |        REAL CLI EXECUTION        |
                         |     (Isolated Mission Dir)       |
                         +-----------------+----------------+
                                           |
                    +----------------------+----------------------+
                    v                                             v
+---------------------------------------+     +---------------------------------------+
|          DELIVERABLE MANAGER          |     |            QUALITY AUDITOR            |
| (Artifacts Registration & SHA-256)    |     | (Unittest, AST & Independent CLI Audit)|
+-------------------+-------------------+     +-------------------+-------------------+
                    |                                             |
                    +----------------------+----------------------+
                                           |
                                           v
                         +----------------------------------+
                         |        COLLABORATION HUB         |
                         |   (Structured Handover Ledger)   |
                         +-----------------+----------------+
                                           |
                                           v
                         +----------------------------------+
                         |    COLLECTIVE MEMORY CLIENT      |
                         |    (Ingestion Port 19224)        |
                         +----------------------------------+
```

---

## 3. Registre Officiel des Six CLI

Le registre officiel des 6 CLI supportées par CAMCS v2.0 s'établit comme suit :

| CLI | Commande Système | Chemin Binaire Réel | Version Détectée | Protocole Réel | Statut Normalisé |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Clixad** | `clixad` | `/usr/local/bin/clixad` | `0.0.1-beta.41` | Headless Question/Reponse (`clixad ask`) | `IMPLEMENTED_AND_VERIFIED` |
| **Google Antigravity** | `agy` | `/root/.local/bin/agy` | `1.4.12` | Headless Prompt (`agy -p ... --dangerously-skip-permissions`) | `IMPLEMENTED_AND_VERIFIED` |
| **OpenCode** | `opencode` | `/usr/local/bin/opencode` | `opencode v2.2.1` | Run Standalone (`opencode run --standalone --auto -m ...`) | `IMPLEMENTED_AND_VERIFIED` |
| **CodeBuddy Code** | `codebuddy` | `/usr/local/bin/codebuddy` | `2.178.1` | Headless Non-interactif (`codebuddy -p -y ...`) | `IMPLEMENTED_AND_VERIFIED` |
| **OmniRush** | `omnirush` | `/usr/local/bin/omnirush` | `omnirush 2.7.8` | Cloud API CLI (`omnirush -p ...`) | `BLOCKED` (Maintenance Serveur Upstream) |
| **Freebuff** | `freebuff` | `/usr/local/bin/freebuff` | `0.2.35` | TUI Plein Écran (Curses interactif) | `PARTIALLY_IMPLEMENTED` (TUI uniquement) |

---

## 4. Composants Détaillés du Moteur

### 4.1 Base CLI Runner (`camcs/runners/base_runner.py`)
Tous les runners implémentent le contrat standardisé :
- `initialize() -> bool` : Découvre le binaire et extrait sa version.
- `health_check() -> Tuple[bool, str, str]` : Vérifie la viabilité opérationnelle et retourne un statut normalisé avec détails.
- `execute_task(task_id, mission_id, prompt, cwd, timeout, output_callback) -> CliExecutionResult` : Lance le processus OS réel, capture les sorties en flux, et retourne le code de sortie, la durée, le PID, et la liste des fichiers créés/modifiés.
- `cancel_task(task_id) -> bool` : Termine proprement le processus avec signal `SIGTERM` puis `SIGKILL`.
- `collect_artifacts(cwd, snapshot_before) -> Tuple[List[str], List[str]]` : Détecte les modifications physiques du système de fichiers.

### 4.2 Routeur Intelligent & Fallback (`camcs/cli_registry.py`)
Le routeur analyse le type de tâche (`ARCHITECTURE`, `CODING`, `REVIEW`, `TESTING`, `DOCUMENTATION`, `DEVOPS`) et oriente l'exécution vers la CLI la plus adaptée selon :
1. Sa disponibilité opérationnelle réelle (`is_operational == True`).
2. Son adéquation avec le rôle demandé.
3. Son historique de temps de réponse et de succès.
4. En cas d'indisponibilité ou d'échec, un basculement (*fallback*) automatique est opéré vers une CLI alternative opérationnelle (ex: `antigravity` -> `codebuddy` ou `opencode`).

### 4.3 Gestionnaire de Processus & Ressources (`camcs/process_manager.py`)
Compte tenu de l'environnement contraint Android / PRoot ARM64 :
- **Plafond de concurrence** : Fixé à **2 processus simultanés maximum** pour préserver la RAM et le processeur mobile.
- **Suivi des processus** : Surveillance active des PID, utilisation CPU et mémoire (via `/proc/<pid>/stat`).
- **Timeouts stricts** : Annulation automatique des processus bloqués (60s à 180s selon l'outil).
- **Nettoyage des orphelins** : Détection et terminaison systématique des processus résiduels.

### 4.4 Usine Dynamique d'Agents (`camcs/agent_factory.py`)
Ne limite plus le système à une liste statique de bots. Crée des agents spécialisés selon la mission :
- `Lead Architect`
- `Backend Engineer`
- `Android Developer`
- `Frontend Developer`
- `QA Engineer`
- `Security Auditor`
- `Documentation Specialist`
- `DevOps Engineer`
Chaque agent est doté d'instructions système précises et d'un rôle d'autorité.

### 4.5 Planificateur DAG & Dépendances (`camcs/dag_planner.py`)
Découpe l'objectif en graphe acyclique dirigé (DAG). Chaque tâche définit ses dépendances, ses critères d'acceptation stricts et son livrable attendu. Une tâche dépendante ne peut s'exécuter que lorsque tous ses prérequis ont été validés avec succès.

### 4.6 Gestionnaire de Livrables & Empreintes Cryptographiques (`camcs/deliverable_manager.py`)
Garantit qu'aucune mission n'est déclarée réussie sur la seule foi de communications simulées :
- Enregistrement des fichiers physiques générés dans `camcs.db`.
- Calcul systématique de l'empreinte cryptographique **SHA-256** pour chaque livrable.
- Stockage des preuves d'exécution (CLI, commande exacte, PID, durée, code de sortie 0).

### 4.7 Auditeur Qualité Indépendant (`camcs/quality_auditor.py`)
Vérifie la validité des artefacts produits :
- Contrôle d'intégrité physique (non-vacuité, format).
- Analyse statique de code Python (`ast.parse`) et linting.
- Exécution des tests unitaires (`unittest`) dans un environnement sandboxé.
- Audit croisé par une CLI indépendante distincte de la CLI productrice.

### 4.8 Hub de Collaboration Agent-to-Agent (`camcs/collaboration_hub.py`)
Structure les échanges inter-agents : transmission formelle de livrables (*Handover*), requêtes d'assistance, rapports de bugs et validations d'acceptation.

### 4.9 Intégration Mémoire Collective (`camcs/collective_memory_client.py`)
Communique avec le service `CodeStation-Memory` (port `19224`) pour indexer les missions achevées et fournir le contexte historique aux agents lors de la planification.

---

## 5. Modèle de Données & Persistance SQLite WAL

Toutes les données sont persistées dans `/root/.codestation/camcs.db` avec le mode SQLite **WAL** (*Write-Ahead Logging*) :
- `missions` : Objectifs, statuts (`PENDING`, `WORKING`, `COMPLETED`, `FAILED`), gouvernance, horodatages.
- `agents` : Identifiants, rôles, CLI assignées, contextes d'exécution.
- `tasks` : Graphe DAG, prérequis, statuts, critères d'acceptation.
- `deliverables` : Fichiers physiques, types, empreintes SHA-256, tailles en octets.
- `execution_proofs` : PIDs OS réels, codes de retour, durées en millisecondes, commandes exécutées.
- `agent_communications` : Journal structuré des échanges inter-agents.
- `events` : Flux d'événements SSE pour l'interface Android.

---

## 6. Flux d'Événements Temps Réel (SSE)

L'API expose un flux `GET /api/camcs/events` transmettant en temps réel chaque étape de l'exécution à l'application Android :
- `MISSION_CREATED`
- `TASK_PLANNED`
- `TASK_ASSIGNED`
- `CLI_PROCESS_STARTED` (avec PID réel)
- `CLI_OUTPUT_RECEIVED` (streaming des logs réels)
- `CLI_PROCESS_FINISHED` (avec code de sortie)
- `DELIVERABLE_CREATED` (avec empreinte SHA-256)
- `TEST_EXECUTED`
- `TASK_VALIDATED`
- `MISSION_COMPLETED`
