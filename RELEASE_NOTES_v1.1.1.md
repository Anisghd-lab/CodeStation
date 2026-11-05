# CodeStation v1.1.1

Mise à jour corrective et de stabilité majeure : Résolution définitive du routage des modèles Google Antigravity (AGY), alignement complet de l'interface Android native avec la CLI officielle, et fiabilisation du démarrage CAMCS.

### Points Clés de la Version 1.1.1 :

1. **Routage Officiel des Modèles Google Antigravity (AGY)** :
   - Éradication totale des identifiants et modèles fictifs/obsolètes (`gemini-4-argon-medium`, `gemini-4-argon-high`, `claude-opus-5-5-*`).
   - Intégration stricte des 6 modèles réels officiellement reconnus par la CLI `agy` :
     - `gemini-3.8-flash-medium` *(Modèle par défaut actif)*
     - `gemini-3.8-flash-high`
     - `gemini-3.8-flash-low`
     - `claude-sonnet-4-6` *(Thinking)*
     - `claude-opus-4-6-thinking` *(Thinking)*
     - `gpt-oss-120b-medium`
   - Interdiction de toute substitution silencieuse et résolution canonique déterministe avec redirection sécurisée.

2. **Interface Android Native & Widgets Synchronisés** :
   - Mise à jour du catalogue de modèles dans `Bot.kt` et `BotRepositoryImpl.kt` pour n'afficher que les modèles certifiés.
   - Actualisation des widgets d'accueil (`widget_bots_large.xml`, `widget_team_large.xml`, `CodeStationWidgetManager.kt`) affichant l'état prêt avec `gemini-3.8-flash-medium`.

3. **Commandes Slash Dynamiques (`/model` et `/signin`)** :
   - `/model` : Interrogation en direct via format JSON-stream (`agy -p /model`) pour afficher le modèle réellement actif et son niveau d'effort.
   - `/model <id>` : Bascule dynamique du modèle avec persistance immédiate dans `~/.gemini/antigravity-cli/settings.json`.
   - `/signin` : Vérification du jeton OAuth (`antigravity-oauth-token`) et conservation de la session connectée après reconnexion.

4. **Résilience du Moteur CAMCS & Registre d'Identifiants** :
   - Importation PyYAML tolérante avec repli regex natif pour la lecture de `hosts.yml` dans `credential_registry.py`.
   - Démarrage propre de CAMCS Engine sans blocage même si un module externe n'est pas préinstallé.

5. **Synchronisation Automatique de la Mémoire Continue** :
   - Synchronisation périodique et résiliente du dépôt `CodeStation-Memory` avec GitHub sans perte de données locales.
