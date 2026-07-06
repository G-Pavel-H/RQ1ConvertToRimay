"""Stage 2 entry point: offline scoring against the human gold standard.

Reads ``outputs/conversions/<strategy>.jsonl`` (Stage 1 manifest) plus
``data/gold_annotations.csv``; all comparison logic is pure functions
in ``src/scoring/``. Never touches MLflow.

Evaluates only requirements present in BOTH the gold CSV and the
manifest, excluding FSL exemplar IDs (defaults to the ``id`` fields in
``prompts/examples/fsl_examples.json``; override/extend with
``--fsl-example-ids``). Reports the counts.

Writes:
  outputs/scoring/<strategy>/metrics.md          Track 1 + Track 2 tables
  outputs/scoring/<strategy>/per_requirement.md  manual-review worksheet
  outputs/scoring/comparison.csv                 tidy, ALL strategies —
      rebuilt from every manifest in outputs/conversions/*.jsonl on
      each invocation, so running any one strategy refreshes the file.

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
from typing import Dict, List, Optional, Tuple

THIS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(THIS_DIR.parent))

from src import config  # noqa: E402
from src.gold_loader import GoldRequirement, load_gold  # noqa: E402
from src.scoring import field_accuracy as fa  # noqa: E402
from src.scoring import conversion_quality as cq  # noqa: E402


# --------------------------------------------------------------------------
# IO helpers
# --------------------------------------------------------------------------


def load_manifest(path: Path) -> Dict[str, dict]:
    """Read a Stage 1 JSONL manifest into an ordered reqId -> row dict."""
    if not path.is_file():
        raise FileNotFoundError(
            f"Manifest not found: {path}. Run Stage 1 first "
            f"(scripts/run_conversion.py)."
        )
    rows: Dict[str, dict] = {}
    with path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            row = json.loads(line)
            rows[str(row["reqId"])] = row
    return rows


def default_fsl_exemplar_ids() -> set:
    if not config.FSL_EXAMPLES_JSON.is_file():
        return set()
    examples = json.loads(config.FSL_EXAMPLES_JSON.read_text(encoding="utf-8"))
    return {str(ex.get("id")) for ex in examples if ex.get("id")}


def select_evaluated(
    gold: Dict[str, GoldRequirement],
    manifest: Dict[str, dict],
    exemplar_ids: set,
) -> Tuple[List[str], List[str]]:
    """Return (evaluated reqIds, skipped-as-exemplar reqIds), gold order."""
    evaluated, skipped = [], []
    for req_id in gold:
        if req_id not in manifest:
            continue
        if req_id in exemplar_ids:
            skipped.append(req_id)
        else:
            evaluated.append(req_id)
    return evaluated, skipped


# --------------------------------------------------------------------------
# Formatting helpers
# --------------------------------------------------------------------------


def _fmt(value: Optional[float], digits: int = 3) -> str:
    return "n/a" if value is None else f"{value:.{digits}f}"


def _fmt_pct(value: Optional[float]) -> str:
    return "n/a" if value is None else f"{value:.1%}"


def _dist_row(label: str, d: cq.Distribution) -> str:
    return (
        f"| {label} | {d.n} | {_fmt(d.mean)} | {_fmt(d.median)} "
        f"| {_fmt(d.stdev)} | {_fmt(d.min)} | {_fmt(d.max)} |"
    )


# --------------------------------------------------------------------------
# Scoring per strategy
# --------------------------------------------------------------------------


class StrategyScores:
    """All per-strategy scoring products, ready for rendering."""

    def __init__(
        self,
        strategy: str,
        gold: Dict[str, GoldRequirement],
        manifest: Dict[str, dict],
        evaluated: List[str],
    ) -> None:
        self.strategy = strategy
        self.gold = gold
        self.manifest = manifest
        self.evaluated = evaluated

        self.slot_rows = [
            fa.RequirementSlots(
                req_id=req_id,
                gold_slots=gold[req_id].gold_slots,
                llm_slots=manifest[req_id]["llm_slots"],
                gold_overall_incomplete=gold[req_id].gold_overall_incomplete,
            )
            for req_id in evaluated
        ]
        self.track1 = fa.score(self.slot_rows)

        # Track 2 — similarity (skip empty canonicalRimay, with a warning)
        self.sim_seq: Dict[str, float] = {}
        self.sim_jac: Dict[str, float] = {}
        self.skipped_no_canonical: List[str] = []
        for req_id in evaluated:
            canonical = gold[req_id].canonical_rimay
            if not canonical.strip():
                self.skipped_no_canonical.append(req_id)
                continue
            llm_rimay = manifest[req_id]["rimay"]
            self.sim_seq[req_id] = cq.conversion_similarity(llm_rimay, canonical)
            self.sim_jac[req_id] = cq.token_jaccard(llm_rimay, canonical)

        self.llm_gold_seq = cq.Distribution(list(self.sim_seq.values()))
        self.llm_gold_jac = cq.Distribution(list(self.sim_jac.values()))

        # Human-human baseline over the same evaluated set
        hh_seq: List[float] = []
        hh_jac: List[float] = []
        for req_id in evaluated:
            texts = gold[req_id].human_rimay_texts
            hh_seq.extend(cq.pairwise_similarities(texts, cq.conversion_similarity))
            hh_jac.extend(cq.pairwise_similarities(texts, cq.token_jaccard))
        self.human_seq = cq.Distribution(hh_seq)
        self.human_jac = cq.Distribution(hh_jac)

        self.paska = cq.summarize_paska([manifest[r] for r in evaluated])


# --------------------------------------------------------------------------
# Report rendering
# --------------------------------------------------------------------------


def render_metrics_md(
    s: StrategyScores, n_gold: int, n_converted: int, skipped_exemplars: List[str]
) -> str:
    t1 = s.track1
    lines: List[str] = []
    lines.append(f"# Scoring — strategy `{s.strategy}`")
    lines.append("")
    lines.append(
        f"Counts: gold requirements = {n_gold}, converted = {n_converted}, "
        f"evaluated = {len(s.evaluated)}, skipped as FSL exemplars = "
        f"{len(skipped_exemplars)}{skipped_exemplars or ''}"
    )
    lines.append("")

    lines.append("## Track 1 — field accuracy")
    lines.append("")
    lines.append(
        "Binary collapse: gold `present` and `implied` count as `not-missing`; "
        "the LLM only signals missing-or-not, so metrics below are for the "
        "**\"missing\" class** (TP = LLM flagged a slot the gold says is missing)."
    )
    lines.append("")
    lines.append("| Slot | TP | FP | FN | TN | Gold-missing | Precision | Recall | F1 |")
    lines.append("|---|---|---|---|---|---|---|---|---|")
    for slot, c in t1.per_slot.items():
        lines.append(
            f"| {slot} | {c.tp} | {c.fp} | {c.fn} | {c.tn} | {c.gold_positives} "
            f"| {_fmt(c.precision)} | {_fmt(c.recall)} | {_fmt(c.f1)} |"
        )
    m = t1.micro
    lines.append(
        f"| **micro** | {m.tp} | {m.fp} | {m.fn} | {m.tn} | {m.gold_positives} "
        f"| {_fmt(m.precision)} | {_fmt(m.recall)} | {_fmt(m.f1)} |"
    )
    lines.append(
        f"| **macro** | | | | | | {_fmt(t1.macro_precision)} "
        f"| {_fmt(t1.macro_recall)} | {_fmt(t1.macro_f1)} |"
    )
    lines.append("")
    lines.append(
        "n/a = undefined (zero denominator; e.g. no gold-missing slots for "
        "recall). Macro averages are over slots where the value is defined."
    )
    lines.append("")

    lines.append("### Secondary lenses")
    lines.append("")
    lines.append(
        "| Slot | Gold implied | LLM filled (inferred) | LLM missing (over-flag) "
        "| Gold missing | LLM missing (flagged) | LLM filled (silent fill) |"
    )
    lines.append("|---|---|---|---|---|---|---|")
    for slot, lens in t1.per_slot_lens.items():
        lines.append(
            f"| {slot} | {lens.n_gold_implied} | {lens.n_implied_llm_filled} "
            f"| {lens.n_implied_llm_missing} | {lens.n_gold_missing} "
            f"| {lens.n_missing_llm_missing} | {lens.n_missing_llm_filled} |"
        )
    o = t1.lens_overall
    lines.append(
        f"| **all slots** | {o.n_gold_implied} | {o.n_implied_llm_filled} "
        f"| {o.n_implied_llm_missing} | {o.n_gold_missing} "
        f"| {o.n_missing_llm_missing} | {o.n_missing_llm_filled} |"
    )
    lines.append("")
    lines.append(
        f"Among gold-implied slots the LLM filled {_fmt_pct(o.implied_filled_rate)} "
        f"(inferred context rather than over-flagging). Among gold-missing slots "
        f"the LLM correctly flagged {_fmt_pct(o.missing_flagged_rate)}; the rest "
        f"were silently filled (possible compensation / hallucination)."
    )
    lines.append("")

    v = t1.verdict
    lines.append("### Overall verdict (incomplete = any mandatory slot missing)")
    lines.append("")
    lines.append(f"Agreement with `gold_overallIncomplete`: **{_fmt_pct(v.agreement_rate)}** (n={v.n})")
    lines.append("")
    lines.append("| | LLM incomplete | LLM complete |")
    lines.append("|---|---|---|")
    lines.append(f"| **Gold incomplete** | {v.gold_inc_llm_inc} | {v.gold_inc_llm_com} |")
    lines.append(f"| **Gold complete** | {v.gold_com_llm_inc} | {v.gold_com_llm_com} |")
    lines.append("")

    lines.append("## Track 2 — conversion quality")
    lines.append("")
    lines.append(
        "v0 similarity is a placeholder (SequenceMatcher ratio + token Jaccard "
        "on normalized, placeholder-stripped text) pending a structural/semantic "
        "metric. The human-human distribution is the interpretive ceiling: the "
        "LLM-vs-gold number only means something relative to how much humans "
        "vary among themselves."
    )
    lines.append("")
    if s.skipped_no_canonical:
        lines.append(
            f"WARNING: skipped in the similarity lens (empty canonicalRimay): "
            f"{s.skipped_no_canonical}"
        )
        lines.append("")
    lines.append("| Distribution | n | mean | median | stdev | min | max |")
    lines.append("|---|---|---|---|---|---|---|")
    lines.append(_dist_row("LLM vs gold — SequenceMatcher", s.llm_gold_seq))
    lines.append(_dist_row("Human vs human — SequenceMatcher", s.human_seq))
    lines.append(_dist_row("LLM vs gold — token Jaccard", s.llm_gold_jac))
    lines.append(_dist_row("Human vs human — token Jaccard", s.human_jac))
    lines.append("")

    p = s.paska
    lines.append("### Paska validation (structural fidelity)")
    lines.append("")
    lines.append(
        f"Pass rate: **{_fmt_pct(p.pass_rate)}** "
        f"({p.n_passed} passed / {p.n_failed} smelly / {p.n_errors} errored, "
        f"n={p.n_total})"
    )
    lines.append("")
    if p.smell_frequencies:
        lines.append("| Smell | Occurrences |")
        lines.append("|---|---|")
        for smell, count in sorted(
            p.smell_frequencies.items(), key=lambda kv: (-kv[1], kv[0])
        ):
            lines.append(f"| {smell} | {count} |")
    else:
        lines.append("No smells fired on any evaluated requirement.")
    lines.append("")
    return "\n".join(lines)


def render_per_requirement_md(s: StrategyScores) -> str:
    lines: List[str] = []
    lines.append(f"# Per-requirement review — strategy `{s.strategy}`")
    lines.append("")
    for req_id in s.evaluated:
        req = s.gold[req_id]
        row = s.manifest[req_id]
        matches = fa.per_slot_matches(
            fa.RequirementSlots(
                req_id=req_id,
                gold_slots=req.gold_slots,
                llm_slots=row["llm_slots"],
                gold_overall_incomplete=req.gold_overall_incomplete,
            )
        )
        llm_verdict = fa.llm_overall_incomplete(row["llm_slots"])

        lines.append(f"## {req_id}")
        lines.append("")
        lines.append(f"**NL:** {req.nl_text}")
        lines.append("")
        lines.append("| Slot | Gold | LLM | Match |")
        lines.append("|---|---|---|---|")
        for slot in config.SLOTS:
            mark = "yes" if matches[slot] else "**MISMATCH**"
            lines.append(
                f"| {slot} | {req.gold_slots[slot]} | {row['llm_slots'][slot]} | {mark} |"
            )
        lines.append("")
        lines.append(
            f"Overall: gold_incomplete={req.gold_overall_incomplete}, "
            f"llm_incomplete={llm_verdict} "
            f"({'agree' if req.gold_overall_incomplete == llm_verdict else '**DISAGREE**'})"
        )
        lines.append("")
        lines.append(f"**Gold canonical Rimay:** {req.canonical_rimay or '(empty)'}")
        lines.append("")
        lines.append("**Human conversions:**")
        for ann in req.annotations:
            lines.append(f"- *{ann.annotator}*: {ann.rimay_text or '(empty)'}")
        lines.append("")
        lines.append(f"**LLM Rimay:** {row['rimay']}")
        lines.append("")
        seq = s.sim_seq.get(req_id)
        jac = s.sim_jac.get(req_id)
        lines.append(
            f"Similarity vs gold: SequenceMatcher={_fmt(seq)}, Jaccard={_fmt(jac)}"
            + (" (skipped: empty canonical)" if req_id in s.skipped_no_canonical else "")
        )
        passed = row.get("paska_passed")
        smells = row.get("paska_smells") or []
        paska_str = (
            "ERROR: " + row.get("paska_error", "unknown")
            if passed is None
            else ("passed" if passed else f"smelly: {', '.join(smells)}")
        )
        lines.append(f"Paska: {paska_str}")
        lines.append("")
    return "\n".join(lines)


def comparison_rows(s: StrategyScores) -> List[dict]:
    rows = []
    for req_id in s.evaluated:
        req = s.gold[req_id]
        m = s.manifest[req_id]
        slot_row = fa.RequirementSlots(
            req_id=req_id,
            gold_slots=req.gold_slots,
            llm_slots=m["llm_slots"],
            gold_overall_incomplete=req.gold_overall_incomplete,
        )
        matches = fa.per_slot_matches(slot_row)
        llm_verdict = fa.llm_overall_incomplete(m["llm_slots"])
        row = {"strategy": s.strategy, "reqId": req_id}
        for slot in config.SLOTS:
            row[f"gold_{slot}"] = req.gold_slots[slot]
            row[f"llm_{slot}"] = m["llm_slots"][slot]
            row[f"match_{slot}"] = matches[slot]
        row["gold_overall_incomplete"] = req.gold_overall_incomplete
        row["llm_overall_incomplete"] = llm_verdict
        row["verdict_match"] = req.gold_overall_incomplete == llm_verdict
        row["sim_seqmatch"] = (
            round(s.sim_seq[req_id], 4) if req_id in s.sim_seq else ""
        )
        row["sim_jaccard"] = (
            round(s.sim_jac[req_id], 4) if req_id in s.sim_jac else ""
        )
        row["paska_passed"] = m.get("paska_passed")
        rows.append(row)
    return rows


def write_comparison_csv(
    out_path: Path, gold: Dict[str, GoldRequirement], exemplar_ids: set
) -> List[str]:
    """Rebuild comparison.csv from every manifest present. Returns strategies included."""
    strategies = sorted(
        p.stem for p in config.CONVERSIONS_DIR.glob("*.jsonl") if p.stat().st_size
    )
    all_rows: List[dict] = []
    for strategy in strategies:
        manifest = load_manifest(config.CONVERSIONS_DIR / f"{strategy}.jsonl")
        evaluated, _ = select_evaluated(gold, manifest, exemplar_ids)
        scores = StrategyScores(strategy, gold, manifest, evaluated)
        all_rows.extend(comparison_rows(scores))
    if not all_rows:
        return strategies
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with out_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=list(all_rows[0].keys()))
        writer.writeheader()
        writer.writerows(all_rows)
    return strategies


# --------------------------------------------------------------------------
# CLI
# --------------------------------------------------------------------------


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strategy", required=True)
    parser.add_argument("--gold", type=Path, default=config.GOLD_CSV)
    parser.add_argument(
        "--fsl-example-ids",
        default=None,
        help="Comma-separated reqIds to exclude as FSL exemplars "
        "(default: ids from prompts/examples/fsl_examples.json).",
    )
    parser.add_argument("--out", type=Path, default=config.SCORING_DIR)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    gold = load_gold(args.gold)
    manifest = load_manifest(config.CONVERSIONS_DIR / f"{args.strategy}.jsonl")

    exemplar_ids = default_fsl_exemplar_ids()
    if args.fsl_example_ids:
        exemplar_ids |= {x.strip() for x in args.fsl_example_ids.split(",") if x.strip()}

    evaluated, skipped_exemplars = select_evaluated(gold, manifest, exemplar_ids)
    if not evaluated:
        print("Nothing to evaluate: no overlap between gold CSV and manifest.")
        return 1

    scores = StrategyScores(args.strategy, gold, manifest, evaluated)

    out_dir = args.out / args.strategy
    out_dir.mkdir(parents=True, exist_ok=True)
    metrics_md = render_metrics_md(scores, len(gold), len(manifest), skipped_exemplars)
    (out_dir / "metrics.md").write_text(metrics_md, encoding="utf-8")
    (out_dir / "per_requirement.md").write_text(
        render_per_requirement_md(scores), encoding="utf-8"
    )
    strategies = write_comparison_csv(args.out / "comparison.csv", gold, exemplar_ids)

    t1, p = scores.track1, scores.paska
    print(f"[scoring] strategy={args.strategy}")
    print(
        f"[counts] gold={len(gold)} converted={len(manifest)} "
        f"evaluated={len(evaluated)} skipped_exemplars={len(skipped_exemplars)}"
    )
    if scores.skipped_no_canonical:
        print(
            f"[warn] similarity lens skipped (empty canonicalRimay): "
            f"{scores.skipped_no_canonical}"
        )
    print(f"[track1] macro-F1 (missing detection) = {_fmt(t1.macro_f1)}")
    print(f"[track1] overall-verdict agreement    = {_fmt_pct(t1.verdict.agreement_rate)}")
    print(f"[track2] mean LLM-vs-gold similarity  = {_fmt(scores.llm_gold_seq.mean)} (SequenceMatcher)")
    print(f"[track2] mean human-human similarity  = {_fmt(scores.human_seq.mean)} (SequenceMatcher)")
    print(f"[track2] Paska pass rate              = {_fmt_pct(p.pass_rate)}")
    print(f"[out] {out_dir / 'metrics.md'}")
    print(f"[out] {out_dir / 'per_requirement.md'}")
    print(f"[out] {args.out / 'comparison.csv'} (strategies: {', '.join(strategies)})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
