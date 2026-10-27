# Plan de Test & Matrice de Vérification de Parité UI/UX

Date : 27 Octobre 2026  
Projet : **Synapse** (`com.ghdinteractivestudio.synapse`)  

---

## 1. Objectifs de Qualification

Ce document définit les protocoles de tests automatisés et de vérification manuelle garantissant que :
1. L'application Synapse reproduit scrupuleusement l'expérience utilisateur et les éléments visuels de Codync Android.
2. Les interactions dynamiques (animations, appuis, saisie clavier, défilement) se comportent selon les spécifications.
3. Les communications locales avec l'hôte Termux sur `http://127.0.0.1:19222` sont robustes, gèrent les interruptions et reflètent immédiatement le statut dans l'interface.

---

## 2. Suite de Tests Automatisés (Unitaires & Régression)

Les tests sont situés dans `app/src/test/kotlin/com/ghdinteractivestudio/synapse/` et exécutés automatiquement dans le pipeline CI GitHub Actions.

### 2.1. `DesignTokensTest.kt` (Validation des Tokens Graphiques)
- **Objectif** : Vérifier que la palette monochrome officielle Codync n'a pas dérivé vers des couleurs saturées arbitraires.
- **Vérifications** :
  - `Palette.dark().background` == `#0A0A0A` (noir profond).
  - `Palette.dark().surface` == `#141414`.
  - `Palette.dark().bubbleAgent` == `#1E1E1E`.
  - `Palette.dark().bubbleUser` == `#262626`.
  - `Palette.dark().warning` == `#F59E0B` (ambre d'attention).
  - `Palette.dark().danger` == `#EF4444` (rouge critique).
  - Présence des 8 formes géométriques de `AvatarPalette` (`blob`, `pebble`, `squircle`, `tablet`, `wedge`, `hex`, `cloud`, `teardrop`).
  - Constantes de `Motion` (`PRESS_SCALE = 0.98f`, durées d'accélération).
- **Statut** : ✅ Réussi.

### 2.2. `ServerConnectionTest.kt` (Validation de la Couche Réseau & Termux)
- **Objectif** : Tester la gestion des URL, la classification du mode de connexion, la désérialisation de l'API et la gestion des codes d'erreur HTTP.
- **Vérifications** :
  - Détection automatique de `ServerMode.LOCAL_LOOPBACK` pour `http://127.0.0.1:19222` et `localhost`.
  - Formatage automatique des URL dépourvues de préfixe `http://`.
  - Parsing des URL d'appairage au schéma `synapse://pair?token=...&host=...`.
  - Succès de la sonde de santé `GET /health` (`ok: true`, version "2.28.0").
  - Gestion de l'erreur `401 Unauthorized` sur `POST /api/hello` avec message d'erreur clair à l'utilisateur.
- **Statut** : ✅ Réussi.

### 2.3. `ModelValidationTest.kt` (Persistance & Modèles de Domaine)
- **Objectif** : Valider la conversion bidirectionnelle entre les entités SQLite Room et les modèles de domaine Kotlin.
- **Vérifications** :
  - Conversion roundtrip `Bot` ⟷ `BotEntity` sans perte d'information (statut, lead, épinglé, avatars, mode workspace).
  - Détection des groupes multi-agents via la présence d'identifiants dans `memberBotIds`.
  - Préservation du statut du bot coordinateur `isLead`.
- **Statut** : ✅ Réussi.

### 2.4. `RosterViewModelTest.kt` (Ordonnancement & Filtrage du Roster)
- **Objectif** : Valider la logique de présentation de la liste des agents.
- **Vérifications** :
  - Le bot `isLead` est systématiquement extrait et positionné en première place (`state.leadBot`).
  - Les bots épinglés (`isPinned`) apparaissent ensuite de manière prioritaire (`state.pinnedBots`).
  - Le filtre de recherche en temps réel isole instantanément les bots par leur nom.
- **Statut** : ✅ Réussi.

### 2.5. `ChatViewModelTest.kt` (Flux de Conversation & Approbations)
- **Objectif** : Valider l'envoi de messages, le déclenchement des actions d'approbation et la gestion de la réponse en streaming.
- **Vérifications** :
  - Appel conforme à `chatRepository.sendMessage` lors de la validation du compositeur.
  - Transmission du statut de décision (`APPROVED`, `REJECTED`, `isAlwaysApproved`) au repository d'approbations.
  - Réinitialisation du champ de saisie après expédition.
- **Statut** : ✅ Réussi.

---

## 3. Matrice de Vérification Visuelle & Ergonomique (11 Écrans)

| Écran / Composant | Critère Visuel Spécifié | Comportement Attendu | Résultat |
|---|---|---|---|
| **Palette Globale** | Fond noir `#0A0A0A` | Aucun artefact violet ou bleu fluo en arrière-plan. | Conforme |
| **Typographie** | Dynamic Type Inter | Crénage optique condensé sur les grands titres, aéré sur les petits textes. | Conforme |
| **CharacterAvatar** | Formes 3D & Éclairage | Rendu net des 8 formes vectorielles, gradient d'illumination sphérique en haut à gauche, yeux animés clignotants. | Conforme |
| **ThinkingOrb** | Wireframe 3D | Orbe rotatif 3D filaire fluide affiché pendant la réflexion de l'agent. | Conforme |
| **RosterScreen** | Liste hiérarchisée | Lead bot en tête avec badge étoile, badge ambre pour les bots en attente d'input, badge wifi Termux. | Conforme |
| **ChatScreen** | Fil de discussion | Bulles grises neutres (`#1E1E1E` / `#262626`), en-tête avec ping wifi en temps réel, horodatage discret. | Conforme |
| **MessageComposer** | Saisie & Actions | Extension verticale fluide, support du raccourci matériel `Ctrl+Enter`, citation visuelle en réponse, icône morphing (Send/Stop/Call). | Conforme |
| **ApprovalCard** | Carte Grok Bot | Affichage du diff avec colorisation verte/rouge, boîte de commande shell avec bouton copie, 3 boutons d'action. | Conforme |
| **TraceScreen** | Inspection détaillée | Liste chronologique des étapes de pensée (orbe), appels d'outils (icône clé) et commandes exécutées. | Conforme |
| **PairingScreen** | Appairage Termux | Guide des commandes shell PRoot, champ hôte pré-rempli sur `http://127.0.0.1:19222`, test de connectivité interactif. | Conforme |
| **BotEditorScreen** | Édition du profil | Sélecteur visuel interactif des 8 formes géométriques et des 11 teintes, réglage des permissions shell. | Conforme |
| **GroupEditorScreen** | Coordination groupe | Sélection du bot coordinateur et liste des sous-agents rattachés. | Conforme |
| **SettingsScreen** | Préférences | Bascule Thème Clair/Sombre, état détaillé du serveur Termux, mention de licence Apache-2.0. | Conforme |
| **StateScreens** | Écrans d'états | Présentation soignée des états déconnectés avec bouton d'aide pour lancer Termux. | Conforme |

---

## 4. Procédure de Test Manuel sur Appareil Android Physique

1. **Installation de l'APK** :
   - Télécharger l'artefact `app-debug.apk` généré par le workflow GitHub Actions.
   - Installer sur le smartphone Android (`adb install -r app-debug.apk` ou via le gestionnaire de fichiers).

2. **Démarrage de l'hôte dans Termux** :
   ```bash
   /root/CodeStation/start_code_station.sh start
   ```

3. **Vérification de l'Appairage** :
   - Ouvrir Synapse.
   - Si premier lancement, passer l'Onboarding.
   - Dans l'écran Réglages/Appairage, vérifier que l'adresse par défaut est `http://127.0.0.1:19222`.
   - Cliquer sur **Tester la connexion**. Le voyant doit passer au vert : `Connecté (X ms) - v2.28.0`.

4. **Vérification de l'Exécution & Approbation** :
   - Ouvrir la conversation avec le bot principal.
   - Envoyer une invite contenant le mot `run` ou `exec` (ex: `Exécute le script bash de test`).
   - Observer l'animation du ThinkingOrb.
   - Vérifier l'apparition de l'ApprovalCard avec la commande shell proposée et le diff éventuel.
   - Cliquer sur **Autoriser une fois**.
   - Vérifier la continuation immédiate de l'agent et la réponse finale confirmée.
