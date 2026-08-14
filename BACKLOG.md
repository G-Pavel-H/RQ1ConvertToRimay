# Shared Backlog — AnnotationToolForRimay & RQ1ConvertToRimay

Forward-looking, **technical** work items for the two coupled repos. This is a
single shared backlog on purpose: the annotation tool produces the data the RQ1
experiment consumes, so a change in one usually implies a change in the other.
Items that span both repos are marked **[coupled]**.

Each item is written for a Claude Code CLI implementer to pick up. Scope is
deliberately technical (workflow, storage, scoring plumbing) — **not** research
logic, research questions, or annotation-scheme design.

Status legend: `TODO` = not started · `IN PROGRESS` · `DONE`.

---

## RQ1ConvertToRimay

### R1 — Pass the output run folder name on the command line; remove `new_run.sh`  ·  `DONE`

**Landed:** `bin/new_run.sh` and the highest-`runN` discovery in `bin/_common.sh`
are gone. Every convert/score script takes the batch folder as its first
argument (`bin/convert_zsl.sh run2` → `outputs/run2/zsl/`, validated by
`require_run_name`), `--run-name` is required in `scripts/run_conversion.py`,
and `config.next_run_id()` was removed. The name is free-form; re-using one
overwrites that batch.


**What:** Stop inferring the run folder implicitly. Today `bin/new_run.sh`
creates the next `outputs/runN/` and the convert/score scripts auto-target the
highest-numbered batch. Replace this with an **explicit run-folder name passed on
the command line** at execution time.

**Why:** The auto-incrementing scheme hides where results land and couples the
convert and score steps to "whatever the latest folder is." Passing the name
explicitly makes each invocation self-describing, reproducible, and safe to run
out of order or in parallel.

**Direction (for the implementer to detail):**
- Remove `bin/new_run.sh`.
- The convert scripts (`bin/convert_zsl.sh`, `convert_fsl.sh`, `convert_cot.sh`)
  and score scripts (`bin/score_*.sh`) should take the target output folder name
  as an argument instead of discovering it. The Python entry points already
  accept `--run-name` / `--run`; the shell wrappers should forward a
  user-supplied name rather than computing one.
- If no name is given, decide on a sensible behaviour (e.g. require it, or fall
  back to a default) — implementer's call, but no silent "latest folder" magic.
- Update `README.md` and `architecture.txt` so the documented workflow matches
  (the "Start a fresh batch" section and the `new_run.sh` references go away).

### R2 — Remove Rimay-conversion similarity-to-gold from scoring  ·  `DONE`  ·  **[coupled with A1]**

**Landed:** `canonicalRimay` is no longer read anywhere — dropped from
`GoldRecord` and from `QualityItem`. `llm_vs_gold_similarity()` is replaced by
`llm_vs_human_similarity()` (every LLM-annotator pair) plus
`similarity_to_humans()` for the per-requirement mean/best; the human-human
ceiling is unchanged. Track 1's categorical gold is untouched, as specified.
A1 can drop the column from the export without breaking RQ1 — the loader
already ignores it.


**What:** In Stage 2 scoring, remove the part of Track 2 that measures similarity
of the LLM conversion against an adjudicated overall gold conversion
(`canonicalRimay`). Keep the **per-annotator** similarity comparisons (the
human-human baseline and LLM-vs-annotator style comparisons).

**Why:** We are dropping the idea of a single overall gold standard for the Rimay
*conversion text* (see A1). Scoring the conversion is meant to be done against the
individual annotators' conversions, not one canonical reference — so the
LLM-vs-gold-canonical similarity is no longer meaningful.

**Direction (for the implementer to detail):**
- In `src/scoring/conversion_quality.py`, drop the LLM-vs-gold
  (`canonicalRimay`) similarity distribution and any reporting of it.
- Retain the human baseline / per-annotator similarity machinery.
- `src/gold_loader.py`: stop requiring / reading `canonicalRimay` if nothing
  else depends on it (implementer to check other consumers first).
- Update the metrics report template, `tests/test_conversion_quality.py`,
  `README.md` ("Track 2 — conversion quality", "Similarity to gold"), and any
  non-goal notes referencing the placeholder gold metric.
- **Note the Track-1 field-accuracy scoring still uses the categorical gold**
  (`gold_*` slots) — this item is only about the free-text *conversion*
  similarity, not the categorical accuracy track. Do not remove that.

### R3 — Report the scoring results as a web page, not markdown  ·  `DONE`

**Landed:** Stage 2 no longer writes `metrics.md` / `per_requirement.md`. It
writes `scoring/results.json` (counts, both tracks, per-requirement detail) next
to the unchanged `comparison.csv`. `scripts/build_report.py` + `src/report.py`
collect every run's `results.json` and inject them into `templates/report.html`,
producing `outputs/report.html`: one self-contained static page (no server, no
dependencies) with a run picker, an all-runs overview, both tracks per run, a
legend defining every column, and a per-requirement drill-down. Stage 2 rebuilds
the page on every scoring run (`--no-report` opts out), so it is never stale;
`bin/report.sh` builds and opens it.

### R4 — LLM verdict stage over the scoring results  ·  `TODO`

**What:** A stage that reads a run's `scoring/results.json`, asks an LLM to
analyse the metrics, and writes its conclusion back into the `verdict` field
(currently `null` on every run).

**Why:** The numbers need interpretation — which strategy won, where the LLM
compensates for missing information, whether the similarity gap to the human
ceiling is meaningful. Writing that verdict into the results file keeps it with
the run it describes.

**Direction (for the implementer to detail):**
- The `verdict` slot already exists in `results.json` and the report already
  renders it when non-null (it reads `verdict.text`, falling back to the raw
  value) — decide the final shape (free text? per-track findings? a score?) and
  update `templates/report.html` to match.
- Keep the stage offline and re-runnable: input is `results.json` only, no
  re-conversion, no Paska.
- Comparing runs (which strategy won) needs more than one `results.json`;
  decide whether the verdict is per-run, per-batch, or both.

---

## AnnotationToolForRimay

### A1 — Remove the overall gold standard for the Rimay conversion  ·  `TODO`  ·  **[coupled with R2]**

**What:** Remove the notion of a single adjudicated/canonical gold standard for
the **Rimay conversion text** (`canonicalRimay`). Conversion scoring is done as
per-annotator similarity, so an overall gold conversion has no purpose.

**Why:** There is no meaningful single "correct" conversion to adjudicate to;
comparison is annotator-to-annotator. Carrying a canonical conversion adds a
field the workflow doesn't use and implies a gold that doesn't exist.

**Direction (for the implementer to detail):**
- Remove `canonicalRimay` from the adjudication model / flow
  (`backend/src/models/Adjudication.js`, adjudication route, the admin
  adjudication UI, the exporter's `canonicalRimay` column, and serializers).
- **Scope check for the implementer:** this item targets the *conversion-text*
  gold only. The categorical adjudication (`goldSlots`, `goldConditionType`,
  `goldOverallIncomplete`) is a separate concern — confirm with the plan before
  touching it. Default assumption: leave categorical adjudication intact unless
  told otherwise.
- Update `DATABASE.md`, `README.md` (data model, export shape, adjudication
  mentions), and `WORKFLOW.md` where the canonical conversion is described.
- The exported CSV is the handoff to RQ1 — coordinate the column change with
  **R2** so the RQ1 gold loader / scorer isn't left expecting a dropped column.

---

## Coupling notes

- **A1 ↔ R2** are the same conceptual change on both sides of the data handoff:
  the annotation tool stops producing an overall gold conversion, and RQ1 stops
  scoring against one. Land them together (or A1 first, then R2) and re-export
  `data/gold_annotations.csv` for RQ1 after the annotation-tool export shape
  changes.
