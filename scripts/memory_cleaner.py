#!/usr/bin/env python3
"""
CodeStation Memory Sanitation & Noise Purge Engine
Audits /root/CodeStation-Memory/ and removes:
- Conversational noise & chit-chat (hellos, pleasantries, greetings)
- Generic assistant prompt echoes without technical failure signatures
- Non-reproducible suggestions lacking verified solutions

Preserves:
- Architectural decisions (ADRs)
- Core architectural procedures & integration solutions
- Genuine bug-fixes with verified signatures and remediation actions
"""

import os
import sys
import glob
import json
import subprocess

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
try:
    from scripts.memory_verification_gate import MemoryVerificationGate
    from scripts.memory_continuous_watcher import ContinuousMemoryEngine, MEMORY_REPO_PATH
except ImportError:
    from memory_verification_gate import MemoryVerificationGate
    from memory_continuous_watcher import ContinuousMemoryEngine, MEMORY_REPO_PATH

def purge_polluted_memories():
    print("=" * 70)
    print("  CODESTATION MEMORY SANITATION & NOISE PURGE")
    print("=" * 70)

    knw_dir = os.path.join(MEMORY_REPO_PATH, "knowledge")
    exp_dir = os.path.join(MEMORY_REPO_PATH, "experiences")

    knw_files = glob.glob(f"{knw_dir}/*.json")
    print(f"Total knowledge files avant purge : {len(knw_files)}")

    purged_count = 0
    preserved_count = 0

    for fpath in knw_files:
        fname = os.path.basename(fpath)
        with open(fpath, "r", encoding="utf-8") as f:
            knw = json.load(f)

        title = knw.get("title", "")
        content = str(knw.get("content", ""))
        ktype = knw.get("type", "")
        tags = knw.get("tags", [])

        # Core architectural documents & decisions are protected
        is_exempt = (
            ktype in ("decision", "architecture", "procedure")
            or "decision" in tags
            or "mission-synthesis" in tags
            or fname.startswith("knw-cs-00")
        )

        is_noise, reason = MemoryVerificationGate.is_chat_noise(content, title)

        should_purge = False
        purge_reason = ""

        if is_noise:
            should_purge = True
            purge_reason = f"Bavardage/Chat détecté : {reason}"
        elif not is_exempt:
            acc, score, audit = MemoryVerificationGate.evaluate_quality(
                title=title,
                problem=title,
                solution=content,
                context=knw.get("source", ""),
                tags=tags
            )
            if not acc:
                should_purge = True
                purge_reason = f"Score qualité insuffisant ({score} < 0.85) : {audit.get('reasons')}"

        if should_purge:
            # Corresponding experience file
            exp_id = fname.replace("knw-", "exp-")
            exp_file = os.path.join(exp_dir, exp_id)

            os.remove(fpath)
            if os.path.exists(exp_file):
                os.remove(exp_file)

            purged_count += 1
            print(f"  [PURGÉ] {fname} : {title[:60]}")
            print(f"          Motif : {purge_reason}")
        else:
            preserved_count += 1
            print(f"  [CONSERVÉ] {fname} : {title[:60]}")

    print("\n" + "-" * 70)
    print(f"Bilan de purge : {purged_count} fichiers éliminés, {preserved_count} connaissances d'élite conservées.")

    # Rebuild indexes and manifest
    engine = ContinuousMemoryEngine()
    engine.curator.rebuild_all()
    print("Index et manifeste reconstruits avec succès.")

    # Git commit
    subprocess.run(["git", "add", "."], cwd=MEMORY_REPO_PATH, check=False)
    subprocess.run(
        ["git", "commit", "-m", f"Memory Sanitation: Purged {purged_count} chat/noise entries via MemoryVerificationGate"],
        cwd=MEMORY_REPO_PATH,
        check=False
    )
    print("Nettoyage validé et consigné dans CodeStation-Memory.")

if __name__ == "__main__":
    purge_polluted_memories()
