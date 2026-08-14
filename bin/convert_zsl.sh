#!/usr/bin/env bash
# Stage 1 — zero-shot conversion into outputs/<run-folder>/zsl/.
# The run folder is named explicitly; extra args pass through, e.g.:
#   bin/convert_zsl.sh run2 --n-samples 3
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

require_run_name "${1:-}"
run="$1"; shift

echo "Converting zsl -> outputs/$run/zsl"
python scripts/run_conversion.py --strategy zsl --run-name "$run/zsl" "$@"
