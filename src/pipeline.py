"""Stage 1 single-requirement orchestration.

For one ``(req_id, nl_text)`` tuple this:

  1. Builds the prompt (``prompt_builder``).
  2. Calls the LLM (``llm_converter``) to produce Rimay with
     ``<MISSING_*>`` placeholders.
  3. Strips the placeholders and the ``<NON_ATOMIC>`` flag.
  4. Runs Paska once, on the stripped Rimay only (the sole Paska call
     in this repo), and derives ``paska_passed = (no smells)``.
  5. Logs everything to MLflow under the strategy's experiment.
  6. Returns a manifest record (the scorer's only input from Stage 1).

Paska is the only stage that can fail on infrastructure (missing
tagger, parser error); such failures are caught and recorded as
``paska_error`` with ``paska_passed = None`` so the LLM-side data and the
manifest line still appear.
"""
from __future__ import annotations

from pathlib import Path
from typing import Dict, List, Optional

import mlflow

from src import config, tracking
from src.llm_converter import LLMResponse, convert, strip_missing_placeholders
from src.paska_runner import (
    RIMAY_PATTERN_COL,
    SMELL_COLUMNS,
    PaskaResult,
    run_paska,
)


def derive_llm_slots(rimay: str) -> Dict[str, str]:
    """Map each structural slot to ``"missing"`` or ``"filled"``.

    A slot is ``"missing"`` iff its ``<MISSING_*>`` placeholder appears in
    the raw LLM Rimay; otherwise it is ``"filled"``.
    """
    return {
        slot: ("missing" if config.SLOT_PLACEHOLDERS[slot] in rimay else "filled")
        for slot in config.SLOTS
    }


def _slot_occurrences(rimay: str) -> Dict[str, int]:
    return {slot: rimay.count(config.SLOT_PLACEHOLDERS[slot]) for slot in config.SLOTS}


def _is_non_atomic(rimay: str) -> bool:
    return config.NON_ATOMIC_FLAG in rimay


def _persist_rimay(req_id: str, strategy: str, rimay: str) -> Path:
    safe_id = req_id.replace("/", "_").replace(" ", "_")
    out_dir = config.LLM_RIMAY_DIR / strategy
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{safe_id}.txt"
    path.write_text(rimay, encoding="utf-8")
    return path


def extract_smells(res: PaskaResult) -> List[dict]:
    """Flatten a PaskaResult into a list of fired-smell records.

    One record per (segment, smell-column) that has a non-empty value.
    The suggested Rimay-pattern column is not a smell and is excluded,
    matching :pyattr:`PaskaResult.smell_count`.
    """
    smells: List[dict] = []
    for seg in res.segments:
        seg_id = (seg.get("Segment ID") or "").strip()
        for col in SMELL_COLUMNS:
            if col == RIMAY_PATTERN_COL:
                continue
            val = (seg.get(col) or "").strip()
            if val:
                smells.append(
                    {"segment_id": seg_id, "smell": col.strip(), "value": val}
                )
    return smells


def _run_paska_safe(
    items,
) -> tuple[Optional[PaskaResult], Optional[str]]:
    """Run Paska, returning (result, error_message). One of them is None."""
    try:
        results = run_paska(items, source="rimay")
    except Exception as exc:  # noqa: BLE001 — infra failures shouldn't drop the row
        return None, f"{type(exc).__name__}: {exc}"
    if not results:
        return None, "Paska returned an empty result mapping."
    expected_id = items[0][0]
    res = results.get(expected_id) or next(iter(results.values()), None)
    if res is None:
        return None, f"Paska produced no rows for req_id={expected_id!r}."
    return res, None


def run_single(
    req_id: str,
    nl_text: str,
    *,
    run_cfg: config.RunConfig,
) -> dict:
    """Convert one requirement, log to MLflow, return its manifest record."""
    config.ensure_output_dirs()

    with tracking.start_run(
        strategy=run_cfg.strategy,
        req_id=req_id,
        model_name=run_cfg.model,
    ):
        params = {
            "temperature": run_cfg.temperature,
            "max_tokens": run_cfg.max_tokens,
        }
        if run_cfg.strategy == "fsl":
            params["n_fsl_examples"] = run_cfg.n_fsl_examples
        tracking.log_params(params)

        with mlflow.start_span(name="pipeline.run_single", span_type="CHAIN") as root:
            root.set_inputs(
                {
                    "req_id": req_id,
                    "strategy": run_cfg.strategy,
                    "model": run_cfg.model,
                    "nl_text": nl_text,
                }
            )

            # 1-2. LLM conversion (raw Rimay with placeholders)
            llm: LLMResponse = convert(nl_text, run_cfg=run_cfg)
            _persist_rimay(req_id, run_cfg.strategy, llm.rimay)

            # 3. Strip placeholders for Paska
            rimay_stripped = strip_missing_placeholders(llm.rimay)

            # Derived slot / structural signals
            llm_slots = derive_llm_slots(llm.rimay)
            occurrences = _slot_occurrences(llm.rimay)
            n_missing_placeholders = sum(occurrences.values())
            non_atomic = _is_non_atomic(llm.rimay)

            # 4. Paska (only call in the repo), on the stripped Rimay
            paska_res, paska_error = _run_paska_safe([(f"{req_id}_rimay", rimay_stripped)])
            if paska_res is not None:
                paska_smells = extract_smells(paska_res)
                paska_passed: Optional[bool] = len(paska_smells) == 0
                n_paska_smells = len(paska_smells)
            else:
                paska_smells = []
                paska_passed = None
                n_paska_smells = None

            # Artifacts
            tracking.log_text_artifact(nl_text, "nl_text.txt")
            tracking.log_text_artifact(llm.rimay, "rimay.txt")
            if llm.raw != llm.rimay:
                # Full model response (e.g. leaked CoT scratchpad) for audit.
                tracking.log_text_artifact(llm.raw, "rimay_raw.txt")
            tracking.log_text_artifact(rimay_stripped, "rimay_stripped.txt")
            tracking.log_text_artifact(llm.prompt.system, "prompt_system.md")
            tracking.log_text_artifact(llm.prompt.user, "prompt_user.md")
            paska_artifact = {
                "req_id": req_id,
                "paska_passed": paska_passed,
                "n_smells": n_paska_smells,
                "smells": paska_smells,
                "suggested_rimay_patterns": (
                    paska_res.suggested_rimay_patterns if paska_res else []
                ),
                "error": paska_error,
                "segments": paska_res.segments if paska_res else [],
            }
            tracking.log_json_artifact(paska_artifact, "paska.json")

            # Metrics
            metrics = {
                "n_missing_placeholders": n_missing_placeholders,
                "latency_ms": llm.latency_ms,
            }
            metric_slot_name = {
                "scope": "missing_scope",
                "condition": "missing_condition",
                "actor": "missing_actor",
                "modalVerb": "missing_modal_verb",
                "action": "missing_action",
            }
            for slot, value in llm_slots.items():
                metrics[metric_slot_name[slot]] = 1 if value == "missing" else 0
            if n_paska_smells is not None:
                metrics["n_paska_smells"] = n_paska_smells
            if llm.input_tokens is not None:
                metrics["input_tokens"] = llm.input_tokens
            if llm.output_tokens is not None:
                metrics["output_tokens"] = llm.output_tokens
            tracking.log_metrics(metrics)

            # Tags
            mlflow.set_tag(
                "paska_passed",
                "error" if paska_passed is None else str(paska_passed).lower(),
            )
            mlflow.set_tag("is_non_atomic", str(non_atomic).lower())
            if llm.stop_reason:
                mlflow.set_tag("llm_stop_reason", llm.stop_reason)

            # Root span summary (Traces UI row)
            root.set_outputs(
                {
                    "rimay": llm.rimay,
                    "rimay_stripped": rimay_stripped,
                    "llm_slots": llm_slots,
                    "n_missing_placeholders": n_missing_placeholders,
                    "is_non_atomic": non_atomic,
                    "paska_passed": paska_passed,
                    "n_paska_smells": n_paska_smells,
                    "paska_smells": paska_smells,
                    "paska_error": paska_error,
                    "latency_ms": llm.latency_ms,
                }
            )

    return {
        "reqId": req_id,
        "strategy": run_cfg.strategy,
        "rimay": llm.rimay,
        "rimay_stripped": rimay_stripped,
        "llm_slots": llm_slots,
        "is_non_atomic": non_atomic,
        "paska_passed": paska_passed,
        "paska_smells": paska_smells,
        "paska_error": paska_error,
        "model": llm.model,
        "latency_ms": llm.latency_ms,
        "input_tokens": llm.input_tokens,
        "output_tokens": llm.output_tokens,
    }
