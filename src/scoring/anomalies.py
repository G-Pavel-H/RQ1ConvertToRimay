"""Things in a run worth a second look: format breaches, wrong unit counts, failures.

Built from the raw conversion manifest (before units are aligned to the gold),
so it sees exactly what the model returned. Stored in ``results.json`` as
``anomalies`` and shown by the dashboard and the verdict stage.
"""
from __future__ import annotations

from typing import Dict, List, Optional

# kind -> what it means; the order is the display order.
KINDS = {
    "empty": "no Rimay line in the response",
    "truncated": "the response hit the token limit",
    "paska_error": "Paska failed on the conversion",
    "no_block": "no <rimay> answer block (older readers used)",
    "unclosed_block": "the <rimay> block was not closed",
    "text_outside": "text outside the answer block",
    "dropped_lines": "non-Rimay lines inside the answer, removed",
    "extra_lines": "more than one Rimay line for an atomic conversion",
    "non_atomic_flag": "the model flagged an atomic requirement <NON_ATOMIC>",
    "split_atomic": "an atomic requirement returned as several units",
    "merged_non_atomic": "a non-atomic requirement returned as one unit",
    "count_mismatch": "returned a different number of units than the gold",
}


def find(manifest: Dict[str, dict], *, task: str, strategy: str,
         units_info: Optional[dict] = None) -> dict:
    items: List[dict] = []

    def add(kind: str, req_id: str, detail: str = "") -> None:
        items.append({"kind": kind, "reqId": req_id, "detail": detail})

    for rid, rec in manifest.items():
        fmt = rec.get("format") or {}
        if not (rec.get("units") if task != "atomic" else rec.get("rimay")):
            add("empty", rid)
        if rec.get("stop_reason") == "max_tokens":
            add("truncated", rid)
        if rec.get("paska_error"):
            add("paska_error", rid, str(rec["paska_error"])[:200])
        if not fmt:
            continue  # converted before the answer block existed
        if not fmt.get("block"):
            add("no_block", rid)
        elif not fmt.get("closed"):
            add("unclosed_block", rid)
        # Reasoning before the block is the CoT contract; anything after it, or
        # anything at all around a zsl/fsl answer, is a breach.
        outside = fmt.get("outside", 0) + (fmt.get("before", 0) if strategy != "cot" else 0)
        if fmt.get("block") and outside:
            add("text_outside", rid, f"{outside} characters")
        if fmt.get("dropped"):
            add("dropped_lines", rid, " | ".join(fmt["dropped"][:3])[:300])
        if task == "atomic":
            if fmt.get("lines", 0) > 1:
                add("extra_lines", rid, f"{fmt['lines']} Rimay lines; the first is scored")
            if rec.get("is_non_atomic"):
                add("non_atomic_flag", rid)

    for r in (units_info or {}).get("perRequirement", []):
        exp, ret = r["expected"], r["returned"]
        if exp == ret:
            continue
        if task == "blind" and exp == 1:
            add("split_atomic", r["reqId"], f"1 → {ret} units")
        elif task == "blind" and ret == 1:
            add("merged_non_atomic", r["reqId"], f"{exp} → 1 unit")
        else:
            add("count_mismatch", r["reqId"], f"{exp} → {ret} units")

    counts = {k: sum(i["kind"] == k for i in items) for k in KINDS}
    return {"counts": {k: v for k, v in counts.items() if v},
            "affected": len({i["reqId"] for i in items}),
            "items": sorted(items, key=lambda i: (list(KINDS).index(i["kind"]), i["reqId"])),
            "kinds": KINDS}
