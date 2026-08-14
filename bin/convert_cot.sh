#!/usr/bin/env bash
# Stage 1 — chain-of-thought conversion into outputs/<run-folder>/cot/.
# The run folder is named explicitly; extra args pass through, e.g.:
#   bin/convert_cot.sh run2 --n-samples 3
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

require_run_name "${1:-}"
run="$1"; shift

echo "Converting cot -> outputs/$run/cot"
python scripts/run_conversion.py --strategy cot --run-name "$run/cot" "$@"
