# FEATURE ADOPTION PLAN — PLAN D'ADOPTION TECHNIQUE FESTIVAL DANS CODESTATION

> **Projet source** : Festival  
> **Projet cible** : CodeStation (Local `/root/CodeStation`)  
> **Principe directeur** : Intégration modulaire, sélective et incrémentale. Aucune réécriture globale. Préservation stricte de la stabilité, des API, de la mémoire perpétuelle et du Design System Glassmorphism.

---

## 1. Synthèse des Capacités Sélectionnées

Après audit approfondi des deux codebases, six axes technologiques majeurs de Festival ont été retenus pour enrichir CodeStation :

```
┌────────────────────────────────────────────────────────────────────────┐
│                   CODESTATION FEATURE ENRICHMENT                       │
├──────────────────────────────┬─────────────────────────────────────────┤
│ 1. Tri Topologique Kahn      │ Graphe DAG déterministe & anti-cycles   │
│ 2. Hiérarchie Phases/Séquences│ Découpage multi-niveaux des missions   │
│ 3. Quality Gates Multi-types │ Testing, Review, Commit, Security gates │
│ 4. Session Recovery & Respin │ Reprise après crash & réconciliation    │
│ 5. Boucle Auto-Réparatrice   │ MaxIterations, retry intelligent & escalade│
│ 6. Supervision & Dashboard UI│ Panneaux Glassmorphism enrichis         │
└──────────────────────────────┴─────────────────────────────────────────┘
```

---

## 2. Détail Technique des 6 Modules d'Enrichissement

### A. Intelligent Task Planner & Kahn DAG
- **Objectif** : Remplacer l'ordonnancement séquentiel basique par un algorithme mathématique de tri topologique de Kahn avec détection de cycles.
- **Bénéfices** :
  - Détection formelle d'interblocage (cycles dans les dépendances) avant exécution.
  - Identification immédiate des tâches avec in-degree = 0 (tâches parallélisables sans conflit).
  - Découpage structuré en Phases (ex: 01-discovery, 02-scaffolding, 03-implementation, 04-verification).

### B. Quality Gates & Acceptance Verifiers
- **Objectif** : Compléter la vérification des fichiers par des portes de contrôle réelles inspirées de Festival :
  - `GateTesting` : Exécution automatique des tests unitaires/intégration (`evaluator_command`).
  - `GateReview` : Revue de livrable et critères d'acceptation.
  - `GateSecurity` : Contrôle de fuite de tokens, clés privées ou secrets dans les commits.
  - `GateCommit` : Validation de l'intégrité Git avant soumission.
- **Bénéfices** : Zéro fausse complétion ; les tâches ne sont déclarées `COMPLETED` que si toutes les portes sont validées.

### C. Session Recovery & Auto-Reconciliation
- **Objectif** : Permettre au démon CAMCS de réconcilier automatiquement les missions et tâches interrompues lors d'un crash, arrêt du conteneur PRoot ou déconnexion.
- **Bénéfices** :
  - Détection des tâches orphelines restées en `RUNNING`.
  - Vérification des livrables partiels déjà présents sur le disque.
  - Reprise automatique ou bascule contrôlée en `NEEDS_RETRY` avec journalisation dans `camcs.db`.

### D. Auto-Repair Loop & Error Recovery avec Escalade
- **Objectif** : Implémenter une boucle de remédiation automatique autonome inspirée de `fest/internal/loop/checker.go`.
- **Bénéfices** :
  - En cas d'échec d'une Quality Gate ou d'un runner CLI, réinjection de la trace d'erreur précise dans le prompt de remédiation.
  - Seuil d'itérations borné (`max_iterations = 3`).
  - Si le seuil est atteint sans succès, escalade automatique en demande d'approbation humaine (`NEEDS_APPROVAL`), sans boucle infinie ni crash du système.

### E. Persistent Memory & Context Retention
- **Objectif** : Lier formellement chaque mission et phase complétée à `CodeStation-Memory` en créant un enregistrement d'expérience complet (`manifest.json`, relations, règles apprises).
- **Bénéfices** : Réutilisation automatique des solutions apprises lors des missions futures.

### F. Mission Dashboard & Glassmorphism UI
- **Objectif** : Mettre à disposition de l'utilisateur un panneau de visualisation des Quality Gates, des phases DAG et de l'état de reprise dans `CamcsControlCenterScreen.kt`.
- **Bénéfices** : Visibilité temps réel complète sans aucune modification de la charte graphique ni des couleurs.

---

## 3. Ordre d'Exécution Séquentiel (Task-by-Task)

Toutes les tâches respecteront le cycle strict :
`ANALYSER → PLANIFIER → MODIFIER → TESTER → VALIDER → COMMIT → PUSH → TÂCHE SUIVANTE`.

1. **TASK-001** : Graphe DAG & Tri Topologique de Kahn (Détection de cycles et parallélisation).
2. **TASK-002** : Système de Quality Gates Multiniveaux (Testing, Review, Security, Deliverables).
3. **TASK-003** : Moteur de Reprise après Interruption & Réconciliation de Session (Session Recovery).
4. **TASK-004** : Boucle Auto-Itérative Corrective avec Escalade Humaine (Auto-Repair Loop).
5. **TASK-005** : Intégration Mémoire Perpétuelle des Décisions & Expériences de Mission.
6. **TASK-006** : Enrichissement UI du Centre de Contrôle CAMCS (Visualisation des Gates & Phases en Glassmorphism).
