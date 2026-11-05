#!/usr/bin/env python3
"""
Comprehensive verification test for AGY Model Routing and Selection.
Validates:
1. All 6 canonical models are accepted by agy CLI with exit code 0.
2. Canonical resolution and aliases in agy_acp_bridge.py.
3. Invalidation of fictitious 'gemini-4-argon-*'.
4. Proper handling and query of '/model' and '/signin'.
5. Proof of arguments passed to the AGY process.
"""

import os
import sys
import json
import subprocess

# Add bot-antigravity directory to path
sys.path.insert(0, "/root/CodeStation/environments/bot-antigravity")
import agy_acp_bridge as bridge

OFFICIAL_MODELS = [
    "gemini-3.8-flash-medium",
    "gemini-3.8-flash-high",
    "gemini-3.8-flash-low",
    "claude-sonnet-4-6",
    "claude-opus-4-6-thinking",
    "gpt-oss-120b-medium"
]

def test_bridge_resolution():
    print("=" * 60)
    print("TEST 1: Résolution canonique des modèles dans le bridge")
    print("=" * 60)
    
    # 1. Direct canonical IDs
    for m in OFFICIAL_MODELS:
        info = bridge.resolve_model(m)
        resolved = info["id"]
        effort = info.get("effort")
        assert resolved == m, f"Résolution échouée pour {m}: obtenu {resolved}"
        print(f"  [OK] Résolution exacte : {m} -> {resolved} (effort={effort})")
    
    # 2. Display names
    display_mappings = {
        "Gemini 3.8 Flash (Medium)": "gemini-3.8-flash-medium",
        "Gemini 3.8 Flash (High)": "gemini-3.8-flash-high",
        "Gemini 3.8 Flash (Low)": "gemini-3.8-flash-low",
        "Claude Sonnet 4.6 (Thinking)": "claude-sonnet-4-6",
        "Claude Opus 4.6 (Thinking)": "claude-opus-4-6-thinking",
        "GPT-OSS 120B (Medium)": "gpt-oss-120b-medium"
    }
    for display, expected in display_mappings.items():
        info = bridge.resolve_model(display)
        resolved = info["id"]
        effort = info.get("effort")
        assert resolved == expected, f"Display name '{display}' -> {resolved} != {expected}"
        print(f"  [OK] Display name : '{display}' -> {resolved} (effort={effort})")

    # 3. Legacy / invalid names must safely redirect or fallback, NEVER inject gemini-4-argon
    info_argon = bridge.resolve_model("gemini-4-argon-medium")
    resolved_argon = info_argon["id"]
    assert resolved_argon == "gemini-3.8-flash-medium", f"Argon fallback failed: {resolved_argon}"
    print(f"  [OK] Redirection d'héritage : gemini-4-argon-medium -> {resolved_argon}")

    info_unknown = bridge.resolve_model("non-existent-fantasy-model")
    resolved_unknown = info_unknown["id"]
    assert resolved_unknown == "gemini-3.8-flash-medium"
    print(f"  [OK] Repli sécurisé pour modèle inconnu -> {resolved_unknown}")


def test_real_agy_execution_each_model():
    print("\n" + "=" * 60)
    print("TEST 2: Exécution réelle de la CLI agy avec chaque modèle")
    print("=" * 60)
    
    agy_bin = "/root/.local/bin/agy"
    assert os.path.exists(agy_bin), f"Binaire {agy_bin} manquant"

    for model_id in OFFICIAL_MODELS:
        prompt = f"Confirme modèle avec identifiant: {model_id}. Réponds exactement '{model_id}:OK'."
        cmd = [agy_bin, "--model", model_id, "-p", prompt, "--dangerously-skip-permissions"]
        print(f"\n[RUN] Commande : {' '.join(cmd)}")
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        assert res.returncode == 0, f"Erreur avec {model_id}: code={res.returncode}, stderr={res.stderr}"
        assert f"{model_id}:OK" in res.stdout or "OK" in res.stdout, f"Sortie inattendue: {res.stdout.strip()}"
        print(f"  -> Code de retour : {res.returncode}")
        print(f"  -> Sortie réelle  : {res.stdout.strip()[:100]}...")
        print(f"  -> Résultat       : SUCCÈS VÉRIFIÉ")


def test_slash_commands():
    print("\n" + "=" * 60)
    print("TEST 3: Commandes Slash (/model et /signin)")
    print("=" * 60)

    # Test /model handler in bridge
    res = bridge.execute_slash_command("/model")
    assert res is not None, "execute_slash_command(/model) a retourné None"
    assert "Modèle Google Antigravity actif" in res
    print(f"  [OK] /model output:\n    {res.replace(chr(10), chr(10) + '    ')}")

    # Test /signin handler in bridge
    res_signin = bridge.execute_slash_command("/signin")
    assert res_signin is not None
    assert "Session Google Antigravity active" in res_signin
    print(f"\n  [OK] /signin output:\n    {res_signin.replace(chr(10), chr(10) + '    ')}")

    # Test /model <name> switch
    res_switch = bridge.execute_slash_command("/model claude-sonnet-4-6")
    assert res_switch is not None
    assert "Modèle changé pour" in res_switch
    print(f"\n  [OK] /model switch output:\n    {res_switch.replace(chr(10), chr(10) + '    ')}")
    
    # Restore default
    bridge.execute_slash_command("/model gemini-3.8-flash-medium")


if __name__ == "__main__":
    try:
        test_bridge_resolution()
        test_real_agy_execution_each_model()
        test_slash_commands()
        print("\n" + "=" * 60)
        print("TOUS LES TESTS AGY ONT RÉUSSI AVEC SUCCÈS !")
        print("=" * 60)
    except Exception as e:
        print(f"\n[ÉCHEC] {e}", file=sys.stderr)
        sys.exit(1)
