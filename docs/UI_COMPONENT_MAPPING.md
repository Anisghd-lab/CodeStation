# Cartographie des Composants UI : Codync vs Synapse

Date : 27 Octobre 2026  
Projet : **Synapse** (`com.ghdinteractivestudio.synapse`)  
Référence : `/root/Codyn/apps/android`  

---

## 1. Vue d'Ensemble de l'Architecture

La structure de l'application Synapse est organisée de manière modulaire, propre et lisible, tout en reproduisant fidèlement les concepts visuels et les composants du dépôt Android officiel de Codync.

```
Codync (apps/android/)                           Synapse (app/src/main/kotlin/.../synapse/)
├── design/src/main/kotlin/dev/codync/design/    ├── ui/theme/ & ui/components/
│   ├── theme/Palette.kt                   ───>  │   ├── Palette.kt (Tokens monochromes purs)
│   ├── theme/Type.kt                      ───>  │   ├── Type.kt (Inter variable & dynamic tracking)
│   ├── theme/Motion.kt                    ───>  │   ├── Motion.kt (Easing curves & press scale)
│   ├── theme/Shape.kt                     ───>  │   ├── Shape.kt (Continuous smoothing & capsules)
│   ├── components/CharacterAvatar.kt      ───>  │   ├── CharacterAvatar.kt (8 formes 3D & regard)
│   ├── components/ThinkingOrb.kt          ───>  │   ├── ThinkingOrb.kt + ThinkingOrbGeometry.kt
│   ├── components/Controls.kt             ───>  │   ├── Controls.kt (Boutons, Diff, Codes, Toggles)
│   ├── components/Chrome.kt               ───>  │   ├── Chrome.kt (Headers, BackButton, TabBar)
│   ├── components/Glass.kt                ───>  │   ├── Glass.kt (Stage, ScrollEdge, GlassModifier)
│   └── symbols/SFDrawables                ───>  │   └── 172 Drawables sf_*.xml dans res/drawable/
│
├── app/src/main/kotlin/dev/codync/app/ui/       ├── ui/screens/ & viewmodel/
│   ├── RosterView.kt                      ───>  │   ├── RosterScreen.kt + RosterViewModel.kt
│   ├── ChatView.kt                        ───>  │   ├── ChatScreen.kt + ChatViewModel.kt
│   ├── PairingView.kt                     ───>  │   ├── PairingScreen.kt + PairingViewModel.kt
│   ├── TraceView.kt                       ───>  │   ├── TraceScreen.kt + TraceViewModel.kt
│   ├── BotEditorView.kt                   ───>  │   ├── BotEditorScreen.kt + BotEditorViewModel.kt
│   ├── GroupEditorView.kt                 ───>  │   ├── GroupEditorScreen.kt + GroupEditorViewModel.kt
│   ├── SettingsView.kt                    ───>  │   ├── SettingsScreen.kt + SettingsViewModel.kt
│   ├── ApprovalsView.kt                   ───>  │   ├── ApprovalsScreen.kt + ApprovalsViewModel.kt
│   ├── OnboardingView.kt                  ───>  │   ├── OnboardingScreen.kt
│   └── States.kt                          ───>  │   └── StateScreens.kt
│
└── core/src/main/kotlin/dev/codync/core/        ├── data/ & domain/
    ├── api/                               ───>  │   ├── remote/SynapseApiService.kt (Retrofit/SSE)
    ├── model/                             ───>  │   ├── domain/model/ (Bot, ChatMessage, Approval)
    └── repository/                        ───>  │   └── data/local/ (Room DAOs, ServerRepositoryImpl)
```

---

## 2. Table de Correspondance Détaillée

### 2.1. Thème et Fondations Graphiques

| Fichier Source Codync | Fichier Cible Synapse | Responsabilité & Spécificité |
|---|---|---|
| `design/.../theme/Palette.kt` | `ui/theme/Palette.kt` | Définition des couleurs monochromes (`0xFF0A0A0A`, `0xFF141414`, `0xFF1E1E1E`, `0xFF262626`), des 11 couleurs d'avatar et des statuts d'alerte. |
| `design/.../theme/Type.kt` | `ui/theme/Type.kt` | Configuration de la police variable `Inter.ttf`, calcul de l'espacement dynamique des lettres (tracking optique SF). |
| `design/.../theme/Motion.kt` | `ui/theme/Motion.kt` | Courbes d'accélération d'animation (`hover`, `fade`, `press`, `morph`, `conversation`), constante `PRESS_SCALE = 0.98f`. |
| `design/.../theme/Shape.kt` | `ui/theme/Shape.kt` | Formes à coins continus : `Capsule()`, `RoundedRectangle(radius)`, rayons de 6.dp à 28.dp. |
| `design/.../theme/Theme.kt` | `ui/theme/Theme.kt` | Intégration Compose `MaterialTheme`, `LocalDark`, gestion de la bascule sombre/clair. |

### 2.2. Composants Vectoriels & Visuels

| Fichier Source Codync | Fichier Cible Synapse | Responsabilité & Spécificité |
|---|---|---|
| `design/.../components/CharacterAvatar.kt` | `ui/components/CharacterAvatar.kt` | 8 formes d'avatar (`blob`, `pebble`, `squircle`, `tablet`, `wedge`, `hex`, `cloud`, `teardrop`), éclairage sphérique 3D et yeux animés clignotants. |
| `design/.../components/AvatarWithStatus.kt` | `ui/components/BotAvatar.kt` | Badges de statut : point ambre `needsInput`, badge d'agent Lead (étoile), point vert connecté, indicateur de non-lus. |
| `design/.../components/ThinkingOrb.kt` | `ui/components/ThinkingOrb.kt` + `ThinkingOrbGeometry.kt` | Orbe géométrique 3D en fil de fer avec projection matricielle sur Canvas et rotation continue fluide. |
| `design/.../components/Controls.kt` | `ui/components/Controls.kt` | `PrimaryButton`, `SecondaryButton`, `IconButton`, `LinkButton`, `CodyncSwitch`, `Spinner`, `SegmentedChoice`, `CardForm`, `CodeBox`, `DiffView`. |
| `design/.../components/Chrome.kt` | `ui/components/Chrome.kt` | `ScreenHeader`, `ModalHeader`, `BackButton`, `PageHeader`, `GlassTabBar` (barre de navigation flottante). |
| `design/.../components/Glass.kt` | `ui/components/Glass.kt` | `GlassStage` (conteneur bord-à-bord avec gestion du scroll), `ScrollEdge` (dégradé d'estompage). |
| `design/.../components/PermissionCard.kt` | `ui/components/ApprovalCard.kt` | Carte d'approbation d'exécution : affichage du diff, bloc commande shell, boutons "Autoriser une fois", "Toujours", "Refuser". |
| `design/.../components/MessageComposer.kt` | `ui/components/MessageComposer.kt` | Zone de saisie auto-extensible, raccourci clavier `Ctrl/Meta+Enter`, bannière de citation (`QuotedReply`), morphing de l'icône d'action. |
| `design/.../components/DrawnSymbols.kt` | `ui/components/DrawnSymbols.kt` | Tracés vectoriels purs sur Canvas pour les symboles de discussion (`Bubbles`) et de cartes (`CardStack`). |
| `design/.../symbols/SFDrawables` (172 drawables) | `res/drawable/sf_*.xml` + `ui/components/Symbols.kt` | Ensemble exhaustif des 172 icônes SF d'Apple converties en drawables vectoriels Android. |

### 2.3. Écrans Applicatifs (11 Écrans)

| Fichier Source Codync | Fichier Cible Synapse | Fonctionnalités Clés |
|---|---|---|
| `app/.../ui/RosterView.kt` | `ui/screens/RosterScreen.kt` | Vue principale : sélecteur de compte, badge d'état du serveur Termux, liste triée des bots (Lead > Épinglés > Récents), barre de navigation inférieure. |
| `app/.../ui/ChatView.kt` | `ui/screens/ChatScreen.kt` | Conversation : en-tête avec indicateur wifi temps réel, bulle de message neutre, ThinkingOrb pour le raisonnement, carte d'approbation intégrée, compositeur. |
| `app/.../ui/PairingView.kt` | `ui/screens/PairingScreen.kt` | Écran d'appairage : guide Termux/PRoot complet, détection de `http://127.0.0.1:19222`, saisie du jeton, prise en charge de l'URI `synapse://pair`. |
| `app/.../ui/TraceView.kt` | `ui/screens/TraceScreen.kt` | Inspection du raisonnement de l'agent : affichage pas-à-pas des étapes, logs Bash, pensées intermédiaires, chronologie des actions. |
| `app/.../ui/ApprovalsView.kt` | `ui/screens/ApprovalsScreen.kt` | Liste dédiée des requêtes d'approbation en attente sur l'ensemble des agents avec visualisation du code/diff. |
| `app/.../ui/BotEditorView.kt` | `ui/screens/BotEditorScreen.kt` | Éditeur d'agent : réglage du nom, sélecteur de forme et de couleur d'avatar, modèle de langage (Claude, OpenAI, Local), permissions shell. |
| `app/.../ui/GroupEditorView.kt` | `ui/screens/GroupEditorScreen.kt` | Création de groupe d'agents : désignation du bot coordinateur (Lead) et sélection des agents subordonnés. |
| `app/.../ui/SettingsView.kt` | `ui/screens/SettingsScreen.kt` | Paramètres généraux : URL du serveur local Termux, statut de la connexion, thème sombre/clair, nettoyage du cache, mention de licence Apache-2.0. |
| `app/.../ui/OnboardingView.kt` | `ui/screens/OnboardingScreen.kt` | Écran de bienvenue guidant l'utilisateur sur le concept d'agents autonomes locaux sur Android. |
| `app/.../ui/States.kt` | `ui/screens/StateScreens.kt` | Écrans d'états d'erreur, hors-ligne (serveur Termux non démarré), chargement et listes vides. |

### 2.4. Couche Réseau & Persistance

| Composant Codync | Composant Synapse | Rôle |
|---|---|---|
| Client HTTP SSE | `data/remote/SynapseApiService.kt` | Définition des endpoints réels : `GET /health`, `POST /api/hello`, `POST /api/send`, `POST /api/stop`, `POST /api/respondPermission`, `GET /events?since=0`. |
| Dependency Container | `SynapseContainer.kt` | Initialisation manuelle des dépendances (Retrofit + OkHttp avec timeout adaptés à Termux, Room Database, Repositories). |
| Configuration Hôte | `data/local/ServerRepositoryImpl.kt` | Gestion du serveur local loopback `http://127.0.0.1:19222`, parsing de l'URL d'appairage, mesure de latence réelle par ping HTTP `/health`. |
| Discussion & Approbations | `data/local/ChatRepositoryImpl.kt` | Persistance Room des messages (`ChatMessageEntity`), des requêtes d'approbation (`ApprovalEntity`) et synchronisation réseau bidirectionnelle. |
| Catalogue de Bots | `data/local/BotRepositoryImpl.kt` | Persistance Room des agents et groupes (`BotEntity`), gestion du statut (IDLE, WORKING, NEEDS_INPUT, OFFLINE). |
| Préférences | `data/preferences/PreferencesRepositoryImpl.kt` | Persistance DataStore de l'URL du serveur, du jeton de sécurité et des préférences utilisateur. |
