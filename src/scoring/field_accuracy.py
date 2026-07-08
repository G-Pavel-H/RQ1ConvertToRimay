"""Track 1 — field accuracy (pure functions, no IO).

Compares the LLM's binary per-slot signal (missing / filled, derived
from ``<MISSING_*>`` placeholders) against the adjudicated gold slot
labels (present / implied / missing).

Planned API (implemented after the Stage 1 schema checkpoint):

* Primary — binary collapse. Gold ``present`` and ``implied`` collapse
  to ``not-missing``; ``missing`` stays. Rationale: the LLM only
  signals missing-or-not, it cannot distinguish present from implied.
  Per slot (scope, condition, actor, modalVerb, action): confusion of
  LLM-missing vs gold-missing, then precision / recall / F1 for the
  "missing" class, plus micro and macro averages.
* Secondary lenses:
  - among gold ``implied`` slots: LLM filled (inferred context) vs
    flagged missing (over-flag);
  - among gold ``missing`` slots: LLM flagged (correct) vs silently
    filled (possible compensation / hallucination).
* Overall verdict. LLM overall-incomplete = any mandatory slot (actor,
  modalVerb, action) flagged missing. Compared to
  ``gold_overallIncomplete``: agreement rate + 2x2 confusion.
"""
from __future__ import annotations

raise NotImplementedError(
    "Track 1 scoring is built after the Stage 1 manifest schema is "
    "confirmed at the checkpoint (build order step 5)."
)
