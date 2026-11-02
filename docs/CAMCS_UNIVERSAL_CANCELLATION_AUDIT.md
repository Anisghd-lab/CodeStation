# Audit Technique — Annulation Universelle des Missions & Failover CAMCS

## 1. Contexte & Problématique Initiale
Avant cette mise à niveau, le Centre de Contrôle CAMCS présentait les anomalies suivantes :
1. **Absence du bouton d'annulation** sur les anciennes missions actives, le sélecteur horizontal et les cartes d'agents individuels.
2. **Blocage en `WAITING_FOR_AGENT`** : lorsqu'un agent terminait sa sous-tâche, l'appel à `record_state_transition(mission.id, "WAITING_FOR_AGENT", ..., agent_id=arch.id)` écrasait le statut de l'agent terminé au lieu de conserver son état `COMPLETED`.
3. **Missions figées à 75%** : le DAG planner prévoyait 4 agents (Architecte, Développeur, Spécialiste Documentation, Auditeur QA). L'ancien workflow n'exécutait que 3 agents, laissant le spécialiste documentation à 0% (`CREATED`), ce qui provoquait une moyenne de `(100+100+0+100)/4 = 75%` même après la fin de la mission.
4. **Blocage lors de non-réponse d'une CLI** : si un runner CLI (ex: `antigravity`) dépassait le timeout ou échouait (ex: Code -9), le moteur ne disposait d'aucun basculement automatique vers une autre CLI opérationnelle.

---

## 2. Solutions & Corrections Appliquées

### A. Annulation Universelle
- **Backend (`scripts/camcs_engine.py` & `process_manager.py`)** :
  - `cancel_mission(mission_id)` interrompt immédiatement les boucles de travail, tue l'arborescence complète des processus OS (`process_manager.cancel_mission_tasks`), marque tous les agents et tâches en `CANCELLED` et émet l'événement SSE.
  - Prise en charge des missions en mémoire et directement dans SQLite pour les missions plus anciennes.
  - Déverrouillage récursif via `threading.RLock()`.
  - Nouvelle méthode et route API `delete_all_missions` (`/api/camcs/missions/clear`) pour purger la base et le système de fichiers.

- **Frontend Android (`CamcsControlCenterScreen.kt`)** :
  - Action d'annulation accessible sur chaque mission non-terminale dans :
    - `MissionMasterCard` (carte principale)
    - `MissionSelectorRow` (sélecteur horizontal de toutes les missions)
    - `LiveAgentCard` (arrêt individuel de tâche par agent)
  - États annulables : `CREATED`, `PLANNING`, `DISPATCHING`, `QUEUED`, `STARTING`, `RUNNING`, `STREAMING`, `WAITING_FOR_AGENT`, `WAITING_FOR_APPROVAL`, `VALIDATING`, `BLOCKED`, `PAUSED`, `WORKING`.
  - Masquage automatique pour les états terminaux (`COMPLETED`, `FAILED`, `CANCELLED`).
  - Protection contre les doubles requêtes via `cancellingMissions` (`CircularProgressIndicator` + désactivation du bouton pendant le transit réseau).

### B. Moteur de Failover & Exécution Résiliente (`execute_with_failover`)
- Si un runner CLI échoue, est interrompu (code -9) ou ne répond pas dans le délai :
  1. Détection immédiate de l'échec et émission de l'événement SSE `cli.failover`.
  2. Basculement automatique vers la prochaine CLI opérationnelle vérifiée (`codebuddy`, `clixad`, `opencode`).
  3. Si toutes les CLIs locales échouent : basculement direct vers l'API modèle gratuite (OpenRouter / xKIRO).
- Pour les tâches de recherche et de documentation ne nécessitant pas de token :
  - Interrogation directe des modèles gratuits via API OpenRouter / xKIRO sans bloquer de thread OS.

### C. Exécution Complète du DAG Multi-Agents
- Prise en charge explicite de tous les agents intermédiaires (`extra_agents`), y compris le spécialiste documentation et recherche.
- Mise à jour stricte :
  - Chaque agent qui termine passe à `COMPLETED` avec `progress = 100`.
  - La transition `WAITING_FOR_AGENT` cible l'agent suivant sans dégrader l'agent terminé.
  - À la clôture de la mission, `mission.progress = 100` et `mission.status = "COMPLETED"`.
