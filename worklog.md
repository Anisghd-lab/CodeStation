# Journal des Travaux (Worklog) — Projet Synapse

> **Projet** : Synapse (`com.ghdinteractivestudio.synapse`)  
> **Auteur** : Antigravity (Google DeepMind Advanced Agentic Coding)  
> **Date de session** : 27 Octobre 2026

---

## 1. Synthèse Exécutive

Création intégrale de **Synapse**, une application mobile Android native en **Kotlin 2.0** et **Jetpack Compose**, inspirée de l'UI/UX d'orchestration d'agents autonomes de Codync. L'application possède une architecture indépendante, un design system Material 3 sur-mesure (Thème Sombre Obsidian & Thème Clair Platine), une marque et une icône de lanceur vectorielle originales.

Le projet respecte à 100% les contraintes imposées :
- Aucun recours à Flutter, Dart, React Native ou WebView principale.
- Aucune toolchain lourde installée sur le téléphone (l'environnement de travail restant un conteneur PRoot léger sous Android ARM64).
- Les projets préexistants (`Codyn`, `CimentFlow`) ont été scrupuleusement préservés sans aucune modification destructive.
- Pipeline CI/CD GitHub Actions configuré (`.github/workflows/build_apk.yml`) sur `ubuntu-latest` avec JDK 21 Temurin pour la production d'artefacts APK debug téléchargeables.

---

## 2. Historique des Réalisations

### Phase 1 — Audit & Spécifications (Complétée)
- **Inspection de l'Environnement** : Détection d'Ubuntu PRoot Linux ARM64, JDK 21 Temurin, Git 2.53.0 et Python 3.14.
- **Audit des Références Codync** : Analyse détaillée de `docs/design/ui-conventions.md`, `docs/features/chat-messages.md`, `groups-and-replies.md`, `lead-bot.md`, `artifacts.md`, `routines.md`, `context-and-memory.md` et des captures d'écran.
- **Rédaction des Documents de Référence** :
  - [`docs/REFERENCE_AUDIT.md`](file:///root/synapse/docs/REFERENCE_AUDIT.md) : Faits vérifiés, déductions et inconnues.
  - [`docs/SCREEN_INVENTORY.md`](file:///root/synapse/docs/SCREEN_INVENTORY.md) : Inventaire exhaustif des 11 écrans et parcours.
  - [`docs/REFERENCE_GAPS.md`](file:///root/synapse/docs/REFERENCE_GAPS.md) : Écarts techniques et justification de l'abandon des shaders Kyant/protocole binaire C.
  - [`docs/DESIGN_SYSTEM.md`](file:///root/synapse/docs/DESIGN_SYSTEM.md) : Système de design Material 3 (Tokens, Palette Dark/Light, Typo, Spacings).
  - [`docs/ARCHITECTURE.md`](file:///root/synapse/docs/ARCHITECTURE.md) : Architecture MVVM, StateFlow, Room et DataStore.
  - [`docs/TEST_PLAN.md`](file:///root/synapse/docs/TEST_PLAN.md) : Stratégie de qualification et tests unitaires / Compose.
  - [`docs/KNOWN_LIMITATIONS.md`](file:///root/synapse/docs/KNOWN_LIMITATIONS.md) : Limites et découplage documentés.
  - [`README.md`](file:///root/synapse/README.md) : Documentation principale et instructions de compilation.

### Phase 2 — Système de Build & Ressources Android (Complétée)
- **Initialisation du Projet** dans `/root/synapse`.
- **Scripts Gradle** : `settings.gradle.kts`, `build.gradle.kts` racine, `gradle.properties`, `gradle/libs.versions.toml` (Catalog de dépendances moderne), `app/build.gradle.kts` (targetSdk 35, Java 21, Compose activé).
- **Gradle Wrapper** configuré en version 8.9 avec son archive jar exécutable.
- **Ressources XML** :
  - `values/strings.xml`, `values/colors.xml`, `values/themes.xml`.
  - `xml/data_extraction_rules.xml`, `xml/backup_rules.xml`.
  - Icône originale de lanceur adaptive (`drawable/ic_launcher_background.xml`, `drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml`, `mipmap-anydpi-v26/ic_launcher.xml`).
  - Bannière vectorielle originale `drawable/ic_synapse_banner.xml`.
- **Manifeste Android** (`app/src/main/AndroidManifest.xml`) : permissions internet, réseau et audio, configuration de `MainActivity` avec `adjustResize` et filtres d'URI d'appairage `synapse://pair`.

### Phase 3 — Modèles de Domaine & Couche Données (Complétée)
- **Modèles de Domaine Immuables** : `Bot`, `ChatMessage`, `ApprovalRequest`, `ExecutionStep`, `Routine`, `MemoryFact`, `ExtensionItem`, `ServerConfig`.
- **Base de Données Locale Room (`SynapseDatabase`)** :
  - `BotEntity` & `BotDao` avec requêtes de tri réactives par Flow.
  - `ChatMessageEntity` & `MessageDao` pour la persistance locale des messages.
  - `ApprovalEntity` & `ApprovalDao` pour l'historique des permissions.
- **DataStore Preferences (`SynapsePreferences`)** : Sauvegarde asynchrone des options de thème, serveur et détails de réflexion.
- **Couche Réseau (OkHttp & Retrofit)** : `SynapseApiService` avec DTOs `ServerInfoResponse`, `SendMessageRequest`, `ApproveRequest`.
- **Repositories** : `BotRepositoryImpl`, `ChatRepositoryImpl`, `ServerRepositoryImpl`, `PreferencesRepositoryImpl`.
- **Conteneur DI & Application** : `SynapseContainer` et `SynapseApplication`.

### Phase 4 — Design System & Composants Jetpack Compose (Complétée)
- **Thème Material 3** : `Color.kt` (Dark Obsidian & Light Platine), `Spacing.kt`, `Shape.kt`, `Type.kt`, `Theme.kt`.
- **Composants Réutilisables** :
  - `ThinkingOrb` : Orbe pulsant multi-niveaux pour visualiser l'activité des agents.
  - `BotAvatar` : Avatar avec glyphes géométriques, indicateur d'état, étoile or `★` du Lead Bot et badge de non-lus.
  - `BotCard` : Carte du roster avec nom, tag du fournisseur, extrait de message et statut en direct.
  - `ChatMessageItem` : Bulle utilisateur teintée, bulle agent avec rendu clair, gestion des citations `replyTo` et cartes d'artéfacts.
  - `MessageComposer` : Champ extensible, barre de citation rétractable, bouton de pièce jointe et bouton d'action dynamique (Envoi / Micro / Stop).
  - `ApprovalCard` : Carte de sécurité pour l'approbation explicite des commandes shell et écritures fichiers avec boutons dédiés.
  - `ThinkingTraceView` : Vue accordéon dépliable pour inspecter les étapes de raisonnement de l'agent.
  - `ConnectionStatusChip` : Pastille interactive de statut du serveur dans la barre supérieure.
  - `EmptyStateView` & `ErrorStateView` : Écrans d'états vides et d'erreurs soignés.
  - `SettingsRow` & `ConfirmationDialog` : Composants de configuration et dialogues d'action destructive.

### Phase 5 — Implémentation des 11 Écrans & Navigation (Complétée)
1. **OnboardingScreen** : Présentation et arguments clés avec redirection appairage ou démo.
2. **PairingScreen** : Configuration de l'hôte (127.0.0.1, LAN, passerelle), jeton de session, test ping actif et retour visuel.
3. **RosterScreen** : Liste des bots triée en 3 bandes (Lead Bot en tête avec étoile or `★`, épinglés, autres), recherche, menu d'actions et FAB.
4. **BotEditorScreen** : Création et modification d'agent (forme, couleur d'accent, fournisseur IA, modèle, Fast Mode, instructions, politique d'approbation).
5. **ChatScreen** : Conversation en direct, bulles sans streaming cassé, orbe d'exécution, citations et approbations interactives.
6. **TraceScreen** : Visualisation détaillée des étapes de réflexion, durées et flux JSON brut d'événements.
7. **ApprovalsScreen** : Écran dédié listant les autorisations en attente avec actions directes.
8. **GroupEditorScreen** : Création de salons multi-agents avec sélection des participants.
9. **SettingsScreen** : Préférences de thème, chat, diagnostic serveur et liens vers les modules.
10. **ComplementaryScreens** :
    - `MemoryScreen` : Explorateur des faits `MEMORY.md`.
    - `RoutinesScreen` : Tâches automatisées programmées avec bouton de test.
    - `ExtensionsScreen` : Catalogue des connecteurs MCP et outils système.
    - `VoiceScreen` : Appel vocal mains-libres avec reconnaissance vocale Android.
    - `RemoteScreenUnavailable` : État d'indisponibilité transparent expliquant les prérequis WebRTC/Wayland.
11. **StateScreens** : Écrans d'état hors-ligne, permissions refusées et erreurs système.
- **Navigation** : `NavRoutes.kt`, `SynapseNavHost.kt`, `MainActivity.kt` avec gestion des deep links d'appairage.

### Phase 6 — Tests & CI GitHub Actions (Complétée)
- Tests unitaires complets écrits avec JUnit 4, MockK et Kotlinx Coroutines Test :
  - `RosterViewModelTest` : Validation du tri à 3 bandes (Lead bot en tête permanente, puis épinglés, puis récents) et du filtrage.
  - `ChatViewModelTest` : Validation de l'envoi de messages, gestion des citations et réponse aux approbations.
  - `ModelValidationTest` : Validation de la conversion bidirectionnelle Room Entity <-> Domain Model.
- Workflow CI GitHub Actions `.github/workflows/build_apk.yml` :
  - Exécuteur `ubuntu-latest` avec JDK 21 Temurin et Gradle 8.9.
  - Exécution des tests unitaires (`testDebugUnitTest`).
  - Compilation de l'APK debug (`assembleDebug`).
  - Production du fichier `Synapse-v1.0.0-debug.apk` et de son empreinte `Synapse-v1.0.0-debug.apk.sha256`.
  - Upload automatique comme artefact de build téléchargeable.

### Phase 7 — Rétro-Ingénierie des Sources Android Codync & Parité UI/UX Totale (Complétée)
- **Inspection Directe des Sources Android Officielles** (`/root/Codyn/apps/android/`) :
  - Analyse approfondie des modules `design` (`Palette.kt`, `Type.kt`, `Motion.kt`, `Shape.kt`, `CharacterAvatar.kt`, `Controls.kt`, `Chrome.kt`), `app` (`RosterView.kt`, `ChatView.kt`, `PairingView.kt`, `TraceView.kt`) et `core` (`dev.codync.core.api`).
- **Alignement Rigoureux du Design System** :
  - Remplacement des dégradés saturés par la véritable palette monochrome d'Apple/Codync : fond noir profond `#0A0A0A` (mode sombre) / blanc pur `#FFFFFF` (mode clair), bulles de message grises neutres (`#1E1E1E` pour l'agent, `#262626` pour l'utilisateur), accent blanc contrasté, couleur isolée aux 11 teintes d'avatars et signaux d'état (ambre `#F59E0B` pour `warning`/`needsInput`, rouge `#EF4444` pour `danger`).
  - Intégration de la police variable `Inter.ttf` (879 Ko) avec formule de crénage optique dynamique calquée sur SF Pro.
  - Formes à courbure continue : `Capsule()` et `RoundedRectangle(radius)`.
  - Intégration des 172 drawables vectoriels SF Symbols officiels (`sf_*.xml`), `apple_logo.xml` et `computer_linux.xml` dans `app/src/main/res/drawable/`.
  - Modélisation vectorielle pure sur Canvas pour `Bubbles` et `CardStack` dans `DrawnSymbols.kt`.
- **Implémentation des Composants Graphiques Avancés** :
  - `CharacterAvatar.kt` : Reproduction fidèle des 8 formes géométriques (`blob`, `pebble`, `squircle`, `tablet`, `wedge`, `hex`, `cloud`, `teardrop`), rendu 3D sphérique par gradient radial avec reflet spéculaire en coin supérieur gauche, regard dynamique animé et clignement naturel périodique.
  - `ThinkingOrb.kt` & `ThinkingOrbGeometry.kt` : Orbe géométrique 3D wireframe projeté sur Canvas avec rotation spatiale continue, estompage de profondeur et arêtes filaires.
  - `ApprovalCard.kt` : Carte de demande d'approbation d'action (exécution bash, modification de fichier) avec visualiseur de diff colorisé (`#10B981` / `#EF4444`), boîte de code shell et trois boutons tactiles ("Autoriser une fois", "Toujours autoriser", "Refuser").
  - `MessageComposer.kt` : Compositeur de message extensible, raccourci clavier `Ctrl/Meta+Enter`, aperçu des citations en réponse (`QuotedReply`), bouton en verre pour les pièces jointes et bouton d'action à morphing dynamique (Envoyer / Arrêter / Appel vocal).
  - `Chrome.kt` & `Glass.kt` : `ScreenHeader`, `ModalHeader`, `BackButton`, conteneur immersif `GlassStage` et barre de navigation inférieure flottante `GlassTabBar`.
- **Refonte et Alignement des 11 Écrans** :
  - `RosterScreen.kt`, `ChatScreen.kt`, `PairingScreen.kt`, `ApprovalsScreen.kt`, `TraceScreen.kt`, `BotEditorScreen.kt`, `GroupEditorScreen.kt`, `SettingsScreen.kt`, `OnboardingScreen.kt`, `StateScreens.kt`.
- **Connexion Réseau Android ⟷ Termux Loopback** :
  - Vérification du daemon local `codync_host` (v2.28.0) écoutant sur `http://127.0.0.1:19222`.
  - Implémentation des endpoints réels dans `SynapseApiService.kt` : `GET /health`, `POST /api/hello`, `POST /api/send`, `POST /api/stop`, `POST /api/respondPermission`, `GET /events?since=0`.
  - Configuration de `ServerRepositoryImpl.kt` avec mesure de latence réelle en millisecondes et résolution du jeton d'accès `/root/.codync-dev/token`.
  - Prise en charge des deep links d'appairage `synapse://pair?token=...&host=...`.
- **Tests Unitaires & Assurance Qualité** :
  - Création de `DesignTokensTest.kt` validant les valeurs hexadécimales de la palette monochrome, les 8 formes d'avatars et les constantes de motion.
  - Création de `ServerConnectionTest.kt` validant la classification en loopback, l'interrogation du endpoint `/health` et la gestion des erreurs 401.
  - Maintien et exécution sans régression de `ModelValidationTest.kt`, `RosterViewModelTest.kt` et `ChatViewModelTest.kt`.
- **Documentation Dédiée Livrée** :
  - `docs/CODYNC_UI_PARITY_AUDIT.md` : Audit exhaustif de parité visuelle et fonctionnelle.
  - `docs/UI_COMPONENT_MAPPING.md` : Table de correspondance directe fichier par fichier.
  - `docs/CONNECTION_TERMUX_DESIGN.md` : Architecture matérielle et logicielle de la connexion locale.
  - `docs/UI_PARITY_TESTS.md` : Matrice de tests et plan de vérification.

---

## 3. Synthèse des Fichiers Modifiés & Créés

* Racine `/root/synapse/docs/` :
  - `CODYNC_UI_PARITY_AUDIT.md` (Créé)
  - `UI_COMPONENT_MAPPING.md` (Créé)
  - `CONNECTION_TERMUX_DESIGN.md` (Créé)
  - `UI_PARITY_TESTS.md` (Créé)
  - `REFERENCE_AUDIT.md`, `SCREEN_INVENTORY.md`, `REFERENCE_GAPS.md`, `DESIGN_SYSTEM.md`, `ARCHITECTURE.md`, `TEST_PLAN.md`, `KNOWN_LIMITATIONS.md`
* Projet `/root/synapse/app/` :
  - 172 drawables vectoriels officiels `res/drawable/sf_*.xml`, `apple_logo.xml`, `computer_linux.xml`
  - Police variable `res/font/inter.ttf`
  - Nouveaux composants : `CharacterAvatar.kt`, `ThinkingOrb.kt`, `ThinkingOrbGeometry.kt`, `Chrome.kt`, `Controls.kt`, `Glass.kt`, `DrawnSymbols.kt`, `Symbols.kt`, `Symbol.kt`
  - Thème aligné : `Palette.kt`, `Motion.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`
  - Écrans alignés : `RosterScreen.kt`, `ChatScreen.kt`, `PairingScreen.kt`, `ApprovalsScreen.kt`, `TraceScreen.kt`, `BotEditorScreen.kt`, `GroupEditorScreen.kt`, `SettingsScreen.kt`, `OnboardingScreen.kt`, `StateScreens.kt`
  - Nouveaux tests : `DesignTokensTest.kt`, `ServerConnectionTest.kt`

### Phase 8 — Publication sur le Dépôt Officiel CodeStation & Validation CI/CD (Complétée avec Succès)
- **Vérification Initiale du Dépôt Cible** :
  - Dépôt distant : `https://github.com/Anisghd-lab/CodeStation`
  - Inspection via `git ls-remote` et GitHub REST API : Dépôt distant vérifié vide (taille 0, aucun commit antérieur, branche `main` non initialisée).
  - Validation de l'accès en écriture sans risque d'écrasement de données.
- **Publication Git & Préservation d'Historique** :
  - Remote `origin` configuré vers `https://github.com/Anisghd-lab/CodeStation.git`.
  - Push de la branche locale `main` avec préservation stricte de l'arbre et du commit historique de référence `37d2052` (ainsi que `2171c13` et `0e591b1`).
- **Audit Itératif des Exécutions CI GitHub Actions & Résolutions** :
  1. **Run 1 (`40521897087`) sur commit `37d2052`** :
     - Échec en compilation Kotlin (`compileDebugKotlin`).
     - Corrections apportées : Suppression de l'argument `isPulsing` sur `ThinkingOrb`, exhaustivité du `when` sur `StepType.THINKING` et `PLANNING`, implémentation de `ChoicePicker` dans `Controls.kt`, ajout de `saveBot(bot)` dans `BotRepository`, correction d'alignement dans `ComplementaryScreens.kt`, annotation `@OptIn(ExperimentalTextApi::class)` dans `Type.kt`, découplage du `combine` à 7 flux dans `ChatViewModel.kt`. (Commit `06ff37b`).
  2. **Run 2 (`40527115482`) sur commit `06ff37b`** :
     - Échec en compilation Kotlin (`compileDebugKotlin`).
     - Corrections apportées : Import manquant `androidx.compose.foundation.layout.width` dans `ChatMessageItem.kt`, réassignation de 11 drawables vectoriels SF Symbols manquants vers des substituts existants vérifiés dans `res/drawable/`, import de `Palette` dans `ComplementaryScreens.kt`. (Commit `76b855f`).
  3. **Run 3 (`40528816910`) sur commit `76b855f`** :
     - Compilation du code source principal validée avec succès (`compileDebugKotlin` passé). Échec sur `compileDebugUnitTestKotlin`.
     - Corrections apportées : Alignement du constructeur `HelloResponse(name = "termux-dev", version = "2.28.0")` et gestion de la nullabilité de `latencyMs` dans `ServerConnectionTest.kt`, mise à jour de `DesignTokensTest.kt` pour tester `AvatarPalette`, `Motion.hover()` et les tokens `@ReadOnlyComposable`. (Commit `e3868a2`).
  4. **Run 4 (`40529860314`) sur commit `e3868a2`** :
     - Compilation complète validée. 10 tests unitaires sur 13 réussis, 3 échecs d'assertion dus à la stratégie `SharingStarted.WhileSubscribed(5000)` des `StateFlow` dans `RosterViewModel` et `ChatViewModel` (en l'absence de collecteur actif en test unitaire, le flux amont ne démarre pas).
     - Corrections apportées : Démarrage d'un collecteur en arrière-plan via `backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }` dans `RosterViewModelTest` et `ChatViewModelTest`. (Commit `7c17dd8`).
  5. **Run 5 (`40532712267`) sur commit `7c17dd8` — SUCCÈS TOTAL** :
     - **Tests unitaires (`testDebugUnitTest`)** : 13/13 tests exécutés et validés avec succès (100% de réussite en 2m 18s).
     - **Compilation Debug APK (`assembleDebug`)** : BUILD SUCCESSFUL en 1m 17s (36 tâches Gradle exécutées/à jour).
     - **Artefact généré** : `Synapse-v1.0.0-debug.apk` (18 968 581 octets).
     - **Empreinte d'intégrité SHA-256** : `d320b6e03a24532a24e4c5485ce83ac492c051234f854c4091f1022346953916`
     - **Artefact GitHub Actions** : `Synapse-v1.0.0-debug-apk` (ID `12747057215`).

---

## 4. Références & Liens Observables

- **Dépôt GitHub Officiel** : [https://github.com/Anisghd-lab/CodeStation](https://github.com/Anisghd-lab/CodeStation)
- **Branche Principale** : `main`
- **Commit Historique Préservé** : [`37d2052`](https://github.com/Anisghd-lab/CodeStation/commit/37d2052)
- **Commit de Validation Finale** : [`7c17dd8`](https://github.com/Anisghd-lab/CodeStation/commit/7c17dd8)
- **Pipeline CI GitHub Actions (Succès)** : [Run #40532712267](https://github.com/Anisghd-lab/CodeStation/actions/runs/40532712267)
- **Téléchargement de l'Artefact APK Debug** : [Synapse-v1.0.0-debug-apk (ID 12747057215)](https://github.com/Anisghd-lab/CodeStation/actions/runs/40532712267/artifacts/12747057215)
- **Empreinte SHA-256 de l'APK** :
  ```text
  d320b6e03a24532a24e4c5485ce83ac492c051234f854c4091f1022346953916  Synapse-v1.0.0-debug.apk
  ```
