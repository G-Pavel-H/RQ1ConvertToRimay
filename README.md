# RQ1 — NL → Rimay Conversion, Scored Against a Human Gold Standard

First-paper experiment of a PhD thesis in requirements engineering. The
pipeline forces an LLM to convert natural-language (NL) feature requests
into **Rimay** (a controlled natural language for requirements) under
three prompting strategies — zero-shot (`zsl`), few-shot (`fsl`),
chain-of-thought (`cot`) — even when the source NL is missing
information, marking absent structural slots with `<MISSING_*>`
placeholders. Output is scored against an **adjudicated human gold
standard** two ways:

* **Track 1 — field accuracy.** The LLM's per-slot missing/filled signal
  vs the gold slot labels (precision / recall / F1 per slot, plus the
  overall complete/incomplete verdict).
* **Track 2 — conversion quality.** (a) Text similarity between the
  LLM's Rimay and the gold's canonical Rimay (v0 metric is a deliberate
  placeholder), with the human-human similarity distribution as the
  interpretive ceiling; (b) Paska smell detection on the LLM Rimay as an
  independent structural fidelity check.

## Two decoupled stages

```
Stage 1 (conversion, online)          Stage 2 (scoring, offline)
gold CSV ──► prompt ──► LLM ──► Rimay      manifest + gold CSV
        strip <MISSING_*> ──► Paska        ──► Track 1 field accuracy
        ──► MLflow + JSONL manifest        ──► Track 2 conversion quality
```

* **Stage 1** (`scripts/run_conversion.py`) converts, runs **Paska
  exactly once per requirement, on the placeholder-stripped LLM Rimay**
  (never on the NL), logs runs/traces to MLflow (`gold_zsl` /
  `gold_fsl` / `gold_cot`, SQLite backend), and writes a machine-readable
  manifest `outputs/conversions/<strategy>.jsonl`.
* **Stage 2** (`scripts/run_scoring.py`) reads only the manifest + the
  gold CSV — it never touches MLflow — and writes
  `outputs/scoring/<strategy>/metrics.md`, `per_requirement.md` and
  `outputs/scoring/comparison.csv`.

A full flow diagram lives in `architecture.txt` *(produced in build step
5, after the Stage 1 schema checkpoint)*.

## Setup

Requires **macOS or Linux** (stanza's dependency chain does not run the
same way on Windows), **Python 3.11+** and **Java 1.8** (for the Paska
jar).

```bash
python3.11 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python -c "import stanza; stanza.download('en', processors='tokenize,pos,constituency')"
cp .env.example .env    # then fill in ANTHROPIC_API_KEY and PASKA_POS_TAGGER_PATH
python scripts/verify_setup.py
```

The Stanford POS tagger (`models/english-left3words-distsim.tagger`,
gitignored) must be downloaded from
<https://nlp.stanford.edu/software/tagger.shtml> and referenced by
`PASKA_POS_TAGGER_PATH` in `.env`.

## Running

```bash
# Stage 1 — convert (repeat per strategy)
python scripts/run_conversion.py --strategy zsl [--n-samples N] [--model ...] \
    [--temperature 0.0] [--max-tokens 1024] [--n-fsl-examples 2]

# Inspect runs/traces
mlflow ui --backend-store-uri sqlite:///mlruns/mlflow.db

# Stage 2 — score offline (built after the schema checkpoint)
python scripts/run_scoring.py --strategy zsl --gold data/gold_annotations.csv \
    [--fsl-example-ids id1,id2] [--out outputs/scoring]
```

## Data

`data/gold_annotations.csv` is the adjudicated export from the
annotation app: one row per (requirement, annotator), four annotators
per requirement; the `gold_*` columns and `canonicalRimay` are identical
across a requirement's rows. `nlText` is the LLM input; the per-annotator
`rimayText` columns feed the Track 2 human baseline. The `pragyanIncomp`
column is an old third-party label and is **ignored everywhere**.

> The current file is the pilot export (10 requirements × 4 annotators)
> copied from the annotation tool; replace it with newer exports as the
> set grows — nothing hard-codes annotator names or requirement counts.

## Prompts

* `prompts/system_prompt.md` — Rimay grammar + conversion rules +
  placeholder convention. **Carried over from the previous repo
  (RQ1ExpCode), where the grammar reference was pasted in; review and
  replace if the grammar text should change.**
* `prompts/{zsl,fsl,cot}_prompt.md` — per-strategy user templates.
* `prompts/examples/fsl_examples.json` — FSL exemplars. The two seeded
  entries are **placeholder demonstrations** (one clean complete
  conversion, one showing a `<MISSING_*>` placeholder) to be replaced
  with real exemplars drawn from the training stage. Exemplars must
  never overlap with the gold evaluation set — they are in-context
  demonstrations, not scored items; `run_conversion.py` defensively
  skips any gold reqId that appears as an exemplar `id`.

## Paska (reused verbatim from RQ1ExpCode)

Paska runs in two steps: (1) constituency parsing to PTB trees — the
original Python 3.8 + allennlp step was replaced by **stanza**, running
in-process on modern Python (`src/parsing_trees.py`); (2) the Java smell
detector — `java -jar paska/smell_detector.jar <trees> <out> <tagger>`
(Java 1.8). `src/paska_runner.py` wraps both steps and caches results
under `outputs/paska_smells/.cache/<sha256-of-input>/` so identical text
is never re-parsed.

## Non-goals

* No comparison against `pragyanIncomp` or any third-party label.
* No Paska on the NL — Paska runs once, on the LLM Rimay only.
* No inter-annotator kappa here (computed elsewhere on raw annotations).
* Track 2's v0 similarity metric is a deliberate, isolated placeholder.
* No LangChain / LangGraph — the conversion is a straight sequence in
  plain Python.
