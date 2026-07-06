"""MLflow setup and helpers.

Backend: SQLite at ``mlruns/mlflow.db`` (required for the trace UI;
FileStore does not support trace queries). Artifacts live under
``mlruns/<exp_id>/<run_id>/artifacts/``.

Each strategy gets its own experiment (``gold_zsl``, ``gold_fsl``,
``gold_cot``); each requirement processed becomes a run inside the
matching experiment. Anthropic autolog is enabled so the Traces tab
shows the LLM request/response and Paska spans for each requirement.

Stage 2 (scoring) never touches MLflow — its only input from Stage 1
is the JSONL manifest under ``outputs/conversions/``.
"""
from __future__ import annotations

import json
import time
from contextlib import contextmanager
from typing import Iterator

import mlflow
import mlflow.anthropic

from src import config

_AUTOLOG_ENABLED = False


def _tracking_uri() -> str:
    return f"sqlite:///{config.MLFLOW_TRACKING_DB.resolve()}"


def _enable_autolog_once() -> None:
    """Patch the Anthropic SDK so every messages.create() emits a span.

    Idempotent — safe to call from every run.
    """
    global _AUTOLOG_ENABLED
    if _AUTOLOG_ENABLED:
        return
    mlflow.anthropic.autolog(log_traces=True, disable=False, silent=True)
    _AUTOLOG_ENABLED = True


def init_tracking(strategy: str) -> str:
    """Configure MLflow and return the experiment ID."""
    config.ensure_output_dirs()
    mlflow.set_tracking_uri(_tracking_uri())
    _enable_autolog_once()
    experiment_name = f"gold_{strategy}"
    return mlflow.set_experiment(experiment_name).experiment_id


@contextmanager
def start_run(
    *,
    strategy: str,
    req_id: str,
    model_name: str,
) -> Iterator[mlflow.ActiveRun]:
    init_tracking(strategy)
    run_name = f"{strategy}_{req_id}_{int(time.time())}"
    with mlflow.start_run(run_name=run_name) as run:
        mlflow.set_tags(
            {
                "strategy": strategy,
                "model_name": model_name,
                "req_id": req_id,
            }
        )
        yield run


def log_text_artifact(content: str, filename: str) -> None:
    mlflow.log_text(content, artifact_file=filename)


def log_json_artifact(payload: object, filename: str) -> None:
    mlflow.log_text(
        json.dumps(payload, indent=2, ensure_ascii=False, default=str),
        artifact_file=filename,
    )


def log_params(params: dict) -> None:
    mlflow.log_params({k: v for k, v in params.items() if v is not None})


def log_metrics(metrics: dict) -> None:
    mlflow.log_metrics({k: v for k, v in metrics.items() if v is not None})
