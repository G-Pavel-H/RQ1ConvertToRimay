#!/usr/bin/env python3
"""Run Paska over the human annotations — the pipeline step behind "Run Paska".

Reads ``data/curation.json``, runs every annotator unit through Paska with the
same preprocessing as the LLM output (placeholders stripped; Paska's verdict is
"valid" when it fires no smell), and writes
``data/curated/paska_annotations.json``. Only texts that changed since the last
run are re-checked.

    python scripts/paska_annotations.py

A unit passes when the annotator's verdict and Paska's coincide — complete and
valid Rimay, or incomplete and rejected — and fails when they differ.
"""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from src import curation  # noqa: E402


def main() -> int:
    if not curation.STATE_PATH.exists():
        print("No data/curation.json yet — run scripts/build_curation.py first.", file=sys.stderr)
        return 1
    out = curation.run_paska_on_units(curation.load_state())

    def pct(x):
        return "  —  " if x is None else f"{x:5.0%}"

    print()
    print("pass = the annotator's verdict and Paska's coincide")
    print(f"{'group':12} {'pass':>6} {'n':>5} | {'complete+valid':>14} {'incompl+rejected':>16} | {'complete BUT rejected':>21} {'incompl BUT accepted':>20}")
    order = [g for g in out["summary"] if not g.startswith("set:") and g != "all"]
    order += sorted(g for g in out["summary"] if g.startswith("set:")) + ["all"]
    for g in order:
        c = out["summary"][g]
        print(f"{g:12} {pct(c['agreement']):>6} {c['n']:>5} | {c['agree_complete']:>14} {c['agree_incomplete']:>16} | "
              f"{c['differ_complete']:>21} {c['differ_incomplete']:>20}")
    print(f"\nWrote {curation.PASKA_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
