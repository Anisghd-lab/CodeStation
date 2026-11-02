# Rapport d'Audit Architectural & Diagnostic du Moteur CAMCS v2.0
**CodeStation Autonomous Multi-Agent Collaboration System**
*Date d'audit : 2 Novembre 2026*
*Système hôte : Ubuntu PRoot Linux ARM64 / Android Native (CodeStation)*

---

## 1. Contexte et Objectifs de la Mission

L'audit technique du moteur CAMCS existant avait pour objectif d'éradiquer les divergences entre l'affichage de l'interface Android et la réalité technique de l'exécution, d'éliminer les simulations ou interpolations visuelles, et de transformer CAMCS en un système autonome de production logicielle fiable, axé sur l'accomplissement des objectifs utilisateur.

L'injonction stricte de l'utilisateur a été intégrée comme invariant absolu : **CAMCS doit fonctionner de manière 100% autonome et utiliser exclusivement des modèles 100% gratuits (`:free`, allocations sans coût)** pour toute inférence API (OpenRouter, xKIRO, OpenCode Inference) tout en exploitant les CLI locaux opérationnels sur ARM64.

---

## 2. Reproduction et Diagnostic Exhaustif des Anomalies Observées

L'observation initiale du Centre de Contrôle CodeStation présentait un faisceau d'incohérences critiques :

| Symptôme Observé | Cause Racine Technique Identifiée | Impact Métier & Risque |
|---|---|---|
| **Mission affichée TERMINÉE à 75%** | Écrasement mutuel des statuts lors des transitions d'agents (`mission.status = agent.status`) combiné à une formule de progression basée sur un ratio d'agents plutôt que sur la validation des tâches obligatoires. | L'utilisateur voyait une mission réussie alors que des tâches critiques étaient non exécutées. |
| **8/8 tâches terminées mais progression à 75%** | Dans `_load_missions_from_db`, une liste de tâches fictives en dur était instanciée si la table SQLite `tasks` contenait des enregistrements partiels ou désynchronisés. | Fausse impression de progression, décorrélation entre les livrables réels et l'état affiché. |
| **0/4 agents actifs alors que plusieurs cartes affichent 100%** | Compteur d'agents actifs calculé par un filtre `status == RUNNING` strict dans l'UI sans consolidation avec la machine à états des agents. Un agent terminé n'était plus compté comme actif, mais le dénominateur restait figé à 4 (liste statique codée en dur). | Perte de confiance utilisateur dans les métriques affichées. |
| **Agents affichés ATTENTE AGENT malgré livrables validés** | L'état `WAITING_FOR_DEPENDENCY` ou `WAITING_FOR_AGENT` était injecté statiquement dans la vue Android lorsque l'agent n'avait pas émis d'événement dans les 30 dernières secondes, sans vérifier son état réel dans le backend. | Confusion de l'opérateur pensant que le système est gelé. |
| **Phases d'orchestration toutes terminées malgré incohérences** | Composant `OrchestrationFlowCard.kt` calculant localement la complétion des étapes par simple seuil de progression (`progress > 20% -> PLANNING OK`, `progress > 50% -> DISPATCHING OK`), sans lire `mission.orchestrationPhase` du backend. | Mensonge visuel sur l'état d'avancement de l'orchestration. |
| **Boutons Arrêter visibles sur des agents terminés** | Condition d'affichage dans `AgentGridCard.kt` autorisant le bouton `Arrêter` sur tout agent dont l'ID n'était pas vide, sans validation contre les états terminaux (`COMPLETED`, `FAILED`, `CANCELLED`). | Possibilité d'envoyer des requêtes d'annulation sur des processus déjà éteints. |
| **Anciennes missions sans bouton Annuler** | Liste blanche restrictive `isCancellableState` dans `CamcsControlCenterScreen.kt` omettant `INITIALIZING`, `PLANNING` et `DISPATCHING`. | Blocage complet des missions en phase amont impossible à interrompre par l'utilisateur. |
| **Divergence Backend / Processus CLI / DB / UI** | Absence de centralisation des événements et des états dans une source de vérité unique. Les runners CLI tournaient en tâche de fond sans notifier les transitions de manière transactionnelle dans SQLite. | Processus orphelins, états zombies et incohérence lors des redémarrages. |
| **Missions bloquées en DISPATCHING ou RUNNING sans diagnostic** | Boucle de dispatching bloquante sans timeout adaptatif ni surveillance de santé des CLI / providers API. | Gel silencieux du système nécessitant un redémarrage manuel du conteneur. |

---

## 3. Découverte Réelle de l'Environnement d'Exécution (Ubuntu ARM64)

L'audit physique des exécutables et clés d'API configurés sur la machine a révélé l'état réel suivant :

### A. Exécutables CLI Locaux
1. **Google Antigravity (`/root/.local/bin/agy`)** :
   - *Statut* : **OPÉRATIONNEL** (v1.4.12).
   - *Capacités* : Architecture, génération de code, refactorisation, outillage terminal, sandbox/bypass auto-approuvés.
2. **Clixad (`/usr/local/bin/clixad`)** :
   - *Statut* : **OPÉRATIONNEL** (v0.0.1-beta.41).
   - *Capacités* : Exécution rapide en ligne de commande, tâches QA, documentation, analyse syntaxique.
3. **OpenCode (`/usr/local/bin/opencode`)** :
   - *Statut* : **OPÉRATIONNEL** (v2.2.1).
   - *Capacités* : Multi-modèles, édition autonome de codebase, protocole ACP/stdio.
4. **CodeBuddy Code (`/usr/local/bin/codebuddy`)** :
   - *Statut* : **OPÉRATIONNEL** (v2.179.2).
   - *Capacités* : Mode non-interactif `-p -y`, éditions de projet complètes, tests unitaires.
5. **OmniRush (`/usr/local/bin/omnirush`)** :
   - *Statut* : **BLOQUÉ** (v2.7.8).
   - *Raison* : Session révoquée / expirée. Rejeté par le registre de santé CAMCS jusqu'à reconnexion explicite.
6. **Freebuff (`/usr/local/bin/freebuff`)** :
   - *Statut* : **NON RETENU POUR AUTOMATISATION** (v0.2.35).
   - *Raison* : Interface interactive ncurses stricte sans mode headless batch exploitable en daemon.

### B. Fournisseurs d'Inférence API (100% Modèles Gratuits)
1. **OpenRouter API** :
   - Clé active configurée dans l'environnement.
   - Filtrage strict : Découverte automatique des modèles avec tarification `$0.00 / prompt` et `$0.00 / completion`, ou suffixés `:free` (ex: `recursive/flint-v1.0:free`, `liquid/lfm-2.5-2.6b:free`, `nvidia/nemotron-3.5-lightning:free`).
2. **xKIRO API (Endpoint Compatible OpenAI)** :
   - Clé active trouvée dans `/root/.secrets/xkiro-key`.
   - Modèles gratuits identifiés : `qwen/qwen3-coder-plus:free`, `qwen/qwen3.6-27b:free`, `qwen/qwen3.5-plus:free`, `qwen/qwen3.7-max:free`, `meituan/longcat-2.5-preview:free`.
3. **OpenCode Inference Tier** :
   - Modèles gratuits certifiés : `jev-1.13-free`, `glm-5.3-flash-free`.

---

## 4. Architecture Rénovée CAMCS v2.0

Le moteur a été intégralement restructuré selon une architecture modulaire en couches garantissant la séparation des responsabilités :

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CodeStation Android UI                          │
│          (Jetpack Compose, Flow, StateInvariants, Clean Contract)      │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │ SSE Streams & REST API (Port 19223)
┌────────────────────────────────────▼───────────────────────────────────┐
│                    CAMCS Engine Kernel (Source Unique)                 │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ State Machines        │  │ DAG Objective Planner                 │  │
│  │ (Mission, Agent, Task)│  │ (Weighted progress, topological sort) │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ Dynamic Agent Factory │  │ Task Classifier (11 categories)       │  │
│  │ (Role & Skill Match)  │  │ (Resource & Complexity Profiling)     │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ Provider Registry     │  │ Deliverable & Quality Auditor         │  │
│  │ (100% Free Models)    │  │ (SHA-256 Checksum, Executable Proofs) │  │
│  └───────────────────────┘  └───────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ SQLite Transactional Persistence (/root/.codestation/camcs.db)    │  │
│  │ (Atomic commits, State Transitions, Event Log, Deliverables)     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────▲───────────────────────────────────┘
                                     │ Subprocess Supervisor (PIDs)
┌────────────────────────────────────▼───────────────────────────────────┐
│                 Operating Execution Engines (Ubuntu ARM64)              │
│   [Google Antigravity]  [Clixad]  [OpenCode]  [CodeBuddy]  [Free APIs] │
└────────────────────────────────────────────────────────────────────────┘
```

### Principes Architecturaux Clés Introduits :
1. **Source de Vérité Unique** : Aucune donnée d'état n'est générée par l'interface Android. Tout statut, progression, PID ou phase provient exclusivement de l'état SQLite persisté par le backend.
2. **Découplage des Transitions** : Les transitions d'état d'un agent individuel ne modifient jamais directement le statut de la mission globale. La mission évolue selon sa propre machine à états basée sur les validations des tâches du DAG.
3. **Formule Mathématique Pondérée** :
   $$\text{Progression Mission} = \frac{\sum_{i \in \text{Tâches Obligatoires}} \text{Weight}_i \times \text{Progress}_i}{\sum_{i \in \text{Tâches Obligatoires}} \text{Weight}_i}$$
   La mission ne peut atteindre `100%` et le statut `COMPLETED` que lorsque l'ensemble des livrables obligatoires ont été validés par l'auditeur de qualité indépendant.
4. **Annulation Universelle et Nettoyage de Processus** :
   Toute demande d'annulation (`/api/camcs/missions/cancel` ou `/api/camcs/tasks/stop`) résout récursivement les PIDs associés via `ProcessManager`, envoie un signal `SIGTERM` (suivi de `SIGKILL` si non terminé en 3 secondes), et persiste la raison d'arrêt.
5. **Résilience et Restauration à Froid** :
   Au redémarrage du démon, l'état complet des missions, des tâches et des livrables est rechargé depuis SQLite sans corruption ni perte d'historique.
