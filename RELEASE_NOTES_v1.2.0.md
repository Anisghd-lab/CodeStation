# CodeStation Release Notes v1.2.0 (Build 15)

### Architecture Multi-Agents Autonome & Enrichissement Festival

Cette version majeure marque l'intégration complète des meilleures innovations architecturales issues du framework Festival au sein du système multi-agents CAMCS de CodeStation, avec préservation stricte du Design System Glassmorphism.

---

### Nouvelles Fonctionnalités & Améliorations Majeures

1. **Intelligent DAG Planner (Ordonnancement Topologique de Kahn)** :
   * Implémentation mathématique de l'algorithme de Kahn avec calcul précis des degrés entrants (`in_degrees`).
   * Détection préventive et absolue des dépendances circulaires (`CycleDetectedException`).
   * Découpage et extraction automatique des vagues de tâches parallélisables (`get_parallel_waves()`).

2. **Système de Quality Gates Multi-Types & Acceptance Verifiers** :
   * Validation automatique des livrables selon 4 vérificateurs déterministes :
     * `COMMAND` : exécution de tests ou linters avec contrôle du code de retour.
     * `FILE_EXISTS` : vérification stricte de présence des fichiers créés.
     * `REGEX_MATCH` : validation de patterns et conformité de code source.
     * `CUSTOM_CHECK` : injection de callbacks Python personnalisés.
   * Journal d'audit persistant et horodaté stocké dans SQLite.

3. **Moteur de Reprise après Interruption & Réconciliation de Session** :
   * Sauvegarde transactionnelle et incrémentale de l'état d'avancement des missions (`checkpointing`).
   * Détection des pannes de processus ou d'arrêt inopiné du daemon via contrôle de `/proc/{pid}`.
   * Reprise automatique et sans duplication d'exécution (`RESUME_READY` et `RESTART_INTERRUPTED`).

4. **Boucle Auto-Itérative Corrective avec Escalade Humaine** :
   * Mécanisme de remédiation autonome (`AutoRepairLoop`) avec injection du journal d'erreur dans le contexte de prompt.
   * Plafond paramétrable de 3 tentatives avant bascule en escalade humaine (`NEEDS_APPROVAL`).
   * Nouvelles routes REST d'arbitrage manuel : `/api/camcs/tasks/approve` et `/api/camcs/tasks/reject`.

5. **Mémoire Perpétuelle des Décisions d'Architecture (ADR) & Expériences** :
   * Enregistrement systématique des décisions d'architecture (ADR) et des règles d'évitement d'erreurs après remédiation.
   * Synchronisation résiliente avec `CodeStation-Memory` (port 19224) et fallback direct sur disque.

6. **Enrichissement UI du Centre de Contrôle CAMCS (Glassmorphism)** :
   * Intégration de cartes de vérification des Quality Gates avec surfaces translucides (`frostedLiquidGlass()`), badges néon vert (`#00E676`) et rouge (`#FF5252`).
   * Carte contextuelle d'alerte et d'approbation d'escalade humaine lors des états `NEEDS_APPROVAL`.
   * Sanctification absolue des palettes Obsidian Dark, Platine Light, Cyan Électrique (`#00D2FF`) et Violet Astral (`#9D4EDD`).

---

### Fichiers de Téléchargement & Vérification
* `app-release.apk` : APK Android natif signé
* `CodeStation-v1.2.0.apk` : APK numéroté pour archivage direct
* `app-release.apk.sha256` : Empreinte de sécurité SHA-256
