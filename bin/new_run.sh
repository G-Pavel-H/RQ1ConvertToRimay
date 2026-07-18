#!/usr/bin/env bash
# Start a fresh batch: create the next empty outputs/runN/ folder. After this,
# the convert scripts (convert_zsl/fsl/cot.sh) write into this new run.
source "$(dirname "${BASH_SOURCE[0]}")/_common.sh"

run="$(new_run)"
echo "Started fresh batch: outputs/$run/"
echo "Convert scripts (bin/convert_*.sh) will now write into outputs/$run/"
