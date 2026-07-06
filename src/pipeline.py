"""Single-requirement conversion orchestration (Stage 1).

For one ``(req_id, nl_text)`` tuple this:

  1. Builds the prompt (via ``prompt_builder``).
  2. Calls the LLM (via ``llm_converter``) to get Rimay with
     ``<MISSING_*>`` placeholders; persists it under
     ``outputs/llm_rimay/<strategy>/<reqId>.txt``.
  3. Strips placeholders and runs Paska on the stripped Rimay — the
     only Paska invocation in the whole repo.
  4. Logs tags/params/metrics/artifacts to MLflow under the strategy's
     ``gold_<strategy>`` experiment, with trace spans for the LLM and
     Paska calls.
  5. Returns a ``ConversionOutcome`` whose ``manifest_line()`` is the
     JSON object appended to ``outputs/conversions/<strategy>.jsonl``
     — the scorer's single input from this stage (the scorer never
     reads MLflow).

Paska failures (missing tagger, parser error, …) are caught and
recorded (``paska_error``) so the LLM-side data still lands in MLflow
and the manifest; ``paska_passed`` is ``None`` in that case.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Optional

import mlflow

from src import config, tracking
from src.llm_converter import (
    LLMResponse,
    convert,
    derive_llm_slots,
    strip_missing_placeholders,
)
from src.paska_runner import RIMAY_PATTERN_COL, SMELL_COLUMNS, PaskaResult, run_paska


@dataclass
class ConversionOutcome:
    req_id: str
    strategy: str
    nl_text: str
    rimay: str
    rimay_stripped: str
    llm_slots: Dict[str, str]  # slot -> "missing" | "filled"
    is_non_atomic: bool
    paska_passed: Optional[bool]  # None when Paska errored
    paska_smells: List[str]
    paska_error: Optional[str]
    model: str
    latency_ms: int

    def manifest_line(self) -> dict:
        line = {
            "reqId": self.req_id,
            "strategy": self.strategy,
            "rimay": self.rimay,
            "rimay_stripped": self.rimay_stripped,
            "llm_slots": self.llm_slots,
            "paska_passed": self.paska_passed,
            "paska_smells": self.paska_smells,
            "model": self.model,
            "latency_ms": self.latency_ms,
        }
        if self.paska_error:
            line["paska_error"] = self.paska_error
        return line


def _smell_names(result: PaskaResult) -> List[str]:
    """Flatten a PaskaResult into the list of smell types that fired.

    One entry per (segment, smell) hit, so repeated smells across
    segments keep their multiplicity for the Track 2 frequency counts.
    The suggested-Rimay-pattern column is advice, not a smell.
    """
    names: List[str] = []
    for seg in result.segments:
        for col in SMELL_COLUMNS:
            if col == RIMAY_PATTERN_COL:
                continue
            if (seg.get(col) or "").strip():
                names.append(col)
    return names


def _persist_rimay(req_id: str, strategy: str, rimay: str) -> Path:
    safe_id = req_id.replace("/", "_").replace(" ", "_")
    out_dir = config.LLM_RIMAY_DIR / strategy
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{safe_id}.txt"
    path.write_text(rimay, encoding="utf-8")
    return path


def run_single(
    req_id: str,
    nl_text: str,
    *,
    run_cfg: config.RunConfig,
) -> ConversionOutcome:
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
        tracking.log_text_artifact(nl_text, "nl_text.txt")

        with mlflow.start_span(name="pipeline.run_single", span_type="CHAIN") as root_span:
            root_span.set_inputs(
                {
                    "req_id": req_id,
                    "strategy": run_cfg.strategy,
                    "model": run_cfg.model,
                    "nl_text": nl_text,
                }
            )

            # 1. LLM conversion
            llm_response: LLMResponse = convert(nl_text, run_cfg=run_cfg)
            _persist_rimay(req_id, run_cfg.strategy, llm_response.rimay)
            tracking.log_text_artifact(llm_response.rimay, "rimay.txt")
            tracking.log_text_artifact(llm_response.prompt.user, "prompt_user.md")
            tracking.log_text_artifact(llm_response.prompt.system, "prompt_system.md")

            llm_slots = derive_llm_slots(llm_response.rimay)
            n_missing = sum(1 for v in llm_slots.values() if v == "missing")
            non_atomic = config.NON_ATOMIC_FLAG in llm_response.rimay

            # 2. Paska on the stripped Rimay (the only Paska call)
            stripped = strip_missing_placeholders(llm_response.rimay)
            tracking.log_text_artifact(stripped, "rimay_stripped.txt")

            paska_passed: Optional[bool] = None
            paska_smells: List[str] = []
            paska_error: Optional[str] = None
            try:
                results = run_paska([(req_id, stripped)], source="rimay")
                result = results.get(req_id)
                if result is None:
                    paska_error = (
                        f"Paska produced no rows for req_id={req_id!r}; "
                        f"got rows for: {list(results)}"
                    )
                else:
                    paska_smells = _smell_names(result)
                    paska_passed = not paska_smells
                    tracking.log_json_artifact(result.to_dict(), "paska.json")
            except Exception as exc:  # noqa: BLE001 — one bad requirement must not kill a batch
                paska_error = f"{type(exc).__name__}: {exc}"
            if paska_error:
                tracking.log_text_artifact(paska_error, "paska_error.txt")

            # 3. Metrics
            metrics = {
                "n_missing_placeholders": n_missing,
                "n_paska_smells": len(paska_smells),
                "latency_ms": llm_response.latency_ms,
                "input_tokens": llm_response.input_tokens,
                "output_tokens": llm_response.output_tokens,
            }
            for slot, placeholder in config.SLOT_PLACEHOLDERS.items():
                # e.g. <MISSING_MODAL_VERB> -> missing_modal_verb
                metrics[placeholder.strip("<>").lower()] = int(llm_slots[slot] == "missing")
            tracking.log_metrics(metrics)

            # 4. Tags
            mlflow.set_tag(
                "paska_passed",
                "error" if paska_passed is None else str(paska_passed).lower(),
            )
            mlflow.set_tag("is_non_atomic", str(non_atomic).lower())
            if llm_response.stop_reason:
                mlflow.set_tag("llm_stop_reason", llm_response.stop_reason)

            # 5. Root span outputs (Traces UI row summary)
            root_span.set_outputs(
                {
                    "rimay": llm_response.rimay,
                    "rimay_stripped": stripped,
                    "llm_slots": llm_slots,
                    "n_missing_placeholders": n_missing,
                    "is_non_atomic": non_atomic,
                    "paska_passed": paska_passed,
                    "paska_smells": paska_smells,
                    "paska_error": paska_error,
                    "llm_latency_ms": llm_response.latency_ms,
                    "input_tokens": llm_response.input_tokens,
                    "output_tokens": llm_response.output_tokens,
                }
            )

    return ConversionOutcome(
        req_id=req_id,
        strategy=run_cfg.strategy,
        nl_text=nl_text,
        rimay=llm_response.rimay,
        rimay_stripped=stripped,
        llm_slots=llm_slots,
        is_non_atomic=non_atomic,
        paska_passed=paska_passed,
        paska_smells=paska_smells,
        paska_error=paska_error,
        model=llm_response.model,
        latency_ms=llm_response.latency_ms,
    )
