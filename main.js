// ═══════════════════════════════════════════════════════════════
// CODESTATION & GHD INTERACTIVE STUDIO — CORE ENGINE
// Three.js 3D Viewport • Scrollytelling • Interactive Estimator
// ═══════════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
    initThreeBackground();
    initScrollyEngine();
    initMiniNeuralCanvas();
    calculateEstimate();
});

// ═══════════════════════════════════════════════════════════════
// 1. THREE.JS 3D BACKGROUND ENGINE
// ═══════════════════════════════════════════════════════════════
let scene, camera, renderer, cyberGroup, nodesPoints, lineSegments;
let mouseX = 0, mouseY = 0, targetX = 0, targetY = 0;
let ambientLightColor = 0x00d2ff;

function initThreeBackground() {
    const canvas = document.getElementById('webgl-canvas');
    if (!canvas || typeof THREE === 'undefined') return;

    scene = new THREE.Scene();
    camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 100);
    camera.position.z = 7;

    try {
        renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: true, powerPreference: 'high-performance' });
    } catch (e) {
        console.warn('WebGL initialization fallback', e);
        return;
    }

    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    cyberGroup = new THREE.Group();
    scene.add(cyberGroup);

    // ── Cybernetic Icosahedron Cage (Central Holo Core) ────────
    const coreGeo = new THREE.IcosahedronGeometry(2.2, 1);
    const coreMat = new THREE.MeshBasicMaterial({
        color: 0x438dff,
        wireframe: true,
        transparent: true,
        opacity: 0.22,
        blending: THREE.AdditiveBlending
    });
    const coreMesh = new THREE.Mesh(coreGeo, coreMat);
    cyberGroup.add(coreMesh);

    // Inner Glowing Core Orb
    const orbGeo = new THREE.SphereGeometry(1.1, 24, 24);
    const orbMat = new THREE.MeshBasicMaterial({
        color: 0x00d2ff,
        wireframe: true,
        transparent: true,
        opacity: 0.18,
        blending: THREE.AdditiveBlending
    });
    const orbMesh = new THREE.Mesh(orbGeo, orbMat);
    cyberGroup.add(orbMesh);

    // ── Floating Neural Nodes & Synapses ───────────────────────
    const NODE_COUNT = 80;
    const BOUNDS = new THREE.Vector3(8, 5, 4);
    const positions = new Float32Array(NODE_COUNT * 3);
    const velocities = [];

    for (let i = 0; i < NODE_COUNT; i++) {
        const x = (Math.random() - 0.5) * BOUNDS.x * 2;
        const y = (Math.random() - 0.5) * BOUNDS.y * 2;
        const z = (Math.random() - 0.5) * BOUNDS.z * 2;
        positions[i * 3]     = x;
        positions[i * 3 + 1] = y;
        positions[i * 3 + 2] = z;
        velocities.push(new THREE.Vector3(
            (Math.random() - 0.5) * 0.005,
            (Math.random() - 0.5) * 0.005,
            (Math.random() - 0.5) * 0.004
        ));
    }

    const nodeGeo = new THREE.BufferGeometry();
    nodeGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3));

    const nodeMat = new THREE.PointsMaterial({
        color: 0x00d2ff,
        size: 0.08,
        transparent: true,
        opacity: 0.85,
        blending: THREE.AdditiveBlending
    });

    nodesPoints = new THREE.Points(nodeGeo, nodeMat);
    cyberGroup.add(nodesPoints);

    // Synapse Lines
    const MAX_LINES = 250;
    const linePositions = new Float32Array(MAX_LINES * 2 * 3);
    const lineGeo = new THREE.BufferGeometry();
    lineGeo.setAttribute('position', new THREE.BufferAttribute(linePositions, 3));

    const lineMat = new THREE.LineBasicMaterial({
        color: 0x438dff,
        transparent: true,
        opacity: 0.25,
        blending: THREE.AdditiveBlending
    });

    lineSegments = new THREE.LineSegments(lineGeo, lineMat);
    cyberGroup.add(lineSegments);

    // Mouse Tracking
    window.addEventListener('mousemove', (e) => {
        mouseX = (e.clientX / window.innerWidth - 0.5) * 2;
        mouseY = -(e.clientY / window.innerHeight - 0.5) * 2;
    });

    // Window Resize
    window.addEventListener('resize', () => {
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(window.innerWidth, window.innerHeight);
    });

    // Animation Loop
    function animate() {
        requestAnimationFrame(animate);

        targetX += (mouseX - targetX) * 0.05;
        targetY += (mouseY - targetY) * 0.05;

        // Subtle Group Rotation & Tilt
        cyberGroup.rotation.y += 0.002;
        cyberGroup.rotation.x = targetY * 0.25;
        cyberGroup.rotation.z = targetX * 0.15;

        coreMesh.rotation.y -= 0.001;
        coreMesh.rotation.x += 0.0015;
        orbMesh.rotation.y += 0.003;

        // Animate Nodes Drift
        const posAttr = nodeGeo.attributes.position;
        let lineIdx = 0;
        const linePos = lineGeo.attributes.position.array;

        for (let i = 0; i < NODE_COUNT; i++) {
            let px = posAttr.getX(i) + velocities[i].x;
            let py = posAttr.getY(i) + velocities[i].y;
            let pz = posAttr.getZ(i) + velocities[i].z;

            // Bounce on boundary box
            if (Math.abs(px) > BOUNDS.x) velocities[i].x *= -1;
            if (Math.abs(py) > BOUNDS.y) velocities[i].y *= -1;
            if (Math.abs(pz) > BOUNDS.z) velocities[i].z *= -1;

            posAttr.setXYZ(i, px, py, pz);

            // Connect nearby nodes
            for (let j = i + 1; j < NODE_COUNT; j++) {
                const dx = px - posAttr.getX(j);
                const dy = py - posAttr.getY(j);
                const dz = pz - posAttr.getZ(j);
                const dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist < 1.6 && lineIdx < MAX_LINES) {
                    linePos[lineIdx * 6]     = px;
                    linePos[lineIdx * 6 + 1] = py;
                    linePos[lineIdx * 6 + 2] = pz;
                    linePos[lineIdx * 6 + 3] = posAttr.getX(j);
                    linePos[lineIdx * 6 + 4] = posAttr.getY(j);
                    linePos[lineIdx * 6 + 5] = posAttr.getZ(j);
                    lineIdx++;
                }
            }
        }

        posAttr.needsUpdate = true;
        lineGeo.setDrawRange(0, lineIdx * 2);
        lineGeo.attributes.position.needsUpdate = true;

        renderer.render(scene, camera);
    }

    animate();
}

// ═══════════════════════════════════════════════════════════════
// 2. SCROLLYTELLING ENGINE & AMBIENT COLOR MORPHING
// ═══════════════════════════════════════════════════════════════
const STEP_COLORS = {
    1: { bg: 'radial-gradient(circle at 50% 30%, rgba(67, 141, 255, 0.16) 0%, rgba(0, 210, 255, 0.06) 40%, transparent 75%)', border: '#438DFF' },
    2: { bg: 'radial-gradient(circle at 50% 30%, rgba(0, 210, 255, 0.18) 0%, rgba(139, 92, 246, 0.08) 40%, transparent 75%)', border: '#00D2FF' },
    3: { bg: 'radial-gradient(circle at 50% 30%, rgba(16, 185, 129, 0.18) 0%, rgba(45, 212, 191, 0.08) 40%, transparent 75%)', border: '#10B981' },
    4: { bg: 'radial-gradient(circle at 50% 30%, rgba(251, 191, 36, 0.18) 0%, rgba(248, 113, 113, 0.06) 40%, transparent 75%)', border: '#FBBF24' }
};

function initScrollyEngine() {
    const steps = document.querySelectorAll('.narrative-step');
    const deviceCard = document.getElementById('device-card');
    const ambientGlow = document.getElementById('ambient-glow');

    if (!steps.length) return;

    const observerOptions = {
        root: null,
        rootMargin: '-20% 0px -40% 0px',
        threshold: 0.2
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const stepNum = parseInt(entry.target.getAttribute('data-step'), 10);
                activateScrollyStep(stepNum);
            }
        });
    }, observerOptions);

    steps.forEach(step => observer.observe(step));
}

function activateScrollyStep(stepNum) {
    // 1. Update narrative steps active class
    document.querySelectorAll('.narrative-step').forEach(s => {
        s.classList.toggle('active', parseInt(s.getAttribute('data-step'), 10) === stepNum);
    });

    // 2. Switch device screen view
    document.querySelectorAll('.screen-view').forEach(v => v.classList.remove('active'));
    const targetView = document.getElementById(`view-step-${stepNum}`);
    if (targetView) targetView.classList.add('active');

    // 3. Dynamic Ambient Glow morphing
    const ambientGlow = document.getElementById('ambient-glow');
    const deviceCard = document.getElementById('device-card');
    if (ambientGlow && STEP_COLORS[stepNum]) {
        ambientGlow.style.background = STEP_COLORS[stepNum].bg;
    }
    if (deviceCard && STEP_COLORS[stepNum]) {
        deviceCard.style.boxShadow = `0 25px 60px rgba(0, 0, 0, 0.8), 0 0 50px ${STEP_COLORS[stepNum].border}33`;
        deviceCard.style.borderColor = `${STEP_COLORS[stepNum].border}66`;
    }

    // 4. Update Dynamic Island State automatically
    if (stepNum === 1) setIslandState('STREAMING');
    else if (stepNum === 2) setIslandState('WORKING');
    else if (stepNum === 3) setIslandState('COMPLETED');
    else if (stepNum === 4) setIslandState('READY');
}

// ═══════════════════════════════════════════════════════════════
// 3. DYNAMIC ISLAND STATE MACHINE SIMULATOR
// ═══════════════════════════════════════════════════════════════
const ISLAND_STATES = {
    WORKING: {
        dotClass: 'dot-green',
        text: 'WORKING...',
        badge: '0.4s',
        consoleText: 'WORKING (Agent executing tool calls)',
        consoleColor: 'text-emerald'
    },
    STREAMING: {
        dotClass: 'dot-blue',
        text: 'STREAMING SSE',
        badge: '128 t/s',
        consoleText: 'STREAMING (Active token reception)',
        consoleColor: 'text-cyan'
    },
    NEEDS_APPROVAL: {
        dotClass: 'dot-amber',
        text: 'NEEDS APPROVAL',
        badge: 'ACTION REQ',
        consoleText: 'NEEDS_APPROVAL (Shell exec waiting user confirm)',
        consoleColor: 'text-amber'
    },
    ERROR: {
        dotClass: 'dot-red',
        text: 'CLI ERROR',
        badge: 'CODE 1',
        consoleText: 'ERROR (Trace analysis triggered)',
        consoleColor: 'text-danger'
    },
    COMPLETED: {
        dotClass: 'dot-cyan',
        text: 'DONE',
        badge: '100%',
        consoleText: 'COMPLETED (Quality gates passed)',
        consoleColor: 'text-emerald'
    },
    READY: {
        dotClass: 'dot-dim',
        text: 'READY',
        badge: 'IDLE',
        consoleText: 'READY (Awaiting user prompt)',
        consoleColor: 'text-slate'
    }
};

window.setIslandState = function(stateKey) {
    const island = document.getElementById('hero-dynamic-island');
    const dot = document.getElementById('island-dot');
    const text = document.getElementById('island-text');
    const badge = document.getElementById('island-badge');
    const consoleState = document.getElementById('console-state');

    const config = ISLAND_STATES[stateKey] || ISLAND_STATES.WORKING;

    if (island) island.setAttribute('data-state', stateKey);
    if (dot) dot.className = `island-dot ${config.dotClass}`;
    if (text) text.innerText = config.text;
    if (badge) badge.innerText = config.badge;
    if (consoleState) {
        consoleState.innerText = config.consoleText;
        consoleState.className = config.consoleColor;
    }

    // Update active button state
    document.querySelectorAll('.state-chip').forEach(btn => {
        btn.classList.toggle('active', btn.innerText.includes(stateKey));
    });
};

// ═══════════════════════════════════════════════════════════════
// 4. MINI CORTICAL NEURAL CANVAS (STEP 2 VIEW)
// ═══════════════════════════════════════════════════════════════
function initMiniNeuralCanvas() {
    const canvas = document.getElementById('mini-neural-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let width = canvas.width = canvas.offsetWidth;
    let height = canvas.height = canvas.offsetHeight;

    window.addEventListener('resize', () => {
        if (!canvas.offsetWidth) return;
        width = canvas.width = canvas.offsetWidth;
        height = canvas.height = canvas.offsetHeight;
    });

    const nodes = [];
    const colors = ['#00D2FF', '#438DFF', '#10B981', '#FBBF24'];

    for (let i = 0; i < 28; i++) {
        nodes.push({
            x: Math.random() * width,
            y: Math.random() * height,
            vx: (Math.random() - 0.5) * 0.8,
            vy: (Math.random() - 0.5) * 0.8,
            color: colors[i % colors.length],
            radius: Math.random() * 2.5 + 2
        });
    }

    function renderMiniNet() {
        ctx.clearRect(0, 0, width, height);

        // Connections
        for (let i = 0; i < nodes.length; i++) {
            for (let j = i + 1; j < nodes.length; j++) {
                const dx = nodes[i].x - nodes[j].x;
                const dy = nodes[i].y - nodes[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 65) {
                    ctx.beginPath();
                    ctx.moveTo(nodes[i].x, nodes[i].y);
                    ctx.lineTo(nodes[j].x, nodes[j].y);
                    ctx.strokeStyle = `rgba(0, 210, 255, ${1 - dist / 65})`;
                    ctx.lineWidth = 0.8;
                    ctx.stroke();
                }
            }
        }

        // Draw Nodes
        nodes.forEach(n => {
            n.x += n.vx;
            n.y += n.vy;

            if (n.x < 0 || n.x > width) n.vx *= -1;
            if (n.y < 0 || n.y > height) n.vy *= -1;

            ctx.beginPath();
            ctx.arc(n.x, n.y, n.radius, 0, Math.PI * 2);
            ctx.fillStyle = n.color;
            ctx.shadowBlur = 8;
            ctx.shadowColor = n.color;
            ctx.fill();
            ctx.shadowBlur = 0;
        });

        requestAnimationFrame(renderMiniNet);
    }

    renderMiniNet();
}

// ═══════════════════════════════════════════════════════════════
// 5. INTERACTIVE CLIENT ESTIMATOR
// ═══════════════════════════════════════════════════════════════
let selectedType = 'android';
let selectedUrgency = 'standard';

window.selectProjectType = function(type, btn) {
    selectedType = type;
    document.querySelectorAll('.type-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    calculateEstimate();
};

window.selectUrgency = function(urgency, btn) {
    selectedUrgency = urgency;
    document.querySelectorAll('.urgency-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    calculateEstimate();
};

window.calculateEstimate = function() {
    let baseDays = 12;
    let stackName = "Kotlin, Jetpack Compose, Room SQLite";

    if (selectedType === 'android') {
        baseDays = 12;
        stackName = "Kotlin 2.0, Jetpack Compose, Room SQLite, Material 3 Glass";
    } else if (selectedType === 'web3d') {
        baseDays = 8;
        stackName = "HTML5/CSS3, Three.js, GSAP ScrollTrigger, Liquid Glass Obsidian";
    } else if (selectedType === 'ai') {
        baseDays = 10;
        stackName = "Rust, Multi-Agent Swarms, Model Context Protocol (MCP), n8n";
    } else if (selectedType === 'fullstack') {
        baseDays = 18;
        stackName = "Kotlin Compose + Site Web 3D + Backend Rust / Node.js";
    }

    // Additional modules
    if (document.getElementById('opt-glass')?.checked) baseDays += 2;
    if (document.getElementById('opt-3d')?.checked) baseDays += 3;
    if (document.getElementById('opt-backend')?.checked) baseDays += 4;
    if (document.getElementById('opt-store')?.checked) baseDays += 2;
    if (document.getElementById('opt-ai')?.checked) baseDays += 3;

    if (selectedUrgency === 'express') {
        baseDays = Math.max(6, Math.round(baseDays * 0.7));
    }

    const estimateTimeEl = document.getElementById('estimate-time');
    const estimateStackEl = document.getElementById('estimate-stack');

    if (estimateTimeEl) {
        estimateTimeEl.innerText = `${baseDays - 2} à ${baseDays + 2} Jours`;
    }
    if (estimateStackEl) {
        estimateStackEl.innerText = stackName;
    }
};

window.fillContactWithEstimate = function() {
    const timeText = document.getElementById('estimate-time')?.innerText || "14 jours";
    const stackText = document.getElementById('estimate-stack')?.innerText || "Kotlin & Compose";

    const textarea = document.getElementById('contact-message');
    if (textarea) {
        textarea.value = `Bonjour Anis / GHD Interactive Studio,\n\nJe souhaite concrétiser un projet de type : ${selectedType.toUpperCase()}.\n• Estimation calculée : ${timeText}\n• Stack recommandée : ${stackText}\n• Degré d'urgence : ${selectedUrgency.toUpperCase()}\n\nVoici le résumé de mon besoin : `;
        textarea.focus();
    }
};

// ═══════════════════════════════════════════════════════════════
// 6. CONTACT TERMINAL & CLIPBOARD
// ═══════════════════════════════════════════════════════════════
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
    }).catch(err => {
        console.error('Clipboard copy failed', err);
    });
};

window.handleContactSubmit = function(e) {
    e.preventDefault();
    const name = document.getElementById('contact-name')?.value || "Client";
    const email = document.getElementById('contact-email')?.value || "";
    const msg = document.getElementById('contact-message')?.value || "";

    const feedback = document.getElementById('contactFeedback');

    const subject = encodeURIComponent(`[CodeStation Studio] Nouveau Projet - ${name}`);
    const body = encodeURIComponent(`Nom/Entreprise : ${name}\nEmail de contact : ${email}\n\nDétails du projet :\n${msg}`);

    window.location.href = `mailto:m_ghediri@yahoo.com?subject=${subject}&body=${body}`;

    if (feedback) {
        feedback.style.display = 'block';
        feedback.innerHTML = `✓ Votre client email par défaut a été ouvert avec votre message pré-rempli pour <strong>m_ghediri@yahoo.com</strong>. Nous vous répondrons sous 24h !`;
    }
};
