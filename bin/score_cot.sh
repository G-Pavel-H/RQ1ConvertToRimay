#!/usr/bin/env bash
# Stage 2 — score the chain-of-thought run in the latest batch: outputs/runN/cot/.
# Extra args pass through, e.g.:  bin/score_cot.sh --gold path/to/gold.csv
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

run="$(latest_run)"
echo "Scoring cot <- outputs/$run/cot"
python scripts/run_scoring.py --run "$run/cot" "$@"
