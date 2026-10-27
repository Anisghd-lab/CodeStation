# TEST_PLAN.md — Plan de Qualification & Stratégie de Test

> **Application** : Synapse (`com.ghdinteractivestudio.synapse`)  
> **Date** : 27 Octobre 2026

---

## 1. Objectifs de Test

Valider la conformité fonctionnelle, la robustesse de l'architecture MVVM, la précision des flux d'état réactifs et l'intégrité de l'interface Jetpack Compose sans dépendance à une machine physique Android pendant la phase CI.

---

## 2. Niveaux de Test

### 2.1 Tests Unitaires de Logique Métier (JVM Unit Tests)
* **Frameworks** : JUnit 4 / JUnit 5 + MockK / Kotlinx Coroutines Test (`StandardTestDispatcher`, `runTest`).
* **Périmètre couvert** :
  1. `RosterViewModelTest` :
     - Vérification du tri strict en 3 bandes (Lead Bot avec étoile or `★` en tête permanente, puis bots épinglés, puis bots triés par récence).
     - Filtrage par mot-clé et sélection de filtre (Tous, Connectés, Hors-ligne).
     - Gestion du rafraîchissement d'état et des erreurs réseau.
  2. `ChatViewModelTest` :
     - Envoi de message avec nonce unique.
     - Gestion de la citation (`replyTo`) : attachement du texte cité et réinitialisation de la barre de réponse après envoi.
     - Prise en charge des approbations : soumission d'une décision (Approuver / Rejeter) et mise à jour de l'état UI.
     - Réception d'un message complet final sans streaming corrompu.
  3. `PairingViewModelTest` :
     - Validation des formats d'URL (`http://127.0.0.1:19222`, `https://...`).
     - Validation et extraction des paramètres d'URI d'appairage (`synapse://pair?token=...`).
     - Simulation des réponses ping (succès, délai dépassé, hôte introuvable).
  4. `RepositoryTests` (`BotRepository`, `ChatRepository`) :
     - Fonctionnement du cache local Room avec bascule fluide en cas de déconnexion réseau.

### 2.2 Tests de Composants UI Compose
* **Framework** : Compose UI Testing (`createComposeRule`).
* **Périmètre couvert** :
  - `BotCardTest` : Rendu du nom, du statut actif pulsant et du badge de non-lus.
  - `MessageComposerTest` : Bascule dynamique du bouton d'action (Microphone quand vide vs Flèche d'envoi quand du texte est saisi).
  - `ApprovalCardTest` : Déclenchement des callbacks lors du clic sur « Approuver une fois » et « Rejeter ».
  - `EmptyStateTest` : Présence du bouton d'action et du message incitatif.

---

## 3. Matrice de Validation CI (GitHub Actions)

La suite de tests est exécutée à chaque push et pull request via `.github/workflows/build_apk.yml` sur une image `ubuntu-latest` :
```bash
./gradlew testDebugUnitTest --continue
./gradlew assembleDebug
```
L'APK de debug produit est téléchargeable directement comme artefact GitHub Actions (`synapse-debug.apk`).
