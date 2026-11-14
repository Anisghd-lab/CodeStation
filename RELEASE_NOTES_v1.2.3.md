# CodeStation v1.2.3 — Kimi Code CLI & The Agency (283 Agents Spécialisés)

## 🌟 Nouveautés Majeures

### 1. Intégration Native du CLI Kimi Code (`bot-kimi`)
- **Nouveau Bot Officiel** : `bot-kimi` est désormais intégré au registre officiel des CLI CodeStation (7 CLI opérationnels au total).
- **Moteur & Protocole** : Kimi Code CLI v2.6.1 avec support natif du protocole Agent Client Protocol (ACP) via stdio et exécution directe non-interactive (`-p`).
- **Modèles Supportés** : OpenRouter Flint v1.0 (`openrouter/flint-v1`), Nemotron 3 Ultra (`nvidia/nemotron-3-ultra`) et Kimi Latest.
- **Roster & Sélecteur de Fournisseurs** : Kimi Code est sélectionnable dans l'éditeur de bot (`BotEditorScreen`) et initialisé automatiquement dans la base Room locale (`botDao`).

### 2. Catalogue Interactif des 283 Experts Autonomes Agency
- **Nouvel Écran "The Agency" (`AgencyAgentsScreen.kt`)** :
  - Accessible directement depuis le 2ᵉ onglet de la barre de navigation principale (`[Bots]  [Agents 283]  [État]`).
  - Également intégré en 5ᵉ onglet dans le Centre de Contrôle Multi-Agents CAMCS (`CamcsControlCenterScreen.kt`).
- **Fonctionnalités du Catalogue** :
  - **Recherche instantanée** par nom, rôle, compétence ou mot-clé parmi les 283 spécialistes.
  - **Filtres par Division** (Ingénierie & Dev, Qualité & Tests, Cybersécurité, Architecture, Design UI/UX, Data & AI, GIS & Spatial, Game Dev, etc.).
  - **Fiche détaillée de chaque expert** : rôle, chemin du fichier compétence (`~/.agents/skills/agency-.../SKILL.md`), et bouton "Copier la directive d'activation".

### 3. Méta-Prompt d'Activation Autonome des Agents
- Injection automatique des directives de l'Agency dans le superviseur CAMCS et dans le contexte de chat Rust du Host (`host/src/chat/context.rs`).
- Annonce formelle de chaque spécialiste mobilisé :
  `🎭 [AGENCY ACTIVATION : <Nom de l'Agent> (<Division>)]`

### 4. Design System CodeStation — Exclusivité Dark Mode Obsidian Glass
- Respect absolu de la charte CodeStation : surfaces OLED noires (`#0B0D12`), cartes translucides fumées (`#171B24`), reflets spéculaires `border-white/10`, typographie monospace `JetBrains Mono` et accents néon Cyan (`#00D2FF`) et Bleu électrique (`#438DFF`).
