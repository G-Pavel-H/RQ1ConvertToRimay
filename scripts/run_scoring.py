"""Stage 2 entry point: offline scoring against the human gold.

Reads the Stage 1 manifest (``outputs/<run>/conversions/manifest.jsonl``)
and ``data/gold_annotations.csv``. All comparison logic lives in
``src/scoring/`` (pure functions); this script does IO only. It never
touches MLflow.

Evaluates only requirements present in both the manifest and the gold,
excluding any FSL exemplar IDs. Writes, inside the run folder:

  * ``scoring/results.json``    — every metric + per-requirement detail
  * ``scoring/comparison.csv``  — tidy per-requirement rows for stats tools

It then rebuilds ``outputs/report.html`` over every scored run, so the
report is never stale (``--no-report`` skips that; ``scripts/build_report.py``
rebuilds it on its own).

Usage:
    python scripts/run_scoring.py --run run2/zsl
        [--gold data/gold_annotations.csv] [--fsl-example-ids id1,id2]
        [--no-report]
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from datetime import datetime
from pathlib import Path
from typing import Dict, List

THIS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(THIS_DIR.parent))

from src import config  # noqa: E402
from src.gold_loader import load_gold  # noqa: E402
from src.report import collect_results, render_report  # noqa: E402
from src.scoring import conversion_quality as cq  # noqa: E402
from src.scoring import field_accuracy as fa  # noqa: E402

SLOTS = config.SLOTS


# --- loading -----------------------------------------------------------------


def load_manifest(path: Path) -> Dict[str, dict]:
    if not path.is_file():
        raise FileNotFoundError(
            f"No manifest at {path}. Run scripts/run_conversion.py first."
        )
    records: Dict[str, dict] = {}
    with path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            rec = json.loads(line)
            records[rec["reqId"]] = rec
    return records


def load_run_meta(run_paths: config.RunPaths) -> dict:
    if not run_paths.meta_path.is_file():
        return {}
    return json.loads(run_paths.meta_path.read_text(encoding="utf-8"))


def _default_fsl_ids() -> set[str]:
    """Exemplar ids + source_reqIds to exclude from scoring (the real join key)."""
    path = config.PROMPTS_DIR / "examples" / "fsl_examples.json"
    if not path.is_file():
        return set()
    examples = json.loads(path.read_text(encoding="utf-8"))
    ids: set[str] = set()
    for ex in examples:
        for key in ("id", "source_reqId"):
            if ex.get(key):
                ids.add(str(ex[key]))
    return ids


# --- comparison.csv ----------------------------------------------------------


def comparison_columns() -> List[str]:
    cols = ["strategy", "reqId"]
    for slot in SLOTS:
        cols += [f"gold_{slot}", f"llm_{slot}", f"match_{slot}"]
    cols += [
        "gold_overall_incomplete",
        "llm_overall_incomplete",
        "verdict_match",
        "n_humans",
        "seq_ratio_mean",
        "seq_ratio_max",
        "jaccard_mean",
        "paska_passed",
    ]
    return cols


def comparison_row(strategy: str, r: dict) -> dict:
    row = {"strategy": strategy, "reqId": r["reqId"]}
    for slot in SLOTS:
        row[f"gold_{slot}"] = r["gold_slots"][slot]
        row[f"llm_{slot}"] = r["llm_slots"][slot]
        row[f"match_{slot}"] = int(r["slot_match"][slot])
    sim = r["similarity"]
    row["gold_overall_incomplete"] = int(r["gold_overall"])
    row["llm_overall_incomplete"] = int(r["llm_overall"])
    row["verdict_match"] = int(r["verdict_match"])
    row["n_humans"] = sim["n_humans"]
    row["seq_ratio_mean"] = f"{sim['seq_ratio_mean']:.4f}"
    row["seq_ratio_max"] = f"{sim['seq_ratio_max']:.4f}"
    row["jaccard_mean"] = f"{sim['jaccard_mean']:.4f}"
    row["paska_passed"] = "" if r["paska_passed"] is None else int(r["paska_passed"])
    return row


def write_comparison_csv(path: Path, strategy: str, rows: List[dict]) -> Path:
    """Write this run's tidy per-requirement comparison rows."""
    with path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=comparison_columns())
        w.writeheader()
        for r in rows:
            w.writerow(comparison_row(strategy, r))
    return path


# --- main --------------------------------------------------------------------


def _fmt(x: float) -> str:
    return f"{x:.3f}"


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description="Offline scoring against gold (Stage 2)")
    p.add_argument(
        "--run",
        required=True,
        help="Run folder under outputs/, e.g. run2/zsl.",
    )
    p.add_argument(
        "--gold",
        default=None,
        help="Gold CSV path. Default: the gold_csv recorded in run_meta.json.",
    )
    p.add_argument(
        "--fsl-example-ids",
        default="",
        help="Comma-separated reqIds to exclude (defaults to fsl_examples.json ids).",
    )
    p.add_argument(
        "--no-report",
        action="store_true",
        help="Skip rebuilding outputs/report.html at the end.",
    )
    args = p.parse_args(argv)

    run_paths = config.RunPaths(args.run)
    if not run_paths.root.is_dir():
        raise SystemExit(
            f"No run folder: {run_paths.root}. "
            "Run scripts/run_conversion.py first, or check --run."
        )
    meta = load_run_meta(run_paths)
    strategy = meta.get("strategy") or Path(args.run).name
    gold_path = Path(args.gold) if args.gold else Path(meta.get("gold_csv") or config.GOLD_CSV)

    gold = load_gold(gold_path)
    manifest = load_manifest(run_paths.manifest_path)

    if args.fsl_example_ids.strip():
        fsl_ids = {s.strip() for s in args.fsl_example_ids.split(",") if s.strip()}
    else:
        fsl_ids = _default_fsl_ids()

    eval_ids = [rid for rid in gold if rid in manifest and rid not in fsl_ids]
    skipped_ids = [rid for rid in gold if rid in manifest and rid in fsl_ids]

    # Build scoring inputs + per-req rows in one pass.
    slot_evals: List[fa.SlotEval] = []
    quality_items: List[cq.QualityItem] = []
    per_req_rows: List[dict] = []

    for rid in eval_ids:
        g = gold[rid]
        m = manifest[rid]
        llm_slots = m["llm_slots"]

        slot_evals.append(
            fa.SlotEval(
                req_id=rid,
                gold_slots=g.gold_slots,
                llm_slots=llm_slots,
                gold_overall_incomplete=g.gold_overall_incomplete,
            )
        )
        quality_items.append(
            cq.QualityItem(
                req_id=rid,
                llm_rimay=m["rimay"],
                human_rimays=g.human_rimays,
                paska_passed=m.get("paska_passed"),
                paska_smells=m.get("paska_smells", []),
            )
        )

        slot_match = {
            slot: (fa.llm_is_missing(llm_slots.get(slot, ""))
                   == fa.collapse_gold_missing(g.gold_slots.get(slot, "")))
            for slot in SLOTS
        }
        gold_overall = g.gold_overall_incomplete
        llm_overall = fa.llm_overall_incomplete(llm_slots)
        per_req_rows.append(
            {
                "reqId": rid,
                "nl_text": g.nl_text,
                "human_rimays": g.human_rimays,
                "llm_rimay": m["rimay"],
                "gold_slots": g.gold_slots,
                "llm_slots": llm_slots,
                "slot_match": slot_match,
                "gold_overall": gold_overall,
                "llm_overall": llm_overall,
                "verdict_match": gold_overall == llm_overall,
                "similarity": cq.similarity_to_humans(m["rimay"], g.human_rimays),
                "paska_passed": m.get("paska_passed"),
                "paska_smells": m.get("paska_smells", []),
            }
        )

    fa_rep = fa.field_accuracy_report(slot_evals)
    cqr = cq.conversion_quality_report(quality_items)

    results = {
        "run": args.run,
        "strategy": strategy,
        "scored_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "gold_csv": str(gold_path),
        "meta": meta,
        "counts": {
            "gold": len(gold),
            "converted": len(manifest),
            "evaluated": len(eval_ids),
            "skipped": len(skipped_ids),
            "skipped_ids": skipped_ids,
        },
        "field_accuracy": fa_rep.as_dict(),
        "conversion_quality": cqr,
        "requirements": per_req_rows,
        # Reserved for the future analysis stage: an LLM reads these results
        # and writes its verdict here. The report renders it when present.
        "verdict": None,
    }

    run_paths.scoring_dir.mkdir(parents=True, exist_ok=True)
    run_paths.results_path.write_text(
        json.dumps(results, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    write_comparison_csv(
        run_paths.scoring_dir / "comparison.csv", strategy, per_req_rows
    )

    # Rebuild the report so outputs/report.html always reflects the runs on
    # disk; scoring a run is the only thing that changes what it shows.
    if not args.no_report:
        config.REPORT_HTML.write_text(
            render_report(collect_results()), encoding="utf-8"
        )

    # --- compact stdout summary ---
    counts = results["counts"]
    lvh = cqr["similarity"]["llm_vs_human"]["seq_ratio"]
    hh = cqr["similarity"]["human_human"]["seq_ratio"]
    print(f"Scoring — run={args.run} strategy={strategy}")
    print(
        f"  gold={counts['gold']} converted={counts['converted']} "
        f"evaluated={counts['evaluated']} skipped={counts['skipped']}"
    )
    print(f"  macro-F1 (missing-detection): {_fmt(fa_rep.macro['f1'])}")
    print(f"  overall-verdict agreement:   {_fmt(fa_rep.verdict['agreement_rate'])}")
    print(f"  mean LLM-vs-human seq_ratio: {_fmt(lvh['mean'])}")
    print(f"  mean human-human seq_ratio:  {_fmt(hh['mean'])}")
    print(f"  Paska pass rate:             {_fmt(cqr['paska']['pass_rate'])}")
    print(f"  outputs: {run_paths.scoring_dir}/")
    if not args.no_report:
        print(f"  report:  {config.REPORT_HTML}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
