"""Inter-annotator agreement — Fleiss' and Cohen's Kappa, plus raw agreement.

A faithful port of the annotation tool's formulas
(AnnotationToolForRimay/backend/src/utils/agreement.js and its Python twin
analysis/pilot_agreement.py), so numbers computed here on the curated data are
directly comparable to the ones the tool shows for the original annotations.
Cross-checked against pilot_agreement.py on the same CSV (see
scripts/agreement_annotations.py --verify).

Input everywhere is ``subjects``: ``{subject: {rater: category}}``.
"""
from __future__ import annotations

from collections import Counter
from itertools import combinations
from typing import Dict, List, Optional, Sequence

Subjects = Dict[str, Dict[str, str]]


def fleiss_kappa(matrix: Sequence[Sequence[float]]) -> Optional[float]:
    """Fleiss' Kappa from a subject x category count matrix.

    Subjects with fewer than two ratings are dropped. ``None`` when undefined:
    no subject has two ratings, or one category was used throughout (so the
    expected agreement is already 1).
    """
    rows = [r for r in matrix if sum(r) >= 2]
    if not rows:
        return None
    n_i = [sum(r) for r in rows]
    total = sum(n_i)
    n_cat = len(rows[0])
    p_j = [sum(r[j] for r in rows) / total for j in range(n_cat)]
    p_i = [(sum(c * c for c in r) - n) / (n * (n - 1)) for r, n in zip(rows, n_i)]
    p_bar = sum(p_i) / len(p_i)
    p_e = sum(p * p for p in p_j)
    if 1 - p_e <= 1e-12:
        return None
    return (p_bar - p_e) / (1 - p_e)


def cohen_kappa(pairs: Sequence[tuple], categories: Sequence[str]) -> Optional[float]:
    """Cohen's Kappa for one pair of raters over the subjects both rated."""
    if not pairs:
        return None
    n = len(pairs)
    observed = sum(1 for a, b in pairs if a == b) / n
    ca, cb = Counter(a for a, _ in pairs), Counter(b for _, b in pairs)
    expected = sum((ca[c] / n) * (cb[c] / n) for c in categories)
    if 1 - expected <= 1e-12:
        return None
    return (observed - expected) / (1 - expected)


def landis_koch(kappa: Optional[float]) -> str:
    if kappa is None:
        return "undefined (no category variation)"
    if kappa < 0:
        return "poor"
    if kappa <= 0.20:
        return "slight"
    if kappa <= 0.40:
        return "fair"
    if kappa <= 0.60:
        return "moderate"
    if kappa <= 0.80:
        return "substantial"
    return "almost perfect"


def analyze(subjects: Subjects, categories: Sequence[str]) -> dict:
    """Everything reported for one field, matching the tool's Agreement tab."""
    subjects = {s: {r: v for r, v in rv.items() if v in categories} for s, rv in subjects.items()}
    subjects = {s: rv for s, rv in subjects.items() if rv}

    matrix = [[sum(1 for v in rv.values() if v == c) for c in categories] for rv in subjects.values()]
    kappa = fleiss_kappa(matrix)

    unanimous = majority = used = 0
    for rv in subjects.values():
        n = len(rv)
        if n < 2:
            continue
        used += 1
        top = max(Counter(rv.values()).values())
        unanimous += top == n
        majority += top >= n - 1

    raters = sorted({r for rv in subjects.values() for r in rv})
    pairs = []
    for a, b in combinations(raters, 2):
        shared = [(rv[a], rv[b]) for rv in subjects.values() if a in rv and b in rv]
        k = cohen_kappa(shared, categories)
        pairs.append({
            "a": a, "b": b, "n": len(shared), "kappa": k, "band": landis_koch(k),
            "observed": (sum(1 for x, y in shared if x == y) / len(shared)) if shared else None,
        })
    defined = [p["kappa"] for p in pairs if p["kappa"] is not None]

    return {
        "kappa": kappa,
        "band": landis_koch(kappa),
        "cohenMean": (sum(defined) / len(defined)) if defined else None,
        "cohenPairs": pairs,
        "unanimous": (unanimous / used) if used else None,
        "majority": (majority / used) if used else None,
        "nSubjects": used,
        "distribution": dict(Counter(v for rv in subjects.values() for v in rv.values())),
    }
