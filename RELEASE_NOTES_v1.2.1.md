# CodeStation Release Notes v1.2.1 (Build 16)

### Intégration Native des Fournisseurs IA OpenRouter & xKIRO

Cette mise à jour connecte directement les fournisseurs d'inférence distante **OpenRouter** et **xKIRO / Okiro** dans l'application Android native CodeStation, les écrans de configuration et le serveur hôte.

---

### Nouveautés et Améliorations

1. **Intégration Complète des Nouveaux Providers (`ProviderType`)** :
   * Ajout de `ProviderType.OPENROUTER` et `ProviderType.XKIRO` au modèle de données de l'application.
   * Modèles gratuits certifiés immédiatement sélectionnables :
     * **OpenRouter** : `liquid/lfm-2.5-2.6b:free`, `nvidia/nemotron-3.5-lightning:free`, `recursive/flint-v1.0:free`, `dots-studio/dots-3-note-preview:free`, `xingchen-agi/xing4.0-29b-a4b:free`.
     * **xKIRO** : `qwen/qwen3-coder-plus:free` (1M tokens de contexte), `qwen/qwen3.6-27b:free`, `qwen/qwen3.5-plus:free`, `qwen/qwen3.6-max-preview:free`, `qwen/qwen3.5-flash:free`.

2. **Nouveaux Bots Pré-configurés dans le Roster** :
   * **Agent OpenRouter** (`bot-openrouter`) : Spécialiste en ingénierie logicielle autonome propulsé par Liquid LFM / Nemotron.
   * **Agent xKIRO Qwen Coder** (`bot-xkiro`) : Spécialiste en architecture et génération de code haute performance (1 048 576 tokens de contexte).
   * Injection automatique et sécurisée dans la base Room locale (`ensureEssentialBots()`).

3. **Enrichissement des Écrans Android (Glassmorphism)** :
   * **Paramètres (`SettingsScreen`)** : Nouvelle section *« Fournisseurs IA & APIs d'Inférence »* affichant l'état actif, les badges de connexion certifiés et le nombre de modèles accessibles.
   * **Éditeur de Bot (`BotEditorScreen`)** : Nouvelle section dédiée aux fournisseurs cloud avec badges néon vert (`Palette.emeraldAccent`).
   * **Centre de Contrôle CAMCS (`CamcsControlCenterScreen`)** : Visibilité live des statuts `OPERATIONAL` et des quotas dans l'onglet Providers & Services.

4. **Ponts ACP Dédiés sur le Backend Hôte** :
   * Création des ponts `openrouter_acp_bridge.py` et `xkiro_acp_bridge.py` pour un streaming de discussion fluide et temps réel avec `codestation-host`.
   * Enregistrement des harnais dans le catalogue natif Rust de l'hôte.

---

### Fichiers de Téléchargement & Vérification
* `app-release.apk` : APK Android natif signé
* `CodeStation-v1.2.1.apk` : APK numéroté pour archivage direct
* `app-release.apk.sha256` : Empreinte de sécurité SHA-256
