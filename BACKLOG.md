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

The human-side playbook for turning the 15 non-atomic `main` requirements into
gold. This is the *process*, decided; the reasoning behind it is in the
**Perspective** section below, and it feeds items **A2** and **R5**. Concept
only — no implementation detail here.

**Decisions locked (the conceptual frame):**
- **Non-atomic ≠ incomplete.** They are orthogonal defects: incomplete = missing
  information; non-atomic = multiple system responses fused. The model emits
  three distinct verdicts — *convertible*, *incomplete*, *non-atomic* — never
  collapsing non-atomic into incomplete. (Collapsing them would repeat the exact
  taxonomy conflation this thesis critiques.)
- **Keeping the slots on a non-atomic requirement requires decomposition** —
  there is no valid single-frame representation of multiple actors/conditions/
  actions. Rimay's atomicity rule *is* "one slot tuple per requirement." So the
  path is detect-and-decompose, not detect-and-stop, because we want the slots on
  these 15.
- **No multi-valued slots, ever.** A slot holding two actions breaks the
  present/implied/missing scheme and breaks Fleiss' Kappa. Decomposition restores
  one value per slot per unit.
- **Parent carries no slots.** A non-atomic requirement is represented as a
  parent (flag only) + N child units, each an ordinary single-valued annotation.

**The gold-record shape (parent → children):**
> one requirement → { is it non-atomic? · if yes, an *ordered* list of units ·
> each unit = one Rimay sentence + five slots + one condition type }.
> Atomic requirements are the N=1 case of the same shape, so the existing 35 are
> unchanged.

**Steps:**

1. **Write the atomicity definition first — blocking.** Anchor to the ISO/IEC/IEEE
   29148 *singular* characteristic (one system response/capability, "stand-alone,
   not grouped"). Operationalise it against the hardest real cases — the four
   borderline reqs (`175-Signal`, `4821-Signal`, `5904-Signal`, `7345-Signal`)
   plus the trickiest of the 15 — until it gives a clean yes/no on each. Output:
   a short atomicity rule + a decided verdict on the four disputed requirements.
2. **Fix the unit shape and split convention before the session.** Confirm the
   parent/children shape above, and set one convention now: **units are listed in
   source-text order** (this is what later lets any two decompositions — human or
   model — be aligned unit-by-unit).
3. **Each annotator decomposes independently, blind.** Every non-atomic
   requirement is split into units and each unit's slots annotated, per annotator.
   This deliberately yields two separable things: the *split* (boundaries) and the
   *slots* inside each unit.
4. **Measure agreement on the split separately from the slots — before
   adjudicating.** Fleiss' Kappa assumes predefined shared units and cannot cope
   with annotators disagreeing on *how many* units exist; use **Krippendorff's
   unitizing alpha** for the segmentation agreement. Also compute plain binary
   agreement on the flag itself ("is this non-atomic?") over all 50 — that is the
   ceiling the model's *detection* can be scored against. Low agreement here means
   the Step 1 definition is still too loose → loop back and tighten it.
5. **Adjudicate to one canonical decomposition per requirement.** Annotators +
   adjudicator reconcile: number and boundaries of units first, then each unit's
   slots. Every non-obvious call becomes a written amendment to the atomicity
   guide (iterative guideline development). Requirements that won't converge are
   explicitly flagged-or-excluded, not forced.
6. **Freeze the gold and the definition together.** The adjudicated
   decompositions are the gold; the finalised atomicity definition is frozen
   alongside them because that exact wording is what the model will later be given
   (and can only fairly be scored against). Parents stay out of every aggregate;
   atomic (35) and non-atomic (15) are always reported as separate lines.

**Open decision to lock before scheduling the session:** do **all three
annotators** decompose, or only the adjudicator? Recommendation: **all three** —
it costs more session time but is the only way to produce the unitizing-alpha
ceiling in Step 4, and measuring the reliability of *how to split* is itself part
of the contribution a reviewer will expect. This choice changes A2's data model,
so settle it early.

*Methodology anchors: ISO/IEC/IEEE 29148 "singular"; Krippendorff's unitizing
alpha (segmentation/unitizing agreement); standard double-annotate-then-adjudicate
gold-standard construction.*

---

# Open

### A2 — Adjudication space for non-atomic requirements  ·  `TODO`  ·  **[coupled with R5]**

**What:** Extend adjudication so a requirement judged non-atomic can carry the
panel's canonical decomposition: the parent holds no slot labels, and N child
units are recorded, each with its own Rimay conversion text, slot labels and
condition type.

**Why:** 15 of the 50 `main` requirements are non-atomic. Rimay admits one system
response per requirement, so these cannot be converted as they stand, and today
they have no representation in the gold beyond a per-annotator boolean. Without a
reference decomposition there is nothing for R5 to be scored against.

**Direction (for the implementer to detail):**
- Additive schema change in `backend/src/models/Adjudication.js`: `goldNonAtomic:
  Boolean` and `goldSplits: [{ rimayText, slots, conditionType }]`. Checked
  read-only against Atlas: existing adjudication documents load and validate
  unchanged under the extended schema (an absent array reads as `[]`), so **no
  migration is required**.
- Guard `goldOverallIncomplete`. It is derived as "any mandatory slot is
  missing" (`backend/src/utils/incompleteness.js`), so a parent deliberately left
  all-`missing` would be written as structurally incomplete. That is a
  decomposition marker, not a finding, and it would contaminate any aggregate.
- Adjudication UI (`frontend/src/app/features/admin/adjudication.component.*`):
  a repeatable sub-form — add/remove unit, conversion text + five slot selects +
  condition type per unit. This is the bulk of the work; the rest is small.
- Export: splits are one-to-many **per requirement** and do not fit the flat
  one-row-per-(requirement, annotator) export. Suggested shape is a second
  artifact, `rimay_splits_<group>.csv`, joined on `reqId`, plus a `gold_nSplits`
  count column on the main export. Note `rowsToCsv` takes its header from the
  first row's keys, so any new column must be present on **every** row.
- The **Atomicity** tab on the admin dashboard already lists the flagged set with
  each annotator's verdict, and is the natural entry point into this flow.

**Data note:** the working set is the 15 `main` requirements flagged via the
non-atomic checkbox. Four further `main` requirements — `175-Signal`,
`4821-Signal`, `5904-Signal`, `7345-Signal` — carry a `<NON_ATOMIC>` marker in one
annotator's conversion text without the box ticked (verified as `main`, not
`pilot`). Whether they join the set is an open call for the panel.

### R5 — Model-side detection and decomposition of non-atomic requirements  ·  `TODO`  ·  **[coupled with A2]**

**What:** Extend the conversion task so the model must decide whether a
requirement is atomic and, when it is not, return the decomposition — N atomic
requirements, each with its own conversion and slots — instead of a single
conversion. Score detection, decomposition and per-unit slot-filling separately.

**Why:** 30% of the `main` set is non-atomic. A pipeline that only converts
pre-cleaned requirements does not answer the research question, and detecting
non-convertibility is arguably the more valuable capability to measure.

**Direction (for the implementer to detail):**
- Design the output JSON contract **once**: the same shape serves as A2's
  `goldSplits` storage and the model's required output. Doing this before either
  side is built avoids a translation layer between them.
- Prompt work in `prompts/` for all three strategies (zsl/fsl/cot): the
  atomicity criterion and the required output shape must both be stated, and the
  criterion must match the one in the annotation guide verbatim.
- Scoring (`src/scoring/`): three separate metrics — detection (binary over all
  50), decomposition (starting with exact-match on N), and the existing
  categorical slot accuracy applied per unit. Do not blend them into one score.
- Fix the alignment rule before scoring (see Perspective §5) and record it in the
  metrics report so the numbers are reproducible.
- `results.json` / `templates/report.html` need a place for the new metrics, and
  the per-requirement drill-down needs to render a decomposition.

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

# Perspective — handling the non-atomic cases

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
