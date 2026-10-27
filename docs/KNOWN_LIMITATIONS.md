# KNOWN_LIMITATIONS.md — Limites Connues & Fonctions Découplées

> **Projet** : Synapse (`com.ghdinteractivestudio.synapse`)  
> **Date** : 27 Octobre 2026

---

## 1. Principes d'Intégrité

Conformément aux directives absolues du projet, aucune fonctionnalité simulée de manière trompeuse n'est intégrée. Lorsque des fonctionnalités avancées de l'écosystème amont (conçues pour macOS / Linux x86_64 avec accélération graphique et pilotes matériels) ne sont pas transposables directement dans une application mobile Android native légère, elles sont clairement documentées et l'interface affiche un état honnête d'indisponibilité.

---

## 2. Limites Identifiées

### 2.1 Télécommande d'Écran (Remote Screen WebRTC)
* **Description amont** : Système de capture d'écran d'ordinateur via portail Wayland/PipeWire (Linux) ou extension d'écran macOS, relayé par WebRTC.
* **État dans Synapse** : Une application mobile Android autonome n'embarque pas de serveur de capture d'écran d'ordinateur. Le visualiseur d'écran distant affiche une carte explicative honnête informant que cette fonctionnalité nécessite un hôte desktop doté du module de capture vidéo.

### 2.2 Protocole Chiffré Propriétaire Rust (X25519/ChaCha20-Poly1305)
* **Description amont** : Codync implémentait un protocole binaire en couches sur WebSocket chiffré par un binaire Rust personnalisé.
* **État dans Synapse** : Synapse utilise un client réseau standard HTTP / WebSocket sécurisé (OkHttp/Retrofit avec jeton Bearer d'appairage). Cela permet de connecter Synapse à n'importe quel serveur ou passerelle d'agents (y compris un script Python léger ou un serveur d'agents local Termux) sans nécessiter de compiler des bibliothèques C++ ou Rust natives dans l'APK.

### 2.3 Moteurs Vocaux Temps Réel Spécifiques (Gemini Live / OpenAI Realtime)
* **Description amont** : Nécessite des clés API cloud propriétaires directes pour le streaming audio bidirectionnel WebSocket.
* **État dans Synapse** : Synapse intègre la dictée vocale et la lecture via les services natifs certifiés d'Android (`SpeechRecognizer` et `TextToSpeech`), respectant la confidentialité locale et évitant d'exposer des clés d'API tierces dans l'application.

### 2.4 Compilation Locale sur Téléphone
* **Description** : L'environnement de développement de l'utilisateur est un téléphone Android avec Termux et Ubuntu PRoot.
* **Conséquence** : L'installation du SDK Android complet (plusieurs gigaoctets) et l'exécution d'un daemon Gradle lourd sur le téléphone ralentiraient ou bloqueraient l'appareil. La génération du code et des tests est effectuée par Antigravity, tandis que la compilation du binaire APK est déléguée à **GitHub Actions** sur des exécuteurs Linux Ubuntu standards avec JDK 21.
