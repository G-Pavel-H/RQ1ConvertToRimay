"""Track 2 — conversion quality. Pure functions, no IO.

There is **no single gold conversion**: a requirement has as many valid
Rimay conversions as it has annotators, so the LLM is compared against the
annotators themselves, never against one canonical reference. Two lenses:

* **LLM vs annotators.** Every (LLM, annotator) pair for every
  requirement. The metric is isolated behind one swappable function,
  :func:`conversion_similarity`. v0 is a deliberate, dependency-light
  *placeholder*: difflib's ``SequenceMatcher`` ratio on normalised text
  plus a token Jaccard. Both are reported. This is NOT the final metric —
  it is a stand-in for a future structural / semantic measure; swap
  :func:`conversion_similarity` (and, if needed, :func:`similarity_pair`)
  when that lands.

* **Human baseline (the ceiling).** The LLM number is only interpretable
  against how much the annotators vary among themselves, so we compute the
  pairwise human-human similarity distribution with the *same* function
  over the *same* texts and report it side by side.

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
from src.scoring import embeddings

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


def cosine_similarity(a: str, b: str) -> Optional[float]:
    """Semantic similarity: cosine between sentence embeddings (0..1-ish).

    The lexical measures above cannot tell that two differently-worded
    conversions mean the same thing, and Rimay's boilerplate gives them a high
    floor — two unrelated requirements score ~0.35 on ``seq_ratio`` from shared
    scaffolding alone. This sees meaning instead.

    Returns ``None`` when the embedding service is unavailable, so the metric
    drops out of the report rather than breaking the run. Empty/placeholder-only
    text follows the same convention as the lexical metrics: both empty is a
    perfect match, one empty is no match.
    """
    na, nb = normalize_text(a), normalize_text(b)
    if not na and not nb:
        return 1.0
    if not na or not nb:
        return 0.0
    va, vb = embeddings.embed(na), embeddings.embed(nb)
    if va is None or vb is None:
        return None
    return embeddings.cosine(va, vb)


def conversion_similarity(a: str, b: str) -> float:
    """The single swappable primary similarity (v0 = SequenceMatcher ratio).

    Placeholder for a future structural / semantic metric — swap the body
    here and every caller (LLM-vs-gold and human-human) updates together.
    """
    return seq_ratio(a, b)


def similarity_pair(a: str, b: str) -> Dict[str, Optional[float]]:
    """Every measure for one pair, for side-by-side reporting.

    Both similarity views are built from this one function, so adding a measure
    here surfaces it in the LLM-vs-annotator and human-human distributions at
    the same time and on the same texts.
    """
    return {
        "seq_ratio": seq_ratio(a, b),
        "jaccard": token_jaccard(a, b),
        "cosine": cosine_similarity(a, b),
    }


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


def _non_blank(texts: Sequence[str]) -> List[str]:
    return [t for t in texts if (t or "").strip()]


class QualityItem:
    """One requirement's Track-2 inputs."""

    __slots__ = ("req_id", "llm_rimay", "human_rimays",
                 "paska_passed", "paska_smells", "llm_incomplete")

    def __init__(
        self,
        req_id: str,
        llm_rimay: str,
        human_rimays: Sequence[str],
        paska_passed: Optional[bool],
        paska_smells: Sequence[dict],
        llm_incomplete: Optional[bool] = None,
    ) -> None:
        self.req_id = req_id
        self.llm_rimay = llm_rimay
        self.human_rimays = list(human_rimays)
        self.paska_passed = paska_passed
        self.paska_smells = list(paska_smells)
        # The LLM's own verdict, needed to read the Paska result correctly —
        # see paska_summary.
        self.llm_incomplete = llm_incomplete


def similarity_to_humans(llm_rimay: str, human_rimays: Sequence[str]) -> Dict[str, float]:
    """One requirement's LLM-vs-annotator similarity, summarised.

    ``mean`` is how close the LLM is to the annotators in general;
    ``max`` is how close it got to the annotator it agreed with most.
    """
    rimays = _non_blank(human_rimays)
    if not rimays:
        return {"n_humans": 0, "seq_ratio_mean": 0.0, "seq_ratio_max": 0.0,
                "jaccard_mean": 0.0, "jaccard_max": 0.0,
                "cosine_mean": None, "cosine_max": None}
    pairs = [similarity_pair(llm_rimay, h) for h in rimays]
    seq = [p["seq_ratio"] for p in pairs]
    jac = [p["jaccard"] for p in pairs]
    cos = [p["cosine"] for p in pairs if p["cosine"] is not None]
    return {
        "n_humans": len(rimays),
        "seq_ratio_mean": statistics.fmean(seq),
        "seq_ratio_max": max(seq),
        "jaccard_mean": statistics.fmean(jac),
        "jaccard_max": max(jac),
        "cosine_mean": statistics.fmean(cos) if cos else None,
        "cosine_max": max(cos) if cos else None,
    }


def llm_vs_human_similarity(items: Sequence[QualityItem]) -> Dict[str, object]:
    """Distribution over every (LLM, annotator) pair; skips blank conversions."""
    seq_vals: List[float] = []
    jac_vals: List[float] = []
    cos_vals: List[float] = []
    evaluated: List[str] = []
    skipped: List[str] = []
    for it in items:
        rimays = _non_blank(it.human_rimays)
        if not rimays:
            skipped.append(it.req_id)
            continue
        evaluated.append(it.req_id)
        for human in rimays:
            pair = similarity_pair(it.llm_rimay, human)
            seq_vals.append(pair["seq_ratio"])
            jac_vals.append(pair["jaccard"])
            if pair["cosine"] is not None:
                cos_vals.append(pair["cosine"])
    return {
        "seq_ratio": _distribution(seq_vals),
        "jaccard": _distribution(jac_vals),
        "cosine": _distribution(cos_vals) if cos_vals else None,
        "n_pairs": len(seq_vals),
        "n_evaluated": len(evaluated),
        "n_skipped_no_humans": len(skipped),
        "skipped_req_ids": skipped,
    }


def human_human_similarity(items: Sequence[QualityItem]) -> Dict[str, object]:
    """Pairwise human-human similarity distribution (the ceiling)."""
    seq_vals: List[float] = []
    jac_vals: List[float] = []
    cos_vals: List[float] = []
    for it in items:
        for a, b in combinations(_non_blank(it.human_rimays), 2):
            pair = similarity_pair(a, b)
            seq_vals.append(pair["seq_ratio"])
            jac_vals.append(pair["jaccard"])
            if pair["cosine"] is not None:
                cos_vals.append(pair["cosine"])
    return {
        "seq_ratio": _distribution(seq_vals),
        "jaccard": _distribution(jac_vals),
        "cosine": _distribution(cos_vals) if cos_vals else None,
        "n_pairs": len(seq_vals),
    }


def _paska_rate(items: Sequence[QualityItem]) -> Dict[str, object]:
    """passed / failed / rate over one subset, ignoring Paska errors."""
    passed = sum(1 for it in items if it.paska_passed is True)
    failed = sum(1 for it in items if it.paska_passed is False)
    scored = passed + failed
    return {
        "n": len(items),
        "n_passed": passed,
        "n_failed": failed,
        "n_scored": scored,
        "pass_rate": (passed / scored) if scored else None,
    }


def paska_summary(items: Sequence[QualityItem]) -> Dict[str, object]:
    """Paska pass rate + smell-type frequencies over the evaluated set.

    Split by the LLM's own verdict, because an unsplit pass rate is misleading.
    When the LLM declares a requirement **incomplete** it emits ``<MISSING_*>``
    placeholders, and the stripped text that reaches Paska is then a fragment
    with no system response — so Paska rejects it almost by construction. That
    failure is the LLM correctly reporting an incomplete source, not a bad
    conversion.

    The meaningful number is therefore ``pass_rate_llm_complete``: of the
    conversions the LLM asserted were complete requirements, how many are
    structurally valid Rimay. That is the case Paska is actually adjudicating.
    """
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

    claimed_complete = [it for it in items if it.llm_incomplete is False]
    claimed_incomplete = [it for it in items if it.llm_incomplete is True]
    by_verdict = {
        "llm_complete": _paska_rate(claimed_complete),
        "llm_incomplete": _paska_rate(claimed_incomplete),
    }
    return {
        "n": n,
        "n_passed": n_passed,
        "n_failed": n_failed,
        "n_error": n_error,
        "n_scored": scored,
        "pass_rate": (n_passed / scored) if scored else 0.0,
        # Headline: validity of the conversions the LLM stood behind.
        "pass_rate_llm_complete": by_verdict["llm_complete"]["pass_rate"],
        "by_llm_verdict": by_verdict,
        "n_with_smells": n_with_smells,
        "smell_frequency": dict(freq.most_common()),
    }


def conversion_quality_report(items: Sequence[QualityItem]) -> Dict[str, object]:
    return {
        "similarity": {
            "llm_vs_human": llm_vs_human_similarity(items),
            "human_human": human_human_similarity(items),
        },
        "paska": paska_summary(items),
        "embedding": {
            "available": embeddings.available(),
            "model": embeddings.MODEL if embeddings.available() else None,
        },
    }
