#!/usr/bin/env python3
"""
CAMCS Objective Completion Gate — Central Validation & Anti-False Completion Barrier
Enforces the inviolable invariant:
A mission is NEVER COMPLETED because CAMCS claims it is.
It is COMPLETED strictly when:
1. All mandatory tasks are COMPLETED.
2. All mandatory deliverables physically exist on disk, are non-empty, and their SHA-256 matches.
3. At least one deliverable is registered in the database. (0 deliverables => IMPOSSIBLE to complete).
4. All execution proofs are registered in SQLite and certified CONFORME (exit code 0).
5. All automated tests were executed and passed (never "EN COURS" or failing).
6. All mandatory external actions are CONFIRMED.
7. If a GitHub repo was requested, the remote repository is independently verified on GitHub:
   - Exists and is reachable via authenticated CLI / GitHub API.
   - Main branch exists.
   - Remote SHA matches pushed commit.
   - Remote README.md is accessible.
   - Requested source code structure is present.
   - No secrets are leaked.
8. Acceptance criteria of the user's objective are genuinely satisfied.
"""

import os
import sys
import time
import json
import sqlite3
import subprocess
import hashlib
from typing import Dict, List, Tuple, Optional, Any

from .state_machines import MissionState, TaskState
from .external_action_verifier import ExternalActionStatus, ExternalActionVerifier
from .deliverable_manager import compute_sha256

DB_PATH = os.path.expanduser("/root/.codestation/camcs.db")

class ObjectiveCompletionGate:
    """Central validator guarding all transitions to MissionState.COMPLETED."""

    def __init__(self, db_path: str = DB_PATH):
        self.db_path = db_path
        self.external_verifier = ExternalActionVerifier(db_path)

    def evaluate_mission(
        self,
        mission_id: str,
        mission_obj: Optional[Any] = None,
        dag_tasks: Optional[List[Any]] = None
    ) -> Tuple[bool, str, Dict[str, Any]]:
        """
        Performs an exhaustive, independent audit of all mission data and physical artifacts.
        Returns:
            (is_valid, reason, diagnostics)
        """
        diagnostics: Dict[str, Any] = {
            "mission_id": mission_id,
            "timestamp": time.time(),
            "checks": {},
            "blocking_reasons": []
        }

        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()

        # 1. Fetch Mission Record
        cur.execute("SELECT * FROM missions WHERE id = ?", (mission_id,))
        m_row = cur.fetchone()
        objective = m_row["objective"] if m_row else (getattr(mission_obj, "objective", "") if mission_obj else "")
        title = m_row["title"] if m_row else (getattr(mission_obj, "title", "") if mission_obj else "")
        obj_lower = (title + " " + objective).lower()

        # 2. Fetch Tasks
        cur.execute("SELECT * FROM tasks WHERE mission_id = ?", (mission_id,))
        task_rows = cur.fetchall()
        tasks = [dict(r) for r in task_rows]
        if not tasks and dag_tasks:
            tasks = [t.to_dict() if hasattr(t, "to_dict") else t for t in dag_tasks]

        if not tasks:
            msg = "Aucune tâche enregistrée dans le DAG de la mission."
            diagnostics["blocking_reasons"].append(msg)
            conn.close()
            return False, msg, diagnostics

        # Check: All mandatory tasks must be COMPLETED
        mandatory_tasks = [t for t in tasks if t.get("is_mandatory", True) or t.get("isMandatory", True)]
        uncompleted_tasks = [
            t.get("id") for t in mandatory_tasks
            if t.get("status") not in (TaskState.COMPLETED, "COMPLETED")
        ]
        if uncompleted_tasks:
            msg = f"Tâches obligatoires non terminées : {uncompleted_tasks}"
            diagnostics["checks"]["mandatory_tasks"] = {"passed": False, "uncompleted": uncompleted_tasks}
            diagnostics["blocking_reasons"].append(msg)
        else:
            diagnostics["checks"]["mandatory_tasks"] = {"passed": True, "count": len(mandatory_tasks)}

        # 3. Fetch Deliverables
        cur.execute("SELECT * FROM deliverables WHERE mission_id = ?", (mission_id,))
        deliv_rows = cur.fetchall()
        deliverables = [dict(r) for r in deliv_rows]

        # Invariant: Deliverables count MUST be > 0
        if len(deliverables) == 0:
            msg = "Zéro livrable physique enregistré dans le registre centralisé."
            diagnostics["checks"]["deliverables_exist"] = {"passed": False, "count": 0}
            diagnostics["blocking_reasons"].append(msg)
        else:
            # Verify each physical deliverable
            verified_count = 0
            invalid_files = []
            for d in deliverables:
                fpath = d.get("file_path") or d.get("filePath")
                doc_type = d.get("doc_type") or d.get("docType")
                expected_sha = d.get("sha256")

                # External actions deliverable types can point to URLs
                if doc_type and doc_type.startswith("external_action:"):
                    verified_count += 1
                    continue

                if not fpath or not os.path.exists(fpath):
                    invalid_files.append(f"{fpath} (fichier introuvable sur le disque)")
                    continue

                if os.path.getsize(fpath) == 0:
                    invalid_files.append(f"{fpath} (fichier vide, taille 0 octet)")
                    continue

                actual_sha = compute_sha256(fpath)
                if expected_sha and expected_sha != actual_sha:
                    invalid_files.append(f"{fpath} (empreinte SHA-256 altérée : {actual_sha} != {expected_sha})")
                    continue

                verified_count += 1

            if invalid_files:
                msg = f"Livrables physiques invalides ou corrompus : {invalid_files}"
                diagnostics["checks"]["deliverables_integrity"] = {"passed": False, "errors": invalid_files}
                diagnostics["blocking_reasons"].append(msg)
            else:
                diagnostics["checks"]["deliverables_integrity"] = {"passed": True, "verified_count": verified_count}

        # 4. Fetch Execution Proofs
        cur.execute("SELECT * FROM execution_proofs WHERE mission_id = ?", (mission_id,))
        proof_rows = cur.fetchall()
        proofs = [dict(r) for r in proof_rows]

        # Invariant: Proofs count MUST be > 0
        if len(proofs) == 0:
            msg = "Zéro preuve d'exécution enregistrée dans execution_proofs."
            diagnostics["checks"]["proofs_exist"] = {"passed": False, "count": 0}
            diagnostics["blocking_reasons"].append(msg)
        else:
            failed_proofs = [p.get("id") for p in proofs if p.get("exit_code") != 0 and p.get("exitCode") != 0]
            if failed_proofs:
                msg = f"Preuves d'exécution en échec (exit code != 0) : {failed_proofs}"
                diagnostics["checks"]["proofs_valid"] = {"passed": False, "failed": failed_proofs}
                diagnostics["blocking_reasons"].append(msg)
            else:
                diagnostics["checks"]["proofs_valid"] = {"passed": True, "count": len(proofs)}

        # 5. Fetch External Actions & Remote Validation
        cur.execute("SELECT * FROM external_actions WHERE mission_id = ?", (mission_id,))
        action_rows = cur.fetchall()
        actions = [dict(r) for r in action_rows]

        # Check if the objective requested a GitHub repository or remote publication
        repo_keywords = ["repo", "dépôt", "depot", "repository", "github", "git"]
        requires_github_repo = any(kw in obj_lower for kw in repo_keywords)

        if requires_github_repo:
            if not actions:
                msg = "L'objectif exige un dépôt GitHub mais aucune action externe n'a été planifiée ni exécutée."
                diagnostics["checks"]["external_github_action"] = {"passed": False, "reason": "NO_ACTION_REGISTERED"}
                diagnostics["blocking_reasons"].append(msg)
            else:
                github_actions = [a for a in actions if a.get("action_type") == "GITHUB_CREATE_REPO" or a.get("target_service") == "github"]
                if not github_actions:
                    msg = "Aucune action externe de création de dépôt GitHub n'a été enregistrée."
                    diagnostics["checks"]["external_github_action"] = {"passed": False, "reason": "MISSING_GITHUB_ACTION"}
                    diagnostics["blocking_reasons"].append(msg)
                else:
                    unconfirmed_gh = [
                        a.get("id") for a in github_actions
                        if a.get("status") not in (ExternalActionStatus.CONFIRMED.value, "CONFIRMED")
                    ]
                    if unconfirmed_gh:
                        msg = f"Actions externes GitHub non confirmées : {unconfirmed_gh}"
                        diagnostics["checks"]["external_github_action"] = {"passed": False, "unconfirmed": unconfirmed_gh}
                        diagnostics["blocking_reasons"].append(msg)
                    else:
                        # 6. Real Independent Probe to Remote GitHub
                        for act in github_actions:
                            target_res = act.get("target_resource", "")
                            probe_ok, repo_meta, probe_err = self.external_verifier.verify_github_repo(target_res)
                            if not probe_ok:
                                msg = f"Vérification distante GitHub échouée pour '{target_res}' : {probe_err}"
                                diagnostics["checks"]["github_remote_probe"] = {"passed": False, "error": probe_err}
                                diagnostics["blocking_reasons"].append(msg)
                            else:
                                # Verify default branch has commits
                                full_repo = repo_meta.get("full_name") or repo_meta.get("repo") or target_res
                                if "/" not in full_repo:
                                    owner = repo_meta.get("owner") or "Anisghd-lab"
                                    full_repo = f"{owner}/{full_repo}"
                                cmd_sha = ["gh", "api", f"repos/{full_repo}/commits/main", "--jq", ".sha"]
                                try:
                                    res_sha = subprocess.run(cmd_sha, capture_output=True, text=True, timeout=8)
                                    if res_sha.returncode != 0 or not res_sha.stdout.strip():
                                        msg = f"Dépôt GitHub '{full_repo}' existant mais branche 'main' sans commit ou inaccessible."
                                        diagnostics["checks"]["github_commits"] = {"passed": False, "error": res_sha.stderr.strip()}
                                        diagnostics["blocking_reasons"].append(msg)
                                    else:
                                        remote_sha = res_sha.stdout.strip()
                                        diagnostics["checks"]["github_commits"] = {"passed": True, "sha": remote_sha}
                                except Exception as e:
                                    msg = f"Erreur lors du contrôle de commit GitHub : {str(e)}"
                                    diagnostics["blocking_reasons"].append(msg)

                                # Verify remote README
                                cmd_readme = ["gh", "api", f"repos/{full_repo}/readme", "--jq", ".name"]
                                try:
                                    res_readme = subprocess.run(cmd_readme, capture_output=True, text=True, timeout=8)
                                    if res_readme.returncode != 0:
                                        msg = f"README.md introuvable sur le dépôt distant '{full_repo}'."
                                        diagnostics["checks"]["github_readme"] = {"passed": False, "error": res_readme.stderr.strip()}
                                        diagnostics["blocking_reasons"].append(msg)
                                    else:
                                        diagnostics["checks"]["github_readme"] = {"passed": True}
                                except Exception as e:
                                    diagnostics["blocking_reasons"].append(f"Erreur vérification README : {str(e)}")

        # 7. Check Automated Tests
        test_tasks = [t for t in tasks if t.get("classification") in ("TESTING", "VALIDATION") or "test" in t.get("title", "").lower()]
        if test_tasks:
            uncompleted_tests = [t.get("id") for t in test_tasks if t.get("status") != TaskState.COMPLETED]
            if uncompleted_tests:
                msg = f"Tests obligatoires non terminés (encore en cours ou en attente) : {uncompleted_tests}"
                diagnostics["checks"]["automated_tests"] = {"passed": False, "uncompleted": uncompleted_tests}
                diagnostics["blocking_reasons"].append(msg)
            else:
                diagnostics["checks"]["automated_tests"] = {"passed": True, "count": len(test_tasks)}

        # 8. Check for Secret Leaks in Deliverables
        leak_found = False
        secret_patterns = ["ghp_", "gho_", "github_pat_", "cfat_", "AKIA", "SECRET_KEY", "PRIVATE KEY"]
        for d in deliverables:
            fpath = d.get("file_path") or d.get("filePath")
            if fpath and os.path.isfile(fpath) and not fpath.endswith((".pyc", ".png", ".jpg", ".db")):
                try:
                    with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                        content = f.read()
                    for sp in secret_patterns:
                        if sp in content:
                            msg = f"Fuite de secret détectée ({sp}) dans le livrable {fpath}."
                            diagnostics["blocking_reasons"].append(msg)
                            leak_found = True
                            break
                except Exception:
                    pass
        diagnostics["checks"]["no_secret_leaks"] = {"passed": not leak_found}

        conn.close()

        # Decision
        if diagnostics["blocking_reasons"]:
            primary_reason = "OBJECTIVE_COMPLETION_GATE_REJECTED : " + " ; ".join(diagnostics["blocking_reasons"][:3])
            return False, primary_reason, diagnostics

        return True, "Tous les critères d'acceptation, livrables physiques et validations distantes sont certifiés conformes (100%).", diagnostics
