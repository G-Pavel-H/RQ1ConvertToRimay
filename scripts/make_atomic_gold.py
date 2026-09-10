#!/usr/bin/env python3
"""Build the atomic-only gold CSV from a full annotation-tool export.

Rimay admits one system response per requirement, but the pipeline has no
decomposition step yet (see BACKLOG A2/R5). Until it does, requirements the
annotators judged non-atomic cannot be converted meaningfully, so this filters
them out and leaves a gold set the current pipeline can actually run on.

Two filters are applied, in this order:

1. **Drafts are dropped.** Only ``annotationStatus == submitted`` rows form the
   human baseline.
2. **Non-atomic requirements are dropped**, by any of three signals — the
   annotators did not all use the same one:
     * the ``nonAtomic`` checkbox is true;
     * the conversion text carries a ``<NON_ATOMIC>`` marker;
     * the notes describe a split in prose.
   A requirement is excluded if **any** annotator gave **any** of these signals.
   Using the checkbox alone leaves genuinely non-atomic requirements in the set.

Usage:
    python scripts/make_atomic_gold.py \
        --export data/gold_annotations_main.csv \
        --out    data/gold_annotations_main_atomic.csv

Writes the filtered CSV plus a sibling ``*_excluded.txt`` listing what was
removed and why, so the exclusion is reproducible and auditable rather than a
one-off manual edit.
"""
from __future__ import annotations

import argparse
import csv
import re
import sys
from collections import OrderedDict
from pathlib import Path

# The explicit marker one annotator used inside the conversion text.
TAG_RE = re.compile(r"<NON_ATOMIC>", re.I)
# Prose descriptions of a split, used in the notes field.
PROSE_RE = re.compile(
    r"non[- ]?atomic|split into|multiple requests|multiple (?:distinct|separate) ", re.I
)

SUBMITTED = "submitted"


def _truthy(value: str) -> bool:
    return (value or "").strip().lower() in {"true", "1", "yes", "y"}


def signals(row: dict) -> list[str]:
    """Which non-atomicity signals this single annotation carries."""
    found = []
    if _truthy(row.get("nonAtomic", "")):
        found.append("checkbox")
    if TAG_RE.search(row.get("rimayText") or ""):
        found.append("tag")
    if PROSE_RE.search(row.get("notes") or ""):
        found.append("notes")
    return found


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--export", required=True, type=Path, help="Full export CSV from the annotation tool.")
    p.add_argument("--out", required=True, type=Path, help="Where to write the atomic-only gold CSV.")
    p.add_argument(
        "--keep-drafts",
        action="store_true",
        help="Keep draft annotations (default: submitted only).",
    )
    args = p.parse_args(argv)

    if not args.export.is_file():
        print(f"Export not found: {args.export}", file=sys.stderr)
        return 1

    with args.export.open(newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        fieldnames = reader.fieldnames or []
        rows = list(reader)

    if "reqId" not in fieldnames:
        print(f"{args.export} has no 'reqId' column", file=sys.stderr)
        return 1

    total_reqs = len({r["reqId"] for r in rows})

    # 1. Drop drafts (and the placeholder rows unannotated requirements emit).
    kept = [r for r in rows if (r.get("annotatorUsername") or "").strip()]
    if not args.keep_drafts:
        kept = [r for r in kept if (r.get("annotationStatus") or "").strip().lower() == SUBMITTED]

    # 2. Collect non-atomicity signals per requirement, preserving export order.
    flagged: "OrderedDict[str, dict[str, list[str]]]" = OrderedDict()
    for r in kept:
        found = signals(r)
        if not found:
            continue
        flagged.setdefault(r["reqId"], {})[r.get("annotatorUsername", "?")] = found

    atomic_rows = [r for r in kept if r["reqId"] not in flagged]
    atomic_reqs = {r["reqId"] for r in atomic_rows}

    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(atomic_rows)

    excluded_path = args.out.with_name(args.out.stem + "_excluded.txt")
    with excluded_path.open("w", encoding="utf-8") as fh:
        fh.write(f"# Excluded from {args.out.name} as non-atomic\n")
        fh.write(f"# Source: {args.export.name}\n")
        fh.write("# Signal key: checkbox = nonAtomic ticked, tag = <NON_ATOMIC> in "
                 "conversion text, notes = split described in prose\n\n")
        for req_id, by_annotator in flagged.items():
            who = "; ".join(f"{a}: {'+'.join(s)}" for a, s in sorted(by_annotator.items()))
            fh.write(f"{req_id}\t{who}\n")

    print(f"Read     {args.export}  ({total_reqs} requirements, {len(rows)} rows)")
    print(f"Kept     {len(atomic_rows)} rows over {len(atomic_reqs)} atomic requirements")
    print(f"Excluded {len(flagged)} non-atomic requirements -> {excluded_path.name}")
    print(f"Wrote    {args.out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
