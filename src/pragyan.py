"""Our incompleteness verdicts against Pragyan et al.'s (2025) on the same requests.

Pragyan et al. label a feature request incomplete when it lacks "crucial
information needed to fully describe the intended system behavior"; their
annotators describe what is missing in free text and as clarification
questions, with no span and no sub-class. Ours is structural: a requirement is
incomplete when a slot the Rimay grammar requires (actor, modal verb, action)
is gold-missing.

To make the difference measurable, each of their clarification questions is
coded by what it asks for (``CODES``). CL pre-codes every question
(``CL_CODES``); the coder confirms or changes them in ``scripts/pragyan.py``,
and the result lives in ``data/pragyan_coding.json``.
"""
from __future__ import annotations

import csv
import io
import json
import shutil
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional

import pandas as pd

from src import config

CORPUS = config.DATA_DIR / "pragyan" / "100-FeatureRequests-Corpus-Reconciled.csv"
GOLD = config.DATA_DIR / "curated" / "gold_all_units.csv"
CODING_PATH = config.DATA_DIR / "pragyan_coding.json"
REQUIRED = ("actor", "modalVerb", "action")

# What a clarification question asks for. "level" groups them for the argument:
# requirement = a slot Rimay requires; optional = a slot Rimay leaves optional;
# design = detail a developer decides later; other = not about the behaviour.
CODES = {
    "req_action":      {"level": "requirement", "label": "What is requested (action)",
                        "tip": "No recoverable system action: the request itself is unclear. Rimay's required action slot."},
    "req_actor":       {"level": "requirement", "label": "Who performs it (actor)",
                        "tip": "Which system or component must act. Rimay's required actor slot."},
    "opt_applicability": {"level": "optional", "label": "Who / what it applies to",
                        "tip": "Which users, groups, messages it covers — Rimay's optional scope slot."},
    "opt_condition":   {"level": "optional", "label": "When it applies (condition)",
                        "tip": "The trigger or precondition — Rimay's optional condition slot."},
    "ui_access":       {"level": "design", "label": "Where in the UI",
                        "tip": "Where the feature is accessed, enabled or shown."},
    "ui_presentation": {"level": "design", "label": "What it looks like",
                        "tip": "Visual form: icons, colours, layout."},
    "behaviour":       {"level": "design", "label": "How it works in detail",
                        "tip": "Design choices for the behaviour: mechanism, steps, options."},
    "configuration":   {"level": "design", "label": "Default / optional / settings",
                        "tip": "On by default or opt-in, toggles, granularity of settings."},
    "parameter":       {"level": "design", "label": "Values and limits",
                        "tip": "Sizes, durations, thresholds, defaults."},
    "edge_case":       {"level": "design", "label": "Edge cases and failures",
                        "tip": "Errors, exceptions, recovery, alternative flows."},
    "rationale":       {"level": "other", "label": "Why (rationale)",
                        "tip": "Motivation or benefit — not part of the behaviour."},
    "terminology":     {"level": "other", "label": "Meaning of a term",
                        "tip": "What a word in the request means (closer to ambiguity)."},
    "requester_context": {"level": "other", "label": "Requester's own setup",
                        "tip": "How the requester uses the app today."},
}
LEVELS = {"requirement": "Required Rimay slot", "optional": "Optional Rimay slot",
          "design": "Design detail", "other": "Not behaviour"}

# Per requirement: the coder's judgement of Pragyan's "incomplete" label.
JUDGEMENTS = {
    "genuine": "Genuinely incomplete — a required part of the requirement is missing",
    "optional_only": "Only optional information (scope / condition) is missing",
    "design_detail": "Asks for design detail, not missing requirement content",
    "mixed": "Mixed: some required, mostly design detail",
    "unsure": "Unsure",
}

# CL's first-pass coding: one code per clarification question, in order.
CL_CODES: Dict[str, List[str]] = {
    "4031-Signal": ["ui_access", "behaviour"],
    "4333-Signal": ["configuration", "ui_access", "rationale"],
    "455-Signal": ["configuration", "configuration", "behaviour"],
    "107-Mastodon": ["ui_presentation"],
    "83-Signal": ["req_action", "req_action"],
    "801-Signal": ["ui_access", "configuration", "opt_applicability", "behaviour", "behaviour"],
    "1363-Signal": ["ui_presentation"],
    "6457-Signal": ["behaviour"],
    "828-Signal": ["opt_applicability", "behaviour", "behaviour"],
    "245-Signal": ["req_action"],
    "1761-Signal": ["configuration", "ui_access", "behaviour"],
    "1504-Signal": ["req_action", "req_action"],
    "8-Mastodon": ["ui_access", "behaviour", "terminology"],
    "592-Signal": ["req_action"],
    "6031-Signal": ["configuration", "behaviour", "edge_case"],
    "496-Signal": ["ui_access", "opt_applicability", "parameter", "behaviour", "rationale"],
    "4230-Signal": ["behaviour", "configuration", "configuration", "behaviour"],
    "757-Signal": ["ui_access", "behaviour", "opt_applicability", "opt_applicability"],
    "1283-Signal": ["behaviour", "behaviour"],
    "7310-Signal": ["behaviour", "ui_presentation", "behaviour"],
    "42-Mastodon": ["ui_access"],
    "175-Signal": ["ui_access"],
    "2055-Signal": ["edge_case"],
    "6390-Signal": ["ui_access", "configuration"],
    "222-Signal": ["ui_access", "behaviour"],
    "6111-Signal": ["behaviour"],
    "6362-Signal": ["ui_access", "behaviour", "opt_applicability"],
    "7567-Signal": ["requester_context", "edge_case"],
    "802-Signal": ["terminology", "behaviour"],
}


def _cl_judgement(codes: List[str]) -> str:
    levels = {CODES[c]["level"] for c in codes}
    if levels == {"requirement"}:
        return "genuine"
    if "requirement" in levels:
        return "mixed"
    if "design" in levels:
        return "design_detail"
    return "optional_only" if "optional" in levels else "unsure"


# ---------------------------------------------------------------- loading
def load_corpus(path: Path = CORPUS) -> Dict[str, dict]:
    df = pd.read_csv(path, dtype=str, keep_default_na=False)
    out = {}
    for _, row in df.iterrows():
        rid = row["RequestNumber-App"].strip()
        raw = row["Annotation"].strip()
        anns = json.loads(raw) if raw.startswith("[") else []
        counts = row["FinalDefectCount"]
        n_inc = int(counts.split("'Incompleteness':")[1].split("}")[0]) if "'Incompleteness'" in counts else 0
        items = []
        for a in anns:
            if a.get("category") != "Incompleteness":
                continue
            items.append({"comment": a.get("comment", "").strip(),
                          "questions": [q.strip() for q in a.get("question", "").split(";") if q.strip()],
                          "interpretation": a.get("interpretation", "").strip(),
                          "affectsDownstream": a.get("affectsDownstream"),
                          "affectDescription": a.get("affectDescription", "").strip(),
                          "text": a.get("text")})
        out[rid] = {"incomplete": n_inc > 0, "defectType": row["FinalDefectType"].strip(),
                    "ambiguities": sum(1 for a in anns if a.get("category") == "Ambiguity"), "items": items}
    return out


def _gold_requirements(path: Path = GOLD) -> List[dict]:
    """One record per requirement: our gold per unit, annotators' texts, verdict."""
    df = pd.read_csv(path, dtype=str, keep_default_na=False)
    reqs = []
    for rid, grp in df.groupby("reqId", sort=False):
        first = grp.iloc[0]
        units = []
        for unit, ug in grp.groupby("unit", sort=True):
            u0 = ug.iloc[0]
            slots = {s: u0[f"gold_{s}"] for s in config.SLOTS}
            units.append({"unit": int(unit), "slots": slots,
                          "missingRequired": [s for s in REQUIRED if slots[s] == "missing"],
                          "incomplete": u0["gold_overallIncomplete"] == "true",
                          "annotators": [{"name": r["annotatorDisplayName"], "rimay": r["rimayText"]}
                                         for _, r in ug.iterrows()]})
        reqs.append({"reqId": rid, "group": "atomic" if int(first["nUnits"]) == 1 else "non-atomic",
                     "nUnits": int(first["nUnits"]), "nl": first["nlText"], "description": first["nlDescription"],
                     "units": units, "oursIncomplete": any(u["incomplete"] for u in units)})
    return reqs


def _exp1_verdicts(outputs_dir: Path = config.OUTPUTS_DIR) -> Dict[str, Dict[str, bool]]:
    """{reqId: {"opus/fsl": llm says incomplete}} from every scored experiment-1 run."""
    out: Dict[str, Dict[str, bool]] = {}
    for path in sorted(outputs_dir.glob("exp1_atomic_*/*/scoring/results.json")):
        run = f"{path.parent.parent.parent.name.split('_')[-1]}/{path.parent.parent.name}"
        for q in json.loads(path.read_text(encoding="utf-8"))["requirements"]:
            out.setdefault(q["reqId"], {})[run] = bool(q["llm_overall"])
    return out


# ---------------------------------------------------------------- coding state
def load_coding(path: Path = CODING_PATH) -> dict:
    """The coding file, created from CL's first pass when absent."""
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    state = {"savedAt": None, "requirements": {}}
    for rid, codes in CL_CODES.items():
        state["requirements"][rid] = {"codes": list(codes), "judgement": _cl_judgement(codes),
                                      "note": "", "source": "CL"}
    save_coding(state, path)
    return state


def save_coding(state: dict, path: Path = CODING_PATH) -> None:
    for rid, rec in state.get("requirements", {}).items():
        bad = [c for c in rec.get("codes", []) if c and c not in CODES]
        if bad or (rec.get("judgement") and rec["judgement"] not in JUDGEMENTS):
            raise ValueError(f"{rid}: unknown code or judgement {bad or rec.get('judgement')}")
    if path.exists():
        shutil.copyfile(path, path.with_suffix(".json.bak"))
    state["savedAt"] = datetime.now().astimezone().isoformat(timespec="seconds")
    path.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------- statistics
def cohen_kappa(a: List[bool], b: List[bool]) -> Optional[float]:
    n = len(a)
    if not n:
        return None
    po = sum(x == y for x, y in zip(a, b)) / n
    pa, pb = sum(a) / n, sum(b) / n
    pe = pa * pb + (1 - pa) * (1 - pb)
    return None if pe == 1 else (po - pe) / (1 - pe)


def _cross(reqs: List[dict], key_a: str, key_b: str) -> dict:
    a = [r[key_a] for r in reqs]
    b = [r[key_b] for r in reqs]
    return {"n": len(reqs),
            "both": sum(x and y for x, y in zip(a, b)), "onlyA": sum(x and not y for x, y in zip(a, b)),
            "onlyB": sum(y and not x for x, y in zip(a, b)), "neither": sum(not x and not y for x, y in zip(a, b)),
            "agreement": (sum(x == y for x, y in zip(a, b)) / len(a)) if a else None,
            "kappa": cohen_kappa(a, b)}


def view() -> dict:
    """Everything the page needs."""
    corpus = load_corpus()
    coding = load_coding()
    llm = _exp1_verdicts()
    reqs = _gold_requirements()
    for r in reqs:
        p = corpus[r["reqId"]]
        r["pragyan"] = p
        r["pragyanIncomplete"] = p["incomplete"]
        r["llm"] = llm.get(r["reqId"], {})
        r["coding"] = coding["requirements"].get(r["reqId"])

    stats = {}
    for name, subset in (("atomic", [r for r in reqs if r["group"] == "atomic"]),
                         ("non-atomic", [r for r in reqs if r["group"] == "non-atomic"]), ("all", reqs)):
        stats[name] = _cross(subset, "pragyanIncomplete", "oursIncomplete")
    runs = sorted({k for r in reqs for k in r["llm"]})
    atomic = [r for r in reqs if r["group"] == "atomic"]
    stats["llm"] = []
    for run in runs:
        rows = [r for r in atomic if run in r["llm"]]
        llm_inc = [r["llm"][run] for r in rows]
        stats["llm"].append({"run": run, "n": len(rows), "flagged": sum(llm_inc),
                             "vsOurs": cohen_kappa(llm_inc, [r["oursIncomplete"] for r in rows]),
                             "vsPragyan": cohen_kappa(llm_inc, [r["pragyanIncomplete"] for r in rows]),
                             "agreeOurs": sum(x == r["oursIncomplete"] for x, r in zip(llm_inc, rows)) / len(rows),
                             "agreePragyan": sum(x == r["pragyanIncomplete"] for x, r in zip(llm_inc, rows)) / len(rows)})
    return {"requirements": reqs, "stats": stats, "codes": CODES, "levels": LEVELS, "judgements": JUDGEMENTS,
            "savedAt": coding.get("savedAt"),
            "pragyanReportedKappa": [0.65, 0.40, 0.87],
            "definitions": {
                "pragyan": "Incomplete feature requests are those that lack crucial information needed to fully "
                           "describe the intended system behavior (Pragyan et al., 2025, citing [8]). Annotators "
                           "describe what is missing in free text; no span, no sub-class.",
                "ours": "A requirement is incomplete when a slot the Rimay grammar requires — actor, modal verb or "
                        "action — is missing (gold = per-unit majority of three annotators). Scope and condition "
                        "are optional and never make a requirement incomplete."}}


def export_csv() -> str:
    """One row per clarification question, with both verdicts and its code."""
    data = view()
    buf = io.StringIO()
    w = csv.writer(buf)
    w.writerow(["reqId", "group", "pragyan_incomplete", "ours_incomplete", "ours_missing_required",
                "judgement", "coding_source", "question_no", "question", "code", "code_level", "pragyan_comment"])
    for r in data["requirements"]:
        c = r["coding"] or {}
        missing = sorted({s for u in r["units"] for s in u["missingRequired"]})
        qs = [(item["comment"], q) for item in r["pragyan"]["items"] for q in item["questions"]]
        if not qs:
            w.writerow([r["reqId"], r["group"], r["pragyanIncomplete"], r["oursIncomplete"], " ".join(missing),
                        c.get("judgement", ""), c.get("source", ""), "", "", "", "", ""])
        for i, (comment, q) in enumerate(qs):
            code = (c.get("codes") or [])[i] if i < len(c.get("codes") or []) else ""
            w.writerow([r["reqId"], r["group"], r["pragyanIncomplete"], r["oursIncomplete"], " ".join(missing),
                        c.get("judgement", ""), c.get("source", ""), i + 1, q, code,
                        CODES[code]["level"] if code else "", comment])
    return buf.getvalue()
