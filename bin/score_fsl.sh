#!/usr/bin/env bash
# Stage 2 — score the few-shot run in outputs/<run-folder>/fsl/.
# The run folder is named explicitly; extra args pass through, e.g.:
#   bin/score_fsl.sh run2 --gold path/to/gold.csv
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

require_run_name "${1:-}"
run="$1"; shift

echo "Scoring fsl <- outputs/$run/fsl"
python scripts/run_scoring.py --run "$run/fsl" "$@"
