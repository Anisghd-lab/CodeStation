#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Base CLI Runner Interface
Defines the standard execution lifecycle contract for all 6 CLIs.
"""

import os
import time
import hashlib
from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Any, Callable, Tuple
from dataclasses import dataclass, field

@dataclass
class CliExecutionResult:
    task_id: str
    mission_id: str
    cli_id: str
    exit_code: int
    stdout: str
    stderr: str
    duration_ms: int
    working_dir: str
    command_executed: str
    files_created: List[str] = field(default_factory=list)
    files_modified: List[str] = field(default_factory=list)
    verified: bool = False
    verdict: str = "PENDING_AUDIT"
    metadata: Dict[str, Any] = field(default_factory=dict)

def compute_file_sha256(file_path: str) -> str:
    if not os.path.isfile(file_path):
        return ""
    h = hashlib.sha256()
    try:
        with open(file_path, "rb") as f:
            while chunk := f.read(65536):
                h.update(chunk)
        return h.hexdigest()
    except Exception:
        return ""

class BaseCliRunner(ABC):
    def __init__(self, cli_id: str, name: str, binary: str):
        self.cli_id = cli_id
        self.name = name
        self.binary = binary
        self.path: Optional[str] = None
        self.version: Optional[str] = None
        self.active_processes: Dict[str, Any] = {}

    @abstractmethod
    def initialize(self) -> bool:
        """Initialize and verify binary availability and path."""
        pass

    @abstractmethod
    def health_check(self) -> Tuple[bool, str, str]:
        """
        Check health of the CLI.
        Returns: (is_healthy, status_enum, details)
        Status values: IMPLEMENTED_AND_VERIFIED, IMPLEMENTED_BUT_UNVERIFIED,
                       PARTIALLY_IMPLEMENTED, INTERNAL_ONLY, SIMULATED, BLOCKED, NOT_IMPLEMENTED
        """
        pass

    @abstractmethod
    def execute_task(
        self,
        task_id: str,
        mission_id: str,
        prompt: str,
        cwd: str,
        timeout: int = 120,
        output_callback: Optional[Callable[[str, str], None]] = None,
        pid_callback: Optional[Callable[[int], None]] = None
    ) -> CliExecutionResult:
        """Execute a task non-interactively using real OS process."""
        pass

    @abstractmethod
    def cancel_task(self, task_id: str) -> bool:
        """Cancel a running task execution."""
        pass

    def take_directory_snapshot(self, directory: str) -> Dict[str, Tuple[int, str]]:
        """Snapshot files with mtime and size to detect creations/modifications."""
        snapshot = {}
        if not os.path.isdir(directory):
            return snapshot
        for root, _, files in os.walk(directory):
            for f in files:
                full_path = os.path.join(root, f)
                try:
                    st = os.stat(full_path)
                    snapshot[full_path] = (st.st_mtime_ns, compute_file_sha256(full_path))
                except Exception:
                    pass
        return snapshot

    def collect_artifacts(self, cwd: str, snapshot_before: Dict[str, Tuple[int, str]]) -> Tuple[List[str], List[str]]:
        """Compares current directory state against snapshot_before to find created and modified files."""
        created = []
        modified = []
        snapshot_after = self.take_directory_snapshot(cwd)
        for fpath, (mtime, sha) in snapshot_after.items():
            if fpath not in snapshot_before:
                created.append(fpath)
            elif snapshot_before[fpath][1] != sha:
                modified.append(fpath)
        return created, modified

    def cleanup(self):
        """Cleanup any stale resources or temp files."""
        pass
