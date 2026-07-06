"""Load the adjudicated gold-annotation CSV.

The CSV (``data/gold_annotations.csv``) is an export from the annotation
app: one row per (requirement, annotator). The ``gold_*`` columns and
``canonicalRimay`` are adjudicated and identical across a requirement's
rows; the ``slot_*`` / ``rimayText`` / ``overallIncomplete`` columns are
that annotator's own answers (used for the human baseline in Track 2).

Nothing here hard-codes annotator names or requirement counts — both
are derived from the data, so the loader keeps working when the set
grows from 10 to 50 requirements.

``pragyanIncomp`` is an old third-party label and is deliberately not
loaded anywhere.
"""
from __future__ import annotations

import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional

import pandas as pd

from src import config

_TRUTHY = {"true", "1", "yes"}


def _to_bool(value: object) -> bool:
    """Normalize TRUE/true/True/1 (any case, any type) to bool."""
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in _TRUTHY


def _clean(value: object) -> str:
    if value is None or (isinstance(value, float) and pd.isna(value)):
        return ""
    return str(value).strip()


@dataclass(frozen=True)
class HumanAnnotation:
    """One annotator's own conversion + labels for one requirement."""

    annotator: str
    rimay_text: str
    slots: Dict[str, str]  # slot name -> present | implied | missing
    condition_type: str
    overall_incomplete: bool
    non_atomic: bool
    notes: str = ""


@dataclass(frozen=True)
class GoldRequirement:
    """The adjudicated gold record for one requirement, plus the raw
    per-annotator conversions used for the human baseline."""

    req_id: str
    phase: str
    order: int
    nl_text: str
    nl_description: str
    gold_slots: Dict[str, str]  # slot name -> present | implied | missing
    gold_condition_type: str
    gold_overall_incomplete: bool
    gold_had_disagreement: bool
    canonical_rimay: str
    annotations: List[HumanAnnotation] = field(default_factory=list)

    @property
    def human_rimay_texts(self) -> List[str]:
        return [a.rimay_text for a in self.annotations if a.rimay_text]


def load_gold(csv_path: Optional[Path] = None) -> Dict[str, GoldRequirement]:
    """Parse the gold CSV into one GoldRequirement per reqId.

    Returns an insertion-ordered dict keyed by reqId, sorted by the
    gold ``order`` column. Raises if the gold columns disagree across a
    requirement's annotator rows (they are adjudicated and must match).
    """
    path = csv_path or config.GOLD_CSV
    if not path.is_file():
        raise FileNotFoundError(f"Gold annotations CSV not found: {path}")

    df = pd.read_csv(path)
    required = {"reqId", "nlText", "canonicalRimay", "gold_overallIncomplete"} | {
        f"gold_{slot}" for slot in config.SLOTS
    }
    missing_cols = required - set(df.columns)
    if missing_cols:
        raise ValueError(f"{path} is missing expected columns: {sorted(missing_cols)}")

    requirements: Dict[str, GoldRequirement] = {}
    for req_id, group in df.groupby("reqId", sort=False):
        gold_cols = [f"gold_{slot}" for slot in config.SLOTS] + [
            "gold_conditionType",
            "gold_overallIncomplete",
            "gold_hadDisagreement",
            "canonicalRimay",
            "nlText",
        ]
        for col in gold_cols:
            if col in group.columns and group[col].astype(str).nunique() > 1:
                raise ValueError(
                    f"reqId={req_id!r}: adjudicated column {col!r} differs "
                    f"across annotator rows — the export is inconsistent."
                )

        first = group.iloc[0]
        annotations = [
            HumanAnnotation(
                annotator=_clean(row.get("annotatorUsername")),
                rimay_text=_clean(row.get("rimayText")),
                slots={slot: _clean(row.get(f"slot_{slot}")).lower() for slot in config.SLOTS},
                condition_type=_clean(row.get("conditionType")).lower(),
                overall_incomplete=_to_bool(row.get("overallIncomplete")),
                non_atomic=_to_bool(row.get("nonAtomic")),
                notes=_clean(row.get("notes")),
            )
            for _, row in group.iterrows()
        ]

        requirements[str(req_id)] = GoldRequirement(
            req_id=str(req_id),
            phase=_clean(first.get("phase")),
            order=int(first.get("order", 0)),
            nl_text=_clean(first.get("nlText")),
            nl_description=_clean(first.get("nlDescription")),
            gold_slots={slot: _clean(first.get(f"gold_{slot}")).lower() for slot in config.SLOTS},
            gold_condition_type=_clean(first.get("gold_conditionType")).lower(),
            gold_overall_incomplete=_to_bool(first.get("gold_overallIncomplete")),
            gold_had_disagreement=_to_bool(first.get("gold_hadDisagreement")),
            canonical_rimay=_clean(first.get("canonicalRimay")),
            annotations=annotations,
        )

    return dict(sorted(requirements.items(), key=lambda kv: kv[1].order))


def main() -> None:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else config.GOLD_CSV
    gold = load_gold(path)
    print(f"Loaded {len(gold)} requirements from {path}")
    annotators = sorted({a.annotator for g in gold.values() for a in g.annotations})
    print(f"Annotators ({len(annotators)}): {', '.join(annotators)}")
    print("First requirements:")
    for req in list(gold.values())[:5]:
        slots = " ".join(f"{s}={v}" for s, v in req.gold_slots.items())
        print(
            f"  {req.req_id:<20} order={req.order:<3} "
            f"incomplete={req.gold_overall_incomplete} {slots}"
        )


if __name__ == "__main__":
    main()
