# REFERENCE_GAPS.md — Écarts de Référence, Choix Techniques & Décisions d'Architecture

> **Projet** : Synapse (`com.ghdinteractivestudio.synapse`)  
> **Date de publication** : 27 Octobre 2026

---

## 1. Objectif du Document

Ce document identifie de manière rigoureuse les écarts entre les spécifications amont de Codync (pensées pour un écosystème hybride macOS / iOS / Electron / Rust) et l'implémentation Android native Jetpack Compose de **Synapse**. Il justifie chaque décision technique prise pour garantir la stabilité, la sécurité et la conformité aux contraintes de la mission.

---

## 2. Tableau Comparatif des Écarts Techniques

| Domaine | Référence Amont Codync | Implémentation Synapse (Android Natif) | Justification & Valeur Ajoutée |
|---|---|---|---|
| **Langage & Framework UI** | Swift (iOS) / Electron React (Desktop) / Kotlin expérimental | **100% Kotlin 2.0 + Jetpack Compose** | Adhérence absolue aux standards modernes Android, performance et légèreté. |
| **Design System** | Shaders Kyant (RuntimeShader RenderEffect C++) & Liquid Glass iOS | **Material 3 Entièrement Personnalisé** (Surfaces sombres Obsidian, dégradés d'accent néon, formes géométriques pures) | Compatibilité universelle avec tous les processeurs graphiques Android (GPU Mali/Adreno), aucun crash lié aux shaders non supportés. |
| **Gestion d'État** | Objet unique monolithique `AppStore.kt` de 3 000 lignes | **Clean Architecture MVVM + `StateFlow` + Repositories** | Séparation stricte de la logique métier, immutabilité des états UI et testabilité unitaire totale. |
| **Persistance des Données** | Fichiers bruts JSON et accès SQLite direct C/Rust | **Room Database (SQLite typé) + DataStore Preferences** | Requêtes asynchrones sûres via Coroutines, migrations de schéma gérées et persistance réactive. |
| **Protocole Réseau** | ChaCha20-Poly1305 / X25519 propriétaire sur socket brut | **Client HTTP / WebSocket standardisé (OkHttp + Retrofit)** | Protocole standard, interopérable avec n'importe quelle passerelle d'agents ou serveur hôte local Termux, sans bibliothèque native NDK instable. |
| **Identité de Marque** | Nom et logos propriétaires Codync | **Marque originale Synapse & icône vectorielle adaptative** | Respect de la propriété intellectuelle et identité visuelle distincte. |
| **Télécommande d'Écran (Remote Screen)** | Capture vidéo H.264 WebRTC via portal Wayland / macOS screencapture | **État honnête d'indisponibilité** | Une application mobile n'a pas vocation à embarquer un moteur lourd de streaming vidéo WebRTC propriétaire s'il n'y a pas de serveur de capture dédié. |
| **Voix Mains-Libres** | Intégration audio Apple SFSpeech / macOS helper | **Android SpeechRecognizer + TextToSpeech natif** | Exploitation des API système certifiées Android, sans dépendance propriétaire externe. |

---

## 3. Analyse Détaillée des Écarts

### 3.1 Abandon des Shaders Kyant & Rendu Graphique
* **Constat amont** : Le sous-projet `apps/android/design` amont incluait une bibliothèque nommée `kyant-backdrop` utilisant des `RuntimeShader` OpenGL/Vulkan pour simuler le flou de verre iOS (*Liquid Glass*).
* **Risque identifié** : Sur de nombreux appareils Android (notamment les architectures ARM intermédiaires et les versions d'Android inférieures à 13), ces shaders provoquent des plantages fatals au démarrage (`Shader compilation error` ou fallback noir opaque).
* **Résolution Synapse** : Conception d'un design system Material 3 sur-mesure utilisant des conteneurs de surfaces hiérarchisés (`surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`), des contours subtils semi-transparents (`outlineVariant`), des élévations douces et des micro-animations de pulsation. Le rendu visuel est à la fois ultra-moderne, réactif et exempt de tout risque de crash matériel.

### 3.2 Refonte de la Couche Réseau & Découplage de la Cryptographie Rust
* **Constat amont** : L'appairage amont dépendait de primitives cryptographiques C/Rust (X25519, Ed25519, ChaCha20-Poly1305) imbriquées dans le binaire Rust hôte, rendant l'application cliente incapable de fonctionner avec une simple API REST ou une passerelle standardisée.
* **Résolution Synapse** : Synapse implémente un modèle d'échange basé sur des jetons de session d'appairage (`synapse://pair?token=...` ou `http://127.0.0.1:port/api`), avec un client OkHttp gérant l'authentification Bearer, les timeouts configurables, la reconnexion automatique en cas de rupture de socket et la compatibilité bi-directionnelle avec les endpoints d'agents locaux.

### 3.3 Traitement des Fonctionnalités Spécialisées (Voix, Mémoire, Écran)
* **Principe d'intégrité** : Aucune fonctionnalité fictive ou "simulée" sans valeur n'est présentée comme fonctionnelle.
* **Comportement adopté** :
  - La **Voix** utilise le moteur vocal Android standard pour la dictée et la lecture de message, avec avertissement si la permission microphone est refusée.
  - La **Mémoire** offre un explorateur complet des notes d'agent, avec mention claire si le moteur de l'agent ne supporte pas l'extension mémoire.
  - Les **Routines** permettent de créer, afficher et tester des planifications réelles, avec feedback explicite.
  - L'**Écran distant** affiche un état indisponible honnête expliquant qu'un hôte graphique compatible avec capture d'écran est requis.

---

## 4. Synthèse d'Audit & Passage en Phase d'Implémentation

Tous les prérequis d'audit de la **Phase 1** sont validés. Les 11 flux fonctionnels sont définis avec exactitude, les composants réutilisables sont cartographiés et les frontières architecturales sont établies. L'implémentation de **Synapse** dans `/root/synapse` commence immédiatement.
