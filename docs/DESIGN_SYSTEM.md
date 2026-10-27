# DESIGN_SYSTEM.md — Système de Design & Tokens Visuels de Synapse

> **Projet** : Synapse (`com.ghdinteractivestudio.synapse`)  
> **Composant UI** : Jetpack Compose + Material 3 Personnalisé  
> **Date** : 27 Octobre 2026

---

## 1. Philosophie & Principes Directeurs

Le design system de **Synapse** allie l'ergonomie fonctionnelle d'un centre de commande d'agents IA à l'élégance visuelle d'un environnement cybernétique moderne. Il s'inspire des meilleures qualités d'outils de pointe (contraste lisible, densité d'information optimisée, micro-animations discrètes) tout en respectant scrupuleusement les guidelines Android Material 3 :

1. **Hiérarchie de Surface Claire** : Distinction visuelle nette entre le fond d'écran, les cartes de contenu, les bulles de discussion et les commandes flottantes.
2. **Accents Néon Subtils** : Le Cyan Électrique (`#00D2FF`) et le Violet Astral (`#7928CA`) attirent l'attention sur les statuts actifs sans fatiguer les yeux.
3. **Typographie Rigoureuse** : Échelle typographique harmonieuse basée sur les polices système sans-serif, optimisée pour la lecture de code technique et de prose Markdown.
4. **Accessibilité & Insets** : Respect des zones tactiles minimales (48dp), gestion native des barres système (edge-to-edge avec `WindowInsets`), support du contraste élevé et du mode sombre/clair.

---

## 2. Palette Chromatique

### 2.1 Thème Sombre Obsidian (Défaut)
```kotlin
// Palette Dark Synapse
val SynapseDarkBackground       = Color(0xFF0A0E17) // Fond d'écran ultra-sombre
val SynapseDarkSurface          = Color(0xFF111726) // Surface principale des cartes
val SynapseDarkSurfaceVariant   = Color(0xFF1B2234) // Surface surélevée (bulles bot, conteneurs)
val SynapseDarkSurfaceHighlight = Color(0xFF263047) // Surface interactive / hover / sélection

val SynapseDarkPrimary          = Color(0xFF00D2FF) // Cyan Électrique (actions primaires, focus)
val SynapseDarkPrimaryVariant   = Color(0xFF0098BA)
val SynapseDarkOnPrimary         = Color(0xFF001F29)

val SynapseDarkSecondary        = Color(0xFF9D4EDD) // Violet Astral (groupes, multi-agents)
val SynapseDarkSecondaryVariant = Color(0xFF7209B7)
val SynapseDarkOnSecondary       = Color(0xFFFFFFFF)

val SynapseDarkAccentGold       = Color(0xFFFFB703) // Étoile or pour le Lead Bot (★)
val SynapseDarkSuccess          = Color(0xFF00E676) // Statut en ligne, approbation validée
val SynapseDarkWarning          = Color(0xFFFF9100) // Attention, délai dépassé, avertissement
val SynapseDarkError            = Color(0xFFFF5252) // Erreur, déconnexion, rejet d'approbation

val SynapseDarkTextPrimary      = Color(0xFFF1F5F9) // Texte principal à fort contraste
val SynapseDarkTextSecondary    = Color(0xFF94A3B8) // Texte secondaire, horodatages, sous-titres
val SynapseDarkTextTertiary     = Color(0xFF64748B) // Métadonnées discrètes, placeholders
val SynapseDarkOutline          = Color(0xFF2E384D) // Bordures subtiles de cartes
val SynapseDarkUserBubble       = Color(0xFF1E3A8A) // Bulle message utilisateur (Bleu profond)
```

### 2.2 Thème Clair Platine
```kotlin
// Palette Light Synapse
val SynapseLightBackground       = Color(0xFFF8FAFC) // Fond platine doux
val SynapseLightSurface          = Color(0xFFFFFFFF) // Blanc pur pour les cartes
val SynapseLightSurfaceVariant   = Color(0xFFF1F5F9) // Gris clair surélevé
val SynapseLightSurfaceHighlight = Color(0xFFE2E8F0) // Surface sélectionnée

val SynapseLightPrimary          = Color(0xFF0284C7) // Cyan Profond / Bleu Océan
val SynapseLightOnPrimary         = Color(0xFFFFFFFF)

val SynapseLightSecondary        = Color(0xFF7C3AED) // Violet Indigo
val SynapseLightOnSecondary       = Color(0xFFFFFFFF)

val SynapseLightAccentGold       = Color(0xFFD97706) // Étoile or ambrée
val SynapseLightSuccess          = Color(0xFF16A34A)
val SynapseLightWarning          = Color(0xFFEA580C)
val SynapseLightError            = Color(0xFFDC2626)

val SynapseLightTextPrimary      = Color(0xFF0F172A) // Noir charbon
val SynapseLightTextSecondary    = Color(0xFF475569) // Gris ardoise
val SynapseLightTextTertiary     = Color(0xFF94A3B8)
val SynapseLightOutline          = Color(0xFFCBD5E1) // Ligne de séparation
val SynapseLightUserBubble       = Color(0xFFDBEAFE) // Bulle utilisateur claire
```

---

## 3. Typographie

L'échelle typographique s'appuie sur `Typography` de Material 3 :
* **Display Large / Medium** : 28sp / 24sp, SemiBold, pour les en-têtes d'onboarding et titres majeurs.
* **Title Large / Medium** : 20sp / 16sp, Medium / SemiBold, pour les noms d'agents dans le roster et en-tête de chat.
* **Body Large / Medium** : 15sp / 14sp, Regular, hauteur de ligne 1.45, pour les messages et descriptions.
* **Label Medium / Small** : 12sp / 11sp, SemiBold / Medium, pour les badges de statut, horodatages et pastilles.
* **Monospace Code** : 13sp, police à chasse fixe (`FontFamily.Monospace`), pour les commandes de shell, chemins de fichiers et extraits JSON.

---

## 4. Échelle des Espacements, Rayons & Élévations

### 4.1 Espacements (Spacing Tokens)
* `space4` : 4dp (Micro-intervalles, pastilles)
* `space8` : 8dp (Écart standard entre icône et libellé, marges de cartes d'outils)
* `space12` : 12dp (Marges internes compactes de bulles de chat)
* `space16` : 16dp (Marge latérale par défaut sur mobile, padding des écrans)
* `space20` : 20dp (Séparations de sections dans les formulaires)
* `space24` : 24dp (Espace majeur entre blocs de contenu)
* `space32` : 32dp (Espacement des en-têtes majeurs)

### 4.2 Rayons de Courbure (Corner Radii)
* `radiusSmall` : 8dp (Boutons compacts, étiquettes, chips de sélection)
* `radiusMedium` : 14dp (Cartes de bots, champs de saisie, conteneurs d'outils)
* `radiusLarge` : 20dp (Bulles de discussion principales, cartes d'approbation)
* `radiusExtraLarge` : 28dp (Boîtes de dialogue, pilules d'état, feuilles modales)
* `radiusFull` : 9999dp (Boutons d'action circulaires, avatars, badges d'état)

### 4.3 Élévations
* `elevationFlat` : 0dp (Bordure subtile `outlineVariant` de 1dp)
* `elevationLow` : 2dp (Cartes du roster au repos)
* `elevationMedium` : 4dp (Menu d'actions contextuel, barre de saisie flottante)
* `elevationHigh` : 8dp (Feuille modale inférieure `ModalBottomSheet`, popups)

---

## 5. Micro-Animations & Retours Haptiques

* **Pulsation d'Activité** : L'orbe de statut du bot (`ThinkingOrb`) utilise une pulsation d'échelle continue (`0.9x` à `1.15x`) et d'opacité (`0.7` à `1.0`) cadencée à 1 200 ms via `rememberInfiniteTransition()`.
* **Arrivée de Message** : Animation d'entrée fluide avec glissement vers le haut (décalage 10dp) et fondu d'opacité (durée 220 ms, courbe cubique standard).
* **Respect de "Reduce Motion"** : Si l'utilisateur a activé la réduction des animations dans les réglages système, les transitions sont instantanées.

---

## 6. Identité de l'Icône de Lanceur (Launcher Icon)

L'icône originale de **Synapse** a été spécialement dessinée au format Vectoriel XML Android :
1. `ic_launcher_background.xml` : Fond sombre dégradé centré avec maillage néon discret.
2. `ic_launcher_foreground.xml` : Forme géométrique originale représentant 4 nœuds synaptiques reliés par des faisceaux électriques convergeant vers un noyau central lumineux en forme de diamant.
3. `ic_launcher_monochrome.xml` : Version à contour plat pour l'intégration automatique dans le thème monochrome d'Android 13+.
