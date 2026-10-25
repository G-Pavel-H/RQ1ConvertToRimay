#!/usr/bin/env python3
"""Build ``outputs/dashboard.html`` from every scored run (and open it).

    python scripts/build_dashboard.py            # build
    python scripts/build_dashboard.py --open     # build and open in the browser

Scoring a run rebuilds it automatically; run this after a verdict, or to view.
"""
from __future__ import annotations

import argparse
import sys
import webbrowser
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from src import anomaly_view, dashboard  # noqa: E402


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--out", type=Path, default=None, help="Default: outputs/dashboard.html")
    p.add_argument("--open", action="store_true")
    args = p.parse_args(argv)
    out = dashboard.build(args.out)
    print(f"Dashboard: {out}")
    print(f"Anomaly viewer: {anomaly_view.build(3)}")
    if args.open:
        webbrowser.open(out.resolve().as_uri())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
