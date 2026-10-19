#!/bin/bash
# Experiments 1 and 2 on the curated gold, unattended.
#   Exp 1 — atomic:  data/curated/gold_atomic.csv, normal conversion.
#   Exp 2 — units:   data/curated/gold_units.csv, the prompt states how many units.
# Each model × experiment is one batch: outputs/exp<k>_<task>_<model>/<strategy>.
# Waits for the subscription limit and retries throttled stages. Runs that are
# already scored are skipped, so it can be stopped and restarted at any time.
#   ./run_experiments.sh                 # all models
#   MODELS="haiku" ./run_experiments.sh  # a subset
cd "$(dirname "$0")"
LOG="outputs/_logs/experiments.log"; mkdir -p outputs/_logs
PY=.venv/bin/python
export ANTHROPIC_API_KEY=
model_id() { case "$1" in haiku) echo claude-haiku-4-5-20251001;; sonnet) echo claude-sonnet-5-5;; opus) echo claude-opus-5-5;; esac; }
MODELS=${MODELS:-"haiku sonnet opus"}
VERDICT_MODEL=claude-opus-5-5

say() { echo "[$(date '+%F %H:%M:%S')] $*" >> "$LOG"; }

probe() {
  $PY - <<'PROBE' 2>&1
import sys; sys.path.insert(0, ".")
from src import llm_backend
llm_backend.complete(system="Reply with OK.", prompt="OK", model="claude-haiku-4-5-20251001",
                     max_tokens=10, temperature=None, backend="subscription")
print("PROBE_OK")
PROBE
}
wait_available() {
  for i in $(seq 1 48); do
    probe | grep -q PROBE_OK && return 0
    say "subscription limited (probe $i); sleeping 20m"; sleep 1200
  done
  return 1
}

# Run a stage; on failure look only at THIS attempt's output for the limit message.
stage() {
  local label="$1"; shift
  for attempt in 1 2 3 4 5 6; do
    say "$label (attempt $attempt)"
    local tmp; tmp=$(mktemp)
    if "$@" > "$tmp" 2>&1; then cat "$tmp" >> "$LOG"; rm -f "$tmp"; say "$label OK"; return 0; fi
    cat "$tmp" >> "$LOG"
    if grep -qi "limit" "$tmp"; then say "$label hit a usage limit"; rm -f "$tmp"; wait_available
    else say "$label failed for another reason; sleeping 5m"; rm -f "$tmp"; sleep 300; fi
  done
  say "$label GAVE UP"; return 1
}

say "=== experiments starting: models [$MODELS] ==="
wait_available || { say "subscription never became available"; exit 1; }
done_run() { [ -f "outputs/$1/scoring/results.json" ]; }
for m in $MODELS; do
  M=$(model_id "$m")
  B1="exp1_atomic_$m"; B2="exp2_units_$m"
  for s in zsl fsl cot; do
    done_run "$B1/$s" && { say "$B1/$s already scored — skip"; continue; }
    stage "$B1 CONVERT $s" $PY scripts/run_conversion.py --strategy "$s" --run-name "$B1/$s" --gold data/curated/gold_atomic.csv --model "$M"
    stage "$B1 SCORE $s"   $PY scripts/run_scoring.py --run "$B1/$s" --no-dashboard
  done
  for s in zsl fsl cot; do
    done_run "$B2/$s" && { say "$B2/$s already scored — skip"; continue; }
    stage "$B2 CONVERT $s" $PY scripts/run_conversion.py --units --strategy "$s" --run-name "$B2/$s" --gold data/curated/gold_units.csv --model "$M"
    stage "$B2 SCORE $s"   $PY scripts/run_scoring.py --run "$B2/$s" --no-dashboard
  done
  [ -f "outputs/$B1/verdict.md" ] || stage "$B1 VERDICT" $PY scripts/run_verdict.py --batch "$B1" --model "$VERDICT_MODEL"
  [ -f "outputs/$B2/verdict.md" ] || stage "$B2 VERDICT" $PY scripts/run_verdict.py --batch "$B2" --model "$VERDICT_MODEL"
  stage "DASHBOARD" $PY scripts/build_dashboard.py
done
say "=== ALL DONE ==="
