# FESTIVAL TECHNICAL AUDIT — RAPPORT D'AUDIT TECHNIQUE APPROFONDI

> **Projet audité** : Festival ([https://github.com/Obedience-Corp/festival](https://github.com/Obedience-Corp/festival))  
> **Composants sources examinés** : Suite `festival` (v0.3.25), `fest` (v0.3.x, commit `765955e`), `camp` (commit `f9f05c3`), `festival-installer` (commit `65cf902`), distribution daemon `obey` (v0.2.3)  
> **Date de l'audit** : 5 Novembre 2026  
> **Auditeur** : Senior Software Architect & Multi-Agent Systems Engineer  

---

## 1. Vue d'Ensemble & Synthèse Exécutive

Festival est présenté comme un *framework de « vibe engineering »* et d'ingénierie logicielle autonome, conçu pour permettre aux développeurs d'assigner des objectifs de haut niveau à des agents d'intelligence artificielle de manière structurée, reproductible et vérifiable.

Contrairement aux frameworks centrés sur une simple boucle de chat ou des prompts monolithiques, Festival décompose le travail en trois couches architecturales distinctes :
1. **Camp (`camp`)** : Gestionnaire d'espace de travail unifié, d'intentions de développement (*intents*), d'inventaire de projets interconnectés (*worktrees* et liens git), et de mémoire de contexte partagé persistant.
2. **Fest (`fest`)** : Moteur de planification et d'exécution déterministe de missions de longue durée (*festivals*), structurées en phases hiérarchiques, séquences de tâches, graphes de dépendances acycliques (DAG), et portes d'approbation/qualité (*quality gates*).
3. **Obey (`obey` daemon & `ob` CLI)** : Démon résident d'arrière-plan (*supervisor daemon*), gérant l'orchestration des sessions agents (Claude Code, Codex, Grok, OpenCode), la capture des flux d'exécution, la surveillance des terminaux, et l'interaction via des commandes spécialisées (`ob plan`, `ob impl`, `ob review`, `ob commit`).

### Règle d'or de l'audit : Réalité Implémentée vs Annoncée
- **Ce qui est réellement open-source et exécutable dans le dépôt** : Les outils `camp`, `fest`, et `festival-installer` sont intégralement écrits en **Go** (Go 1.22/1.23) avec une suite de tests unitaires et d'intégration très poussée. Le format de persistance repose sur le système de fichiers (`.fest/`, `.camp/`, fichiers Markdown avec frontmatter YAML).
- **Ce qui est distribué sous forme de binaire externe propriétaire/fermé** : Le démon `obey` et sa CLI `ob` font l'objet d'un produit binaire packagé par `festival-installer` depuis les releases publiques GitHub (`Obedience-Corp/obey-releases`), avec spécifications protocolaires et hooks d'environnement (`OBEY_SESSION_ID`, `obey session notify`).

---

## 2. Architecture Générale

### 2.1 Organisation du Dépôt
Le dépôt racine `festival` agit comme un métadépôt structuré :
- `.gitmodules` : Lie les dépôts composants en sous-modules :
  - `fest/` : Moteur de planification, DAG, validation de contrat, boucles d'évaluation et portes de qualité.
  - `camp/` : Gestionnaire de contexte de travail, dépôts git, intentions, items de travail et transferts réseau.
  - `festival-installer/` : Outil de packaging, installateur TUI (Bubbletea), distribution binaire sécurisée (vérification cryptographique des signatures et manifests JSON).
- `claude-plugin/` & `cursor-plugin/` : Connecteurs et plugins d'intégration pour les éditeurs IDE et assistants de code.
- `skills/` : 12 skills standardisés (YAML + Markdown) guidant les agents de façon autonome (`fest-execution`, `fest-planning`, `fest-methodology`, `camp-workitems`, `camp-projects`, `campaign-structure`, etc.).
- `hooks/` : Scripts de hooks git et de démarrage de session.
- `docs/` : Documentation Hugo exhaustive des commandes et de la méthodologie.

### 2.2 Communication Interne & Flux de Données
```
┌─────────────────────────────────────────────────────────────┐
│                       OPÉRATEUR HUMAIN                      │
└──────────────┬──────────────────────────────┬───────────────┘
               │ (Objectif / Intentions)      │ (Supervision TUI / CLI)
               ▼                              ▼
┌──────────────────────────────┐ ┌────────────────────────────┐
│      CAMP (Contexte)         │ │   OBEY (Superviseur Daem.) │
│  - Projects & Worktrees      │ │  - Local HTTP/Unix socket  │
│  - Intents & Designs         │ │  - Session tracking/tree   │
│  - Context registry (~/.obey)│ │  - Process wrap & PIDs     │
└──────────────┬───────────────┘ └────────────┬───────────────┘
               │                              │
               │ Fournit le workspace & state │ Lance & supervise
               ▼                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      FEST (Moteur de Plan)                   │
│  - Parser Markdown/YAML (Festivals, Phases, Sequences)      │
│  - Graph DAG & Kahn's Topological Sort                      │
│  - Gate Evaluator (Command, File, Schema, Human Approval)   │
│  - Continuation Token & Session Resume Loop                 │
└──────────────────────────────┬──────────────────────────────┘
                               │ Assigne la tâche N
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                      AGENTS D'EXÉCUTION                     │
│        (Claude Code, Codex, Grok, OpenCode, etc.)           │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Analyse Détaillée des Modules Clés

### 3.1 Task Planning (Fest)
- **Décomposition Hiérarchique** : `Festival` → `Phase` (01-discovery, 02-scaffolding, 03-implementation...) → `Sequence` (séries ordonnées de tâches) → `Task` (fiches unitaires Markdown).
- **Graphe de Dépendances (DAG)** : Implémenté dans `fest/internal/deps/graph.go`. Utilise l'algorithme de tri topologique de **Kahn** pour identifier les tâches prêtes à être exécutées, calculer l'ordre déterministe d'exécution et détecter immédiatement les cycles (`CycleError`).
- **Parallélisation** : Détermination des tâches ayant un in-degree de 0. Celles-ci peuvent être exécutées simultanément sans conflit de dépendance.
- **Réévaluation en cas d'échec** : Gérée par la boucle `fest/internal/loop/checker.go`. Si une tâche échoue ou ne remplit pas les conditions, le moteur exécute une boucle corrective jusqu'à `MaxIterations`. Au-delà, il déclenche une escalade (`escalate`) vers l'opérateur humain.

### 3.2 Orchestration et Gestion des Agents (Obey & Skills)
- **Sessions Découplées** : Festival n'exécute pas l'IA dans un processus bloquant monolithique. Les agents sont des processus CLI autonomes exécutés dans leur propre arborescence ou worktree.
- **Contrôle via Hooks et Enveloppes** : Injection des variables d'environnement (`OBEY_SESSION_ID`, `OBEY_CAMPAIGN_ID`).
- **Mode Dispatch & Handoff** : Capacité de passer le résultat d'une tâche d'un agent d'exploration (ex: recherche d'architecture) à un agent de codage (implémentation), puis à un agent de test (validation).

### 3.3 Persistent Memory & Contexte Partagé (Camp)
- **Stockage Transparent dans Git** : Aucun stockage opaque propriétaire. Les plans et décisions sont rédigés en Markdown (`FESTIVAL.md`, `PHASE.md`, `TASK.md`, `WORKFLOW.md`, `LOOP.yaml`).
- **Registres de Camp** : Fichier `~/.obey/campaign/registry.json` et métadonnées locales `.camp/config.json`.
- **Historique et Décisions** : Fichiers `design/`, `intents/`, et `dungeon/` (dépôt d'archives des missions passées réutilisables).

### 3.4 Session Management & Reprise après Interruption
- **Continuation Tokens** : Implémenté dans `fest/internal/continuation/continuation.go`.
- **Persistance d'État par Événements** : `StateEvent` stocké dans un fichier journal d'état (`STATE.yaml`).
- **Idempotence & Anti-Replay** : Prévention stricte des doubles exécutions grâce aux statuts de tâche immuables (`settled`, `completed`, `blocked`).

### 3.5 Quality Gates & Critères d'Acceptation
- **Types de Gates Réels** (`fest/internal/gates/gate_type.go`) :
  1. `GateTesting` : Exécution de commandes de tests automatisés.
  2. `GateReview` : Revue de code obligatoire par un modèle ou un humain.
  3. `GateIterate` : Boucle de révision avec injection des retours de test.
  4. `GateCommit` : Validation de l'intégrité du git stage avant commit.
  5. `GateSecurity` : Contrôles d'absence de secrets et permissions.
  6. `GatePerformance` : Seuils de temps d'exécution.
- **Évaluateurs Intégrés** (`fest/internal/loop/`):
  - `evaluator_command.go` : Exécution d'une commande shell avec code retour.
  - `evaluator_file_exists.go` : Vérification physique des fichiers générés.
  - `evaluator_schema.go` : Vérification du respect d'un schéma JSON/YAML.
  - `evaluator_markers.go` : Contrôle des cases à cocher et marqueurs obligatoires dans les documents.

### 3.6 CLI et Intégrations Fournisseurs
- **Support Multi-CLI** : Conçu pour s'interfacer avec Claude Code, GitHub Copilot CLI/Codex, Grok Build, OpenCode, et Gemini CLI.
- **Transparence Financière** : Aucun modèle imposé ; Festival utilise les abonnements et clés déjà configurés sur la machine locale du développeur.

---

## 4. Forces et Faiblesses Identifiées dans Festival

### Forces Majeures
1. **Rigueur Mathématique du Plan** : Utilisation formelle d'un graphe acyclique dirigé (Kahn's Topological Sort) et détection robuste des boucles/deadlocks.
2. **Quality Gates Hiérarchiques** : Définition de politiques de qualité globales, héritées ou surchargées au niveau phase/tâche.
3. **Persistance 100% lisible** : Données au format Markdown/YAML directement lisibles et versionnables dans Git.
4. **Reprise Résiliente** : Continuation tokens et réconciliation de sessions interrompues.

### Faiblesses et Limitations
1. **Fragmentation en Trois Outils Séparés** : `camp`, `fest`, et `obey` requièrent une coordination manuelle ou des scripts complexes.
2. **Obey n'est pas Open Source** : Le cœur du superviseur daemon reste un binaire closed-source téléchargé depuis GitHub releases.
3. **Absence d'Interface Mobile / Native Moderne** : Festival est purement CLI/TUI pour serveurs Linux/macOS.
4. **Pas de Base de Connaissances Vectorielle ou Relationnelle Native** : La recherche de mémoire repose uniquement sur des parcours de fichiers Markdown via git.

---

## 5. Recommandations pour l'Intégration dans CodeStation

CodeStation dispose déjà :
- D'un backend hôte performant en **Rust** (`codestation-host`).
- D'un moteur d'orchestration multi-agents autonome en **Python** (`CAMCS` sur port 19223).
- D'une mémoire perpétuelle collective (`CodeStation-Memory`).
- D'une application **Android Kotlin 2.0 / Jetpack Compose** avec un design Glassmorphism d'exception.

**Éléments à adopter impérativement de Festival** :
1. **Algorithme de Tri Topologique Déterministe & Détection de Cycles (Kahn's Algorithm)** dans le `DagPlanner` de CAMCS.
2. **Système de Quality Gates Multiniveaux** (`Testing`, `Review`, `Commit`, `Security`) avec évaluateurs de commandes et de fichiers.
3. **Mécanisme de Reprise et Continuation Tokens** pour reprendre instantanément une mission interrompue après crash ou reboot.
4. **Matrice de Résilience avec Boucle Corrective Auto-itérative** (`MaxIterations` avec escalade humaine).
5. **Enrichissement du Tableau de Bord de Supervision CAMCS** dans l'application Android dans le respect absolu du thème Glassmorphism.
