# CodeStation — Journal de Travail (worklog.md)

**Branche de travail :** `fix/agent-execution-and-termux-ui`  
**Date :** 28 Octobre 2026  
**Auteur :** Antigravity AI  

---

## 1. Cartographie des Composants et du Flux d'une Requête

```mermaid
sequenceDiagram
    autonumber
    actor User as Utilisateur (Android)
    participant ChatUI as ChatScreen & ChatViewModel
    participant Room as Base locale Room (messageDao)
    participant SSE as CodeStationEventStreamClient (/events)
    participant Host as CodeStation Host (Rust :19222)
    participant PRoot as Ubuntu PRoot (bot-opencode-glm / bot-antigravity)
    participant CLI as Agent CLI (opencode / agy)

    User->>ChatUI: Saisie "Hi" et validation
    ChatUI->>Room: Insertion message utilisateur (status: SENT)
    ChatUI->>Host: POST /api/send {botId, text, clientNonce}
    Host->>Host: Inscription dans SQLite codync.db (turn, kind: "user")
    Host->>PRoot: Lancement de l'agent via wrapper run.sh
    PRoot->>CLI: Exécution CLI (ex: opencode acp)
    CLI->>CLI: Traitement LLM (GLM-5.3-Flash Free)
    CLI-->>Host: Protocole ACP stdio (output agent, thought, tool)
    Host->>Host: Inscription SQLite codync.db (kind: "agent", text: "Hi! 👋...")
    Host-->>SSE: Événement SSE 'entry' {kind: "agent", data: {text: "Hi! 👋..."}}
    Note over SSE: [POINT DE RUPTURE IDENTIFIÉ]
    SSE-->>Room: Insertion dans messageDao
    Room-->>ChatUI: Émission Flow<List<ChatMessage>>
    ChatUI-->>User: Affichage du message dans la vue
```

---

## 2. Causes Démontrées des Dysfonctionnements

### Cause 1 : Filtrage erroné des messages dans l'écouteur SSE Android (`CodeStationEventStreamClient.kt:178`)
- **Constat** : Dans le code existant de `CodeStationEventStreamClient.kt`, le filtre d'insertion Room était :
  ```kotlin
  if (text.isNotBlank() && (kind == "assistant" || kind == "notice" || kind == "user"))
  ```
- **Fait démontré dans la base SQLite réelle (`codync.db`)** :
  L'hôte CodeStation enregistre et diffuse les réponses des bots avec `kind = "agent"` (`EntryKind::Agent` dans `host/src/store/model.rs`).
- **Conséquence directe** :
  Lorsque OpenCode a répondu avec succès :
  `"Hi! 👋 I'm OpenCode GLM, ready to help. What would you like to work on today?"` (kind: `"agent"`),
  le client SSE Android a **ignoré et jeté** la réponse. Le message n'a jamais été inséré dans `messageDao`, et l'interface est restée bloquée indéfiniment sur « Réflexion en cours… ».

### Cause 2 : Absence de réinitialisation de `_isBotWorking` et absence de timeout UI
- `ChatViewModel` activait `_isBotWorking.value = true` lors de l'envoi, mais rien ne remettait cette valeur à `false` lorsqu'une réponse était reçue ou lorsque le bot revenait à l'état `IDLE`.
- Aucun délai d'attente (timeout) n'était configuré : si le serveur ou le fournisseur tardait, le spinner tournait indéfiniment.

### Cause 3 : Absence de déclenchement interactif d'une session Termux
- Le serveur CodeStation exécute les agents en arrière-plan via des pipes stdio pour l'API ACP (`Acp::spawn`).
- Aucune commande Intent Android vers Termux (`com.termux.RUN_COMMAND` ou intent d'activité) n'était exposée dans l'interface pour permettre à l'utilisateur d'ouvrir interactivement un terminal dans le conteneur du bot.

### Cause 4 : Déclenchement abusif des notifications et divergence avec `StatusBarPreview`
- Dans `CodeStationEventStreamClient.kt`, le statut `"idle"` du bot était automatiquement mappé à `TaskNotificationPhase.COMPLETED`, provoquant la publication répétée de la notification « Tâche exécutée avec succès » alors même qu'aucune tâche n'avait été exécutée.
- La notification ne reflétait pas le design capsule/pill avec chronomètre visible dans `StatusBarPreview`.

### Cause 5 : Encombrement de l'écran « Connexion Termux »
- Les onglets « Réseau LAN » et « Passerelle » étaient mis en avant alors qu'ils ne sont pas encore opérationnels.
- Les conteneurs de bots étaient relégués tout en bas d'une longue page déroulante, et le jeton d'authentification s'affichait sans masquage.

---

## 3. Plan d'Implémentation des Correctifs

1. **`CodeStationEventStreamClient.kt`** :
   - Accepter `kind == "agent"` et `kind == "assistant"`.
   - Ignorer les messages internes silencieux de passage `(pass)`.
   - Mapper les pensées (`kind == "thought"`) et les exécutions d'outils (`kind == "tool"`).
   - Corriger la détection des approvations (`kind == "permission"`).
   - N'émettre la notification `COMPLETED` que si une transition réelle depuis l'état `WORKING` a eu lieu.

2. **`ChatViewModel.kt`** :
   - Réinitialiser `_isBotWorking` dès qu'un message du bot arrive ou que le bot passe à `IDLE`.
   - Ajouter un timeout configurable (60 secondes) avec message d'avertissement et bouton Réessayer.
   - Corriger `stopTurn()` pour forcer l'arrêt immédiat et insérer un avis d'annulation.

3. **Intégration Session Termux Interactive (`TermuxSessionLauncher.kt`)** :
   - Créer un lanceur dédié capable d'envoyer l'intent Termux `com.termux.RUN_COMMAND` ou d'ouvrir l'application Termux avec la commande prête.
   - Ajouter des boutons explicites « Ouvrir la session Termux » dans l'interface.

4. **Notifications & Barre d'État / Dynamic Island Pill (`CodeStationNotificationManager.kt` & Composant Compose)** :
   - Nettoyer les fausses réussites.
   - Créer le composant `DynamicIslandBar` reproduisant fidèlement `StatusBarPreview` en haut de l'écran.

5. **Restructuration de `PairingScreen.kt`** :
   - Remplacer les onglets non fonctionnels LAN/Passerelle par des onglets clairs :
     1. « Connexion & Diagnostic »
     2. « Conteneurs Ubuntu PRoot »
     3. « Commandes & Ressources »
   - Masquer le jeton par défaut avec icône oeil pour révélation explicite.
