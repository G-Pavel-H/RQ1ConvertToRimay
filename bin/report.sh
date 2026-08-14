#!/usr/bin/env bash
# Build the HTML results report over every scored run and open it.
# Takes no run folder: the report always covers all of outputs/.
#   bin/report.sh
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

python scripts/build_report.py "$@"

report="$REPO_ROOT/outputs/report.html"
if [[ -f "$report" ]] && command -v open >/dev/null 2>&1; then
  open "$report"
fi
