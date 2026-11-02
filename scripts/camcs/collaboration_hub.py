#!/usr/bin/env python3
"""
CAMCS Real Execution Engine — Inter-Agent Collaboration Hub
Manages structured agent-to-agent exchanges, deliverable handovers, and review requests.
"""

import time
import uuid
import json
import sqlite3
from typing import Dict, List, Optional, Any

DB_PATH = os = "/root/.codestation/camcs.db"

class CollaborationHub:
    def __init__(self, db_path: str = DB_PATH):
        self.db_path = db_path

    def record_communication(
        self,
        mission_id: str,
        sender_id: str,
        sender_name: str,
        receiver_id: str,
        receiver_name: str,
        message_type: str,
        content: str,
        payload: Optional[Dict[str, Any]] = None,
        impact: Optional[str] = None
    ) -> Dict[str, Any]:
        comm_id = f"comm-{uuid.uuid4().hex[:8]}"
        now = time.time()
        payload_str = json.dumps(payload or {})

        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()
        cur.execute("""
            INSERT OR REPLACE INTO agent_communications
            (id, mission_id, sender_agent_id, sender_name, receiver_agent_id, receiver_name, message_type, content, payload_json, status, impact_on_workflow, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            comm_id, mission_id, sender_id, sender_name, receiver_id, receiver_name,
            message_type, content, payload_str, "SENT", impact, now
        ))
        conn.commit()
        conn.close()

        return {
            "id": comm_id,
            "missionId": mission_id,
            "senderAgentId": sender_id,
            "senderName": sender_name,
            "receiverAgentId": receiver_id,
            "receiverName": receiver_name,
            "messageType": message_type,
            "content": content,
            "status": "SENT",
            "impactOnWorkflow": impact,
            "createdAt": now
        }

    def get_mission_communications(self, mission_id: str) -> List[Dict[str, Any]]:
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        cur = conn.cursor()
        cur.execute("""
            SELECT id, mission_id, sender_agent_id, sender_name, receiver_agent_id, receiver_name, message_type, content, status, impact_on_workflow, created_at
            FROM agent_communications WHERE mission_id = ? ORDER BY created_at ASC
        """, (mission_id,))
        rows = cur.fetchall()
        conn.close()

        return [
            {
                "id": r["id"],
                "missionId": r["mission_id"],
                "senderAgentId": r["sender_agent_id"],
                "senderName": r["sender_name"],
                "receiverAgentId": r["receiver_agent_id"],
                "receiverName": r["receiver_name"],
                "messageType": r["message_type"],
                "content": r["content"],
                "status": r["status"],
                "impactOnWorkflow": r["impact_on_workflow"],
                "createdAt": r["created_at"]
            }
            for r in rows
        ]
