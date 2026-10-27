# Conception & Architecture de Connexion Android ⟷ Termux

Date : 27 Octobre 2026  
Projet : **Synapse** (`com.ghdinteractivestudio.synapse`)  
Environnement d'exécution : Téléphone Android physique, Termux & Ubuntu PRoot  

---

## 1. Topologie Réseau et Environnement d'Exécution

Synapse est conçu spécifiquement pour fonctionner comme l'interface native mobile sur un smartphone Android, connectée directement à l'environnement d'agent IA qui s'exécute localement sur le même appareil, à l'intérieur de **Termux** et d'un conteneur **Ubuntu PRoot**.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        APPAREIL ANDROID PHYSIQUE                       │
│                                                                        │
│   ┌─────────────────────────────┐       ┌───────────────────────────┐  │
│   │       SYNAPSE (APK)         │       │    TERMUX / UBUNTU PROOT  │  │
│   │   Kotlin / Jetpack Compose  │       │                           │  │
│   │  com.ghdinteractivestudio   │       │  Hôte Agent (Port 19222)  │  │
│   │          .synapse           │       │  CodeStation / Codync     │  │
│   └──────────────┬──────────────┘       └─────────────▲─────────────┘  │
│                  │                                    │                │
│                  └────────── loopback (lo) ───────────┘                │
│                         http://127.0.0.1:19222                         │
└────────────────────────────────────────────────────────────────────────┘
```

### Avantages de cette architecture locale
1. **Zéro latence réseau externe** : Les échanges HTTP et SSE s'effectuent sur l'interface loopback (`lo`), offrant des temps de réponse instantanés (< 10 ms).
2. **Confidentialité totale** : Aucun code, invite, token ou réponse d'agent ne transite par un serveur intermédiaire tiers non consenti.
3. **Fonctionnement hors-ligne** : L'écosystème fonctionne même en mode avion, sans connexion Internet extérieure (si des modèles locaux sont configurés) ou via la connexion cellulaire/WiFi pour interroger les LLM distants depuis Termux.

---

## 2. Configuration de Sécurité Android (`Network Security Config`)

Par défaut, Android 9+ (API 28+) bloque tout trafic HTTP en clair (*Cleartext Traffic*). Étant donné que le serveur Termux tourne en local sur `127.0.0.1` sans certificat SSL/TLS (inutile et complexe à renouveler sur un loopback sans nom de domaine), Synapse intègre une configuration de sécurité réseau explicite :

Dans `app/src/main/res/xml/network_security_config.xml` :
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain> <!-- Émulateur Android -->
        <domain includeSubdomains="true">192.168.1.1</domain>
    </domain-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```
Et déclaré dans `AndroidManifest.xml` :
```xml
android:networkSecurityConfig="@xml/network_security_config"
```
Cela permet la communication directe et non chiffrée avec le daemon Termux sur l'hôte local, tout en interdisant le clair vers les domaines publics non sécurisés.

---

## 3. Protocole de Communication & Endpoints

Le serveur local écoute sur le port standard **19222**. Il implémente les routes suivantes :

### 3.1. `GET /health` (Sonde de santé)
- **Authentification** : Aucune (publique sur loopback).
- **Rôle** : Vérifier la disponibilité immédiate du daemon et obtenir sa version.
- **Exemple de réponse JSON** :
  ```json
  {
    "ok": true,
    "version": "2.28.0",
    "busy": false
  }
  ```

### 3.2. `POST /api/hello` (Handshake & Négociation de session)
- **Authentification** : Requise (`Authorization: Bearer <TOKEN>`).
- **Rôle** : Valider le jeton d'appairage et récupérer les métadonnées de l'hôte (nom d'hôte, versions des backends disponibles : Antigravity, Codex, etc.).
- **Exemple de réponse JSON** :
  ```json
  {
    "ok": true,
    "version": "2.28.0",
    "host": "localhost",
    "platform": "linux-arm64",
    "backends": ["Google Antigravity", "OpenAI Codex", "OpenCode"]
  }
  ```

### 3.3. `POST /api/send` (Transmission de consigne utilisateur)
- **Authentification** : Requise (`Authorization: Bearer <TOKEN>`).
- **Corps de la requête** :
  ```json
  {
    "botId": "bot-main-1",
    "text": "Exécute les tests unitaires et analyse le code.",
    "clientNonce": "550e8400-e29b-41d4-a716-446655440000",
    "replyTo": "optional-parent-message-id"
  }
  ```

### 3.4. `POST /api/stop` (Interruption de tour d'agent)
- **Authentification** : Requise (`Authorization: Bearer <TOKEN>`).
- **Corps de la requête** :
  ```json
  {
    "botId": "bot-main-1"
  }
  ```

### 3.5. `POST /api/respondPermission` (Réponse d'approbation)
- **Authentification** : Requise (`Authorization: Bearer <TOKEN>`).
- **Corps de la requête** :
  ```json
  {
    "entryId": "approval-entry-uuid",
    "optionId": "allow_once" // ou "allow_always", "reject_once"
  }
  ```

### 3.6. `GET /events?since=0` (Flux d'événements en streaming SSE)
- **Authentification** : Requise (`Authorization: Bearer <TOKEN>`).
- **Rôle** : Flux continu Server-Sent Events (SSE) notifiant l'application Android des nouvelles pensées, des étapes de raisonnement de l'agent (`step_start`, `step_finish`), des nouvelles requêtes d'approbation (`permission_request`) et des messages terminaux.

---

## 4. Gestion des Jetons & Flux d'Appairage

### 4.1. Emplacement du Jeton dans Termux / Ubuntu PRoot
Dans l'environnement PRoot de Termux, le daemon stocke ou lit son jeton d'accès dans :
`/root/.codync-dev/token` (ou généré lors de la commande de démarrage).

### 4.2. Format de l'URI d'Appairage
Pour appairer instantanément l'application Android sans saisie manuelle de longs tokens hexadécimaux :
```
synapse://pair?token=<TOKEN_SECRET>&host=http://127.0.0.1:19222
```
Synapse prend également en charge le schéma alternatif pour une interopérabilité transparente :
```
codync://pair?token=<TOKEN_SECRET>&host=http://127.0.0.1:19222
```

### 4.3. Procédure Utilisateur Recommandée
1. Ouvrir Termux.
2. Démarrer le serveur local :
   ```bash
   /root/CodeStation/start_code_station.sh start
   ```
3. Obtenir l'URI d'appairage ou le jeton :
   ```bash
   cat /root/.codync-dev/token
   # ou
   /root/CodeStation/start_code_station.sh pair
   ```
4. Dans Synapse, coller le token ou l'URI dans l'écran **Appairage** et cliquer sur **Tester & Enregistrer**.
5. L'indicateur wifi dans l'en-tête de Synapse passe immédiatement au vert avec la latence en millisecondes affichée.

---

## 5. Résilience et Mode Déconnecté

L'application Synapse applique le principe de tolérance aux pannes locale :
- Toute la structure (bots, historique des conversations, approbations en attente) est persistée localement dans une base de données **Room SQLite** chiffrée et ultra-rapide.
- Si le serveur Termux est momentanément arrêté ou redémarré :
  - L'application reste totalement réactive et navigable.
  - Un badge visuel discret `Termux hors-ligne` informe l'utilisateur.
  - Dès que le daemon est relancé dans Termux, la sonde de santé reconnecte automatiquement le flux sans nécessiter de redémarrage de l'application.
