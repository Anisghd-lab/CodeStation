# CAMCS v2.0 — Registre des Services Externes & Moniteur de Santé API

## 1. Vue d'Ensemble du Registre

Le composant central `ExternalServiceRegistry` (`scripts/camcs/external_service_registry.py`) supervise l'ensemble des intégrations tierces accessibles depuis l'environnement Ubuntu PRoot ARM64.

Aucun service n'est déclaré `CONNECTED` sur la simple présence d'un fichier de configuration ou d'une clé. Un service n'obtient le statut `CONNECTED` qu'après **l'exécution réussie d'une sonde HTTP physique non destructive**.

---

## 2. Matrice Complète des Services Connectés

| Service | Endpoint Officiel | Type Auth | Statut Actuel | Scopes / Permissions Vérifiées | Latence Réelle | Quotas / Limites |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **GitHub API & CLI** | `https://api.github.com` | OAuth2 Bearer (`~/.config/gh/hosts.yml`) | **`CONNECTED`** | `gist`, `read:org`, `repo`, `workflow` | ~550 ms | 4985 / 5000 req/h |
| **OpenRouter API** | `https://openrouter.ai/api/v1` | API Key (`OPENROUTER_API_KEY`) | **`CONNECTED`** | `is_free_tier:true` | ~397 ms | 48 / 50 requêtes/j (Gratuit) |
| **xKIRO / Okiro API** | `https://api.xkiro.com/v1` | Bearer Key (`~/.secrets/xkiro-key`) | **`CONNECTED`** | `models:read`, `chat:completions` | ~380 ms | Illimité (Tier Qwen3 gratuit) |
| **OpenCode Inference Tier** | `https://opencode.ai/inference` | API Key (`~/.secrets/opencode-key`) | **`CONNECTED`** | `tier:free`, `local_cli_routing` | ~45 ms | Quota local CLI |
| **Cloudflare R2 Object Storage** | `https://7b2260ba6e6ea12f6271c9e8e78bc391.r2.cloudflarestorage.com` | S3 Auth v4 KeySet (`/root/AGENTS.md`) | **`CONNECTED`** | `s3:PutObject`, `s3:GetObject`, `s3:ListBucket` | ~68 ms | Stockage persistant artefacts |

---

## 3. Détails d'Implémentation des Sondes de Santé (`APIHealthMonitor`)

### 3.1. Sonde GitHub API
- **Endpoint sondé** : `GET https://api.github.com/user`
- **En-têtes transmis** :
  ```http
  Authorization: Bearer <masked>
  User-Agent: CodeStation-CAMCS/2.0
  Accept: application/vnd.github.v3+json
  ```
- **Métriques extraites** :
  - `X-OAuth-Scopes` : Détection automatique des droits accordés (`repo`, `workflow`, etc.).
  - `X-RateLimit-Remaining` et `X-RateLimit-Limit` : Contrôle préventif de saturation.

### 3.2. Sonde OpenRouter API
- **Endpoint sondé** : `GET https://openrouter.ai/api/v1/auth/key`
- **Validation** :
  - Vérification du champ JSON `data.is_free_tier == true`.
  - Contrôle du sous-objet `free_model_daily_requests` (`limit: 50`, `remaining: 48`).
  - Alerting immédiat si le solde payant non nul était débité (garantie de coût $0.00).

### 3.3. Sonde xKIRO / Okiro API
- **Endpoint sondé** : `GET https://api.xkiro.com/v1/models`
- **Validation** :
  - Joignabilité du cluster haute performance Qwen.
  - Disponibilité des modèles d'architecture : `qwen/qwen3-coder-plus:free`, `qwen/qwen3.6-27b:free`.

### 3.4. Sonde Cloudflare R2
- **Endpoint sondé** : Endpoint compatible AWS S3 API de Cloudflare.
- **Paramètres validés** :
  - Bucket cible dédié : `camcs-artifacts`.
  - Signature SHA-256 des fichiers livrables et archivage des releases.

---

## 4. Persistance SQLite & Moniteur Embarqué

Les résultats des sondes sont persistés dans la table `external_service_stats` de `/root/.codestation/camcs.db` :

```sql
CREATE TABLE IF NOT EXISTS external_service_stats (
    service_id TEXT PRIMARY KEY,
    name TEXT,
    endpoint TEXT,
    status TEXT,
    is_authenticated INTEGER,
    scopes_json TEXT,
    rate_limit_remaining INTEGER,
    rate_limit_limit INTEGER,
    rate_limit_reset REAL,
    latency_ms REAL,
    last_checked REAL,
    last_error TEXT,
    capabilities_json TEXT,
    free_models_json TEXT,
    successful_ops INTEGER,
    failed_ops INTEGER
);
```

Une mise en cache avec un temps de rétention de 45 secondes (`_probe_ttl = 45.0s`) évite de saturer les quotas lors d'appels répétés depuis l'interface Android, tout en garantissant des données toujours fraîches.
