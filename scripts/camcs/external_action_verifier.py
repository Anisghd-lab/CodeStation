#!/usr/bin/env python3
"""
CAMCS External Action Verification Engine
Centralized, independent verification for all external actions:
- GitHub Repository Creation & Publication
- Remote Git Push
- Cloud / API Deployments
- External Service Resources

Enforces strict separation between local task completion and objective completion.
States: PLANNED, IN_PROGRESS, SUBMITTED, CONFIRMED, FAILED, BLOCKED, NOT_VERIFIED.
"""

import os
import sys
import time
import json
import uuid
import sqlite3
import subprocess
import urllib.request
import urllib.error
from enum import Enum
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field, asdict

class ExternalActionStatus(str, Enum):
    PLANNED = "PLANNED"
    AUTHENTICATING = "AUTHENTICATING"
    CONNECTED = "CONNECTED"
    IN_PROGRESS = "IN_PROGRESS"
    SUBMITTED = "SUBMITTED"
    VERIFYING = "VERIFYING"
    CONFIRMED = "CONFIRMED"
    FAILED = "FAILED"
    BLOCKED = "BLOCKED"
    NOT_VERIFIED = "NOT_VERIFIED"

class DeliverableType(str, Enum):
    LOCAL_FILE = "LOCAL_FILE"
    LOCAL_DIRECTORY = "LOCAL_DIRECTORY"
    GIT_COMMIT = "GIT_COMMIT"
    REMOTE_REPOSITORY = "REMOTE_REPOSITORY"
    REMOTE_FILE = "REMOTE_FILE"
    DEPLOYMENT = "DEPLOYMENT"
    API_RESOURCE = "API_RESOURCE"
    TEST_REPORT = "TEST_REPORT"

@dataclass
class ExternalAction:
    id: str
    mission_id: str
    task_id: str
    action_type: str  # GITHUB_CREATE_REPO, GIT_PUSH, DEPLOY_SERVICE, API_RESOURCE
    target_service: str  # github, api, cloudflare
    target_resource: str  # e.g., Anisghd-lab/CAMCS
    status: ExternalActionStatus = ExternalActionStatus.PLANNED
    is_mandatory: bool = True
    parameters: Dict[str, Any] = field(default_factory=dict)
    execution_command: Optional[str] = None
    exit_code: Optional[int] = None
    verification_method: str = "INDEPENDENT_API"
    verified_at: Optional[float] = None
    verification_details: Dict[str, Any] = field(default_factory=dict)
    error_message: Optional[str] = None
    created_at: float = field(default_factory=time.time)
    updated_at: float = field(default_factory=time.time)

    def to_dict(self) -> Dict[str, Any]:
        d = asdict(self)
        d["status"] = self.status.value if isinstance(self.status, ExternalActionStatus) else self.status
        details = self.verification_details or {}
        d["account"] = details.get("account")
        d["scopes"] = details.get("scopes", [])
        d["auth_duration_ms"] = details.get("auth_duration_ms")
        d["last_event"] = details.get("last_event")
        d["verified_url"] = details.get("verified_url") or details.get("url")
        d["owner"] = details.get("owner")
        d["repo_name"] = details.get("repoName")
        d["current_step"] = details.get("current_step")
        d["completed_steps"] = details.get("completed_steps", [])
        return d

class ExternalActionVerifier:
    def __init__(self, db_path: str = "/root/.codestation/camcs.db"):
        self.db_path = db_path
        self._init_db()

    def _init_db(self):
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS external_actions (
                id TEXT PRIMARY KEY,
                mission_id TEXT,
                task_id TEXT,
                action_type TEXT,
                target_service TEXT,
                target_resource TEXT,
                status TEXT,
                is_mandatory INTEGER,
                parameters_json TEXT,
                execution_command TEXT,
                exit_code INTEGER,
                verification_method TEXT,
                verified_at REAL,
                verification_details_json TEXT,
                error_message TEXT,
                created_at REAL,
                updated_at REAL,
                FOREIGN KEY(mission_id) REFERENCES missions(id)
            )
        """)
        conn.commit()
        conn.close()

    def register_action(
        self,
        mission_id: str,
        task_id: str,
        action_type: str,
        target_service: str,
        target_resource: str,
        is_mandatory: bool = True,
        parameters: Optional[Dict[str, Any]] = None,
        verification_method: str = "INDEPENDENT_API"
    ) -> ExternalAction:
        action_id = f"ext-{uuid.uuid4().hex[:8]}"
        action = ExternalAction(
            id=action_id,
            mission_id=mission_id,
            task_id=task_id,
            action_type=action_type,
            target_service=target_service,
            target_resource=target_resource,
            status=ExternalActionStatus.PLANNED,
            is_mandatory=is_mandatory,
            parameters=parameters or {},
            verification_method=verification_method
        )
        self.save_action(action)
        return action

    def save_action(self, action: ExternalAction):
        action.updated_at = time.time()
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        status_val = action.status.value if isinstance(action.status, ExternalActionStatus) else action.status
        cur.execute("""
            INSERT OR REPLACE INTO external_actions
            (id, mission_id, task_id, action_type, target_service, target_resource, status, is_mandatory, parameters_json, execution_command, exit_code, verification_method, verified_at, verification_details_json, error_message, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            action.id, action.mission_id, action.task_id, action.action_type,
            action.target_service, action.target_resource, status_val,
            1 if action.is_mandatory else 0, json.dumps(action.parameters),
            action.execution_command, action.exit_code, action.verification_method,
            action.verified_at, json.dumps(action.verification_details),
            action.error_message, action.created_at, action.updated_at
        ))
        conn.commit()
        conn.close()

    def get_mission_actions(self, mission_id: str) -> List[ExternalAction]:
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("SELECT * FROM external_actions WHERE mission_id = ?", (mission_id,))
        rows = cur.fetchall()
        conn.close()

        actions = []
        for r in rows:
            act = ExternalAction(
                id=r[0],
                mission_id=r[1],
                task_id=r[2],
                action_type=r[3],
                target_service=r[4],
                target_resource=r[5],
                status=ExternalActionStatus(r[6]) if r[6] in ExternalActionStatus._value2member_map_ else ExternalActionStatus.NOT_VERIFIED,
                is_mandatory=bool(r[7]),
                parameters=json.loads(r[8] or "{}"),
                execution_command=r[9],
                exit_code=r[10],
                verification_method=r[11],
                verified_at=r[12],
                verification_details=json.loads(r[13] or "{}"),
                error_message=r[14],
                created_at=r[15],
                updated_at=r[16]
            )
            actions.append(act)
        return actions

    def are_all_mandatory_actions_confirmed(self, mission_id: str, objective: str = "") -> Tuple[bool, List[str]]:
        """
        Validates the fundamental rule:
        A mission with mandatory external actions can NEVER reach OBJECTIVE_COMPLETED
        unless 100% of its mandatory actions are CONFIRMED.
        If objective requested a remote resource (e.g. GitHub repo) but no action exists, it fails.
        """
        actions = self.get_mission_actions(mission_id)
        repo_keywords = ["repo", "dépôt", "depot", "repository", "github", "git"]
        if any(kw in objective.lower() for kw in repo_keywords) and not actions:
            return False, ["L'objectif exigeait un dépôt GitHub mais aucune action externe n'a été enregistrée."]

        if not actions:
            return True, []  # Purely local mission

        unconfirmed = []
        for a in actions:
            if a.is_mandatory and a.status != ExternalActionStatus.CONFIRMED:
                unconfirmed.append(f"Action '{a.action_type}' sur '{a.target_resource}' a le statut {a.status.value}")

        return len(unconfirmed) == 0, unconfirmed

    def verify_github_repo(self, repo_full_name: str) -> Tuple[bool, Dict[str, Any], str]:
        """
        Independent verification of a GitHub repository using GitHub CLI / API.
        Never relies on local git configs.
        Returns: (is_confirmed, details_dict, error_message)
        """
        # Method 1: Using gh CLI
        cmd = ["gh", "repo", "view", repo_full_name, "--json", "name,owner,url,isPrivate,defaultBranchRef,description"]
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=12)
            if res.returncode == 0:
                data = json.loads(res.stdout)
                owner = data.get("owner", {}).get("login", "")
                url = data.get("url", "")
                default_branch = data.get("defaultBranchRef", {}).get("name", "main")
                is_private = data.get("isPrivate", False)

                details = {
                    "exists": True,
                    "repo": repo_full_name,
                    "owner": owner,
                    "url": url,
                    "defaultBranch": default_branch,
                    "visibility": "private" if is_private else "public",
                    "verifiedAt": time.time(),
                    "method": "gh_cli"
                }
                return True, details, ""
            else:
                return False, {}, f"GitHub CLI: Le dépôt n'a pas pu être résolu ({res.stderr.strip()})"
        except Exception as e:
            return False, {}, f"Erreur lors de la vérification GitHub: {e}"

    def verify_github_files(self, repo_full_name: str, expected_files: List[str]) -> Tuple[bool, Dict[str, Any], str]:
        """
        Verifies presence of specific files in the remote GitHub repository.
        """
        missing = []
        found = []
        for fpath in expected_files:
            cmd = ["gh", "api", f"repos/{repo_full_name}/contents/{fpath}"]
            try:
                res = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
                if res.returncode == 0:
                    cdata = json.loads(res.stdout)
                    found.append({"path": fpath, "sha": cdata.get("sha", "")})
                else:
                    missing.append(fpath)
            except Exception:
                missing.append(fpath)

        if missing:
            return False, {"found": found, "missing": missing}, f"Fichiers manquants sur le dépôt distant : {', '.join(missing)}"
        return True, {"found": found, "missing": []}, ""

    def verify_remote_git_sha(self, repo_url: str, branch: str, expected_sha: str) -> Tuple[bool, str]:
        """
        Verifies that remote branch HEAD matches the expected commit SHA via git ls-remote.
        """
        cmd = ["git", "ls-remote", repo_url, f"refs/heads/{branch}"]
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=10)
            if res.returncode == 0 and res.stdout.strip():
                remote_sha = res.stdout.strip().split()[0]
                if remote_sha == expected_sha:
                    return True, f"SHA distant confirmé : {remote_sha}"
                return False, f"Désynchronisation SHA : attendu {expected_sha}, distant {remote_sha}"
            return False, f"Branche '{branch}' introuvable sur {repo_url}"
        except Exception as e:
            return False, f"Erreur ls-remote: {e}"
