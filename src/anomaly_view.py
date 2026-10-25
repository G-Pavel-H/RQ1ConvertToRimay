"""Anomaly viewer: every requirement of an experiment, across its 9 runs, with the full replies.

Reads only what the runs already saved (``conversions/manifest.jsonl`` and
``scoring/results.json``); nothing is converted or re-scored. Injects the data
into ``templates/anomalies.html`` and writes ``outputs/anomalies.html``.
Built for Experiment 3 (blind); Experiment 2 works the same way.
"""
from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path
from typing import Optional

from src import config

TEMPLATE = config.TEMPLATES_DIR / "anomalies.html"
OUT = config.OUTPUTS_DIR / "anomalies.html"
_PLACEHOLDER = "__ANOMALY_DATA__"
_PREFIX = {2: "exp2_units", 3: "exp3_blind"}
MODELS = ("haiku", "sonnet", "opus")
STRATS = ("zsl", "fsl", "cot")


def _manifest(path: Path) -> dict:
    out = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.strip():
            rec = json.loads(line)
            out[rec["reqId"]] = rec
    return out


def collect(exp: int = 3, outputs_dir: Optional[Path] = None) -> dict:
    root = Path(outputs_dir) if outputs_dir else config.OUTPUTS_DIR
    runs, reqs, cells = [], {}, {}
    for m in MODELS:
        for s in STRATS:
            run_dir = root / f"{_PREFIX[exp]}_{m}" / s
            res_p, man_p = run_dir / "scoring" / "results.json", run_dir / "conversions" / "manifest.jsonl"
            if not (res_p.exists() and man_p.exists()):
                continue
            key = f"{m}/{s}"
            R = json.loads(res_p.read_text(encoding="utf-8"))
            man = _manifest(man_p)
            an = R.get("anomalies") or {"items": [], "kinds": {}}
            runs.append({"key": key, "model": m, "strat": s, "modelId": (R.get("meta") or {}).get("model"),
                         "counts": an.get("counts", {}), "kinds": an.get("kinds", {})})
            # Gold units: one scored row per "<reqId>#<k>".
            scored = {}
            for q in R["requirements"]:
                rid, _, k = q["reqId"].partition("#")
                scored.setdefault(rid, {})[int(k or 1)] = q
                g = reqs.setdefault(rid, {"nl": q["nl_text"], "units": {}})
                g["units"].setdefault(int(k or 1), {"humans": q["human_rimays"], "slots": q["gold_slots"],
                                                    "overall": q["gold_overall"]})
            items = {}
            for i in an.get("items", []):
                items.setdefault(i["reqId"], []).append({"kind": i["kind"], "detail": i.get("detail", "")})
            for u in R["units"]["perRequirement"]:
                rid = u["reqId"]
                rec = man.get(rid, {})
                cells.setdefault(rid, {})[key] = {
                    "expected": u["expected"], "returned": u["returned"],
                    "mapping": u["mapping"], "missing": u["missingGoldUnits"],
                    "cos": {k: q["similarity"].get("cosine_mean") for k, q in scored.get(rid, {}).items()},
                    "units": [{"rimay": x.get("rimay", ""), "slots": x.get("llm_slots", {}),
                               "paska": x.get("paska_passed"), "smells": [sm.get("smell") for sm in x.get("paska_smells", [])],
                               "nonAtomic": x.get("is_non_atomic")} for x in rec.get("units", [])],
                    "raw": rec.get("raw", ""), "format": rec.get("format") or {},
                    "stop": rec.get("stop_reason"), "outTok": rec.get("output_tokens"),
                    "latency": rec.get("latency_ms"), "anomalies": items.get(rid, []),
                }
    for g in reqs.values():
        g["units"] = [g["units"][k] for k in sorted(g["units"])]
    return {"exp": exp, "generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
            "runs": runs, "reqs": reqs, "cells": cells}


def build(exp: int = 3, out: Optional[Path] = None) -> Path:
    out = Path(out) if out else (OUT if exp == 3 else config.OUTPUTS_DIR / f"anomalies_exp{exp}.html")
    data = json.dumps(collect(exp), ensure_ascii=False).replace("</", "<\\/")
    out.write_text(TEMPLATE.read_text(encoding="utf-8").replace(_PLACEHOLDER, data), encoding="utf-8")
    return out
