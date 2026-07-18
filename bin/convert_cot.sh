#!/usr/bin/env bash
# Stage 1 — chain-of-thought conversion into the current run: outputs/runN/cot/.
# The current run is the highest runN/ (run1 if none). Start a fresh batch with
# bin/new_run.sh. Extra args pass through, e.g.:  bin/convert_cot.sh --n-samples 3
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

run="$(current_run)"
echo "Converting cot -> outputs/$run/cot"
python scripts/run_conversion.py --strategy cot --run-name "$run/cot" "$@"
