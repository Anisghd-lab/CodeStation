import unittest
import os
import json
import glob
from scripts.memory_continuous_watcher import ContinuousMemoryEngine, MEMORY_REPO_PATH

class TestMemoryNeuralSync(unittest.TestCase):
    """
    Tests de validation pour la Tâche 11 : synchronisation et exactitude
    des compteurs de mémoire neuronale (agents, relations, nœuds, expériences, connaissances).
    """

    def setUp(self):
        self.engine = ContinuousMemoryEngine()

    def test_status_returns_exact_real_counts(self):
        """Vérifie que get_status() extrait les données réelles enregistrées sur disque."""
        status = self.engine.get_status()
        self.assertEqual(status["service"], "CodeStation Continuous Memory Engine")
        self.assertEqual(status["status"], "OPERATIONAL")
        self.assertEqual(status["sync_status"], "synchronized")

        # Vérifier que les comptages correspondent aux fichiers réels
        expected_exp = len(glob.glob(os.path.join(MEMORY_REPO_PATH, "experiences", "*.json")))
        expected_knw = len(glob.glob(os.path.join(MEMORY_REPO_PATH, "knowledge", "*.json")))
        expected_ag = len(glob.glob(os.path.join(MEMORY_REPO_PATH, "agents", "*.json")))

        self.assertEqual(status["totalExperiences"], expected_exp)
        self.assertEqual(status["experiences_count"], expected_exp)
        self.assertEqual(status["totalKnowledge"], expected_knw)
        self.assertEqual(status["knowledge_count"], expected_knw)
        self.assertEqual(status["totalAgents"], expected_ag)
        self.assertEqual(status["agents_count"], expected_ag)

        # Vérifier les relations réelles
        rel_file = os.path.join(MEMORY_REPO_PATH, "relations", "relations.json")
        expected_rel = 0
        if os.path.exists(rel_file):
            with open(rel_file, "r", encoding="utf-8") as rf:
                data = json.load(rf)
                if isinstance(data, list):
                    expected_rel = len(data)

        self.assertEqual(status["totalRelations"], expected_rel)
        self.assertEqual(status["relations_count"], expected_rel)

        # Vérifier le calcul strict de totalNodes (1 Core + agents + experiences + knowledge)
        expected_nodes = 1 + expected_ag + expected_exp + expected_knw
        self.assertEqual(status["totalNodes"], expected_nodes)
        self.assertEqual(status["nodes_count"], expected_nodes)

    def test_status_matches_manifest_invariants(self):
        """Vérifie que les compteurs sont cohérents avec le manifeste s'il existe."""
        manifest_path = os.path.join(MEMORY_REPO_PATH, "manifest.json")
        if os.path.exists(manifest_path):
            with open(manifest_path, "r", encoding="utf-8") as mf:
                manifest = json.load(mf)
                counts = manifest.get("counts", {})
                status = self.engine.get_status()
                if "experiences" in counts:
                    self.assertEqual(status["totalExperiences"], counts["experiences"])
                if "knowledge" in counts:
                    self.assertEqual(status["totalKnowledge"], counts["knowledge"])
                if "agents" in counts:
                    self.assertEqual(status["totalAgents"], counts["agents"])
                if "relations" in counts:
                    self.assertEqual(status["totalRelations"], counts["relations"])

if __name__ == "__main__":
    unittest.main()
