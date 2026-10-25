# Experiments

Gold: `data/curated/` — exported from the curation tool (`scripts/curate.py` → Export).

| # | Data | Task | Batches (`outputs/…/<zsl\|fsl\|cot>`) |
|---|---|---|---|
| 1 | `gold_atomic.csv` — 35 requirements | normal conversion | `exp1_atomic_{haiku,sonnet,opus}` |
| 2 | `gold_units.csv` — 15 requirements → 42 units | the prompt says the requirement is non-atomic and how many units to return | `exp2_units_{haiku,sonnet,opus}` |
| 3 | `gold_all_units.csv` — all 50 → 77 units | blind: the prompt says neither atomicity nor the count | `exp3_blind_{haiku,sonnet,opus}` |

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

## Changes (2026-10-24) — experiment 3

- `run_conversion.py --blind`; prompts `prompts/{zsl,fsl,cot}_blind_prompt.md`; system prompt = the units one with the first passage asking the model to decide the count (`prompt_builder._BLIND_FIRST`). Exp 2 prompts unchanged.
- FSL: the 3 atomic + 3 non-atomic examples, alternating, without the count line.
- Scoring as the units task; `units.nonAtomicDetection` (tp/fp/fn/tn, P/R/F1) added. Verdict payloads now include `task` and the unit summary.
- Dashboard: Experiment 3 section, detection table, "Non-atomic detected (F1)" metric. `run_experiments.sh` runs exp 3 after exp 2.
- Status and caveats: `STATUS.md`.

## Changes (2026-10-24) — v2 rerun of all three experiments

- **Answer block:** every prompt asks for `<rimay>` … `</rimay>` (CoT reasons before it); FSL examples show it. `llm_converter.parse_answer` reads the last block, keeps lines with a modal verb (or `<MISSING_MODAL_VERB>`), records the rest; no block → the older readers.
- **Atomicity definition** in the system prompt ("What counts as one system response"), shared by all experiments; the blind passage points to it.
- **Anomalies** (`src/scoring/anomalies.py` → `results.json` `anomalies`): no/unclosed block, text outside it, dropped lines, extra lines, `<NON_ATOMIC>` on atomic, atomic split / non-atomic merged, count mismatch, empty, truncated, Paska error.
- **Verdict:** fixed sections (Summary, Missing-slot detection, Conversion quality, Units and atomicity, Anomalies, Next step), terse style, ≤ 400 words; task-aware context (the old note wrongly said non-atomic requirements were excluded); gets anomalies and cross-strategy count disagreements.
- **Dashboard:** anomalies column in scoreboards, Anomalies card per experiment and run, unit-count matrix (requirements off the gold or disputed between runs).
- v1 outputs moved to `outputs/_archive/2026-10-24_v1/`.
