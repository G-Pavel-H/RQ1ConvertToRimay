"""Paska smell-detection wrapper.

Two phases:

  1. Build constituency-parsing trees in-process via :mod:`src.parsing_trees`
     (stanza, modern Python). Replaces Paska's original
     ``get_cparsingtrees.py``, which required Python 3.8 + allennlp 2.10.1.
  2. Run ``java -jar smell_detector.jar`` against the parsing-trees CSV.
     This step is unchanged from upstream Paska — same jar, same Stanford
     POS tagger, same output schema.

Provenance-agnostic: takes ``(req_id, text)`` tuples. In this repo
Paska runs exactly once per requirement, on the placeholder-stripped
LLM Rimay (never on the NL).

Caching: the input CSV's content is hashed (SHA-256). If the cache
already contains the smell CSV for that hash, the parser and Java step
are skipped.
"""
from __future__ import annotations

import csv
import hashlib
import shutil
import subprocess
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

import mlflow

from src import config
from src.parsing_trees import write_parsing_trees_csv

PASKA_SMELL_COLUMNS: Tuple[str, ...] = (
    "Req ID",
    "Segment ID",
    "Req Segment",
    "Non-atomic requirement",
    "Incomplete requirement",
    "Incorrect order requirement",
    "Coordination ambiguity",
    "Not requirement",
    "Incomplete condition",
    "Incomplete system response",
    "Passive voice",
    "Not precise verb",
    "Rimay Pattern ",
)

SMELL_COLUMNS: Tuple[str, ...] = tuple(
    c for c in PASKA_SMELL_COLUMNS if c not in {"Req ID", "Segment ID", "Req Segment"}
)
RIMAY_PATTERN_COL = "Rimay Pattern "


@dataclass
class PaskaResult:
    """Per-requirement Paska output."""

    req_id: str
    segments: List[Dict[str, str]] = field(default_factory=list)

    @property
    def smell_count(self) -> int:
        n = 0
        for seg in self.segments:
            for col in SMELL_COLUMNS:
                if col == RIMAY_PATTERN_COL:
                    continue
                if (seg.get(col) or "").strip():
                    n += 1
        return n

    @property
    def suggested_rimay_patterns(self) -> List[str]:
        out: List[str] = []
        for seg in self.segments:
            v = (seg.get(RIMAY_PATTERN_COL) or "").strip()
            if v:
                out.append(v)
        return out

    def to_dict(self) -> dict:
        return {
            "req_id": self.req_id,
            "smell_count": self.smell_count,
            "suggested_rimay_patterns": self.suggested_rimay_patterns,
            "segments": self.segments,
        }


def _hash_csv_content(rows: Sequence[Tuple[str, str]]) -> str:
    h = hashlib.sha256()
    for req_id, text in rows:
        h.update(req_id.encode("utf-8"))
        h.update(b"\x1f")
        h.update(text.encode("utf-8"))
        h.update(b"\x1e")
    return h.hexdigest()


def _run(cmd: list[str], log_prefix: str) -> None:
    print(f"[paska] {log_prefix}: {' '.join(cmd)}")
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.stdout:
        print(proc.stdout, end="" if proc.stdout.endswith("\n") else "\n")
    if proc.stderr:
        print(proc.stderr, end="" if proc.stderr.endswith("\n") else "\n")
    if proc.returncode != 0:
        raise RuntimeError(f"{log_prefix} failed with exit code {proc.returncode}")


def _parse_smells_csv(path: Path) -> Dict[str, PaskaResult]:
    results: Dict[str, PaskaResult] = {}
    with path.open(newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter=";")
        for row in reader:
            req_id = (row.get("Req ID") or "").strip()
            if not req_id:
                continue
            results.setdefault(req_id, PaskaResult(req_id=req_id))
            results[req_id].segments.append({k: (v or "") for k, v in row.items()})
    return results


@mlflow.trace(name="paska.run", span_type="TOOL")
def run_paska(
    items: Iterable[Tuple[str, str]],
    *,
    source: str = "rimay",
    pos_tagger_path: Optional[str] = None,
    workdir_name: Optional[str] = None,
    use_cache: bool = True,
) -> Dict[str, PaskaResult]:
    """Run Paska on the supplied (req_id, text) tuples.

    Returns a mapping req_id -> PaskaResult. Paska may segment one
    requirement into multiple rows; segments are collected per req_id.

    ``source`` is a free-form label attached as a span input for the
    MLflow Traces UI (always ``"rimay"`` in this repo — Paska never
    runs on the NL here).
    """
    items_list = list(items)
    if not items_list:
        return {}

    pos_tagger = pos_tagger_path or config.PASKA_POS_TAGGER_PATH
    if not pos_tagger:
        raise RuntimeError(
            "PASKA_POS_TAGGER_PATH is not set. Download "
            "english-left3words-distsim.tagger from "
            "https://nlp.stanford.edu/software/tagger.shtml#Download "
            "and set the env var."
        )
    if not Path(pos_tagger).exists():
        raise RuntimeError(f"POS tagger not found at: {pos_tagger}")

    config.ensure_output_dirs()
    digest = _hash_csv_content(items_list)
    cache_dir = config.PASKA_SMELLS_CACHE_DIR / digest
    cache_smells_csv = cache_dir / "smells.csv"

    if use_cache and cache_smells_csv.is_file():
        print(f"[paska] cache hit: {cache_dir.relative_to(config.PROJECT_ROOT)}")
        return _parse_smells_csv(cache_smells_csv)

    workdir_name = workdir_name or f"run_{digest[:12]}_{int(time.time())}"
    parsing_dir = config.PASKA_PARSING_TREES_DIR / workdir_name
    smells_dir = config.PASKA_SMELLS_DIR / workdir_name
    parsing_dir.mkdir(parents=True, exist_ok=True)
    smells_dir.mkdir(parents=True, exist_ok=True)

    parsing_csv = parsing_dir / "parsing-trees.csv"
    print(f"[paska] step 1: parsing trees (stanza) → {parsing_csv}")
    with mlflow.start_span(name="paska.parsing_trees", span_type="PARSER") as span:
        span.set_inputs({"n_items": len(items_list)})
        write_parsing_trees_csv(items_list, parsing_csv)
        span.set_outputs({"output_csv": str(parsing_csv)})

    with mlflow.start_span(name="paska.smell_detector", span_type="TOOL") as span:
        span.set_inputs({"jar": str(config.PASKA_JAR), "parsing_csv": str(parsing_csv)})
        _run(
            [
                "java",
                "-jar",
                str(config.PASKA_JAR),
                str(parsing_dir),
                str(smells_dir),
                str(pos_tagger),
            ],
            "step 2 (smell detector)",
        )
        smells_csvs = list(smells_dir.glob("*.csv"))
        if not smells_csvs:
            raise RuntimeError(f"Paska produced no CSV in {smells_dir}; check stderr above")
        smells_csv = smells_csvs[0]
        span.set_outputs({"smells_csv": str(smells_csv)})

    cache_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(smells_csv, cache_smells_csv)
    return _parse_smells_csv(smells_csv)


def main() -> None:
    sample = [
        (
            "RQSVV.024",
            "If System-A has successfully performed all the validation rules, "
            'then System-A must set the state of the Settlement Request to "Valid".',
        )
    ]
    results = run_paska(sample, use_cache=False)
    for req_id, res in results.items():
        print(f"=== {req_id} ===")
        print(f"  segments: {len(res.segments)}")
        print(f"  smell_count: {res.smell_count}")
        print(f"  suggested patterns: {res.suggested_rimay_patterns}")


if __name__ == "__main__":
    main()
