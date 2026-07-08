"""Track 2 — conversion quality (pure functions, no IO).

Two lenses on the LLM's Rimay, implemented after the Stage 1 schema
checkpoint:

* Similarity to gold. Reference = ``canonicalRimay``. One swappable
  function ``conversion_similarity(a, b) -> float``. v0 is a
  deliberate, dependency-light placeholder for a future structural /
  semantic metric: difflib SequenceMatcher ratio on normalized text
  (lowercase, whitespace-collapsed, placeholders stripped) plus a
  token Jaccard — both reported.
* Human baseline (the ceiling). Pairwise similarities among the human
  ``rimayText`` conversions with the same function, aggregated into a
  human-human distribution reported beside the LLM-vs-gold
  distribution per strategy.
* Paska validation as a fidelity signal. From the manifest:
  ``paska_passed`` rate per strategy and per-smell-type frequencies —
  the independent structural check on conversion fidelity.
"""
from __future__ import annotations

raise NotImplementedError(
    "Track 2 scoring is built after the Stage 1 manifest schema is "
    "confirmed at the checkpoint (build order step 5)."
)
