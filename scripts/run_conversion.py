"""Stage 1 entry point: NL → Rimay conversion + Paska + MLflow + manifest.

For every requirement in the gold CSV (keyed by reqId, LLM input =
``nlText``), under the chosen strategy:

  prompt → LLM → Rimay (with <MISSING_*> placeholders) → strip →
  Paska (once, on the stripped Rimay) → MLflow run + manifest line.

The manifest ``outputs/conversions/<strategy>.jsonl`` is rewritten
fresh on every invocation (lines are flushed one by one, so a crashed
batch still leaves the completed lines on disk). It is the scorer's
single input from this stage.

Requirements whose reqId appears as an ``id`` in
``prompts/examples/fsl_examples.json`` are skipped defensively — FSL
exemplars come from a separate training pool and must never be scored.

Usage:
  python scripts/run_conversion.py --strategy zsl [--n-samples N]
      [--model ...] [--temperature 0.0] [--max-tokens 1024]
      [--n-fsl-examples 2]
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

THIS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(THIS_DIR.parent))

from src import config  # noqa: E402
from src.gold_loader import load_gold  # noqa: E402
from src.pipeline import run_single  # noqa: E402
from src.prompt_builder import STRATEGIES  # noqa: E402


def _fsl_exemplar_ids() -> set[str]:
    path = config.FSL_EXAMPLES_JSON
    if not path.is_file():
        return set()
    examples = json.loads(path.read_text(encoding="utf-8"))
    return {str(ex.get("id")) for ex in examples if ex.get("id")}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--strategy", required=True, choices=sorted(STRATEGIES))
    parser.add_argument("--n-samples", type=int, default=None,
                        help="Convert only the first N requirements (dev runs).")
    parser.add_argument("--model", default=config.DEFAULT_MODEL)
    parser.add_argument("--temperature", type=float, default=config.DEFAULT_TEMPERATURE)
    parser.add_argument("--max-tokens", type=int, default=config.DEFAULT_MAX_TOKENS)
    parser.add_argument("--n-fsl-examples", type=int, default=config.DEFAULT_N_FSL_EXAMPLES)
    parser.add_argument("--gold", type=Path, default=config.GOLD_CSV)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    run_cfg = config.RunConfig(
        strategy=args.strategy,
        model=args.model,
        temperature=args.temperature,
        max_tokens=args.max_tokens,
        n_fsl_examples=args.n_fsl_examples,
    )

    gold = load_gold(args.gold)
    exemplar_ids = _fsl_exemplar_ids()

    todo = []
    skipped = []
    for req_id, req in gold.items():
        if req_id in exemplar_ids:
            skipped.append(req_id)
            continue
        todo.append(req)
    if args.n_samples is not None:
        todo = todo[: args.n_samples]

    if skipped:
        print(f"[skip] {len(skipped)} requirement(s) are FSL exemplars: {skipped}")
    print(
        f"[run] strategy={args.strategy} model={run_cfg.model} "
        f"requirements={len(todo)}/{len(gold)}"
    )

    config.ensure_output_dirs()
    manifest_path = config.CONVERSIONS_DIR / f"{args.strategy}.jsonl"
    n_ok = 0
    n_failed = 0
    with manifest_path.open("w", encoding="utf-8") as manifest:
        for i, req in enumerate(todo, start=1):
            print(f"[{i}/{len(todo)}] {req.req_id} ...")
            try:
                outcome = run_single(req.req_id, req.nl_text, run_cfg=run_cfg)
            except Exception as exc:  # noqa: BLE001 — keep the batch going
                n_failed += 1
                print(f"  ERROR: {type(exc).__name__}: {exc}")
                continue
            manifest.write(json.dumps(outcome.manifest_line(), ensure_ascii=False) + "\n")
            manifest.flush()
            n_ok += 1
            print(
                f"  rimay: {outcome.rimay}\n"
                f"  slots: {outcome.llm_slots}\n"
                f"  paska_passed: {outcome.paska_passed} "
                f"(smells: {outcome.paska_smells or 'none'})"
            )

    print(
        f"[done] converted={n_ok} failed={n_failed} skipped_exemplars={len(skipped)}\n"
        f"[done] manifest: {manifest_path}\n"
        f"[done] MLflow: experiment gold_{args.strategy} "
        f"(mlflow ui --backend-store-uri sqlite:///mlruns/mlflow.db)"
    )
    return 0 if n_failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
