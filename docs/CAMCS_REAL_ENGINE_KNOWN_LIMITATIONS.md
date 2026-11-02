# Limitations Connues & Contraintes Techniques — CAMCS Real Engine (v2.0)

**Date d'Audit** : Novembre 2026  
**Environnement d'Exécution** : Android / Termux / Ubuntu PRoot ARM64  
**Référence** : `docs/CAMCS_REAL_ENGINE_KNOWN_LIMITATIONS.md`  

---

## 1. Contraintes Liées à l'Environnement PRoot ARM64 sur Android

L'exécution de plusieurs environnements de développement IA sur un appareil mobile Android via **Termux** et **PRoot** impose des contraintes physiques et logiques strictes :

### 1.1 Emulation PTRACE et Surcharge CPU
PRoot intercepte l'ensemble des appels système (`syscalls`) via `ptrace` au niveau de l'espace utilisateur pour faire croire aux binaires qu'ils s'exécutent avec les privilèges `root` dans une arborescence Linux standard (`/`).
- **Impact** : Tout accès massif au système de fichiers (par exemple `git status` ou `find` sur de grandes arborescences) subit une surcharge importante comparée à une machine Linux native.
- **Remédiation Implémentée** : Les runners CAMCS isolent strictement chaque tâche dans son propre sous-répertoire de mission et limitent les opérations d'E/S superflues.

### 1.2 Limite de Concurrence Processus (`max_concurrent = 2`)
Sur un processeur mobile ARM64 (SoC smartphone/tablette) partagé avec le système Android et l'interface Jetpack Compose, lancer 4 ou 6 processus LLM simultanés entraînerait une saturation rapide de la mémoire vive (RAM) et le déclenchement immédiat de l'**OOM Killer** (Out-Of-Memory) du noyau Android.
- **Règle Stricte** : Le `ProcessManager` de CAMCS bride la concurrence à **2 processus simultanés maximum**.
- **Gestion de File d'Attente** : Les tâches excédentaires sont mises en attente dans la file DAG et déclenchées dès la libération d'un slot.

---

## 2. Découverte Critique : La Frontière Git et l'Explosion du Scan de `/root`

### 2.1 Symptôme Observé
Lors des premiers essais d'exécution d'OpenCode et d'Antigravity dans `/tmp/...`, des dépassements de délai d'attente (*timeout*) de plus de 120 secondes survenaient alors que le prompt ne demandait qu'une ligne de code. L'inspection des processus système a révélé la commande suivante en cours d'exécution :
```bash
git --git-dir ... --work-tree /root ls-files --others ...
```

### 2.2 Cause Racine
Les outils CLI avancés (OpenCode, Claude Code, Antigravity) recherchent automatiquement la racine du dépôt git parent pour construire leur contexte de projet.
Lorsqu'une tâche était lancée dans `/tmp/mission/...` sans dépôt git local, l'outil remontait récursivement l'arborescence jusqu'à rencontrer `/root/.git`. Il tentait alors d'indexer les millions de fichiers de build et d'archives présents dans `/root`, paralysant le thread d'analyse sous PRoot.

### 2.3 Solution Pérenne
Dans `OpenCodeRunner` et les scripts de mission, chaque répertoire de travail est désormais initialisé immédiatement comme son propre dépôt git indépendant :
```python
if not os.path.exists(os.path.join(cwd, ".git")):
    subprocess.run(["git", "init", "-q", cwd], check=False, timeout=5)
```
Cette mesure confine le balayage git aux seuls fichiers de la mission et divise le temps d'exécution par dix.

---

## 3. Particularité OpenCode : Daemon Global vs Mode `--standalone`

### 3.1 Problématique
Au démarrage du système (`start_code_station.sh`), un service d'arrière-plan `opencode serve --service` est lancé dans `/root`.
Lorsqu'un appel classique `opencode run` est émis, le binaire s'adresse par défaut à ce service d'arrière-plan, héritant de l'espace de travail `/root`.

### 3.2 Solution Implémentée
L'adaptateur `OpenCodeRunner` passe systématiquement les arguments :
```bash
opencode run --standalone --auto -m xkiro/qwen3-coder-plus --format default "<prompt>"
```
L'argument `--standalone` force la création d'une instance privée confinée au répertoire `cwd` courant, tandis que `--auto` évite tout blocage sur des demandes interactives de permissions.

---

## 4. Statut des CLI Partiellement Opérationnelles ou Bloquées

### 4.1 OmniRush (`omnirush`) — Statut `BLOCKED`
- **Cause** : Le service en amont (*upstream cloud*) géré par OmniRush renvoie une erreur HTTP 503 :
  > *« omnirush server scaling up until 12:00 UTC 02 Nov »*
- **Gestion CAMCS** : Le contrôle de santé détecte cette indisponibilité et positionne le statut sur `BLOCKED`. Le routeur intelligent bascule automatiquement toute tâche dévolue à OmniRush vers `codebuddy` ou `antigravity`.

### 4.2 Freebuff (`freebuff`) — Statut `PARTIALLY_IMPLEMENTED`
- **Cause** : Le binaire natif Freebuff v0.2.35 ne propose pas de drapeau d'exécution sans interface (`-p` / `--print` ou `--headless`). Il requiert obligatoirement un pseudo-terminal TTY pour afficher son interface graphique Curses.
- **Gestion CAMCS** : Freebuff est identifié comme `PARTIALLY_IMPLEMENTED` (mode TUI interactif seul). Il est accessible pour l'utilisateur dans un terminal interactif, mais le routeur CAMCS ne lui assigne pas de tâches de production automatique sans surveillance humaine.

---

## 5. Latence Réseau & Délais d'Attente (Timeouts)

L'interrogation d'API LLM distantes (Google Gemini, xKiro Qwen, CodeBuddy) combinée à l'émulation PRoot requiert des fenêtres de temps réalistes :
- Spécification architecturale complexe (AGY) : **60s à 95s**
- Génération de module complet (OpenCode) : **90s à 145s**
- Audit indépendant exhaustif (CodeBuddy) : **90s à 140s**
- Revue légère (Clixad) : **2s à 5s**

Les timeouts système sont configurés entre **150s et 180s** pour éviter toute coupure prématurée lors de fluctuations réseau mobiles.
