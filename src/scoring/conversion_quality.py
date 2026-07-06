"""Track 2 — conversion quality (pure functions, no IO).

Three lenses on the LLM's Rimay:

* **Similarity to gold.** Reference = ``canonicalRimay``. The metric
  sits behind one swappable function ``conversion_similarity(a, b)``.

  v0 IS A DELIBERATE PLACEHOLDER: dependency-light surface measures —
  difflib ``SequenceMatcher`` ratio on normalized text (lowercase,
  whitespace-collapsed, ``<MISSING_*>``/``<NON_ATOMIC>`` stripped) and
  a token Jaccard — chosen so the harness, reports and baseline
  plumbing can be validated end to end. Neither measure understands
  Rimay structure or meaning; a future structural/semantic metric
  (slot-aligned comparison, embedding similarity, …) replaces
  ``conversion_similarity`` without touching anything else.

* **Human baseline (the ceiling).** Pairwise similarities among the
  human ``rimayText`` conversions of the same requirement, with the
  same functions, aggregated into a human-human distribution. The
  LLM-vs-gold number is only interpretable against how much humans
  vary among themselves.

* **Paska validation as a fidelity signal.** From the Stage 1 manifest:
  ``paska_passed`` rate and per-smell-type frequencies — the
  independent structural check on conversion fidelity.
"""
from __future__ import annotations

import re
import statistics
from dataclasses import dataclass, field
from difflib import SequenceMatcher
from itertools import combinations
from typing import Callable, Dict, List, Optional, Sequence

_PLACEHOLDER_RE = re.compile(r"<MISSING_[A-Z_]+>|<NON_ATOMIC>")
_WHITESPACE_RE = re.compile(r"\s+")
_TOKEN_RE = re.compile(r"[a-z0-9_]+")

SimilarityFn = Callable[[str, str], float]


def normalize_rimay(text: str) -> str:
    """Lowercase, strip ``<MISSING_*>``/``<NON_ATOMIC>``, collapse whitespace.

    Applied to both sides before any similarity measure so that
    placeholder tokens and formatting never inflate or deflate the
    score.
    """
    s = _PLACEHOLDER_RE.sub(" ", text)
    s = s.lower()
    s = _WHITESPACE_RE.sub(" ", s)
    return s.strip()


def conversion_similarity(a: str, b: str) -> float:
    """THE swappable similarity metric (v0 placeholder, see module docstring).

    difflib SequenceMatcher ratio on normalized text, in [0, 1].
    ``ratio()`` is order-dependent, so we average both directions — the
    human-human baseline compares unordered pairs and needs symmetry.
    """
    na, nb = normalize_rimay(a), normalize_rimay(b)
    if not na and not nb:
        return 1.0
    return (
        SequenceMatcher(None, na, nb).ratio()
        + SequenceMatcher(None, nb, na).ratio()
    ) / 2


def token_jaccard(a: str, b: str) -> float:
    """Jaccard overlap of the normalized token sets, in [0, 1]."""
    ta = set(_TOKEN_RE.findall(normalize_rimay(a)))
    tb = set(_TOKEN_RE.findall(normalize_rimay(b)))
    if not ta and not tb:
        return 1.0
    union = ta | tb
    return len(ta & tb) / len(union)


def pairwise_similarities(
    texts: Sequence[str], similarity_fn: SimilarityFn = conversion_similarity
) -> List[float]:
    """All C(n,2) pairwise similarities among ``texts`` (empty texts dropped).

    Used for the human-human baseline within one requirement.
    """
    usable = [t for t in texts if t and t.strip()]
    return [similarity_fn(a, b) for a, b in combinations(usable, 2)]


@dataclass
class Distribution:
    """Summary statistics of a set of similarity values."""

    values: List[float] = field(default_factory=list)

    @property
    def n(self) -> int:
        return len(self.values)

    @property
    def mean(self) -> Optional[float]:
        return statistics.fmean(self.values) if self.values else None

    @property
    def median(self) -> Optional[float]:
        return statistics.median(self.values) if self.values else None

    @property
    def stdev(self) -> Optional[float]:
        return statistics.stdev(self.values) if len(self.values) > 1 else None

    @property
    def min(self) -> Optional[float]:
        return min(self.values) if self.values else None

    @property
    def max(self) -> Optional[float]:
        return max(self.values) if self.values else None


@dataclass
class PaskaSummary:
    """Paska pass/smell aggregation over the evaluated manifest rows."""

    n_total: int = 0
    n_passed: int = 0
    n_failed: int = 0
    n_errors: int = 0  # paska_passed was null (Paska crashed) — excluded from the rate
    smell_frequencies: Dict[str, int] = field(default_factory=dict)

    @property
    def pass_rate(self) -> Optional[float]:
        scored = self.n_passed + self.n_failed
        return self.n_passed / scored if scored else None


def summarize_paska(manifest_rows: Sequence[dict]) -> PaskaSummary:
    """Aggregate ``paska_passed`` / ``paska_smells`` from Stage 1 manifest rows."""
    summary = PaskaSummary()
    for row in manifest_rows:
        summary.n_total += 1
        passed = row.get("paska_passed")
        if passed is None:
            summary.n_errors += 1
            continue
        if passed:
            summary.n_passed += 1
        else:
            summary.n_failed += 1
        for smell in row.get("paska_smells") or []:
            summary.smell_frequencies[smell] = summary.smell_frequencies.get(smell, 0) + 1
    return summary
