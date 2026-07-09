"""Stage 1 entry point: convert NL -> Rimay, run Paska, log, write manifest.

Reads requirements from the gold CSV (``nlText`` keyed by ``reqId``),
runs the chosen prompting strategy over each, and writes:

  * ``outputs/llm_rimay/<strategy>/<reqId>.txt``  — raw Rimay per req
  * ``outputs/conversions/<strategy>.jsonl``      — the scorer's manifest
  * one MLflow run per requirement under experiment ``gold_<strategy>``

FSL exemplars are demonstrations, never scored items: any reqId that
appears in ``prompts/examples/fsl_examples.json`` is skipped defensively
(in normal operation the exemplar pool and the gold eval set do not
overlap).

Usage:
    python scripts/run_conversion.py --strategy zsl [--n-samples N]
        [--model ...] [--temperature 0.0] [--max-tokens 1024]
        [--n-fsl-examples 2]
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

THIS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(THIS_DIR.parent))

from src import config  # noqa: E402
from src.gold_loader import load_gold  # noqa: E402
from src.pipeline import run_single  # noqa: E402
from src.prompt_builder import STRATEGIES  # noqa: E402


def _fsl_exemplar_ids() -> set[str]:
    """reqIds/ids to skip: both the exemplar ``id`` and its ``source_reqId``.

    ``source_reqId`` is the real join key, so this defends against an
    exemplar's source requirement accidentally appearing in the gold set.
    """
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


def parse_args(argv=None) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="NL -> Rimay conversion (Stage 1)")
    p.add_argument(
        "--strategy",
        required=True,
        choices=sorted(STRATEGIES),
        help="Prompting strategy.",
    )
    p.add_argument("--n-samples", type=int, default=None, help="Limit #requirements.")
    p.add_argument("--model", default=config.DEFAULT_MODEL)
    p.add_argument("--temperature", type=float, default=config.DEFAULT_TEMPERATURE)
    p.add_argument("--max-tokens", type=int, default=config.DEFAULT_MAX_TOKENS)
    p.add_argument(
        "--n-fsl-examples", type=int, default=config.DEFAULT_N_FSL_EXAMPLES
    )
    p.add_argument("--gold", default=str(config.GOLD_CSV), help="Gold CSV path.")
    p.add_argument(
        "--run-name",
        default=None,
        help="Output folder name under outputs/. Default: auto (runN_<strategy>).",
    )
    return p.parse_args(argv)


def main(argv=None) -> int:
    args = parse_args(argv)
    run_cfg = config.RunConfig(
        strategy=args.strategy,
        model=args.model,
        temperature=args.temperature,
        max_tokens=args.max_tokens,
        n_fsl_examples=args.n_fsl_examples,
    )

    config.ensure_output_dirs()

    run_id = args.run_name or config.next_run_id(
        args.strategy, args.n_fsl_examples if args.strategy == "fsl" else None
    )
    run_paths = config.RunPaths(run_id)
    run_paths.ensure()

    gold = load_gold(Path(args.gold))
    exemplar_ids = _fsl_exemplar_ids()

    items = [(r.req_id, r.nl_text) for r in gold.values()]
    if args.n_samples is not None:
        items = items[: args.n_samples]

    manifest_path = run_paths.manifest_path

    print(f"Stage 1 conversion — run={run_id} strategy={args.strategy} model={args.model}")
    print(f"  gold reqs: {len(gold)}  to process: {len(items)}")
    print(f"  run dir:   {run_paths.root.relative_to(config.PROJECT_ROOT)}")
    print("=" * 60)

    processed = 0
    skipped = 0
    skipped_ids = []
    with manifest_path.open("w", encoding="utf-8") as mf:
        for i, (req_id, nl_text) in enumerate(items, start=1):
            if req_id in exemplar_ids:
                skipped += 1
                skipped_ids.append(req_id)
                print(f"[{i}/{len(items)}] SKIP {req_id} (FSL exemplar)")
                continue
            if not nl_text.strip():
                skipped += 1
                skipped_ids.append(req_id)
                print(f"[{i}/{len(items)}] SKIP {req_id} (empty nlText)")
                continue
            print(f"[{i}/{len(items)}] {req_id} ...", flush=True)
            record = run_single(req_id, nl_text, run_cfg=run_cfg, run_paths=run_paths)
            mf.write(json.dumps(record, ensure_ascii=False) + "\n")
            mf.flush()
            processed += 1
            passed = record["paska_passed"]
            missing = [s for s, v in record["llm_slots"].items() if v == "missing"]
            print(
                f"        paska_passed={passed} "
                f"missing_slots={missing or '-'} "
                f"latency_ms={record['latency_ms']}"
            )

    meta = {
        "run_id": run_id,
        "strategy": args.strategy,
        "model": args.model,
        "temperature": args.temperature,
        "max_tokens": args.max_tokens,
        "n_fsl_examples": args.n_fsl_examples if args.strategy == "fsl" else None,
        "gold_csv": str(Path(args.gold)),
        "created_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "n_processed": processed,
        "n_skipped": skipped,
        "skipped_ids": skipped_ids,
    }
    run_paths.meta_path.write_text(
        json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    print("=" * 60)
    print(f"Done. run={run_id} processed={processed} skipped={skipped}")
    print(f"Run dir:  {run_paths.root}")
    print(f"Next:     python scripts/run_scoring.py --run {run_id}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
