"""Vérifie que la traçabilité Git affichée correspond aux vraies commandes Git."""

import os
import subprocess
import sys
import unittest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from camcs_git_status import collect_git_status  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


def _git(*args) -> str:
    return subprocess.run(
        ["git", *args], cwd=REPO, capture_output=True, text=True
    ).stdout


def _porcelain():
    return [line for line in _git("status", "--porcelain=v1").splitlines() if line.strip()]


class TestGitStatus(unittest.TestCase):

    def test_branch_and_commit_match_real_git(self):
        status = collect_git_status(REPO)

        self.assertTrue(status["ok"])
        self.assertEqual(status["branch"], _git("rev-parse", "--abbrev-ref", "HEAD").strip())
        self.assertEqual(status["lastCommit"]["hash"], _git("log", "-1", "--format=%H").strip())
        self.assertEqual(status["lastCommit"]["shortHash"], _git("log", "-1", "--format=%h").strip())

    def test_every_changed_file_is_classified_exactly_once(self):
        status = collect_git_status(REPO)
        total = (
            len(status["modified"])
            + len(status["added"])
            + len(status["deleted"])
            + len(status["renamed"])
        )

        self.assertEqual(total, len(_porcelain()))

    def test_clean_flag_reflects_local_state(self):
        status = collect_git_status(REPO)

        self.assertEqual(status["clean"], (len(_porcelain()) == 0))

    def test_read_only_and_refuses_paths_outside_project(self):
        self.assertFalse(collect_git_status("/tmp")["ok"])
        head_before = _git("rev-parse", "HEAD").strip()
        collect_git_status(REPO)
        self.assertEqual(_git("rev-parse", "HEAD").strip(), head_before)


if __name__ == "__main__":
    unittest.main()
