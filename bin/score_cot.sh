#!/usr/bin/env bash
# Stage 2 — score the chain-of-thought run in outputs/<run-folder>/cot/.
# The run folder is named explicitly; extra args pass through, e.g.:
#   bin/score_cot.sh run2 --gold path/to/gold.csv
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

require_run_name "${1:-}"
run="$1"; shift

echo "Scoring cot <- outputs/$run/cot"
python scripts/run_scoring.py --run "$run/cot" "$@"
