# Limites Connues et Recommandations Opérationnelles : Agents & Sessions

**Projet** : CodeStation  
**Date** : 2 novembre 2026

---

## 1. Limites Techniques Identifiées

### 1.1 Exécutabilité des CLI dans l'Environnement PRoot ARM64
- **Google Antigravity (`agy`)** :
  Fonctionne parfaitement en mode direct, mais requiert des permissions de sous-processus sans contrainte de sandbox imbriquée (`BypassSandbox: true` lors de l'exécution de commandes automatisées).
- **Freebuff (`freebuff`)** :
  CLI basée principalement sur une interface Terminal TUI interactive. L'adaptateur CAMCS actuel capture les sorties standard mais ne prend pas en charge la négociation complexe des touches de contrôle TUI sans pseudo-terminal PTY alloué.
- **OmniRush (`omnirush`)** :
  Expose un serveur HTTP local et requiert une connectivité stable sur ses endpoints pour les échanges ACP. En cas de forte charge mémoire sur l'appareil Android hôte, le démon peut être suspendu par le gestionnaire OOM.

### 1.2 Isolation SQLite et KSP sous PRoot
- Lors de la compilation KSP sous PRoot sur Android (`Linux-Android`), SQLite JDBC natif tente de charger `libsqlitejdbc.so`. La configuration de `ksp { arg("room.verify.schemas", "false") }` et des options JVM Linux émule l'environnement requis. La compilation et le packaging APK (`assembleDebug`, `assembleRelease`) sont 100% fonctionnels.

### 1.3 Gestion des Déconnexions Réseau ADB
- L'adresse IP et le port du débogage sans fil Android changent périodiquement lors du renouvellement des baux DHCP ou des reconnexions Wi-Fi. Le pipeline de déploiement supporte la spécification dynamique de l'adresse cible.

---

## 2. Recommandations pour l'Équipe et les Évolutions Futures

1. **Purge Automatique des Sessions Inactives** :
   Ajouter une tâche de nettoyage périodique dans CAMCS pour archiver automatiquement les conversations terminées depuis plus de 30 jours afin de préserver la taille de `codync.db`.
2. **Support PTY Complet pour Freebuff** :
   Intégrer une allocation pseudo-terminal (`pty`) dans le runner `freebuff_runner.py` pour étendre les fonctionnalités de la TUI.
3. **Mise à Jour In-App OTA** :
   Conserver la procédure standard d'installation directe via `github_update_service.dart` / service Android avec double vérification SHA-256 et contrôle de package ID (`com.ghdinteractivestudio.codestation`).
