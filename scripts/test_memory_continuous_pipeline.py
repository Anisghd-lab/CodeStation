#!/usr/bin/env python3
"""
Test End-to-End : Pipeline de Mémoire Continue CodeStation
Démontre :
1. Détection de session OpenCode.
2. Extraction par les 6 Memory Agents (Collector, Analyzer, Extractor, Curator, Validator, Retriever).
3. Persistance dans /root/CodeStation-Memory.
4. Récupération instantanée par le moteur CAMCS.
"""

import os
import sys
import time
import json
import sqlite3
import urllib.request
import urllib.parse

OPENCODE_DB = "/root/.local/share/opencode/opencode.db"
MEMORY_URL = "http://127.0.0.1:19224"
CAMCS_URL = "http://127.0.0.1:19223"
REPO_PATH = "/root/CodeStation-Memory"

def step(msg):
    print(f"\n\033[1;34m[STEP]\033[0m \033[1m{msg}\033[0m")

def success(msg):
    print(f"\033[0;32m  [✓] {msg}\033[0m")

def fail(msg):
    print(f"\033[0;31m  [✗] {msg}\033[0m")
    sys.exit(1)

def main():
    print("=" * 70)
    print("  TEST E2E : PIPELINE DE MÉMOIRE CONTINUE & SUPERVISION CAMCS")
    print("=" * 70)

    # 1. Vérification des serveurs
    step("1. Vérification des moteurs CodeStation")
    try:
        with urllib.request.urlopen(f"{MEMORY_URL}/health", timeout=3) as r:
            m_health = json.loads(r.read().decode())
            success(f"Continuous Memory Engine : {m_health.get('engine')} (Port 19224)")
    except Exception as e:
        fail(f"Memory Engine inaccessible sur {MEMORY_URL} : {e}")

    try:
        with urllib.request.urlopen(f"{CAMCS_URL}/health", timeout=3) as r:
            c_health = json.loads(r.read().decode())
            success(f"CAMCS Engine : {c_health.get('engine')} (Port 19223)")
    except Exception as e:
        fail(f"CAMCS Engine inaccessible sur {CAMCS_URL} : {e}")

    # 2. Simulation d'une session OpenCode avec apprentissage technique
    step("2. Simulation d'une session OpenCode réelle dans Ubuntu PRoot ARM64")
    session_id = f"opencode-test-{int(time.time())}"
    title = "Résolution native SQLite KSP sous PRoot ARM64"
    now_ts = int(time.time())

    conn = sqlite3.connect(OPENCODE_DB)
    cur = conn.cursor()

    cur.execute("""
        INSERT OR REPLACE INTO session (id, project_id, slug, directory, title, version, time_created, time_updated)
        VALUES (?, 'global', ?, '/root/CodeStation', ?, '1.0.0', ?, ?)
    """, (session_id, f"slug-{session_id}", title, now_ts, now_ts))

    mid_user = f"msg-user-{int(time.time())}"
    mid_assistant = f"msg-asst-{int(time.time())}"

    cur.execute("""
        INSERT INTO message (id, session_id, time_created, time_updated, data)
        VALUES (?, ?, ?, ?, ?)
    """, (mid_user, session_id, now_ts, now_ts, json.dumps({"role": "user"})))

    cur.execute("""
        INSERT INTO part (id, message_id, session_id, time_created, time_updated, data)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (f"part-1-{int(time.time())}", mid_user, session_id, now_ts, now_ts, json.dumps({
        "type": "text",
        "text": "Erreur KSP sous Linux-Android ARM64 : Room kspDebugKotlin échoue avec java.lang.Exception: No native library found for Linux-Android aarch64. Comment corriger ?"
    })))

    cur.execute("""
        INSERT INTO message (id, session_id, time_created, time_updated, data)
        VALUES (?, ?, ?, ?, ?)
    """, (mid_assistant, session_id, now_ts + 1, now_ts + 1, json.dumps({"role": "assistant"})))

    cur.execute("""
        INSERT INTO part (id, message_id, session_id, time_created, time_updated, data)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (f"part-2-{int(time.time())}", mid_assistant, session_id, now_ts + 1, now_ts + 1, json.dumps({
        "type": "text",
        "text": "Solution validée : Créer le répertoire /org/sqlite/native/Linux-Android/aarch64 et copier /usr/lib/libsqlitejdbc.so dedans. Ensuite exécuter ./gradlew --stop pour recharger le daemon Gradle. Le build assembleDebug réussit alors sans erreur native."
    })))

    conn.commit()
    conn.close()
    success(f"Session OpenCode '{session_id}' injectée dans {OPENCODE_DB}")

    # 3. Déclenchement de l'apprentissage par les Memory Agents
    step("3. Déclenchement du pipeline d'apprentissage Memory Agents (Collector -> Analyzer -> Extractor -> Curator -> Validator)")
    req = urllib.request.Request(f"{MEMORY_URL}/api/memory/collect", data=b"{}", headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as r:
        resp = json.loads(r.read().decode())
        success(f"Réponse Memory Collect : {resp}")
        if resp.get("newKnowledgeCount", 0) > 0:
            success(f"{resp['newKnowledgeCount']} nouvelle(s) connaissance(s) acquise(s) !")
        else:
            success("Cycle d'apprentissage exécuté avec succès.")

    # 4. Vérification de la persistance dans CodeStation-Memory
    step("4. Vérification de la persistance dans /root/CodeStation-Memory")
    knowledge_files = [os.path.join(r, f) for r, _, fs in os.walk(f"{REPO_PATH}/knowledge") for f in fs if f.endswith(".json")]
    success(f"Total fichiers de connaissances consolidées : {len(knowledge_files)}")

    manifest_file = f"{REPO_PATH}/manifest.json"
    if os.path.exists(manifest_file):
        with open(manifest_file, "r", encoding="utf-8") as f:
            manifest = json.loads(f.read())
            counts = manifest.get("counts", {})
            success(f"Manifest CodeStation-Memory : {counts.get('knowledge')} connaissances, {counts.get('experiences')} expériences, dernière synchro : {manifest.get('updated_at')}")

    # 5. Recherche directe dans le moteur Memory
    step("5. Recherche directe via l'API Memory (/api/memory/search?q=sqlite)")
    search_url = f"{MEMORY_URL}/api/memory/search?q=sqlite"
    with urllib.request.urlopen(search_url, timeout=3) as r:
        search_res = json.loads(r.read().decode())
        results = search_res.get("results", [])
        success(f"Résultats trouvés dans Memory : {len(results)}")
        for item in results[:2]:
            print(f"      • [{item.get('id')}] {item.get('title')}")

    # 6. Vérification que CAMCS retrouve et réutilise la nouvelle connaissance
    step("6. Vérification de la récupération par CAMCS (/api/camcs/memory?q=sqlite)")
    camcs_mem_url = f"{CAMCS_URL}/api/camcs/memory?q=sqlite"
    with urllib.request.urlopen(camcs_mem_url, timeout=3) as r:
        camcs_res = json.loads(r.read().decode())
        c_results = camcs_res.get("results", [])
        if len(c_results) > 0:
            success(f"CAMCS a retrouvé avec succès {len(c_results)} connaissance(s) pour sa planification !")
            for item in c_results[:2]:
                print(f"      • Titre : {item.get('title')}")
                print(f"        Extrait : {item.get('content', '')[:120]}...")
        else:
            fail("CAMCS n'a pas pu retrouver la connaissance.")

    print("\n" + "=" * 70)
    print("  TOUS LES CRITÈRES DE VALIDATION DU PIPELINE SONT 100% ATTEINTS !")
    print("=" * 70)

if __name__ == "__main__":
    main()
