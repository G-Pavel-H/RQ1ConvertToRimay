"""Stage 3 — LLM analysis of the scoring results (BACKLOG R4).

Reads a scored run's ``scoring/results.json``, asks an LLM to interpret the
metrics, and writes the answer back into that file's ``verdict`` field, where
``templates/dashboard.html`` renders it.

Deliberately independent of Stage 1 and Stage 2: the only input is
``results.json``. Nothing here re-converts, calls Paska, or touches the gold
CSV, so it is cheap to re-run with a different model whenever the numbers are
re-scored.

Two scopes:

* **run** — one strategy. "What happened in this run?"
* **batch** — every strategy under one batch folder, compared. "Which strategy
  won, and where do they differ?" Written to ``outputs/<batch>/verdict.md``
  rather than into any single run, since it is about all of them.

The payload sent to the model is a *digest*, not the raw file: full aggregate
metrics, one compact row per requirement, and the full text of a few worst
cases. The raw file carries every requirement's NL text and all human
conversions, which is mostly noise for this task and expensive to send.
"""
from __future__ import annotations

import hashlib
import json
import os
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import List, Optional

from src import config, llm_backend

# How many of the weakest requirements to include with their full text.
N_WORST_EXAMPLES = 6

SYSTEM_PROMPT = """\
You are analysing results from a requirements-engineering experiment.

The experiment converts natural-language feature requests into Rimay, a
controlled natural language for requirements. An LLM performs the conversion
under one of three prompting strategies (zsl = zero-shot, fsl = few-shot,
cot = chain-of-thought). Its output is scored against a human gold standard
produced by three annotators.

The metrics you will see:

TRACK 1 — field accuracy (categorical)
  Each requirement has five structural slots: scope, condition, actor,
  modalVerb, action. The human gold labels each as present / implied / missing.
  The scoring asks a narrower question: when the source requirement was
  genuinely MISSING a slot, did the LLM flag it as missing, or silently invent
  content to fill it? So precision/recall/f1 are computed over the "missing"
  class. A high fill_rate on implied slots is expected and fine. A low
  flag_rate on missing slots means the LLM is fabricating requirements content
  that the source never stated — the central risk this experiment probes.

  CHECK SUPPORT BEFORE INTERPRETING TRACK 1. Every per-slot entry carries
  support_gold_missing. If that is 0 for every slot, there were no
  genuinely-missing slots to detect, so precision/recall/f1 of 0.0 is an
  artifact of absent gold labels and says NOTHING about the model. In that
  case state plainly that Track 1 is not measurable on this data and explain
  why, rather than reporting the model as having failed. The usual cause is
  that the requirements have not been adjudicated yet, leaving the gold_*
  labels empty.

TRACK 2 — conversion quality
  * llm_vs_human similarity: how close the LLM conversion is to each human's.
  * human_human similarity: how close the humans are to EACH OTHER. This is the
    ceiling. Humans do not agree perfectly, so an LLM score at or near the
    human-human number is as good as the task allows; interpret llm_vs_human
    RELATIVE to it, never as an absolute.
  * paska: an independent rule-based smell detector run over the LLM output.
    pass_rate is the share with no smells; smell_frequency counts each type.

TASKS (the "task" field)
  * atomic: one Rimay per requirement.
  * units: a non-atomic requirement; the prompt says so and gives the number of
    units. Each gold unit "<reqId>#<n>" is scored as its own requirement.
  * blind: atomic and non-atomic requirements mixed; the prompt says neither
    which nor how many units. "units" reports count compliance and
    nonAtomicDetection (positive = gold has more than one unit, predicted =
    the model returned more than one).

ANOMALIES (the "anomalies" field) list what the model returned that breaks
the expected shape: format breaches (no answer block, text outside it,
commentary lines removed), wrong unit counts, atomic requirements split,
non-atomic ones merged, empty or truncated answers, Paska errors. In a batch,
"count_disagreements" lists requirements on which the strategies returned
different numbers of units. Treat these as findings: name the reqIds, say
whether they distort the headline metrics, and say whether a pattern is visible.

Your job is interpretation, not restatement. Do not list numbers the reader
can see; cite a number only when it supports a claim. Say what the results
mean, where and how the model fails, and what to do next. Cite reqIds when a
pattern is visible in them. Be calibrated: say when the sample is too small,
when differences are within noise, or when a result is inconclusive.

STYLE. Professional, terse, specific. Every sentence must make a claim, give
evidence for one, or recommend an action — no preamble, no restating the
task, no hedging filler, no closing summary of what you just said. Use the
markdown structure the task prescribes: "## " section headings, short
paragraphs, and "- " bullets only for lists of items (anomalies, next steps).
No tables. At most 400 words; shorter when there is less to say."""

RUN_TASK = """\
Analyse this single run. Write exactly these sections, in this order:

## Summary
Two or three sentences: the main result and the main weakness.
## Missing-slot detection
Track 1: which slots the model flags correctly, which it fills or over-flags.
## Conversion quality
Track 2: similarity relative to the human-human ceiling; what Paska's smells say.
## Units and atomicity
Only for the units and blind tasks: count compliance, splitting and merging
errors, non-atomic detection. Omit the section for the atomic task.
## Anomalies
One bullet per pattern, with the reqIds. "None." if the list is empty.
## Next step
One to three bullets."""

BATCH_TASK = """\
Compare these runs: the same requirements and model under the three prompting
strategies. Write exactly these sections, in this order:

## Summary
Two or three sentences: which strategy is best on what, and whether the gap
matters at this sample size.
## Missing-slot detection
Where the strategies differ on Track 1 and why.
## Conversion quality
Track 2 across strategies, relative to the human-human ceiling.
## Units and atomicity
Only for the units and blind tasks: count compliance and atomicity decisions
per strategy, and the count disagreements between strategies. Omit the
section for the atomic task.
## Anomalies and inconsistencies
One bullet per pattern, with the strategy and reqIds: format breaches, wrong
counts, requirements the strategies treat differently. "None." if nothing.
## Next step
One to three bullets.
If the strategies are indistinguishable, say so rather than declaring a winner."""

CONTEXT_NOTE = """\
Study context. The gold is a curated set of 50 requirements, each converted
by three annotators; the gold slots are the per-unit majority.
* task "atomic" (experiment 1): the 35 atomic requirements, one Rimay each.
  Only 4 are gold-incomplete, so incomplete-F1 moves in large steps.
* task "units" (experiment 2): the 15 non-atomic requirements (42 units); the
  prompt says the requirement is non-atomic and how many units to return.
  All 42 gold units are complete, so incomplete-F1 is undefined there.
* task "blind" (experiment 3): all 50 requirements (77 units); the prompt
  defines atomicity but says neither whether this requirement is non-atomic
  nor how many units to return.
Scope is optional in Rimay and gold-missing almost everywhere, so it dominates
the missing-slot counts."""


@dataclass(frozen=True)
class VerdictResult:
    text: str
    model: str
    scope: str
    digest: str

    def as_dict(self) -> dict:
        return {
            "text": self.text,
            "model": self.model,
            "scope": self.scope,
            "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
            "results_digest": self.digest,
        }


def _requirement_row(req: dict) -> dict:
    """One compact per-requirement line: outcomes only, no prose."""
    sim = req.get("similarity") or {}
    return {
        "reqId": req.get("reqId"),
        "slot_match": req.get("slot_match"),
        "gold_slots": req.get("gold_slots"),
        "llm_slots": req.get("llm_slots"),
        "verdict_match": req.get("verdict_match"),
        "seq_ratio_mean": sim.get("seq_ratio_mean"),
        "paska_passed": req.get("paska_passed"),
        "paska_smells": req.get("paska_smells"),
    }


def _score(req: dict) -> tuple:
    """Rank key for 'worst' — fewest slot matches, then lowest similarity."""
    matches = sum(1 for v in (req.get("slot_match") or {}).values() if v)
    sim = (req.get("similarity") or {}).get("seq_ratio_mean") or 0.0
    return (matches, sim)


def digest_run(results: dict) -> dict:
    """Compact one run's results.json into the payload sent to the model."""
    reqs: List[dict] = results.get("requirements") or []
    worst = sorted(reqs, key=_score)[:N_WORST_EXAMPLES]
    return {
        "run": results.get("run"),
        "strategy": results.get("strategy"),
        "model_under_test": (results.get("meta") or {}).get("model"),
        "counts": results.get("counts"),
        "task": results.get("task"),
        "units": _units_digest(results.get("units")),
        "anomalies": _anomalies_digest(results.get("anomalies")),
        "field_accuracy": results.get("field_accuracy"),
        "conversion_quality": results.get("conversion_quality"),
        "per_requirement": [_requirement_row(r) for r in reqs],
        "worst_examples": [
            {
                "reqId": r.get("reqId"),
                "nl_text": r.get("nl_text"),
                "llm_rimay": r.get("llm_rimay"),
                "human_rimays": r.get("human_rimays"),
                "gold_slots": r.get("gold_slots"),
                "llm_slots": r.get("llm_slots"),
                "paska_smells": r.get("paska_smells"),
            }
            for r in worst
        ],
    }


def _units_digest(units: Optional[dict]) -> Optional[dict]:
    """Unit-count compliance and non-atomicity detection, with the misses only."""
    if not units:
        return None
    misses = [{k: r[k] for k in ("reqId", "expected", "returned")}
              for r in units.get("perRequirement", []) if r["expected"] != r["returned"]]
    keep = ("requirements", "exactCount", "expectedUnits", "returnedUnits", "reordered", "nonAtomicDetection")
    return {**{k: units.get(k) for k in keep}, "countMismatches": misses}


def _anomalies_digest(anomalies: Optional[dict]) -> Optional[dict]:
    if not anomalies:
        return None
    return {"counts": anomalies.get("counts"), "affected": anomalies.get("affected"),
            "kinds": {k: v for k, v in (anomalies.get("kinds") or {}).items() if k in (anomalies.get("counts") or {})},
            "items": anomalies.get("items", [])[:60]}


def count_disagreements(results_list: List[dict]) -> List[dict]:
    """Requirements on which the runs returned different numbers of units."""
    counts: dict = {}
    for r in results_list:
        for q in (r.get("units") or {}).get("perRequirement", []):
            row = counts.setdefault(q["reqId"], {"reqId": q["reqId"], "gold": q["expected"], "returned": {}})
            row["returned"][r.get("strategy")] = q["returned"]
    return [row for row in counts.values()
            if len(set(row["returned"].values())) > 1 or set(row["returned"].values()) != {row["gold"]}]


def digest_batch(results_list: List[dict]) -> dict:
    """Compact several runs for cross-strategy comparison (no per-req detail)."""
    return {
        "count_disagreements": count_disagreements(results_list),
        "batch_runs": [
            {
                "run": r.get("run"),
                "strategy": r.get("strategy"),
                "model_under_test": (r.get("meta") or {}).get("model"),
                "counts": r.get("counts"),
                "task": r.get("task"),
                "units": _units_digest(r.get("units")),
                "anomalies": _anomalies_digest(r.get("anomalies")),
                "field_accuracy": r.get("field_accuracy"),
                "conversion_quality": r.get("conversion_quality"),
            }
            for r in results_list
        ]
    }


def build_prompt(payload: dict, scope: str) -> str:
    task = BATCH_TASK if scope == "batch" else RUN_TASK
    body = json.dumps(payload, ensure_ascii=False, indent=1, default=str)
    return f"{task}\n\n{CONTEXT_NOTE}\n\nRESULTS DATA (JSON):\n{body}"


def generate(
    payload: dict,
    *,
    scope: str,
    model: str,
    max_tokens: int = 2048,
    temperature: Optional[float] = None,
    backend: Optional[str] = None,
) -> VerdictResult:
    """Ask the model to interpret the digested metrics.

    ``temperature`` is only sent when explicitly set: newer models (Opus 5
    among them) reject the parameter outright, and this stage wants to stay
    usable with whatever model the analyst picks.
    """
    prompt = build_prompt(payload, scope)
    completion = llm_backend.complete(
        system=SYSTEM_PROMPT,
        prompt=prompt,
        model=model,
        max_tokens=max_tokens,
        temperature=temperature,
        backend=backend,
    )
    text = completion.text.strip()
    if completion.truncated:
        # Silently storing a half-finished analysis is worse than failing: the
        # verdict reads as complete prose and the cut is only visible if you
        # notice the last sentence stops mid-clause.
        raise RuntimeError(
            f"the analysis hit the {max_tokens}-token limit and was cut off "
            f"mid-sentence ({len(text.split())} words written). Re-run with a "
            f"larger --max-tokens."
        )
    return VerdictResult(
        text=text,
        model=completion.model or model,
        scope=scope,
        digest=hashlib.sha1(
            json.dumps(payload, sort_keys=True, default=str).encode("utf-8")
        ).hexdigest()[:12],
    )


def load_results(path: Path) -> dict:
    if not path.is_file():
        raise FileNotFoundError(f"No results.json at {path} — score the run first.")
    return json.loads(path.read_text(encoding="utf-8"))


def results_path(run: str, outputs_dir: Optional[Path] = None) -> Path:
    root = Path(outputs_dir) if outputs_dir else config.OUTPUTS_DIR
    return root / run / "scoring" / "results.json"


def find_batch_runs(batch: str, outputs_dir: Optional[Path] = None) -> List[Path]:
    """Every scored run directly under ``outputs/<batch>/``."""
    root = Path(outputs_dir) if outputs_dir else config.OUTPUTS_DIR
    return sorted((root / batch).glob("*/scoring/results.json"))


def write_verdict(path: Path, verdict: VerdictResult) -> None:
    """Write the verdict into a run's results.json, leaving the rest intact."""
    data = json.loads(path.read_text(encoding="utf-8"))
    data["verdict"] = verdict.as_dict()
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
