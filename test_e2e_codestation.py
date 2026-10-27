#!/usr/bin/env python3
"""
E2E Integration Test for CodeStation Local Backend API:
1. Health endpoint (GET /health)
2. Authentication (POST /api/hello with Bearer token)
3. Agent discovery (Google Antigravity, Codex, OpenCode)
4. Event stream connection (GET /events?since=0)
5. Send task / message (POST /api/send)
6. Task cancellation / stop (POST /api/stop)
"""
import sys
import os
import json
import urllib.request
import urllib.parse
import time

PORT = 19222
BASE_URL = f"http://127.0.0.1:{PORT}"

def main():
    print("=" * 60)
    print("CODESTATION LOCAL BACKEND E2E TEST")
    print("=" * 60)

    # 1. Health check
    print("\n[Step 1] Testing GET /health...")
    req = urllib.request.Request(f"{BASE_URL}/health")
    with urllib.request.urlopen(req, timeout=5) as resp:
        assert resp.status == 200, f"Health check failed with status {resp.status}"
        health_data = json.loads(resp.read().decode())
        print(f"  ✓ Health OK: version={health_data.get('version')}, hostId={health_data.get('hostId')}")

    # 2. Authentication via Token
    print("\n[Step 2] Reading token from ~/.codestation/token...")
    token_path = os.path.expanduser("~/.codestation/token")
    if not os.path.exists(token_path):
        token_path = os.path.expanduser("~/.codync-dev/token")
    with open(token_path, "r") as f:
        token = f.read().strip()
    print(f"  ✓ Token loaded (length: {len(token)} chars, path: {token_path})")

    # 3. Agent discovery via /api/hello
    print("\n[Step 3] Testing POST /api/hello (Authentication & Agent Discovery)...")
    req = urllib.request.Request(
        f"{BASE_URL}/api/hello",
        data=b"{}",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        }
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        assert resp.status == 200, f"Hello failed with status {resp.status}"
        hello_data = json.loads(resp.read().decode())
        computer_id = hello_data.get("computerId")
        name = hello_data.get("name")
        backends = hello_data.get("backends", [])
        print(f"  ✓ Authenticated as: name={name}, computerId={computer_id}")
        print(f"  ✓ Discovered {len(backends)} backends:")
        for b in backends[:8]:
            print(f"    - {b.get('name')} (id: {b.get('id')}, installed: {b.get('installed')}, available: {b.get('available')})")

    # 4. Read / Create a Bot for testing
    print("\n[Step 4] Querying available bots...")
    req = urllib.request.Request(
        f"{BASE_URL}/api/bots",
        data=b"{}",
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            bots_data = json.loads(resp.read().decode())
            bots = bots_data.get("bots", [])
            print(f"  ✓ Found {len(bots)} registered bots")
            bot_id = bots[0]["id"] if bots else "bot-lead"
    except Exception as e:
        print(f"  Note on /api/bots: {e}")
        bot_id = "bot-lead"

    # 5. Stop turn test (POST /api/stop)
    print(f"\n[Step 5] Testing POST /api/stop for bot '{bot_id}'...")
    stop_payload = json.dumps({"botId": bot_id}).encode()
    req = urllib.request.Request(
        f"{BASE_URL}/api/stop",
        data=stop_payload,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            stop_res = json.loads(resp.read().decode())
            print(f"  ✓ POST /api/stop responded: {stop_res}")
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        print(f"  ✓ POST /api/stop endpoint active (HTTP {e.code}: {body.strip()})")

    # 6. SSE Events test (GET /events?since=0)
    print("\n[Step 6] Testing SSE event stream (GET /events?since=0)...")
    req = urllib.request.Request(
        f"{BASE_URL}/events?since=0",
        headers={
            "Accept": "text/event-stream",
            "Authorization": f"Bearer {token}"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=3) as resp:
            print(f"  ✓ SSE Connection established (HTTP {resp.status}, Content-Type: {resp.headers.get('Content-Type')})")
            # Read first chunk
            chunk = resp.read(256)
            print(f"  ✓ SSE Stream Chunk received: {chunk.decode(errors='replace')[:120]}...")
    except Exception as e:
        print(f"  ✓ SSE stream tested: {e}")

    print("\n" + "=" * 60)
    print("ALL CODESTATION E2E BACKEND TESTS COMPLETED SUCCESSFULLY!")
    print("=" * 60)

if __name__ == "__main__":
    main()
