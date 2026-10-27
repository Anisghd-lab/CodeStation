# Audit de Parité UI/UX : Codync vs Synapse

Date : 27 Octobre 2026  
Projet : **Synapse** (`com.ghdinteractivestudio.synapse`)  
Référence analysée : `/root/Codyn/apps/android` (Dépôt officiel Android Codync)  
Auteur : Ingénieur Senior Android Jetpack Compose  

---

## 1. Résumé Exécutif & Contexte

L'objectif de cet audit est d'évaluer et de garantir la stricte parité visuelle, ergonomique et interactive entre l'application officielle Android de **Codync** (située dans `/root/Codyn/apps/android`) et **Synapse** (`/root/synapse`), tout en préservant l'identité autonome et le package indépendant (`com.ghdinteractivestudio.synapse`) requis par les directives du projet.

Contrairement aux premières interprétations qui imaginaient un style "cyberpunk" fluo ou des dégradés violets/cyan, l'inspection directe des fichiers sources réels de Codync (`apps/android/design/src/main/kotlin/dev/codync/design/theme/Palette.kt`, `apps/android/app/src/main/kotlin/dev/codync/app/ui/`) a révélé un design system hautement raffiné, inspiré de l'esthétique minimaliste d'Apple :
- Un fond monochrome pur : noir absolu (`#0A0A0A`) en mode sombre, blanc pur (`#FFFFFF`) en mode clair.
- Des bulles de chat neutres (`#1E1E1E` pour l'agent, `#262626` pour l'utilisateur) sans saturation inutile.
- La couleur est strictement isolée aux avatars des bots (palette de 11 teintes expressives) et aux signaux d'état fonctionnels (ambre `#F59E0B` pour l'attention/demande d'approbation, rouge `#EF4444` pour le danger).
- Une typographie Inter variable avec compensation optique du crénage dynamique.
- Des coins adoucis continus (squircle et capsules).
- Une bibliothèque de 172 icônes vectorielles inspirées de SF Symbols.
- Un avatar vivant animé en 3D avec éclairage sphérique et regard dynamique.
- Un orbe géométrique 3D wireframe pour représenter la réflexion de l'agent.

Synapse a été entièrement restructuré pour intégrer 100% de ces règles et composants.

---

## 2. Parité du Design System

### 2.1. Palette de Couleurs (Tokens Monochromes & Sémantiques)

| Token Codync | Valeur Sombre (Hex) | Rôle dans l'UI | Statut Synapse |
|---|---|---|---|
| `background` | `#0A0A0A` | Fond d'écran principal (noir profond) | ✅ Implémenté à l'identique |
| `surface` | `#141414` | Cartes, conteneurs, barres supérieures | ✅ Implémenté à l'identique |
| `bubbleAgent` | `#1E1E1E` | Bulle de message des agents | ✅ Implémenté à l'identique |
| `bubbleUser` | `#262626` | Bulle de message de l'utilisateur | ✅ Implémenté à l'identique |
| `border` | `rgba(255,255,255, 0.08)` | Bordure subtile de séparation | ✅ Implémenté à l'identique |
| `borderStrong` | `rgba(255,255,255, 0.16)` | Bordure accentuée des champs de saisie | ✅ Implémenté à l'identique |
| `text` | `#FFFFFF` | Texte principal haute lisibilité | ✅ Implémenté à l'identique |
| `secondary` | `rgba(255,255,255, 0.65)` | Sous-titres, horodatages, métadonnées | ✅ Implémenté à l'identique |
| `tertiary` | `rgba(255,255,255, 0.40)` | Placeholders, textes désactivés | ✅ Implémenté à l'identique |
| `accent` | `#FFFFFF` (sombre) / `#0A0A0A` (clair) | Bouton d'action principal | ✅ Implémenté à l'identique |
| `onAccent` | `#0A0A0A` (sombre) / `#FFFFFF` (clair) | Texte / icône sur bouton accent | ✅ Implémenté à l'identique |
| `accentDim` | `rgba(255,255,255, 0.08)` | Fond des boutons secondaires | ✅ Implémenté à l'identique |
| `warning` / `attention` | `#F59E0B` | Ambre d'alerte, badge `needsInput` | ✅ Implémenté à l'identique |
| `danger` | `#EF4444` | Rouge critique, refus, action destructive | ✅ Implémenté à l'identique |
| `added` | `#10B981` | Vert pour les ajouts dans le diff | ✅ Implémenté à l'identique |
| `removed` | `#EF4444` | Rouge pour les suppressions dans le diff | ✅ Implémenté à l'identique |
| `codeBackground` | `#0F0F0F` | Fond des blocs de code et commandes shell | ✅ Implémenté à l'identique |

### 2.2. Typographie & Crénage Optique

Dans Codync officiel (`Type.kt`) :
- Utilise la police variable Inter (`R.font.inter`).
- Implémente une formule de crénage optique dynamique calquée sur SF Pro :
  $$\text{trackingEm} = -0.022 \times \mathrm{clamp}\left(\frac{\text{pointSize}}{16}, 0.6, 1.4\right)$$
- Fournit 10 niveaux typographiques : `titleLarge` (34sp), `titleMedium` (24sp), `titleSmall` (20sp), `headline` (17sp Bold), `bodyLarge` (17sp), `bodyMedium` (15sp), `bodySmall` (13sp), `caption` (11sp), `code` (13sp monospace), `codeSmall` (11sp monospace).

Dans Synapse :
- Police variable `inter.ttf` (879 Ko) installée dans `app/src/main/res/font/inter.ttf`.
- La classe `Type.kt` calcule exactement ce crénage optique et expose les styles via `Typography`.

### 2.3. Formes & Continuité de Courbure (Continuous Smoothing)

- Définition des composants `Capsule()` et `RoundedRectangle(radius)` dans `Shape.kt`.
- Rayons standardisés :
  - Extra Small (badges) : 6.dp
  - Small (boutons compacts, tags) : 10.dp
  - Medium (champs de saisie, cartes compactes) : 14.dp
  - Large (bulles de message, cartes de bot) : 20.dp
  - Extra Large (feuilles modales, carte d'approbation) : 28.dp
  - Full (boutons pilule, indicateurs d'état) : 999.dp

### 2.4. Physique des Animations (Motion)

Dans Codync officiel (`Motion.kt`) :
- `DURATION_HOVER = 150ms` (`FastOutSlowInEasing`)
- `DURATION_FADE = 180ms` (`LinearOutSlowInEasing`)
- `DURATION_PRESS = 100ms` (`FastOutLinearInEasing`)
- `DURATION_MORPH = 260ms` (`CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)`)
- `DURATION_LAYOUT = 220ms`
- `DURATION_CONVERSATION = 300ms`
- `DURATION_PRESENT = 240ms`
- `PRESS_SCALE = 0.98f` : Micro-rebond haptique visuel lors de l'appui sur un bouton ou une carte.

Synapse intègre strictement ces constantes dans `com.ghdinteractivestudio.synapse.ui.theme.Motion` et les applique aux modificateurs cliquables.

---

## 3. Composants Graphiques Avancés

### 3.1. CharacterAvatar (L'avatar vivant des bots)
- **8 Formes géométriques authentiques** :
  1. `blob` : Forme organique à 4 lobes asymétriques.
  2. `pebble` : Galet poli arrondi.
  3. `squircle` : Super-ellipse à courbure continue.
  4. `tablet` : Rectangle vertical arrondi type gélule.
  5. `wedge` : Forme biseautée adoucie.
  6. `hex` : Hexagone adouci à 6 sommets arrondis.
  7. `cloud` : Nuage à 3 dômes harmonieux.
  8. `teardrop` : Goutte d'eau stylisée.
- **Rendu 3D sphérique** :
  - Gradient radial simulant une lumière en coin supérieur gauche (`Color.White.copy(alpha = 0.35f)`).
  - Ombrage doux en coin inférieur droit.
- **Regard vivant et clignement** :
  - Deux pupilles vectorielles animées.
  - Direction du regard (`gazeOffset`) réagissant subtilement à l'activité de l'agent.
  - Clignement périodique naturel (`blinkScaleY` descendant de 1.0f à 0.1f toutes les 4 secondes).

### 3.2. ThinkingOrb (L'orbe géométrique 3D de réflexion)
- Rendu vectoriel sur `Canvas` d'un polyèdre 3D rotatif.
- 12 sommets projetés via matrice de rotation tridimensionnelle ($X, Y, Z$).
- 24 arêtes reliées en fil de fer avec estompage de profondeur (`alpha` fonction de la coordonnée $Z$).
- Remplace avantageusement les indicateurs génériques de chargement circulaire Material 3.

### 3.3. ApprovalCard (Carte de choix d'approbation)
- Carte modale et intégrée au flux de discussion pour les demandes d'intervention (exécution Bash, modification de fichier, accès réseau).
- Affichage du diff avec colorisation des lignes ajoutées (`#10B981`) et supprimées (`#EF4444`).
- Boîte de commande shell avec bouton de copie et style terminal.
- 3 Actions tactiles bien différenciées :
  1. **Autoriser une fois** (`PrimaryButton` avec fond blanc contrasté).
  2. **Toujours autoriser** (`SecondaryButton` avec contour subtil).
  3. **Refuser** (`SecondaryButton` avec couleur sémantique `danger`).

### 3.4. MessageComposer (Compositeur de message)
- Champ de saisie extensible avec support du retour à la ligne.
- Raccourci clavier de validation : `Ctrl+Enter` / `Meta+Enter` (reconnaissance physique des claviers externes sur Android).
- Bouton "+" en verre pour les pièces jointes (fichiers, images, contexte).
- Bannière de citation en réponse (`QuotedReply`) avec extrait de texte et bouton de fermeture.
- Bouton d'action à morphing dynamique :
  - État inactif / saisie : Bouton "Envoyer" (`sf_arrow_up`).
  - État agent actif : Bouton "Arrêter" (`sf_stop_fill`).
  - État vocal : Bouton "Appel" (`sf_phone_fill`).

### 3.5. Chrome & GlassStage
- `GlassStage` : Conteneur principal gérant le défilement bord à bord sans coupure avec estompage doux aux bordures (`ScrollEdge`).
- `ScreenHeader` & `ModalHeader` : En-têtes épurés avec titre centré ou aligné, bouton retour standardisé (`sf_arrow_left`) et actions d'en-tête.
- `GlassTabBar` : Barre de navigation inférieure flottante à effet de verre, avec indicateurs actifs discrets.

---

## 4. Inventaire Comparatif des 11 Écrans

| Écran | Rôle & Fonctionnalités | Implémentation Codync | Implémentation Synapse | Statut |
|---|---|---|---|---|
| **1. RosterScreen** | Liste des bots, statut en direct, tri épinglés/lead, recherche, accès aux réglages et créations. | `RosterView.kt` | `RosterScreen.kt` | ✅ Parité Totale |
| **2. ChatScreen** | Flux de messages, bulles grises, thinking orb, cards d'approbation inline, composer. | `ChatView.kt` | `ChatScreen.kt` | ✅ Parité Totale |
| **3. PairingScreen** | Connexion Termux/PRoot local, saisie token, scan URI `synapse://pair`, aide commande. | `PairingView.kt` | `PairingScreen.kt` | ✅ Parité Totale |
| **4. ApprovalsScreen** | Revue globale de toutes les demandes en attente sur l'ensemble des agents. | `ApprovalsView.kt` | `ApprovalsScreen.kt` | ✅ Parité Totale |
| **5. TraceScreen** | Déroulement pas-à-pas de l'exécution, pensées cachées, appels d'outils, logs Bash. | `TraceView.kt` | `TraceScreen.kt` | ✅ Parité Totale |
| **6. BotEditorScreen** | Configuration bot : nom, avatar, forme, couleur, modèle IA, permissions, workspace. | `BotEditorView.kt` | `BotEditorScreen.kt` | ✅ Parité Totale |
| **7. GroupEditorScreen** | Création de groupes multi-agents, sélection du bot coordinateur et des membres. | `GroupEditorView.kt` | `GroupEditorScreen.kt` | ✅ Parité Totale |
| **8. SettingsScreen** | Configuration hôte Termux, jeton, bascule thème clair/sombre, vider le cache, À propos. | `SettingsView.kt` | `SettingsScreen.kt` | ✅ Parité Totale |
| **9. OnboardingScreen** | Accueil premier lancement, présentation des capacités multi-agents locales. | `OnboardingView.kt` | `OnboardingScreen.kt` | ✅ Parité Totale |
| **10. StateScreens** | Écrans d'états : hors-ligne, erreur réseau, recherche vide, chargement initial. | `States.kt` | `StateScreens.kt` | ✅ Parité Totale |
| **11. AccountModal** | Changement de profil local, réinitialisation des clés et sessions. | `AccountView.kt` | Intégré dans `RosterScreen` & `Settings` | ✅ Parité Totale |

---

## 5. Respect des Directives de Marque & Licences

Conformément à la licence Apache-2.0 du projet de référence et aux directives de l'utilisateur :
1. **Nom de marque indépendant** : L'application est intitulée **Synapse** et n'utilise pas le nom ou la marque Codync pour son packaging public.
2. **Package Android standardisé** : `com.ghdinteractivestudio.synapse`.
3. **Attribution de licence** : La notice légale et les en-têtes de licence Apache-2.0 sont scrupuleusement respectés.
4. **Graphisme original** : L'icône de lanceur et les assets de marque de Synapse sont originaux, nets et optimisés pour Android Adaptive Icons (fond monochrome et masque vectoriel).
