#!/usr/bin/env bash
# Stage 2 — score the few-shot run in the latest batch: outputs/runN/fsl/.
# Extra args pass through, e.g.:  bin/score_fsl.sh --gold path/to/gold.csv
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

run="$(latest_run)"
echo "Scoring fsl <- outputs/$run/fsl"
python scripts/run_scoring.py --run "$run/fsl" "$@"
