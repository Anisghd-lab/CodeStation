# Architecture Complète : Agents, Sessions et Intégration CAMCS v2.0

**Projet** : CodeStation  
**Date** : 2 novembre 2026  
**Version Système** : 2.28.1

---

## 1. Vue d'Ensemble Architecturale

L'architecture de CodeStation sépare clairement la surface de contrôle Android, le pont d'orchestration Rust Host, le moteur multi-agents autonome CAMCS, et le moteur de mémoire continue persistante :

```
+--------------------------------------------------------------------------+
|                       CODESTATION ANDROID CLIENT                         |
|  (Kotlin 2.0 / Jetpack Compose / Liquid Glass / Room Database v3 / SSE)  |
+--------------------------------------------------------------------------+
                     | REST /api/createBot, /api/createConversation, /api/send
                     | SSE /events (Dynamic Island, Live Tasks, Message stream)
                     v
+--------------------------------------------------------------------------+
|                       CODESTATION HOST RUST (Port 19222)                  |
|  - Agent Registry & Workspace Isolation                                  |
|  - Multi-Session ACP Router (`conv_{conversationId}`)                    |
|  - SQLite Store (`codync.db`: bots, conversations, entries, kv)          |
|  - SSE Event Broadcaster & Process Supervisor                            |
+--------------------------------------------------------------------------+
                     |                             |
                     v                             v
+-----------------------------+     +--------------------------------------+
|    CAMCS ENGINE (Port 19223)|     | CONTINUOUS MEMORY ENGINE (Port 19224)|
|  - Real Execution Engine 2.0|     |  - Multi-Agent Learning Loop         |
|  - Dynamic Agent Factory    |     |  - Collector, Analyzer, Extractor    |
|  - DAG Planner & Dispatcher |     |  - Git Sync: CodeStation-Memory      |
|  - Deliverable Manager      |     +--------------------------------------+
+-----------------------------+
                     |
                     v
+--------------------------------------------------------------------------+
|                     REGISTRE OFFICIEL DES SIX CLI                        |
|                                                                          |
|  [Clixad]        [Google Antigravity]       [OpenCode]                   |
|  "clixad"        "agy"                      "opencode"                   |
|  Statut: VALIDÉ  Statut: VALIDÉ             Statut: VALIDÉ               |
|                                                                          |
|  [CodeBuddy]     [OmniRush]                 [Freebuff]                   |
|  "codebuddy"     "omnirush"                 "freebuff"                   |
|  Statut: VALIDÉ  Statut: OPÉRATIONNEL       Statut: TUI ADAPTATEUR       |
+--------------------------------------------------------------------------+
```

---

## 2. Cycle de Vie d'un Agent

1. **Création / Déclaration** :
   - L'utilisateur configure un agent personnalisé via `BotEditorScreen` (Nom, Rôle, Description, Instructions système, CLI associée parmi les 6 officielles, Modèle, Permissions).
   - `BotRepositoryImpl` soumet la requête au Host (`POST /api/createBot`).
   - Le Host valide la configuration, crée le dossier de travail sécurisé `/root/.codestation/bots/<bot_id>/workspace`, initialise les métadonnées et persiste dans `codync.db`.
   - `bot_env_manager.py` génère l'environnement PRoot sous Ubuntu ARM64 et configure les scripts d'amorçage `run.sh`.
   - La base locale Room enregistre `BotEntity` et émet la mise à jour réactive vers le `RosterViewModel`.

2. **Exécution et Surveillance** :
   - Lorsqu'une mission ou une commande est soumise, le Host assigne un processus au runner CLI correspondant.
   - Les sorties streaming, les notifications persistantes Android 16 et les événements Dynamic Island sont diffusés via le flux SSE `/events`.

---

## 3. Gestion Multi-Sessions et Isolation des Conversations

Pour garantir une étanchéité absolue entre les discussions simultanées d'un même agent :
1. **Identifiants de Conversation Dédiés** :
   Chaque session possède un UUID unique (`conversation_id`).
2. **Cloisonnement du Stockage** :
   - Dans SQLite (`codync.db`), les messages (`entries`) portent le tag `$.conversationId`.
   - Dans Android (`codestation.db`), la table `chat_messages` intègre une clé étrangère et un index sur `conversationId`.
3. **Isolation des Processus et Contexte ACP** :
   - Le Host Rust associe chaque conversation à un slot de session ACP : `conv_{conversationId}`.
   - La CLI sous-jacente (ex: OpenCode, Antigravity) instancie une nouvelle session de travail sans relire les prompts des discussions parallèles.

---

## 4. Intégration CAMCS et Réseau d'Agents Autonomes

CAMCS n'est pas un système de simulation :
- **DAG Planner** : Décompose un objectif logiciel de haut niveau en graphe orienté acyclique de tâches concrètes.
- **Agent Factory** : Génère dynamiquement les agents spécialistes (Lead Architect, Backend Engineer, QA Engineer, etc.) en leur associant les meilleures CLI du registre selon leurs aptitudes.
- **Deliverable Manager & Quality Auditor** : Inspecte physiquement les fichiers produits dans l'arborescence, valide la compilation et les tests avant de déclarer une étape réussie.
- **Collective Memory Client** : Extrait automatiquement les leçons apprises et les stocke dans `CodeStation-Memory` sous forme de fiches Markdown/JSON réutilisables.
