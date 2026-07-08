"""Load and normalise the human gold-standard annotation CSV.

The export (``data/gold_annotations.csv``) has one row per
(requirement, annotator), four annotators per requirement. The
``gold_*`` columns plus ``canonicalRimay`` and ``nlText`` are
adjudicated and identical across the rows of a given requirement; the
per-annotator columns (``rimayText``, ``slot_*``, ``overallIncomplete``,
``nonAtomic`` ...) differ per row and form the human baseline.

Nothing here hard-codes annotator names or requirement counts — both are
derived from the data so the loader keeps working as the set grows.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional

import pandas as pd

from src import config

# The five structural slots, in canonical order (see config.SLOTS). The gold
# columns are `gold_<slot>`; each annotator's own labels are `slot_<slot>`.
SLOTS = config.SLOTS

# Ternary gold slot values.
GOLD_SLOT_VALUES = ("present", "implied", "missing")


def _norm_str(value) -> str:
    if value is None:
        return ""
    if isinstance(value, float) and pd.isna(value):
        return ""
    return str(value).strip()


def _norm_bool(value) -> bool:
    """Normalise TRUE/true/1/yes → True; everything else → False."""
    return _norm_str(value).lower() in {"true", "1", "yes", "y"}


def _norm_slot(value) -> str:
    """Normalise a ternary slot label to lower case; empty stays empty."""
    return _norm_str(value).lower()


@dataclass(frozen=True)
class HumanAnnotation:
    """One annotator's own conversion + labels for one requirement."""

    annotator: str
    rimay: str
    slots: Dict[str, str]  # slot -> present|implied|missing (that annotator's)
    condition_type: str
    non_atomic: bool
    overall_incomplete: bool
    notes: str


@dataclass(frozen=True)
class GoldRecord:
    """Adjudicated gold for one requirement plus its human baseline."""

    req_id: str
    phase: str
    order: int
    nl_text: str
    nl_description: str
    # adjudicated ternary slot labels: slot -> present|implied|missing
    gold_slots: Dict[str, str]
    gold_condition_type: str
    gold_overall_incomplete: bool
    gold_had_disagreement: bool
    canonical_rimay: str
    human_annotations: List[HumanAnnotation] = field(default_factory=list)

    @property
    def human_rimays(self) -> List[str]:
        """Non-empty per-annotator Rimay conversions (the human baseline)."""
        return [a.rimay for a in self.human_annotations if a.rimay]


def _gold_slots_from_row(row: pd.Series) -> Dict[str, str]:
    return {slot: _norm_slot(row.get(f"gold_{slot}")) for slot in SLOTS}


def _annotator_slots_from_row(row: pd.Series) -> Dict[str, str]:
    return {slot: _norm_slot(row.get(f"slot_{slot}")) for slot in SLOTS}


def load_gold(csv_path: Optional[Path] = None) -> Dict[str, GoldRecord]:
    """Parse the gold CSV into ``{reqId: GoldRecord}``.

    Requirements are keyed by ``reqId``. For each, the ``gold_*`` columns
    are de-duplicated (taken from the first row) and every row contributes
    one :class:`HumanAnnotation` to the baseline.
    """
    path = Path(csv_path) if csv_path else config.GOLD_CSV
    if not path.is_file():
        raise FileNotFoundError(f"Gold CSV not found: {path}")

    df = pd.read_csv(path, dtype=str, keep_default_na=False)
    if "reqId" not in df.columns:
        raise ValueError(f"{path} has no 'reqId' column; got {list(df.columns)}")

    records: Dict[str, GoldRecord] = {}
    # Preserve first-seen order of requirements.
    for req_id, group in df.groupby("reqId", sort=False):
        first = group.iloc[0]

        try:
            order = int(float(_norm_str(first.get("order")) or 0))
        except ValueError:
            order = 0

        human_annotations: List[HumanAnnotation] = []
        for _, row in group.iterrows():
            human_annotations.append(
                HumanAnnotation(
                    annotator=_norm_str(row.get("annotatorUsername")),
                    rimay=_norm_str(row.get("rimayText")),
                    slots=_annotator_slots_from_row(row),
                    condition_type=_norm_str(row.get("conditionType")).lower(),
                    non_atomic=_norm_bool(row.get("nonAtomic")),
                    overall_incomplete=_norm_bool(row.get("overallIncomplete")),
                    notes=_norm_str(row.get("notes")),
                )
            )

        records[str(req_id)] = GoldRecord(
            req_id=str(req_id),
            phase=_norm_str(first.get("phase")),
            order=order,
            nl_text=_norm_str(first.get("nlText")),
            nl_description=_norm_str(first.get("nlDescription")),
            gold_slots=_gold_slots_from_row(first),
            gold_condition_type=_norm_str(first.get("gold_conditionType")).lower(),
            gold_overall_incomplete=_norm_bool(first.get("gold_overallIncomplete")),
            gold_had_disagreement=_norm_bool(first.get("gold_hadDisagreement")),
            canonical_rimay=_norm_str(first.get("canonicalRimay")),
            human_annotations=human_annotations,
        )
    return records


def load_conversion_inputs(
    csv_path: Optional[Path] = None,
) -> List[tuple[str, str]]:
    """Return ``[(reqId, nlText), ...]`` in gold order for Stage 1 input."""
    gold = load_gold(csv_path)
    return [(r.req_id, r.nl_text) for r in gold.values()]


def main() -> None:
    gold = load_gold()
    print(f"Loaded {len(gold)} requirements from {config.GOLD_CSV}")
    annotators = sorted({a.annotator for r in gold.values() for a in r.human_annotations})
    print(f"Annotators ({len(annotators)}): {', '.join(annotators)}")
    print("First few:")
    for req_id, rec in list(gold.items())[:5]:
        print(
            f"  {req_id}: phase={rec.phase} "
            f"gold_incomplete={rec.gold_overall_incomplete} "
            f"n_humans={len(rec.human_annotations)} "
            f"gold_slots={rec.gold_slots}"
        )


if __name__ == "__main__":
    main()
