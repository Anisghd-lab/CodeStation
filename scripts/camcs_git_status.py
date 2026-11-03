"""Collecte en lecture seule de l'état Git réel du dépôt CodeStation.

Aucune commande d'écriture n'est exécutée ici : pas de commit, pas de push,
pas de fetch, pas de reset. Les données proviennent uniquement de `git status`,
`git rev-parse`, `git rev-list` et `git log`.
"""

import os
import subprocess
import time
from typing import Any, Dict

DEFAULT_REPO = "/root/CodeStation"
ALLOWED_PREFIXES = (
    "/root/CodeStation",
    "/root/CodeStation-Memory",
    "/root/.codestation",
)


def _git(args, cwd: str):
    try:
        res = subprocess.run(
            ["git", *args], cwd=cwd, capture_output=True, text=True, timeout=5
        )
        return res.returncode, res.stdout.strip(), res.stderr.strip()
    except Exception as exc:  # pragma: no cover - dépend de l'environnement
        return 1, "", str(exc)


def collect_git_status(repo_path: str = DEFAULT_REPO) -> Dict[str, Any]:
    repo = os.path.abspath(repo_path or DEFAULT_REPO)
    if not any(repo == prefix or repo.startswith(prefix + os.sep) for prefix in ALLOWED_PREFIXES):
        return {"ok": False, "path": repo, "error": "Chemin non autorisé"}
    if not os.path.isdir(repo):
        return {"ok": False, "path": repo, "error": "Dépôt introuvable"}

    code, branch, err = _git(["rev-parse", "--abbrev-ref", "HEAD"], repo)
    if code != 0:
        return {"ok": False, "path": repo, "error": err or "Pas un dépôt Git"}

    rc_upstream, upstream, _ = _git(
        ["rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}"], repo
    )
    upstream = upstream if rc_upstream == 0 else ""
    ahead = behind = 0
    if upstream:
        rc_count, counts, _ = _git(
            ["rev-list", "--left-right", "--count", "HEAD...@{u}"], repo
        )
        parts = counts.split() if rc_count == 0 else []
        if len(parts) == 2:
            ahead, behind = int(parts[0]), int(parts[1])

    rc_status, porcelain, _ = _git(["status", "--porcelain=v1"], repo)
    modified, added, deleted, renamed = [], [], [], []
    if rc_status == 0 and porcelain:
        for line in porcelain.splitlines():
            if len(line) < 4:
                continue
            xy, path = line[:2], line[3:].strip()
            if " -> " in path:
                path = path.split(" -> ", 1)[1]
            if xy == "??" or xy[0] == "A":
                added.append(path)
            elif "D" in xy:
                deleted.append(path)
            elif "R" in xy:
                renamed.append(path)
            else:
                modified.append(path)

    last_commit = None
    rc_log, log_out, _ = _git(["log", "-1", "--format=%H%x1f%h%x1f%s%x1f%cI"], repo)
    if rc_log == 0 and log_out:
        parts = log_out.split("\x1f")
        if len(parts) >= 4:
            last_commit = {
                "hash": parts[0],
                "shortHash": parts[1],
                "subject": parts[2],
                "date": parts[3],
            }

    last_sync = 0.0
    for marker in ("FETCH_HEAD", "ORIG_HEAD"):
        marker_path = os.path.join(repo, ".git", marker)
        if os.path.isfile(marker_path):
            last_sync = max(last_sync, os.path.getmtime(marker_path))

    return {
        "ok": True,
        "path": repo,
        "branch": branch,
        "upstream": upstream,
        "ahead": ahead,
        "behind": behind,
        "modified": modified,
        "added": added,
        "deleted": deleted,
        "renamed": renamed,
        "lastCommit": last_commit,
        "clean": not (modified or added or deleted or renamed),
        "remoteAccessible": bool(upstream),
        "lastSyncAt": last_sync,
        "generatedAt": time.time(),
    }
