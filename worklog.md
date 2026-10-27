# Journal des Travaux (Worklog) — Projet Synapse

> **Projet** : Synapse (`com.ghdinteractivestudio.synapse`)  
> **Auteur** : Antigravity (Google DeepMind Advanced Agentic Coding)  
> **Démarrage** : 27 Octobre 2026

---

## Entrées Chronologiques

### 2026-10-27 — Étape 1 : Audit Initial & Analyse des Références (Phase 1)
* **Inspection du Système & de l'Environnement** :
  - Système d'exploitation : Ubuntu PRoot sous Android ARM64 dans Termux.
  - Détection de Java 21 Temurin (`21.0.12.1`), Git (`2.53.0`), Python 3.14.
  - Confirmation absolue de la contrainte : **ne pas installer de lourdes toolchains Android SDK sur le téléphone**.
  - Respect scrupuleux des projets existants (`Codyn`, `CimentFlow`) : aucune suppression, aucune modification destructive.
* **Audit des Références Codync** :
  - Étude exhaustive des spécifications d'interface (`ui-conventions.md`, `chat-messages.md`, `groups-and-replies.md`, `lead-bot.md`, `artifacts.md`, `routines.md`, `context-and-memory.md`, `voice-call.md`).
  - Analyse des captures d'écran réelles (`android-roster.png`, `android-account.png`, `desktop-macos.png`).
  - Délimitation rigoureuse des faits vérifiés, déductions et inconnues.
* **Rédaction des Documents de Spécification** :
  - `docs/REFERENCE_AUDIT.md` : Audit complet des références et de l'environnement.
  - `docs/SCREEN_INVENTORY.md` : Cartographie intégrale des 11 écrans et parcours fonctionnels.
  - `docs/REFERENCE_GAPS.md` : Justification technique des écarts et solutions Android natives.
  - `docs/DESIGN_SYSTEM.md` : Système de design centralisé Material 3 (Tokens, Palette Dark/Light, Typo, Spacing, Animations).
  - `docs/ARCHITECTURE.md` : Spécification d'architecture propre MVVM, UDF, Room, DataStore et client réseau.
  - `docs/TEST_PLAN.md` : Stratégie de qualification et tests unitaires / Compose.
  - `docs/KNOWN_LIMITATIONS.md` : Documentation transparente des limites techniques.
  - `README.md` : Présentation du projet et guide complet.

---

### Prochaines Étapes Planifiées
* Initialisation de la structure du projet Android dans `/root/synapse`.
* Configuration du Gradle Wrapper, `settings.gradle.kts`, `build.gradle.kts` et `libs.versions.toml`.
* Création des ressources graphiques, icônes adaptatives et thèmes Jetpack Compose.
* Implémentation des modèles de domaine, base de données Room et DataStore.
* Implémentation des composants Compose réutilisables (`BotCard`, `ChatMessage`, `ApprovalCard`...).
* Implémentation des 11 écrans et de la navigation.
* Écriture des tests unitaires et configuration du pipeline GitHub Actions (`build_apk.yml`).
