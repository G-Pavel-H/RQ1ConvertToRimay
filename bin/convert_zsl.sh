#!/usr/bin/env bash
# Stage 1 — zero-shot conversion into the current run: outputs/runN/zsl/.
# The current run is the highest runN/ (run1 if none). Start a fresh batch with
# bin/new_run.sh. Extra args pass through, e.g.:  bin/convert_zsl.sh --n-samples 3
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

run="$(current_run)"
echo "Converting zsl -> outputs/$run/zsl"
python scripts/run_conversion.py --strategy zsl --run-name "$run/zsl" "$@"
