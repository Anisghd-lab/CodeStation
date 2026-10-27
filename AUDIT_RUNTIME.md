# Audit Runtime & État Réel — CodeStation (Phase 11)

**Date :** 27 Octobre 2026  
**Environnement :** Android 16 (OP5E03L1 / PLE110) + Termux + Ubuntu PRoot (aarch64)  
**Dépôt officiel :** `https://github.com/Anisghd-lab/CodeStation.git`  
**Branche :** `main`  
**Commit HEAD audité :** `bc0e50e`  

---

## 1. État des Dépôts et Sources de Vérité

| Répertoire / Entité | État constaté | Source de Vérité | Notes |
|---|---|---|---|
| `/root/CodeStation` | Clone Git complet, branch `main`, tracking `origin/main` | **OUI** | Code source complet de l'application Android native, tests unitaires et intégration. |
| `/root/Codyn/host` | Code source Rust du host (Cargo 1.93.1) | **OUI** | Contient le serveur HTTP/SSE, le moteur ACP et le catalogue de backends. |
| `/usr/local/bin/codestation-host` | Binaire exécutable natif aarch64 (69 MB) | **OUI** | Compilé avec succès depuis `/root/Codyn/host`. |
| `/usr/local/bin/codestation` | Script adaptateur CLI CodeStation | **OUI** | Gère `serve`, `status`, `pair`, `token`, `stop`. |
| `~/.codestation/token` | Jeton d'authentification Bearer (64 chars hex) | **OUI** | Généré localement, protégé (chmod 600). |
| `https://github.com/Anisghd-lab/CodeStation` | Dépôt GitHub distant | **OUI** | Dépôt distant synchronisé, exécute les workflows CI GitHub Actions. |

> [!IMPORTANT]
> Aucune régression : l'intégralité du code existant dans `/root/CodeStation` est préservée. Le dépôt distant n'a pas été écrasé.

---

## 2. Inventaire des Fonctionnalités : Réel vs Simulé vs Absent

### A. Notifications Android
- **Constat :** Dans `StateScreen.kt`, la section *Live updates* et *Status bar* ne contenait qu'un aperçu visuel en mémoire Compose avec un sélecteur de phases fictif (`TaskPhase.Working to "Au travail"`, etc.). Aucun code n'appelait `NotificationManager` ou `NotificationCompat.Builder` pour afficher de véritables notifications système Android dans la barre d'état.
- **Statut :** **INCOMPLET / SIMULATION UI SEULEMENT**.
- **Action requise :** Créer un module natif `CodeStationNotificationManager` et un service d'arrière-plan `CodeStationTaskSyncService` pour émettre et synchroniser de vraies notifications persistantes basées sur les événements SSE du serveur.

### B. Découverte des Agents dans Ubuntu PRoot
- **Constat :** Le host possède un catalogue statique de 46 backends dans `catalog.rs`. Cependant, les statuts réels d'authentification, de version et les causes d'indisponibilité ne sont pas tous résolus dynamiquement.
  - `agy` (Google Antigravity CLI v1.4.8) est présent à `/root/.local/bin/agy`.
  - `opencode` (OpenCode CLI v2.2.0) est présent à `/usr/local/bin/opencode`.
  - `codex` (Codex CLI v0.153.3) est présent à `/data/data/com.termux/files/usr/bin/codex`.
  - `claude` : Absent de PATH.
  - `gemini` : Absent de PATH.
- **Statut :** **FONCTIONNELLEMENT DÉTECTABLE MAIS SANS DÉTAIL DYNAMIQUE DANS L'APP**.
- **Action requise :** Implémenter un moteur de découverte dynamique `AgentDiscoveryService` documentant chemins réels, versions, compatibilité ACP et états d'authentification.

### C. Flux Événements Temps Réel (SSE)
- **Constat :** Le serveur Rust écoute sur `http://127.0.0.1:19222/events?since=0` et émet en temps réel les événements `{"type": "bot", ...}` et `{"type": "entry", ...}`. Cependant, l'application Android Kotlin n'avait pas de client SSE actif pour écouter ce flux et répercuter l'état dans Room.
- **Statut :** **SERVEUR OPÉRATIONNEL / CLIENT ANDROID MANQUANT**.
- **Action requise :** Implémenter `CodeStationEventStreamClient` avec OkHttp (lecture en flux continu de `text/event-stream`), reconnexion automatique avec backoff et injection dans Room et `NotificationManager`.

---

## 3. Plan de Correction Structuré

1. **Phase 1 : Agent Discovery Engine**
   - Créer un inspecteur d'agents documentant chaque CLI détectée (`agy`, `opencode`, `codex`, etc.).
   - Produire `AGENT_DISCOVERY.md`.
2. **Phase 2 : Système de Notifications Android Réelles**
   - Implémenter `CodeStationNotificationManager.kt` : canaux de notification Android 8+, gestion de `POST_NOTIFICATIONS` sur Android 13+, styles de notification persistants, chronomètre en cours, boutons d'action d'approbation et PendingIntent vers la conversation.
   - Produire `NOTIFICATION_ARCHITECTURE.md`.
3. **Phase 3 : Client SSE et Service de Synchronisation Temps Réel**
   - Implémenter `CodeStationEventStreamClient.kt` et `CodeStationTaskSyncService.kt`.
   - Synchroniser les événements du serveur vers Room Database et vers `CodeStationNotificationManager`.
4. **Phase 4 : Tests Automatisés et Validation CI**
   - Ajouter des tests unitaires pour le parsing SSE, la découverte d'agents et le gestionnaire de notifications.
   - Valider la compilation et les tests via GitHub Actions.
5. **Phase 5 : Validation sur Appareil Réel via ADB**
   - Installer l'APK généré.
   - Déclencher une tâche, observer la notification système Android réelle, valider les changements d'état et l'arrêt.
   - Produire `INTEGRATION_TEST_REPORT.md` et mettre à jour `worklog.md`.
