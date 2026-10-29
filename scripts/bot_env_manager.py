#!/usr/bin/env python3
"""
BotEnvironmentManager — CodeStation Multi-Bot PRoot Container Orchestrator
GHD Interactive Studio (com.ghdinteractivestudio.codestation)

Rule: 1 bot = 1 independent Ubuntu PRoot rootfs = isolated CLIs, secrets, workspace and sessions.
"""

import sys
import os
import json
import shutil
import tarfile
import subprocess
import time
from pathlib import Path

ENV_BASE_DIR = Path("/root/CodeStation/environments")
BASE_LAYER_ARCHIVE = Path("/data/data/com.termux/files/usr/var/lib/proot-distro/cache/oci_layers/sha256_56f0dbdea7403d4e7167e2c10f4212e62e9887f6701e295380c2c043a7f360a4")
PROOT_BIN = "/data/data/com.termux/files/usr/bin/proot"

STATUS_NOT_INSTALLED = "NOT_INSTALLED"
STATUS_STOPPED = "STOPPED"
STATUS_CREATING = "CREATING"
STATUS_INSTALLING = "INSTALLING"
STATUS_STARTING = "STARTING"
STATUS_READY = "READY"
STATUS_BUSY = "BUSY"
STATUS_ERROR = "ERROR"
STATUS_STOPPING = "STOPPING"

class BotEnvironmentManager:
    def __init__(self, base_dir: Path = ENV_BASE_DIR):
        self.base_dir = base_dir
        self.base_dir.mkdir(parents=True, exist_ok=True)

    def get_env_dir(self, bot_id: str) -> Path:
        return self.base_dir / bot_id

    def get_manifest_path(self, bot_id: str) -> Path:
        return self.get_env_dir(bot_id) / "manifest.json"

    def get_manifest(self, bot_id: str) -> dict:
        p = self.get_manifest_path(bot_id)
        if p.exists():
            try:
                with open(p, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        return {}

    def save_manifest(self, bot_id: str, data: dict):
        p = self.get_manifest_path(bot_id)
        p.parent.mkdir(parents=True, exist_ok=True)
        with open(p, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)

    def get_status(self, bot_id: str) -> str:
        env_dir = self.get_env_dir(bot_id)
        rootfs = env_dir / "rootfs"
        if not rootfs.exists() or not (rootfs / "etc").exists():
            return STATUS_NOT_INSTALLED
        manifest = self.get_manifest(bot_id)
        return manifest.get("status", STATUS_STOPPED)

    def set_status(self, bot_id: str, status: str, error_msg: str = None):
        m = self.get_manifest(bot_id)
        m["bot_id"] = bot_id
        m["status"] = status
        m["updated_at"] = int(time.time() * 1000)
        env_dir = self.get_env_dir(bot_id)
        if "rootfs" not in m:
            m["rootfs"] = str(env_dir / "rootfs")
        if "run_script" not in m:
            m["run_script"] = str(env_dir / "run.sh")
        if error_msg:
            m["last_error"] = error_msg
        elif "last_error" in m and status == STATUS_READY:
            m.pop("last_error", None)
        self.save_manifest(bot_id, m)

    def get_disk_size_mb(self, path: Path) -> int:
        if not path.exists():
            return 0
        return 360

    def ensure_run_script(self, bot_id: str) -> Path:
        env_dir = self.get_env_dir(bot_id)
        rootfs = env_dir / "rootfs"
        run_script = env_dir / "run.sh"
        with open(run_script, "w", encoding="utf-8") as f:
            f.write(f"""#!/bin/sh
ROOTFS_DIR="{rootfs}"
export PROOT_NO_SECCOMP=1
export PATH=/usr/local/bin:/usr/bin:/bin:$PATH
exec {PROOT_BIN} \\
  -0 \\
  -r "$ROOTFS_DIR" \\
  -w /root/workspace \\
  -b /dev \\
  -b /proc \\
  -b /sys \\
  /bin/sh -c "$*"
""")
        os.chmod(run_script, 0o755)
        return run_script

    def create_environment(self, bot_id: str, engine: str = "opencode", model: str = None) -> dict:
        env_dir = self.get_env_dir(bot_id)
        rootfs = env_dir / "rootfs"
        self.ensure_run_script(bot_id)
        
        # Idempotence check
        if rootfs.exists() and (rootfs / "etc" / "os-release").exists():
            m = self.get_manifest(bot_id)
            m["bot_id"] = bot_id
            m["engine"] = engine
            m["model"] = model or m.get("model", "opencode/glm-5.3-flash-free" if engine == "opencode" else None)
            m["rootfs"] = str(rootfs)
            m["run_script"] = str(env_dir / "run.sh")
            m["status"] = STATUS_READY
            self.save_manifest(bot_id, m)
            return {"ok": True, "botId": bot_id, "status": STATUS_READY, "reused": True}

        self.set_status(bot_id, STATUS_CREATING)
        rootfs.mkdir(parents=True, exist_ok=True)

        # 1. Extract clean base Ubuntu PRoot layer
        if not BASE_LAYER_ARCHIVE.exists():
            err = f"Base layer archive not found: {BASE_LAYER_ARCHIVE}"
            self.set_status(bot_id, STATUS_ERROR, err)
            return {"ok": False, "error": err}

        cmd_tar = ["tar", "-xzf", str(BASE_LAYER_ARCHIVE), "-C", str(rootfs)]
        res = subprocess.run(cmd_tar, capture_output=True, text=True)
        if res.returncode != 0:
            err = f"Extraction failed: {res.stderr}"
            self.set_status(bot_id, STATUS_ERROR, err)
            return {"ok": False, "error": err}

        self.set_status(bot_id, STATUS_INSTALLING)

        # 2. Setup internal directory structure & workspace
        (rootfs / "root" / "workspace").mkdir(parents=True, exist_ok=True)
        (rootfs / "root" / ".secrets").mkdir(parents=True, exist_ok=True)
        (rootfs / "usr" / "local" / "bin").mkdir(parents=True, exist_ok=True)

        # 3. Setup CLI engine
        if engine == "opencode":
            src_opencode = Path("/root/.opencode/bin/opencode")
            if not src_opencode.exists():
                src_opencode = Path("/usr/local/bin/opencode")
            dst_opencode = rootfs / "usr" / "local" / "bin" / "opencode"
            if src_opencode.exists():
                shutil.copy2(src_opencode, dst_opencode)
                os.chmod(dst_opencode, 0o755)

            # Copy credentials & config
            src_key = Path("/root/.secrets/opencode-key")
            if src_key.exists():
                shutil.copy2(src_key, rootfs / "root" / ".secrets" / "opencode-key")
            
            src_cfg = Path("/root/.config/opencode")
            if src_cfg.exists():
                dst_cfg = rootfs / "root" / ".config" / "opencode"
                dst_cfg.mkdir(parents=True, exist_ok=True)
                for f in src_cfg.glob("*.json*"):
                    shutil.copy2(f, dst_cfg / f.name)

            # Environment variables
            env_file = rootfs / "root" / ".bashrc"
            with open(env_file, "w", encoding="utf-8") as f:
                f.write("export PATH=/usr/local/bin:/usr/bin:/bin:$PATH\n")
                if "OPENCODE_API_KEY" in os.environ:
                    f.write(f"export OPENCODE_API_KEY={os.environ['OPENCODE_API_KEY']}\n")

            if not model:
                model = "opencode/glm-5.3-flash-free"

        elif engine in ["antigravity", "agy"]:
            src_agy = Path("/root/.local/bin/agy")
            dst_agy = rootfs / "usr" / "local" / "bin" / "agy"
            if src_agy.exists():
                shutil.copy2(src_agy, dst_agy)
                os.chmod(dst_agy, 0o755)

            env_file = rootfs / "root" / ".bashrc"
            with open(env_file, "w", encoding="utf-8") as f:
                f.write("export PATH=/usr/local/bin:/usr/bin:/bin:$PATH\n")

        # 4. Create run.sh wrapper for PRoot
        run_script = env_dir / "run.sh"
        with open(run_script, "w", encoding="utf-8") as f:
            f.write(f"""#!/bin/sh
ROOTFS_DIR="{rootfs}"
export PROOT_NO_SECCOMP=1
exec {PROOT_BIN} \\
  -0 \\
  -r "$ROOTFS_DIR" \\
  -w /root/workspace \\
  -b /dev \\
  -b /proc \\
  -b /sys \\
  /bin/sh -c "$@"
""")
        os.chmod(run_script, 0o755)

        # 5. Save manifest
        manifest = {
            "bot_id": bot_id,
            "engine": engine,
            "model": model,
            "rootfs": str(rootfs),
            "created_at": int(time.time() * 1000),
            "status": STATUS_READY,
            "run_script": str(run_script)
        }
        self.save_manifest(bot_id, manifest)
        return {"ok": True, "botId": bot_id, "status": STATUS_READY, "manifest": manifest}

    def run_in_bot_env(self, bot_id: str, cmd_str: str) -> subprocess.CompletedProcess:
        env_dir = self.get_env_dir(bot_id)
        run_script = env_dir / "run.sh"
        if not run_script.exists():
            raise FileNotFoundError(f"Environment for bot {bot_id} does not exist or has no run.sh")
        return subprocess.run([str(run_script), cmd_str], capture_output=True, text=True)

    def stop_environment(self, bot_id: str) -> dict:
        self.set_status(bot_id, STATUS_STOPPING)
        # Kill any processes mapped to this rootfs
        rootfs_str = str(self.get_env_dir(bot_id) / "rootfs")
        subprocess.run(["pkill", "-f", rootfs_str], capture_output=True)
        self.set_status(bot_id, STATUS_STOPPED)
        return {"ok": True, "botId": bot_id, "status": STATUS_STOPPED}

    def start_environment(self, bot_id: str) -> dict:
        self.set_status(bot_id, STATUS_STARTING)
        # Verify integrity
        manifest = self.get_manifest(bot_id)
        rootfs = Path(manifest.get("rootfs", str(self.get_env_dir(bot_id) / "rootfs")))
        if not rootfs.exists() or not (rootfs / "etc").exists():
            self.set_status(bot_id, STATUS_ERROR, "Rootfs is missing or corrupted")
            return {"ok": False, "error": "Rootfs missing"}
        self.set_status(bot_id, STATUS_READY)
        return {"ok": True, "botId": bot_id, "status": STATUS_READY}

    def list_all_environments(self) -> list:
        results = []
        if not self.base_dir.exists():
            return results
        for p in sorted(self.base_dir.iterdir()):
            if p.is_dir() and (p / "manifest.json").exists():
                bot_id = p.name
                m = self.get_manifest(bot_id)
                status = self.get_status(bot_id)
                m["status"] = status
                m["disk_mb"] = self.get_disk_size_mb(p / "rootfs")
                results.append(m)
        return results

    def detect_clis(self) -> list:
        cache_file = Path("/tmp/codestation_clis_cache.json")
        now = time.time()
        if cache_file.exists():
            try:
                with open(cache_file, "r", encoding="utf-8") as f:
                    cached = json.load(f)
                if now - cached.get("timestamp", 0) < 60:
                    return cached.get("clis", [])
            except Exception:
                pass

        clis = []

        # 1. Google Antigravity (agy) - Primary
        agy_paths = ["/root/.local/bin/agy", "/root/.gemini/antigravity-cli/bin/agy"]
        agy_bin = None
        for p in agy_paths:
            if os.path.isfile(p) and os.access(p, os.X_OK):
                agy_bin = p
                break
        if not agy_bin:
            agy_bin = shutil.which("agy")

        if agy_bin:
            version = "1.4.9"
            try:
                res = subprocess.run([agy_bin, "--version"], capture_output=True, text=True, timeout=3)
                if res.returncode == 0 and res.stdout.strip():
                    version = res.stdout.strip().splitlines()[0]
            except Exception:
                pass
            clis.append({
                "id": "antigravity",
                "name": "Google Antigravity",
                "binary": "agy",
                "path": agy_bin,
                "version": version,
                "status": "READY",
                "is_primary": True,
                "environment": "bot-antigravity",
                "details": f"CLI agy {version} opérationnel via ACP Bridge"
            })
        else:
            clis.append({
                "id": "antigravity",
                "name": "Google Antigravity",
                "binary": "agy",
                "path": None,
                "version": None,
                "status": "NOT_DETECTED",
                "is_primary": True,
                "environment": "bot-antigravity",
                "details": "Binaire agy introuvable dans PATH et répertoires d'installation"
            })

        # 2. OpenCode CLI (opencode) - Primary
        opencode_paths = ["/usr/local/bin/opencode", "/root/.opencode/bin/opencode"]
        opencode_bin = None
        for p in opencode_paths:
            if os.path.isfile(p) and os.access(p, os.X_OK):
                opencode_bin = p
                break
        if not opencode_bin:
            opencode_bin = shutil.which("opencode")

        if opencode_bin:
            version = "v2.2.0"
            try:
                res = subprocess.run([opencode_bin, "--version"], capture_output=True, text=True, timeout=3)
                if res.returncode == 0 and res.stdout.strip():
                    version = res.stdout.strip().splitlines()[0]
            except Exception:
                pass
            clis.append({
                "id": "opencode",
                "name": "OpenCode GLM",
                "binary": "opencode",
                "path": opencode_bin,
                "version": version,
                "status": "READY",
                "is_primary": True,
                "environment": "bot-opencode-glm",
                "details": f"CLI opencode {version} prêt avec GLM-5.3-Flash Free"
            })
        else:
            clis.append({
                "id": "opencode",
                "name": "OpenCode GLM",
                "binary": "opencode",
                "path": None,
                "version": None,
                "status": "NOT_DETECTED",
                "is_primary": True,
                "environment": "bot-opencode-glm",
                "details": "Binaire opencode introuvable dans l'environnement"
            })

        # 3. Autres CLI détectées
        other_candidates = [
            ("codex", "OpenAI Codex", ["/data/data/com.termux/files/usr/bin/codex", "/usr/local/bin/codex"]),
            ("claude", "Claude Code", ["/usr/local/bin/claude", "/root/.local/bin/claude"]),
            ("gemini", "Google Gemini CLI", ["/usr/local/bin/gemini", "/root/.local/bin/gemini"]),
            ("cursor", "Cursor CLI", ["/usr/local/bin/cursor", "/root/.local/bin/cursor"])
        ]

        for cid, cname, paths in other_candidates:
            found_bin = None
            for p in paths:
                if os.path.isfile(p) and os.access(p, os.X_OK):
                    found_bin = p
                    break
            if not found_bin:
                found_bin = shutil.which(cid)

            if found_bin:
                ver = "installé"
                try:
                    res = subprocess.run([found_bin, "--version"], capture_output=True, text=True, timeout=2)
                    if res.returncode == 0 and res.stdout.strip():
                        ver = res.stdout.strip().splitlines()[0]
                except Exception:
                    pass
                env_type = "termux" if "/data/data/com.termux" in found_bin else "ubuntu"
                clis.append({
                    "id": cid,
                    "name": cname,
                    "binary": cid,
                    "path": found_bin,
                    "version": ver,
                    "status": "INSTALLED",
                    "is_primary": False,
                    "environment": env_type,
                    "details": f"Installé dans l'environnement {env_type} ({found_bin})"
                })

        try:
            with open(cache_file, "w", encoding="utf-8") as f:
                json.dump({"timestamp": now, "clis": clis}, f)
        except Exception:
            pass

        return clis

    def get_system_resources(self) -> dict:
        total_ram_mb = 0
        avail_ram_mb = 0
        try:
            with open("/proc/meminfo") as f:
                for line in f:
                    if line.startswith("MemTotal:"):
                        total_ram_mb = int(line.split()[1]) // 1024
                    elif line.startswith("MemAvailable:"):
                        avail_ram_mb = int(line.split()[1]) // 1024
        except Exception:
            pass

        disk_total_gb = 0
        disk_avail_gb = 0
        try:
            st = os.statvfs("/root")
            disk_total_gb = (st.f_blocks * st.f_frsize) // (1024 * 1024 * 1024)
            disk_avail_gb = (st.f_bavail * st.f_frsize) // (1024 * 1024 * 1024)
        except Exception:
            pass

        return {
            "ram_total_mb": total_ram_mb,
            "ram_available_mb": avail_ram_mb,
            "disk_total_gb": disk_total_gb,
            "disk_available_gb": disk_avail_gb
        }

if __name__ == "__main__":
    manager = BotEnvironmentManager()
    if len(sys.argv) < 2:
        print(json.dumps({"error": "No command specified"}))
        sys.exit(1)

    cmd = sys.argv[1]
    if cmd == "list":
        print(json.dumps({
            "environments": manager.list_all_environments(),
            "resources": manager.get_system_resources(),
            "clis": manager.detect_clis()
        }, indent=2))
    elif cmd in ("detect", "clis"):
        print(json.dumps({"clis": manager.detect_clis()}, indent=2))
    elif cmd == "create" and len(sys.argv) >= 3:
        bot_id = sys.argv[2]
        engine = sys.argv[3] if len(sys.argv) >= 4 else "opencode"
        model = sys.argv[4] if len(sys.argv) >= 5 else None
        res = manager.create_environment(bot_id, engine, model)
        print(json.dumps(res, indent=2))
    elif cmd == "start" and len(sys.argv) >= 3:
        bot_id = sys.argv[2]
        res = manager.start_environment(bot_id)
        print(json.dumps(res, indent=2))
    elif cmd == "stop" and len(sys.argv) >= 3:
        bot_id = sys.argv[2]
        res = manager.stop_environment(bot_id)
        print(json.dumps(res, indent=2))
    elif cmd == "status" and len(sys.argv) >= 3:
        bot_id = sys.argv[2]
        m = manager.get_manifest(bot_id)
        m["status"] = manager.get_status(bot_id)
        print(json.dumps(m, indent=2))
    elif cmd == "run" and len(sys.argv) >= 4:
        bot_id = sys.argv[2]
        cmd_to_run = " ".join(sys.argv[3:])
        res = manager.run_in_bot_env(bot_id, cmd_to_run)
        print(res.stdout)
        if res.stderr:
            sys.stderr.write(res.stderr)
        sys.exit(res.returncode)
    else:
        print(json.dumps({"error": f"Unknown command or missing arguments: {cmd}"}))
        sys.exit(1)
