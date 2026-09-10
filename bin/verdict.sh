#!/usr/bin/env bash
# Stage 3 — LLM analysis of a batch's scoring results.
# The batch folder is named explicitly; extra args pass through, e.g.:
#   bin/verdict.sh main30 --model claude-opus-5
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

require_run_name "${1:-}"
batch="$1"; shift

echo "Analysing outputs/$batch"
python scripts/run_verdict.py --batch "$batch" "$@"
