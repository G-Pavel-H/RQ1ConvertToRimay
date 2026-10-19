#!/usr/bin/env bash
# Build the experiment dashboard over every scored run and open it.
#   bin/dashboard.sh
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

python scripts/build_dashboard.py --open "$@"
