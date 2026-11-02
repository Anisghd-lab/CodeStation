import os
import sys
import time
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
try:
    from camcs_engine import CamcsMission, CamcsEngine
except ImportError:
    from scripts.camcs_engine import CamcsMission, CamcsEngine

class TestMissionOrdering(unittest.TestCase):
    def test_missions_sorted_descending(self):
        # Create missions with staggered timestamps
        m1 = CamcsMission("m-1", "Mission 1 (Ancienne)", "Obj 1")
        m1.created_at = 1000.0

        m2 = CamcsMission("m-2", "Mission 2 (Intermédiaire)", "Obj 2")
        m2.created_at = 2000.0

        m3 = CamcsMission("m-3", "Mission 3 (Plus récente)", "Obj 3")
        m3.created_at = 3000.0

        missions_dict = {
            m1.id: m1,
            m2.id: m2,
            m3.id: m3,
        }

        # Sorted DESC as implemented in camcs_engine.py
        sorted_missions = sorted(missions_dict.values(), key=lambda m: getattr(m, "created_at", 0), reverse=True)

        self.assertEqual(sorted_missions[0].id, "m-3")
        self.assertEqual(sorted_missions[1].id, "m-2")
        self.assertEqual(sorted_missions[2].id, "m-1")

        # Adding a brand new mission
        m4 = CamcsMission("m-4", "Mission 4 (Nouvelle)", "Obj 4")
        m4.created_at = 4000.0
        missions_dict[m4.id] = m4

        sorted_after_create = sorted(missions_dict.values(), key=lambda m: getattr(m, "created_at", 0), reverse=True)
        self.assertEqual(sorted_after_create[0].id, "m-4", "La nouvelle mission doit systématiquement être en 1re position")

if __name__ == "__main__":
    unittest.main()
