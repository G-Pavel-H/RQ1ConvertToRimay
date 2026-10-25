#!/usr/bin/env python3
"""Build the unit-anomaly viewer from the saved replies (nothing is re-run) and open it.

    python scripts/build_anomalies.py            # Experiment 3 -> outputs/anomalies.html
    python scripts/build_anomalies.py --exp 2    # Experiment 2 -> outputs/anomalies_exp2.html
"""
from __future__ import annotations

import argparse
import sys
import webbrowser
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from src import anomaly_view  # noqa: E402


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--exp", type=int, choices=(2, 3), default=3)
    p.add_argument("--no-open", action="store_true")
    args = p.parse_args(argv)
    out = anomaly_view.build(args.exp)
    print(f"Anomaly viewer: {out}")
    if not args.no_open:
        webbrowser.open(out.resolve().as_uri())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
