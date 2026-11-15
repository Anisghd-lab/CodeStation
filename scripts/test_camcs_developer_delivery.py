#!/usr/bin/env python3
"""
Test Suite: CAMCS Real Developer GitHub Delivery Engine
Covers all 5 mandatory scenarios:
1. Création et livraison d'un nouveau projet (repo dédié, .gitignore, README, commit, push, vérification distante).
2. Modification d'un projet existant (réutilisation du repo existant, branche dédiée, PR sans création d'un autre repo).
3. Échec d'un push sans déclaration abusive de réussite (BLOCKED / FAILED, jamais de faux succès).
4. Reprise d'une mission interrompue sans duplication (détection anti-doublon).
5. Vérification indépendante de la présence du commit attendu sur GitHub.
"""

import os
import sys
import time
import json
import shutil
import unittest
import tempfile
import subprocess
from unittest.mock import patch, MagicMock

# Add scripts directory
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from camcs.github_workflow import GitHubWorkflowExecutor, ProjectType, ProjectContext
from camcs.external_action_verifier import ExternalActionVerifier, ExternalActionStatus, DeliverableType
from camcs.objective_completion_gate import ObjectiveCompletionGate
from camcs.state_machines import MissionState, TaskState
from camcs_engine import init_db

class TestCamcsDeveloperDelivery(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp(prefix="camcs_dev_test_")
        self.db_path = os.path.join(self.temp_dir, "test_camcs.db")
        init_db(self.db_path)
        self.verifier = ExternalActionVerifier(self.db_path)
        self.executor = GitHubWorkflowExecutor(self.verifier)
        self.completion_gate = ObjectiveCompletionGate(self.db_path)

    def tearDown(self):
        shutil.rmtree(self.temp_dir, ignore_errors=True)

    def test_1_new_project_creation_and_delivery(self):
        """Scenario 1: Création et livraison d'un nouveau projet."""
        workspace = os.path.join(self.temp_dir, "new_project_ws")
        os.makedirs(workspace, exist_ok=True)
        # Create some real project source files
        with open(os.path.join(workspace, "main.py"), "w") as f:
            f.write("print('Hello from CAMCS Autonomous Developer')\n")
        with open(os.path.join(workspace, "test_main.py"), "w") as f:
            f.write("import unittest\nclass T(unittest.TestCase):\n    def test(self): self.assertTrue(True)\n")

        mission_id = "test-mission-new-001"
        task_id = "task-delivery-001"

        # Mock git and gh CLI calls for remote execution
        with patch.object(self.executor, "get_authenticated_user", return_value=(True, "Anisghd-lab", "")), \
             patch.object(self.verifier, "verify_github_repo", return_value=(True, {"full_name": "Anisghd-lab/camcs-new-app", "owner": "Anisghd-lab", "visibility": "private"}, "")), \
             patch("subprocess.run") as mock_subproc:

            def subproc_side_effect(cmd, *args, **kwargs):
                cmd_str = " ".join(cmd) if isinstance(cmd, list) else str(cmd)
                m = MagicMock()
                m.returncode = 0
                if "rev-parse" in cmd_str:
                    m.stdout = "a1b2c3d4e5f6789012345678901234567890abcd\n"
                elif "api" in cmd_str and "commits" in cmd_str:
                    m.stdout = "a1b2c3d4e5f6789012345678901234567890abcd\n"
                elif "api" in cmd_str and "readme" in cmd_str:
                    m.stdout = "README.md\n"
                elif "auth" in cmd_str and "token" in cmd_str:
                    m.stdout = "gho_dummy_token\n"
                else:
                    m.stdout = ""
                m.stderr = ""
                return m

            mock_subproc.side_effect = subproc_side_effect

            ok, details, err = self.executor.execute_developer_delivery(
                mission_id=mission_id,
                task_id=task_id,
                workspace_dir=workspace,
                title="Super Application Autonome",
                objective="Créer un nouveau projet autonome avec architecture propre",
                config={"repo_name": "camcs-new-app", "is_private": True}
            )

            self.assertTrue(ok, f"La livraison du nouveau projet a échoué : {err}")
            self.assertEqual(details.get("projectType"), ProjectType.NEW_PROJECT.value)
            self.assertEqual(details.get("branch"), "main")
            self.assertEqual(details.get("remoteSha"), "a1b2c3d4e5f6789012345678901234567890abcd")
            self.assertTrue(details.get("commitUrl").startswith("https://github.com/Anisghd-lab/camcs-new-app/commit/"))

            # Verify .gitignore exists and contains secret exclusions
            gitignore_file = os.path.join(workspace, ".gitignore")
            self.assertTrue(os.path.isfile(gitignore_file))
            with open(gitignore_file) as f:
                content = f.read()
                self.assertIn(".env", content)
                self.assertIn("*.key", content)

    def test_2_existing_project_reuse_and_branch_pr(self):
        """Scenario 2: Modification d'un projet existant sans créer de nouveau repo."""
        workspace = os.path.join(self.temp_dir, "existing_project_ws")
        os.makedirs(workspace, exist_ok=True)
        # Initialize a fake git repo with origin pointing to CodeStation
        subprocess.run(["git", "init"], cwd=workspace, check=True, capture_output=True)
        subprocess.run(["git", "remote", "add", "origin", "https://github.com/Anisghd-lab/CodeStation.git"], cwd=workspace, check=True, capture_output=True)

        with open(os.path.join(workspace, "Feature.kt"), "w") as f:
            f.write("// Nouvelle fonctionnalité ajoutée\n")

        mission_id = "test-mission-mod-002"
        task_id = "task-delivery-002"

        with patch.object(self.executor, "get_authenticated_user", return_value=(True, "Anisghd-lab", "")), \
             patch.object(self.verifier, "verify_github_repo", return_value=(True, {"full_name": "Anisghd-lab/CodeStation"}, "")), \
             patch("subprocess.run") as mock_subproc:

            def subproc_side_effect(cmd, *args, **kwargs):
                cmd_str = " ".join(cmd) if isinstance(cmd, list) else str(cmd)
                m = MagicMock()
                m.returncode = 0
                if "rev-parse" in cmd_str:
                    m.stdout = "f9e8d7c6b5a43210123456789012345678901234\n"
                elif "api" in cmd_str and "commits" in cmd_str:
                    m.stdout = "f9e8d7c6b5a43210123456789012345678901234\n"
                elif "pr" in cmd_str and "create" in cmd_str:
                    m.stdout = "https://github.com/Anisghd-lab/CodeStation/pull/42\n"
                elif "auth" in cmd_str and "token" in cmd_str:
                    m.stdout = "gho_dummy_token\n"
                elif "remote" in cmd_str and "get-url" in cmd_str:
                    m.stdout = "https://github.com/Anisghd-lab/CodeStation.git\n"
                elif "branch" in cmd_str and "--show-current" in cmd_str:
                    m.stdout = "main\n"
                else:
                    m.stdout = ""
                m.stderr = ""
                return m

            mock_subproc.side_effect = subproc_side_effect

            ok, details, err = self.executor.execute_developer_delivery(
                mission_id=mission_id,
                task_id=task_id,
                workspace_dir=workspace,
                title="Ajout composant Dynamic Island",
                objective="Modifier CodeStation pour ajuster le composant DynamicIslandBar"
            )

            self.assertTrue(ok, f"La modification du projet existant a échoué : {err}")
            self.assertEqual(details.get("projectType"), ProjectType.EXISTING_PROJECT.value)
            self.assertEqual(details.get("repoName"), "CodeStation")
            self.assertIn("camcs/feat-", details.get("branch"))
            self.assertEqual(details.get("prUrl"), "https://github.com/Anisghd-lab/CodeStation/pull/42")
            self.assertEqual(details.get("prNumber"), 42)

    def test_3_push_failure_blocks_mission_without_false_success(self):
        """Scenario 3: L'échec d'un push ne déclare JAMAIS la mission réussie."""
        workspace = os.path.join(self.temp_dir, "push_fail_ws")
        os.makedirs(workspace, exist_ok=True)
        with open(os.path.join(workspace, "code.py"), "w") as f:
            f.write("# code\n")

        mission_id = "test-mission-fail-003"
        task_id = "task-delivery-003"

        with patch.object(self.executor, "get_authenticated_user", return_value=(True, "Anisghd-lab", "")), \
             patch.object(self.verifier, "verify_github_repo", return_value=(True, {"full_name": "Anisghd-lab/test-fail"}, "")), \
             patch("subprocess.run") as mock_subproc:

            def subproc_side_effect(cmd, *args, **kwargs):
                cmd_str = " ".join(cmd) if isinstance(cmd, list) else str(cmd)
                m = MagicMock()
                if "push" in cmd_str:
                    m.returncode = 1
                    m.stderr = "fatal: Authentication failed for remote repo"
                    m.stdout = ""
                elif "rev-parse" in cmd_str:
                    m.returncode = 0
                    m.stdout = "112233445566\n"
                else:
                    m.returncode = 0
                    m.stdout = ""
                    m.stderr = ""
                return m

            mock_subproc.side_effect = subproc_side_effect

            ok, details, err = self.executor.execute_developer_delivery(
                mission_id=mission_id,
                task_id=task_id,
                workspace_dir=workspace,
                title="Mission Fail Test",
                objective="Tester le comportement en cas d'erreur réseau",
                config={"repo_name": "test-fail"}
            )

            # Invariant: Must return False and record BLOCKED status
            self.assertFalse(ok)
            self.assertIn("Échec du push Git", err)

            # Check action state in SQLite
            actions = self.verifier.get_mission_actions(mission_id)
            self.assertEqual(len(actions), 1)
            self.assertEqual(actions[0].status, ExternalActionStatus.BLOCKED)

    def test_4_resume_interrupted_mission_without_duplicate_repo(self):
        """Scenario 4: La reprise d'une mission interrompue ne recrée pas de doublon."""
        workspace = os.path.join(self.temp_dir, "resume_ws")
        os.makedirs(workspace, exist_ok=True)
        with open(os.path.join(workspace, "index.js"), "w") as f:
            f.write("console.log('resume');\n")

        mission_id = "test-mission-resume-004"
        task_id = "task-delivery-004"

        # Mock that repo already exists remotely
        with patch.object(self.executor, "get_authenticated_user", return_value=(True, "Anisghd-lab", "")), \
             patch.object(self.verifier, "verify_github_repo", return_value=(True, {"full_name": "Anisghd-lab/existing-repo", "owner": "Anisghd-lab"}, "")), \
             patch("subprocess.run") as mock_subproc:

            executed_commands = []
            def subproc_side_effect(cmd, *args, **kwargs):
                cmd_str = " ".join(cmd) if isinstance(cmd, list) else str(cmd)
                executed_commands.append(cmd_str)
                m = MagicMock()
                m.returncode = 0
                if "rev-parse" in cmd_str:
                    m.stdout = "998877665544\n"
                elif "api" in cmd_str and "commits" in cmd_str:
                    m.stdout = "998877665544\n"
                elif "api" in cmd_str and "readme" in cmd_str:
                    m.stdout = "README.md\n"
                else:
                    m.stdout = ""
                m.stderr = ""
                return m

            mock_subproc.side_effect = subproc_side_effect

            ok, details, err = self.executor.execute_developer_delivery(
                mission_id=mission_id,
                task_id=task_id,
                workspace_dir=workspace,
                title="Mission Reprise",
                objective="Vérifier la reprise sans recréer le repo",
                config={"repo_name": "existing-repo"}
            )

            self.assertTrue(ok)
            # Ensure "gh repo create" was NOT executed because repo already existed!
            repo_create_calls = [c for c in executed_commands if "gh repo create" in c]
            self.assertEqual(len(repo_create_calls), 0, "gh repo create ne doit pas être appelé si le dépôt existe déjà !")

    def test_5_objective_completion_gate_requires_github_delivery(self):
        """Scenario 5: ObjectiveCompletionGate exige la présence distante certifiée du commit."""
        mission_id = "test-mission-gate-005"
        task_id = "task-gate-005"

        # Register physical deliverable
        deliv_path = os.path.join(self.temp_dir, "app.py")
        with open(deliv_path, "w") as f:
            f.write("# valid code\n")

        import sqlite3
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("INSERT INTO missions (id, title, objective, status) VALUES (?, ?, ?, ?)",
                    (mission_id, "Code Delivery Mission", "Développer le backend et livrer sur GitHub", "RUNNING"))
        cur.execute("INSERT INTO tasks (id, mission_id, title, status, is_mandatory) VALUES (?, ?, ?, ?, ?)",
                    (task_id, mission_id, "Implémentation", "COMPLETED", 1))
        import hashlib
        real_hash = hashlib.sha256(b"# valid code\n").hexdigest()
        cur.execute("INSERT INTO deliverables (id, mission_id, task_id, file_path, sha256, validation_status) VALUES (?, ?, ?, ?, ?, ?)",
                    ("deliv-1", mission_id, task_id, deliv_path, real_hash, "VERIFIED"))
        cur.execute("INSERT INTO execution_proofs (id, mission_id, task_id, exit_code) VALUES (?, ?, ?, ?)",
                    ("proof-1", mission_id, task_id, 0))
        conn.commit()
        conn.close()

        # Gate should REJECT because GitHub delivery action is missing!
        gate_ok, reason, diag = self.completion_gate.evaluate_mission(mission_id)
        self.assertFalse(gate_ok)
        self.assertIn("OBJECTIVE_COMPLETION_GATE_REJECTED", reason)

        # Now register a CONFIRMED external action with verified remote commit
        action = self.verifier.register_action(
            mission_id=mission_id,
            task_id=task_id,
            action_type="GITHUB_CREATE_REPO",
            target_service="github",
            target_resource="Anisghd-lab/camcs-validated-app",
            is_mandatory=True
        )
        action.status = ExternalActionStatus.CONFIRMED
        action.verification_details = {
            "fullName": "Anisghd-lab/camcs-validated-app",
            "branch": "main",
            "remoteSha": "commit123456"
        }
        self.verifier.save_action(action)

        # Mock GitHub API check during completion gate evaluation
        with patch.object(self.completion_gate.external_verifier, "verify_github_repo", return_value=(True, {"full_name": "Anisghd-lab/camcs-validated-app"}, "")), \
             patch("subprocess.run") as mock_subproc:

            def subproc_side_effect(cmd, *args, **kwargs):
                cmd_str = " ".join(cmd) if isinstance(cmd, list) else str(cmd)
                m = MagicMock()
                m.returncode = 0
                if "commits" in cmd_str:
                    m.stdout = "commit123456\n"
                elif "readme" in cmd_str:
                    m.stdout = "README.md\n"
                else:
                    m.stdout = ""
                m.stderr = ""
                return m

            mock_subproc.side_effect = subproc_side_effect

            gate_ok_after, reason_after, _ = self.completion_gate.evaluate_mission(mission_id)
            self.assertTrue(gate_ok_after, f"Le gate a rejeté alors que toutes les preuves sont fournies : {reason_after}")

if __name__ == "__main__":
    unittest.main()
