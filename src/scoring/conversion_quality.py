"""Track 2 — conversion quality. Pure functions, no IO.

Two lenses:

* **Similarity to gold.** Reference = ``canonicalRimay``. The metric is
  isolated behind one swappable function, :func:`conversion_similarity`.
  v0 is a deliberate, dependency-light *placeholder*: difflib's
  ``SequenceMatcher`` ratio on normalised text plus a token Jaccard. Both
  are reported. This is NOT the final metric — it is a stand-in for a
  future structural / semantic measure; swap :func:`conversion_similarity`
  (and, if needed, :func:`similarity_pair`) when that lands.

* **Human baseline (the ceiling).** The LLM-vs-gold number is only
  interpretable against how much the human annotators vary among
  themselves, so we compute the pairwise human-human similarity
  distribution with the *same* function and report it side by side.

Plus a Paska-pass summary (pass rate + smell-type frequencies) as the
independent structural fidelity signal. All inputs are plain structures.
"""
from __future__ import annotations

import re
import statistics
from collections import Counter
from difflib import SequenceMatcher
from itertools import combinations
from typing import Dict, List, Optional, Sequence

from src import config

_PLACEHOLDER_RE = re.compile(r"<MISSING_[A-Z_]+>|<NON_ATOMIC>")
_WS_RE = re.compile(r"\s+")
_TOKEN_RE = re.compile(r"[a-z0-9]+")


def normalize_text(text: str) -> str:
    """Lowercase, strip placeholders, collapse whitespace."""
    # Strip placeholders before lowercasing — the tokens are uppercase.
    s = _PLACEHOLDER_RE.sub(" ", text or "")
    s = _WS_RE.sub(" ", s.lower())
    return s.strip()


def seq_ratio(a: str, b: str) -> float:
    """difflib SequenceMatcher ratio on normalised text (0..1)."""
    na, nb = normalize_text(a), normalize_text(b)
    if not na and not nb:
        return 1.0
    return SequenceMatcher(None, na, nb).ratio()


def token_jaccard(a: str, b: str) -> float:
    """Jaccard over the token sets of normalised text (0..1)."""
    ta = set(_TOKEN_RE.findall(normalize_text(a)))
    tb = set(_TOKEN_RE.findall(normalize_text(b)))
    if not ta and not tb:
        return 1.0
    if not ta or not tb:
        return 0.0
    return len(ta & tb) / len(ta | tb)


def conversion_similarity(a: str, b: str) -> float:
    """The single swappable primary similarity (v0 = SequenceMatcher ratio).

    Placeholder for a future structural / semantic metric — swap the body
    here and every caller (LLM-vs-gold and human-human) updates together.
    """
    return seq_ratio(a, b)


def similarity_pair(a: str, b: str) -> Dict[str, float]:
    """Both v0 measures for one pair, for side-by-side reporting."""
    return {"seq_ratio": seq_ratio(a, b), "jaccard": token_jaccard(a, b)}


def _distribution(values: Sequence[float]) -> Dict[str, float]:
    vals = list(values)
    if not vals:
        return {"n": 0, "mean": 0.0, "median": 0.0, "min": 0.0, "max": 0.0, "std": 0.0}
    return {
        "n": len(vals),
        "mean": statistics.fmean(vals),
        "median": statistics.median(vals),
        "min": min(vals),
        "max": max(vals),
        "std": statistics.pstdev(vals) if len(vals) > 1 else 0.0,
    }


class QualityItem:
    """One requirement's Track-2 inputs."""

    __slots__ = ("req_id", "llm_rimay", "canonical_rimay", "human_rimays",
                 "paska_passed", "paska_smells")

    def __init__(
        self,
        req_id: str,
        llm_rimay: str,
        canonical_rimay: str,
        human_rimays: Sequence[str],
        paska_passed: Optional[bool],
        paska_smells: Sequence[dict],
    ) -> None:
        self.req_id = req_id
        self.llm_rimay = llm_rimay
        self.canonical_rimay = canonical_rimay
        self.human_rimays = list(human_rimays)
        self.paska_passed = paska_passed
        self.paska_smells = list(paska_smells)


def llm_vs_gold_similarity(items: Sequence[QualityItem]) -> Dict[str, object]:
    """Distribution of LLM-vs-canonical similarity; skips empty canonical."""
    seq_vals: List[float] = []
    jac_vals: List[float] = []
    skipped: List[str] = []
    for it in items:
        if not (it.canonical_rimay or "").strip():
            skipped.append(it.req_id)
            continue
        pair = similarity_pair(it.llm_rimay, it.canonical_rimay)
        seq_vals.append(pair["seq_ratio"])
        jac_vals.append(pair["jaccard"])
    return {
        "seq_ratio": _distribution(seq_vals),
        "jaccard": _distribution(jac_vals),
        "n_evaluated": len(seq_vals),
        "n_skipped_no_canonical": len(skipped),
        "skipped_req_ids": skipped,
    }


def human_human_similarity(items: Sequence[QualityItem]) -> Dict[str, object]:
    """Pairwise human-human similarity distribution (the ceiling)."""
    seq_vals: List[float] = []
    jac_vals: List[float] = []
    n_pairs = 0
    for it in items:
        rimays = [r for r in it.human_rimays if (r or "").strip()]
        for a, b in combinations(rimays, 2):
            pair = similarity_pair(a, b)
            seq_vals.append(pair["seq_ratio"])
            jac_vals.append(pair["jaccard"])
            n_pairs += 1
    return {
        "seq_ratio": _distribution(seq_vals),
        "jaccard": _distribution(jac_vals),
        "n_pairs": n_pairs,
    }


def paska_summary(items: Sequence[QualityItem]) -> Dict[str, object]:
    """Paska pass rate + smell-type frequencies over the evaluated set."""
    n = len(items)
    n_passed = sum(1 for it in items if it.paska_passed is True)
    n_failed = sum(1 for it in items if it.paska_passed is False)
    n_error = sum(1 for it in items if it.paska_passed is None)
    n_with_smells = sum(1 for it in items if it.paska_smells)
    freq: Counter = Counter()
    for it in items:
        for smell in it.paska_smells:
            freq[smell.get("smell", "?")] += 1
    scored = n_passed + n_failed  # exclude Paska errors from the rate
    return {
        "n": n,
        "n_passed": n_passed,
        "n_failed": n_failed,
        "n_error": n_error,
        "n_scored": scored,
        "pass_rate": (n_passed / scored) if scored else 0.0,
        "n_with_smells": n_with_smells,
        "smell_frequency": dict(freq.most_common()),
    }


def conversion_quality_report(items: Sequence[QualityItem]) -> Dict[str, object]:
    return {
        "similarity": {
            "llm_vs_gold": llm_vs_gold_similarity(items),
            "human_human": human_human_similarity(items),
        },
        "paska": paska_summary(items),
    }
