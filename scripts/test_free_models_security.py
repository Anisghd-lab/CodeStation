#!/usr/bin/env python3
"""
Test de Sécurité des Modèles 100% Gratuits CAMCS v2.0
- Vérifie le blocage pré-émission des modèles payants
- Vérifie la tarification 0$ des modèles gratuits
- Vérifie l'absence totale de fuite de tokens ou secrets
"""

import sys
import os
import unittest

sys.path.insert(0, "/root/CodeStation/scripts")
from camcs.provider_registry import ProviderRegistry, ProviderSelector, ProviderFallbackManager

class TestFreeModelsSecurity(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.reg = ProviderRegistry()

    def test_01_blocked_paid_model_openrouter(self):
        """1. Rejet absolu des modèles payants sur OpenRouter avant appel réseau."""
        adapter = self.reg.openrouter
        ok, msg, dur = adapter.chat_completion(
            model="openai/gpt-4o",
            messages=[{"role": "user", "content": "test"}]
        )
        self.assertFalse(ok)
        self.assertIn("ERREUR_SECURITE", msg)
        self.assertIn("pas certifié 100% gratuit", msg)
        print(f"[*] Rejet modèle payant OpenRouter (gpt-4o) validé : {msg}")

    def test_02_blocked_paid_model_xkiro(self):
        """2. Rejet absolu des modèles payants sur xKIRO avant appel réseau."""
        adapter = self.reg.xkiro
        ok, msg, dur = adapter.chat_completion(
            model="anthropic/claude-3.5-sonnet",
            messages=[{"role": "user", "content": "test"}]
        )
        self.assertFalse(ok)
        self.assertIn("ERREUR_SECURITE", msg)
        self.assertIn("pas certifié gratuit", msg)
        print(f"[*] Rejet modèle payant xKIRO (claude-3.5-sonnet) validé : {msg}")

    def test_03_blocked_paid_model_opencode(self):
        """3. Rejet absolu des modèles payants sur OpenCode Inference."""
        adapter = self.reg.opencode_inf
        ok, msg, dur = adapter.chat_completion(
            model="deepseek/deepseek-r1-paid",
            messages=[{"role": "user", "content": "test"}]
        )
        self.assertFalse(ok)
        self.assertIn("ERREUR_SECURITE", msg)
        print(f"[*] Rejet modèle payant OpenCode Inference validé : {msg}")

    def test_04_verify_all_registered_models_are_free(self):
        """4. Vérification que 100% des modèles répertoriés sont certifiés gratuits."""
        all_models = self.reg.get_all_free_models()
        self.assertGreater(len(all_models), 10)
        for m in all_models:
            self.assertTrue(m.is_free, f"Le modèle {m.model_id} ({m.provider_id}) n'est pas marqué gratuit !")
            self.assertTrue(
                ":free" in m.model_id or "free" in m.model_id.lower() or m.provider_id == "openrouter",
                f"Modèle suspect non suffixé free: {m.model_id}"
            )
        print(f"[*] Certification gratuite confirmée pour l'ensemble des {len(all_models)} modèles enregistrés.")

    def test_05_zero_secret_leakage_in_adapter_repr(self):
        """5. Vérification qu'aucun token/clé API n'apparaît dans les statuts exposés."""
        status = self.reg.get_status()
        status_str = str(status)
        for token_frag in ["sk-or-", "xkiro-", "Bearer "]:
            self.assertNotIn(token_frag, status_str)
        print("[*] Absence totale de secrets vérifiée dans le statut public des fournisseurs.")

if __name__ == "__main__":
    unittest.main()
