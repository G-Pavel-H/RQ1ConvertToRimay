"""Build the HTML results report over every scored run.

Collects each run's ``scoring/results.json`` and writes a single
self-contained page (``outputs/report.html``) that shows the metrics,
the Paska validation, and the per-requirement detail for every run —
open it in a browser. Rendering logic lives in ``src/report.py``; this
script does IO only.

Usage:
    python scripts/build_report.py [--outputs outputs] [--out outputs/report.html]
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

THIS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(THIS_DIR.parent))

from src import config  # noqa: E402
from src.report import collect_results, render_report  # noqa: E402


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description="Build the HTML results report")
    p.add_argument(
        "--outputs",
        default=str(config.OUTPUTS_DIR),
        help="Directory to scan for scored runs. Default: outputs/",
    )
    p.add_argument(
        "--out",
        default=str(config.REPORT_HTML),
        help="Where to write the report. Default: outputs/report.html",
    )
    args = p.parse_args(argv)

    results = collect_results(Path(args.outputs))
    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(render_report(results), encoding="utf-8")

    if results:
        for r in results:
            print(f"  {r['run']:<24} {r['counts']['evaluated']} reqs")
    else:
        print("  no scored runs found — run scripts/run_scoring.py first")
    print(f"Report ({len(results)} runs): {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
