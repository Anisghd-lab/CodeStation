# CodeStation Release Notes — Version 1.1.2

**Date de version** : 5 Novembre 2026  
**Plateforme cible** : Android 16 (ARM64 PRoot / Native Engine)

---

## 🎯 Correctifs Majeurs & Améliorations

### 1. Affichage Intégral des Messages de Réponse pour Tous les Bots dans l'Application
- **Persistance de `conversationId`** : Correction dans `host/src/agent/bot/updates.rs` et `turn.rs`. Les chunks de texte streamés (`flush`) et les messages finaux conservent désormais systématiquement le `conversationId` de la conversation active au lieu de le tronquer à `""`.
- **Tolérance de filtrage Room (`MessageDao`)** : Mise à jour de la requête `getMessagesByBotAndConversationStream` dans SQLite Room (`(:conversationId IS NULL OR :conversationId = '' OR conversationId = :conversationId OR conversationId = '' OR conversationId IS NULL)`). Tout message rattaché à un agent ou non encore assigné s'affiche directement dans la conversation active et n'est plus jamais masqué ou filtré dans l'UI.
- **Résolution automatique de conversation fallback** : `CodeStationEventStreamClient` associe automatiquement les messages d'agent orphelins à la conversation la plus récente du bot via `conversationDao.getLatestConversationForBot`.
- **Suppression des instructions contradictoires système (`outbox::INSTRUCTIONS`)** : Élimination de la directive qui incitait les modèles à répondre silencieusement `(pass)`. Tous les bots (`bot-antigravity`, `bot-opencode-glm`, `bot-clixad`, `bot-omnirush`, `bot-freebuff`, `bot-codebuddy`) écrivent directement leur réponse texte dans le chat.
- **Migration automatique de base de données (Schema v7)** : Backfill automatique dans `codync.db` pour restaurer et réassocier les anciens messages d'agent aux conversations utilisateurs d'origine.

---

## 🧪 Tests & Vérifications Réelles
- [x] Compilation et tests unitaires Rust : `test store::schema::tests::migrate_7_backfills_conversation_id ... ok`
- [x] Test E2E réel avec chaque bot (`bot-antigravity`, `bot-opencode-glm`, `bot-clixad`, `bot-omnirush`, `bot-freebuff`, `bot-codebuddy`) validant la génération de messages d'agent contenant `conversationId` et `final: true`.
- [x] Suite de tests globale E2E : 16/16 tests validés avec succès.
