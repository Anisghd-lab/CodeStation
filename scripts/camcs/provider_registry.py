#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Provider Registry, Health Monitor & Free Fallback Manager
Strictly enforces: ONLY 100% FREE MODELS AND LOCAL CLIS ARE AUTHORIZED.
Zero billing surprise, zero paid model invocation.
"""

import os
import time
import json
import urllib.request
import urllib.error
from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Any, Tuple
from dataclasses import dataclass, field

@dataclass
class ModelCapability:
    model_id: str
    name: str
    provider_id: str
    is_free: bool
    context_length: int = 32768
    supports_tool_calling: bool = False
    supports_streaming: bool = True
    openai_compatible: bool = True
    task_suitability: List[str] = field(default_factory=list)  # coding, research, documentation, reasoning

class BaseProviderAdapter(ABC):
    def __init__(self, provider_id: str, name: str, base_url: str):
        self.provider_id = provider_id
        self.name = name
        self.base_url = base_url
        self.total_requests = 0
        self.failed_requests = 0
        self.latencies: List[float] = []
        self.last_checked: float = 0.0
        self.is_healthy: bool = False
        self.last_error: Optional[str] = None

    @abstractmethod
    def is_configured(self) -> bool:
        pass

    @abstractmethod
    def get_available_free_models(self) -> List[ModelCapability]:
        pass

    @abstractmethod
    def chat_completion(
        self,
        model: str,
        messages: List[Dict[str, str]],
        max_tokens: int = 1000,
        temperature: float = 0.2
    ) -> Tuple[bool, str, float]:
        pass


class OpenRouterAdapter(BaseProviderAdapter):
    """OpenRouter Provider — Filtered strictly to 100% Free models."""
    def __init__(self):
        super().__init__("openrouter", "OpenRouter", "https://openrouter.ai/api/v1")
        self.api_key = os.environ.get("OPENROUTER_API_KEY", "").strip()
        self._cached_models: List[ModelCapability] = []
        self._cache_time = 0.0

    def is_configured(self) -> bool:
        return bool(self.api_key)

    def get_available_free_models(self) -> List[ModelCapability]:
        now = time.time()
        if self._cached_models and (now - self._cache_time < 300):
            return self._cached_models

        # Curated verified free models on OpenRouter
        curated_free = [
            ModelCapability("liquid/lfm-2.5-2.6b:free", "LFM 2.5 2.6B (Free)", "openrouter", True, 32768, False, True, True, ["lightweight", "research"]),
            ModelCapability("dots-studio/dots-3-note-preview:free", "Dots 3 Note Preview (Free)", "openrouter", True, 65536, False, True, True, ["documentation", "research"]),
            ModelCapability("nvidia/nemotron-3.5-lightning:free", "Nemotron 3.5 Lightning (Free)", "openrouter", True, 65536, True, True, True, ["coding", "reasoning"]),
            ModelCapability("nvidia/nemotron-3.5-super-vl:free", "Nemotron 3.5 Super VL (Free)", "openrouter", True, 131072, False, True, True, ["research", "documentation"]),
            ModelCapability("thinkingmachines/inkling:free", "Inkling (Free)", "openrouter", True, 32768, False, True, True, ["lightweight", "documentation"]),
            ModelCapability("recursive/flint-v1.0:free", "Flint v1.0 (Free)", "openrouter", True, 32768, False, True, True, ["coding", "lightweight"]),
            ModelCapability("xingchen-agi/xing4.0-29b-a4b:free", "Xing 4.0 29B (Free)", "openrouter", True, 32768, False, True, True, ["reasoning", "coding"])
        ]

        if not self.is_configured():
            self._cached_models = curated_free
            return curated_free

        try:
            req = urllib.request.Request(
                f"{self.base_url}/models",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "HTTP-Referer": "https://codestation.app"
                }
            )
            with urllib.request.urlopen(req, timeout=6) as resp:
                data = json.loads(resp.read().decode())
                fetched: List[ModelCapability] = []
                for m in data.get("data", []):
                    mid = m.get("id", "")
                    # STRICT RULE: Must be free
                    pricing = m.get("pricing", {})
                    is_zero_cost = (
                        str(pricing.get("prompt", "0")) == "0" and
                        str(pricing.get("completion", "0")) == "0"
                    )
                    if ":free" in mid or is_zero_cost:
                        suitability = ["research", "documentation"]
                        if any(w in mid.lower() for w in ["coder", "code", "lightning", "flint"]):
                            suitability.append("coding")
                        fetched.append(ModelCapability(
                            model_id=mid,
                            name=m.get("name", mid),
                            provider_id="openrouter",
                            is_free=True,
                            context_length=m.get("context_length", 32768),
                            supports_tool_calling=False,
                            supports_streaming=True,
                            openai_compatible=True,
                            task_suitability=suitability
                        ))
                if fetched:
                    self._cached_models = fetched
                    self._cache_time = now
                    return fetched
        except Exception as e:
            self.last_error = str(e)

        self._cached_models = curated_free
        return curated_free

    def chat_completion(
        self,
        model: str,
        messages: List[Dict[str, str]],
        max_tokens: int = 1000,
        temperature: float = 0.2
    ) -> Tuple[bool, str, float]:
        # Enforce free model invariant
        if ":free" not in model and not any(m.model_id == model and m.is_free for m in self.get_available_free_models()):
            return False, f"ERREUR_SECURITE: Le modèle '{model}' n'est pas certifié 100% gratuit.", 0.0

        t0 = time.time()
        self.total_requests += 1
        req_data = json.dumps({
            "model": model,
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature
        }).encode("utf-8")

        req = urllib.request.Request(
            f"{self.base_url}/chat/completions",
            data=req_data,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
                "HTTP-Referer": "https://codestation.app",
                "X-Title": "CodeStation CAMCS"
            }
        )
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                elapsed = (time.time() - t0) * 1000
                self.latencies.append(elapsed)
                res = json.loads(resp.read().decode("utf-8"))
                choice = res.get("choices", [{}])[0].get("message", {}).get("content", "")
                self.is_healthy = True
                return True, choice.strip() if choice else "Réponse vide de l'API", elapsed
        except Exception as e:
            self.failed_requests += 1
            self.is_healthy = False
            self.last_error = str(e)
            elapsed = (time.time() - t0) * 1000
            return False, f"Erreur OpenRouter ({type(e).__name__}): {e}", elapsed


class XKiroAdapter(BaseProviderAdapter):
    """xKIRO / Okiro Provider — Filtered strictly to 100% Free models."""
    def __init__(self):
        super().__init__("xkiro", "xKIRO", "https://api.xkiro.com/v1")
        self.api_key = self._load_key()
        self._cached_models: List[ModelCapability] = []
        self._cache_time = 0.0

    def _load_key(self) -> str:
        key_path = os.path.expanduser("~/.secrets/xkiro-key")
        if os.path.exists(key_path):
            try:
                with open(key_path) as fp:
                    return fp.read().strip()
            except Exception:
                pass
        return os.environ.get("XKIRO_API_KEY", "").strip()

    def is_configured(self) -> bool:
        return bool(self.api_key)

    def get_available_free_models(self) -> List[ModelCapability]:
        now = time.time()
        if self._cached_models and (now - self._cache_time < 300):
            return self._cached_models

        # Free models configured in opencode.json for xkiro
        free_models = [
            ModelCapability("qwen/qwen3-coder-plus:free", "Qwen3 Coder Plus (Free)", "xkiro", True, 1048576, True, True, True, ["coding", "architecture", "debugging"]),
            ModelCapability("qwen/qwen3.6-27b:free", "Qwen3.6 27B (Free)", "xkiro", True, 262144, True, True, True, ["reasoning", "coding"]),
            ModelCapability("qwen/qwen3.5-plus:free", "Qwen3.5 Plus (Free)", "xkiro", True, 1000000, True, True, True, ["documentation", "research"]),
            ModelCapability("qwen/qwen3.7-max:free", "Qwen3.7 Max (Free)", "xkiro", True, 262144, True, True, True, ["architecture", "reasoning"]),
            ModelCapability("qwen/qwen3.8-max:free", "Qwen3.8 Max (Free)", "xkiro", True, 262144, True, True, True, ["reasoning", "validation"]),
            ModelCapability("meituan/longcat-2.5-preview:free", "Longcat 2.5 Preview (Free)", "xkiro", True, 262144, False, True, True, ["research", "documentation"]),
            ModelCapability("qwen/qwen3-vl-plus:free", "Qwen3 VL Plus (Free)", "xkiro", True, 131072, False, True, True, ["multimodal", "research"]),
            ModelCapability("qwen/qwen3-omni-flash:free", "Qwen3 Omni Flash (Free)", "xkiro", True, 131072, False, True, True, ["lightweight", "research"])
        ]
        self._cached_models = free_models
        self._cache_time = now
        return free_models

    def chat_completion(
        self,
        model: str,
        messages: List[Dict[str, str]],
        max_tokens: int = 1000,
        temperature: float = 0.2
    ) -> Tuple[bool, str, float]:
        # Enforce free model invariant
        if ":free" not in model:
            return False, f"ERREUR_SECURITE: Le modèle xKIRO '{model}' n'est pas certifié gratuit.", 0.0

        t0 = time.time()
        self.total_requests += 1
        req_data = json.dumps({
            "model": model,
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature
        }).encode("utf-8")

        req = urllib.request.Request(
            f"{self.base_url}/chat/completions",
            data=req_data,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
                "User-Agent": "opencode/1.0.0"
            }
        )
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                elapsed = (time.time() - t0) * 1000
                self.latencies.append(elapsed)
                res = json.loads(resp.read().decode("utf-8"))
                choice = res.get("choices", [{}])[0].get("message", {}).get("content", "")
                self.is_healthy = True
                return True, choice.strip() if choice else "Réponse vide de l'API", elapsed
        except Exception as e:
            self.failed_requests += 1
            self.is_healthy = False
            self.last_error = str(e)
            elapsed = (time.time() - t0) * 1000
            return False, f"Erreur xKIRO ({type(e).__name__}): {e}", elapsed


class OpenCodeInferenceAdapter(BaseProviderAdapter):
    """OpenCode Inference Free Adapter — Connects to free inference tier."""
    def __init__(self):
        super().__init__("opencode-inference", "OpenCode Inference", "https://opencode.ai/inference")
        self.api_key = self._load_key()
        self._cached_models: List[ModelCapability] = []

    def _load_key(self) -> str:
        key_path = os.path.expanduser("~/.secrets/opencode-key")
        if os.path.exists(key_path):
            try:
                with open(key_path) as fp:
                    return fp.read().strip()
            except Exception:
                pass
        return os.environ.get("OPENCODE_API_KEY", "").strip()

    def is_configured(self) -> bool:
        return bool(self.api_key)

    def get_available_free_models(self) -> List[ModelCapability]:
        return [
            ModelCapability("jev-1.13-free", "Jev 1.13 (Free)", "opencode-inference", True, 200000, True, True, True, ["coding", "reasoning"]),
            ModelCapability("glm-5.3-flash-free", "GLM Flash Free (Free)", "opencode-inference", True, 128000, False, True, True, ["lightweight", "research"])
        ]

    def chat_completion(
        self,
        model: str,
        messages: List[Dict[str, str]],
        max_tokens: int = 1000,
        temperature: float = 0.2
    ) -> Tuple[bool, str, float]:
        if "free" not in model.lower() and ":free" not in model:
            return False, f"ERREUR_SECURITE: Le modèle '{model}' n'est pas certifié gratuit.", 0.0

        t0 = time.time()
        self.total_requests += 1
        req_data = json.dumps({
            "model": model,
            "messages": messages,
            "max_tokens": max_tokens
        }).encode("utf-8")

        req = urllib.request.Request(
            f"{self.base_url}/chat/completions",
            data=req_data,
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json"
            }
        )
        try:
            with urllib.request.urlopen(req, timeout=12) as resp:
                elapsed = (time.time() - t0) * 1000
                self.latencies.append(elapsed)
                res = json.loads(resp.read().decode("utf-8"))
                choice = res.get("choices", [{}])[0].get("message", {}).get("content", "")
                self.is_healthy = True
                return True, choice.strip(), elapsed
        except Exception as e:
            self.failed_requests += 1
            self.is_healthy = False
            self.last_error = str(e)
            elapsed = (time.time() - t0) * 1000
            return False, f"Erreur OpenCode Inference ({type(e).__name__}): {e}", elapsed


class ProviderRegistry:
    """Central registry of API providers and Model Capabilities (100% Free)."""
    def __init__(self):
        self.openrouter = OpenRouterAdapter()
        self.xkiro = XKiroAdapter()
        self.opencode_inf = OpenCodeInferenceAdapter()
        self._providers: Dict[str, BaseProviderAdapter] = {
            "openrouter": self.openrouter,
            "xkiro": self.xkiro,
            "opencode-inference": self.opencode_inf
        }

    def get_adapter(self, provider_id: str) -> Optional[BaseProviderAdapter]:
        return self._providers.get(provider_id)

    def get_all_free_models(self) -> List[ModelCapability]:
        models: List[ModelCapability] = []
        for prov in self._providers.values():
            if prov.is_configured():
                models.extend(prov.get_available_free_models())
        return models

    def get_status(self) -> List[Dict[str, Any]]:
        results = []
        for pid, prov in self._providers.items():
            free_models = prov.get_available_free_models()
            avg_latency = round(sum(prov.latencies) / max(len(prov.latencies), 1), 1) if prov.latencies else 0.0
            results.append({
                "id": pid,
                "name": prov.name,
                "status": "OPERATIONAL" if prov.is_configured() else "NOT_CONFIGURED",
                "isConfigured": prov.is_configured(),
                "modelsCount": len(free_models),
                "freeModelsCount": len(free_models),  # Strictly 100% free
                "activeModel": free_models[0].model_id if free_models else "none",
                "totalRequests": prov.total_requests,
                "failedRequests": prov.failed_requests,
                "averageLatencyMs": avg_latency,
                "lastError": prov.last_error,
                "lastChecked": time.time()
            })
        return results


class ProviderSelector:
    """Selects the best 100% FREE model based on task classification and provider health."""
    def __init__(self, registry: ProviderRegistry):
        self.registry = registry

    def select_best_free_model(self, task_type: str) -> Tuple[BaseProviderAdapter, str]:
        # Priority order: OpenRouter free models, then xKIRO free models, then OpenCode
        free_models = self.registry.get_all_free_models()
        if not free_models:
            # Fallback to default safe OpenRouter free model
            return self.registry.openrouter, "dots-studio/dots-3-note-preview:free"

        # Score candidates strictly within free models
        ranked: List[Tuple[BaseProviderAdapter, str, int]] = []
        for m in free_models:
            prov = self.registry.get_adapter(m.provider_id)
            if not prov or not prov.is_configured():
                continue

            score = 10
            if task_type in m.task_suitability:
                score += 50
            if "coder" in m.model_id.lower() and task_type in ["coding", "development", "debugging"]:
                score += 30
            if "lightning" in m.model_id.lower() or "flash" in m.model_id.lower():
                score += 20
            # Prefer providers with lower failure rates
            if prov.failed_requests == 0 and prov.total_requests > 0:
                score += 15

            ranked.append((prov, m.model_id, score))

        if not ranked:
            return self.registry.openrouter, "liquid/lfm-2.5-2.6b:free"

        ranked.sort(key=lambda x: x[2], reverse=True)
        best_prov, best_model, _ = ranked[0]
        return best_prov, best_model


class ProviderFallbackManager:
    """Manages sequential fallback between free models and providers with context preservation."""
    def __init__(self, registry: ProviderRegistry):
        self.registry = registry

    def execute_with_fallback(
        self,
        task_type: str,
        messages: List[Dict[str, str]],
        max_attempts: int = 3
    ) -> Tuple[bool, str, str, str, float]:
        """
        Executes chat completion across alternative 100% free models.
        Returns: (success, result_content, used_provider, used_model, latency_ms)
        """
        # Build candidate list of free models across configured providers
        candidates: List[Tuple[BaseProviderAdapter, str]] = []

        if self.registry.openrouter.is_configured():
            for m in self.registry.openrouter.get_available_free_models():
                candidates.append((self.registry.openrouter, m.model_id))

        if self.registry.xkiro.is_configured():
            for m in self.registry.xkiro.get_available_free_models():
                candidates.append((self.registry.xkiro, m.model_id))

        if self.registry.opencode_inf.is_configured():
            for m in self.registry.opencode_inf.get_available_free_models():
                candidates.append((self.registry.opencode_inf, m.model_id))

        if not candidates:
            return False, "Aucun fournisseur API avec modèles gratuits configuré", "none", "none", 0.0

        errors = []
        for attempt, (prov, model_id) in enumerate(candidates[:max_attempts]):
            ok, text, elapsed = prov.chat_completion(model_id, messages)
            if ok and text:
                return True, text, prov.provider_id, model_id, elapsed
            errors.append(f"[{prov.provider_id}:{model_id}] {text}")

        return False, "Tous les modèles gratuits ont échoué: " + " | ".join(errors), "none", "none", 0.0
