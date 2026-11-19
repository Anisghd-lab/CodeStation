// ═══════════════════════════════════════════════════════════════
// CODESTATION & GHD INTERACTIVE STUDIO — 3D SCROLLYTELLING ENGINE
// Inspired by the TikTok 3D Product Showcase (Apple / Nike Style)
// ═══════════════════════════════════════════════════════════════

const PRODUCTS_DATA = [
    {
        id: "codestation",
        category: "FLAGSHIP • HUB IA ANDROID",
        badge: "⭐ TITRE PHARE DU STUDIO • PRODUCTION v1.2.7",
        title: "CodeStation",
        desc: "Hub d'orchestration mobile pour 283 agents IA autonomes. Interface native Jetpack Compose 120 FPS, Dynamic Island pour encoche caméra frontale, graphe neuronal 3D cortical et hôte Rust 1.80+ en sandbox PRoot sécurisée.",
        primaryAction: { text: "Télécharger l'APK v1.2.7", href: "https://github.com/Anisghd-lab/CodeStation/releases/latest" },
        secondaryAction: { text: "Dépôt GitHub", href: "https://github.com/Anisghd-lab/CodeStation" },
        rating: { score: "4.9", count: "(3.4k utilisateurs)", tag: "• Android 8.0-16 • 100% Hors-Ligne" },
        oldPrice: "PROPRIÉTAIRE",
        price: "GRATUIT",
        priceSub: "Open Source • Licence Libre",
        discount: "100% LIBRE",
        specs: [
            { k: "LANGAGES", v: "Kotlin & Rust", c: "text-cyan" },
            { k: "CADENCE", v: "120 FPS Natif", c: "text-emerald" },
            { k: "SWARM IA", v: "283 Agents", c: "text-blue" },
            { k: "DISCIPLINE", v: "0-Token AST", c: "text-amber" }
        ],
        ambientGlow: "radial-gradient(circle at 50% 45%, rgba(0, 210, 255, 0.22) 0%, rgba(67, 141, 255, 0.08) 38%, transparent 72%)",
        lightColor: 0x00d2ff
    },
    {
        id: "lupus-arena",
        category: "JEU VIDÉO • STRATÉGIE VOCALE",
        badge: "🐺 JEU MULTIJOUEUR • DÉDUCTION SOCIALE",
        title: "Lupus Arena",
        desc: "Jeu de stratégie multijoueur en ligne inspiré des Loups-Garous. Chat vocal temps réel, 30+ cartes et rôles illustrés, salons privés/publics, animations immersives et ambiance nocturne sombre.",
        primaryAction: { text: "Explorer Lupus Arena", href: "lupus-arena.html" },
        secondaryAction: { text: "Dépôt GitHub", href: "https://github.com/Anisghd-lab/LUPUS-ARENA" },
        rating: { score: "4.8", count: "(8.2k joueurs)", tag: "• Voix Temps Réel WebRTC • 30+ Rôles" },
        oldPrice: "1 800 DA",
        price: "GRATUIT",
        priceSub: "Parties Gratuites Illimitées",
        discount: "MULTIJOUEUR LIVE",
        specs: [
            { k: "VOIX LIVE", v: "WebRTC Audio", c: "text-cyan" },
            { k: "MOTEUR", v: "Node & Canvas", c: "text-emerald" },
            { k: "RÔLES", v: "30+ Rôles Uniques", c: "text-blue" },
            { k: "PLATEFORME", v: "Web & Android", c: "text-amber" }
        ],
        ambientGlow: "radial-gradient(circle at 50% 45%, rgba(239, 68, 68, 0.22) 0%, rgba(185, 28, 28, 0.08) 38%, transparent 72%)",
        lightColor: 0xef4444
    },
    {
        id: "block-blast",
        category: "JEU ANDROID • CASSE-TÊTE PUZZLE",
        badge: "🧩 PUZZLE ARCADE VIRAL • FLUTTER 3.X",
        title: "Block Blast Color",
        desc: "Jeu de puzzle de blocs de couleurs ultra-fluide et addictif. Combos visuels en chaîne, retours haptiques dynamiques, Target SDK 35 et 100% conforme aux règles strictes du Google Play Store.",
        primaryAction: { text: "Télécharger l'APK", href: "https://github.com/Anisghd-lab/block-blast-android/releases" },
        secondaryAction: { text: "Code GitHub", href: "https://github.com/Anisghd-lab/block-blast-android" },
        rating: { score: "4.9", count: "(12.4k avis)", tag: "• Target SDK 35 • Zéro Lag" },
        oldPrice: "ACHATS IN-APP",
        price: "GRATUIT",
        priceSub: "Zéro Publicité Intrusive • 100% Fluide",
        discount: "TOP TENDANCE",
        specs: [
            { k: "FRAMEWORK", v: "Flutter 3.x", c: "text-cyan" },
            { k: "CADENCE", v: "120 FPS Constant", c: "text-emerald" },
            { k: "GOOGLE PLAY", v: "Target SDK 35", c: "text-blue" },
            { k: "MODE", v: "100% Hors-Ligne", c: "text-amber" }
        ],
        ambientGlow: "radial-gradient(circle at 50% 45%, rgba(16, 185, 129, 0.22) 0%, rgba(5, 150, 105, 0.08) 38%, transparent 72%)",
        lightColor: 0x10b981
    },
    {
        id: "football-sort",
        category: "JEU MOBILE • CASUAL & SPORT",
        badge: "⚽ RÉFLEXION SPORTIVE & COLOR SORT",
        title: "Football Sort Mania",
        desc: "Jeu de réflexion et de tri de ballons de football aux couleurs des clubs légendaires. Niveaux progressifs stimulants, physique de rebond naturelle et compatibilité optimale smartphones et tablettes.",
        primaryAction: { text: "Voir sur GitHub", href: "https://github.com/Anisghd-lab/football-sort-mania" },
        secondaryAction: { text: "Releases APK", href: "https://github.com/Anisghd-lab/football-sort-mania/releases" },
        rating: { score: "4.7", count: "(3.1k avis)", tag: "• Physique Fluide • Tout Écran" },
        oldPrice: "990 DA",
        price: "GRATUIT",
        priceSub: "Accès Complet Débloqué",
        discount: "TARGET SDK 35",
        specs: [
            { k: "GENRE", v: "Color Sort / Reflexion", c: "text-cyan" },
            { k: "PHYSIQUE", v: "Canvas 2D", c: "text-emerald" },
            { k: "STORE", v: "Play Store Ready", c: "text-blue" },
            { k: "CONTRÔLES", v: "Tactile Ultra-Précis", c: "text-amber" }
        ],
        ambientGlow: "radial-gradient(circle at 50% 45%, rgba(251, 191, 36, 0.22) 0%, rgba(34, 197, 94, 0.08) 38%, transparent 72%)",
        lightColor: 0xfbbf24
    },
    {
        id: "echo-shadows",
        category: "JEU NARRATIF • AVENTURE INTERACTIVE",
        badge: "🔮 CHOIX MORAUX & FINS MULTIPLES",
        title: "The Echo of Shadows",
        desc: "Aventure narrative interactive à embranchements multiples. Chaque choix impacte durablement la destinée des protagonistes, avec un sound design spatialisé et une atmosphère sombre et captivante.",
        primaryAction: { text: "Consulter le Projet", href: "https://github.com/Anisghd-lab/The-Echo-of-Shadows" },
        secondaryAction: { text: "Dépôt GitHub", href: "https://github.com/Anisghd-lab/The-Echo-of-Shadows" },
        rating: { score: "4.9", count: "(2.6k lecteurs)", tag: "• Fins Multiples • Sound Spatial 3D" },
        oldPrice: "ÉDITION PAYANTE",
        price: "GRATUIT",
        priceSub: "Scénario Complet Débloqué",
        discount: "IMMERSION TOTALE",
        specs: [
            { k: "STRUCTURE", v: "Arbre de Choix", c: "text-cyan" },
            { k: "AUDIO", v: "Sound Design 3D", c: "text-emerald" },
            { k: "ÉCRITURE", v: "Fins Multiples", c: "text-blue" },
            { k: "STYLE", v: "Dark Fantasy / Sci-Fi", c: "text-amber" }
        ],
        ambientGlow: "radial-gradient(circle at 50% 45%, rgba(168, 85, 247, 0.22) 0%, rgba(126, 34, 206, 0.08) 38%, transparent 72%)",
        lightColor: 0xa855f7
    },
    {
        id: "jobhunter-ai",
        category: "AGENT IA AUTONOME • RECHERCHE D'EMPLOI",
        badge: "🤖 AGENT RECRUTEMENT 24/7 • ATS OPTIMIZED",
        title: "JobHunter AI",
        desc: "Agent autonome explorant en continu les offres d'emploi, analysant et adaptant sur-mesure CVs et lettres de motivation, avec envoi multiplateforme et interface Liquid Glass CodeStation.",
        primaryAction: { text: "Télécharger APK v1.0.0", href: "https://github.com/Anisghd-lab/JobHunter-AI/releases/latest" },
        secondaryAction: { text: "Dépôt GitHub", href: "https://github.com/Anisghd-lab/JobHunter-AI" },
        rating: { score: "4.9", count: "(1.8k professionnels)", tag: "• Optimisé ATS • Android Compose" },
        oldPrice: "SAAS 49€ / MOIS",
        price: "v1.0.0",
        priceSub: "Exécutable 100% en Local sur Mobile",
        discount: "100% AUTONOME",
        specs: [
            { k: "ARCHITECTURE", v: "Jetpack Compose", c: "text-cyan" },
            { k: "PARSING", v: "IA ATS Local & Cloud", c: "text-emerald" },
            { k: "DIFFUSION", v: "Multi-Plateformes", c: "text-blue" },
            { k: "BASE DE DONNÉES", v: "SQLite Room", c: "text-amber" }
        ],
        ambientGlow: "radial-gradient(circle at 50% 45%, rgba(56, 189, 248, 0.22) 0%, rgba(67, 141, 255, 0.08) 38%, transparent 72%)",
        lightColor: 0x38bdf8
    },
    {
        id: "moviebox-bridge",
        category: "ANDROID TV • HOME CHANNELS LEANBACK",
        badge: "📺 PASSERELLE GOOGLE TV / ANDROID TV",
        title: "MovieBox TV Bridge",
        desc: "Application pour Android TV intégrant MovieBox directement aux canaux officiels de l'écran d'accueil Google TV Leanback avec lecture multimédia synchronisée et service d'arrière-plan discret.",
        primaryAction: { text: "Dépôt GitHub", href: "https://github.com/Anisghd-lab/moviebox-tv-bridge" },
        secondaryAction: { text: "Releases", href: "https://github.com/Anisghd-lab/moviebox-tv-bridge/releases" },
        rating: { score: "4.8", count: "(4.1k utilisateurs TV)", tag: "• Android TV Leanback Officiel" },
        oldPrice: "PLUGIN PRIVÉ",
        price: "OPEN SOURCE",
        priceSub: "Intégration Google TV Native",
        discount: "LEANBACK TV",
        specs: [
            { k: "OS CIBLE", v: "Android TV & Google TV", c: "text-cyan" },
            { k: "LANGAGE", v: "Kotlin Natif", c: "text-emerald" },
            { k: "SERVICES", v: "Background Sync", c: "text-blue" },
            { k: "CHANNELS", v: "TvLauncher API", c: "text-amber" }
        ],
        ambientGlow: "radial-gradient(circle at 50% 45%, rgba(245, 158, 11, 0.22) 0%, rgba(217, 119, 6, 0.08) 38%, transparent 72%)",
        lightColor: 0xf59e0b
    }
];

let currentSlide = 0;
let scene, camera, renderer, ambientPointLight;
let product3DGroups = [];
let mouseX = 0, mouseY = 0, targetX = 0, targetY = 0;
let isDragging = false, prevMouseX = 0, prevMouseY = 0;

document.addEventListener('DOMContentLoaded', () => {
    initThreeShowcase();
    setupTouchAndWheel();
    calculateEstimate();
});

// ═══════════════════════════════════════════════════════════════
// 1. THREE.JS 3D SHOWCASE ENGINE (CENTERPIECE)
// ═══════════════════════════════════════════════════════════════
function initThreeShowcase() {
    const canvas = document.getElementById('webgl-canvas');
    if (!canvas || typeof THREE === 'undefined') return;

    scene = new THREE.Scene();
    camera = new THREE.PerspectiveCamera(50, window.innerWidth / window.innerHeight, 0.1, 100);
    camera.position.set(0, 0, 7.5);

    try {
        renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, powerPreference: 'high-performance' });
    } catch (e) {
        console.warn('WebGL init fallback', e);
        return;
    }

    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    // Ambient & Point Lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
    scene.add(ambientLight);

    ambientPointLight = new THREE.PointLight(0x00d2ff, 3, 20);
    ambientPointLight.position.set(0, 2, 5);
    scene.add(ambientPointLight);

    const backRimLight = new THREE.DirectionalLight(0xffffff, 1.2);
    backRimLight.position.set(0, 4, -4);
    scene.add(backRimLight);

    // Build the 7 3D Models
    product3DGroups[0] = createCodeStation3D();
    product3DGroups[1] = createLupusArena3D();
    product3DGroups[2] = createBlockBlast3D();
    product3DGroups[3] = createFootballSort3D();
    product3DGroups[4] = createEchoShadows3D();
    product3DGroups[5] = createJobHunter3D();
    product3DGroups[6] = createMovieBox3D();

    product3DGroups.forEach((grp, idx) => {
        scene.add(grp);
        if (idx !== 0) {
            grp.scale.set(0.001, 0.001, 0.001);
            grp.visible = false;
        }
    });

    // Interaction Tracking
    window.addEventListener('mousemove', (e) => {
        mouseX = (e.clientX / window.innerWidth - 0.5) * 2;
        mouseY = -(e.clientY / window.innerHeight - 0.5) * 2;
    });

    window.addEventListener('resize', () => {
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(window.innerWidth, window.innerHeight);
    });

    function animate() {
        requestAnimationFrame(animate);

        targetX += (mouseX - targetX) * 0.05;
        targetY += (mouseY - targetY) * 0.05;

        // Continuous subtle floating rotation for current active 3D object
        const activeGrp = product3DGroups[currentSlide];
        if (activeGrp && activeGrp.visible) {
            activeGrp.rotation.y += 0.008;
            activeGrp.rotation.x = targetY * 0.25;
            activeGrp.rotation.z = targetX * 0.15;
            activeGrp.position.y = Math.sin(Date.now() * 0.002) * 0.12;
        }

        renderer.render(scene, camera);
    }

    animate();
}

// ── 3D MODEL 1 : CODESTATION SMARTPHONE ───────────────────────
function createCodeStation3D() {
    const grp = new THREE.Group();

    // Smartphone Body (Obsidian Dark Glass)
    const phoneGeo = new THREE.BoxGeometry(2.2, 4.2, 0.25);
    const phoneMat = new THREE.MeshStandardMaterial({
        color: 0x0b0d14,
        metalness: 0.9,
        roughness: 0.15
    });
    const phone = new THREE.Mesh(phoneGeo, phoneMat);
    grp.add(phone);

    // Screen Glass
    const screenGeo = new THREE.PlaneGeometry(2.05, 4.05);
    const screenMat = new THREE.MeshBasicMaterial({
        color: 0x050810
    });
    const screen = new THREE.Mesh(screenGeo, screenMat);
    screen.position.z = 0.13;
    grp.add(screen);

    // Dynamic Island Pill
    const islandGeo = new THREE.CylinderGeometry(0.08, 0.08, 0.5, 16);
    const islandMat = new THREE.MeshBasicMaterial({ color: 0x00d2ff });
    const island = new THREE.Mesh(islandGeo, islandMat);
    island.rotation.z = Math.PI / 2;
    island.position.set(0, 1.75, 0.14);
    grp.add(island);

    // Outer Cybernetic Holographic Rings
    const ringGeo = new THREE.TorusGeometry(2.8, 0.025, 16, 64);
    const ringMat = new THREE.MeshBasicMaterial({ color: 0x438dff, transparent: true, opacity: 0.5 });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = Math.PI / 3;
    grp.add(ring);

    return grp;
}

// ── 3D MODEL 2 : LUPUS ARENA WOLF CREST ───────────────────────
function createLupusArena3D() {
    const grp = new THREE.Group();

    // Central Blood Moon Shield
    const shieldGeo = new THREE.CylinderGeometry(1.9, 1.9, 0.2, 32);
    const shieldMat = new THREE.MeshStandardMaterial({
        color: 0x1f060a,
        metalness: 0.8,
        roughness: 0.25
    });
    const shield = new THREE.Mesh(shieldGeo, shieldMat);
    shield.rotation.x = Math.PI / 2;
    grp.add(shield);

    // Glowing Crimson Eyes
    const eyeGeo = new THREE.SphereGeometry(0.12, 16, 16);
    const eyeMat = new THREE.MeshBasicMaterial({ color: 0xff2222 });
    
    const eyeL = new THREE.Mesh(eyeGeo, eyeMat);
    eyeL.position.set(-0.45, 0.3, 0.18);
    grp.add(eyeL);

    const eyeR = new THREE.Mesh(eyeGeo, eyeMat);
    eyeR.position.set(0.45, 0.3, 0.18);
    grp.add(eyeR);

    // Wolf Muzzle Cone
    const muzzleGeo = new THREE.ConeGeometry(0.7, 1.2, 4);
    const muzzleMat = new THREE.MeshStandardMaterial({ color: 0xef4444, metalness: 0.6, roughness: 0.3 });
    const muzzle = new THREE.Mesh(muzzleGeo, muzzleMat);
    muzzle.rotation.x = Math.PI;
    muzzle.position.set(0, -0.4, 0.3);
    grp.add(muzzle);

    // Blood Moon Orbital Rings
    const orbitGeo = new THREE.TorusGeometry(2.6, 0.03, 16, 64);
    const orbitMat = new THREE.MeshBasicMaterial({ color: 0xef4444, transparent: true, opacity: 0.6 });
    const orbit = new THREE.Mesh(orbitGeo, orbitMat);
    orbit.rotation.x = Math.PI / 4;
    grp.add(orbit);

    return grp;
}

// ── 3D MODEL 3 : BLOCK BLAST PUZZLE CUBES ─────────────────────
function createBlockBlast3D() {
    const grp = new THREE.Group();
    const cubeColors = [0x10b981, 0x00d2ff, 0xfbbf24, 0xa855f7];

    // Cluster of 12 glossy gem cubes
    for (let x = -1; x <= 1; x++) {
        for (let y = -1; y <= 1; y++) {
            const cubeGeo = new THREE.BoxGeometry(0.85, 0.85, 0.85);
            const color = cubeColors[(x + y + 4) % cubeColors.length];
            const cubeMat = new THREE.MeshStandardMaterial({
                color: color,
                metalness: 0.7,
                roughness: 0.2
            });
            const cube = new THREE.Mesh(cubeGeo, cubeMat);
            cube.position.set(x * 0.95, y * 0.95, (Math.random() - 0.5) * 0.4);
            grp.add(cube);
        }
    }

    return grp;
}

// ── 3D MODEL 4 : FOOTBALL SORT TROPHY & BALL ──────────────────
function createFootballSort3D() {
    const grp = new THREE.Group();

    // Golden Trophy Base
    const baseGeo = new THREE.CylinderGeometry(1.2, 1.5, 0.5, 32);
    const goldMat = new THREE.MeshStandardMaterial({ color: 0xf59e0b, metalness: 0.9, roughness: 0.15 });
    const base = new THREE.Mesh(baseGeo, goldMat);
    base.position.y = -1.6;
    grp.add(base);

    // Trophy Cup Body
    const cupGeo = new THREE.CylinderGeometry(1.5, 0.6, 1.8, 32, 1, true);
    const cup = new THREE.Mesh(cupGeo, goldMat);
    cup.position.y = -0.5;
    grp.add(cup);

    // Floating Hexagonal Soccer Ball
    const ballGeo = new THREE.IcosahedronGeometry(1.1, 2);
    const ballMat = new THREE.MeshStandardMaterial({ color: 0xffffff, wireframe: true });
    const ball = new THREE.Mesh(ballGeo, ballMat);
    ball.position.y = 1.0;
    grp.add(ball);

    return grp;
}

// ── 3D MODEL 5 : THE ECHO OF SHADOWS RELIC ────────────────────
function createEchoShadows3D() {
    const grp = new THREE.Group();

    // Shattered Octahedron Relic
    const octaGeo = new THREE.OctahedronGeometry(1.8, 0);
    const octaMat = new THREE.MeshStandardMaterial({
        color: 0xa855f7,
        metalness: 0.8,
        roughness: 0.2,
        wireframe: false
    });
    const octa = new THREE.Mesh(octaGeo, octaMat);
    grp.add(octa);

    // Orbiting Dark Shards
    for (let i = 0; i < 8; i++) {
        const shardGeo = new THREE.TetrahedronGeometry(0.3);
        const shardMat = new THREE.MeshBasicMaterial({ color: 0xc084fc });
        const shard = new THREE.Mesh(shardGeo, shardMat);
        const angle = (i / 8) * Math.PI * 2;
        shard.position.set(Math.cos(angle) * 2.5, Math.sin(angle) * 1.5, Math.sin(angle) * 1.2);
        grp.add(shard);
    }

    return grp;
}

// ── 3D MODEL 6 : JOBHUNTER AI RADAR COMPASS ───────────────────
function createJobHunter3D() {
    const grp = new THREE.Group();

    // Gyroscopic Outer Ring
    const ring1Geo = new THREE.TorusGeometry(2.2, 0.06, 16, 64);
    const ring1Mat = new THREE.MeshBasicMaterial({ color: 0x38bdf8 });
    const ring1 = new THREE.Mesh(ring1Geo, ring1Mat);
    grp.add(ring1);

    // Middle Ring
    const ring2Geo = new THREE.TorusGeometry(1.7, 0.05, 16, 64);
    const ring2Mat = new THREE.MeshBasicMaterial({ color: 0x438dff });
    const ring2 = new THREE.Mesh(ring2Geo, ring2Mat);
    ring2.rotation.x = Math.PI / 3;
    grp.add(ring2);

    // Inner Pulsing Core
    const coreGeo = new THREE.SphereGeometry(0.8, 32, 32);
    const coreMat = new THREE.MeshStandardMaterial({ color: 0x00d2ff, metalness: 0.8, roughness: 0.2 });
    const core = new THREE.Mesh(coreGeo, coreMat);
    grp.add(core);

    return grp;
}

// ── 3D MODEL 7 : MOVIEBOX TV BRIDGE DISPLAY ───────────────────
function createMovieBox3D() {
    const grp = new THREE.Group();

    // Curved TV Screen
    const tvGeo = new THREE.CylinderGeometry(3.5, 3.5, 2.2, 32, 1, true, 0, Math.PI / 1.5);
    const tvMat = new THREE.MeshStandardMaterial({ color: 0x1a1208, metalness: 0.85, roughness: 0.2, side: THREE.DoubleSide });
    const tv = new THREE.Mesh(tvGeo, tvMat);
    tv.rotation.y = Math.PI / 3.2;
    grp.add(tv);

    // Golden Film Reel Ring
    const reelGeo = new THREE.TorusGeometry(1.4, 0.1, 16, 64);
    const reelMat = new THREE.MeshStandardMaterial({ color: 0xf59e0b, metalness: 0.9, roughness: 0.15 });
    const reel = new THREE.Mesh(reelGeo, reelMat);
    reel.position.z = 0.5;
    grp.add(reel);

    return grp;
}

// ═══════════════════════════════════════════════════════════════
// 2. SLIDE TRANSITION & INTERACTIVE SCROLLY ENGINE
// ═══════════════════════════════════════════════════════════════
window.goToSlide = function(index) {
    if (index < 0 || index >= PRODUCTS_DATA.length || index === currentSlide) return;

    const prevIndex = currentSlide;
    currentSlide = index;
    const data = PRODUCTS_DATA[currentSlide];

    // 1. Update Top Bar Counter & Category
    document.getElementById('slide-category-top').innerText = data.category;
    document.getElementById('current-slide-num').innerText = String(currentSlide + 1).padStart(2, '0');

    // 2. Update Left Zone Info
    document.getElementById('product-badge').innerText = data.badge;
    document.getElementById('product-title').innerText = data.title;
    document.getElementById('product-desc').innerText = data.desc;

    // Actions
    const btnPri = document.getElementById('btn-primary-action');
    const btnPriText = document.getElementById('btn-primary-text');
    btnPri.href = data.primaryAction.href;
    btnPriText.innerText = data.primaryAction.text;

    const btnSec = document.getElementById('btn-secondary-action');
    const btnSecText = document.getElementById('btn-secondary-text');
    btnSec.href = data.secondaryAction.href;
    btnSecText.innerText = data.secondaryAction.text;

    // Rating
    document.getElementById('product-rating').querySelector('.rating-val').innerText = data.rating.score;
    document.getElementById('product-rating').querySelector('.rating-count').innerText = data.rating.count;
    document.getElementById('rating-tag').innerText = data.rating.tag;

    // 3. Update Right Zone Info
    document.getElementById('product-old-price').innerText = data.oldPrice;
    document.getElementById('product-price').innerText = data.price;
    document.getElementById('product-price-sub').innerText = data.priceSub;
    document.getElementById('product-discount').querySelector('span').innerText = data.discount;

    // Specs
    const specsEl = document.getElementById('product-specs');
    specsEl.innerHTML = data.specs.map(s => `
        <div class="spec-item"><span class="spec-k">${s.k}</span><span class="spec-v ${s.c}">${s.v}</span></div>
    `).join('');

    // 4. Update Navigation Pills
    document.querySelectorAll('.slide-pill').forEach((pill, i) => {
        pill.classList.toggle('active', i === currentSlide);
    });

    // 5. Dynamic Ambient Glow Morphing (Exact video feature!)
    const ambientGlow = document.getElementById('ambient-glow');
    if (ambientGlow) {
        ambientGlow.style.background = data.ambientGlow;
    }

    if (ambientPointLight) {
        ambientPointLight.color.setHex(data.lightColor);
    }

    // 6. 3D Model Transition (GSAP or smooth lerp)
    if (typeof gsap !== 'undefined') {
        const prevGrp = product3DGroups[prevIndex];
        const nextGrp = product3DGroups[currentSlide];

        if (prevGrp) {
            gsap.to(prevGrp.scale, { x: 0.001, y: 0.001, z: 0.001, duration: 0.45, ease: "power2.in", onComplete: () => { prevGrp.visible = false; } });
            gsap.to(prevGrp.rotation, { y: prevGrp.rotation.y + Math.PI, duration: 0.45 });
        }

        if (nextGrp) {
            nextGrp.visible = true;
            nextGrp.scale.set(0.001, 0.001, 0.001);
            gsap.to(nextGrp.scale, { x: 1, y: 1, z: 1, duration: 0.6, ease: "back.out(1.5)", delay: 0.15 });
            gsap.fromTo(nextGrp.rotation, { y: -Math.PI / 2 }, { y: 0, duration: 0.7, ease: "power2.out", delay: 0.15 });
        }

        // Trigger CSS entry animations on left & right text
        const stageLeft = document.getElementById('stage-left');
        const stageRight = document.getElementById('stage-right');
        stageLeft.style.animation = 'none';
        stageRight.style.animation = 'none';
        void stageLeft.offsetWidth; // trigger reflow
        stageLeft.style.animation = 'fadeInSlide 0.5s cubic-bezier(0.16, 1, 0.3, 1)';
        stageRight.style.animation = 'fadeInSlide 0.5s cubic-bezier(0.16, 1, 0.3, 1)';
    }
};

window.nextSlide = function() {
    const nextIdx = (currentSlide + 1) % PRODUCTS_DATA.length;
    goToSlide(nextIdx);
};

window.prevSlide = function() {
    const prevIdx = (currentSlide - 1 + PRODUCTS_DATA.length) % PRODUCTS_DATA.length;
    goToSlide(prevIdx);
};

// ── Wheel Scroll & Touch Detection on Showcase ────────────────
function setupTouchAndWheel() {
    const showcaseSection = document.getElementById('showcase');
    if (!showcaseSection) return;

    let isScrolling = false;
    let touchStartY = 0;

    window.addEventListener('wheel', (e) => {
        // If user is inside the showcase area
        const rect = showcaseSection.getBoundingClientRect();
        if (rect.top <= 10 && rect.bottom >= window.innerHeight * 0.7) {
            if (isScrolling) return;

            if (e.deltaY > 30) {
                if (currentSlide < PRODUCTS_DATA.length - 1) {
                    e.preventDefault();
                    isScrolling = true;
                    nextSlide();
                    setTimeout(() => { isScrolling = false; }, 650);
                }
            } else if (e.deltaY < -30) {
                if (currentSlide > 0) {
                    e.preventDefault();
                    isScrolling = true;
                    prevSlide();
                    setTimeout(() => { isScrolling = false; }, 650);
                }
            }
        }
    }, { passive: false });

    window.addEventListener('touchstart', (e) => {
        touchStartY = e.touches[0].clientY;
    });

    window.addEventListener('touchend', (e) => {
        const rect = showcaseSection.getBoundingClientRect();
        if (rect.top <= 20 && rect.bottom >= window.innerHeight * 0.7) {
            const touchEndY = e.changedTouches[0].clientY;
            const diffY = touchStartY - touchEndY;

            if (diffY > 50 && currentSlide < PRODUCTS_DATA.length - 1) {
                nextSlide();
            } else if (diffY < -50 && currentSlide > 0) {
                prevSlide();
            }
        }
    });
}

// ═══════════════════════════════════════════════════════════════
// 3. INTERACTIVE CLIENT ESTIMATOR
// ═══════════════════════════════════════════════════════════════
let selectedType = 'android';

window.selectProjectType = function(type, btn) {
    selectedType = type;
    document.querySelectorAll('.type-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    calculateEstimate();
};

window.calculateEstimate = function() {
    let baseDays = 12;
    let stackName = "Kotlin, Jetpack Compose, Room SQLite";

    if (selectedType === 'android') {
        baseDays = 12;
        stackName = "Kotlin 2.0, Jetpack Compose, Room SQLite, Material 3 Glass";
    } else if (selectedType === 'game') {
        baseDays = 14;
        stackName = "Flutter 3.x / Godot Engine, Canvas 2D/3D, Target SDK 35";
    } else if (selectedType === 'web3d') {
        baseDays = 8;
        stackName = "Three.js, WebGL, GSAP ScrollTrigger, Liquid Glass Obsidian";
    } else if (selectedType === 'ai') {
        baseDays = 10;
        stackName = "Rust, Multi-Agent Swarms, Model Context Protocol (MCP), n8n";
    }

    if (document.getElementById('opt-glass')?.checked) baseDays += 2;
    if (document.getElementById('opt-3d')?.checked) baseDays += 3;
    if (document.getElementById('opt-backend')?.checked) baseDays += 4;
    if (document.getElementById('opt-store')?.checked) baseDays += 2;

    const estimateTimeEl = document.getElementById('estimate-time');
    const estimateStackEl = document.getElementById('estimate-stack');

    if (estimateTimeEl) estimateTimeEl.innerText = `${baseDays - 2} à ${baseDays + 2} Jours`;
    if (estimateStackEl) estimateStackEl.innerText = stackName;
};

window.fillContactWithEstimate = function() {
    const timeText = document.getElementById('estimate-time')?.innerText || "14 jours";
    const stackText = document.getElementById('estimate-stack')?.innerText || "Kotlin & Compose";

    const textarea = document.getElementById('contact-message');
    if (textarea) {
        textarea.value = `Bonjour Anis / GHD Interactive Studio,\n\nJe souhaite concrétiser un projet de type : ${selectedType.toUpperCase()}.\n• Estimation calculée : ${timeText}\n• Stack recommandée : ${stackText}\n\nVoici le résumé de mon besoin : `;
        textarea.focus();
    }
};

window.copyContactEmail = function() {
    const email = "m_ghediri@yahoo.com";
    navigator.clipboard.writeText(email).then(() => {
        const btnText = document.getElementById('copy-email-btn-text');
        if (btnText) {
            const original = btnText.innerText;
            btnText.innerText = "✓ Email Copié (m_ghediri@yahoo.com) !";
            btnText.style.color = "#10B981";
            setTimeout(() => {
                btnText.innerText = original;
                btnText.style.color = "";
            }, 3000);
        }
    });
};

window.handleContactSubmit = function(e) {
    e.preventDefault();
    const name = document.getElementById('contact-name')?.value || "Client";
    const email = document.getElementById('contact-email')?.value || "";
    const msg = document.getElementById('contact-message')?.value || "";
    const feedback = document.getElementById('contactFeedback');

    const subject = encodeURIComponent(`[GHD Studio] Nouveau Projet - ${name}`);
    const body = encodeURIComponent(`Nom : ${name}\nEmail : ${email}\n\nDétails du projet :\n${msg}`);

    window.location.href = `mailto:m_ghediri@yahoo.com?subject=${subject}&body=${body}`;

    if (feedback) {
        feedback.style.display = 'block';
        feedback.innerHTML = `✓ Votre client email par défaut a été ouvert avec votre message pour <strong>m_ghediri@yahoo.com</strong>. Nous vous répondrons sous 24h !`;
    }
};
