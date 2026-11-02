#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Credential Registry & Secret Resolver
Discovers, resolves, validates, and manages credentials from:
- Environment variables (Ubuntu PRoot ARM64)
- Files in ~/.secrets/
- CLI configurations (~/.config/gh/hosts.yml, ~/.config/opencode/)
- System credentials (AGENTS.md)

SECURITY INVARIANT:
Raw secret values are NEVER logged, serialized into SQLite, printed,
injected into client responses, or exposed to LLM prompts.
Only secure masked fingerprints (e.g., 'sk-or-***...c91') are exposed.
"""

import os
import re
import yaml
import json
import hashlib
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, field

@dataclass
class ResolvedSecret:
    service_id: str
    auth_type: str  # bearer_token, oauth2_token, api_key, s3_keyset
    source: str     # env_var, file, cli_config
    masked_value: str
    secret_hash: str
    length: int
    raw_value: str = field(repr=False)  # Excluded from repr and logging

    def get_raw(self) -> str:
        """Secure accessor for authenticated HTTP client calls only."""
        return self.raw_value

    def to_safe_dict(self) -> Dict[str, Any]:
        """Safe dictionary representation for API and UI consumption."""
        return {
            "serviceId": self.service_id,
            "authType": self.auth_type,
            "source": self.source,
            "maskedValue": self.masked_value,
            "secretHash": self.secret_hash[:12],
            "length": self.length,
            "isConfigured": bool(self.raw_value)
        }

def mask_secret(raw: str) -> str:
    """Masks secret to show only a small safe prefix and suffix."""
    if not raw:
        return "<none>"
    if len(raw) <= 8:
        return "*" * len(raw)
    prefix = raw[:4]
    suffix = raw[-4:]
    return f"{prefix}{'*' * min(16, len(raw) - 8)}{suffix}"

def hash_secret(raw: str) -> str:
    if not raw:
        return ""
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()

class SecretResolver:
    @staticmethod
    def resolve_github_token() -> Optional[ResolvedSecret]:
        # 1. Check gh hosts.yml
        hosts_path = os.path.expanduser("~/.config/gh/hosts.yml")
        if os.path.exists(hosts_path):
            try:
                with open(hosts_path, "r", encoding="utf-8") as f:
                    data = yaml.safe_load(f)
                    gh_entry = data.get("github.com", {})
                    token = gh_entry.get("oauth_token", "")
                    if token:
                        return ResolvedSecret(
                            service_id="github",
                            auth_type="oauth2_token",
                            source="~/.config/gh/hosts.yml",
                            masked_value=mask_secret(token),
                            secret_hash=hash_secret(token),
                            length=len(token),
                            raw_value=token
                        )
            except Exception:
                pass

        # 2. Check GITHUB_TOKEN or GH_TOKEN env
        for env_k in ["GITHUB_TOKEN", "GH_TOKEN"]:
            val = os.environ.get(env_k, "").strip()
            if val:
                return ResolvedSecret(
                    service_id="github",
                    auth_type="bearer_token",
                    source=f"env:{env_k}",
                    masked_value=mask_secret(val),
                    secret_hash=hash_secret(val),
                    length=len(val),
                    raw_value=val
                )
        return None

    @staticmethod
    def resolve_openrouter_key() -> Optional[ResolvedSecret]:
        val = os.environ.get("OPENROUTER_API_KEY", "").strip()
        if val:
            return ResolvedSecret(
                service_id="openrouter",
                auth_type="api_key",
                source="env:OPENROUTER_API_KEY",
                masked_value=mask_secret(val),
                secret_hash=hash_secret(val),
                length=len(val),
                raw_value=val
            )
        # Check ~/.secrets/openrouter-key
        fpath = os.path.expanduser("~/.secrets/openrouter-key")
        if os.path.exists(fpath):
            try:
                with open(fpath, "r") as f:
                    key = f.read().strip()
                    if key:
                        return ResolvedSecret(
                            service_id="openrouter",
                            auth_type="api_key",
                            source="~/.secrets/openrouter-key",
                            masked_value=mask_secret(key),
                            secret_hash=hash_secret(key),
                            length=len(key),
                            raw_value=key
                        )
            except Exception:
                pass
        return None

    @staticmethod
    def resolve_xkiro_key() -> Optional[ResolvedSecret]:
        # 1. Check ~/.secrets/xkiro-key
        fpath = os.path.expanduser("~/.secrets/xkiro-key")
        if os.path.exists(fpath):
            try:
                with open(fpath, "r") as f:
                    key = f.read().strip()
                    if key:
                        return ResolvedSecret(
                            service_id="xkiro",
                            auth_type="api_key",
                            source="~/.secrets/xkiro-key",
                            masked_value=mask_secret(key),
                            secret_hash=hash_secret(key),
                            length=len(key),
                            raw_value=key
                        )
            except Exception:
                pass

        val = os.environ.get("XKIRO_API_KEY", "").strip()
        if val:
            return ResolvedSecret(
                service_id="xkiro",
                auth_type="api_key",
                source="env:XKIRO_API_KEY",
                masked_value=mask_secret(val),
                secret_hash=hash_secret(val),
                length=len(val),
                raw_value=val
            )
        return None

    @staticmethod
    def resolve_opencode_key() -> Optional[ResolvedSecret]:
        fpath = os.path.expanduser("~/.secrets/opencode-key")
        if os.path.exists(fpath):
            try:
                with open(fpath, "r") as f:
                    key = f.read().strip()
                    if key:
                        return ResolvedSecret(
                            service_id="opencode-inference",
                            auth_type="api_key",
                            source="~/.secrets/opencode-key",
                            masked_value=mask_secret(key),
                            secret_hash=hash_secret(key),
                            length=len(key),
                            raw_value=key
                        )
            except Exception:
                pass
        val = os.environ.get("OPENCODE_API_KEY", "").strip()
        if val:
            return ResolvedSecret(
                service_id="opencode-inference",
                auth_type="api_key",
                source="env:OPENCODE_API_KEY",
                masked_value=mask_secret(val),
                secret_hash=hash_secret(val),
                length=len(val),
                raw_value=val
            )
        return None

    @staticmethod
    def resolve_cloudflare_r2() -> Optional[ResolvedSecret]:
        # Dynamically resolved from environment or system AGENTS.md
        token = os.environ.get("CLOUDFLARE_R2_TOKEN", "").strip()
        if not token:
            agents_md = "/root/AGENTS.md"
            if os.path.exists(agents_md):
                try:
                    with open(agents_md, "r", encoding="utf-8") as f:
                        content = f.read()
                        m = re.search(r"API Token\s*[*]*\s*:\s*`?([^\s\n\r`]+)`?", content)
                        if m:
                            token = m.group(1).strip("` ")
                except Exception:
                    pass

        if not token:
            return None

        return ResolvedSecret(
            service_id="cloudflare_r2",
            auth_type="s3_keyset",
            source="system:AGENTS.md",
            masked_value=mask_secret(token),
            secret_hash=hash_secret(token),
            length=len(token),
            raw_value=token
        )

    @staticmethod
    def resolve_huggingface() -> Optional[ResolvedSecret]:
        val = os.environ.get("HUGGINGFACE_API_KEY", "").strip()
        if val:
            return ResolvedSecret(
                service_id="huggingface",
                auth_type="bearer_token",
                source="env:HUGGINGFACE_API_KEY",
                masked_value=mask_secret(val),
                secret_hash=hash_secret(val),
                length=len(val),
                raw_value=val
            )
        return None

class CredentialRegistry:
    def __init__(self):
        self._secrets: Dict[str, ResolvedSecret] = {}
        self.reload_all()

    def reload_all(self):
        resolvers = [
            SecretResolver.resolve_github_token,
            SecretResolver.resolve_openrouter_key,
            SecretResolver.resolve_xkiro_key,
            SecretResolver.resolve_opencode_key,
            SecretResolver.resolve_cloudflare_r2,
            SecretResolver.resolve_huggingface
        ]
        for res_fn in resolvers:
            sec = res_fn()
            if sec:
                self._secrets[sec.service_id] = sec

    def get_secret(self, service_id: str) -> Optional[ResolvedSecret]:
        return self._secrets.get(service_id)

    def get_raw_secret(self, service_id: str) -> Optional[str]:
        sec = self.get_secret(service_id)
        return sec.get_raw() if sec else None

    def list_safe_credentials(self) -> List[Dict[str, Any]]:
        return [sec.to_safe_dict() for sec in self._secrets.values()]
