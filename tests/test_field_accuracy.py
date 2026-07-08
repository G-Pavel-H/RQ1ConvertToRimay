"""Tests for Track 1 field-accuracy pure functions."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.scoring import field_accuracy as fa


def _eval(req_id, gold, llm, gold_incomplete=False):
    return fa.SlotEval(req_id, gold, llm, gold_incomplete)


ALL_FILLED = {s: "filled" for s in fa.SLOTS}


def test_collapse_gold_missing():
    assert fa.collapse_gold_missing("missing") is True
    assert fa.collapse_gold_missing("MISSING") is True
    assert fa.collapse_gold_missing("present") is False
    assert fa.collapse_gold_missing("implied") is False
    assert fa.collapse_gold_missing("") is False


def test_slot_confusion_counts():
    items = [
        # gold missing, llm missing -> TP
        _eval("a", {**ALL_FILLED, "actor": "missing"}, {**ALL_FILLED, "actor": "missing"}),
        # gold present, llm missing -> FP
        _eval("b", {**ALL_FILLED, "actor": "present"}, {**ALL_FILLED, "actor": "missing"}),
        # gold missing, llm filled -> FN
        _eval("c", {**ALL_FILLED, "actor": "missing"}, {**ALL_FILLED, "actor": "filled"}),
        # gold implied, llm filled -> TN (implied collapses to not-missing)
        _eval("d", {**ALL_FILLED, "actor": "implied"}, {**ALL_FILLED, "actor": "filled"}),
    ]
    cm = fa.slot_confusion(items, "actor")
    assert cm == {"tp": 1, "fp": 1, "fn": 1, "tn": 1}


def test_prf_perfect_and_empty():
    # perfect
    items = [
        _eval("a", {**ALL_FILLED, "action": "missing"}, {**ALL_FILLED, "action": "missing"}),
        _eval("b", ALL_FILLED, ALL_FILLED),
    ]
    scores = fa.per_slot_scores(items)["action"]
    assert scores["precision"] == 1.0
    assert scores["recall"] == 1.0
    assert scores["f1"] == 1.0
    # no positives at all -> zeros, no crash
    scores2 = fa.per_slot_scores([_eval("a", ALL_FILLED, ALL_FILLED)])["action"]
    assert scores2["f1"] == 0.0


def test_micro_macro_average():
    items = [
        _eval("a", {**ALL_FILLED, "actor": "missing", "action": "missing"},
              {**ALL_FILLED, "actor": "missing", "action": "filled"}),
    ]
    per_slot = fa.per_slot_scores(items)
    micro = fa.micro_average(per_slot)
    macro = fa.macro_average(per_slot)
    # actor TP=1; action FN=1. micro recall = 1/(1+1)=0.5
    assert micro["recall"] == 0.5
    # macro averages per-slot f1 across all 5 slots
    assert 0.0 <= macro["f1"] <= 1.0


def test_implied_analysis():
    items = [
        _eval("a", {**ALL_FILLED, "scope": "implied"}, {**ALL_FILLED, "scope": "filled"}),
        _eval("b", {**ALL_FILLED, "scope": "implied"}, {**ALL_FILLED, "scope": "missing"}),
    ]
    res = fa.implied_analysis(items)["overall"]
    assert res["n_implied"] == 2
    assert res["llm_filled"] == 1
    assert res["llm_flagged_missing"] == 1
    assert res["fill_rate"] == 0.5


def test_missing_analysis_compensation():
    items = [
        # gold missing, llm filled -> silent fill (compensation)
        _eval("a", {**ALL_FILLED, "action": "missing"}, ALL_FILLED),
        # gold missing, llm flagged -> correct
        _eval("b", {**ALL_FILLED, "action": "missing"}, {**ALL_FILLED, "action": "missing"}),
    ]
    res = fa.missing_analysis(items)["overall"]
    assert res["n_missing"] == 2
    assert res["llm_silently_filled"] == 1
    assert res["llm_flagged_missing"] == 1
    assert res["flag_rate"] == 0.5


def test_llm_overall_incomplete_only_mandatory():
    # missing a non-mandatory slot (scope) does NOT make it incomplete
    assert fa.llm_overall_incomplete({**ALL_FILLED, "scope": "missing"}) is False
    # missing a mandatory slot does
    assert fa.llm_overall_incomplete({**ALL_FILLED, "actor": "missing"}) is True


def test_overall_verdict_confusion():
    items = [
        # llm complete, gold complete -> TN
        _eval("a", ALL_FILLED, ALL_FILLED, gold_incomplete=False),
        # llm incomplete (actor missing), gold incomplete -> TP
        _eval("b", ALL_FILLED, {**ALL_FILLED, "actor": "missing"}, gold_incomplete=True),
        # llm incomplete, gold complete -> FP
        _eval("c", ALL_FILLED, {**ALL_FILLED, "action": "missing"}, gold_incomplete=False),
    ]
    v = fa.overall_verdict(items)
    c = v["confusion"]
    assert c["both_complete"] == 1
    assert c["both_incomplete"] == 1
    assert c["llm_incomplete_gold_complete"] == 1
    assert c["gold_incomplete_llm_complete"] == 0
    assert v["agreement_rate"] == 2 / 3


def test_full_report_shape():
    items = [_eval("a", {**ALL_FILLED, "actor": "missing"}, {**ALL_FILLED, "actor": "missing"})]
    rep = fa.field_accuracy_report(items).as_dict()
    for key in ("per_slot", "micro", "macro", "implied_analysis",
                "missing_analysis", "overall_verdict"):
        assert key in rep
    assert rep["n"] == 1
