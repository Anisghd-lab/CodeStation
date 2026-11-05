# Rapport de Tâche — TASK-004 : Boucle Auto-Itérative Corrective avec Escalade Humaine

## Informations Générales
- **Identifiant** : TASK-004
- **Titre** : Auto-Repair Loop avec injection de contexte d'erreur et seuil d'escalade
- **Inspiration Festival** : `fest/internal/loop/checker.go`, `evaluator.go`, et `state.go`
- **Date** : 2026-11-05
- **Statut** : VALIDATED (Tests 6/6 passés avec succès)
- **Auteur** : Senior Multi-Agent Systems Engineer

---

## 1. Objectif & Contexte
Dans Festival, `loop/checker.go` analyse les échecs de conditions ou d'évaluations et orchestre des boucles de retry jusqu'à `max_iterations`, réinjectant les traces d'erreur (`validation_output`, `marker_list`) dans le prompt de la prochaine étape. Si le seuil maximal est atteint, Festival consigne une action `escalate` nécessitant une revue humaine (`Human review required`).

CodeStation CAMCS disposait d'un basculement de fournisseur (`ProviderFallbackManager`), mais ne possédait pas de boucle itérative unifiée pour corriger de manière autonome les livrables défaillants ou les tests en échec avant d'abandonner.

L'objectif de TASK-004 a été de doter CAMCS d'une boucle auto-réparatrice complète :
1. Capture et structuration du contexte d'erreur (`format_error_context`) avec code de sortie, traces stderr/stdout, et échecs de Quality Gates.
2. Construction d'un prompt correctif contextuel (`build_retry_prompt`) indiquant la tentative courante (`RETRY x/max`).
3. Comptabilisation des itérations par tâche (`iteration_count`, `max_iterations = 3`).
4. Escalade automatique vers l'opérateur humain (`TaskState.NEEDS_APPROVAL`, `MissionState.WAITING_FOR_APPROVAL`) dès dépassement du seuil.
5. Résolution d'escalade par l'opérateur (`approve_task` ou `reject_task`) avec ré-autorisation de tentatives et prompt de correction éventuel.
6. Persistance complète de l'historique dans SQLite (`repair_attempts`).

---

## 2. Modifications Réalisées

### A. Nouveau module : `scripts/camcs/auto_repair_loop.py`
- Classe `AutoRepairLoop` :
  - `attempt_repair(task, error_msg, stdout, stderr, exit_code, gate_failures)` : incrémente le compteur, évalue le seuil, génère le prompt de remédiation ou bascule en `NEEDS_APPROVAL`.
  - `build_retry_prompt(...)` : prompt structuré injectant l'erreur pour orienter précisément l'agent.
  - `format_error_context(...)` : mise en forme normalisée des signaux d'erreur.
  - `resolve_escalation(...)` : traitement de l'arbitrage humain (autorisation de tentatives supplémentaires ou clôture en échec).
  - `get_task_repair_history(...)` : extraction de la traçabilité des tentatives.

### B. Machine à états : `scripts/camcs/state_machines.py`
- Ajout de l'état `NEEDS_APPROVAL` à `TaskState`.
- Ajout des transitions autorisées : `RUNNING -> NEEDS_APPROVAL`, `VALIDATING -> NEEDS_APPROVAL`, `NEEDS_APPROVAL -> READY | RUNNING | CANCELLED | FAILED`.

### C. Modèle DAG & Schéma SQLite : `scripts/camcs/dag_planner.py` & `scripts/camcs_engine.py`
- Champs ajoutés sur `DagTask` : `iteration_count`, `max_iterations`, `last_error`, `escalation_reason`.
- Migration automatique SQLite pour les colonnes `iteration_count`, `max_iterations`, `escalation_reason` dans `tasks`.
- Table `repair_attempts` pour l'historique complet des itérations.

### D. Moteur d'Orchestration : `scripts/camcs_engine.py`
- Intégration de la boucle itérative dans `_execute_mission_workflow` : en cas d'erreur CLI ou d'invalidation, CAMCS déclenche l'auto-réparation plutôt qu'un arrêt brutal.
- Si le seuil (3) est dépassé, la tâche passe en `NEEDS_APPROVAL`, la mission en `WAITING_FOR_APPROVAL`, et l'événement SSE `task.escalated` est diffusé.
- Ajout des méthodes `approve_task()` et `reject_task()` sur le moteur.
- Ajout des routes REST :
  - `POST /api/camcs/tasks/approve`
  - `POST /api/camcs/tasks/reject`
  - `GET /api/camcs/tasks/{id}/repairs`

---

## 3. Validation & Tests
- **Tests unitaires dédiés** : `scripts/tests/test_auto_repair_loop.py` (6 tests, 100% succès) :
  - Formatage du contexte d'erreur.
  - Génération des prompts de retry.
  - Cycle de 3 tentatives puis escalade automatique à la 4ème tentative.
  - Résolution d'escalade avec approbation humaine et attribution de tentatives supplémentaires.
  - Résolution d'escalade avec rejet humain et transition vers `FAILED`.
  - Persistance et lecture de l'historique SQLite.
- **Régression complète** : Tous les tests CAMCS v2 et DAG Planner validés.

---

## 4. Respect du Design System CodeStation
Cette tâche étant une fonctionnalité d'orchestration backend et multi-agents, elle n'impacte aucun composant graphique existant. Les états et données d'escalade sont exposés via les API REST et événements SSE, prêts à être consommés par l'interface Jetpack Compose lors de TASK-006.
