"""Stage 2 entry point: offline scoring against the human gold standard.

Reads ``outputs/conversions/<strategy>.jsonl`` (Stage 1 manifest) plus
``data/gold_annotations.csv`` and computes:

* Track 1 — field accuracy (src/scoring/field_accuracy.py)
* Track 2 — conversion quality (src/scoring/conversion_quality.py)

Writes ``outputs/scoring/<strategy>/metrics.md``,
``outputs/scoring/<strategy>/per_requirement.md`` and
``outputs/scoring/comparison.csv``. Never touches MLflow.

Usage:
  python scripts/run_scoring.py --strategy zsl --gold data/gold_annotations.csv
      [--fsl-example-ids id1,id2] [--out outputs/scoring]
"""
from __future__ import annotations

import sys

raise SystemExit(
    "run_scoring.py is built after the Stage 1 manifest schema is "
    "confirmed at the checkpoint (build order step 5)."
)
