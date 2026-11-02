# Rapport de Réparation : Système Multi-Sessions et Nouvelles Discussions (New Discussion)

**Projet** : CodeStation (Android Native + Rust Host + CAMCS v2.0)  
**Date** : 2 novembre 2026  
**Auteur** : Lead Software Engineer  
**Statut** : Validé et Déployé (100%)

---

## 1. Contexte et Problématique Initiale

L'application souffrait de l'absence d'une architecture multi-conversations par agent (Problème B - New Discussion / New Session) :
- Tous les messages envoyés à un agent étaient mélangés dans un flux unique global indexé uniquement par `botId`.
- Impossible de créer une nouvelle conversation indépendante sans écraser ou continuer l'historique précédent.
- Les runners CLI (OpenCode, Antigravity, etc.) réutilisaient la même session ACP en arrière-plan, provoquant des fuites de mémoire et de contexte entre discussions traitant de sujets distincts.
- L'interface Android ne proposait aucun sélecteur de sessions, ni boîte de dialogue pour renommer ou supprimer une discussion spécifique.

---

## 2. Architecture Multi-Discussions Développée

Pour résoudre définitivement ce problème, une isolation complète sur l'ensemble de la pile a été implémentée :

```
[Android Jetpack Compose] 
       | (conversationId)
       v
[Room Database: conversations + chat_messages (conversationId)]
       |
       v (HTTP REST / WebSocket / SSE)
[Rust Host: /api/createConversation, /api/history, /api/send]
       |
       v
[SQLite Host: Table conversations + Table entries (conversationId)]
       |
       v
[ACP Session Key Slot: conv_{conversationId}]
       |
       v
[Isolated Real CLI Execution (OpenCode, Antigravity, etc.)]
```

### 2.1 Modèle de Données Universel de Conversation

Chaque conversation dispose de la structure complète requise :
- `id` (String / UUID) : Identifiant unique de la discussion.
- `bot_id` (String) : Identifiant de l'agent associé.
- `title` (String) : Titre personnalisable de la session.
- `cli_id` (Option<String>) : CLI spécifique exécutant la session.
- `runner_id` (Option<String>) : Runner de pilotage.
- `cli_session_id` (Option<String>) : Identifiant de session ACP isolé.
- `status` (String) : `ACTIVE`, `ARCHIVED`, `CLOSED`.
- `created_at` (i64) : Horodatage millisecondes de création.
- `updated_at` (i64) : Horodatage de dernière activité.

---

## 3. Détails des Implémentations

### 3.1 Host Rust

1. **Table et Index SQL (`host/src/store/schema.rs` & `host/src/store/conversations.rs`)** :
   ```sql
   CREATE TABLE IF NOT EXISTS conversations (
       id TEXT PRIMARY KEY,
       bot_id TEXT NOT NULL,
       title TEXT NOT NULL,
       cli_id TEXT,
       runner_id TEXT,
       cli_session_id TEXT,
       status TEXT NOT NULL DEFAULT 'ACTIVE',
       created_at INTEGER NOT NULL,
       updated_at INTEGER NOT NULL
   );
   CREATE INDEX IF NOT EXISTS idx_conversations_bot ON conversations (bot_id, updated_at DESC);
   ```
2. **Filtrage de l'Historique par Conversation (`host/src/store/conversations.rs`)** :
   Requête `history_conversation` filtrant les entrées JSON où `json_extract(data, '$.conversationId') = ?`.
3. **Propagation dans le Flux de Tour (`host/src/agent/bot/`)** :
   - Ajout de `turn_conversation_id: Option<String>` sur l'acteur de tour.
   - Injection automatique de `conversationId` dans chaque message et log généré.
   - **Isolation de la Session ACP (`host/src/agent/bot/session.rs`)** :
     Routage de `slot(&self)` vers `conv_{conversationId}`, assurant que chaque discussion bénéficie d'une instance de runner et d'un contexte de session ACP hermétique.

### 3.2 Client Android

1. **Migration Room Database v2 -> v3 (`data/local/CodeStationDatabase.kt`)** :
   - Ajout de la table `conversations`.
   - Migration sûre ajoutant la colonne `conversationId` sur `chat_messages` (avec repli par défaut sur `botId`).
2. **Gestionnaire de Sessions dans `ChatViewModel.kt`** :
   - Gestion de l'état `_activeConversationId`, `conversations` (Flow réactif).
   - Bascule fluide `selectConversation(id)`.
   - Actions `createNewConversation()`, `deleteConversation(id)`, `renameConversation(id, title)`.
   - Redirection automatique du flux de messages via `flatMapLatest`.
3. **Composants d'Interface Jetpack Compose (`ui/screens/ChatScreen.kt` & `RosterScreen.kt`)** :
   - En-tête de conversation avec capsule dynamique affichant le titre de la discussion courante et chevron dropdown.
   - Dialogue complet de gestion des sessions (`showConversationDialog`) permettant de visualiser les sessions actives, en créer une nouvelle, renommer ou supprimer.
   - Boîte de dialogue de sélection d'agent lors du clic sur « Nouvelle Discussion » depuis le Roster.

---

## 4. Résultats et Tests d'Isolation Réalisés

Les scénarios 2 et 3 ont été validés avec les moteurs réels :
1. **Scénario 2** : Création de la conversation `8623f01c-502e-43b9-947a-6ff12365f327` pour `bot-opencode-glm`. Envoi du prompt `Echo CODESTATION_SESSION_1`. Réception et enregistrement d'une réponse dédiée.
2. **Scénario 3** : Création d'une seconde conversation `86b1ee8b-60d5-43a5-9a35-a872cbac1930` pour le même bot. Envoi d'un prompt distinct `Echo STRICTLY_ISOLATED_2`.
3. **Contrôle d'étanchéité** : Vérification par requête SQL et API que la conversation 1 ne contient aucune trace du prompt de la conversation 2, et inversement (0 fuite de contexte).
