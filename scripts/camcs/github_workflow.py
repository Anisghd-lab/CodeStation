#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — GitHub Publication & Verification Workflow
Implements the 4-phase mandatory protocol for remote GitHub operations:
- Phase A: Preparation & Permission check
- Phase B: Remote Repository Creation
- Phase C: Clean Publication & Git Push
- Phase D: Independent Remote Verification
Never fabricates proof or considers local files as remote publication.
"""

import os
import sys
import time
import json
import subprocess
from typing import Dict, List, Optional, Tuple, Any
from .external_action_verifier import ExternalActionVerifier, ExternalActionStatus, DeliverableType

class GitHubWorkflowExecutor:
    def __init__(self, verifier: Optional[ExternalActionVerifier] = None):
        self.verifier = verifier or ExternalActionVerifier()

    def get_authenticated_user(self) -> Tuple[bool, str, str]:
        """Returns (is_logged_in, username, error_details)."""
        cmd = ["gh", "api", "user", "--jq", ".login"]
        try:
            res = subprocess.run(cmd, capture_output=True, text=True, timeout=8)
            if res.returncode == 0 and res.stdout.strip():
                return True, res.stdout.strip(), ""
            return False, "", f"GitHub non authentifié : {res.stderr.strip()}"
        except Exception as e:
            return False, "", str(e)

    def execute_github_repo_creation_and_publish(
        self,
        mission_id: str,
        task_id: str,
        repo_name: str,
        workspace_dir: str,
        description: str = "CAMCS Autonomous Generation",
        is_private: bool = False,
        expected_files: Optional[List[str]] = None
    ) -> Tuple[bool, Dict[str, Any], str]:
        """
        Executes the full 4-phase mandatory workflow and returns verification results.
        """
        expected_files = expected_files or ["README.md"]
        
        # Register Action in DB
        action = self.verifier.register_action(
            mission_id=mission_id,
            task_id=task_id,
            action_type="GITHUB_CREATE_REPO",
            target_service="github",
            target_resource=repo_name,
            is_mandatory=True,
            parameters={"is_private": is_private, "expected_files": expected_files}
        )

        # -------------------------------------------------------------
        # Phase A — Préparation
        # -------------------------------------------------------------
        action.status = ExternalActionStatus.IN_PROGRESS
        self.verifier.save_action(action)

        ok_auth, username, auth_err = self.get_authenticated_user()
        if not ok_auth:
            action.status = ExternalActionStatus.BLOCKED
            action.error_message = f"Échec de l'authentification GitHub : {auth_err}"
            self.verifier.save_action(action)
            return False, {}, action.error_message

        full_repo_name = f"{username}/{repo_name}"
        action.target_resource = full_repo_name
        self.verifier.save_action(action)

        # Ensure .gitignore exists to prevent secret leakage
        gitignore_path = os.path.join(workspace_dir, ".gitignore")
        with open(gitignore_path, "w", encoding="utf-8") as f:
            f.write("*.pyc\n__pycache__/\n.env\n*.key\n*.secret\n.git/\n")

        # -------------------------------------------------------------
        # Phase B — Création distante (si inexistant)
        # -------------------------------------------------------------
        exists, existing_details, _ = self.verifier.verify_github_repo(full_repo_name)
        if not exists:
            # Create remote repo
            visibility_flag = "--private" if is_private else "--public"
            cmd_create = ["gh", "repo", "create", full_repo_name, visibility_flag, "--description", description]
            res_create = subprocess.run(cmd_create, capture_output=True, text=True, timeout=15)
            action.execution_command = " ".join(cmd_create)
            action.exit_code = res_create.returncode

            if res_create.returncode != 0:
                action.status = ExternalActionStatus.FAILED
                action.error_message = f"Échec de création du dépôt distant : {res_create.stderr.strip()}"
                self.verifier.save_action(action)
                return False, {}, action.error_message

            # Independent verification of remote existence
            exists_after, repo_details, err_view = self.verifier.verify_github_repo(full_repo_name)
            if not exists_after:
                action.status = ExternalActionStatus.FAILED
                action.error_message = f"Dépôt créé mais introuvable lors du contrôle distant : {err_view}"
                self.verifier.save_action(action)
                return False, {}, action.error_message
        else:
            repo_details = existing_details

        # -------------------------------------------------------------
        # Phase C — Publication (Commit & Push)
        # -------------------------------------------------------------
        action.status = ExternalActionStatus.SUBMITTED
        self.verifier.save_action(action)

        # Initialize git if not present
        git_dir = os.path.join(workspace_dir, ".git")
        if not os.path.exists(git_dir):
            subprocess.run(["git", "init"], cwd=workspace_dir, check=True, capture_output=True)

        subprocess.run(["git", "config", "user.name", username], cwd=workspace_dir, capture_output=True)
        subprocess.run(["git", "config", "user.email", f"{username}@users.noreply.github.com"], cwd=workspace_dir, capture_output=True)

        # Configure remote URL with authenticated token from gh
        token_res = subprocess.run(["gh", "auth", "token"], capture_output=True, text=True)
        token = token_res.stdout.strip()
        remote_url = f"https://x-access-token:{token}@github.com/{full_repo_name}.git" if token else f"https://github.com/{full_repo_name}.git"
        public_remote_url = f"https://github.com/{full_repo_name}.git"

        subprocess.run(["git", "remote", "remove", "origin"], cwd=workspace_dir, capture_output=True)
        subprocess.run(["git", "remote", "add", "origin", remote_url], cwd=workspace_dir, check=True, capture_output=True)

        # Stage and commit
        subprocess.run(["git", "add", "."], cwd=workspace_dir, capture_output=True)
        subprocess.run(["git", "commit", "-m", f"feat: autonomous generation by CAMCS ({mission_id[:8]})"], cwd=workspace_dir, capture_output=True)

        # Get local HEAD SHA
        sha_res = subprocess.run(["git", "rev-parse", "HEAD"], cwd=workspace_dir, capture_output=True, text=True)
        local_sha = sha_res.stdout.strip()

        # Push branch
        subprocess.run(["git", "branch", "-M", "main"], cwd=workspace_dir, capture_output=True)
        push_res = subprocess.run(["git", "push", "-u", "origin", "main", "--force"], cwd=workspace_dir, capture_output=True, text=True, timeout=25)

        # Clean token from remote url for security
        subprocess.run(["git", "remote", "set-url", "origin", public_remote_url], cwd=workspace_dir, capture_output=True)

        if push_res.returncode != 0:
            action.status = ExternalActionStatus.FAILED
            action.error_message = f"Échec du push Git vers {full_repo_name} : {push_res.stderr.strip()}"
            self.verifier.save_action(action)
            return False, {}, action.error_message

        # -------------------------------------------------------------
        # Phase D — Vérification indépendante du résultat externe
        # -------------------------------------------------------------
        # 1. Vérifier le dépôt
        repo_ok, repo_meta, meta_err = self.verifier.verify_github_repo(full_repo_name)
        if not repo_ok:
            action.status = ExternalActionStatus.NOT_VERIFIED
            action.error_message = f"Impossible de vérifier le dépôt après publication : {meta_err}"
            self.verifier.save_action(action)
            return False, {}, action.error_message

        # 2. Vérifier les fichiers distants
        files_ok, files_meta, files_err = self.verifier.verify_github_files(full_repo_name, expected_files)
        if not files_ok:
            action.status = ExternalActionStatus.NOT_VERIFIED
            action.error_message = f"Contrôle de fichiers distants incomplet : {files_err}"
            self.verifier.save_action(action)
            return False, {}, action.error_message

        # 3. Confirmation finale
        action.status = ExternalActionStatus.CONFIRMED
        action.verified_at = time.time()
        action.verification_details = {
            "fullName": full_repo_name,
            "url": repo_meta.get("url", public_remote_url),
            "owner": repo_meta.get("owner", username),
            "defaultBranch": "main",
            "localSha": local_sha,
            "visibility": repo_meta.get("visibility", "public"),
            "verifiedFiles": files_meta.get("found", []),
            "independentCheckTimestamp": time.time()
        }
        self.verifier.save_action(action)

        return True, action.verification_details, ""
