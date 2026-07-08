"""Tests for Track 2 conversion-quality pure functions."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.scoring import conversion_quality as cq


def _item(req_id, llm, canonical, humans, passed=True, smells=None):
    return cq.QualityItem(req_id, llm, canonical, humans, passed, smells or [])


def test_normalize_strips_placeholders_and_case():
    assert cq.normalize_text("The App must <MISSING_ACTION>.") == "the app must ."
    assert cq.normalize_text("  A   B <NON_ATOMIC>") == "a b"


def test_seq_ratio_bounds():
    assert cq.seq_ratio("the app must show a preview", "the app must show a preview") == 1.0
    assert cq.seq_ratio("", "") == 1.0
    assert 0.0 <= cq.seq_ratio("abc def", "xyz qrs") < 1.0


def test_token_jaccard():
    assert cq.token_jaccard("a b c", "a b c") == 1.0
    assert cq.token_jaccard("a b", "c d") == 0.0
    # {a,b,c} vs {b,c,d} -> 2/4
    assert cq.token_jaccard("a b c", "b c d") == 0.5
    assert cq.token_jaccard("", "") == 1.0
    assert cq.token_jaccard("a", "") == 0.0


def test_conversion_similarity_is_swappable_primary():
    # v0 primary == seq_ratio
    assert cq.conversion_similarity("a b c", "a b c") == cq.seq_ratio("a b c", "a b c")


def test_llm_vs_gold_skips_empty_canonical():
    items = [
        _item("a", "the app must do x", "the app must do x", []),
        _item("b", "whatever", "", []),  # empty canonical -> skipped
    ]
    res = cq.llm_vs_gold_similarity(items)
    assert res["n_evaluated"] == 1
    assert res["n_skipped_no_canonical"] == 1
    assert res["skipped_req_ids"] == ["b"]
    assert res["seq_ratio"]["mean"] == 1.0


def test_human_human_pairs_count():
    # 3 humans -> C(3,2) = 3 pairs
    items = [_item("a", "x", "x", ["one two", "one three", "two three"])]
    res = cq.human_human_similarity(items)
    assert res["n_pairs"] == 3
    assert res["seq_ratio"]["n"] == 3


def test_human_human_ignores_blank_conversions():
    items = [_item("a", "x", "x", ["only one", "", "  "])]
    res = cq.human_human_similarity(items)
    assert res["n_pairs"] == 0  # only one non-blank human -> no pairs


def test_paska_summary_rates_and_frequency():
    items = [
        _item("a", "x", "x", [], passed=True, smells=[]),
        _item("b", "x", "x", [], passed=False,
              smells=[{"smell": "Passive voice"}, {"smell": "Passive voice"}]),
        _item("c", "x", "x", [], passed=None, smells=[]),  # error, excluded from rate
    ]
    p = cq.paska_summary(items)
    assert p["n"] == 3
    assert p["n_passed"] == 1
    assert p["n_failed"] == 1
    assert p["n_error"] == 1
    assert p["n_scored"] == 2
    assert p["pass_rate"] == 0.5
    assert p["n_with_smells"] == 1
    assert p["smell_frequency"] == {"Passive voice": 2}


def test_full_report_shape():
    items = [_item("a", "the app must show x", "the app must show y",
                   ["the app must show x", "the app should show y"])]
    rep = cq.conversion_quality_report(items)
    assert "similarity" in rep and "paska" in rep
    assert "llm_vs_gold" in rep["similarity"]
    assert "human_human" in rep["similarity"]
