#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Grand Project Autonomous Execution & Step-by-Step Audit
Executes and audits a complete multi-agent project using the 283 Agency catalog.
"""

import os
import sys
import time
import json
import hashlib
import urllib.request
import urllib.error

BASE_URL = "http://127.0.0.1:19223"

def http_get(path: str):
    req = urllib.request.Request(f"{BASE_URL}{path}")
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())

def http_post(path: str, data: dict):
    req = urllib.request.Request(
        f"{BASE_URL}{path}",
        data=json.dumps(data).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())

def compute_sha256(filepath: str) -> str:
    if not os.path.isfile(filepath):
        return ""
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

def main():
    print("=======================================================================")
    print(" 🚀 CAMCS v2.0 — DÉPLOIEMENT & AUDIT PAS-À-PAS D'UN GRAND PROJET")
    print("=======================================================================")

    # 1. Vérification du moteur CAMCS
    st = http_get("/api/camcs/status")
    print(f"\n[1/5] État du Moteur CAMCS : {st['engine']} v{st['version']}")
    print(f"      ├─ 100% Modèles Gratuits et Locaux : {st['freeModelsOnly']}")

    # 2. Interrogation des Agents & CLIs opérationnels
    clis_data = http_get("/api/camcs/clis")
    available_clis = [c for c in clis_data["clis"] if c["isOperational"]]
    print(f"\n[2/5] CLIs Réels Détectés & Opérationnels ({len(available_clis)}) :")
    for c in available_clis:
        print(f"      ├─ [{c['id'].upper()}] {c['name']} (v{c['version']}) -> Latence: {c.get('latencyMs', 0)}ms | Modèles: {', '.join(c.get('accessibleModels', [])[:2])}")

    # 3. Création du Grand Projet
    project_title = "Grand Projet Clean Architecture : Système Multi-Agents Haute Performance"
    project_objective = (
        "Concevoir, implémenter et auditer une architecture micro-services Kotlin/Clean Architecture "
        "avec modules Domain, Data, UI, tests de non-régression et audit de sécurité indépendant."
    )

    print(f"\n[3/5] Initialisation du Grand Projet : « {project_title} »...")
    create_payload = {
        "title": project_title,
        "objective": project_objective,
        "governanceMode": "HYBRID"
    }
    create_res = http_post("/api/camcs/missions/create", create_payload)
    assert create_res.get("ok"), f"Échec création : {create_res}"
    mission_id = create_res["missionId"]
    print(f"      ├─ Mission ID : {mission_id}")
    print(f"      ├─ Statut initial : {create_res.get('status', 'PLANNING')}")

    # Récupération des détails de la mission créée
    m_info = http_get(f"/api/camcs/missions/{mission_id}")
    mission = m_info.get("mission", {})
    agents = mission.get("agents", [])
    print(f"      ├─ Équipe Spécialisée Agency Mobilisée ({len(agents)} agents) :")
    for ag in agents:
        agency_slug = ag.get("agencySlug") or "agency-standard"
        print(f"      │   🎭 {ag['name']} ({ag['role']})")
        print(f"      │      └─ CLI: [{ag['cli']}] | Modèle: {ag['model']} | Skill: ~/.agents/skills/{agency_slug}")

    # 4. Suivi pas-à-pas de l'Exécution du DAG
    print(f"\n[4/5] Suivi Dynamique de l'Exécution du DAG (Topologie Kahn)...")
    last_phase = ""
    last_progress = -1
    start_time = time.time()
    max_wait_seconds = 180

    while time.time() - start_time < max_wait_seconds:
        m_state = http_get(f"/api/camcs/missions/{mission_id}")
        m_curr = m_state.get("mission", {})
        curr_status = m_curr.get("status")
        curr_phase = m_curr.get("orchestrationPhase", "PLANNING")
        curr_progress = m_curr.get("progress", 0)

        if curr_phase != last_phase or curr_progress != last_progress:
            last_phase = curr_phase
            last_progress = curr_progress
            print(f"      ├─ Phase: [{curr_phase:12s}] | Progression: {curr_progress:3d}% | Statut: {curr_status}")
            tasks = m_curr.get("tasks", [])
            for t in tasks:
                t_stat = t.get("status")
                if t_stat in ("RUNNING", "COMPLETED"):
                    print(f"      │   • [{t_stat:9s}] {t.get('title')[:65]}")

        if curr_status in ("COMPLETED", "FAILED", "BLOCKED", "CANCELLED"):
            break
        time.sleep(1.0)

    # 5. Audit Indépendant des Livrables et Preuves Cryptographiques
    print(f"\n[5/5] Audit des Livrables Physiques & Preuves SHA-256 scellées :")
    delivs_res = http_get(f"/api/camcs/missions/{mission_id}/deliverables")
    delivs = delivs_res.get("deliverables", [])
    print(f"      ├─ Nombre total de livrables enregistrés : {len(delivs)}")
    
    verified_count = 0
    for d in delivs:
        fpath = d.get("filePath", "")
        if os.path.isfile(fpath):
            fsize = os.path.getsize(fpath)
            sha = compute_sha256(fpath)
            verified_count += 1
            print(f"      ├─ ✅ [CERTIFIÉ] {os.path.basename(fpath)}")
            print(f"      │   ├─ Chemin : {fpath}")
            print(f"      │   ├─ Taille : {fsize} octets")
            print(f"      │   ├─ SHA-256: {sha[:32]}...")
            print(f"      │   └─ Produit par : {d.get('producingCli', 'cli')}")
        else:
            print(f"      ├─ ℹ️ [ACTION/URL] {d.get('description', '')} -> {fpath}")

    # Rapport final
    print(f"\n=======================================================================")
    print(f" 🎯 RÉSULTAT DE L'AUDIT DU GRAND PROJET : {curr_status}")
    print(f" ⏱️  Durée totale d'exécution : {int(time.time() - start_time)} secondes")
    print(f" 📊 Livrables physiques audités et vérifiés : {verified_count}/{len(delivs)}")
    print(f"=======================================================================")
    return curr_status == "COMPLETED"

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
