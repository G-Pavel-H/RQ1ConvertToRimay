"""Central configuration: paths, model defaults, placeholder tokens, env.

Two decoupled stages share this module:

  * Stage 1 (conversion) uses the model/prompt defaults, the Paska paths,
    and MLflow settings.
  * Stage 2 (scoring) uses only the path constants and the placeholder
    tokens; it never touches MLflow.
"""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parent.parent

load_dotenv(PROJECT_ROOT / ".env", override=True)

# --- top-level directories ---------------------------------------------------
DATA_DIR = PROJECT_ROOT / "data"
PASKA_DIR = PROJECT_ROOT / "paska"
PROMPTS_DIR = PROJECT_ROOT / "prompts"
OUTPUTS_DIR = PROJECT_ROOT / "outputs"
MLRUNS_DIR = PROJECT_ROOT / "mlruns"

# --- the human gold standard (Stage 2 reference; also the Stage 1 input) -----
GOLD_CSV = DATA_DIR / "gold_annotations.csv"

# --- Paska integration -------------------------------------------------------
PASKA_JAR = PASKA_DIR / "smell_detector.jar"

LLM_RIMAY_DIR = OUTPUTS_DIR / "llm_rimay"
CONVERSIONS_DIR = OUTPUTS_DIR / "conversions"
PASKA_PARSING_TREES_DIR = OUTPUTS_DIR / "paska_parsing_trees"
PASKA_SMELLS_DIR = OUTPUTS_DIR / "paska_smells"
PASKA_SMELLS_CACHE_DIR = PASKA_SMELLS_DIR / ".cache"
SCORING_DIR = OUTPUTS_DIR / "scoring"

PASKA_POS_TAGGER_PATH = os.environ.get("PASKA_POS_TAGGER_PATH", "")

# --- LLM defaults ------------------------------------------------------------
DEFAULT_MODEL = os.environ.get("ANTHROPIC_MODEL", "claude-haiku-4-5-20251001")
DEFAULT_TEMPERATURE = 0.0
DEFAULT_MAX_TOKENS = 1024
DEFAULT_N_FSL_EXAMPLES = 2

# --- MLflow ------------------------------------------------------------------
MLFLOW_TRACKING_DB = MLRUNS_DIR / "mlflow.db"

# --- placeholder convention (shared by Stage 1 stripping & Stage 2 slots) ----
# The canonical slot order used everywhere the five structural slots appear.
SLOTS = ("scope", "condition", "actor", "modalVerb", "action")
# Mandatory slots: the overall-incomplete verdict fires if any of these is
# flagged missing by the LLM.
MANDATORY_SLOTS = ("actor", "modalVerb", "action")

# Map each slot to its <MISSING_*> placeholder token.
SLOT_PLACEHOLDERS = {
    "scope": "<MISSING_SCOPE>",
    "condition": "<MISSING_CONDITION>",
    "actor": "<MISSING_ACTOR>",
    "modalVerb": "<MISSING_MODAL_VERB>",
    "action": "<MISSING_ACTION>",
}
MISSING_PLACEHOLDERS = tuple(SLOT_PLACEHOLDERS[s] for s in SLOTS)
NON_ATOMIC_FLAG = "<NON_ATOMIC>"


@dataclass(frozen=True)
class RunConfig:
    """Immutable Stage 1 run configuration for a single conversion pass."""

    strategy: str
    model: str = DEFAULT_MODEL
    temperature: float = DEFAULT_TEMPERATURE
    max_tokens: int = DEFAULT_MAX_TOKENS
    n_fsl_examples: int = DEFAULT_N_FSL_EXAMPLES


def ensure_output_dirs() -> None:
    for d in (
        LLM_RIMAY_DIR,
        CONVERSIONS_DIR,
        PASKA_PARSING_TREES_DIR,
        PASKA_SMELLS_DIR,
        PASKA_SMELLS_CACHE_DIR,
        SCORING_DIR,
        MLRUNS_DIR,
    ):
        d.mkdir(parents=True, exist_ok=True)
