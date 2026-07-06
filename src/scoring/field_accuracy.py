"""Track 1 — field accuracy (pure functions, no IO).

Compares the LLM's binary per-slot signal (missing / filled, derived
from ``<MISSING_*>`` placeholders) against the adjudicated gold slot
labels (present / implied / missing).

Primary metric — binary collapse
--------------------------------
The gold is ternary; the LLM signal is binary. Gold ``present`` and
``implied`` collapse to ``not-missing``; ``missing`` stays. Rationale:
the LLM only signals missing-or-not — it cannot distinguish a slot
that is explicit in the NL (present) from one a reader infers
(implied), so both count as "the information was available".

Per slot (scope, condition, actor, modalVerb, action) we build the
confusion of LLM-missing vs gold-missing and report precision / recall
/ F1 for the **"missing" class** (a true positive = the LLM flagged a
slot the gold says is missing), plus micro (pooled counts) and macro
(mean over slots) averages. Zero-denominator cells yield ``None``
("n/a"), and macro averages are taken over the slots where the value
is defined.

Secondary lenses
----------------
* Among gold ``implied`` slots: how often the LLM filled (good — it
  inferred context) vs flagged missing (over-flag).
* Among gold ``missing`` slots: how often the LLM correctly flagged vs
  silently filled (possible compensation / hallucination).

Overall verdict
---------------
LLM overall-incomplete = any mandatory slot (actor, modalVerb, action)
flagged missing. Compared against ``gold_overallIncomplete``:
agreement rate + the 2x2 confusion.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Mapping, Optional, Sequence, Tuple

from src import config

GOLD_LABELS = {"present", "implied", "missing"}
LLM_LABELS = {"missing", "filled"}


@dataclass(frozen=True)
class RequirementSlots:
    """One requirement's slot labels, gold vs LLM."""

    req_id: str
    gold_slots: Mapping[str, str]  # slot -> present | implied | missing
    llm_slots: Mapping[str, str]  # slot -> missing | filled
    gold_overall_incomplete: bool

    def __post_init__(self) -> None:
        for slot in config.SLOTS:
            g = self.gold_slots.get(slot)
            if g not in GOLD_LABELS:
                raise ValueError(
                    f"{self.req_id}: gold label for {slot!r} is {g!r}, "
                    f"expected one of {sorted(GOLD_LABELS)}"
                )
            m = self.llm_slots.get(slot)
            if m not in LLM_LABELS:
                raise ValueError(
                    f"{self.req_id}: LLM label for {slot!r} is {m!r}, "
                    f"expected one of {sorted(LLM_LABELS)}"
                )


def collapse_gold(label: str) -> str:
    """Collapse the ternary gold label to the binary frame the LLM uses."""
    return "missing" if label == "missing" else "not-missing"


def slot_match(gold_label: str, llm_label: str) -> bool:
    """True when the LLM's missing/filled signal agrees with the collapsed gold."""
    return (llm_label == "missing") == (collapse_gold(gold_label) == "missing")


def llm_overall_incomplete(llm_slots: Mapping[str, str]) -> bool:
    """A requirement is LLM-incomplete when any mandatory slot is missing."""
    return any(llm_slots.get(slot) == "missing" for slot in config.MANDATORY_SLOTS)


@dataclass
class Confusion:
    """Binary confusion for the "missing" class."""

    tp: int = 0  # LLM missing, gold missing
    fp: int = 0  # LLM missing, gold not-missing
    fn: int = 0  # LLM filled,  gold missing
    tn: int = 0  # LLM filled,  gold not-missing

    @property
    def support(self) -> int:
        return self.tp + self.fp + self.fn + self.tn

    @property
    def gold_positives(self) -> int:
        return self.tp + self.fn

    @property
    def precision(self) -> Optional[float]:
        denom = self.tp + self.fp
        return self.tp / denom if denom else None

    @property
    def recall(self) -> Optional[float]:
        denom = self.tp + self.fn
        return self.tp / denom if denom else None

    @property
    def f1(self) -> Optional[float]:
        p, r = self.precision, self.recall
        if p is None or r is None or (p + r) == 0:
            return None
        return 2 * p * r / (p + r)

    def add(self, gold_label: str, llm_label: str) -> None:
        gold_missing = collapse_gold(gold_label) == "missing"
        llm_missing = llm_label == "missing"
        if llm_missing and gold_missing:
            self.tp += 1
        elif llm_missing and not gold_missing:
            self.fp += 1
        elif not llm_missing and gold_missing:
            self.fn += 1
        else:
            self.tn += 1


@dataclass
class LensCounts:
    """Secondary-lens tallies for one slot (or pooled over slots)."""

    n_gold_implied: int = 0
    n_implied_llm_filled: int = 0  # good: inferred context
    n_implied_llm_missing: int = 0  # over-flag
    n_gold_missing: int = 0
    n_missing_llm_missing: int = 0  # good: correctly flagged
    n_missing_llm_filled: int = 0  # silent fill / compensation

    @property
    def implied_filled_rate(self) -> Optional[float]:
        return (
            self.n_implied_llm_filled / self.n_gold_implied
            if self.n_gold_implied
            else None
        )

    @property
    def missing_flagged_rate(self) -> Optional[float]:
        return (
            self.n_missing_llm_missing / self.n_gold_missing
            if self.n_gold_missing
            else None
        )

    def add(self, gold_label: str, llm_label: str) -> None:
        if gold_label == "implied":
            self.n_gold_implied += 1
            if llm_label == "missing":
                self.n_implied_llm_missing += 1
            else:
                self.n_implied_llm_filled += 1
        elif gold_label == "missing":
            self.n_gold_missing += 1
            if llm_label == "missing":
                self.n_missing_llm_missing += 1
            else:
                self.n_missing_llm_filled += 1


@dataclass
class VerdictConfusion:
    """2x2 confusion for the overall complete/incomplete verdict.

    "Positive" = incomplete. Cell names read gold-first, e.g.
    ``gold_inc_llm_com`` = gold incomplete but the LLM called it complete.
    """

    gold_inc_llm_inc: int = 0
    gold_inc_llm_com: int = 0
    gold_com_llm_inc: int = 0
    gold_com_llm_com: int = 0

    @property
    def n(self) -> int:
        return (
            self.gold_inc_llm_inc
            + self.gold_inc_llm_com
            + self.gold_com_llm_inc
            + self.gold_com_llm_com
        )

    @property
    def agreement_rate(self) -> Optional[float]:
        return (self.gold_inc_llm_inc + self.gold_com_llm_com) / self.n if self.n else None

    def add(self, gold_incomplete: bool, llm_incomplete: bool) -> None:
        if gold_incomplete and llm_incomplete:
            self.gold_inc_llm_inc += 1
        elif gold_incomplete and not llm_incomplete:
            self.gold_inc_llm_com += 1
        elif not gold_incomplete and llm_incomplete:
            self.gold_com_llm_inc += 1
        else:
            self.gold_com_llm_com += 1


@dataclass
class FieldAccuracyReport:
    """Everything Track 1 computes over a set of requirements."""

    n_requirements: int
    per_slot: Dict[str, Confusion] = field(default_factory=dict)
    micro: Confusion = field(default_factory=Confusion)
    per_slot_lens: Dict[str, LensCounts] = field(default_factory=dict)
    lens_overall: LensCounts = field(default_factory=LensCounts)
    verdict: VerdictConfusion = field(default_factory=VerdictConfusion)

    @property
    def macro_precision(self) -> Optional[float]:
        return _mean_defined([c.precision for c in self.per_slot.values()])

    @property
    def macro_recall(self) -> Optional[float]:
        return _mean_defined([c.recall for c in self.per_slot.values()])

    @property
    def macro_f1(self) -> Optional[float]:
        return _mean_defined([c.f1 for c in self.per_slot.values()])


def _mean_defined(values: Sequence[Optional[float]]) -> Optional[float]:
    defined = [v for v in values if v is not None]
    return sum(defined) / len(defined) if defined else None


def score(rows: Sequence[RequirementSlots]) -> FieldAccuracyReport:
    """Compute the full Track 1 report over the evaluated requirements."""
    report = FieldAccuracyReport(n_requirements=len(rows))
    report.per_slot = {slot: Confusion() for slot in config.SLOTS}
    report.per_slot_lens = {slot: LensCounts() for slot in config.SLOTS}

    for row in rows:
        for slot in config.SLOTS:
            gold_label = row.gold_slots[slot]
            llm_label = row.llm_slots[slot]
            report.per_slot[slot].add(gold_label, llm_label)
            report.micro.add(gold_label, llm_label)
            report.per_slot_lens[slot].add(gold_label, llm_label)
            report.lens_overall.add(gold_label, llm_label)
        report.verdict.add(
            row.gold_overall_incomplete, llm_overall_incomplete(row.llm_slots)
        )

    return report


def per_slot_matches(row: RequirementSlots) -> Dict[str, bool]:
    """Per-slot agreement flags for one requirement (for comparison.csv)."""
    return {
        slot: slot_match(row.gold_slots[slot], row.llm_slots[slot])
        for slot in config.SLOTS
    }
