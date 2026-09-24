"""Local curation of the human annotations — state, export, and Paska checks.

The database behind the annotation tool is never touched. Curation works on a
single local file, ``data/curation.json``, which holds every requirement in the
working set (the 30 atomic ones and the 20 non-atomic ones) with, per
annotator, a list of **units**: one atomic Rimay requirement each, with its own
slots. An atomic requirement has one unit; a non-atomic one is decomposed into
several.

Unit order is meaningful. Units are aligned by position across annotators — the
first unit of every annotator is "the same" atomic requirement — which is how a
per-unit gold is derived. Keep the order consistent when editing.

Used by ``scripts/curate.py`` (the browser tool) and
``scripts/paska_annotations.py`` (the command-line Paska run).
"""
from __future__ import annotations

import csv
import hashlib
import json
import os
import tempfile
import uuid
from collections import Counter, OrderedDict
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

from src import config

SLOTS = ["scope", "condition", "actor", "modalVerb", "action"]
SLOT_VALUES = ["present", "implied", "missing"]
CONDITION_TYPES = ["precondition", "trigger", "temporal", "none"]
MANDATORY = list(config.MANDATORY_SLOTS)
TIE_VALUE = "implied"

STATE_PATH = config.DATA_DIR / "curation.json"
EXPORT_DIR = config.DATA_DIR / "curated"
PASKA_PATH = EXPORT_DIR / "paska_annotations.json"

# The exported atomic CSV is a drop-in replacement for the original gold CSV, so
# it keeps exactly the original columns. The units CSV adds two.
UNIT_COLUMNS = ["unit", "nUnits"]


def new_unit_id() -> str:
    return uuid.uuid4().hex[:10]


def now() -> str:
    return datetime.now().astimezone().isoformat(timespec="seconds")


# --- state --------------------------------------------------------------------


def load_state(path: Path = STATE_PATH) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def save_state(state: dict, path: Path = STATE_PATH) -> None:
    """Write atomically, keeping the previous version as ``.bak``.

    A crash or a malformed save must never leave the curation half-written.
    """
    validate_state(state)
    state["savedAt"] = now()
    payload = json.dumps(state, indent=2, ensure_ascii=False)
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        path.with_suffix(".json.bak").write_text(path.read_text(encoding="utf-8"), encoding="utf-8")
    fd, tmp = tempfile.mkstemp(dir=path.parent, prefix=".curation-", suffix=".json")
    with os.fdopen(fd, "w", encoding="utf-8") as fh:
        fh.write(payload)
    os.replace(tmp, path)


def validate_state(state: dict) -> None:
    """Reject anything that would produce a corrupt export."""
    for req in state["requirements"]:
        for ann, a in req["annotations"].items():
            if not a["units"]:
                raise ValueError(f"{req['reqId']} / {ann}: needs at least one unit")
            for u in a["units"]:
                for s in SLOTS:
                    if u["slots"].get(s) not in SLOT_VALUES:
                        raise ValueError(f"{req['reqId']} / {ann}: bad {s}={u['slots'].get(s)!r}")
                if u.get("conditionType", "none") not in CONDITION_TYPES:
                    raise ValueError(f"{req['reqId']} / {ann}: bad conditionType")


# --- gold -----------------------------------------------------------------------


def majority(values: List[str]) -> str:
    """Strict majority, else TIE_VALUE — the rule used for the atomic gold too."""
    counts = Counter(values)
    top = max(counts.values())
    winners = [v for v, n in counts.items() if n == top]
    return winners[0] if len(winners) == 1 else TIE_VALUE


def unit_gold(req: dict, index: int) -> Dict[str, str]:
    """Gold for the unit at ``index``, over the annotators that have one there."""
    units = [a["units"][index] for a in req["annotations"].values() if len(a["units"]) > index]
    return {s: majority([u["slots"][s] for u in units]) for s in SLOTS}


def n_units(req: dict) -> int:
    """How many atomic requirements this one decomposes into (max over annotators)."""
    return max(len(a["units"]) for a in req["annotations"].values())


def is_atomic(req: dict) -> bool:
    return all(len(a["units"]) == 1 for a in req["annotations"].values())


def overall_incomplete(slots: Dict[str, str]) -> bool:
    return any(slots[s] == "missing" for s in MANDATORY)


# --- export ---------------------------------------------------------------------


def _row(req: dict, annotator: str, unit: dict, gold: Dict[str, str], disagreement: bool) -> dict:
    base = dict(req["annotations"][annotator]["base"])
    base["rimayText"] = unit["rimayText"]
    for s in SLOTS:
        base[f"slot_{s}"] = unit["slots"][s]
        base[f"gold_{s}"] = gold[s]
    base["conditionType"] = unit.get("conditionType", "none")
    base["overallIncomplete"] = "true" if overall_incomplete(unit["slots"]) else "false"
    base["gold_overallIncomplete"] = "true" if overall_incomplete(gold) else "false"
    base["gold_hadDisagreement"] = "true" if disagreement else "false"
    return base


def export(state: dict, out_dir: Path = EXPORT_DIR) -> dict:
    """Write the curated corpus as CSV. Returns a summary.

    * ``gold_atomic.csv`` — every requirement where each annotator has exactly
      one unit, in the original 31 columns: a drop-in ``--gold`` for the
      experiment.
    * ``gold_units.csv`` — every requirement that decomposes into more than one
      unit, one row per (requirement, annotator, unit), plus ``unit`` and
      ``nUnits``.
    * ``gold_all_units.csv`` — both of the above in one file, for analysis.
    """
    fieldnames = state["columns"]
    out_dir.mkdir(parents=True, exist_ok=True)
    atomic_rows, unit_rows = [], []

    for req in state["requirements"]:
        n = n_units(req)
        for i in range(n):
            gold = unit_gold(req, i)
            present = [a["units"][i] for a in req["annotations"].values() if len(a["units"]) > i]
            disagreement = any(len({u["slots"][s] for u in present}) > 1 for s in SLOTS)
            for annotator, a in req["annotations"].items():
                if len(a["units"]) <= i:
                    continue
                row = _row(req, annotator, a["units"][i], gold, disagreement)
                if is_atomic(req):
                    atomic_rows.append(row)
                else:
                    unit_rows.append({**row, "unit": i + 1, "nUnits": len(a["units"])})

    def write(path: Path, rows: list, cols: list) -> None:
        with path.open("w", newline="", encoding="utf-8") as fh:
            w = csv.DictWriter(fh, fieldnames=cols, lineterminator="\n", extrasaction="ignore")
            w.writeheader()
            w.writerows(rows)

    write(out_dir / "gold_atomic.csv", atomic_rows, fieldnames)
    write(out_dir / "gold_units.csv", unit_rows, fieldnames + UNIT_COLUMNS)
    write(out_dir / "gold_all_units.csv",
          [{**r, "unit": 1, "nUnits": 1} for r in atomic_rows] + unit_rows,
          fieldnames + UNIT_COLUMNS)

    atomic_reqs = [r for r in state["requirements"] if is_atomic(r)]
    split_reqs = [r for r in state["requirements"] if not is_atomic(r)]
    return {
        "exportedAt": now(),
        "dir": str(out_dir),
        "atomicRequirements": len(atomic_reqs),
        "atomicRows": len(atomic_rows),
        "splitRequirements": len(split_reqs),
        "splitUnits": sum(n_units(r) for r in split_reqs),
        "unitRows": len(unit_rows),
        # Requirements that started in one set and now belong in the other.
        "movedToAtomic": [r["reqId"] for r in atomic_reqs if r["set"] == "non-atomic"],
        "movedToSplit": [r["reqId"] for r in split_reqs if r["set"] == "atomic"],
    }


# --- Paska over the annotations ------------------------------------------------


def _paska_key(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()


def run_paska_on_units(state: dict, results_path: Path = PASKA_PATH, log=print) -> dict:
    """Run every unit's conversion through Paska, the same way the LLM output is.

    Same preprocessing and pass rule as the experiment pipeline: placeholders
    are stripped, and a unit passes when Paska fires no smell. Results are keyed
    by the stripped text, so only units whose text changed since the last run
    are sent — re-checking after a few edits takes seconds, not minutes.

    A unit whose annotator judged a mandatory slot missing is expected to fail
    (what is left after stripping is a fragment), so the summary reports that
    group separately, exactly as the LLM report does.
    """
    # Heavy imports kept local: the browser tool should start instantly.
    from src.llm_converter import strip_missing_placeholders
    from src.paska_runner import run_paska
    from src.pipeline import extract_smells

    cache: Dict[str, dict] = {}
    if results_path.exists():
        try:
            cache = json.loads(results_path.read_text(encoding="utf-8")).get("byText", {})
        except ValueError:
            cache = {}

    units = []  # (reqId, annotator, index, unit, stripped)
    for req in state["requirements"]:
        for annotator, a in req["annotations"].items():
            for i, u in enumerate(a["units"]):
                units.append((req["reqId"], annotator, i, u, strip_missing_placeholders(u["rimayText"]).strip()))

    todo = OrderedDict()
    for _, _, _, u, stripped in units:
        if stripped and _paska_key(stripped) not in cache:
            todo[_paska_key(stripped)] = stripped
    log(f"Paska: {len(units)} units, {len(todo)} new or changed texts to check")

    if todo:
        results = run_paska([(f"u{k}", text) for k, text in todo.items()], source="annotations")
        for k in todo:
            res = results.get(f"u{k}")
            if res is None:
                cache[k] = {"passed": None, "smells": [], "error": "Paska produced no row for this text"}
            else:
                smells = extract_smells(res)
                cache[k] = {"passed": len(smells) == 0, "smells": smells}

    by_unit: Dict[str, dict] = {}
    tally: Dict[str, Counter] = {}
    for req_id, annotator, i, u, stripped in units:
        if not stripped:
            entry = {"passed": None, "smells": [], "note": "no text left after removing placeholders"}
        else:
            entry = dict(cache[_paska_key(stripped)])
        entry["expectedFail"] = overall_incomplete(u["slots"])
        # The text this verdict is about, so the tool can mark it stale once the
        # unit is edited.
        entry["text"] = u["rimayText"]
        by_unit[u["id"]] = entry

        for group in (annotator, f"set:{'atomic' if len(state_req(state, req_id)['annotations'][annotator]['units']) == 1 else 'split'}", "all"):
            c = tally.setdefault(group, Counter())
            if entry["passed"] is None:
                c["skipped"] += 1
            elif entry["expectedFail"]:
                c["incomplete_" + ("pass" if entry["passed"] else "fail")] += 1
            else:
                c["complete_" + ("pass" if entry["passed"] else "fail")] += 1

    summary = {}
    for group, c in tally.items():
        scored = c["complete_pass"] + c["complete_fail"]
        summary[group] = {
            **dict(c),
            # Headline: validity of the units the annotator judged complete.
            "pass_rate_complete": (c["complete_pass"] / scored) if scored else None,
        }

    out = {"ranAt": now(), "byUnit": by_unit, "summary": summary, "byText": cache}
    results_path.parent.mkdir(parents=True, exist_ok=True)
    results_path.write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding="utf-8")
    return out


def state_req(state: dict, req_id: str) -> dict:
    for r in state["requirements"]:
        if r["reqId"] == req_id:
            return r
    raise KeyError(req_id)
