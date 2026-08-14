#!/usr/bin/env bash
# Shared bootstrap for the convert/score scripts.
# Resolves the repo root, activates the venv, and moves into the repo.
# Sourced by the other scripts — not meant to be run directly.

set -euo pipefail

# Repo root = parent of this bin/ directory (resolves symlinks).
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

VENV_ACTIVATE="$REPO_ROOT/.venv/bin/activate"
if [[ ! -f "$VENV_ACTIVATE" ]]; then
  echo "ERROR: virtualenv not found at $REPO_ROOT/.venv" >&2
  echo "       Create it with:  python -m venv .venv && source .venv/bin/activate && pip install -r requirements.txt" >&2
  exit 1
fi
# shellcheck disable=SC1090
source "$VENV_ACTIVATE"

# --- run-batch helper --------------------------------------------------------
# A "run" is a batch folder outputs/<run>/ holding one subfolder per strategy
# (zsl/ fsl/ cot/), each self-contained with its own conversions/ + scoring/.
# The batch name is always given explicitly on the command line — nothing is
# inferred from what already exists on disk.

# require_run_name <arg> — validate the batch name given as the first argument;
# exit with usage if it is missing or malformed.
require_run_name() {
  local name="${1:-}"
  if [[ -z "$name" ]]; then
    echo "ERROR: missing run folder name." >&2
    echo "       usage: bin/$(basename "$0") <run-folder> [extra args]" >&2
    exit 2
  fi
  if [[ "$name" == -* || "$name" == */* ]]; then
    echo "ERROR: invalid run folder name '$name' (no slashes, no leading dash)." >&2
    exit 2
  fi
}
