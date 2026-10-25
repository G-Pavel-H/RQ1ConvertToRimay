#!/usr/bin/env bash
# Build the Experiment 3 unit-anomaly viewer from the saved replies and open it.
#   bin/anomalies.sh            (--exp 2 for Experiment 2)
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

python scripts/build_anomalies.py "$@"
