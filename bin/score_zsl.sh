#!/usr/bin/env bash
# Stage 2 — score the zero-shot run in outputs/<run-folder>/zsl/.
# The run folder is named explicitly; extra args pass through, e.g.:
#   bin/score_zsl.sh run2 --gold path/to/gold.csv
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

require_run_name "${1:-}"
run="$1"; shift

echo "Scoring zsl <- outputs/$run/zsl"
python scripts/run_scoring.py --run "$run/zsl" "$@"
