# CAMCS — Audit d'Intégrité des Livrables & Séparation des Métriques

**Composant** : `scripts/camcs/deliverable_manager.py` / `CamcsControlCenterScreen.kt`  
**Package** : `com.ghdinteractivestudio.camcs`  
**Date** : Novembre 2026  

---

## 1. Cycle de Vie d'un Livrable dans CAMCS

Chaque livrable généré par un agent autonome est soumis à une chaîne de contrôle cryptographique :

1. **Création sur le disque local** :
   Le fichier est écrit dans l'espace de travail sécurisé `/root/.codestation/missions/<mission_id>/`.
2. **Calcul de l'empreinte SHA-256** :
   La fonction `compute_sha256(file_path)` lit le fichier par blocs de 64 Ko et génère son empreinte hexadécimale.
3. **Enregistrement dans le Registre Centralisé** :
   Insertion dans la table `deliverables` avec métadonnées : nom de l'agent, rôle, outil CLI producteur, taille en octets, empreinte SHA-256, statut initial `PENDING_AUDIT`.
4. **Audit Indépendant par l'Agent Qualité** :
   L'agent d'audit vérifie indépendamment l'existence physique, la conformité de l'empreinte et le contenu sémantique.
5. **Certification & Scellement** :
   Passage à `validation_status = 'VERIFIED'` et inscription dans le rapport de mission scellé `report.md`.

---

## 2. Séparation Rigoureuse des Métriques dans le Centre de Contrôle Android

Afin de supprimer toute ambiguïté visuelle et d'empêcher qu'une exécution partielle ne soit confondue avec une validation complète, le panneau **Résultats & Livrables** du Centre de Contrôle Android sépare strictement les compteurs :

| Indicateur | Signification Métier | Critère de Comptage |
|---|---|---|
| **Tâches Exécutées** | Tâches dont le processus a terminé | `exit_code` retourné par la CLI ou le moteur |
| **Tâches Validées** | Tâches vérifiées conformes aux critères d'acceptation | Statut `COMPLETED` certifié par l'auditeur |
| **Livrables Générés** | Fichiers physiques créés sur le disque | `file_size_bytes > 0` |
| **Livrables Certifiés** | Fichiers dont le SHA-256 et le schéma sont vérifiés | `validation_status == 'VERIFIED'` |
| **Tests Exécutés** | Suites de tests unitaires/intégration lancées | Processus de test exécuté |
| **Tests Validés** | Tests passés sans régression (exit code 0) | Assertions unitaires 100% au vert |
| **Preuves d'Exécution** | Traces d'exécution enregistrées dans SQLite | Entrées dans `execution_proofs` |
| **Actions Distantes Confirmées** | Opérations réseau vérifiées par sondage distant | `external_actions` avec statut `CONFIRMED` |

---

## 3. Gestion des Livrables Distants vs Livrables Locaux

Les livrables de type `REMOTE_REPOSITORY` (ex: `github_repo:CAMCS`) ne sont pas stockés sous forme de fichiers uniques sur le disque local, mais sous forme de pointeurs certifiés.

Pour ces livrables distants :
- Le champ `file_path` contient l'URL HTTPS canonique (ex: `https://github.com/Anisghd-lab/CAMCS`).
- Le champ `validation_details` contient le JSON certifié renvoyé par la sonde distante (propriétaire, visibilité, SHA distant, liste des fichiers vérifiés sur GitHub).
- Le livrable ne peut obtenir `validation_status = 'VERIFIED'` que si la sonde distante renvoie un succès complet.
