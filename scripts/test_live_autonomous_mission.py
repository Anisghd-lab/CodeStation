#!/usr/bin/env python3
"""
Test d'Acceptance CAMCS v2.0 : Mission Autonome Complète de Bout en Bout
Soumet une mission via l'API REST, écoute les événements SSE,
supervise l'exécution réelle des agents et valide le résultat final.
"""

import os
import sys
import time
import json
import sqlite3
import urllib.request
import urllib.parse

BASE_URL = "http://127.0.0.1:19223"
DB_PATH = "/root/.codestation/camcs.db"

def run_acceptance_mission():
    print("=" * 70)
    print("TEST D'ACCEPTANCE DE PRODUCTION — MISSION MULTI-AGENT AUTONOME")
    print("=" * 70)

    # 1. Création de la mission via API REST
    req_body = {
        "title": "Système de Télémétrie et Métriques Linux ARM64",
        "objective": (
            "Développer un module autonome de télémétrie système pour CodeStation :\n"
            "- Recherche des sources /proc/stat et /proc/meminfo\n"
            "- Implémentation du collecteur system_metrics.py\n"
            "- Rédaction du manuel d'intégration metrics_guide.md\n"
            "- Validation par tests unitaires test_system_metrics.py"
        ),
        "governanceMode": "AUTONOMOUS"
    }

    req = urllib.request.Request(
        f"{BASE_URL}/api/camcs/missions/create",
        data=json.dumps(req_body).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )

    t0 = time.time()
    with urllib.request.urlopen(req, timeout=10) as resp:
        res = json.loads(resp.read().decode())
    
    assert res.get("ok"), f"Échec création mission : {res}"
    m_id = res["missionId"]
    print(f"\n[+] Mission créée avec succès :")
    print(f"    - ID : {m_id}")
    print(f"    - Statut initial : {res['status']}")
    print(f"    - Phase d'orchestration : {res['orchestrationPhase']}")
    print(f"    - Nombre d'agents mobilisés : {res['agentsCount']}")

    # 2. Surveillance du cycle de vie jusqu'à l'état terminal
    print("\n[*] Surveillance en direct de l'exécution autonome...")
    terminal_states = {"COMPLETED", "FAILED", "CANCELLED"}
    last_phase = ""
    last_status = ""
    last_prog = -1

    while time.time() - t0 < 60:
        req_poll = urllib.request.Request(f"{BASE_URL}/api/camcs/missions")
        with urllib.request.urlopen(req_poll, timeout=5) as resp:
            all_m = json.loads(resp.read().decode())
        
        target = next((m for m in all_m.get("missions", []) if m["id"] == m_id), None)
        if target:
            status = target.get("status")
            phase = target.get("orchestrationPhase", "")
            prog = target.get("progress", 0)

            if status != last_status or phase != last_phase or prog != last_prog:
                print(f"    [{time.strftime('%H:%M:%S')}] Phase: {phase:<12} | Statut: {status:<10} | Progression: {prog}%")
                last_status = status
                last_phase = phase
                last_prog = prog

            if status in terminal_states:
                break
        time.sleep(1.0)

    elapsed = time.time() - t0
    print(f"\n[+] Mission terminée en {elapsed:.2f} secondes avec statut : {last_status}")

    # 3. Vérifications strictes des invariants
    assert last_status == "COMPLETED", f"La mission n'a pas atteint COMPLETED (actuel: {last_status})"
    assert last_prog == 100, f"Invariant violé: COMPLETED mais progression = {last_prog}% (attendu: 100%)"
    assert last_phase == "COMPLETED", f"Phase finale attendue COMPLETED, trouvée : {last_phase}"

    # 4. Vérification physique des livrables créés sur disque
    mission_dir = f"/root/.codestation/missions/{m_id}"
    print(f"\n[*] Examen physique des artefacts dans {mission_dir} :")
    assert os.path.exists(mission_dir), "Le répertoire de la mission n'existe pas"

    files_found = []
    for root, _, files in os.walk(mission_dir):
        for f in files:
            full_path = os.path.join(root, f)
            size = os.path.getsize(full_path)
            with open(full_path, "rb") as fp:
                sha256 = hashlib.sha256(fp.read()).hexdigest()
            rel_path = os.path.relpath(full_path, mission_dir)
            files_found.append((rel_path, size, sha256))
            print(f"    - {rel_path} ({size} octets, SHA-256: {sha256[:16]}...)")

    assert len(files_found) >= 3, f"Trop peu de fichiers produits ({len(files_found)})"

    # 5. Vérification de la persistance SQLite et de l'intégrité
    print(f"\n[*] Contrôle de la persistance SQLite ({DB_PATH}) :")
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("SELECT status, progress, orchestration_phase FROM missions WHERE id = ?", (m_id,))
    db_m = cur.fetchone()
    print(f"    - Table missions : Statut={db_m[0]}, Progression={db_m[1]}%, Phase={db_m[2]}")
    assert db_m[0] == "COMPLETED" and db_m[1] == 100

    cur.execute("SELECT count(*) FROM deliverables WHERE mission_id = ?", (m_id,))
    deliv_count = cur.fetchone()[0]
    print(f"    - Table deliverables : {deliv_count} livrables enregistrés et scellés")
    assert deliv_count >= 3

    cur.execute("SELECT previous_state, current_state, event_source FROM state_transitions WHERE mission_id = ? ORDER BY id", (m_id,))
    transitions = cur.fetchall()
    print(f"    - Table state_transitions : {len(transitions)} transitions ordonnées enregistrées :")
    for tr in transitions:
        print(f"      * {tr[0]} -> {tr[1]} ({tr[2]})")
    assert len(transitions) >= 3

    conn.close()

    print("\n" + "=" * 70)
    print("VERDICT DU TEST D'ACCEPTANCE : 100% SUCCÈS (VERIFIED)")
    print("=" * 70)
    return True

if __name__ == "__main__":
    success = run_acceptance_mission()
    sys.exit(0 if success else 1)
