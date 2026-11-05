# DESIGN SYSTEM AUDIT & GUIDE DE PRÉSERVATION GLASSMORPHISM

> **Application** : CodeStation (`com.ghdinteractivestudio.codestation`)  
> **Composants sources** :  
> - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/ui/theme/` (`Color.kt`, `Palette.kt`, `Type.kt`, `Spacing.kt`, `Shape.kt`, `Motion.kt`, `Theme.kt`)  
> - `app/src/main/kotlin/com/ghdinteractivestudio/codestation/ui/components/` (`Glass.kt`, `Chrome.kt`, `DynamicIslandBar.kt`, `DynamicNeuralGraphView.kt`, `ThinkingOrb.kt`, `Controls.kt`)  
> **Statut** : RÉFÉRENCE GRAPHIQUE ABSOLUE ET INVIOLABLE  

---

## 1. Engagement Solennel de Non-Régression Visuelle

L'interface de CodeStation arbore une identité cybernétique et épurée "Frosted Liquid Glassmorphism".  
**Toute modification, amélioration ou ajout fonctionnel doit obligatoirement réutiliser les composants, modificateurs et tokens existants.**

Sont formellement proscrits :
- La modification des constantes de couleurs (`Color.kt`, `Palette.kt`).
- L'introduction d'un thème "Material Design plat" ou standard.
- La suppression des effets de réfraction, des dégradés spéculaires et des biseaux lumineux.
- La modification des typographies et des arrondis d'angles des cartes existantes.

---

## 2. Inventaire Détaillé des Tokens Chromatiques

### 2.1 Palette Obsidian Dark (Thème Principal)
| Token Compose | Valeur Hexadécimale | Usage Exclusif |
| :--- | :--- | :--- |
| `CodeStationDarkBackground` | `#0A0E17` | Fond d'écran global profond |
| `CodeStationDarkSurface` | `#111726` | Surface principale des conteneurs et cartes |
| `CodeStationDarkSurfaceVariant` | `#1B2234` | Surface surélevée (cartes secondaires, volets) |
| `CodeStationDarkSurfaceHighlight`| `#263047` | État hover, sélectionné, contour actif |
| `CodeStationDarkPrimary` | `#00D2FF` | **Cyan Électrique** : boutons d'action primaires, indicateurs d'exécution active |
| `CodeStationDarkSecondary` | `#9D4EDD` | **Violet Astral** : agents multiples, liaisons neuronales |
| `CodeStationDarkAccentGold` | `#FFB703` | Étoile or du Lead Bot (★), statuts d'avertissement |
| `CodeStationDarkSuccess` | `#00E676` | **Vert Électrique / Néon** : validation, tests réussis, agent en ligne |
| `CodeStationDarkWarning` | `#FF9100` | Alerte, seuil approchant, approbation requise |
| `CodeStationDarkError` | `#FF5252` | Échec de tâche, test rejeté, cycle détecté |
| `CodeStationDarkOutline` | `#2E384D` | Bordures fines translucides |
| `CodeStationDarkUserBubble` | `#1A365D` | Bulle utilisateur bleu nuit |

### 2.2 Objet `Palette` (Adaptatif Sombre / Clair)
L'objet `Palette` fournit des accesseurs dynamiques qui s'adaptent selon `LocalDark.current` :
- `Palette.background`
- `Palette.surface`
- `Palette.glassSurface`
- `Palette.glassBorder`
- `Palette.accent` (`#00D2FF` en dark)
- `Palette.electricBlue`
- `Palette.neonGreen`
- `Palette.text`
- `Palette.secondary`
- `Palette.danger`

---

## 3. Composants et Modificateurs Glassmorphism Réutilisables

### 3.1 `Modifier.glass(...)` & `Modifier.frostedLiquidGlass(...)`
Défini dans `Glass.kt` :
- **Principe** :
  - Dégradé spéculaire de bordure (`rimBrush`) simulant la lumière zénithale blanche sur le bord supérieur (alpha 0.70) et douce sur le bas (alpha 0.10).
  - Fond translucide dégradé (`bgBrush`) avec verre dépoli.
  - Découpe propre selon la forme (`clip(shape)`).
- **Styles préexistants** :
  - `GlassStyle.Regular` : Cartes standards.
  - `GlassStyle.Clear` : Arrière-plans subtils.
  - `GlassStyle.Menu` : Menus déroulants et popovers.
  - `GlassStyle.Card` : Cartes de tâches, fiches d'agents.
  - `GlassStyle.Accent` : Cartes d'actions prioritaires avec halo cyan.

### 3.2 Composants d'Écran Spécialisés
1. `GlassMenu` & `GlassMenuItem` : Menus contextuels semi-transparents avec biseau fin de 0.5dp.
2. `GlassAlertDialog` : Boîtes de dialogue modales flottantes avec fond liquide dépoli.
3. `DynamicIslandBar` : Barre supérieure interactive avec animations d'expansion, affichant la mission active, l'agent en cours et l'indicateur de chargement.
4. `DynamicNeuralGraphView` : Visualisation de nœuds et liaisons animées avec pulsation lumineuse pour le graphe de dépendances (DAG).
5. `ThinkingOrb` : Orbe lumineuse animée (particules et onde de pensée) pour les phases de réflexion des modèles IA.
6. `CamcsNewTaskCard` : Carte premium d'initiation d'objectif avec pied de page d'identité CAMCS.

---

## 4. Conventions Typographiques & Spatiales

- **Typographie (`Type.kt`)** :
  - Police système sans-serif optimisée (Inter / Roboto).
  - Échelle hiérarchique : `Type.title1`, `Type.title2`, `Type.title3`, `Type.body`, `Type.caption1`, `Type.caption2`.
  - Style de code à chasse fixe : `FontFamily.Monospace` pour les hashes SHA-256, PIDs, commandes et chemins.
- **Espacements (`Spacing.kt`)** :
  - Micro : `space4` (4dp), `space8` (8dp).
  - Standard : `space12` (12dp), `space16` (16dp).
  - Séparation : `space20` (20dp), `space24` (24dp), `space32` (32dp).
- **Formes & Rayons (`Shape.kt`)** :
  - Cartes et panneaux : `RoundedRectangle(16.dp)` ou `RoundedRectangle(20.dp)`.
  - Pastilles et pilules : `Capsule()`.
  - Menus : `RoundedRectangle(18.dp)`.
  - Boîtes de dialogue : `RoundedRectangle(24.dp)`.

---

## 5. Règle d'Intégration pour Toute Nouvelle Vue

Lors de l'enrichissement des écrans CAMCS (ex: affichage des Quality Gates, des phases et des reprises de session) :
1. **Toujours envelopper dans `Modifier.frostedLiquidGlass()` ou `Modifier.glass(style = GlassStyle.Card)`**.
2. **Utiliser `Palette.electricBlue` (`#00D2FF`) pour l'état actif et `Palette.neonGreen` (`#00E676`) pour l'état validé**.
3. **Utiliser `Palette.text` et `Palette.secondary` pour la hiérarchie textuelle**.
4. **Appliquer les micro-animations tactiles `Modifier.pressScale(pressed)` sur tout élément cliquable**.
