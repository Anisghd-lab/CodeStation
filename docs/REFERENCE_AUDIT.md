# REFERENCE_AUDIT.md — Audit des Références Codync & de l'Environnement

> **Projet** : Synapse — Client Mobile Android Natif pour Agents IA Autonomes  
> **Package ID / Application ID** : `com.ghdinteractivestudio.synapse`  
> **Auteur** : Antigravity (Google DeepMind Advanced Agentic Coding)  
> **Date de l'audit** : 27 Octobre 2026  
> **Statut** : Phase 1 Validée — Faits Vérifiés, Déductions & Inconnues

---

## 1. Audit de l'Environnement & Contraintes Système

### 1.1 Environnement d'Exécution Local
* **OS & Noyau** : Linux ARM64 sous environnement conteneurisé Ubuntu PRoot dans Termux Android.
* **Workspace Actif** : `/root`
* **Java SDK** : OpenJDK `21.0.12.1` LTS (Temurin-21.0.12.1+1, 64-Bit Server VM).
* **Git** : Version `2.53.0`, configuré avec l'identité `Anisghd-lab` (`M_ghediri@yahoo.com`).
* **Python** : Version `3.14.4`.
* **Contrainte Toolchain Téléphone** : Conformément aux directives absolues de l'utilisateur, **aucune toolchain Android lourde (Android SDK de plusieurs gigaoctets, émulateurs, NDK) ne doit être installée sur le téléphone**.
* **Rôle de CI/CD** : Antigravity génère l'intégralité du code source, des ressources, des scripts Gradle Wrapper, des tests unitaires et des configurations de build dans le dépôt. **GitHub Actions** assure la compilation sur machine Linux standard (`ubuntu-latest` avec Java 21 et Android SDK) et produit l'APK debug téléchargeable en artefact.

### 1.2 État des Répertoires & Projets Existants
* `/root/Codyn` (symlink `/root/CodeStation`) : Fork amont de Codync (`Anisghd-lab/CodeStation.git`), explorant une adaptation locale PRoot. Ce projet contient les documentations de référence, captures d'écran et spécifications UI/UX. **Ne pas écraser ni supprimer.**
* `/root/CimentFlow` : Application mobile Flutter interne existante (`com.ghdinteractivestudio.cimentflow`). **Ne pas modifier ni écraser.**
* `/root/docs` : Contient l'historique architectural d'anciens projets. Les nouveaux documents de référence et d'architecture de Synapse y sont ajoutés sans effacer les fichiers préexistants.
* **Nouvel Emplacement Projet** : `/root/synapse` — Dépôt et projet Android natif propre, modulaire et totalement indépendant.

---

## 2. Audit des Références Codync

### 2.1 Sources et Documents Consultés
Les références ont été analysées en lecture seule depuis `/root/Codyn/docs` et les spécifications amont :
1. `docs/design/ui-conventions.md` (280 lignes) : Conventions visuelles, hiérarchie de navigation, comportements de liste de bots, détails du chat, transitions d'état.
2. `docs/features/chat-messages.md` (119 lignes) : Format des bulles, absence de streaming destructif (messages livrés complets), gestion des étapes de réflexion (*thinking details*), intégration des outils (*tools*), des artéfacts HTML et des pièces jointes.
3. `docs/features/groups-and-replies.md` (106 lignes) : Salons de discussion multi-bots, tours de parole (*room turns*), citations en réponse (*replyTo*), mentions `@bot`.
4. `docs/features/lead-bot.md` (59 lignes) : Rôle du bot principal épinglé au sommet (*Lead bot*) avec badge étoile or `★`, routines de check-in proactif.
5. `docs/features/artifacts.md` (97 lignes) : Documents HTML autonomes publiés par les agents (tableaux, rapports, dashboards), cartes d'artéfacts et visualiseur dédié.
6. `docs/features/routines.md` (309 lignes) : Tâches automatisées récurrentes (Cron, planification horaire/journalière) et webhooks déclenchés par les bots.
7. `docs/features/context-and-memory.md` (219 lignes) : Mémoire épisodique et persistante (`MEMORY.md` et notes de contexte).
8. `docs/features/voice-call.md` (437 lignes) : Interface d'appel vocal mains-libres avec capsule flottante, onde sonore et synthèse vocale.
9. `docs/features/remote-screen.md` (82 lignes) : Flux vidéo et télécommande WebRTC (fonctionnalité spécifique aux agents avec capture d'écran d'ordinateur).
10. `docs/screenshots/` : Captures vérifiées `android-roster.png` (74 Ko), `android-account.png` (62 Ko), `desktop-macos.png` (168 Ko).

---

## 3. Faits Vérifiés vs Déductions vs Inconnues

### 3.1 Faits Vérifiés (Prouvés par le Code & la Documentation Amont)
| Élément | Preuve / Source | Description Vérifiée |
|---|---|---|
| **Structure du Roster** | `ui-conventions.md:13`, `lead-bot.md:16` | Le roster est trié en 3 bandes strictes : 1. Bot principal (*Lead Bot* avec étoile `★`), 2. Bots épinglés (*Pinned*), 3. Reste des bots par ordre de récence. |
| **Bouton d'Action Vocal** | `ui-conventions.md:5`, `voice-call.md:3` | Le bouton d'appel vocal s'affiche dans le champ de saisie sous forme de capsule ondulée (`waveform`) **uniquement lorsque le champ texte est vide**. Quand du texte est saisi, il cède la place au bouton d'envoi. |
| **Non-streaming du Chat** | `chat-messages.md:13-43` | Les réponses des agents n'apparaissent pas mot par mot en sautant dans l'écran : le message final arrive complet en une seule bulle, tandis que le travail en cours est matérialisé par une pilule d'activité animée. |
| **Détails de Réflexion (Thinking)** | `chat-messages.md:65-85` | Une pilule animée (*Working pill*) affiche le statut en direct. Si l'option *Show thinking details* est activée, un appui déplie les étapes (pensée, appel d'outil MCP, sorties de commandes). |
| **Citations en Réponse** | `groups-and-replies.md:23-28` | Une réponse (`replyTo`) attache l'identifiant du message cité, son auteur et un extrait de 500 octets max. Une barre de citation s'affiche au-dessus du champ de saisie avant l'envoi. |
| **Cartes d'Approbation** | `ui-conventions.md:13`, `chat-messages.md:42` | Les permissions d'exécution de commandes sensibles ou d'accès fichier sont rendues sous forme de cartes d'approbation explicites avec options (Approuver, Rejeter) et indicateur de chargement. |
| **Cartes d'Artéfacts** | `artifacts.md:25-33` | Une carte d'artéfact présente le titre, l'horodatage et un aperçu du document HTML, ouvrant un visualiseur dédié. |
| **Identité Android Amont** | `apps/android/app/build.gradle.kts` | L'implémentation Android amont utilisait `com.codync.android`, des shaders Kyant (non standards) et était fortement couplée au code Rust de Codync. |

### 3.2 Déductions Raisonnées
* **Design Material 3 Personnalisé** : L'interface amont utilise des concepts visuels inspirés d'iOS (Liquid Glass, flous de scroll-edge, Kyant backdrop). Sur Android moderne standard, ces effets doivent être réinterprétés via Material 3 avec des surfaces surélevées (`surfaceContainer`, `surfaceContainerHigh`), des contours subtils (`outlineVariant`), des dégradés néon et des ombres douces, sans dépendre de bibliothèques C++ ou de shaders RenderEffect incompatibles avec les GPU mobiles d'entrée de gamme.
* **Architecture Modulaire Propre** : Plutôt que le modèle monolithique de Codync (`AppStore` géant de 3 000 lignes gérant tout l'état global dans un seul fichier), une architecture MVVM moderne avec `StateFlow`, Repositories isolés, `RoomDatabase` pour la persistance locale et `DataStore` pour les préférences offre une robustesse et une testabilité infiniment supérieures.

### 3.3 Inconnues & Limites Identifiées
* **Protocole Chiffré E2E Propriétaire Codync** : Le protocole complet Codync utilise un canal complexe ChaCha20-Poly1305 avec échange X25519 spécifique au binaire hôte Rust. Pour une application indépendante, l'architecture réseau doit reposer sur un client HTTP/WebSocket standard (OkHttp/Retrofit) communiquant avec une API REST/WebSocket pour agents IA (compatible serveur hôte local Termux ou passerelle d'agents).
* **Moteur Audio WebRTC / Appel Vocal** : L'implémentation complète de WebRTC natif pour la voix et l'écran distant requiert des binaires natifs NDK lourds (libwebrtc). En accord avec la consigne ("afficher un état indisponible honnête si non pris en charge"), les modules Voix et Écran distant proposeront une interface interactive complète avec reconnaissance vocale Android standard (`SpeechRecognizer`) et synthèse vocale (`TextToSpeech`), tout en indiquant honnêtement l'état des liaisons réseau avancées non supportées.

---

## 4. Identité de Marque Originale : Synapse

Afin de respecter scrupuleusement la consigne « Ne copie pas le code propriétaire, le nom, le logo ou les ressources de marque de Codync. Crée une marque et une icône originales » :

* **Nom de marque** : **Synapse** (Synapse AI Studio)
* **Package ID** : `com.ghdinteractivestudio.synapse`
* **Slogan** : *Autonomous Agent Hub & Neural Command Center*
* **Identité Graphique** :
  - Palette Dark : Fond Obsidian (`#0A0E17`), Cartes Élevées (`#131926`, `#1C2436`), Accent Primaire Cyan Électrique (`#00D2FF`), Accent Secondaire Violet Profond (`#7928CA`), Accent Alerte (`#FF4D4D`), Accent Succès (`#00E676`).
  - Palette Light : Fond Platine (`#F8FAFC`), Cartes (`#FFFFFF`, `#F1F5F9`), Accent Indigo (`#4F46E5`), Accent Teal (`#0D9488`).
* **Icône de Lanceur Originale** :
  - Représente la convergence de connexions neuronales autour d'un noyau central rayonnant.
  - Déclinée en ressources XML vectorielles natives (`ic_launcher_background.xml`, `ic_launcher_foreground.xml` et `ic_launcher_monochrome.xml` pour les icônes thématiques Android 13+).
