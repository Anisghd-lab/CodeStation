# Gestion Automatisée des Dépôts GitHub pour les Projets Produits par CAMCS

**Projet** : CAMCS Real Execution Engine v2.0 / CodeStation  
**Date** : 2 novembre 2026

---

## 1. Vision et Objectif

Lorsque le moteur autonome CAMCS reçoit pour objectif de créer un nouveau projet logiciel indépendant (ex: un micro-service, une application mobile, une bibliothèque ou un outil CLI), il ne doit pas mélanger les fichiers produits avec le code source de CodeStation.

CAMCS doit automatiquement :
1. Initialiser un espace de travail dédié dans `/root/.codestation/missions/<mission_id>/project/`.
2. Initialiser un dépôt Git local propre (`git init`).
3. Créer automatiquement le dépôt distant GitHub associé via l'API GitHub (`gh repo create` ou REST API).
4. Configurer les branches principales, les protections de base et les fichiers indispensables (`README.md`, `.gitignore`, `LICENSE`, CI/CD workflows).
5. Assurer le suivi des commits de chaque étape du DAG autonome.

---

## 2. Architecture du Module de Gestion GitHub CAMCS

```
                 CAMCS DELIVERABLE MANAGER
                             |
                             v
               GITHUB PROJECT PROVISIONER
                             |
         +-------------------+-------------------+
         |                                       |
         v                                       v
 LOCAL WORKSPACE INIT                    GITHUB REPO PROVISIONING
 (git init, initial files)              (gh repo create <org>/<name>)
         |                                       |
         +-------------------+-------------------+
                             |
                             v
                  GIT REMOTE CONFIGURATION
               (git remote add origin <url>)
                             |
                             v
                 STEP COMMIT & TAG PIPELINE
                   (git commit / git tag)
                             |
                             v
                 REMOTE PUSH & CI TRIGGER
                    (git push -u origin main)
```

---

## 3. Implémentation du Provisioner GitHub (`github_project_manager.py`)

Un composant automatisé dans `scripts/camcs/github_project_manager.py` assure ces opérations :

```python
import subprocess
import os

class GitHubProjectManager:
    def __init__(self, token=None, org="Anisghd-lab"):
        self.token = token or os.environ.get("GITHUB_TOKEN")
        self.org = org

    def create_project_repository(self, project_name, description, is_private=True):
        repo_slug = f"{self.org}/{project_name}"
        vis_flag = "--private" if is_private else "--public"
        cmd = [
            "gh", "repo", "create", repo_slug,
            vis_flag,
            "--description", description,
            "--confirm"
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        return res.returncode == 0
```

---

## 4. Règles de Sécurité et Bonnes Pratiques

1. **Isolation des Secrets** :
   Le token GitHub personnel n'est jamais consigné en clair dans les dépôts générés. Il est injecté de manière éphémère lors des étapes de push ou récupéré via la configuration sécurisée `gh auth`.
2. **Dépôts Privés par Défaut** :
   Tout nouveau projet logiciel généré par CAMCS est créé en visibilité privée par défaut, laissant à l'opérateur humain le contrôle de publication.
3. **Convention de Nommage des Projets** :
   Préfixe automatique ou slug conventionnel standardisé pour les projets sous l'organisation `Anisghd-lab`.
