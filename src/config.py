"""Central configuration: paths, model defaults, placeholder tokens, env loading.

Everything path-like is anchored to the repo root so scripts work from
any CWD. Secrets and machine-specific paths come from ``.env``
(see ``.env.example``).
"""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parent.parent

load_dotenv(PROJECT_ROOT / ".env", override=True)

DATA_DIR = PROJECT_ROOT / "data"
PASKA_DIR = PROJECT_ROOT / "paska"
PROMPTS_DIR = PROJECT_ROOT / "prompts"
OUTPUTS_DIR = PROJECT_ROOT / "outputs"
MLRUNS_DIR = PROJECT_ROOT / "mlruns"

GOLD_CSV = DATA_DIR / "gold_annotations.csv"
FSL_EXAMPLES_JSON = PROMPTS_DIR / "examples" / "fsl_examples.json"

PASKA_JAR = PASKA_DIR / "smell_detector.jar"

LLM_RIMAY_DIR = OUTPUTS_DIR / "llm_rimay"
CONVERSIONS_DIR = OUTPUTS_DIR / "conversions"
PASKA_PARSING_TREES_DIR = OUTPUTS_DIR / "paska_parsing_trees"
PASKA_SMELLS_DIR = OUTPUTS_DIR / "paska_smells"
PASKA_SMELLS_CACHE_DIR = PASKA_SMELLS_DIR / ".cache"
SCORING_DIR = OUTPUTS_DIR / "scoring"

DEFAULT_MODEL = os.environ.get("ANTHROPIC_MODEL", "claude-haiku-4-5-20251001")
DEFAULT_TEMPERATURE = 0.0
DEFAULT_MAX_TOKENS = 1024
DEFAULT_N_FSL_EXAMPLES = 2

PASKA_POS_TAGGER_PATH = os.environ.get("PASKA_POS_TAGGER_PATH", "")

MLFLOW_TRACKING_DB = MLRUNS_DIR / "mlflow.db"

# The five structural slots, in gold-column order. Keys are the slot
# names used everywhere (manifest llm_slots, gold_* columns, scoring).
SLOTS = ("scope", "condition", "actor", "modalVerb", "action")

# Slots whose absence makes a requirement incomplete overall.
MANDATORY_SLOTS = ("actor", "modalVerb", "action")

# slot name -> the placeholder token the LLM emits when the slot is absent.
SLOT_PLACEHOLDERS = {
    "scope": "<MISSING_SCOPE>",
    "condition": "<MISSING_CONDITION>",
    "actor": "<MISSING_ACTOR>",
    "modalVerb": "<MISSING_MODAL_VERB>",
    "action": "<MISSING_ACTION>",
}
MISSING_PLACEHOLDERS = tuple(SLOT_PLACEHOLDERS.values())
NON_ATOMIC_FLAG = "<NON_ATOMIC>"


@dataclass(frozen=True)
class RunConfig:
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
