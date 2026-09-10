#!/usr/bin/env python3
"""Stage 3 entry point: LLM analysis of scoring results (BACKLOG R4).

Runs entirely offline against ``scoring/results.json`` — no conversion, no
Paska, no gold CSV — so it can be re-run with a different model at any time
without repeating Stage 1 or Stage 2.

Usage:
    # one run; the verdict is written into its results.json
    python scripts/run_verdict.py --run main30/zsl

    # every strategy in a batch: a verdict per run, plus a comparison written
    # to outputs/main30/verdict.md
    python scripts/run_verdict.py --batch main30

    # a stronger model for the analysis than the one under test
    python scripts/run_verdict.py --batch main30 --model claude-opus-5

    # see exactly what would be sent, without calling the API
    python scripts/run_verdict.py --run main30/zsl --dry-run

The report picks verdicts up automatically: rebuild it with bin/report.sh.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from src import config, verdict as V  # noqa: E402


def parse_args(argv=None) -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="LLM analysis of scoring results (Stage 3)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    target = p.add_mutually_exclusive_group(required=True)
    target.add_argument("--run", help="A single scored run, e.g. main30/zsl.")
    target.add_argument(
        "--batch",
        help="A batch folder, e.g. main30: verdicts for every strategy in it, "
             "plus a cross-strategy comparison.",
    )
    p.add_argument(
        "--model",
        default=config.DEFAULT_MODEL,
        help=f"Model that writes the analysis (default: {config.DEFAULT_MODEL}). "
             "Use a strong one, e.g. claude-opus-5 — this is interpretation, "
             "not conversion, and it runs once per run rather than per requirement.",
    )
    # A batch comparison reasons over every strategy at once and needs more
    # room than a single run; models that emit thinking blocks spend this
    # budget too, so the default is generous.
    p.add_argument("--max-tokens", type=int, default=8192)
    p.add_argument(
        "--temperature",
        type=float,
        default=None,
        help="Only sent if given. Newer models (e.g. Opus 5) reject it.",
    )
    p.add_argument(
        "--skip-existing",
        action="store_true",
        help="Leave runs that already have a verdict untouched.",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the prompt that would be sent and exit. No API call.",
    )
    return p.parse_args(argv)


def _one_run(path: Path, args, label: str) -> bool:
    """Generate and store the verdict for a single run. True if written."""
    results = V.load_results(path)

    if args.skip_existing and results.get("verdict"):
        print(f"  {label}: verdict already present — skipped")
        return False

    payload = V.digest_run(results)

    if args.dry_run:
        prompt = V.build_prompt(payload, "run")
        print(f"\n----- {label}: prompt ({len(prompt):,} chars) -----")
        print(prompt[:2000])
        print(f"----- [truncated; {len(prompt):,} chars total] -----\n")
        return False

    print(f"  {label}: asking {args.model} ...")
    result = V.generate(
        payload,
        scope="run",
        model=args.model,
        max_tokens=args.max_tokens,
        temperature=args.temperature,
    )
    V.write_verdict(path, result)
    print(f"  {label}: {len(result.text.split())} words -> {path}")
    return True


def main(argv=None) -> int:
    args = parse_args(argv)

    if args.run:
        path = V.results_path(args.run)
        try:
            _one_run(path, args, args.run)
        except FileNotFoundError as err:
            print(err, file=sys.stderr)
            return 1
        print("\nRebuild the report to see it: bin/report.sh")
        return 0

    # --batch: one verdict per run, then a comparison across them.
    paths = V.find_batch_runs(args.batch)
    if not paths:
        print(
            f"No scored runs under outputs/{args.batch}/ "
            "(expected <batch>/<strategy>/scoring/results.json)",
            file=sys.stderr,
        )
        return 1

    print(f"Batch {args.batch}: {len(paths)} scored run(s)")
    all_results = []
    for path in paths:
        label = path.parent.parent.name
        all_results.append(V.load_results(path))
        _one_run(path, args, label)

    batch_payload = V.digest_batch(all_results)

    if args.dry_run:
        prompt = V.build_prompt(batch_payload, "batch")
        print(f"\n----- batch comparison: prompt ({len(prompt):,} chars) -----")
        print(prompt[:2000])
        print("----- [truncated] -----")
        return 0

    print(f"  comparison: asking {args.model} ...")
    comparison = V.generate(
        batch_payload,
        scope="batch",
        model=args.model,
        max_tokens=args.max_tokens,
        temperature=args.temperature,
    )

    out = config.OUTPUTS_DIR / args.batch / "verdict.md"
    out.write_text(
        f"# Cross-strategy verdict — {args.batch}\n\n"
        f"Model: `{comparison.model}` · generated "
        f"{comparison.as_dict()['generated_at']} · "
        f"runs: {', '.join(p.parent.parent.name for p in paths)}\n\n"
        f"{comparison.text}\n",
        encoding="utf-8",
    )
    print(f"  comparison: {len(comparison.text.split())} words -> {out}")
    print("\nRebuild the report to see the per-run verdicts: bin/report.sh")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
