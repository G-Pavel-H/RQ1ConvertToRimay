# RQ1 — NL → Rimay conversion, scored against a human gold standard

First paper of a PhD thesis in requirements engineering. The pipeline takes
natural-language (NL) feature requests, forces an LLM to convert each into
**Rimay** (a controlled natural language for requirements) under three
prompting strategies (zero-shot / few-shot / chain-of-thought), and forces the
conversion even when the source NL is missing information — marking absent
structural slots with `<MISSING_*>` placeholders. The output is then scored
against an adjudicated **human gold standard**.

The system is split into two cleanly decoupled stages:

- **Stage 1 — conversion** (`scripts/run_conversion.py`): NL → Rimay → Paska,
  logged to MLflow, with a JSONL manifest as the handoff artifact.
- **Stage 2 — scoring** (`scripts/run_scoring.py`): offline comparison of the
  manifest against the gold CSV. It reads *only* the manifest and the gold —
  never MLflow — and never re-runs the LLM or Paska.

See [`architecture.txt`](architecture.txt) for the full flow diagram; the prose
below is kept in sync with it.

## Two scoring tracks

**Track 1 — field accuracy** (`src/scoring/field_accuracy.py`). The LLM slot
signal is binary (missing / filled); the gold is ternary
(present / implied / missing).

- *Primary (binary collapse):* collapse gold `present`+`implied` to
  *not-missing*, keep `missing`. Per slot (scope, condition, actor, modalVerb,
  action) report precision / recall / F1 for the **missing** class, plus micro
  and macro averages. The collapse is deliberate: the LLM only signals
  missing-or-not and cannot distinguish present from implied, so the `missing`
  class is the only apples-to-apples comparison.
- *Secondary lenses:* among gold **implied** slots, how often did the LLM fill
  (inferred context, good) vs over-flag missing? Among gold **missing** slots,
  how often did it correctly flag vs **silently fill** (possible
  compensation / hallucination)?
- *Overall verdict:* LLM overall-incomplete = any mandatory slot
  (actor, modalVerb, action) flagged missing; compared to
  `gold_overallIncomplete` with an agreement rate and 2×2 confusion.

**Track 2 — conversion quality** (`src/scoring/conversion_quality.py`).

- *Similarity to gold:* reference is `canonicalRimay`. The metric is isolated
  behind one swappable function `conversion_similarity(a, b) -> float`. **v0 is
  a deliberate placeholder**: difflib `SequenceMatcher` ratio on normalised text
  plus a token Jaccard (both reported). Swap the function body when a
  structural / semantic metric is ready.
- *Human baseline (the ceiling):* pairwise similarities among the human
  `rimayText` conversions, using the same function, reported side by side with
  the LLM-vs-gold distribution. The LLM number is only interpretable against how
  much humans vary among themselves.
- *Paska validation:* pass rate per strategy and the frequency of each smell
  type — the independent structural check on conversion fidelity.

## The Paska integration (reused verbatim)

Paska is a smell detector for requirements. It runs in **two steps**:

1. **Constituency parsing** — PTB-format parse trees. The original Paska used
   Python 3.8 + allennlp + a 2020 ELMo model; that step was replaced with
   **stanza** (`src/parsing_trees.py`), which runs on modern Python and produces
   equivalent PTB trees in-process.
2. **Smell detection** —
   `java -jar smell_detector.jar <trees> <out> <postagger>`. **Requires Java
   1.8.** Emits a CSV of detected smells and suggested Rimay patterns.

`src/paska_runner.py` writes the `(req_id, text)` tuples to Paska's expected
`;`-separated CSV, runs both steps, parses the smell CSV into a structured
result, and **caches by a SHA-256 hash of the input** so identical text is not
re-parsed. In this repo **Paska runs exactly once, on the stripped LLM Rimay**
(placeholders removed) — never on the NL, and never twice.

The `paska/` jar and files, the `models/` POS tagger, `parsing_trees.py`, and
`paska_runner.py` are carried over unchanged from the prior project.

## Setup

Requires **macOS or Linux** (stanza's dependencies are not configured for
Windows here — use WSL) and **Java 1.8** on `PATH`.

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

# Stanza English constituency model (one-time download):
python -c "import stanza; stanza.download('en', processors='tokenize,pos,constituency')"

# Stanford POS tagger for the Paska jar (not committed; ~15 MB):
#   download english-left3words-distsim.tagger from
#   https://nlp.stanford.edu/software/tagger.shtml
#   and place it in models/ (or point PASKA_POS_TAGGER_PATH at it)

cp .env.example .env    # then fill in ANTHROPIC_API_KEY and PASKA_POS_TAGGER_PATH

python scripts/verify_setup.py   # pass/fail pre-flight report
```

`verify_setup.py` checks the platform, Java 1.8, the Python deps, the stanza
model, the Paska jar, the POS tagger, the gold CSV, and the API key.

## Data

`data/gold_annotations.csv` is an export from a separate annotation app: one row
per (requirement, annotator), four annotators per requirement. The `gold_*`
columns plus `canonicalRimay` and `nlText` are adjudicated and identical across
a requirement's rows; the per-annotator columns (`rimayText`, `slot_*`, …) form
the human baseline. `gold_loader.py` derives everything (annotator names,
requirement count) from the data, so it keeps working as the set grows.

> Non-goals: the `pragyanIncomp` third-party label is ignored; Paska never runs
> on the NL; inter-annotator agreement (Kappa) is computed elsewhere; the
> Track-2 similarity metric is an intentional v0 placeholder.

## Running

Each conversion run gets its own self-contained, auto-numbered folder under
`outputs/` (e.g. `run1_zsl`, `run2_fsl-n3`, `run3_cot`) holding that run's Rimay
files, manifest, scoring reports, and a `run_meta.json` sidecar. The numeric
prefix auto-increments; override the folder name with `--run-name`.

```bash
# Stage 1 — conversion (creates outputs/<run_id>/ + MLflow runs)
python scripts/run_conversion.py --strategy zsl          # -> run1_zsl
python scripts/run_conversion.py --strategy fsl --n-fsl-examples 3   # -> run2_fsl-n3
python scripts/run_conversion.py --strategy cot          # -> run3_cot
#   optional: --n-samples N --model ... --temperature 0.0 --max-tokens 1024
#             --run-name my_custom_name

# Stage 2 — scoring (offline; reads the run's manifest + gold)
python scripts/run_scoring.py --run run1_zsl
python scripts/run_scoring.py --run run2_fsl-n3
python scripts/run_scoring.py --run run3_cot
#   strategy + gold path are read from run_meta.json
#   optional: --gold ... --fsl-example-ids id1,id2

# MLflow UI (Stage 1 exploration only; runs tagged with output_run_id)
mlflow ui --backend-store-uri sqlite:///mlruns/mlflow.db
```

Each run folder looks like:

```
outputs/run1_zsl/
  run_meta.json                    strategy, model, params, counts, timestamp
  llm_rimay/<reqId>.txt            raw Rimay per requirement
  conversions/manifest.jsonl       the scorer's input (Stage 1 -> Stage 2 handoff)
  scoring/metrics.md               Track 1 + Track 2 tables
  scoring/per_requirement.md       manual-review worksheet
  scoring/comparison.csv           tidy, per-requirement (this run)
outputs/_paska/                    Paska cache + working files, SHARED across runs
```

The Paska cache lives in `outputs/_paska/` (not inside run folders) so identical
Rimay text is never re-parsed across runs. To compare strategies, line up the
per-run `scoring/comparison.csv` files (or the printed summaries).

Default development model is `claude-haiku-4-5-20251001` (override with
`--model`). Chain-of-thought is expected to reason before answering; if a model
leaks its scratchpad, `llm_converter.extract_final_rimay()` recovers the
single-line final Rimay and the full response is preserved as the
`rimay_raw.txt` MLflow artifact.

## Prompts & FSL exemplars

- `prompts/system_prompt.md` holds the Rimay grammar + conversion rules +
  placeholder convention. The grammar was carried over from the prior project
  so the pipeline runs out of the box (see the TODO comment at the top of the
  file) — replace it with your authoritative grammar reference when ready.
- `prompts/examples/fsl_examples.json` holds **three exemplars drawn from the
  training-stage export** (`rimay_export_training.csv`; source reqIds
  5963-Signal, 604-Signal, 312-Signal), using the adjudicated `canonicalRimay`
  as the target and the full `nlText` as the input, so the in-context format
  matches what the model sees at eval time. They cover a plain system response,
  a `When … then …` trigger condition, and a quoted-theme action. `prompt_builder`
  reads only the `nl` and `rimay` fields (`id`, `source_reqId`, `note` are
  metadata). **Exemplars are in-context demonstrations, never scored items** —
  they come from the training pool and do not overlap with the pilot gold set;
  both the `id` and the `source_reqId` are defensively skipped in both stages.
  Note: the training-stage gold canonicals realise implied scope as
  "For all users" and omit absent conditions rather than emitting `<MISSING_*>`
  markers, so these exemplars do not demonstrate the placeholder-emitting
  behaviour — adjust if you want the FSL context to model that explicitly.

## Layout

```
data/gold_annotations.csv     the human gold (Stage 1 input + Stage 2 reference)
paska/                        Paska jar + files (reused verbatim)
models/                       Stanford POS tagger (gitignored)
prompts/                      system + zsl/fsl/cot templates + fsl_examples.json
src/
  config.py                   paths, model defaults, placeholder tokens, env
  gold_loader.py              gold CSV -> GoldRecord per reqId + human baseline
  prompt_builder.py           strategy -> assembled prompt
  llm_converter.py            NL -> Rimay; strip_missing_placeholders(); CoT hygiene
  parsing_trees.py            stanza PTB trees (Paska step 1, reused)
  paska_runner.py             Paska wrapper w/ caching (reused; Rimay only)
  pipeline.py                 single-requirement orchestration + MLflow + manifest
  tracking.py                 MLflow setup
  scoring/field_accuracy.py   Track 1 (pure functions)
  scoring/conversion_quality.py  Track 2 (pure functions)
scripts/
  verify_setup.py             pre-flight checks
  run_conversion.py           Stage 1 entry point (creates outputs/<run_id>/)
  run_scoring.py              Stage 2 entry point (--run <run_id>)
tests/                        pytest suite for the scoring modules
outputs/<run_id>/             per-run artifacts: llm_rimay, conversions, scoring, meta
outputs/_paska/               shared Paska cache (gitignored)
mlruns/                       MLflow SQLite backend (gitignored)
architecture.txt              ASCII flow diagram (kept in sync with this README)
```

## Tests

```bash
python -m pytest tests/ -q
```
