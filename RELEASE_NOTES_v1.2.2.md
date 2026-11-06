# CodeStation v1.2.2 — Résolution du routage Bot Host & Intégration Robuste OpenRouter/xKIRO

## 🚀 Correctifs et Améliorations Majeures

### 1. Résolution de l'erreur `HTTP 400: {"error":"unknown bot"}`
- **Cause identifiée** : Le serveur backend `codestation-host` résout les identifiants de bots reçus via `POST /api/send` directement depuis la base SQLite locale (`codestation.db` et `codync.db`). Comme `bot-openrouter` et `bot-xkiro` n'y figuraient pas et que `ensure_default_bots()` dans `host/src/hub.rs` ne contenait que les 6 bots CLI historiques, le serveur rejetait la transmission.
- **Résolution** :
  - Mise à jour de `host/src/hub.rs` pour inclure `bot-openrouter` et `bot-xkiro` dans `ensure_default_bots()`.
  - Migration et insertion immédiate des configurations bots dans les bases SQLite locales `/root/.codestation/codestation.db` et `codync.db`.
  - Recompilation optimisée du binaire Rust `codestation` (release) et déploiement dans `/usr/local/bin/codestation`.

### 2. Optimisation du Fournisseur OpenRouter (Modèles 100% Gratuits)
- **Modèle par défaut optimisé** : Migration de `liquid/lfm-2.5-2.6b:free` (soumis à un rate limit global 429) vers le modèle ultra-rapide et stable `nvidia/nemotron-3.5-lightning:free`.
- **Mécanisme de secours (Fallback Chain)** : Détection automatique des erreurs 429 et basculement instantané sur la liste de modèles gratuits certifiés (`nvidia/nemotron-3.5-lightning:free`, `recursive/flint-v1.0:free`).
- **Support des Tokens de Raisonnement (Thinking/Reasoning)** : Gestion automatique de l'extraction des champs `reasoning` pour les modèles d'inférence avancés sans perte de contenu.

### 3. Validation de Transmission E2E
- Validation en direct de `POST /api/send` avec réponse HTTP 200 et streaming de complétion en français validé pour les deux bots :
  - **OpenRouter** : `Bonjour ! Oui, je suis en ligne.`
  - **xKIRO Qwen Coder** : `Oui, xKIRO Qwen Coder est en ligne et prêt.`
