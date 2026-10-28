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

---

## 3. Audit et Correction des Espaces Vides Excessifs dans l'Interface Android

**Branche dédiée :** `fix/layout-compactness-and-form-spacing`  
**Date :** 28 Octobre 2026

### A. Diagnostic Technique des Espacements
1. **`Controls.kt` (`CardForm`)** :
   - Hardcoding d'un espacement vertical démesuré de `28.dp` (`Arrangement.spacedBy(28.dp)`) entre chaque section, créant un sentiment de vide immense.
   - Padding interne de ligne de `14.dp` qui cumulait de la hauteur inutilement.
   - **Correction** : Paramètre `verticalSpacing: Dp = 14.dp` et `padding: PaddingValues(horizontal = 16.dp, vertical = 12.dp)`, avec interlignes de `11.dp`.

2. **`SettingsScreen.kt` (« Ordinateurs et paramètres »)** :
   - **Fragmentation excessive** : Huit cartes séparées pour des éléments isolés (une carte pour Thème, une pour Langue, une pour Discussion, une pour Analytics...).
   - **Conflit de disposition des interrupteurs** : `CodyncSwitch` intègre déjà son propre `Spacer(Modifier.weight(1f))`. L'envelopper dans un `Row` externe avec un `Text` portant également `weight(1f)` coupait l'espace 50/50, isolant les interrupteurs dans un vide flottant décalé.
   - **Section Notifications** : Mention de messages push en anglais non configurés qui semaient la confusion.
   - **Version repoussée** : Les informations de build et le bouton de réinitialisation étaient éjectés tout en bas hors de vue.
   - **Correction** :
     - Regroupement en 4 cartes naturelles et compactes (Ordinateurs, Préférences générales, Notifications & Diagnostics, À propos).
     - Interrupteurs `CodyncSwitch` directement associés à leur texte sans conflit de `weight(1f)`.
     - Suppression des avertissements push confus et valorisation des notifications locales / Live Updates réelles d'Android.
     - Version 1.0.3 et lien GitHub immédiatement visibles.

3. **`BotEditorScreen.kt` (« Nouvel Agent »)** :
   - **Empilement démesuré** : 4 cartes moteurs verticales géantes (240dp) + 7 cartes modèles (350dp) occupant 600dp à elles seules, suivies d'un immense vide artificiel lorsque des moteurs plus courts étaient sélectionnés.
   - **Avatar et formulaire séparés** : L'avatar occupait un bloc gigantesque au-dessus des champs d'identité.
   - **Boutons d'action non ancrés** : Le bouton « Créer l'Agent » était abandonné au fond du défilement, invisible sur petit écran ou avec clavier virtuel.
   - **Correction** :
     - Disposition compacte : Avatar (54dp) intégré à gauche du champ « Nom de l'Agent ».
     - Moteurs compacts avec badges d'état et modèles compatibles avec sélecteur clair (badge « Gratuit » pour `GLM-5.3-Flash Free`).
     - Interrupteurs Fast Mode et Pinned directement accolés à leurs descriptions.
     - Barre d'actions inférieure fixe (`Surface` avec `imePadding()` et `navigationBarsPadding()`) garantissant la présence permanente des boutons « Annuler » et « Créer l'Agent ».

---

## 4. Affichage Distinct des Versions Android et Backend

**Date :** 28 Octobre 2026

### A. Implémentation
1. **CodeStation Android** :
   - Activation de `buildFeatures { buildConfig = true }` dans `app/build.gradle.kts`.
   - Lecture réelle du `versionName` (`1.0.3`) et `versionCode` (`4`) via `BuildConfig` et l'API `PackageManager` d'Android.
   - Affichage explicite : `CodeStation Android : 1.0.3 (Build 4)`.

2. **CodeStation Host** :
   - Récupération de la version réelle du backend connecté via `ServerConfig.serverVersion` (alimenté par les routes `/health` et `/hello`).
   - Affichage dynamique :
     - Si connecté avec version vérifiée : `CodeStation Host : 2.28.0` (vert émeraude).
     - Si serveur arrêté ou hors ligne : `CodeStation Host : Indisponible`.

3. **Validation & Tests** :
   - Ajout du fichier de tests unitaires `ComponentVersionsTest.kt` validant la séparation stricte des versions, la détection de l'état déconnecté et la non-pollution mutuelle des versions.

---

## 5. Déploiement et Installation Physique sur Appareil Réel (ADB)

**Cible :** `192.168.1.23:33931`  
**Package :** `com.ghdinteractivestudio.codestation.debug`  
**Version installée :** `versionName=1.0.3`, `versionCode=4`  
**SHA-256 APK :** `9fa3a6d4cc6ec1f9e67e1d58d5764614747a15dd8778f5f3ddf19a585062dd5e`

- **Connexion ADB** : `adb connect 192.168.1.23:33931` (état `device`).
- **Installation** : `adb -s 192.168.1.23:33931 install -r CodeStation-v1.0.3-debug.apk` -> `Performing Streamed Install -> Success`.
- **Démarrage automatique** : Lancement de `MainActivity` avec succès (`mCurrentFocus=Window{... MainActivity}`).

---

## 6. Audit Critique de Bout en Bout, Détection des CLI, Messages Illimités, Pièces Jointes, Messages Vocaux et UI Glassmorphism

**Date :** 28 Octobre 2026  
**Branche :** `fix/bot-launch-and-persistent-notifications`

### A. Traçage Complet du Chemin des Messages (Phase 1)
- Implémentation du traceur d'audit structuré `RequestTracer.kt` corrélant chaque message avec un `requestId` unique à travers 13 étapes d'audit :
  1. `message_created` (création avec longueur et pièces jointes)
  2. `message_queued` (file d'attente locale)
  3. `request_sent` (envoi réseau HTTP local)
  4. `request_received` (réception par CodeStation Host)
  5. `bot_resolved` (résolution configuration bot)
  6. `cli_resolved` (moteur résolu : `antigravity` ou `opencode`)
  7. `process_started` (lancement ACP / sous-processus)
  8. `cli_input_delivered` (délivrance stdin sans argument shell)
  9. `provider_request_started` (requête vers fournisseur LLM)
  10. `first_output_received` (premier chunk de réponse)
  11. `response_completed` (réponse finale reçue)
  12. `request_failed` (échec ou timeout explicite)
  13. `request_cancelled` (interruption manuelle par l'utilisateur)
- **Sécurité** : Filtre automatique supprimant tout jeton, mot de passe ou clé API des détails de traçage.

### B. Détection Réelle des CLI dans Ubuntu PRoot (Phase 2)
- Implémentation de la détection réelle dans `scripts/bot_env_manager.py detect` et propagation via `EnvironmentsResponse` :
  - **Google Antigravity (`agy`)** : Détecté dans `/root/.local/bin/agy` (v1.4.9), statut `READY`, `is_primary=true`, environnement `bot-antigravity`.
  - **OpenCode GLM (`opencode`)** : Détecté dans `/usr/local/bin/opencode` (v2.2.0), statut `READY`, `is_primary=true`, environnement `bot-opencode-glm`.
  - **OpenAI Codex (`codex`)** : Détecté dans `/data/data/com.termux/files/usr/bin/codex` (v0.153.3), statut `INSTALLED`, `is_primary=false`, environnement `termux`.
- Dans `BotEditorScreen.kt` : Moteurs prioritaires Google Antigravity et OpenCode GLM placés en tête avec badges d'état en temps réel, suivis d'une section distincte « Autres CLI détectées ».

### C. Messages Sans Limite Artificielle de Caractères (Phase 3)
- **Suppression des contraintes arbitraires** : Aucun `maxLength` dans Compose. Le composant `BasicTextField` s'adapte en hauteur et défile pour les messages très longs.
- **Délivrance sécurisée via stdin** :
  - `environments/bot-antigravity/run.sh` et `agy_acp_bridge.py` utilisent l'argument officiel `agy -p -` avec injection directe sur le flux standard `stdin=subprocess.PIPE`.
  - `environments/bot-opencode-glm/run.sh` utilise le passage de flux standard `printf '%s' | opencode run`.
  - Élimination des limites de taille de ligne de commande (`ARG_MAX`), avec préservation intégrale des retours à la ligne, guillemets, emojis et caractères accentués.

### D. Bouton « + » Pièces Jointes & Design Glassmorphism (Phase 4 & 6)
- **Menu Glassmorphism iPhone-inspired** (`AttachmentMenu.kt`) avec options Photo, Vidéo et Fichier.
- Sélecteurs natifs Android `PickVisualMedia` et `OpenDocument` (Storage Access Framework) sans demander de permissions excessives.
- Cartes d'aperçu de pièces jointes compactes (`AttachmentPreviewRow.kt` et `ChatMessageItem.kt`) avec nom, taille formatée et badge d'avertissement explicite pour les vidéos (« Modèle texte seul »).

### E. Messages Vocaux et Transcription (Phase 5)
- **Enregistreur vocal natif** (`VoiceRecorder.kt`) avec encodage AAC / MediaRecorder, calcul de durée en temps réel et permission `RECORD_AUDIO`.
- **Moteur de transcription** (`VoiceTranscriptionService.kt`) avec `SpeechRecognizer` natif et conversion parole-en-texte injectée dans le compositeur ou envoyée à l'agent.
- Nettoyage automatique des fichiers audio temporaires après transcription.

### F. Validation E2E & Tests Unitaires (Phase 7)
- **Suite de tests E2E `scripts/test_agents_e2e.py`** : **11/11 tests réussis**.
- **Tests unitaires dédiés** :
  - `CliDetectionTest.kt` : Détection des CLI, validation des statuts `READY`/`INSTALLED`, et vérification de l'isolation des rootfs.
  - `MessagePipelineAndLengthTest.kt` : Validation des 13 étapes de traçage, corrélation par `requestId`, filtrage des secrets et intégrité des longs messages.
  - `AttachmentAndVoiceTest.kt` : Classification des pièces jointes et cycle de vie audio.
  - `AntigravityDynamicModelTest.kt` : Découverte dynamique des modèles `agy` et rétention de cache.
  - `BotNotificationAndTraceTest.kt` : Notifications persistantes d'exécution des agents.
  - `NotificationArchitectureTest.kt` : Canaux v2 et complétude des phases de notification.

---

## 7. Notifications Persistantes dans la Barre d'État Android (Android 14/15/16)

**Date :** 28 Octobre 2026  
**Branche :** `fix/bot-launch-and-persistent-notifications`

### Objectif et Problématiques Résolues
1. **Disparition ou rejet par balayage (Swipe dismiss)** : Sous Android 14+ (`API 34/35/36`), les notifications persistantes `setOngoing(true)` peuvent être écartées par l'utilisateur sauf si les drapeaux système `FLAG_ONGOING_EVENT` et `FLAG_NO_CLEAR` sont activement combinés et que le canal est en `IMPORTANCE_DEFAULT` ou `IMPORTANCE_HIGH`.
2. **Icône dans la barre d'état invisible ou tronquée** : Utilisation du drawable vectoriel monochrome optimisé `ic_launcher_monochrome.xml` (avec teinte Indigo `#6366F1`) garantissant un rendu net et visible dans la barre d'état Android sans effet de bloc blanc opaque.
3. **Absence de notification immédiate lors de l'envoi** : `ChatViewModel.sendMessage()` déclenche désormais instantanément la notification persistante de travail avec chronomètre de progression avant même la réception du flux SSE, évitant tout délai d'affichage si l'utilisateur quitte l'application.
4. **Service au premier plan de synchronisation locale (`CodeStationTaskSyncService`)** :
   - Démarré de façon conforme dans `MainActivity.onStart()` avec type `FOREGROUND_SERVICE_TYPE_DATA_SYNC`.
   - Maintient la connexion SSE locale avec CodeStation Host (`127.0.0.1:19222`) active en arrière-plan lorsque l'utilisateur bascule sur Termux ou verrouille l'écran.
5. **Actions contextuelles enrichies** :
   - `Ouvrir` : Redirection immédiate par deep link vers le chat de l'agent concerné.
   - `Arrêter` : Interruption propre et immédiate de l'agent via `NotificationActionReceiver.ACTION_STOP_BOT`.
   - `Autoriser` / `Refuser` : Prise de décision directe pour les commandes shell et modifications depuis la notification d'approbation (`ACTION_APPROVE` / `ACTION_REJECT`).
6. **Mise à niveau des canaux (Migration v2)** : Canaux créés avec IDs mis à jour (`codestation_live_tasks_v2`, `codestation_approvals_v2`, `codestation_completed_v2`) avec suppression des versions obsolètes pour forcer la prise en compte sur les appareils ayant déjà exécuté une version antérieure.


