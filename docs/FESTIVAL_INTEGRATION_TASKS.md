# FESTIVAL INTEGRATION TASKS — REGISTRE PERMANENT DES TÂCHES

> **Cadre de travail** : Mode d'exécution autonome strict Tâche par Tâche (`Task-by-Task`).  
> **Cycle obligatoire** : ANALYSER → PLANIFIER → MODIFIER → TESTER → VALIDER → COMMIT → PUSH → TÂCHE SUIVANTE.  
> **Règle absolue** : Ne jamais traiter plusieurs tâches simultanément. Ne jamais altérer le thème Glassmorphism de CodeStation.

---

## 1. Tableau de Suivi Permanent

| ID | Fonctionnalité | Type | Priorité | État | Commit | GitHub |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TASK-001** | Intelligent DAG Planner & Tri Topologique de Kahn | AMÉLIORATION D'UN MODULE EXISTANT | Critique (P0) | PUSHED | eb06bdf | feature/festival-integration |
| **TASK-002** | Système de Quality Gates Multi-types & Acceptance Verifiers | NOUVELLE FONCTIONNALITÉ | Critique (P0) | PUSHED | c9cc15a | feature/festival-integration |
| **TASK-003** | Moteur de Reprise après Interruption & Réconciliation de Session | NOUVELLE FONCTIONNALITÉ | Critique (P0) | PUSHED | 4bb6dcc | feature/festival-integration |
| **TASK-004** | Boucle Auto-Itérative Corrective avec Escalade Humaine | NOUVELLE FONCTIONNALITÉ | Haute (P1) | PUSHED | 09619c4 | feature/festival-integration |
| **TASK-005** | Intégration Mémoire Perpétuelle des Décisions & Expériences | AMÉLIORATION D'UN MODULE EXISTANT | Haute (P1) | PUSHED | 1ad3e66 | feature/festival-integration |
| **TASK-006** | Enrichissement UI du Centre de Contrôle CAMCS (Quality Gates & Phases Glassmorphism) | AMÉLIORATION D'UNE FONCTIONNALITÉ EXISTANTE | Haute (P1) | PUSHED | c7cb151 | feature/festival-integration |

---

## 2. Fiches Techniques Détaillées des Tâches

### TASK-001 — Intelligent DAG Planner & Tri Topologique de Kahn (Inspiré de Festival `fest/internal/deps/graph.go`)
1. **Nom** : Tri topologique de Kahn, détection de cycles et extraction des tâches parallélisables.
2. **Objectif** : Doter CAMCS d'un ordonnanceur mathématique formel garantissant un ordre d'exécution strict et sans risque d'interblocage.
3. **Problème à résoudre** : Le planificateur actuel de CodeStation ordonnance les tâches par simple liste sans validation de graphe acyclique, risquant des deadlocks si un agent déclare une dépendance circulaire.
4. **Fonctionnalité à ajouter/améliorer** : Implémenter l'algorithme de Kahn dans `DagPlanner`, détecter les cycles (`CycleDetectedException`), et exposer la liste dynamique des tâches immédiatement prêtes (`get_ready_tasks()`).
5. **État actuel dans CodeStation** : `scripts/camcs/dag_planner.py` gère des `dependencies: List[str]` simples sans tri topologique formel.
6. **Solution technique retenue** : Calcul des degrés entrants (`in_degrees`), file d'attente des tâches à degré zéro, défilement avec décrémentation des dépendants, détection d'interblocage si le nombre de tâches ordonnées est inférieur au total.
7. **Fichiers à créer** :
   - `scripts/tests/test_dag_topological_sort.py`
8. **Fichiers à modifier** :
   - `scripts/camcs/dag_planner.py`
   - `scripts/camcs_engine.py`
9. **Classes et fonctions concernées** :
   - Classe `DagPlanner` : `topological_sort()`, `has_cycle()`, `find_cycle()`, `get_ready_tasks()`, `CycleDetectedError`.
   - Classe `CamcsEngine` : intégration dans `create_mission()` et `_execute_dag()`.
10. **Dépendances** : Aucune (Python standard).
11. **Priorité** : **Critique (P0)**.
12. **Risques de régression** : Nuls ; si aucun cycle n'est présent, le comportement existant est parfaitement préservé.
13. **Critères d'acceptation** :
    - Graphe linéaire ordonné dans le bon sens topologique.
    - Graphe avec branches multiples ordonné sans inversion.
    - Détection de cycle levant `CycleDetectedError` avec le chemin du cycle.
    - Tâches indépendantes identifiées pour parallélisation (`in_degree == 0`).
14. **Tests nécessaires** :
    - Test unitaire Python dédié testant DAG linéaire, arborescent, diamant, et circulaire.
    - Exécution de la suite de régression globale CAMCS.
15. **Résultat attendu** : Ordonnancement infaillible et mathématiquement vérifié.

---

### TASK-002 — Système de Quality Gates Multi-types & Acceptance Verifiers (Inspiré de Festival `fest/internal/gates/` & `fest/internal/loop/`)
1. **Nom** : Quality Gates Multi-types (Testing, Review, Security, Deliverables) & Validateurs d'acceptation.
2. **Objectif** : Interdire la validation d'une tâche tant que ses critères de qualité (tests unitaires réels, absence de secrets, intégrité physique) ne sont pas validés.
3. **Problème à résoudre** : Le moteur actuel vérifie uniquement l'existence physique du fichier et son hash SHA-256 sans exécuter formellement les commandes de test ni contrôler la conformité de sécurité.
4. **Fonctionnalité à ajouter/améliorer** : Créer le moteur `QualityGateEngine` évaluant 4 types de portes (`TESTING`, `SECURITY`, `DELIVERABLES`, `REVIEW`) avec exécution de commandes sandboxées et reporting détaillé.
5. **État actuel dans CodeStation** : `QualityAuditor` dans `scripts/camcs/quality_auditor.py` limité au SHA-256 et aux repos distants.
6. **Solution technique retenue** : Classe `QualityGateEngine` intégrée à `QualityAuditor`, table `quality_gates` dans SQLite `camcs.db`, endpoints REST `/api/camcs/missions/<id>/gates` et `/api/camcs/tasks/<id>/gates`.
7. **Fichiers à créer** :
   - `scripts/camcs/quality_gates.py`
   - `scripts/tests/test_quality_gates.py`
8. **Fichiers à modifier** :
   - `scripts/camcs/quality_auditor.py`
   - `scripts/camcs_engine.py`
9. **Classes et fonctions concernées** :
   - Classe `QualityGate`, `QualityGateEngine`, `GateType`, `GateStatus`.
   - Méthodes `evaluate_gate()`, `evaluate_all_for_task()`, `store_gate_result()`.
10. **Dépendances** : TASK-001.
11. **Priorité** : **Critique (P0)**.
12. **Risques de régression** : Faibles. Les vérifications de livrables existantes sont conservées en tant que `DELIVERABLES` gate.
13. **Critères d'acceptation** :
    - Évaluation d'une commande de test (code 0 = `PASSED`, code non-nul = `FAILED`).
    - Évaluation de sécurité (détection de faux secrets/tokens = `FAILED`).
    - Évaluation de livrables (existence + hash = `PASSED`).
    - Statut persisté en base SQLite et transmis par l'API.
14. **Tests nécessaires** :
    - Suite de tests unitaires `scripts/tests/test_quality_gates.py`.
15. **Résultat attendu** : Zéro fausse complétion, vérification rigoureuse avant tout commit.

---

### TASK-003 — Moteur de Reprise après Interruption & Réconciliation de Session (Inspiré de Festival `continuation.go` & `reconcile`)
1. **Nom** : Session Recovery & Auto-Reconciliation des missions interrompues.
2. **Objectif** : Reprendre automatiquement ou réconcilier sans perte les missions et tâches coupées lors d'un crash, d'une déconnexion ou d'un arrêt de PRoot.
3. **Problème à résoudre** : En cas d'arrêt imprévu de l'application ou du démon, les tâches au statut `RUNNING` restent bloquées indéfiniment.
4. **Fonctionnalité à ajouter/améliorer** : Routine de réconciliation au démarrage du démon CAMCS qui analyse les tâches `RUNNING`, vérifie l'état réel des livrables et des processus OS, et reprend le travail au point exact d'interruption.
5. **État actuel dans CodeStation** : Pas de réconciliation automatique au démarrage.
6. **Solution technique retenue** : Méthode `reconcile_and_resume_interrupted_missions()` dans `camcs_engine.py`, avec token de reprise et journalisation de continuité.
7. **Fichiers à créer** :
   - `scripts/tests/test_session_recovery.py`
8. **Fichiers à modifier** :
   - `scripts/camcs_engine.py`
   - `scripts/camcs/process_manager.py`
9. **Classes et fonctions concernées** :
   - `CamcsEngine.reconcile_and_resume_interrupted_missions()`
   - `ProcessManager.cleanup_orphans()`
10. **Dépendances** : TASK-001, TASK-002.
11. **Priorité** : **Critique (P0)**.
12. **Risques de régression** : Faibles. Les missions déjà terminées (`COMPLETED`) ne sont pas altérées.
13. **Critères d'acceptation** :
    - Une mission `RUNNING` au démarrage est détectée.
    - Les tâches orphelines sans livrables sont remises à `READY` et relancées.
    - Les tâches ayant produit leurs livrables sont validées via Quality Gates et marquées `COMPLETED`.
    - Aucune tâche n'est perdue ni dupliquée.
14. **Tests nécessaires** :
    - Test unitaire simulant une mission interrompue et vérifiant sa réconciliation.
15. **Résultat attendu** : Résilience absolue face aux interruptions et reboots.

---

### TASK-004 — Boucle Auto-Itérative Corrective avec Escalade Humaine (Inspiré de Festival `fest/internal/loop/checker.go`)
1. **Nom** : Auto-Repair Loop avec injection de contexte d'erreur et seuil d'escalade.
2. **Objectif** : Tenter une correction autonome des erreurs de tests ou de livrables jusqu'à `max_iterations = 3`, puis escalader vers l'opérateur humain.
3. **Problème à résoudre** : Actuellement, en cas d'erreur de tâche, le système bascule soit en échec direct soit sur un autre modèle sans boucle itérative structurée.
4. **Fonctionnalité à ajouter/améliorer** : Implémenter le cycle itératif de remédiation avec compteur d'itérations, transmission de la trace d'erreur à l'agent, et bascule en `NEEDS_APPROVAL` / `ESCALATED` en cas d'insuccès répété.
5. **État actuel dans CodeStation** : `ProviderFallbackManager` existe mais sans compteur d'itérations d'auto-réparation unifié avec injection d'erreur.
6. **Solution technique retenue** : Module `AutoRepairLoop` dans `scripts/camcs/`, gestion du champ `iteration_count` et `max_iterations` dans la tâche.
7. **Fichiers à créer** :
   - `scripts/camcs/auto_repair_loop.py`
   - `scripts/tests/test_auto_repair_loop.py`
8. **Fichiers à modifier** :
   - `scripts/camcs_engine.py`
9. **Classes et fonctions concernées** :
   - `AutoRepairLoop.attempt_repair()`, `should_escalate()`, `format_error_context()`.
10. **Dépendances** : TASK-001, TASK-002.
11. **Priorité** : **Haute (P1)**.
12. **Risques de régression** : Nuls.
13. **Critères d'acceptation** :
    - Échec d'une gate → incrémentation d'itération et relance avec prompt enrichi de l'erreur.
    - Si tentative <= 3 et succès → tâche marquée `COMPLETED`.
    - Si tentative > 3 → tâche basculée en `NEEDS_APPROVAL` avec raison d'escalade.
14. **Tests nécessaires** :
    - Test unitaire simulant échec puis succès, et échec répété avec escalade.
15. **Résultat attendu** : Autonomie accrue sans risque de boucle infinie.

---

### TASK-005 — Intégration Mémoire Perpétuelle des Décisions & Expériences (Inspiré de Festival `camp/internal/concept` & `dungeon`)
1. **Nom** : Intégration Mémoire des décisions, phases et solutions d'architecture dans `CodeStation-Memory`.
2. **Objectif** : Sauvegarder automatiquement les enseignements et décisions d'architecture d'une mission pour les réutiliser dans les futures missions.
3. **Problème à résoudre** : CAMCS ingère les livrables mais ne consigne pas systématiquement les décisions clés ni les règles d'évitement d'erreurs issues des Quality Gates.
4. **Fonctionnalité à ajouter/améliorer** : Enregistrement structuré des décisions de mission et indexation dans `/root/CodeStation-Memory/experiences/` et `knowledge/`.
5. **État actuel dans CodeStation** : `CollectiveMemoryClient` existe mais n'est appelé que superficiellement en fin de mission.
6. **Solution technique retenue** : Enrichir `CollectiveMemoryClient` et ajouter un hook automatique de capitalisation après chaque Quality Gate réussie ou après escalade résolue.
7. **Fichiers à créer** :
   - `scripts/tests/test_memory_decision_sync.py`
8. **Fichiers à modifier** :
   - `scripts/camcs/collective_memory_client.py`
   - `scripts/camcs_engine.py`
9. **Classes et fonctions concernées** :
   - `CollectiveMemoryClient.record_decision()`, `record_mission_synthesis()`.
10. **Dépendances** : TASK-002.
11. **Priorité** : **Haute (P1)**.
12. **Risques de régression** : Nuls (opération additive non bloquante).
13. **Critères d'acceptation** :
    - Enregistrement d'une décision validée avec horodatage et contexte de mission.
    - Synchronisation avec `/root/CodeStation-Memory`.
14. **Tests nécessaires** :
    - Test unitaire d'enregistrement et recherche de décision.
15. **Résultat attendu** : Capitalisation durable des connaissances d'ingénierie.

---

### TASK-006 — Enrichissement UI du Centre de Contrôle CAMCS (Quality Gates & Phases Glassmorphism)
1. **Nom** : Panneaux Quality Gates, Dépendances DAG & Reprise dans `CamcsControlCenterScreen.kt`.
2. **Objectif** : Visualiser l'état des Quality Gates, les phases de mission et le statut de reprise directement dans l'interface Android, avec préservation stricte du design Glassmorphism.
3. **Problème à résoudre** : L'écran actuel affiche les tâches et les livrables basiques, mais n'expose pas encore l'état détaillé des Quality Gates ni les indicateurs de reprise/itérations.
4. **Fonctionnalité à ajouter/améliorer** : Ajouter un sous-panneau de contrôle des Quality Gates dans `CamcsControlCenterScreen.kt` avec cartes translucides, badges `PASSED` (vert néon `#00E676`) / `FAILED` (rouge `#FF5252`), et affichage des dépendances topologiques.
5. **État actuel dans CodeStation** : `CamcsControlCenterScreen.kt` dispose d'onglets pour la mission, les livrables, les communications et les preuves.
6. **Solution technique retenue** : Étendre les modèles de données Kotlin (`CamcsModels.kt`) et ajouter la section d'inspection des Quality Gates et des phases dans le respect scrupuleux de `docs/DESIGN_SYSTEM_AUDIT.md`.
7. **Fichiers à modifier** :
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/domain/model/CamcsModels.kt`
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/ui/screens/CamcsControlCenterScreen.kt`
8. **Classes et composables concernés** :
   - `CamcsQualityGate`, `CamcsPhaseInfo`.
   - Composable `CamcsQualityGatesSection()`, `CamcsQualityGateCard()`.
9. **Dépendances** : TASK-001, TASK-002, TASK-003, TASK-004.
10. **Priorité** : **Haute (P1)**.
11. **Risques de régression** : Nécessite une vérification rigoureuse de la non-altération du thème et de la compilation Kotlin.
12. **Critères d'acceptation** :
    - Affichage harmonieux des Quality Gates avec le modificateur `Modifier.frostedLiquidGlass()`.
    - Respect absolu des couleurs et de la palette Cyan/Obsidian/Néon.
    - Aucune modification des écrans non concernés.
13. **Tests nécessaires** :
    - Compilation du projet Kotlin.
    - Contrôle visuel et vérification de cohérence du design system.
14. **Résultat attendu** : Clarté opérationnelle sans altération de l'identité visuelle de CodeStation.
