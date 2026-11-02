# Spécification Formelle des Machines à États CAMCS v2.0
**CodeStation Autonomous Multi-Agent Collaboration System**
*Référence : scripts/camcs/state_machines.py*

---

## 1. Vue d'Ensemble

CAMCS implémente trois machines à états finis disjointes, déterministes et validées par assertions strictes :
1. **Machine d'État Mission (`MissionState`)** : Gouverne l'accomplissement de l'objectif global.
2. **Machine d'État Agent (`AgentState`)** : Gouverne le cycle de vie opérationnel d'un exécutant.
3. **Machine d'État Tâche (`TaskState`)** : Gouverne l'avancement d'un nœud du DAG d'exécution.

Toute transition non déclarée dans la matrice des transitions autorisées est **rejetée** avec levée d'exception ou journalisation d'erreur explicite, empêchant tout état incohérent en mémoire ou en base de données.

---

## 2. Machine d'État Mission (`MissionState`)

### A. États Définis (15 États)
- `CREATED` : Mission déclarée, enregistrée en base, en attente d'ordonnancement.
- `PLANNING` : Décomposition de l'objectif, classification des tâches et construction du DAG.
- `DISPATCHING` : Affectation dynamique des agents, compétences, CLIs ou modèles gratuits.
- `QUEUED` : Mission prête, en file d'attente d'exécution sous respect des quotas de concurrence.
- `STARTING` : Démarrage des premiers processus sous-jacents.
- `RUNNING` : Au moins une tâche obligatoire est activement en cours d'exécution physique.
- `STREAMING` : Transmission continue de données d'exécution (ACP, stdout, logs).
- `WAITING_FOR_AGENT` : En attente de libération d'un agent ou d'un créneau d'exécution.
- `WAITING_FOR_APPROVAL` : En attente d'arbitrage de l'opérateur (si mode gouvernance interactif).
- `VALIDATING` : Exécution terminée, contrôle qualité indépendant des livrables et empreintes SHA-256.
- `CANCEL_REQUESTED` : Demande d'arrêt initiée par l'utilisateur ; arrêt des processus en cours.
- `COMPLETED` : **État terminal**. Toutes les tâches obligatoires sont validées, progression = 100%.
- `FAILED` : **État terminal**. Échec non recouvrable d'une tâche critique avec diagnostic d'erreur.
- `BLOCKED` : Blocage externe identifié (quota, dépendance réseau manquante) consigné en DB.
- `CANCELLED` : **État terminal**. Interruption confirmée, processus arrêtés, livrables partiels scellés.

### B. Matrice des Transitions Autorisées
```
CREATED             ──► PLANNING, DISPATCHING, QUEUED, CANCELLED
PLANNING            ──► DISPATCHING, BLOCKED, FAILED, CANCELLED
DISPATCHING         ──► QUEUED, STARTING, RUNNING, BLOCKED, FAILED, CANCELLED
QUEUED              ──► STARTING, RUNNING, CANCEL_REQUESTED, CANCELLED
STARTING            ──► RUNNING, FAILED, CANCEL_REQUESTED, CANCELLED
RUNNING             ──► STREAMING, WAITING_FOR_AGENT, WAITING_FOR_APPROVAL, VALIDATING, FAILED, CANCEL_REQUESTED, CANCELLED
STREAMING           ──► RUNNING, VALIDATING, FAILED, CANCEL_REQUESTED, CANCELLED
WAITING_FOR_AGENT   ──► RUNNING, DISPATCHING, BLOCKED, CANCEL_REQUESTED, CANCELLED
WAITING_FOR_APPROVAL──► RUNNING, CANCEL_REQUESTED, CANCELLED
VALIDATING          ──► COMPLETED, RUNNING (si correction requise), FAILED, CANCELLED
CANCEL_REQUESTED    ──► CANCELLED, FAILED
COMPLETED           ──► (Terminal - Aucune transition sortante)
FAILED              ──► (Terminal - Aucune transition sortante)
BLOCKED             ──► PLANNING, DISPATCHING, CANCELLED
CANCELLED           ──► (Terminal - Aucune transition sortante)
```

### C. Règles Métier Strictes
1. `COMPLETED` implique mathématiquement `progress == 100` et toutes les tâches obligatoires au statut `COMPLETED`.
2. Une mission ne peut jamais passer à `COMPLETED` si une tâche obligatoire est `FAILED`, `CANCELLED`, ou `PENDING`.
3. Une fois dans un état terminal (`COMPLETED`, `FAILED`, `CANCELLED`), aucun événement ne peut altérer le statut de la mission.

---

## 3. Machine d'État Agent (`AgentState`)

### A. États Définis (12 États)
- `CREATED` : Agent instancié par la Dynamic Agent Factory, profil et compétences attribués.
- `ASSIGNED` : Tâche spécifique rattachée à l'agent.
- `QUEUED` : En attente du démarrage du runner ou de la disponibilité de la ressource.
- `STARTING` : Lancement du sous-processus CLI ou de la session d'inférence API.
- `RUNNING` : Processus actif avec PID alloué ou appel API en cours.
- `STREAMING` : Réception active de tokens ou d'événements ACP/stdio.
- `WAITING_FOR_INPUT` : L'agent attend une clarification ou validation humaine.
- `WAITING_FOR_DEPENDENCY` : L'agent attend la finalisation d'un livrable produit par un pair.
- `VALIDATING` : L'agent vérifie localement la syntaxe ou conformité de son travail.
- `CANCEL_REQUESTED` : Signal d'arrêt transmis au runner.
- `COMPLETED` : **État terminal**. Livrable généré et enregistré dans le gestionnaire de livrables.
- `FAILED` : **État terminal**. Sortie en erreur (exit code != 0 ou exception API).
- `CANCELLED` : **État terminal**. Interruption prématurée ordonnée par l'utilisateur ou la mission.

### B. Matrice des Transitions Autorisées
```
CREATED                 ──► ASSIGNED, QUEUED, STARTING, RUNNING, CANCELLED
ASSIGNED                ──► QUEUED, STARTING, RUNNING, CANCELLED
QUEUED                  ──► STARTING, RUNNING, CANCEL_REQUESTED, CANCELLED
STARTING                ──► RUNNING, FAILED, CANCEL_REQUESTED, CANCELLED
RUNNING                 ──► STREAMING, WAITING_FOR_INPUT, WAITING_FOR_DEPENDENCY, VALIDATING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED
STREAMING               ──► RUNNING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED
WAITING_FOR_INPUT       ──► RUNNING, CANCEL_REQUESTED, CANCELLED
WAITING_FOR_DEPENDENCY  ──► RUNNING, CANCEL_REQUESTED, CANCELLED
VALIDATING              ──► COMPLETED, RUNNING, FAILED, CANCELLED
CANCEL_REQUESTED        ──► CANCELLED, FAILED
COMPLETED               ──► (Terminal)
FAILED                  ──► (Terminal)
CANCELLED               ──► (Terminal)
```

### C. Règles Métier Strictes
1. Un agent affiché à `100%` doit avoir son statut à `COMPLETED` et un livrable physique référencé.
2. Un agent au statut `COMPLETED`, `FAILED` ou `CANCELLED` n'est plus actif (`isActive == false`). L'interface désactive ou masque immédiatement le bouton "Arrêter".
3. Un agent au statut `WAITING_FOR_DEPENDENCY` doit obligatoirement expliciter la tâche ou l'agent attendu dans son champ `currentSubtask`.

---

## 4. Machine d'État Tâche (`TaskState`)

### A. États Définis (8 États)
- `PENDING` : Tâche déclarée dans le plan, non encore éligible à l'exécution.
- `READY` : Toutes les dépendances amont sont satisfaites (`status == COMPLETED`).
- `RUNNING` : En cours de réalisation par l'agent assigné.
- `BLOCKED` : Dépendance amont échouée ou annulée.
- `COMPLETED` : Tâche achevée avec succès, livrable vérifié.
- `FAILED` : Exécution échouée après épuisement des tentatives de fallback.
- `CANCELLED` : Annulée par cascade ou arrêt ciblé utilisateur.
- `SKIPPED` : Tâche facultative ignorée lors d'une replanification dynamique.

---

## 5. Mécanisme de Résilience et Gestion des Échecs

```
[Échec Détecté sur CLI Actuel (ex: CodeBuddy Timeout)]
                    │
                    ▼
       [Vérification Fallback Disponible ?]
         ├── OUI ──► Réassignation automatique vers CLI alternatif (ex: OpenCode)
         │           OU sélection modèle gratuit (ex: qwen3-coder-plus:free)
         │           Statut tâche réinitialisé à READY
         │           Nouvel essai sans faire échouer la mission.
         │
         └── NON ──► [La tâche est-elle obligatoire ?]
                       ├── OUI ──► Transition Tâche FAILED
                       │           Transition Mission FAILED avec diagnostic complet.
                       └── NON ──► Transition Tâche SKIPPED
                                   Replanification du DAG
                                   Poursuite de la Mission vers COMPLETED.
```
