#!/usr/bin/env python3
"""Run Paska over the human annotations — the pipeline step behind "Run Paska".

Reads ``data/curation.json``, checks every annotator unit with Paska using the
same preprocessing and pass rule as the LLM output (placeholders stripped; pass
= no smells), and writes ``data/curated/paska_annotations.json``. Only texts that
changed since the last run are re-checked.

    python scripts/paska_annotations.py

The headline is the pass rate over units the annotator judged **complete**;
units with a mandatory slot missing are fragments once stripped and fail by
construction, so they are counted separately.
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
    print(f"{'group':14} {'pass·complete':>13} {'n':>6} {'incomplete f/p':>15} {'no text':>8}")
    order = [g for g in out["summary"] if not g.startswith("set:") and g != "all"]
    order += sorted(g for g in out["summary"] if g.startswith("set:")) + ["all"]
    for g in order:
        c = out["summary"][g]
        n = c.get("complete_pass", 0) + c.get("complete_fail", 0)
        inc = f"{c.get('incomplete_fail', 0)}/{c.get('incomplete_pass', 0)}"
        print(f"{g:14} {pct(c['pass_rate_complete']):>13} {n:>6} {inc:>15} {c.get('skipped', 0):>8}")
    print(f"\nWrote {curation.PASKA_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
