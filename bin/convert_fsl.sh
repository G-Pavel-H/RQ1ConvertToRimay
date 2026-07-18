#!/usr/bin/env bash
# Stage 1 — few-shot conversion (3 exemplars) into the current run: outputs/runN/fsl/.
# The current run is the highest runN/ (run1 if none). Start a fresh batch with
# bin/new_run.sh. Override exemplars or pass extra args, e.g.:
#   bin/convert_fsl.sh --n-fsl-examples 2
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

run="$(current_run)"
echo "Converting fsl -> outputs/$run/fsl"
python scripts/run_conversion.py --strategy fsl --n-fsl-examples 3 --run-name "$run/fsl" "$@"
