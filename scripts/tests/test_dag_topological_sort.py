#!/usr/bin/env python3
"""
Unit tests for CAMCS DAG Planner — Kahn's Topological Sort & Cycle Detection
Inspired by Festival's fest/internal/deps/graph.go.
"""

import unittest
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from camcs.dag_planner import DagPlanner, DagTask, CycleDetectedError

class TestDagTopologicalSort(unittest.TestCase):

    def setUp(self):
        self.planner = DagPlanner()

    def test_linear_dag(self):
        t1 = DagTask(id="task-1", mission_id="m1", agent_id="a1", classification="ARCHITECTURE",
                     title="Task 1", description="Desc 1", prompt="p1", dependencies=[])
        t2 = DagTask(id="task-2", mission_id="m1", agent_id="a1", classification="CODE_GENERATION",
                     title="Task 2", description="Desc 2", prompt="p2", dependencies=["task-1"])
        t3 = DagTask(id="task-3", mission_id="m1", agent_id="a1", classification="VERIFICATION",
                     title="Task 3", description="Desc 3", prompt="p3", dependencies=["task-2"])

        sorted_tasks = self.planner.topological_sort([t3, t1, t2])
        sorted_ids = [t.id for t in sorted_tasks]
        self.assertEqual(sorted_ids, ["task-1", "task-2", "task-3"])

    def test_diamond_dag(self):
        # A -> B, A -> C; B -> D, C -> D
        ta = DagTask(id="A", mission_id="m1", agent_id="a1", classification="ARCHITECTURE",
                     title="A", description="A", prompt="p", dependencies=[])
        tb = DagTask(id="B", mission_id="m1", agent_id="a1", classification="CODE_GENERATION",
                     title="B", description="B", prompt="p", dependencies=["A"])
        tc = DagTask(id="C", mission_id="m1", agent_id="a1", classification="CODE_GENERATION",
                     title="C", description="C", prompt="p", dependencies=["A"])
        td = DagTask(id="D", mission_id="m1", agent_id="a1", classification="VERIFICATION",
                     title="D", description="D", prompt="p", dependencies=["B", "C"])

        sorted_tasks = self.planner.topological_sort([td, tc, tb, ta])
        sorted_ids = [t.id for t in sorted_tasks]

        self.assertEqual(sorted_ids[0], "A")
        self.assertEqual(sorted_ids[-1], "D")
        self.assertIn("B", sorted_ids[1:3])
        self.assertIn("C", sorted_ids[1:3])

    def test_cycle_detection(self):
        # A -> B -> C -> A (Circular)
        ta = DagTask(id="A", mission_id="m1", agent_id="a1", classification="ARCHITECTURE",
                     title="A", description="A", prompt="p", dependencies=["C"])
        tb = DagTask(id="B", mission_id="m1", agent_id="a1", classification="CODE_GENERATION",
                     title="B", description="B", prompt="p", dependencies=["A"])
        tc = DagTask(id="C", mission_id="m1", agent_id="a1", classification="CODE_GENERATION",
                     title="C", description="C", prompt="p", dependencies=["B"])

        self.assertTrue(self.planner.has_cycle([ta, tb, tc]))
        with self.assertRaises(CycleDetectedError) as ctx:
            self.planner.topological_sort([ta, tb, tc])
        self.assertTrue(len(ctx.exception.cycle_path) > 0)

    def test_parallel_runnable_tasks(self):
        # A and B are independent (in-degree = 0), C depends on A and B
        ta = DagTask(id="A", mission_id="m1", agent_id="a1", classification="ARCHITECTURE",
                     title="A", description="A", prompt="p", dependencies=[])
        tb = DagTask(id="B", mission_id="m1", agent_id="a2", classification="ARCHITECTURE",
                     title="B", description="B", prompt="p", dependencies=[])
        tc = DagTask(id="C", mission_id="m1", agent_id="a1", classification="CODE_GENERATION",
                     title="C", description="C", prompt="p", dependencies=["A", "B"])

        runnable = self.planner.get_parallel_runnable_tasks([ta, tb, tc])
        runnable_ids = {t.id for t in runnable}
        self.assertEqual(runnable_ids, {"A", "B"})

    def test_phases_and_sequence_order(self):
        t1 = DagTask(id="t1", mission_id="m1", agent_id="a1", classification="ARCHITECTURE",
                     title="T1", description="D1", prompt="p1", phase="01-discovery", sequence_order=1)
        self.assertEqual(t1.phase, "01-discovery")
        self.assertEqual(t1.sequence_order, 1)

if __name__ == "__main__":
    unittest.main()
