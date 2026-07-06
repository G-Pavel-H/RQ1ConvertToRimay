"""Tests for src/scoring/field_accuracy.py (Track 1)."""
from __future__ import annotations

import pytest

from src.scoring import field_accuracy as fa


def _row(req_id, gold, llm, gold_incomplete=False):
    """Build a RequirementSlots from (scope, condition, actor, modalVerb, action) tuples."""
    slots = ("scope", "condition", "actor", "modalVerb", "action")
    return fa.RequirementSlots(
        req_id=req_id,
        gold_slots=dict(zip(slots, gold)),
        llm_slots=dict(zip(slots, llm)),
        gold_overall_incomplete=gold_incomplete,
    )


# --- binary collapse ---------------------------------------------------------


def test_collapse_present_and_implied_are_not_missing():
    assert fa.collapse_gold("present") == "not-missing"
    assert fa.collapse_gold("implied") == "not-missing"
    assert fa.collapse_gold("missing") == "missing"


def test_slot_match_agreement():
    assert fa.slot_match("missing", "missing") is True
    assert fa.slot_match("present", "filled") is True
    assert fa.slot_match("implied", "filled") is True
    assert fa.slot_match("implied", "missing") is False  # over-flag
    assert fa.slot_match("missing", "filled") is False  # silent fill


def test_invalid_labels_rejected():
    with pytest.raises(ValueError):
        _row("r1", ("bogus", "present", "present", "present", "present"),
             ("filled",) * 5)
    with pytest.raises(ValueError):
        _row("r1", ("present",) * 5,
             ("bogus", "filled", "filled", "filled", "filled"))


# --- confusion / P / R / F1 --------------------------------------------------


def test_confusion_cells_and_prf():
    c = fa.Confusion()
    c.add("missing", "missing")  # TP
    c.add("missing", "missing")  # TP
    c.add("present", "missing")  # FP (implied/present collapse)
    c.add("missing", "filled")   # FN
    c.add("implied", "filled")   # TN
    assert (c.tp, c.fp, c.fn, c.tn) == (2, 1, 1, 1)
    assert c.precision == pytest.approx(2 / 3)
    assert c.recall == pytest.approx(2 / 3)
    assert c.f1 == pytest.approx(2 / 3)
    assert c.gold_positives == 3


def test_zero_denominators_yield_none():
    c = fa.Confusion()
    c.add("present", "filled")  # TN only
    assert c.precision is None  # LLM never said missing
    assert c.recall is None     # gold never missing
    assert c.f1 is None


def test_score_micro_pools_all_slots():
    rows = [
        _row("r1",
             ("missing", "missing", "present", "present", "present"),
             ("missing", "filled", "filled", "filled", "filled")),
        _row("r2",
             ("present", "implied", "present", "present", "missing"),
             ("filled", "missing", "filled", "filled", "missing")),
    ]
    report = fa.score(rows)
    m = report.micro
    # r1: TP(scope), FN(condition), 3x TN; r2: FP(condition), TP(action), 3x TN
    assert (m.tp, m.fp, m.fn, m.tn) == (2, 1, 1, 6)
    assert m.support == 10  # 2 requirements x 5 slots
    assert report.per_slot["scope"].tp == 1
    assert report.per_slot["condition"].fp == 1
    assert report.per_slot["condition"].fn == 1
    assert report.per_slot["action"].tp == 1


def test_macro_skips_undefined_slots():
    rows = [
        _row("r1",
             ("missing", "present", "present", "present", "present"),
             ("missing", "filled", "filled", "filled", "filled")),
    ]
    report = fa.score(rows)
    # Only scope has any missing signal; the other four slots are all-TN
    # with undefined P/R/F1, so the macro average is over scope alone.
    assert report.macro_f1 == pytest.approx(1.0)
    assert report.macro_precision == pytest.approx(1.0)
    assert report.macro_recall == pytest.approx(1.0)


# --- secondary lenses --------------------------------------------------------


def test_implied_lens_counts():
    rows = [
        _row("r1",
             ("implied", "implied", "present", "present", "present"),
             ("filled", "missing", "filled", "filled", "filled")),
    ]
    report = fa.score(rows)
    o = report.lens_overall
    assert o.n_gold_implied == 2
    assert o.n_implied_llm_filled == 1   # inferred context
    assert o.n_implied_llm_missing == 1  # over-flag
    assert o.implied_filled_rate == pytest.approx(0.5)


def test_missing_lens_counts():
    rows = [
        _row("r1",
             ("missing", "missing", "missing", "present", "present"),
             ("missing", "filled", "filled", "filled", "filled")),
    ]
    report = fa.score(rows)
    o = report.lens_overall
    assert o.n_gold_missing == 3
    assert o.n_missing_llm_missing == 1  # correctly flagged
    assert o.n_missing_llm_filled == 2   # silent fill / compensation
    assert o.missing_flagged_rate == pytest.approx(1 / 3)


def test_lens_rates_none_when_no_support():
    rows = [_row("r1", ("present",) * 5, ("filled",) * 5)]
    report = fa.score(rows)
    assert report.lens_overall.implied_filled_rate is None
    assert report.lens_overall.missing_flagged_rate is None


# --- overall verdict ---------------------------------------------------------


def test_llm_verdict_mandatory_slots_only():
    # scope/condition missing do NOT make the requirement incomplete
    assert fa.llm_overall_incomplete(
        {"scope": "missing", "condition": "missing",
         "actor": "filled", "modalVerb": "filled", "action": "filled"}
    ) is False
    for mandatory in ("actor", "modalVerb", "action"):
        slots = {"scope": "filled", "condition": "filled",
                 "actor": "filled", "modalVerb": "filled", "action": "filled"}
        slots[mandatory] = "missing"
        assert fa.llm_overall_incomplete(slots) is True


def test_verdict_confusion_and_agreement():
    rows = [
        # gold incomplete, llm incomplete (actor missing) -> agree
        _row("r1", ("present", "present", "missing", "present", "present"),
             ("filled", "filled", "missing", "filled", "filled"),
             gold_incomplete=True),
        # gold incomplete, llm complete -> disagree
        _row("r2", ("present", "present", "missing", "present", "present"),
             ("filled", "filled", "filled", "filled", "filled"),
             gold_incomplete=True),
        # gold complete, llm incomplete (action flagged) -> disagree
        _row("r3", ("present", "present", "present", "present", "implied"),
             ("filled", "filled", "filled", "filled", "missing"),
             gold_incomplete=False),
        # gold complete, llm complete (scope flag doesn't count) -> agree
        _row("r4", ("missing", "present", "present", "present", "present"),
             ("missing", "filled", "filled", "filled", "filled"),
             gold_incomplete=False),
    ]
    report = fa.score(rows)
    v = report.verdict
    assert v.gold_inc_llm_inc == 1
    assert v.gold_inc_llm_com == 1
    assert v.gold_com_llm_inc == 1
    assert v.gold_com_llm_com == 1
    assert v.agreement_rate == pytest.approx(0.5)


# --- per-requirement match flags --------------------------------------------


def test_per_slot_matches():
    row = _row("r1",
               ("implied", "missing", "present", "missing", "present"),
               ("filled", "missing", "filled", "filled", "missing"))
    matches = fa.per_slot_matches(row)
    assert matches == {
        "scope": True,       # implied vs filled
        "condition": True,   # missing vs missing
        "actor": True,       # present vs filled
        "modalVerb": False,  # missing vs filled (silent fill)
        "action": False,     # present vs missing (over-flag)
    }
