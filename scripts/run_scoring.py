"""Stage 2 entry point: offline scoring against the human gold.

Reads ``outputs/conversions/<strategy>.jsonl`` (the Stage 1 manifest) and
``data/gold_annotations.csv``. All comparison logic lives in
``src/scoring/`` (pure functions); this script does IO only. It never
touches MLflow.

Evaluates only requirements present in both the manifest and the gold,
excluding any FSL exemplar IDs. Writes:

  * ``outputs/scoring/<strategy>/metrics.md``          — Track 1 + Track 2
  * ``outputs/scoring/<strategy>/per_requirement.md``  — review worksheet
  * ``outputs/scoring/comparison.csv``                 — tidy, all strategies

Usage:
    python scripts/run_scoring.py --strategy zsl --gold data/gold_annotations.csv
        [--fsl-example-ids id1,id2] [--out outputs/scoring]
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from pathlib import Path
from typing import Dict, List

THIS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(THIS_DIR.parent))

from src import config  # noqa: E402
from src.gold_loader import load_gold  # noqa: E402
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


# --- markdown helpers --------------------------------------------------------


def _fmt(x: float) -> str:
    return f"{x:.3f}"


def _dist_row(name: str, d: dict) -> str:
    return (
        f"| {name} | {d['n']} | {_fmt(d['mean'])} | {_fmt(d['median'])} | "
        f"{_fmt(d['min'])} | {_fmt(d['max'])} | {_fmt(d['std'])} |"
    )


def render_metrics_md(strategy: str, counts: dict, fa_rep, cqr: dict) -> str:
    L: List[str] = []
    L.append(f"# Scoring metrics — strategy `{strategy}`")
    L.append("")
    L.append(
        f"Gold reqs: {counts['gold']} · converted: {counts['converted']} · "
        f"evaluated: {counts['evaluated']} · skipped: {counts['skipped']}"
    )
    if counts["skipped_ids"]:
        L.append(f"Skipped: {', '.join(counts['skipped_ids'])}")
    L.append("")

    # --- Track 1 ---
    L.append("## Track 1 — field accuracy (missing-detection)")
    L.append("")
    L.append("Binary collapse: gold `present`+`implied` → not-missing; `missing` kept.")
    L.append("P/R/F1 are for the **missing** class (positive = missing).")
    L.append("")
    L.append("### Per-slot")
    L.append("")
    L.append("| Slot | TP | FP | FN | TN | Precision | Recall | F1 | gold-missing support |")
    L.append("|------|----|----|----|----|-----------|--------|----|----------------------|")
    for slot in SLOTS:
        s = fa_rep.per_slot[slot]
        L.append(
            f"| {slot} | {s['tp']} | {s['fp']} | {s['fn']} | {s['tn']} | "
            f"{_fmt(s['precision'])} | {_fmt(s['recall'])} | {_fmt(s['f1'])} | "
            f"{s['support_gold_missing']} |"
        )
    L.append("")
    L.append(
        f"**Micro**: P={_fmt(fa_rep.micro['precision'])} "
        f"R={_fmt(fa_rep.micro['recall'])} F1={_fmt(fa_rep.micro['f1'])}  ·  "
        f"**Macro**: P={_fmt(fa_rep.macro['precision'])} "
        f"R={_fmt(fa_rep.macro['recall'])} F1={_fmt(fa_rep.macro['f1'])}"
    )
    L.append("")

    L.append("### Secondary lenses")
    L.append("")
    imp = fa_rep.implied["overall"]
    mis = fa_rep.missing["overall"]
    L.append(
        f"- Gold **implied** slots (n={imp['n_implied']}): LLM filled "
        f"{imp['llm_filled']} (fill-rate {_fmt(imp['fill_rate'])}), "
        f"over-flagged missing {imp['llm_flagged_missing']}."
    )
    L.append(
        f"- Gold **missing** slots (n={mis['n_missing']}): LLM correctly flagged "
        f"{mis['llm_flagged_missing']} (flag-rate {_fmt(mis['flag_rate'])}), "
        f"silently filled {mis['llm_silently_filled']} (possible compensation)."
    )
    L.append("")

    L.append("### Overall-incomplete verdict")
    L.append("")
    v = fa_rep.verdict
    c = v["confusion"]
    L.append(
        "LLM overall-incomplete = any mandatory slot "
        f"({', '.join(config.MANDATORY_SLOTS)}) flagged missing."
    )
    L.append("")
    L.append(f"- Agreement rate: **{_fmt(v['agreement_rate'])}** (n={v['n']})")
    L.append(
        f"- P/R/F1 (incomplete class): {_fmt(v['precision'])} / "
        f"{_fmt(v['recall'])} / {_fmt(v['f1'])}"
    )
    L.append("")
    L.append("| | gold incomplete | gold complete |")
    L.append("|---|---|---|")
    L.append(f"| **LLM incomplete** | {c['both_incomplete']} | {c['llm_incomplete_gold_complete']} |")
    L.append(f"| **LLM complete** | {c['gold_incomplete_llm_complete']} | {c['both_complete']} |")
    L.append("")

    # --- Track 2 ---
    L.append("## Track 2 — conversion quality")
    L.append("")
    sim = cqr["similarity"]
    lvg, hh = sim["llm_vs_gold"], sim["human_human"]
    L.append(
        f"Similarity metric v0 (placeholder): SequenceMatcher ratio + token Jaccard. "
        f"LLM-vs-gold evaluated on {lvg['n_evaluated']} reqs "
        f"(skipped {lvg['n_skipped_no_canonical']} with empty canonical)."
    )
    L.append("")
    L.append("### Similarity distributions (side by side)")
    L.append("")
    L.append("| Distribution | n | mean | median | min | max | std |")
    L.append("|--------------|---|------|--------|-----|-----|-----|")
    L.append(_dist_row("LLM-vs-gold · seq_ratio", lvg["seq_ratio"]))
    L.append(_dist_row("LLM-vs-gold · jaccard", lvg["jaccard"]))
    L.append(_dist_row("human-human · seq_ratio", hh["seq_ratio"]))
    L.append(_dist_row("human-human · jaccard", hh["jaccard"]))
    L.append("")
    L.append(
        "> The LLM-vs-gold number is only interpretable against the human-human "
        "ceiling (how much annotators vary among themselves)."
    )
    L.append("")
    L.append("### Paska validation (independent structural check)")
    L.append("")
    p = cqr["paska"]
    L.append(
        f"- Pass rate: **{_fmt(p['pass_rate'])}** "
        f"({p['n_passed']}/{p['n_scored']} scored; "
        f"failed {p['n_failed']}, errors {p['n_error']})"
    )
    L.append(f"- Requirements with ≥1 smell: {p['n_with_smells']}")
    if p["smell_frequency"]:
        L.append("")
        L.append("| Smell type | Count |")
        L.append("|------------|-------|")
        for smell, n in p["smell_frequency"].items():
            L.append(f"| {smell} | {n} |")
    else:
        L.append("- No smells fired across the evaluated set.")
    L.append("")
    return "\n".join(L)


def render_per_requirement_md(strategy: str, rows: List[dict]) -> str:
    L: List[str] = [f"# Per-requirement review — strategy `{strategy}`", ""]
    for r in rows:
        L.append(f"## {r['reqId']}")
        L.append("")
        L.append(f"**NL:** {r['nl_text']}")
        L.append("")
        L.append(f"**Gold canonical Rimay:** {r['canonical_rimay'] or '_(empty)_'}")
        L.append("")
        L.append("**Human conversions:**")
        if r["human_rimays"]:
            for hr in r["human_rimays"]:
                L.append(f"- {hr}")
        else:
            L.append("- _(none)_")
        L.append("")
        L.append(f"**LLM Rimay:** {r['llm_rimay']}")
        L.append("")
        L.append("**Slot signals (gold ternary vs LLM binary):**")
        L.append("")
        L.append("| Slot | Gold | LLM | Match |")
        L.append("|------|------|-----|-------|")
        for slot in SLOTS:
            match = "✓" if r["slot_match"][slot] else "✗"
            L.append(
                f"| {slot} | {r['gold_slots'][slot]} | {r['llm_slots'][slot]} | {match} |"
            )
        L.append("")
        L.append(
            f"**Overall verdict:** gold={'incomplete' if r['gold_overall'] else 'complete'} · "
            f"LLM={'incomplete' if r['llm_overall'] else 'complete'} · "
            f"match={'✓' if r['verdict_match'] else '✗'}"
        )
        L.append("")
        L.append(
            f"**Similarity to gold:** seq_ratio={_fmt(r['seq_ratio'])} · "
            f"jaccard={_fmt(r['jaccard'])}"
        )
        L.append("")
        passed = r["paska_passed"]
        passed_str = "error" if passed is None else ("pass" if passed else "FAIL")
        L.append(f"**Paska:** {passed_str}")
        if r["paska_smells"]:
            for s in r["paska_smells"]:
                L.append(f"- {s.get('smell')}: {s.get('value')}")
        L.append("")
        L.append("---")
        L.append("")
    return "\n".join(L)


# --- comparison.csv (all strategies) -----------------------------------------


def comparison_columns() -> List[str]:
    cols = ["strategy", "reqId"]
    for slot in SLOTS:
        cols += [f"gold_{slot}", f"llm_{slot}", f"match_{slot}"]
    cols += [
        "gold_overall_incomplete",
        "llm_overall_incomplete",
        "verdict_match",
        "seq_ratio",
        "jaccard",
        "paska_passed",
    ]
    return cols


def comparison_row(strategy: str, r: dict) -> dict:
    row = {"strategy": strategy, "reqId": r["reqId"]}
    for slot in SLOTS:
        row[f"gold_{slot}"] = r["gold_slots"][slot]
        row[f"llm_{slot}"] = r["llm_slots"][slot]
        row[f"match_{slot}"] = int(r["slot_match"][slot])
    row["gold_overall_incomplete"] = int(r["gold_overall"])
    row["llm_overall_incomplete"] = int(r["llm_overall"])
    row["verdict_match"] = int(r["verdict_match"])
    row["seq_ratio"] = f"{r['seq_ratio']:.4f}"
    row["jaccard"] = f"{r['jaccard']:.4f}"
    row["paska_passed"] = "" if r["paska_passed"] is None else int(r["paska_passed"])
    return row


def write_comparison_csv(path: Path, strategy: str, rows: List[dict]) -> Path:
    """Write this run's tidy per-requirement comparison rows."""
    cols = comparison_columns()
    with path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=cols)
        w.writeheader()
        for r in rows:
            w.writerow(comparison_row(strategy, r))
    return path


# --- main --------------------------------------------------------------------


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description="Offline scoring against gold (Stage 2)")
    p.add_argument(
        "--run",
        required=True,
        help="Run id (folder under outputs/), e.g. run1_zsl.",
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
    args = p.parse_args(argv)

    run_paths = config.RunPaths(args.run)
    if not run_paths.root.is_dir():
        raise SystemExit(
            f"No run folder: {run_paths.root}. "
            "Run scripts/run_conversion.py first, or check --run."
        )
    meta = load_run_meta(run_paths)
    strategy = meta.get("strategy", args.run.split("_")[-1])
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
                canonical_rimay=g.canonical_rimay,
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
        pair = cq.similarity_pair(m["rimay"], g.canonical_rimay)
        per_req_rows.append(
            {
                "reqId": rid,
                "nl_text": g.nl_text,
                "canonical_rimay": g.canonical_rimay,
                "human_rimays": g.human_rimays,
                "llm_rimay": m["rimay"],
                "gold_slots": g.gold_slots,
                "llm_slots": llm_slots,
                "slot_match": slot_match,
                "gold_overall": gold_overall,
                "llm_overall": llm_overall,
                "verdict_match": gold_overall == llm_overall,
                "seq_ratio": pair["seq_ratio"],
                "jaccard": pair["jaccard"],
                "paska_passed": m.get("paska_passed"),
                "paska_smells": m.get("paska_smells", []),
            }
        )

    fa_rep = fa.field_accuracy_report(slot_evals)
    cqr = cq.conversion_quality_report(quality_items)

    counts = {
        "gold": len(gold),
        "converted": len(manifest),
        "evaluated": len(eval_ids),
        "skipped": len(skipped_ids),
        "skipped_ids": skipped_ids,
    }

    scoring_dir = run_paths.scoring_dir
    scoring_dir.mkdir(parents=True, exist_ok=True)
    (scoring_dir / "metrics.md").write_text(
        render_metrics_md(strategy, counts, fa_rep, cqr), encoding="utf-8"
    )
    (scoring_dir / "per_requirement.md").write_text(
        render_per_requirement_md(strategy, per_req_rows), encoding="utf-8"
    )
    write_comparison_csv(scoring_dir / "comparison.csv", strategy, per_req_rows)

    # --- compact stdout summary ---
    lvg = cqr["similarity"]["llm_vs_gold"]["seq_ratio"]
    hh = cqr["similarity"]["human_human"]["seq_ratio"]
    print(f"Scoring — run={args.run} strategy={strategy}")
    print(
        f"  gold={counts['gold']} converted={counts['converted']} "
        f"evaluated={counts['evaluated']} skipped={counts['skipped']}"
    )
    print(f"  macro-F1 (missing-detection): {_fmt(fa_rep.macro['f1'])}")
    print(f"  overall-verdict agreement:   {_fmt(fa_rep.verdict['agreement_rate'])}")
    print(f"  mean LLM-vs-gold seq_ratio:  {_fmt(lvg['mean'])}")
    print(f"  mean human-human seq_ratio:  {_fmt(hh['mean'])}")
    print(f"  Paska pass rate:             {_fmt(cqr['paska']['pass_rate'])}")
    print(f"  outputs: {scoring_dir}/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
