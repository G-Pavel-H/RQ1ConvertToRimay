"""Track 1 — field accuracy. Pure comparison functions, no IO.

The LLM slot signal is binary (``missing`` / ``filled``); the gold is
ternary (``present`` / ``implied`` / ``missing``). We reconcile them
three ways:

1. **Primary — binary collapse.** Collapse gold ``present`` and
   ``implied`` to ``not-missing`` and keep ``missing``. The LLM only ever
   signals missing-or-not; it cannot distinguish present from implied, so
   collapsing to the ``missing`` class is the only apples-to-apples
   comparison. Per slot we build the confusion of *LLM-missing* vs
   *gold-missing* and report precision / recall / F1 for the ``missing``
   class, plus micro and macro averages.

2. **Secondary — the interesting lenses.**
   * Among gold ``implied`` slots: did the LLM fill (inferred context,
     good) or flag missing (over-flag)?
   * Among gold ``missing`` slots: did the LLM correctly flag, or
     silently fill (possible compensation / hallucination)?

3. **Overall verdict.** LLM overall-incomplete = any *mandatory* slot
   (actor, modalVerb, action) flagged missing. Compare to
   ``gold_overallIncomplete``; report the agreement rate and 2x2
   confusion.

Every function takes plain Python structures so the module is trivially
testable and never touches pandas, MLflow, or the filesystem.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Sequence

from src import config

SLOTS = config.SLOTS
MANDATORY_SLOTS = config.MANDATORY_SLOTS


@dataclass(frozen=True)
class SlotEval:
    """One requirement's gold + LLM slot signals for scoring."""

    req_id: str
    gold_slots: Dict[str, str]  # slot -> present|implied|missing
    llm_slots: Dict[str, str]  # slot -> missing|filled
    gold_overall_incomplete: bool


def collapse_gold_missing(value: str) -> bool:
    """Gold slot -> is it in the ``missing`` class (True) or not (False)."""
    return (value or "").strip().lower() == "missing"


def llm_is_missing(value: str) -> bool:
    return (value or "").strip().lower() == "missing"


def _prf(tp: int, fp: int, fn: int) -> Dict[str, float]:
    precision = tp / (tp + fp) if (tp + fp) else 0.0
    recall = tp / (tp + fn) if (tp + fn) else 0.0
    f1 = (
        2 * precision * recall / (precision + recall)
        if (precision + recall)
        else 0.0
    )
    return {"precision": precision, "recall": recall, "f1": f1}


def slot_confusion(items: Sequence[SlotEval], slot: str) -> Dict[str, int]:
    """Confusion for the ``missing`` class on one slot.

    Positive = *missing*. TP = LLM & gold both missing; FP = LLM missing,
    gold not; FN = gold missing, LLM not; TN = both not-missing.
    """
    tp = fp = fn = tn = 0
    for it in items:
        gold_missing = collapse_gold_missing(it.gold_slots.get(slot, ""))
        pred_missing = llm_is_missing(it.llm_slots.get(slot, ""))
        if pred_missing and gold_missing:
            tp += 1
        elif pred_missing and not gold_missing:
            fp += 1
        elif not pred_missing and gold_missing:
            fn += 1
        else:
            tn += 1
    return {"tp": tp, "fp": fp, "fn": fn, "tn": tn}


def per_slot_scores(items: Sequence[SlotEval]) -> Dict[str, Dict[str, float]]:
    out: Dict[str, Dict[str, float]] = {}
    for slot in SLOTS:
        cm = slot_confusion(items, slot)
        scores = _prf(cm["tp"], cm["fp"], cm["fn"])
        out[slot] = {**cm, **scores, "support_gold_missing": cm["tp"] + cm["fn"]}
    return out


def micro_average(per_slot: Dict[str, Dict[str, float]]) -> Dict[str, float]:
    tp = sum(int(s["tp"]) for s in per_slot.values())
    fp = sum(int(s["fp"]) for s in per_slot.values())
    fn = sum(int(s["fn"]) for s in per_slot.values())
    return _prf(tp, fp, fn)


def macro_average(per_slot: Dict[str, Dict[str, float]]) -> Dict[str, float]:
    n = len(per_slot) or 1
    return {
        "precision": sum(s["precision"] for s in per_slot.values()) / n,
        "recall": sum(s["recall"] for s in per_slot.values()) / n,
        "f1": sum(s["f1"] for s in per_slot.values()) / n,
    }


def _rate(numerator: int, denominator: int) -> float:
    return numerator / denominator if denominator else 0.0


def implied_analysis(items: Sequence[SlotEval]) -> Dict[str, object]:
    """Among gold ``implied`` slots: LLM filled (good) vs flagged missing."""
    per_slot: Dict[str, Dict[str, object]] = {}
    total_implied = filled = flagged = 0
    for slot in SLOTS:
        s_implied = s_filled = s_flagged = 0
        for it in items:
            if (it.gold_slots.get(slot, "") or "").strip().lower() != "implied":
                continue
            s_implied += 1
            if llm_is_missing(it.llm_slots.get(slot, "")):
                s_flagged += 1
            else:
                s_filled += 1
        per_slot[slot] = {
            "n_implied": s_implied,
            "llm_filled": s_filled,
            "llm_flagged_missing": s_flagged,
            "fill_rate": _rate(s_filled, s_implied),
        }
        total_implied += s_implied
        filled += s_filled
        flagged += s_flagged
    return {
        "overall": {
            "n_implied": total_implied,
            "llm_filled": filled,
            "llm_flagged_missing": flagged,
            "fill_rate": _rate(filled, total_implied),
        },
        "per_slot": per_slot,
    }


def missing_analysis(items: Sequence[SlotEval]) -> Dict[str, object]:
    """Among gold ``missing`` slots: LLM correctly flagged vs silently filled."""
    per_slot: Dict[str, Dict[str, object]] = {}
    total_missing = flagged = filled = 0
    for slot in SLOTS:
        s_missing = s_flagged = s_filled = 0
        for it in items:
            if not collapse_gold_missing(it.gold_slots.get(slot, "")):
                continue
            s_missing += 1
            if llm_is_missing(it.llm_slots.get(slot, "")):
                s_flagged += 1
            else:
                s_filled += 1
        per_slot[slot] = {
            "n_missing": s_missing,
            "llm_flagged_missing": s_flagged,
            "llm_silently_filled": s_filled,
            "flag_rate": _rate(s_flagged, s_missing),
        }
        total_missing += s_missing
        flagged += s_flagged
        filled += s_filled
    return {
        "overall": {
            "n_missing": total_missing,
            "llm_flagged_missing": flagged,
            "llm_silently_filled": filled,
            "flag_rate": _rate(flagged, total_missing),
        },
        "per_slot": per_slot,
    }


def llm_overall_incomplete(llm_slots: Dict[str, str]) -> bool:
    """LLM overall-incomplete = any mandatory slot flagged missing."""
    return any(llm_is_missing(llm_slots.get(slot, "")) for slot in MANDATORY_SLOTS)


def overall_verdict(items: Sequence[SlotEval]) -> Dict[str, object]:
    """2x2 agreement between LLM overall-incomplete and gold. Positive = incomplete."""
    tp = fp = fn = tn = 0  # positive = "incomplete"
    for it in items:
        pred = llm_overall_incomplete(it.llm_slots)
        gold = bool(it.gold_overall_incomplete)
        if pred and gold:
            tp += 1
        elif pred and not gold:
            fp += 1
        elif not pred and gold:
            fn += 1
        else:
            tn += 1
    n = tp + fp + fn + tn
    return {
        "confusion": {
            "both_incomplete": tp,
            "llm_incomplete_gold_complete": fp,
            "gold_incomplete_llm_complete": fn,
            "both_complete": tn,
        },
        "agreement_rate": _rate(tp + tn, n),
        "n": n,
        **_prf(tp, fp, fn),  # P/R/F1 for the "incomplete" class
    }


@dataclass
class FieldAccuracyReport:
    n: int
    per_slot: Dict[str, Dict[str, float]]
    micro: Dict[str, float]
    macro: Dict[str, float]
    implied: Dict[str, object]
    missing: Dict[str, object]
    verdict: Dict[str, object]

    def as_dict(self) -> dict:
        return {
            "n": self.n,
            "per_slot": self.per_slot,
            "micro": self.micro,
            "macro": self.macro,
            "implied_analysis": self.implied,
            "missing_analysis": self.missing,
            "overall_verdict": self.verdict,
        }


def field_accuracy_report(items: Sequence[SlotEval]) -> FieldAccuracyReport:
    per_slot = per_slot_scores(items)
    return FieldAccuracyReport(
        n=len(items),
        per_slot=per_slot,
        micro=micro_average(per_slot),
        macro=macro_average(per_slot),
        implied=implied_analysis(items),
        missing=missing_analysis(items),
        verdict=overall_verdict(items),
    )
