"""Vérifie que la traçabilité Git affichée correspond aux vraies commandes Git."""

import os
import subprocess
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from camcs_git_status import collect_git_status  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


def _git(*args) -> str:
    return subprocess.run(
        ["git", *args], cwd=REPO, capture_output=True, text=True
    ).stdout


def _porcelain():
    return [line for line in _git("status", "--porcelain=v1").splitlines() if line.strip()]


def test_branch_and_commit_match_real_git():
    status = collect_git_status(REPO)

    assert status["ok"] is True
    assert status["branch"] == _git("rev-parse", "--abbrev-ref", "HEAD").strip()
    assert status["lastCommit"]["hash"] == _git("log", "-1", "--format=%H").strip()
    assert status["lastCommit"]["shortHash"] == _git("log", "-1", "--format=%h").strip()


def test_every_changed_file_is_classified_exactly_once():
    status = collect_git_status(REPO)
    total = (
        len(status["modified"])
        + len(status["added"])
        + len(status["deleted"])
        + len(status["renamed"])
    )

    assert total == len(_porcelain())


def test_clean_flag_reflects_local_state():
    status = collect_git_status(REPO)

    assert status["clean"] == (len(_porcelain()) == 0)


def test_read_only_and_refuses_paths_outside_project():
    assert collect_git_status("/tmp")["ok"] is False
    head_before = _git("rev-parse", "HEAD").strip()
    collect_git_status(REPO)
    assert _git("rev-parse", "HEAD").strip() == head_before
