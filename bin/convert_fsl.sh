#!/usr/bin/env bash
# Stage 1 — few-shot conversion (3 exemplars) into outputs/<run-folder>/fsl/.
# The run folder is named explicitly; extra args pass through, e.g.:
#   bin/convert_fsl.sh run2 --n-fsl-examples 2
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

require_run_name "${1:-}"
run="$1"; shift

echo "Converting fsl -> outputs/$run/fsl"
python scripts/run_conversion.py --strategy fsl --n-fsl-examples 3 --run-name "$run/fsl" "$@"
