# CodeStation Release Notes v1.1.3 (Build 14)

### Nouveautés et Améliorations de l'Expérience Utilisateur

* **Réduction automatique des longs messages envoyés par l'utilisateur** :
  * Les messages volumineux rédigés par l'utilisateur (longues invites, blocs de code, journaux d'erreurs) sont désormais bornés en hauteur (max 135dp) avec un dégradé de fondu subtil (`Liquid Frosted Gradient`).
  * Un bouton élégant **« Afficher la suite »** (avec icône `chevron.down`) permet de déplier le message complet d'un simple toucher avec une animation fluide `animateContentSize(Motion.layout())`.
  * Lorsque le message est déplié, l'option **« Afficher moins »** (`chevron.up`) permet de le replier à tout moment pour préserver la lisibilité de la conversation.

* **Harmonisation complète du branding CodeStation** :
  * Remplacement intégral des composants et commutateurs résiduels (`CodyncSwitch` -> `CodeStationSwitch`) dans tous les écrans Android.
  * Base de données unifiée sur `codestation.db` avec lien de compatibilité transparent.

* **Capitalisation Mémoire Collective** :
  * Synchronisation en continu des résolutions de bugs, des interactions CLI et des échanges inter-agents (`CodeStation-Memory`).
