#!/usr/bin/env python3
"""Apply hand-written refinements to the annotators' conversions, locally.

The annotators' Rimay conversions are revised for quality, but neither the
database nor the original export is ever modified. This reads the original gold
CSV and a list of edits, and writes a *new* CSV with the same columns, in which
``rimayText`` (and, where an edit says so, a slot) is overwritten in place.
Nothing is added — no extra columns — so every downstream tool reads it exactly
like the original.

To redo: edit ``data/refinements.json`` and re-run. The original stays intact.

Because a slot edit can change the majority, the per-requirement gold columns
are recomputed from the refined slots with the same rule as the annotation
tool's ``assign_majority_gold.js``: strict majority wins, a three-way tie
becomes ``implied``.

Usage:
    python scripts/apply_refinements.py \\
        --original data/gold_annotations_main_atomic.csv \\
        --edits    data/refinements.json \\
        --out      data/gold_annotations_main_atomic_refined.csv

Also writes ``<out stem>_review.html``: every edit as a word-level diff, for
reading through the changes.
"""
from __future__ import annotations

import argparse
import csv
import difflib
import html
import json
import re
import sys
from collections import Counter, OrderedDict
from pathlib import Path

SLOTS = ["scope", "condition", "actor", "modalVerb", "action"]
MANDATORY = ["actor", "modalVerb", "action"]
SLOT_VALUES = {"present", "implied", "missing"}
TIE_VALUE = "implied"


def majority(values: list[str]) -> str:
    """Strict majority, or TIE_VALUE when every value ties."""
    counts = Counter(values)
    top = max(counts.values())
    winners = [v for v, n in counts.items() if n == top]
    return winners[0] if len(winners) == 1 else TIE_VALUE


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--original", type=Path, required=True)
    p.add_argument("--edits", type=Path, required=True)
    p.add_argument("--out", type=Path, required=True)
    args = p.parse_args(argv)

    with args.original.open(newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        fieldnames = reader.fieldnames or []
        rows = list(reader)
    doc = json.loads(args.edits.read_text(encoding="utf-8"))
    edits = {(e["reqId"], e["annotator"]): e for e in doc["edits"]}

    # --- apply ----------------------------------------------------------------
    problems: list[str] = []
    applied = 0
    for row in rows:
        key = (row["reqId"], row["annotatorUsername"])
        edit = edits.pop(key, None)
        if edit is None:
            continue
        # An edit is written against a specific original text. If the export has
        # changed since, the edit may no longer make sense — refuse, don't guess.
        if edit.get("original") is not None and edit["original"] != row["rimayText"]:
            problems.append(f"{key}: original text has changed since this edit was written")
            continue
        row["rimayText"] = edit["rimayText"]
        for slot, value in (edit.get("slots") or {}).items():
            if slot not in SLOTS or value not in SLOT_VALUES:
                problems.append(f"{key}: bad slot override {slot}={value}")
                continue
            row[f"slot_{slot}"] = value
        applied += 1
    for key in edits:
        problems.append(f"{key}: no such (requirement, annotator) in {args.original.name}")
    if problems:
        print("Refusing to write — fix these first:", file=sys.stderr)
        for msg in problems:
            print("  - " + msg, file=sys.stderr)
        return 1

    # --- recompute gold from the refined slots ---------------------------------
    by_req: "OrderedDict[str, list[dict]]" = OrderedDict()
    for row in rows:
        by_req.setdefault(row["reqId"], []).append(row)
    gold_changes: Counter = Counter()
    for req_rows in by_req.values():
        gold = {s: majority([r[f"slot_{s}"].strip().lower() for r in req_rows]) for s in SLOTS}
        disagreement = any(len({r[f"slot_{s}"] for r in req_rows}) > 1 for s in SLOTS)
        incomplete = any(gold[s] == "missing" for s in MANDATORY)
        for r in req_rows:
            for s in SLOTS:
                if r[f"gold_{s}"] != gold[s]:
                    gold_changes[s] += 1
                r[f"gold_{s}"] = gold[s]
            r["gold_hadDisagreement"] = "true" if disagreement else "false"
            r["gold_overallIncomplete"] = "true" if incomplete else "false"

    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)

    review = args.out.with_name(args.out.stem + "_review.html")
    review.write_text(render_review(doc, rows), encoding="utf-8")

    print(f"Applied  {applied} edits to {args.original.name}")
    print(f"Gold     cells changed per slot (x rows): {dict(gold_changes) or 'none'}")
    print(f"Wrote    {args.out}")
    print(f"Review   {review}")
    return 0


# --- review page ---------------------------------------------------------------

# Placeholders stay whole and punctuation is its own token, so adding a comma
# after "<MISSING_CONDITION>" diffs as one inserted comma, not a replaced word.
_TOKEN = re.compile(r"<[A-Z_]+>|\s+|\w+|[^\w\s]")


def word_diff(old: str, new: str) -> str:
    a, b = _TOKEN.findall(old or ""), _TOKEN.findall(new or "")
    out = []
    for op, i1, i2, j1, j2 in difflib.SequenceMatcher(None, a, b, autojunk=False).get_opcodes():
        if op == "equal":
            out.append(html.escape("".join(a[i1:i2])))
            continue
        if i2 > i1:
            out.append(f'<del>{html.escape("".join(a[i1:i2]))}</del>')
        if j2 > j1:
            out.append(f'<ins>{html.escape("".join(b[j1:j2]))}</ins>')
    return "".join(out)


def render_review(doc: dict, rows: list[dict]) -> str:
    nl = {r["reqId"]: r["nlText"] for r in rows}
    notes = doc.get("requirementNotes", {})
    by_req: "OrderedDict[str, list[dict]]" = OrderedDict()
    for e in doc["edits"]:
        by_req.setdefault(e["reqId"], []).append(e)
    rules = "".join(f"<dt>{html.escape(k)}</dt><dd>{html.escape(v)}</dd>" for k, v in doc["rules"].items())

    blocks = []
    for i, (rid, eds) in enumerate(by_req.items(), 1):
        note = f'<p class="flag">{html.escape(notes[rid])}</p>' if rid in notes else ""
        items = []
        for e in sorted(eds, key=lambda x: x["annotator"]):
            chips = "".join(f'<span class="chip c-{html.escape(r)}">{html.escape(r)}</span>' for r in e["reasons"])
            slot = "".join(f'<span class="chip c-slot">{html.escape(s)} → {html.escape(v)}</span>'
                           for s, v in (e.get("slots") or {}).items())
            why = f'<div class="why">{html.escape(e["note"])}</div>' if e.get("note") else ""
            items.append(
                f'<div class="ed"><div class="who">{html.escape(e["annotator"])} {chips}{slot}</div>'
                f'<div class="diff">{word_diff(e["original"], e["rimayText"])}</div>{why}</div>'
            )
        blocks.append(
            f'<section><h2><span class="n">{i}</span> {html.escape(rid)}</h2>'
            f'<p class="nl">{html.escape(nl.get(rid, ""))}</p>{note}{"".join(items)}</section>'
        )

    reason_counts = Counter(r for e in doc["edits"] for r in e["reasons"])
    summary = " · ".join(f"{k} {v}" for k, v in reason_counts.most_common())
    return f"""<!doctype html><html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Annotator conversion refinements</title>
<style>
:root{{--bg:#fbfbfc;--card:#fff;--ink:#1b1d21;--muted:#7a8089;--line:#e4e6ea;--del:#fdecea;--delc:#b71c1c;--ins:#e8f5e9;--insc:#1b5e20;--flag:#fdf1dc}}
@media (prefers-color-scheme:dark){{:root{{--bg:#111315;--card:#181b1e;--ink:#e7e9ec;--muted:#9aa1aa;--line:#2a2e33;--del:#3a1717;--delc:#ef8a83;--ins:#15301b;--insc:#8fd49a;--flag:#33280f}}}}
*{{box-sizing:border-box}}body{{margin:0;background:var(--bg);color:var(--ink);font:14px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif}}
.wrap{{max-width:980px;margin:0 auto;padding:28px 20px 60px}}h1{{font-size:21px;margin:0 0 4px}}
.sub{{color:var(--muted);margin:0 0 18px}}dl{{display:grid;grid-template-columns:70px 1fr;gap:4px 12px;font-size:13px;margin:0 0 24px}}
dt{{font-weight:600}}dd{{margin:0;color:var(--muted)}}section{{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px 16px;margin-bottom:12px}}
h2{{font-size:15px;margin:0 0 6px}}.n{{color:var(--muted);font-weight:400;margin-right:4px}}.nl{{color:var(--muted);font-size:12.5px;margin:0 0 10px}}
.flag{{background:var(--flag);border-radius:6px;padding:6px 10px;font-size:12.5px;margin:0 0 10px}}
.ed{{border-top:1px solid var(--line);padding:8px 0}}.who{{font-weight:600;font-size:13px;margin-bottom:3px}}
.diff{{font-family:ui-monospace,SFMono-Regular,Menlo,monospace;font-size:12.5px;white-space:pre-wrap}}
del{{background:var(--del);color:var(--delc)}}ins{{background:var(--ins);color:var(--insc);text-decoration:none}}
.why{{color:var(--muted);font-size:12px;margin-top:3px}}.chip{{font-size:10.5px;font-weight:600;border:1px solid var(--line);border-radius:4px;padding:0 5px;margin-left:4px;color:var(--muted)}}
.c-opus,.c-word{{color:#b26a00;border-color:#b26a00}}.c-slot{{color:#1565c0;border-color:#1565c0}}
</style></head><body><div class="wrap">
<h1>Annotator conversion refinements</h1>
<p class="sub">{len(doc["edits"])} edits across {len(by_req)} requirements · {summary}.
Red = removed, green = added. The original CSV and the database are untouched.</p>
<dl>{rules}</dl>
{"".join(blocks)}
</div></body></html>"""


if __name__ == "__main__":
    raise SystemExit(main())
