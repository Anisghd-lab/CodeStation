# CAMCS Control Center — Mémoire Perpétuelle & Intégration Dynamic Island (Notch)

## 1. Vue d'Ensemble

Cette documentation décrit l'implémentation de la **Mémoire Perpétuelle Continue** et l'**Intégration Temps Réel de la Dynamic Island (Notch)** au sein du système CodeStation et de son moteur d'orchestration multi-agents CAMCS.

---

## 2. Architecture de la Mémoire Perpétuelle

### 2.1 Cycle d'Apprentissage Perpétuel
```
┌────────────────────────────────────────────────────────┐
│             Usage Utilisateur & Missions CAMCS         │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│      Collecte Automatique & Apprentissage Direct       │
│  - POST /api/memory/learn (Règles & Expériences)       │
│  - POST /api/memory/collect (Issues & DAG résolus)     │
│  - Stockage structuré dans /root/CodeStation-Memory/   │
└──────────────────────────┬─────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────┐
│      Injection Automatique dans les Agents CAMCS       │
│  - Query contextuelle avant exécution CLI              │
│  - Injection [MÉMOIRE PERPÉTUELLE — RÈGLES VALIDÉES]   │
│  - Évitement proactif des régressions & pièges connus  │
└────────────────────────────────────────────────────────┘
```

### 2.2 Endpoints Mémoire Actifs
- `GET /health` (Port 19224) : État de santé du moteur de mémoire continue.
- `POST /api/memory/search` : Recherche sémantique et par tags avec paramètre `q` ou `query`.
- `POST /api/memory/learn` : Ingestion directe d'une connaissance consolidée avec validation immédiate.
- `POST /api/memory/collect` : Traitement automatique des résultats de mission CAMCS avec consolidation des leçons apprises.
- `GET /api/camcs/memory?q=...` (Port 19223) : Pont mémoire interne CAMCS interrogeable par les agents.

---

## 3. Dynamic Island (Notch) — Supervision Temps Réel CAMCS

### 3.1 Synchronisation Réelle
La Dynamic Island (service overlay système `DynamicIslandOverlayService` et composant Compose `DynamicIslandBar`) observe en continu :
1. Les bots CodeStation actifs (`BotRepository.getBotsStream()`).
2. Les autorisations en attente (`ApprovalDao.getAllPendingApprovalsStream()`).
3. Les missions CAMCS actives (`CamcsRepository.activeMissionStream`).

### 3.2 Machine à États Unifiée
Priorité stricte des phases :
`NEEDS_APPROVAL` > `ERROR` > `STREAMING` > `WORKING` > `COMPLETED` > `PAUSED` > `IDLE` / `READY`.

### 3.3 Fonctionnalités Déployées
- **Pillule Réduite (Notch physique)** :
  - Dimensions compactes proportionnées (-35% à -40%, 14-16dp).
  - Horloge monotone non-bloquante (`formatIslandElapsed`).
  - Pulsation animée d'exécution (vert émeraude en travail, bleu électrique en streaming).
  - Compteur d'agents actifs ou statut qualitatif immédiat.
- **Carte Dépliée (Spring Glassmorphism)** :
  - Titre de la mission CAMCS en cours et pourcentage de progression vérifiable.
  - Avatar, rôle et nom de l'agent actif avec badge **PID réel**.
  - Extrait terminal des dernières sorties CLI en streaming continu.
  - **Bouton « Arrêter tâche »** : Déclenche l'arrêt immédiat du processus CLI actif via `POST /api/camcs/tasks/stop`.
  - **Bouton « Centre CAMCS »** : Ouvre directement le Centre de Contrôle CAMCS via le deep-link `codestation://camcs?missionId=...`.

---

## 4. Centre de Contrôle CAMCS — Arrêt de Tâche & Machine à 14 États

### 4.1 Arrêt de Tâche à la Demande
- L'interface Android propose un bouton rouge compact d'arrêt (`Arrêter`) sur la tâche active de chaque carte agent ainsi que sur la mission globale.
- L'appel est transmis via `CamcsRepository.stopTask(missionId, taskId, agentId)` vers le backend Python qui envoie les signaux appropriés (SIGTERM puis SIGKILL avec désallocation des PIDs dans `ProcessManager`).

### 4.2 Machine à 14 États Sans Blocage
- Découplage strict entre la phase du DAG et l'état de dispatching.
- Fin du faux gel « DISPATCHING » : la mission bascule immédiatement en `STARTING`, `RUNNING` ou `STREAMING` dès que les processus CLI sont instanciés.
