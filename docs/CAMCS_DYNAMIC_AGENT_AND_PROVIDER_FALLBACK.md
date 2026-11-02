# Spécification de la Fabrique Dynamique d'Agents et de la Cascade de Fallback
**CodeStation Autonomous Multi-Agent Collaboration System**
*Règle d'Or : Modèles 100% Gratuits & Autonomie Totale*

---

## 1. La Dynamic Agent Factory (`AgentFactory`)

Contrairement aux architectures rigides présentant une liste figée d'agents, la **Dynamic Agent Factory** CAMCS instancie les agents à la volée, en stricte adéquation avec les tâches réelles découpées par le planificateur de DAG.

### A. Rôles Spécialisés Supportés
1. **Lead Architect** : Décomposition, modélisation, spécifications techniques, choix d'architecture.
2. **Android Developer** : Développement Kotlin, Jetpack Compose, respect du package `com.ghdinteractivestudio.*`.
3. **Backend Developer** : Développement Python/Rust/Node, APIs REST, structures de données, algorithmes.
4. **Rust Developer** : Composants système haute performance, sécurité mémoire native.
5. **Web Researcher** : Veille documentaire, documentation officielle, extraction de spécifications.
6. **API Integrator** : Conception de connecteurs d'API tiers, gestion des webhooks et flux de données.
7. **Test Engineer** : Écriture de tests unitaires, couverture de code, protocoles TDD.
8. **Security Auditor** : Analyse de vulnérabilités, fuite de secrets, conformité sandbox.
9. **Quality Validator** : Vérification des livrables, calcul des checksums SHA-256.
10. **Debugging Agent** : Diagnostic de traces d'erreur, analyse de crashs, remédiation ciblée.
11. **Documentation Specialist** : Rédaction des manuels, guides d'architecture et README.

### B. Détection et Injection des Compétences d'Environnement (Skills)
Au démarrage, l'`AgentFactory` scanne les répertoires de compétences du système hôte :
- `/root/.gemini/config/plugins/ecc/skills`
- `/root/.agents/skills`
- `/root/.gemini/replica-skill`

Les compétences adaptées au profil de la tâche (ex: `android-clean-architecture`, `tdd-workflow`, `security-review`) sont automatiquement attachées à la charge utile de l'agent.

---

## 2. Registre des Fournisseurs API & Règle des Modèles 100% Gratuits

CAMCS intègre un `ProviderRegistry` (`scripts/camcs/provider_registry.py`) qui régule l'ensemble des inférences externes.

### A. Règle Inviolable : Modèles Gratuits Uniquement
Toute inférence mobilisée par CAMCS doit satisfaire :
- Soit un identifiant de modèle suffixé par `:free` (ex: `qwen/qwen3-coder-plus:free`, `liquid/lfm-2.5-2.6b:free`, `recursive/flint-v1.0:free`).
- Soit une tarification officielle confirmée à `$0.00 / prompt` et `$0.00 / completion` via l'API de découverte des modèles.
- Soit une allocation locale gratuite (`opencode-inference`).

Tout appel à un modèle payant ou non vérifié déclenche une exception de sécurité immédiate (`ERREUR_SECURITE`) et annule la requête avant tout départ sur le réseau.

### B. Fournisseurs Réellement Connectés

| Fournisseur | Mécanisme de Clé / Auth | Modèles Gratuits Actifs | Usage Recommandé |
|---|---|---|---|
| **OpenRouter** | `OPENROUTER_API_KEY` (Environnement) | `recursive/flint-v1.0:free`<br>`liquid/lfm-2.5-2.6b:free`<br>`nvidia/nemotron-3.5-lightning:free` | Documentation, tests rapides, recherche web légère. |
| **xKIRO** | Clé `/root/.secrets/xkiro-key` | `qwen/qwen3-coder-plus:free`<br>`qwen/qwen3.6-27b:free`<br>`qwen/qwen3.5-plus:free`<br>`qwen/qwen3.7-max:free` | Génération de code complexe, architecture, revue. |
| **OpenCode Inf.** | Clé `/root/.secrets/opencode-key` | `jev-1.13-free`<br>`glm-5.3-flash-free` | Inférence autonome directe, validation logique. |

---

## 3. Stratégie de Fallback et Cascade de Remplacement

Si une ressource d'exécution sélectionnée devient indisponible lors d'une mission, CAMCS applique un algorithme de substitution dynamique en 4 niveaux :

```
                  [Tâche Prête pour Exécution]
                                │
                                ▼
         ┌──────────────────────────────────────────────┐
         │ Niveau 1 : CLI Local Principal Opérationnel  │
         │ (ex: Google Antigravity ou CodeBuddy)         │
         └──────────────────────┬───────────────────────┘
                                │ Échec ou Indisponible ?
                                ▼
         ┌──────────────────────────────────────────────┐
         │ Niveau 2 : CLI Local Alternatif Opérationnel │
         │ (ex: Bascule de CodeBuddy vers OpenCode)     │
         └──────────────────────┬───────────────────────┘
                                │ Échec ou Indisponible ?
                                ▼
         ┌──────────────────────────────────────────────┐
         │ Niveau 3 : Modèle API 100% Gratuit Principal │
         │ (ex: xKIRO qwen3-coder-plus:free)            │
         └──────────────────────┬───────────────────────┘
                                │ Erreur HTTP / Quota ?
                                ▼
         ┌──────────────────────────────────────────────┐
         │ Niveau 4 : Modèle API 100% Gratuit Secondaire│
         │ (ex: OpenRouter flint-v1.0:free ou jev-free) │
         └──────────────────────────────────────────────┘
```

Chaque bascule est consignée de manière transparente dans la table `events` et diffusée en temps réel à l'opérateur via le flux SSE.
La mission poursuit ainsi son déroulement sans interruption inutile.
