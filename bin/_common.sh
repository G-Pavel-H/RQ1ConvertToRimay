#!/usr/bin/env bash
# Shared bootstrap for the run/score scripts.
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

# --- run-batch helpers -------------------------------------------------------
# A "run" is a batch folder outputs/runN/ holding one subfolder per strategy
# (zsl/ fsl/ cot/), each self-contained with its own conversions/ + scoring/.

# _highest_run_n — echo the highest N among outputs/runN/ folders, or 0 if none.
_highest_run_n() {
  local best_n=0 d base
  shopt -s nullglob
  for d in "$REPO_ROOT"/outputs/run[0-9]*/; do
    base="$(basename "$d")"
    if [[ "$base" =~ ^run([0-9]+)$ ]]; then
      if (( BASH_REMATCH[1] > best_n )); then
        best_n="${BASH_REMATCH[1]}"
      fi
    fi
  done
  shopt -u nullglob
  printf '%d\n' "$best_n"
}

# current_run — the run a convert script writes into: the highest existing
# runN, or run1 if none exists yet. Echoes the folder name (e.g. "run1").
current_run() {
  local n; n="$(_highest_run_n)"
  (( n == 0 )) && n=1
  printf 'run%d\n' "$n"
}

# latest_run — the run a score script reads from: the highest existing runN.
# Exits non-zero with a helpful message if no run folder exists yet.
latest_run() {
  local n; n="$(_highest_run_n)"
  if (( n == 0 )); then
    echo "ERROR: no run folders under outputs/ — run a convert script first." >&2
    return 1
  fi
  printf 'run%d\n' "$n"
}

# new_run — create the next empty runN/ folder and echo its name.
new_run() {
  local n name; n="$(_highest_run_n)"; name="run$(( n + 1 ))"
  mkdir -p "$REPO_ROOT/outputs/$name"
  printf '%s\n' "$name"
}
