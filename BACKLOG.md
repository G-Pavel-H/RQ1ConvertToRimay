# Shared Backlog — AnnotationToolForRimay & RQ1ConvertToRimay

Forward-looking work items for the two coupled repos. This is a single shared
backlog on purpose: the annotation tool produces the data the RQ1 experiment
consumes, so a change in one usually implies a change in the other. Items that
span both repos are marked **[coupled]**.

Each item is written for a Claude Code CLI implementer to pick up. Scope is
normally technical (workflow, storage, scoring plumbing) rather than research
logic — the one deliberate exception is the **Perspective** section below, which
records a starting position on non-atomic handling so A2/R5 have something to be
refined against.

Item IDs encode the repo: `A*` = AnnotationToolForRimay, `R*` = RQ1ConvertToRimay.

Status legend: `TODO` = not started · `IN PROGRESS` · `DONE`.
Ordering: **open items first, completed items at the bottom.**

---

# Non-atomic handling — the plan (settled process)

How to handle the 15 non-atomic `main` requirements. Concept only. This is the
*simplified* plan after reading the Rimay paper (Veizaga et al. 2020); the
heavier decomposition approach we first sketched is recorded as
**considered-and-rejected** at the end of this section so the reasoning isn't
lost.

**The finding that simplifies everything (Rimay paper §4.3, Listing 5):** Rimay
does **not** restrict a requirement to one system response. Its `SYSTEM RESPONSE`
rule has two forms — an itemized block ("do the following actions: • … • …") and
a logical expression — both explicitly designed to combine *multiple* `ATOMIC
SYSTEM RESPONSE`s with and/or operators inside **one** requirement. Mandatory
content is one actor plus *at least one* system response; the multiplicity lives
*inside* the response. So the earlier premise — "Rimay admits one system response,
therefore non-atomic requirements must be split into N requirements" — was wrong.
A requirement with several actions is directly representable as a single Rimay
statement.

**Decisions locked (the simplified frame):**
- **Non-atomic ≠ incomplete.** Orthogonal defects: incomplete = missing
  information; non-atomic = multiple system responses fused. Kept as distinct
  labels; never collapsed.
- **Non-atomic is a smell/flag, not a decomposition task.** A multi-response
  requirement is *representable* in Rimay (grammar) but is a *quality* defect
  under ISO/IEC/IEEE 29148 singularity — and Paska already emits a "Non-atomic
  requirement" smell on the Rimay (seen in the zsl run). The structural detector
  already exists and already runs; we do not build decomposition machinery.
- **Convert the whole requirement as one Rimay statement; keep all five slots
  single-valued.** Actor / scope / condition / modal verb stay one value each.
  The system response may realise as an itemized *list* — but that is one
  system-response slot whose Rimay text is compound, judged present/implied/
  missing as a single call. So the existing slot scheme and Fleiss' Kappa are
  untouched. (This is the distinction from the rejected plan: the danger was
  splitting *one slot into several independently-labelled values*; a Rimay
  itemized response is still one slot.)
- **Gold barely changes.** It is the existing per-annotator slot annotations plus
  one adjudicated boolean per requirement — "is this non-atomic?" — which the
  annotators already gave. No canonical decomposition, no per-unit gold.

**Steps:**

1. **Write the atomicity/singularity definition — still worth doing, lighter.**
   Anchor to ISO/IEC/IEEE 29148 *singular* (one capability, "stand-alone, not
   grouped"). Operationalise against the borderline reqs (`175-Signal`,
   `4821-Signal`, `5904-Signal`, `7345-Signal`) so the non-atomic flag is applied
   consistently. Output: a short rule + a decided verdict on those four.
2. **Two-minute triage of the 15: "multiple actions" vs "genuinely separate
   requirements".** Most will be multiple actions under one actor/trigger —
   Rimay swallows these whole as an itemized response, no action needed. The
   residual case is a requirement whose responses need *different actors or
   different conditions*; Rimay cannot hold that in one statement. Handle the
   residual at the **data level**, not with an apparatus: either keep it whole and
   let the flag/Paska mark it (fine for detection), or split it into separate
   dataset rows once, by hand, during data prep. Count how many fall in each
   bucket — the guess is that few are truly separate.
3. **Adjudicate the flag to a single boolean per requirement.** Standard
   double-annotate-then-adjudicate on the yes/no non-atomic judgment. Also compute
   plain inter-annotator agreement on the flag over all 50 — that is the ceiling
   the model's *detection* metric can be scored against.
4. **Freeze.** Gold = existing slot annotations + the adjudicated non-atomic
   boolean, plus the frozen singularity definition (the same wording the model is
   later given, so detection is scored fairly). Report atomic (35) and non-atomic
   (15) as separate lines; keep non-atomic parents out of any aggregate that would
   misread them.

**What this drops vs the rejected plan:** the ability to measure whether the LLM
can *split* a compound requirement into its atomic parts. That was never the
thesis question (structural incompleteness + silent compensation), so it is scope
we are deliberately not taking on. If a future paper wants decomposition-as-a-
capability, the rejected plan below is the starting point.

**Model side (feeds R5):** the model converts as one Rimay statement (compound
response allowed) and the non-atomic signal comes from Paska on that Rimay and/or
the model's own flag. Scoring = existing slot accuracy (unchanged) + a binary
detection metric on the flag. No decomposition or alignment scoring.

<details>
<summary><b>Considered and rejected — full decomposition approach</b></summary>

The first sketch treated each non-atomic requirement as a parent with N child
units, each a full single-valued annotation (Rimay + 5 slots + condition type),
requiring: per-annotator independent decomposition, Krippendorff's *unitizing
alpha* for segmentation agreement, an alignment rule (positional vs
similarity-matched) fixed before scoring, an adjudicated canonical decomposition,
and three separate model metrics (detection / decomposition / per-unit slots).
**Rejected** because the Rimay paper (§4.3) shows a multi-response requirement is
directly representable as one Rimay statement, so decomposition into N separate
requirements is unnecessary for conversion; the decomposition *capability* is not
this thesis's question; and the machinery (unitizing alpha, split adjudication,
alignment rule, decomposition scoring track) is disproportionate to what it buys.
The methodology anchors it rested on — ISO 29148 singularity, Krippendorff
unitizing alpha, double-annotate-then-adjudicate — remain sound if that path is
ever revived.
</details>

---

# Open

### A2 — Store the adjudicated non-atomic flag as gold  ·  `TODO`  ·  **[coupled with R5]**

**What:** Give adjudication a single canonical boolean per requirement —
`goldNonAtomic` — reconciled from the annotators' per-annotator non-atomic
verdicts. That is the whole gold change for non-atomic handling; slots stay as
they are. (Superseded the earlier "canonical decomposition" design — see the
rejected approach in the top section.)

**Why:** 15 of 50 `main` requirements are non-atomic. Per the Rimay-paper finding,
these are converted as a single Rimay statement (compound system response), not
decomposed, so the gold needs only an adjudicated flag, not a stored
decomposition. The flag is the reference for R5's detection metric.

**Direction (for the implementer to detail):**
- Additive schema change in `backend/src/models/Adjudication.js`: `goldNonAtomic:
  Boolean`. Existing documents validate unchanged (absent reads as `false`/unset),
  so **no migration required**.
- Export: add a `gold_nonAtomic` column to the existing flat
  one-row-per-(requirement, annotator) export — no second artifact needed, since
  there is no one-to-many decomposition anymore. Note `rowsToCsv` takes its header
  from the first row's keys, so the new column must be present on **every** row.
- Leave the categorical slot adjudication and `goldOverallIncomplete` exactly as
  they are; keep the two labels (incomplete vs non-atomic) distinct.
- The **Atomicity** tab on the admin dashboard already lists the flagged set with
  each annotator's verdict — the natural place to adjudicate the flag.

**Data note:** working set is the 15 `main` requirements flagged non-atomic. Four
further `main` reqs — `175-Signal`, `4821-Signal`, `5904-Signal`, `7345-Signal` —
carry a `<NON_ATOMIC>` marker in one annotator's conversion text without the box
ticked; the Step-1 singularity definition should decide whether they join the set.

### R5 — Model-side non-atomic detection (flag, not decomposition)  ·  `TODO`  ·  **[coupled with A2]**

**What:** Score whether the model correctly identifies non-atomic requirements.
The model converts each requirement as a single Rimay statement (compound system
response allowed); the non-atomic signal comes from Paska's "Non-atomic
requirement" smell on that Rimay and/or the model's own flag. No decomposition,
no per-unit scoring. (Superseded the earlier detection+decomposition+per-unit
design — see the rejected approach in the top section.)

**Why:** Non-atomicity is a detectable quality smell, and Paska already emits it
on the Rimay output. Measuring detection against the adjudicated `goldNonAtomic`
flag answers the relevant question without a decomposition apparatus.

**Direction (for the implementer to detail):**
- Decide the detection source: Paska's non-atomic smell on the converted Rimay,
  an explicit model flag in the prompt, or both compared. Keep it one clear
  signal per requirement.
- Prompt work in `prompts/` (zsl/fsl/cot) only if an explicit model flag is
  wanted; the singularity criterion, if stated, must match the annotation guide
  verbatim.
- Scoring (`src/scoring/`): one new metric — binary detection (precision / recall
  / F1) of non-atomic over all 50, against `gold_nonAtomic`. The existing
  categorical slot accuracy is unchanged and still applies per requirement.
- `results.json` / `templates/report.html`: a place for the detection metric;
  no decomposition rendering needed.

---

# Perspective — handling the non-atomic cases

> **⚠ SUPERSEDED.** This section argues the full-decomposition approach, which was
> **rejected** after reading the Rimay paper (see the top section: a multi-response
> requirement is directly representable as one Rimay statement, so decomposition
> is unnecessary). Kept only as the record of the reasoning behind the rejected
> path. The live plan and the current A2/R5 are at the top of the file.

> **A starting point, not a settled workflow.** Everything here is a default to
> argue with; the open questions at the end are the ones that actually need
> deciding, and the answers will change A2 and R5.

**1. Keep them in the study.** 15 of 50 in `main` (30%). Excluding them would
leave a benchmark built only from requirements that were already clean, which is
not the population a conversion tool meets in practice — in industrial
specifications non-atomic requirements are the normal case, not an edge case. It
would also remove the capability most worth measuring: noticing that a
requirement cannot be converted as a single unit.

**2. Manual vs. model decomposition is better run as both than chosen between.**

- *Condition A — the model decomposes.* It receives the raw requirement and must
  detect non-atomicity, emit N atomic units, and slot-fill each. This is the
  realistic deployment task.
- *Condition B — the human decomposes first.* The adjudicated units are supplied
  and the model only slot-fills.

Treated as alternatives you have to give one up. Treated as **A plus B as its
control**, the gap between them isolates how much of the end-to-end error is
decomposition and how much is slot-filling — which is the thing you would
otherwise be guessing at. B costs almost nothing extra once the gold
decompositions exist, since A needs them anyway. If only one is run, A is the one
that answers the research question.

**3. Gold decompositions are needed either way.** "Let the model do the
decoupling" changes the task, not the need for a reference: a model that returns
three units cannot be judged without a human decomposition to compare against.
The adjudication session is unavoidable. The only question is whether its output
is handed to the model (B) or held back for scoring (A).

**4. Score three things separately.**

1. **Detection** — binary over all 50: precision / recall / F1 on "is this non-atomic".
2. **Decomposition** — did it find the right units? Start with exact-match on N,
   then unit-level correspondence.
3. **Slot-filling** — the existing categorical metric, per atomic unit.

A single blended score is uninterpretable. A model that splits into three where
gold says two will bleed a decomposition error into the slot metric, and "bad at
slots" becomes indistinguishable from "bad at splitting". Keeping them apart is
what lets you name the bottleneck.

**5. Fix the alignment rule before scoring, not after.** When the model returns
three units and gold has two, something must define which maps to which. The
cheapest defensible starting rule: require units in source-text order and align
by position. The alternative is greedy similarity matching on unit text — more
forgiving, but it needs justifying in the methods. Whichever is chosen goes into
the annotation guide *and* the prompt, so humans and model split under the same
convention.

**6. Report stratified, and keep parents out of the aggregates.** Atomic (35) and
non-atomic (15) as separate lines, never one blended number. Related trap on the
tool side: parents carrying all-`missing` slots will read as *structurally
incomplete* in any aggregate that uses `overallIncomplete` — an artifact of how
the decomposition is stored, not a property of the requirements (see A2).

**7. Write down what "atomic" means before the session.** Everything above rests
on a definition none of the current artifacts state. The working one is "one
system response per requirement", and the borderline cases are what will actually
pin it down. Whatever the panel converges on belongs in the annotation guide and,
verbatim, in the model prompt — otherwise the model is scored against a criterion
it was never given.

**Open questions to settle**

- Is N part of the gold, or is any decomposition that covers the source acceptable?
- How is a partially-correct decomposition scored — all-or-nothing on N, or credit per matched unit?
- Does a wrong split invalidate that requirement's slot scores, or are only matched units scored?
- Should each annotator produce a decomposition (giving inter-annotator agreement
  on *how* to split), or is decomposition adjudication-only? This changes A2's
  data model, so it is worth settling early.
- Do non-atomic parents keep any slot labels at all, or is "no labels + a
  decomposition" the only representation?

---

# Done

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

### A1 — Remove the overall gold standard for the Rimay conversion  ·  `DONE`  ·  **[coupled with R2]**

**Status: `DONE`.** `canonicalRimay` is gone from the model, the adjudication
route, the adjudication UI, the exporter and the docs; the gold is now
categorical only (slot labels + condition type). Current exports no longer carry
the column — `analysis/conversion_similarity.py` skips its gold-reference section
for them and only fires on older files. Existing Atlas documents keep the stale
field harmlessly; Mongoose ignores it.

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

### R4 — LLM verdict stage over the scoring results  ·  `DONE`

**Landed:** Stage 3 is `scripts/run_verdict.py` + `src/verdict.py`, wrapped by
`bin/verdict.sh`. It reads a scored run's `scoring/results.json` and writes the
model's analysis into the `verdict` field, which `templates/report.html` already
renders — no template change was needed.

Shape settled as **both** per-run and per-batch:
- `--run <batch>/<strategy>` writes that run's verdict into its `results.json`.
- `--batch <batch>` does every strategy in the folder, then writes a
  cross-strategy comparison to `outputs/<batch>/verdict.md` (a separate file,
  since it is about all the runs rather than any one of them).
- Stored shape is `{text, model, scope, generated_at, results_digest}`; the
  digest hashes the payload sent, so a verdict left stale by a re-score is
  detectable.

The payload is a digest, not the raw file: full aggregate metrics, one compact
row per requirement, and full text for the worst few. `--model` picks the
analyst (run with `claude-opus-5`), `--dry-run` prints the prompt without
calling the API, `--skip-existing` leaves already-analysed runs alone.
`--temperature` is only sent when explicitly passed, because Opus 5 rejects the
parameter. Stage 3 imports nothing from Stage 1/2 — input is `results.json`
alone — so it re-runs with a different model at any time.

The system prompt guards against a degenerate Track 1: if `support_gold_missing`
is 0 on every slot the analyst is told to report the metric as unmeasurable
rather than as a model failure. First run on `main30` hit exactly that case and
diagnosed it unprompted.

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

# Coupling notes

- **A2 ↔ R5** are the two halves of the non-atomic workflow: the annotation tool
  produces the reference decompositions, RQ1 asks the model for its own and
  scores it against them. The JSON shape of a decomposition should be designed
  once and shared — settle it before either side is built.
- **A1 ↔ R2** are the same conceptual change on both sides of the data handoff:
  the annotation tool stops producing an overall gold conversion, and RQ1 stops
  scoring against one. Land them together (or A1 first, then R2) and re-export
  `data/gold_annotations.csv` for RQ1 after the annotation-tool export shape
  changes.
