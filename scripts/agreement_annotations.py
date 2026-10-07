#!/usr/bin/env python3
"""Inter-annotator agreement on the curated annotations — before vs after.

    python scripts/agreement_annotations.py            # compute + print
    python scripts/agreement_annotations.py --verify   # also cross-check the
                                                       # Kappa port against the
                                                       # annotation tool's own code

Reads ``data/curation.json`` and writes ``data/curated/agreement.json`` (also
shown by the "Agreement" button in scripts/curate.py). Per view: Fleiss' Kappa,
mean Cohen's Kappa, raw agreement for the five slots, overallIncomplete and
conditionType; and human~human conversion similarity — seq_ratio, jaccard, and
embedding cosine (needs Ollama running; otherwise cosine is left out).
"""
from __future__ import annotations

import argparse
import importlib.util
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from src import curation  # noqa: E402

TOOL_AGREEMENT = ROOT.parent / "AnnotationToolForRimay" / "analysis" / "pilot_agreement.py"


def fmt(x, pct=False):
    if x is None:
        return "  n/a"
    return f"{x:5.0%}" if pct else f"{x:5.3f}"


def verify() -> bool:
    """Our port must give the tool's numbers on the tool's own input."""
    import pandas as pd
    from src.agreement import analyze

    if not TOOL_AGREEMENT.exists():
        print(f"(skip verify: {TOOL_AGREEMENT} not found)")
        return True
    spec = importlib.util.spec_from_file_location("pilot_agreement", TOOL_AGREEMENT)
    tool = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(tool)

    csv_path = curation.config.DATA_DIR / "gold_annotations_main_atomic.csv"
    df = pd.read_csv(csv_path, dtype=str, keep_default_na=False)
    ok = True
    print(f"\nVerify against the annotation tool's pilot_agreement.py on {csv_path.name}:")
    for label, (column, allowed) in {**tool.SLOT_FIELDS, **tool.EXTRA_FIELDS}.items():
        theirs = tool.analyze_field(df, label, column, allowed)
        subjects = {}
        for _, r in df.iterrows():
            subjects.setdefault(r[tool.SUBJECT_COL], {})[r[tool.RATER_COL]] = str(r[column]).strip().lower()
        ours = analyze(subjects, sorted(allowed))
        tk = None if theirs is None or theirs["kappa"] != theirs["kappa"] else theirs["kappa"]  # NaN -> None
        same = (tk is None and ours["kappa"] is None) or (tk is not None and ours["kappa"] is not None
                                                            and abs(tk - ours["kappa"]) < 1e-12)
        ok &= same
        print(f"  {label:18} tool={fmt(tk)}  ours={fmt(ours['kappa'])}  {'✓' if same else '✗ MISMATCH'}")
    return ok


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--verify", action="store_true")
    args = p.parse_args(argv)

    if args.verify and not verify():
        print("Kappa port disagrees with the annotation tool — not reporting.", file=sys.stderr)
        return 1

    out = curation.agreement_report(curation.load_state())
    print(f"\nEmbedding cosine: {'model ' + out['embedding']['model'] if out['embedding']['available'] else 'UNAVAILABLE (start Ollama)'}")
    print(f"Units left out as not annotator-authored: {out['excludedUnits'] or 'none'}")
    for v in out["views"]:
        print(f"\n=== {v['label']} — {v['nRequirements']} requirements, {v['nSubjects']} subjects")
        print(f"  {'field':18} {'Fleiss':>7} {'Cohen':>7} {'unanim':>7} {'n':>4}  band")
        for f in v["fields"]:
            print(f"  {f['field']:18} {fmt(f['kappa']):>7} {fmt(f['cohenMean']):>7} "
                  f"{fmt(f['unanimous'], True):>7} {f['nSubjects']:>4}  {f['band']}")
        s = v["similarity"]
        cos = f"{s['cosine']['mean']:.3f}" if s["cosine"] else "n/a"
        print(f"  human~human ({s['nPairs']} pairs): cosine {cos} · seq_ratio {s['seq_ratio']['mean']:.3f} "
              f"· jaccard {s['jaccard']['mean']:.3f}")
    print(f"\nWrote {curation.AGREEMENT_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
