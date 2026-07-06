"""Tests for src/scoring/conversion_quality.py (Track 2)."""
from __future__ import annotations

import pytest

from src.scoring import conversion_quality as cq


# --- normalization -----------------------------------------------------------


def test_normalize_lowercases_and_collapses_whitespace():
    assert cq.normalize_rimay("The  App\tMUST\n respond.") == "the app must respond."


def test_normalize_strips_placeholders():
    text = "<MISSING_CONDITION> the App must <MISSING_ACTION>. <NON_ATOMIC>"
    assert cq.normalize_rimay(text) == "the app must ."


# --- conversion_similarity (the swappable v0 metric) -------------------------


def test_similarity_identical_is_one():
    text = "When the user logs in, the System must show the dashboard."
    assert cq.conversion_similarity(text, text) == pytest.approx(1.0)


def test_similarity_ignores_case_whitespace_and_placeholders():
    a = "<MISSING_CONDITION> The App must play a sound."
    b = "the app  must play a sound."
    assert cq.conversion_similarity(a, b) == pytest.approx(1.0)


def test_similarity_symmetric_and_bounded():
    a = "The System must send a confirmation message."
    b = "For each user, the App shall delete the account."
    s_ab = cq.conversion_similarity(a, b)
    s_ba = cq.conversion_similarity(b, a)
    assert s_ab == pytest.approx(s_ba)
    assert 0.0 <= s_ab < 1.0


def test_similarity_both_empty_is_one():
    assert cq.conversion_similarity("<MISSING_ACTION>", "<NON_ATOMIC>") == 1.0


# --- token Jaccard -----------------------------------------------------------


def test_jaccard_known_value():
    a = "the app must play"     # {the, app, must, play}
    b = "the app must stop"     # {the, app, must, stop}
    # intersection 3, union 5
    assert cq.token_jaccard(a, b) == pytest.approx(3 / 5)


def test_jaccard_disjoint_is_zero():
    assert cq.token_jaccard("alpha beta", "gamma delta") == 0.0


# --- pairwise human baseline -------------------------------------------------


def test_pairwise_counts_c_n_2():
    texts = ["a b c", "a b d", "a e f"]
    sims = cq.pairwise_similarities(texts, cq.token_jaccard)
    assert len(sims) == 3  # C(3,2)


def test_pairwise_drops_empty_texts():
    texts = ["the app must respond.", "", "   ", "the app must respond."]
    sims = cq.pairwise_similarities(texts)
    assert sims == [pytest.approx(1.0)]  # only one usable pair


# --- distribution ------------------------------------------------------------


def test_distribution_stats():
    d = cq.Distribution([0.2, 0.4, 0.6])
    assert d.n == 3
    assert d.mean == pytest.approx(0.4)
    assert d.median == pytest.approx(0.4)
    assert d.min == pytest.approx(0.2)
    assert d.max == pytest.approx(0.6)
    assert d.stdev == pytest.approx(0.2)


def test_distribution_empty_is_none():
    d = cq.Distribution([])
    assert d.n == 0
    assert d.mean is None and d.median is None and d.stdev is None


# --- Paska summary from manifest rows ----------------------------------------


def test_paska_summary_counts_and_frequencies():
    rows = [
        {"paska_passed": True, "paska_smells": []},
        {"paska_passed": False, "paska_smells": ["Passive voice", "Not precise verb"]},
        {"paska_passed": False, "paska_smells": ["Passive voice"]},
        {"paska_passed": None, "paska_smells": [], "paska_error": "boom"},
    ]
    s = cq.summarize_paska(rows)
    assert s.n_total == 4
    assert s.n_passed == 1
    assert s.n_failed == 2
    assert s.n_errors == 1
    # errors excluded from the pass-rate denominator
    assert s.pass_rate == pytest.approx(1 / 3)
    assert s.smell_frequencies == {"Passive voice": 2, "Not precise verb": 1}


def test_paska_summary_empty():
    s = cq.summarize_paska([])
    assert s.pass_rate is None
    assert s.smell_frequencies == {}
