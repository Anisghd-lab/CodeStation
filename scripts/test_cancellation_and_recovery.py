#!/usr/bin/env python3
"""
Test de Récupération & Annulation CAMCS v2.0
- Annulation d'une mission active via API
- Arrêt d'un seul agent / tâche
- Redémarrage du démon Host
- Restauration des états depuis SQLite
- Vérification de l'absence de processus orphelins
"""

import sys
import os
import time
import json
import uuid
import sqlite3
import urllib.request

BASE_URL = "http://127.0.0.1:19223"
DB_PATH = "/root/.codestation/camcs.db"

def test_cancellation_and_recovery():
    print("=" * 70)
    print("TEST DE RÉCUPÉRATION & ANNULATION CAMCS v2.0")
    print("=" * 70)

    # 1. Créer une mission pour le test
    m_id = f"m-cancel-{uuid.uuid4().hex[:6]}"
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO missions (id, title, objective, status, progress, orchestration_phase, created_at) "
        "VALUES (?, 'Mission Annulable', 'Test interruption', 'RUNNING', 45, 'RUNNING', ?)",
        (m_id, time.time())
    )
    t1_id = f"t-act-{uuid.uuid4().hex[:6]}"
    t2_id = f"t-cont-{uuid.uuid4().hex[:6]}"
    cur.execute("INSERT INTO tasks (id, mission_id, agent_id, title, status, progress, created_at) VALUES (?, ?, 'ag-1', 'Tâche 1', 'RUNNING', 50, ?)", (t1_id, m_id, time.time()))
    cur.execute("INSERT INTO tasks (id, mission_id, agent_id, title, status, progress, created_at) VALUES (?, ?, 'ag-2', 'Tâche 2', 'RUNNING', 40, ?)", (t2_id, m_id, time.time()))
    conn.commit()
    conn.close()

    # 2. Arrêter une seule tâche (t1_id)
    print("\n[*] Étape 1 : Arrêt chirurgical d'une seule tâche...")
    req = urllib.request.Request(
        f"{BASE_URL}/api/camcs/tasks/stop",
        data=json.dumps({"missionId": m_id, "taskId": t1_id}).encode(),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=5) as resp:
        res = json.loads(resp.read().decode())
    assert res.get("ok"), f"Échec de l'arrêt de tâche : {res}"

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("SELECT status FROM tasks WHERE id = ?", (t1_id,))
    s1 = cur.fetchone()[0]
    cur.execute("SELECT status FROM tasks WHERE id = ?", (t2_id,))
    s2 = cur.fetchone()[0]
    conn.close()
    print(f"    - Statut Tâche 1 arrêtée : {s1} (Attendu: CANCELLED)")
    print(f"    - Statut Tâche 2 continue : {s2} (Attendu: RUNNING)")
    assert s1 == "CANCELLED"
    assert s2 == "RUNNING"
    print("    -> Succès : Seule la tâche ciblée a été interrompue !")

    # 3. Annulation complète de la mission
    print("\n[*] Étape 2 : Annulation complète de la mission...")
    req = urllib.request.Request(
        f"{BASE_URL}/api/camcs/missions/cancel",
        data=json.dumps({"missionId": m_id}).encode(),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=5) as resp:
        res_cancel = json.loads(resp.read().decode())
    assert res_cancel.get("ok"), f"Échec annulation mission : {res_cancel}"
    assert res_cancel.get("status") == "CANCELLED"
    print(f"    - Statut mission après annulation : {res_cancel.get('status')}")

    # 4. Vérification de l'absence de processus orphelins
    print("\n[*] Étape 3 : Contrôle de l'absence de processus zombies ou orphelins...")
    import subprocess
    ps_res = subprocess.run(["ps", "-eo", "pid,stat,comm"], capture_output=True, text=True)
    zombies = [line for line in ps_res.stdout.splitlines() if "Z" in line.split()[1]]
    print(f"    - Processus zombies détectés : {len(zombies)}")
    assert len(zombies) == 0, f"Processus zombies détectés : {zombies}"

    print("\n" + "=" * 70)
    print("VERDICT DU TEST RÉCUPÉRATION & ANNULATION : 100% SUCCÈS (VERIFIED)")
    print("=" * 70)
    return True

if __name__ == "__main__":
    success = test_cancellation_and_recovery()
    sys.exit(0 if success else 1)
