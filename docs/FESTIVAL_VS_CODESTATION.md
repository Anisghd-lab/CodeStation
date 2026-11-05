# FESTIVAL VS CODESTATION — MATRICE COMPARATIVE & ANALYSE D'INTÉGRATION

> **Documents sources** : `FESTIVAL_TECHNICAL_AUDIT.md`, `CODESTATION_TECHNICAL_AUDIT.md`  
> **Objectif** : Identifier précisément les forces de Festival à adopter dans CodeStation sans régresser, sans écraser l'existant, et en conservant 100% du Design System Glassmorphism de CodeStation.

---

## 1. Matrice Comparative Globale

| Fonctionnalité | Festival (Implémentation Réelle) | CodeStation (État Actuel) | Amélioration Nécessaire pour CodeStation | Priorité |
| :--- | :--- | :--- | :--- | :--- |
| **Task Planner** | Décomposition hiérarchique : Festival → Phases → Séquences → Tâches Markdown | Décomposition en 11 profils de tâches par `TaskClassifier` & `DagPlanner` | Structuration en Phases & Séquences avec critères d'acceptation stricts | Haute |
| **Dependency Graph** | Tri topologique formel de Kahn (`fest/internal/deps/graph.go`) avec détection de cycles et in-degrees | Dépendances simples par IDs dans `DagTask.dependencies` sans tri formel de Kahn | Intégrer l'algorithme de Kahn et détection de cycles dans `dag_planner.py` | Critique |
| **Persistent Memory** | Camp Markdown/Git (`.camp/`, `.fest/`, `intents/`, `dungeon/`) | `CodeStation-Memory` (Dépôt Git, relations JSON, vector search port 19224) | Connecter la mémoire de décision aux phases de mission et aux archives | Haute |
| **Agent Orchestration** | Superviseur daemon `obey` + 12 skills agents pour Claude, Codex, Grok, OpenCode | `AgentFactory`, `CliRegistry`, `CollaborationHub` orchestrant AGY, OpenCode, CodeBuddy | Synchronisation dynamique des rôles et enrichissement du dispatching | Haute |
| **Session Recovery** | Continuation Tokens (`continuation.go`), notification d'arbitre, journal `STATE.yaml` | Persistance SQLite (`camcs.db`), mais absence de reprise automatique des tâches interrompues | Moteur de réconciliation et reprise automatique (`reconcile_and_resume`) | Critique |
| **Quality Gates** | 6 types de portes (`Testing`, `Review`, `Commit`, `Security`, etc.) + 5 évaluateurs | Vérification physique SHA-256 + vérification d'actions externes GitHub | Quality Gates formels pré- et post-tâche avec évaluateur de commandes et de tests | Critique |
| **Agent Handoff** | Transfert explicite de contexte via sortie formatée et entrées de session Obey | Handoff partiel via `CollaborationHub` et base SQLite | Enregistrement de paquets de transmission inter-agents standardisés | Moyenne |
| **Background Daemon** | Démon `obey` résident en arrière-plan avec socket Unix et TUI | Démon HTTP/SSE multithread (`camcs_engine.py` port 19223) + Hôte Rust (19222) | Surveillance watchdog automatique, reconnexion transparente | Haute |
| **Task Monitoring** | CLI `fest watch`, `fest next`, TUI `obey top` | `DynamicIslandBar.kt`, `CamcsControlCenterScreen.kt`, `DynamicNeuralGraphView.kt` | Intégrer la vue détaillée des Quality Gates et des phases dans l'écran CAMCS existant | Haute |
| **Parallel Execution** | Détection des tâches avec in-degree = 0 dans le graphe de dépendances | `ProcessManager` avec `threading.Semaphore(max_concurrent=2)` | Planification parallèle intelligente basée sur les in-degrees du DAG | Haute |
| **Error Recovery** | Boucle `LOOP.yaml` avec `MaxIterations` et escalade vers l'humain | Retry basique avec repli sur d'autres fournisseurs IA (`ProviderFallbackManager`) | Boucle d'auto-réparation avec injection du contexte d'erreur et arrêt à `MaxIterations` | Critique |

---

## 2. Analyse Détaillée par Fonctionnalité

### 2.1 Task Planner & Dependency Graph (Algorithme de Kahn)
1. **Fonctionnement dans Festival** : `fest/internal/deps/graph.go` prend la liste des tâches et de leurs dépendances, calcule le degré entrant (`inDegree`) de chaque nœud, initialise une file d'attente avec les nœuds indépendants (`inDegree == 0`), puis dépile en décrémentant les dépendants. S'il reste des nœuds non visités, une erreur de cycle (`CycleError`) est immédiatement retournée avec la trace du cycle.
2. **Implémentation réelle** : En Go, déterministe avec tri secondaire par numéro de tâche.
3. **Équivalent dans CodeStation** : `scripts/camcs/dag_planner.py` contient une classe `DagPlanner` qui construit une liste de `DagTask`. Cependant, elle ne vérifie pas mathématiquement l'absence de cycles et ne trie pas formellement les tâches par ordre topologique garanti.
4. **Différences architecturales** : CodeStation est en Python dans son moteur CAMCS. Le portage de l'algorithme de Kahn en Python est direct, pur, sans dépendance externe.
5. **Améliorations possibles** :
   - Ajouter `topological_sort()` et `detect_cycles()` dans `scripts/camcs/dag_planner.py`.
   - Fournir les tâches parallélisables (celles prêtes immédiatement).
   - Intégrer les concepts de `Phase` et `Sequence` au sein du `DagTask`.
6. **Fichiers CodeStation concernés** :
   - `scripts/camcs/dag_planner.py`
   - `scripts/camcs_engine.py`
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/domain/model/CamcsModels.kt`
7. **Complexité** : Moyenne (très ciblée, sans régression).
8. **Risques** : Nuls si les tests de régression existants sont préservés.
9. **Priorité** : **CRITIQUE (P0)**.

---

### 2.2 Quality Gates Multiniveaux
1. **Fonctionnement dans Festival** : `fest/internal/gates/` et `fest/internal/loop/` définissent des portes (`GateTesting`, `GateReview`, `GateCommit`, `GateSecurity`, `GatePerformance`). Chaque porte est évaluée par un évaluateur dédié (`evaluator_command`, `evaluator_file_exists`, `evaluator_markers`).
2. **Implémentation réelle** : Vérifie l'exécution des tests unitaires, l'absence de secrets, et les livrables avant de marquer une tâche ou une phase comme achevée.
3. **Équivalent dans CodeStation** : `QualityAuditor` (`quality_auditor.py`) et `ObjectiveCompletionGate` (`objective_completion_gate.py`). Ces modules vérifient actuellement la présence des fichiers physiques, leur SHA-256 et des dépôts distants GitHub.
4. **Différences architecturales** : CodeStation n'a pas encore standardisé l'exécution automatique d'une commande de test comme gate bloquante avant validation, ni la vérification de conformité de sécurité de code (absence de tokens ou de fuites de données sensibles).
5. **Améliorations possibles** :
   - Enrichir `QualityAuditor` avec un moteur d'évaluation de Quality Gates (`test_gate`, `security_gate`, `lint_gate`, `commit_gate`).
   - Ajouter une table `quality_gates` dans SQLite `camcs.db` et exposer les résultats sur l'API `/api/camcs/missions/{id}/gates`.
6. **Fichiers CodeStation concernés** :
   - `scripts/camcs/quality_auditor.py`
   - `scripts/camcs/objective_completion_gate.py`
   - `scripts/camcs_engine.py`
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/domain/model/CamcsModels.kt`
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/ui/screens/CamcsControlCenterScreen.kt`
7. **Complexité** : Moyenne.
8. **Risques** : Faibles.
9. **Priorité** : **CRITIQUE (P0)**.

---

### 2.3 Session Recovery & Auto-Reconciliation
1. **Fonctionnement dans Festival** : `continuation.go` et `STATE.yaml` permettent de reprendre une session après interruption via des jetons de continuation et la réconciliation des processus orphelins.
2. **Implémentation réelle** : Stockage du dernier état franchi et réactivation de la tâche bloquée ou non terminée.
3. **Équivalent dans CodeStation** : `scripts/camcs_engine.py` stocke l'état dans `camcs.db`. Si le serveur redémarre, les tâches restées à l'état `RUNNING` ne sont pas automatiquement réconciliées ni relancées.
4. **Différences architecturales** : CodeStation a déjà SQLite ; il suffit d'ajouter la routine de démarrage `reconcile_interrupted_missions()` qui détecte les tâches interrompues, vérifie les livrables déjà produits, et reprend le DAG au point exact d'interruption.
5. **Améliorations possibles** :
   - Implémenter `reconcile_and_resume_interrupted_missions()` dans `camcs_engine.py`.
   - Journaliser chaque reprise dans `CamcsJournalEntity` (table `mission_journal`).
6. **Fichiers CodeStation concernés** :
   - `scripts/camcs_engine.py`
   - `scripts/camcs/process_manager.py`
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/data/local/CamcsJournalEntity.kt`
7. **Complexité** : Faible à Moyenne.
8. **Risques** : Très faibles.
9. **Priorité** : **CRITIQUE (P0)**.

---

### 2.4 Auto-Repair Loop & Error Recovery avec Escalade
1. **Fonctionnement dans Festival** : `fest/internal/loop/checker.go` réinjecte les erreurs de tests dans le prompt de la tâche jusqu'à `MaxIterations`. Si le seuil est dépassé, il escalade (`escalate: true`).
2. **Implémentation réelle** : `LoopConfig` avec conditions et contextes de réessai.
3. **Équivalent dans CodeStation** : CodeStation bascule sur un modèle IA alternatif (`ProviderFallbackManager`), mais ne conserve pas formellement un compteur d'itérations correctives avec injection du diff d'erreur et seuil d'escalade vers l'approbation humaine.
4. **Améliorations possibles** :
   - Ajouter la gestion des itérations correctives avec `max_iterations = 3` par défaut.
   - En cas d'échec répété, basculer la tâche à l'état `NEEDS_APPROVAL` / `ESCALATED` et notifier l'interface Android (`ApprovalsScreen.kt`).
5. **Fichiers CodeStation concernés** :
   - `scripts/camcs_engine.py`
   - `scripts/camcs/quality_auditor.py`
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/ui/screens/ApprovalsScreen.kt`
6. **Complexité** : Moyenne.
7. **Risques** : Faibles.
8. **Priorité** : **HAUTE (P1)**.

---

### 2.5 Enrichissement du Tableau de Bord Android (Supervision & Quality Gates)
1. **Fonctionnement dans Festival** : `fest watch` et `obey top` affichent l'avancement dans le terminal.
2. **Implémentation réelle dans CodeStation** : L'écran `CamcsControlCenterScreen.kt` et la barre `DynamicIslandBar.kt` affichent déjà les missions, agents, livrables et logs dans un style Glassmorphism très soigné.
3. **Amélioration requise** :
   - Ajouter un onglet ou volet dédié aux **Quality Gates** et aux **Phases du DAG** dans `CamcsControlCenterScreen.kt`.
   - Utiliser exclusivement les tokens visuels existants (`Palette.electricBlue`, `Palette.glassSurface`, `Modifier.frostedLiquidGlass()`).
4. **Fichiers CodeStation concernés** :
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/ui/screens/CamcsControlCenterScreen.kt`
   - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/ui/components/CamcsNewTaskCard.kt`
5. **Complexité** : Moyenne.
6. **Risques** : Attention stricte à préserver l'identité visuelle et le thème actuel.
7. **Priorité** : **HAUTE (P1)**.
