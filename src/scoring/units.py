"""Scoring the units task: a non-atomic requirement converted into N units.

The gold (``data/curated/gold_units.csv``) has one row per (requirement,
annotator, unit), with the gold slots computed per unit position. Each unit
becomes its own scoring subject ``<reqId>#<unit>``, so the atomic scorer, the
report and the verdict stage all work on it unchanged.

The LLM's units come back in its own order, which need not be the gold's. They
are matched to the gold units by the assignment that maximises the summed
similarity to the annotators' texts for each gold unit — cosine when the
embedding service is up, seq_ratio otherwise. A gold unit left without an LLM
unit (the model returned too few) is scored as an empty conversion with every
slot missing; LLM units beyond the gold count are reported, not scored.
"""
from __future__ import annotations

from itertools import permutations
from pathlib import Path
from typing import Dict, List, Tuple

import pandas as pd

from src import config
from src.gold_loader import GoldRecord, records_from_frame
from src.scoring.conversion_quality import similarity_pair


def subject(req_id: str, unit: int) -> str:
    return f"{req_id}#{unit}"


def load_gold_units(path: Path) -> Dict[str, GoldRecord]:
    """``{"<reqId>#<unit>": GoldRecord}`` — one record per gold unit."""
    df = pd.read_csv(path, dtype=str, keep_default_na=False)
    if "unit" not in df.columns:
        raise ValueError(f"{path} is not a units CSV (no 'unit' column)")
    df = df.copy()
    df["reqId"] = df["reqId"] + "#" + df["unit"]
    return records_from_frame(df)


def _similarity(llm: str, humans: List[str]) -> float:
    vals = []
    for h in humans:
        p = similarity_pair(llm, h)
        vals.append(p["cosine"] if p["cosine"] is not None else p["seq_ratio"])
    return sum(vals) / len(vals) if vals else 0.0


def _best_assignment(score: List[List[float]], m: int, n: int) -> Dict[int, int]:
    """gold index -> llm index maximising the summed score (n, m are small)."""
    k = min(m, n)
    best, best_map = -1.0, {}
    for golds in permutations(range(n), k) if m < n else [tuple(range(n))]:
        for llms in permutations(range(m), k):
            total = sum(score[l][g] for g, l in zip(golds, llms))
            if total > best:
                best, best_map = total, dict(zip(golds, llms))
    return best_map


def align(manifest: Dict[str, dict], gold: Dict[str, GoldRecord]) -> Tuple[Dict[str, dict], dict]:
    """Per-unit manifest records keyed like the gold, plus what the matching did."""
    by_req: Dict[str, List[str]] = {}
    for sid in gold:
        by_req.setdefault(sid.rsplit("#", 1)[0], []).append(sid)

    out: Dict[str, dict] = {}
    per_req = []
    for req_id, rec in manifest.items():
        if req_id not in by_req:
            continue
        sids = sorted(by_req[req_id], key=lambda s: int(s.rsplit("#", 1)[1]))
        units = rec.get("units") or []
        m, n = len(units), len(sids)
        score = [[_similarity(u["rimay"], gold[s].human_rimays) for s in sids] for u in units]
        mapping = _best_assignment(score, m, n) if m and n else {}
        for g, sid in enumerate(sids):
            if g in mapping:
                u = units[mapping[g]]
                out[sid] = {"reqId": sid, "rimay": u["rimay"], "rimay_stripped": u["rimay_stripped"],
                            "llm_slots": u["llm_slots"], "paska_passed": u["paska_passed"],
                            "paska_smells": u["paska_smells"], "llm_unit": mapping[g] + 1}
            else:
                out[sid] = {"reqId": sid, "rimay": "", "rimay_stripped": "", "llm_unit": None,
                            "llm_slots": {s: "missing" for s in config.SLOTS},
                            "paska_passed": None, "paska_smells": []}
        used = set(mapping.values())
        per_req.append({
            "reqId": req_id, "expected": n, "returned": m,
            "mapping": {str(g + 1): (mapping[g] + 1 if g in mapping else None) for g in range(n)},
            "reordered": any(mapping.get(g, g) != g for g in range(n)),
            "missingGoldUnits": [g + 1 for g in range(n) if g not in mapping],
            "extraLlmUnits": [units[i]["rimay"] for i in range(m) if i not in used],
        })

    info = {
        "alignment": "max summed similarity to the annotators' units (cosine, seq_ratio fallback)",
        "requirements": len(per_req),
        "exactCount": sum(r["expected"] == r["returned"] for r in per_req),
        "expectedUnits": sum(r["expected"] for r in per_req),
        "returnedUnits": sum(r["returned"] for r in per_req),
        "reordered": sum(r["reordered"] for r in per_req),
        "perRequirement": per_req,
    }
    return out, info
