#!/bin/bash
# One-off: regenerate all 9 batch verdicts after the cosine re-score, then rebuild the dashboard.
cd "$(dirname "$0")/../.."
LOG=outputs/_logs/experiments.log; PY=.venv/bin/python; export ANTHROPIC_API_KEY=
say() { echo "[$(date '+%F %H:%M:%S')] $*" >> "$LOG"; }
say "=== verdict regeneration after cosine re-score ==="
for b in exp1_atomic exp2_units exp3_blind; do for m in haiku sonnet opus; do
  B="${b}_$m"
  for attempt in 1 2 3 4 5 6 7 8 9 10 11 12; do
    say "$B VERDICT (attempt $attempt)"
    if $PY scripts/run_verdict.py --batch "$B" --model claude-opus-5-5 --backend subscription >> "$LOG" 2>&1; then say "$B VERDICT OK"; break; fi
    say "$B VERDICT failed; sleeping 20m"; sleep 1200
  done
done; done
$PY scripts/build_dashboard.py >> "$LOG" 2>&1 && say "DASHBOARD OK"
say "=== verdicts DONE ==="
