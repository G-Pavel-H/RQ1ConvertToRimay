#!/bin/bash
# Detached runner for the main30opus batch.
# The Claude subscription session limit resets at 04:10 Europe/London, so this
# waits for the reset before starting and retries any stage that still gets
# throttled. Safe to run unattended.
cd "/Users/pavelghazaryan/Desktop/PhD Journey/RQ1ConvertToRimay"
LOG="outputs/_logs/main30opus.log"
G=data/gold_annotations_main_atomic.csv
R=main30opus
M=claude-opus-5
PY=.venv/bin/python

say() { echo "[$(date '+%H:%M:%S')] $*" >> "$LOG"; }

# --- probe: only wait if the session limit is actually still in force ---
probe() {
  ANTHROPIC_API_KEY= .venv/bin/python - <<'PROBE' 2>&1
import asyncio, os
os.environ.pop("ANTHROPIC_API_KEY", None)
from claude_agent_sdk import query, ClaudeAgentOptions, AssistantMessage, TextBlock
async def go():
    o = ClaudeAgentOptions(system_prompt="Reply with OK.", model="haiku",
                           allowed_tools=[], setting_sources=[], max_turns=1,
                           env={"ANTHROPIC_API_KEY": ""})
    async for m in query(prompt="OK", options=o):
        if isinstance(m, AssistantMessage):
            for b in m.content:
                if isinstance(b, TextBlock):
                    print("PROBE_OK")
                    return
asyncio.run(go())
PROBE
}

for i in $(seq 1 48); do
  out=$(probe)
  if echo "$out" | grep -q PROBE_OK; then
    say "subscription is available — starting"
    break
  fi
  say "subscription still limited (probe $i); sleeping 20m"
  sleep 1200
done

# --- run a stage, retrying while the session limit is still in force ---
stage() {
  local label="$1"; shift
  for attempt in 1 2 3 4 5 6; do
    say "$label (attempt $attempt)"
    if "$@" >> "$LOG" 2>&1; then
      say "$label OK"
      return 0
    fi
    if grep -q "session limit" "$LOG"; then
      say "$label hit the session limit; sleeping 30m"
      sleep 1800
    else
      say "$label failed for another reason; sleeping 5m"
      sleep 300
    fi
  done
  say "$label GAVE UP after 6 attempts"
  return 1
}

say "=== main30opus starting (model $M, subscription backend) ==="
for s in zsl fsl cot; do
  stage "CONVERT $s" $PY scripts/run_conversion.py --strategy "$s" --run-name "$R/$s" --gold "$G" --model "$M" || exit 1
done
for s in zsl fsl cot; do
  stage "SCORE $s" $PY scripts/run_scoring.py --run "$R/$s" || exit 1
done
stage "VERDICT" $PY scripts/run_verdict.py --batch "$R" --model "$M"
stage "REPORT" $PY scripts/build_report.py
say "=== ALL DONE ==="
