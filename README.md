# Synapse — Native Android AI Agent Hub

<p align="center">
  <img src="synapse/app/src/main/res/drawable/ic_synapse_banner.xml" alt="Synapse Logo" width="120" height="120" />
</p>

> **Client Android natif en Kotlin et Jetpack Compose pour la gestion, l'orchestration et la communication bidirectionnelle avec des agents IA autonomes (Claude Code, OpenAI Codex, Google Gemini CLI, OpenCode, agents ACP).**

---

## ⚡ Caractéristiques Principales

* **100% Android Natif** : Développé exclusivement en **Kotlin 2.0** et **Jetpack Compose**, avec un design system Material 3 entièrement personnalisé (Thème Sombre Obsidian & Thème Clair Platine).
* **Zéro Flutter, Zéro WebView Principale** : Interface fluide, performante, réactive aux insets système (`WindowInsets`) et au redimensionnement de clavier (`adjustResize`).
* **Architecture MVVM Réactive & Découplée** :
  - `StateFlow` immuable pour tous les états d'interface.
  - Séparation stricte des couches (Présentation, ViewModels, Modèles de domaine, Repositories).
  - Cache local persistant via **Room Database** (SQLite) et **DataStore Preferences**.
  - Client réseau standardisé via **OkHttp** et **Retrofit**.
* **Expérience Complète Inspirée de l'UI/UX d'Orchestration d'Agents** :
  - **Roster Hiérarchisé** : Lead Bot en tête permanente avec badge étoile or `★`, bots épinglés et bots actifs avec indicateurs d'activité pulsants.
  - **Conversation Sans Streaming Destructif** : Réponses complètes livrées en une fois, pilule d'activité avec orbe animé pendant l'exécution.
  - **Transparence d'Exécution (*Thinking Details*)** : Dépliage des étapes de réflexion, commandes shell exécutées et sorties d'outils.
  - **Approbations de Sécurité Explicites** : Cartes d'approbation claires pour les commandes sensibles (Approuver une fois, Toujours approuver, Rejeter).
  - **Salons Multi-Bots** : Groupes de discussion collaboratifs avec gestion des mentions et des citations de réponse (`replyTo`).
  - **Cartes d'Artéfacts & Fichiers** : Aperçu élégant des documents et rapports HTML générés par les agents.
  - **États d'Erreur & Hors-Ligne Robustes** : Gestion des pannes de connexion, compte à rebours de reconnexion et états vides soignés.

---

## 📱 Structure du Projet

```
synapse/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/ghdinteractivestudio/synapse/
│       │   │   ├── SynapseApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/ (Room Database, DAOs, Entités)
│       │   │   │   ├── preferences/ (DataStore)
│       │   │   │   └── remote/ (Retrofit, OkHttp, WebSocket)
│       │   │   ├── domain/
│       │   │   │   ├── model/ (Bot, ChatMessage, ApprovalRequest...)
│       │   │   │   └── repository/ (Interfaces métier)
│       │   │   ├── ui/
│       │   │   │   ├── theme/ (Color, Typography, Theme, Spacings)
│       │   │   │   ├── components/ (BotCard, BotAvatar, MessageComposer...)
│       │   │   │   ├── navigation/ (NavHost, Routes)
│       │   │   │   └── screens/ (Roster, Chat, Editor, Pairing...)
│       │   │   └── viewmodel/ (RosterViewModel, ChatViewModel...)
│       │   └── res/ (Icônes vectorielles adaptatives, strings, styles)
│       └── test/ (Tests unitaires JUnit, MockK, CoroutinesTest)
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── .github/workflows/
│   └── build_apk.yml (Pipeline CI GitHub Actions)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🛠️ Stack Technique

| Composant | Version & Technologie |
|---|---|
| **Langage** | Kotlin 2.0.20 |
| **UI Toolkit** | Jetpack Compose (BOM 2024.09.00) |
| **Composants** | Material 3 (1.3.0) personnalisé |
| **Navigation** | Navigation Compose (2.8.0) |
| **Gestion d'État** | ViewModel Lifecycle 2.8.4 + Kotlin Coroutines & Flow |
| **Base Locale** | Room Database 2.6.1 |
| **Préférences** | AndroidX DataStore Preferences 1.1.1 |
| **Réseau** | OkHttp 4.12.0 + Retrofit 2.11.0 + Kotlinx Serialization 1.7.1 |
| **Images** | Coil Compose 2.7.0 |
| **Tests** | JUnit 4, Kotlinx Coroutines Test, Turbine |

---

## 🚀 Compilation & Déploiement

### Compilation Locale (Machine avec Android SDK & JDK 21)
```bash
cd synapse
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
L'APK se trouve ensuite dans `app/build/outputs/apk/debug/app-debug.apk`.

### Compilation Automatisée via GitHub Actions
Le dépôt contient le workflow `.github/workflows/build_apk.yml`. À chaque push sur la branche principale :
1. Les vérifications de code et tests unitaires sont exécutés.
2. L'APK de debug est automatiquement compilé.
3. Le fichier APK est publié et téléchargeable dans les **Artifacts** de la release GitHub Actions.

---

## 🔒 Sécurité & Confidentialité

* **Zéro Secret dans le Dépôt** : Aucun mot de passe ni clé d'API en clair.
* **Contrôle d'Exécution Total** : Les commandes shell exécutées par les agents IA doivent être validées manuellement par l'utilisateur via les cartes d'approbation.
* **Stockage Privé** : Toutes les données d'appairage et historiques sont stockés dans le bac à sable applicatif Android sécurisé.

---

## 📜 Documentation Complète
* [Audit des Références](docs/REFERENCE_AUDIT.md)
* [Inventaire des Écrans](docs/SCREEN_INVENTORY.md)
* [Écarts de Référence & Décisions](docs/REFERENCE_GAPS.md)
* [Système de Design](docs/DESIGN_SYSTEM.md)
* [Architecture Technique](docs/ARCHITECTURE.md)
* [Plan de Test](docs/TEST_PLAN.md)
* [Limites Connues](docs/KNOWN_LIMITATIONS.md)
* [Journal des Travaux (Worklog)](worklog.md)

---

## ⚖️ Licence
Projet sous licence Apache 2.0. Identité et marque Synapse © 2026 GHD Interactive Studio.
