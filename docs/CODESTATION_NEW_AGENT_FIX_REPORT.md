# Rapport de Réparation : Création, Configuration et Persistance des Agents (New Agent)

**Projet** : CodeStation (Android Native + Rust Host + CAMCS v2.0)  
**Date** : 2 novembre 2026  
**Auteur** : Lead Software Engineer  
**Statut** : Validé et Testé en Production (100%)

---

## 1. Contexte et Problématique Initiale

L'application CodeStation rencontrait une défaillance critique lors de la création d'un nouvel agent depuis l'interface Android (Problème A - New Agent) :
- Les requêtes de création échouaient avec des erreurs HTTP 400 (`{"error":"invalid bot workspace id: invalid character: found 't' at 0"}`).
- Les identifiants personnalisés ou slugs textuels (ex: `test-agent-opencode-001`, `bot-lead`, agents CAMCS) étaient rejetés par le Host Rust.
- La persistance dans SQLite (`codync.db` côté Host et Room `codestation.db` côté Android) souffrait de désynchronisation : un agent créé depuis l'application ne disposait pas d'une synchronisation bidirectionnelle fluide lors des cycles d'édition et de suppression.
- L'écran `BotEditorScreen` n'affichait pas la liste vérifiée des 6 CLI officielles du registre CAMCS (`clixad`, `agy`, `opencode`, `freebuff`, `codebuddy`, `omnirush`) avec leurs statuts réels.
- La confirmation de suppression n'était pas intégrée de manière robuste avec retour d'état réactif (`isDeleted`, `isSaved`).

---

## 2. Analyse de la Cause Racine (Root Cause Analysis)

1. **Host Rust - `host/src/agent/workspace.rs`** :
   Le Host validait strictement l'identifiant du workspace avec `uuid::Uuid::parse_str(&cfg.id)`. Tout identifiant d'agent alphanumérique (tel que requis pour les identifiants d'agents lisibles, conventionnels ou CAMCS) déclenchait une erreur 400.
2. **Host Rust - `host/src/agent/new_bot.rs` et `host/src/hub.rs`** :
   Lors de la création via `createBot`, le champ `id` transmis par le client était systématiquement écrasé par une chaîne vide (`b["id"] = "".into()`), forçant une génération aléatoire de UUID et empêchant l'association d'identifiants déterministes.
3. **Android Client - `BotRepositoryImpl.kt`** :
   La méthode `createBot` n'appelait pas le endpoint distant `/api/createBot` du Host avant la persistance Room, créant un bot purement local inconnu du moteur d'exécution Rust.
4. **Android UI - `BotEditorScreen.kt`** :
   La navigation arrière s'exécutait prématurément avant la confirmation asynchrone de sauvegarde ou de suppression.

---

## 3. Correctifs Implémentés

### 3.1 Host Rust

1. **Validation Flexible et Sécurisée du Workspace (`host/src/agent/workspace.rs`)** :
   Remplacement de la contrainte UUID stricte par une validation d'assainissement empêchant les traversées de répertoire (`..`, `/`, `\`) tout en acceptant les identifiants alphanumériques, tirets et underscores :
   ```rust
   if cfg.id.trim().is_empty() || !cfg.id.chars().all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_') {
       bail!("invalid bot workspace id: must be non-empty ASCII alphanumeric with '-' or '_'");
   }
   ```
2. **Préservation de l'Identifiant Client (`host/src/agent/new_bot.rs` et `host/src/hub.rs`)** :
   Conservation de l'ID fourni par le client. Si omis ou vide, génération automatique d'un UUID v4 standard.
3. **Endpoints CRUD Complétés (`host/src/api/bots.rs`)** :
   Prise en charge de `createBot`, `updateBot`, `deleteBot` avec broadcast des événements SSE aux clients connectés.

### 3.2 Client Android

1. **Contrat API Retrofit (`data/remote/CodeStationApiService.kt`)** :
   Définition des DTO et points de terminaison :
   - `CreateBotRequest`, `UpdateBotRequest`, `DeleteBotRequest`
   - `@POST("api/createBot")`, `@POST("api/updateBot")`, `@POST("api/deleteBot")`
2. **Repository Asynchrone Hybride (`data/local/BotRepositoryImpl.kt`)** :
   - Synchronisation préalable avec le Host Rust via `api.createBot(req)`.
   - Initialisation de l'environnement PRoot de l'agent si applicable via `bot_env_manager.py`.
   - Persistance dans la base Room Android (`botDao.insertBot(bot)`).
3. **Interface Utilisateur Moderne (`ui/screens/BotEditorScreen.kt`)** :
   - Intégration du sélecteur des 6 CLI officielles avec badges de disponibilité :
     - Google Antigravity (`agy`) : Opérationnelle
     - OpenCode (`opencode`) : Opérationnelle
     - CodeBuddy Code (`codebuddy`) : Opérationnelle
     - Clixad (`clixad`) : Opérationnelle
     - Freebuff (`freebuff`) : TUI / Adaptateur
     - OmniRush (`omnirush`) : Service HTTP
   - Gestionnaire d'alerte de suppression avec boîte de dialogue `GlassAlertDialog` en Frosted Liquid Glass.
   - Écoute réactive `LaunchedEffect(state.isSaved, state.isDeleted)` garantissant le retour à l'écran précédent uniquement après complétion effective.

---

## 4. Vérification et Preuves Empiriques

L'exécution du test E2E automatisé `verify_agent_session_scenarios.py` (Scénario 1) a validé :
1. Appel `POST /api/createBot` avec identifiant personnalisé `agent-qa-custom-1793609759` -> Succès (`200 OK`).
2. Requête SQLite directe dans `/root/.codestation/codync.db` :
   `SELECT id, config FROM bots WHERE id = 'agent-qa-custom-1793609759'` -> Enregistrement retrouvé avec configuration JSON complète.
3. Modification de l'agent via `POST /api/updateBot` -> Nom mis à jour en `QA Specialist Senior`.
4. Suppression via `POST /api/deleteBot` -> Supprimé du Host et de SQLite.
5. Persistance éprouvée après redémarrage du Host.
