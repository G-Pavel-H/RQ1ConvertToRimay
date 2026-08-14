# RQ1 — NL → Rimay conversion, scored against a human gold standard

First paper of a PhD thesis in requirements engineering. The pipeline takes
natural-language (NL) feature requests, forces an LLM to convert each into
**Rimay** (a controlled natural language for requirements) under three
prompting strategies (zero-shot / few-shot / chain-of-thought), and forces the
conversion even when the source NL is missing information — marking absent
structural slots with `<MISSING_*>` placeholders. The output is then scored
against the **human annotations**: an adjudicated categorical gold for the
slots, and the annotators' own conversions for the Rimay text.

The system is split into cleanly decoupled stages:

- **Stage 1 — conversion** (`scripts/run_conversion.py`): NL → Rimay → Paska,
  logged to MLflow, with a JSONL manifest as the handoff artifact.
- **Stage 2 — scoring** (`scripts/run_scoring.py`): offline comparison of the
  manifest against the gold CSV. It reads *only* the manifest and the gold —
  never MLflow — and never re-runs the LLM or Paska. It writes
  `scoring/results.json`.
- **Report** (`scripts/build_report.py`): collects every run's `results.json`
  into one self-contained HTML page, `outputs/report.html`.

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

**There is no gold conversion text.** A requirement has as many valid Rimay
conversions as it has annotators, so the LLM conversion is compared against the
annotators' own `rimayText`, never against a single canonical reference. (The
export's legacy `canonicalRimay` column is unused and not read.)

- *LLM vs annotators:* every (LLM, annotator) pair, plus a per-requirement
  mean/best. The metric is isolated behind one swappable function
  `conversion_similarity(a, b) -> float`. **v0 is a deliberate placeholder**:
  difflib `SequenceMatcher` ratio on normalised text plus a token Jaccard (both
  reported). Swap the function body when a structural / semantic metric is ready.
- *Human baseline (the ceiling):* pairwise similarities among the human
  `rimayText` conversions, using the same function, reported side by side with
  the LLM distribution. The LLM number is only interpretable against how much
  humans vary among themselves.
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
columns and `nlText` are adjudicated and identical across a requirement's rows;
the per-annotator columns (`rimayText`, `slot_*`, …) form the human baseline.
The adjudicated gold is **categorical only** — there is no gold conversion text.
`gold_loader.py` derives everything (annotator names, requirement count) from
the data, so it keeps working as the set grows.

> Non-goals: the `pragyanIncomp` third-party label is ignored; the legacy
> `canonicalRimay` column is not read; Paska never runs on the NL;
> inter-annotator agreement (Kappa) is computed elsewhere; the Track-2
> similarity metric is an intentional v0 placeholder.

## Running

The convenience scripts in [`bin/`](bin/) are the easy path — one per strategy
per stage. A **run** is a batch folder `outputs/<run-folder>/` holding one
self-contained subfolder per strategy (`zsl/`, `fsl/`, `cot/`), each with its own
Rimay files, manifest, scoring results, and `run_meta.json` sidecar.

**You name the batch folder yourself, on every invocation** — nothing is
inferred from what already exists on disk, so a run is reproducible, safe to
repeat, and safe to run out of order:

```bash
# Convert a strategy into the named batch (creates the folder if needed)
bin/convert_zsl.sh run2          # -> outputs/run2/zsl/
bin/convert_fsl.sh run2          # -> outputs/run2/fsl/   (3 FSL exemplars)
bin/convert_cot.sh run2          # -> outputs/run2/cot/

# Score the same batch (offline; manifest + gold); each run also refreshes
# outputs/report.html, so the report is never stale
bin/score_zsl.sh run2            # <- outputs/run2/zsl/
bin/score_fsl.sh run2
bin/score_cot.sh run2

# Open the report (rebuilds it first; only needed to *view* it)
bin/report.sh                    # -> outputs/report.html
```

The name is any folder name you like (`run2`, `haiku-baseline`, …); re-using one
overwrites that batch. The scripts activate `.venv` and pass extra flags straight
through, e.g. `bin/convert_zsl.sh run2 --n-samples 3` or
`bin/score_fsl.sh run2 --gold path/to/gold.csv`.

Under the hood they call the entry points, which you can also run directly —
`--run-name <run-folder>/<strategy>` selects the nested folder:

```bash
python scripts/run_conversion.py --strategy zsl --run-name run2/zsl
#   optional: --n-samples N --model ... --temperature 0.0 --max-tokens 1024
python scripts/run_scoring.py --run run2/zsl
#   strategy + gold path are read from run_meta.json
#   rebuilds outputs/report.html unless --no-report
#   optional: --gold ... --fsl-example-ids id1,id2 --no-report
python scripts/build_report.py
#   standalone rebuild (e.g. after editing the template)
#   optional: --outputs outputs --out outputs/report.html

# MLflow UI (Stage 1 exploration only; runs tagged with output_run_id)
mlflow ui --backend-store-uri sqlite:///mlruns/mlflow.db
```

Each batch looks like:

```
outputs/run2/
  zsl/
    run_meta.json                  strategy, model, params, counts, timestamp
    llm_rimay/<reqId>.txt          raw Rimay per requirement
    conversions/manifest.jsonl     the scorer's input (Stage 1 -> Stage 2 handoff)
    scoring/results.json           every metric + per-requirement detail
    scoring/comparison.csv         tidy, per-requirement (this run)
  fsl/  … same layout
  cot/  … same layout
outputs/report.html                the report over ALL runs (generated, gitignored)
outputs/_paska/                    Paska cache + working files, SHARED across runs
```

The Paska cache lives in `outputs/_paska/` (not inside run folders) so identical
Rimay text is never re-parsed across runs.

## The results report

Scoring writes **data, not prose**: `scoring/results.json` holds the counts, both
tracks' metrics, and the per-requirement detail. Those files are collected and
injected into [`templates/report.html`](templates/report.html), producing one
self-contained page — no server, no build step, no dependencies. It gives you a
run picker, a side-by-side overview of all runs, the full metric tables per run,
and a per-requirement drill-down (NL, LLM Rimay, each annotator's conversion,
slots). Every table carries a legend spelling out what each column means — TP is
the *missing* class, not the present one, and the legends say so.

**The report rebuilds itself on every scoring run**, so it always matches what is
on disk. `scripts/build_report.py` (or `bin/report.sh`) only needs running to
open it, or to re-render after you edit the template.

Each run's `results.json` carries a `verdict` field, currently `null`. It is the
slot for a future analysis stage in which an LLM reads the results and writes its
conclusion; the report already renders it when it is non-null. Nothing else
depends on it.

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
  5963-Signal, 604-Signal, 312-Signal), using that export's adjudicated
  canonical conversion as the target and the full `nlText` as the input, so the
  in-context format matches what the model sees at eval time. The exemplar
  targets are frozen in the JSON file — scoring no longer uses any canonical
  conversion. They cover a plain system response,
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
bin/                          convenience scripts: convert_/score_<strategy>.sh, report.sh
data/gold_annotations.csv     the human gold (Stage 1 input + Stage 2 reference)
paska/                        Paska jar + files (reused verbatim)
models/                       Stanford POS tagger (gitignored)
prompts/                      system + zsl/fsl/cot templates + fsl_examples.json
templates/report.html         the report page (data injected at build time)
src/
  config.py                   paths, model defaults, placeholder tokens, env
  gold_loader.py              gold CSV -> GoldRecord per reqId + human baseline
  prompt_builder.py           strategy -> assembled prompt
  llm_converter.py            NL -> Rimay; strip_missing_placeholders(); CoT hygiene
  parsing_trees.py            stanza PTB trees (Paska step 1, reused)
  paska_runner.py             Paska wrapper w/ caching (reused; Rimay only)
  pipeline.py                 single-requirement orchestration + MLflow + manifest
  tracking.py                 MLflow setup
  report.py                   collect results.json -> rendered HTML
  scoring/field_accuracy.py   Track 1 (pure functions)
  scoring/conversion_quality.py  Track 2 (pure functions)
scripts/
  verify_setup.py             pre-flight checks
  run_conversion.py           Stage 1 entry point (--run-name <run>/<strategy>)
  run_scoring.py              Stage 2 entry point (--run <run>/<strategy>)
  build_report.py             report entry point (all runs -> outputs/report.html)
tests/                        pytest suite for the scoring modules
outputs/<run>/<strategy>/     per-run artifacts: llm_rimay, conversions, scoring, meta
outputs/report.html           generated report over all runs (gitignored)
outputs/_paska/               shared Paska cache (gitignored)
mlruns/                       MLflow SQLite backend (gitignored)
architecture.txt              ASCII flow diagram (kept in sync with this README)
```

## Tests

```bash
python -m pytest tests/ -q
```
