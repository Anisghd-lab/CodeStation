#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Collective Memory Client
Bridges CAMCS missions with the Continuous Memory Engine (port 19224 / /root/CodeStation-Memory).
"""

import json
import urllib.request
import urllib.error
from typing import Dict, List, Optional, Any

class CollectiveMemoryClient:
    def __init__(self, memory_port: int = 19224):
        self.base_url = f"http://127.0.0.1:{memory_port}"

    def query_knowledge(self, query_text: str, limit: int = 3) -> List[Dict[str, Any]]:
        """Queries continuous memory repository for relevant validated solutions."""
        url = f"{self.base_url}/api/memory/search"
        payload = json.dumps({"query": query_text, "limit": limit}).encode("utf-8")
        try:
            req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=3.0) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                return data.get("results", [])
        except Exception:
            # Fallback to GET query
            try:
                import urllib.parse
                get_url = f"{self.base_url}/api/memory/search?q={urllib.parse.quote(query_text)}"
                with urllib.request.urlopen(get_url, timeout=3.0) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    return data.get("results", [])
            except Exception:
                return []

    def record_learning(
        self,
        title: str,
        problem: str,
        solution: str,
        error_avoidance: str = "",
        context: str = "",
        tags: Optional[List[str]] = None,
        author: str = "camcs-agent"
    ) -> bool:
        """Records an immediately learned rule/solution into perpetual memory."""
        url = f"{self.base_url}/api/memory/learn"
        payload = json.dumps({
            "title": title,
            "problem": problem,
            "solution": solution,
            "error_avoidance": error_avoidance,
            "context": context,
            "tags": tags or ["camcs", "learned-rule"],
            "author": author
        }).encode("utf-8")
        try:
            req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=4.0) as resp:
                return resp.status == 200
        except Exception:
            return False

    def ingest_mission_experience(
        self,
        mission_id: str,
        title: str,
        objective: str,
        deliverables: List[Dict[str, Any]],
        proofs: List[Dict[str, Any]],
        success: bool = True
    ) -> bool:
        """Submits verified mission outcomes to collective memory."""
        url = f"{self.base_url}/api/memory/collect"
        payload = json.dumps({
            "missionId": mission_id,
            "title": title,
            "objective": objective,
            "deliverablesCount": len(deliverables),
            "proofsCount": len(proofs),
            "verified": success
        }).encode("utf-8")
        try:
            req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=4.0) as resp:
                return resp.status == 200
        except Exception:
            return False
