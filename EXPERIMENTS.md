# Experiments

Gold: `data/curated/` — exported from the curation tool (`scripts/curate.py` → Export).

| # | Data | Task | Batches (`outputs/…/<zsl\|fsl\|cot>`) |
|---|---|---|---|
| 1 | `gold_atomic.csv` — 35 requirements | normal conversion | `exp1_atomic_{haiku,sonnet,opus}` |
| 2 | `gold_units.csv` — 15 requirements → 42 units | the prompt says the requirement is non-atomic and how many units to return | `exp2_units_{haiku,sonnet,opus}` |
| 3 | — | not started | — |

View: `outputs/dashboard.html` (`bin/dashboard.sh` rebuilds and opens it).
Run everything: `./run_experiments.sh` (log: `outputs/_logs/experiments.log`; `MODELS="haiku"` for a subset).
Models: `claude-haiku-4-5-20251001`, `claude-sonnet-5-5`, `claude-opus-5-5`; subscription backend.

## Changes (2026-10-18)

- **FSL examples** (`prompts/examples/fsl_examples.json`): no scope — `For all users,` → `<MISSING_SCOPE>,`; `<MISSING_CONDITION>,` added where there is no condition; typo and a passive-voice condition fixed. All three pass Paska.
- **Units task** (experiment 2): `run_conversion.py --units` reads `nUnits` from the gold and states it in the prompt.
  - Prompts: `prompts/{zsl,fsl,cot}_units_prompt.md`; FSL examples `prompts/examples/fsl_units_examples.json` (3 unused corpus requirements, 2/3/2 units, all pass Paska).
  - System prompt: for this task only, the three `<NON_ATOMIC>` passages are swapped for a one-requirement-per-line rule (`prompt_builder._ATOMIC_PASSAGES`).
  - Each returned line is a unit with its own slots and Paska verdict (`pipeline.run_single_units`).
  - Scoring (`src/scoring/units.py`): each gold unit is a subject `<reqId>#<unit>`; LLM units are matched to gold units by the best summed similarity to the annotators' texts (order-free). Missing units score as empty, extras are listed. `results.json` gains `task` and `units` (count compliance, mapping); the dashboard and verdict read it unchanged.
- **Subscription backend** uses the installed `claude` CLI (`RIMAY_CLAUDE_CLI` to override); the SDK's bundled one rejects Opus 5.5.
- **Caveat:** all 42 gold units are complete, so the incomplete-verdict F1 is undefined in experiment 2 — read slot scores, similarity and Paska there.
- **Dashboard** replaces the old report (`templates/report.html`, `src/report.py`, `scripts/build_report.py`, `bin/report.sh` removed): `outputs/dashboard.html`, built by `src/dashboard.py` / `scripts/build_dashboard.py`, rebuilt by every scoring (`--no-dashboard` opts out). Views: overview → experiment (scoreboard, models compared per strategy, per-slot tables, cosine vs human↔human, Paska smells, unit-count compliance, requirement × run matrix) → model → run (filterable requirements) → requirement across runs.
- `run_experiments.sh` skips runs already scored, so it can be stopped and restarted; the subscription backend runs with no tools (a tool call ended a 6-unit answer early).
