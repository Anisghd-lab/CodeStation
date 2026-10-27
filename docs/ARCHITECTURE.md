# ARCHITECTURE.md — Architecture Technique de Synapse

> **Package ID** : `com.ghdinteractivestudio.synapse`  
> **Patron d'Architecture** : Clean Architecture & MVVM Réactif (Model-View-ViewModel)  
> **Asynchronisme** : Kotlin Coroutines & `StateFlow` Immuable  
> **Date** : 27 Octobre 2026

---

## 1. Principes Fondamentaux de Conception

L'architecture de **Synapse** a été conçue pour garantir l'indépendance technologique, la robustesse face aux déconnexions réseau et une testabilité unitaire totale :

1. **Séparation Stricte des Responsabilités** : L'interface utilisateur (Composables Jetpack Compose) n'exécute **aucun** appel réseau ni aucune requête de base de données directe. L'UI observe uniquement des flux d'états immuables (`StateFlow`) exposés par les `ViewModels`.
2. **Flux de Données Unidirectionnel (UDF)** :
   ```
   [ Action Utilisateur ] ──> [ ViewModel ] ──> [ Repository ] ──> [ Network / Room ]
           ▲                                                              │
           │                                                              ▼
   [ Recomposition UI ] <── [ StateFlow<UiState> ] <── [ Modèle Métier / LocalStore ]
   ```
3. **Résilience Réseau & Local-First** : Toutes les entités principales (Bots, Messages, Décisions d'approbation) sont mises en cache dans une base de données locale **Room** (SQLite). L'application s'affiche instantanément même en l'absence de réseau ou lors de reconnexions.
4. **Injection de Dépendances Claire & Légère** : Injection par constructeur assistée par un conteneur d'injection de dépendances applicatif simple et robuste (`SynapseAppContainer`), évitant la complexité superflue et les temps de build excessifs de KAPT/KSP Hilt dans un environnement CI léger.

---

## 2. Découpage en Couches

```mermaid
graph TD
    subgraph UI ["Couche Présentation (Jetpack Compose)"]
        Screens[Écrans: Roster, Chat, Editor, Settings...]
        Components[Composants: BotCard, MessageComposer, ApprovalCard...]
        VM[ViewModels: RosterViewModel, ChatViewModel, PairingViewModel...]
    end

    subgraph Domain ["Couche Domaine & Métier"]
        Models[Modèles Immuables: Bot, ChatMessage, ApprovalRequest, Routine...]
        RepoInterfaces[Interfaces de Repository: BotRepository, ChatRepository...]
    end

    subgraph Data ["Couche Données & Infrastructure"]
        RepoImpl[Implémentations: BotRepositoryImpl, ChatRepositoryImpl...]
        LocalData[Room Database: SynapseDatabase, BotDao, MessageDao]
        Preferences[DataStore: SynapsePreferences (Serveur, Thème, Options)]
        Network[OkHttp + Retrofit: SynapseApiService & WebSocketManager]
    end

    Screens --> VM
    VM --> RepoInterfaces
    RepoImpl -.-> RepoInterfaces
    RepoImpl --> LocalData
    RepoImpl --> Preferences
    RepoImpl --> Network
```

---

## 3. Détail des Couches

### 3.1 Couche Présentation (UI & ViewModels)
* **Jetpack Compose** : Écrans déclaratifs découpés en fonctions composables atomiques et réutilisables.
* **Composants Dédiés** :
  - `BotCard` : Carte du roster avec avatar, statuts d'activité et badges.
  - `ChatMessageItem` : Bulle de message (utilisateur ou agent) avec rendu Markdown et blocs de code.
  - `MessageComposer` : Champ de saisie extensible avec bouton dynamique (envoi ou micro) et barre de citation.
  - `ApprovalCard` : Carte interactive pour valider ou rejeter les commandes d'outils sensibles.
  - `ThinkingTrace` : Vue accordéon présentant la réflexion et les appels d'outils.
* **ViewModels** :
  - `RosterViewModel` : Gère le tri des bots (Lead Bot, épinglés, récents), les filtres de recherche et l'état de synchronisation serveur.
  - `ChatViewModel` : Gère le fil de conversation en temps réel, l'envoi de messages, la citation, l'annulation de tour et les approbations.
  - `PairingViewModel` : Gère la configuration du serveur (URL, jeton), la vérification de connectivité et la persistance des préférences.
  - `BotEditorViewModel` : Gère la création/mise à jour d'un agent avec validation des entrées.

### 3.2 Couche Données (Data Layer)
* **Base de Données Locale Room (`SynapseDatabase`)** :
  - `BotEntity` & `BotDao` : Stockage local des agents (id, nom, avatarGlyph, avatarColor, provider, model, isLead, isPinned, status, lastMessage, updatedAt).
  - `ChatMessageEntity` & `MessageDao` : Stockage persistant des messages (id, botId, author, text, timestamp, isFinal, replyToId, replyToText, status).
  - `ApprovalEntity` & `ApprovalDao` : Historique des demandes de permissions et décisions d'approbation.
* **DataStore Preferences (`SynapsePreferences`)** :
  - `serverUrl` : Adresse de l'hôte (ex: `http://127.0.0.1:19222`).
  - `pairingToken` : Jeton d'autorisation sécurisé.
  - `selectedTheme` : Thème actif (Système, Sombre, Clair).
  - `showThinkingDetails` : Afficher ou non les étapes de réflexion par défaut.
* **Couche Réseau (OkHttp & Retrofit)** :
  - `SynapseApiService` : Interface REST pour interroger le serveur (`GET /api/info`, `GET /api/bots`, `POST /api/send`, `POST /api/approve`).
  - `SynapseWebSocketClient` : Gestionnaire de socket temps réel pour recevoir les flux d'événements (`bot_status`, `message_received`, `tool_approval_required`).
  - Intercepteur de sécurité : Injection automatique du jeton Bearer et gestion des délais d'expiration configurables.

---

## 4. Gestion de la Sécurité & Confidentialité

1. **Zéro Secret dans le Dépôt** : Aucune clé d'API, aucun mot de passe ni jeton en clair dans les fichiers de code ou de build.
2. **Protection des Données Locales** : Le jeton de session est sauvegardé dans `DataStore` privé à l'application (`Context.MODE_PRIVATE`).
3. **Approbation Explicite des Actions Destructives** : Les commandes shell potentiellement dangereuses requièrent impérativement un accord tactile explicite de l'utilisateur sur une `ApprovalCard`.
4. **Validation des Entrées** : Contrôle strict des URL saisies, limitation de la taille des messages et assainissement des identifiants d'agents.
