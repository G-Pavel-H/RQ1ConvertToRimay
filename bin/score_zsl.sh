#!/usr/bin/env bash
# Stage 2 — score the zero-shot run in the latest batch: outputs/runN/zsl/.
# Extra args pass through, e.g.:  bin/score_zsl.sh --gold path/to/gold.csv
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

run="$(latest_run)"
echo "Scoring zsl <- outputs/$run/zsl"
python scripts/run_scoring.py --run "$run/zsl" "$@"
