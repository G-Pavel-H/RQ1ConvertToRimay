"""Central configuration: paths, model defaults, placeholder tokens, env.

Two decoupled stages share this module:

  * Stage 1 (conversion) uses the model/prompt defaults, the Paska paths,
    and MLflow settings.
  * Stage 2 (scoring) uses only the path constants and the placeholder
    tokens; it never touches MLflow.
"""
from __future__ import annotations

import os
import re
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

# Paska working files + cache live OUTSIDE the per-run folders and are shared
# across runs, so identical Rimay text is never re-parsed (cache key = SHA-256
# of the input). This is transient/derived; safe to delete.
PASKA_WORK_DIR = OUTPUTS_DIR / "_paska"
PASKA_PARSING_TREES_DIR = PASKA_WORK_DIR / "parsing_trees"
PASKA_SMELLS_DIR = PASKA_WORK_DIR / "smells"
PASKA_SMELLS_CACHE_DIR = PASKA_WORK_DIR / ".cache"

PASKA_POS_TAGGER_PATH = os.environ.get("PASKA_POS_TAGGER_PATH", "")

# --- LLM defaults ------------------------------------------------------------
DEFAULT_MODEL = os.environ.get("ANTHROPIC_MODEL", "claude-haiku-4-5-20251001")
DEFAULT_TEMPERATURE = 0.0
DEFAULT_MAX_TOKENS = 1024
DEFAULT_N_FSL_EXAMPLES = 3

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
    """Create the global (non-per-run) output dirs."""
    for d in (
        OUTPUTS_DIR,
        PASKA_PARSING_TREES_DIR,
        PASKA_SMELLS_DIR,
        PASKA_SMELLS_CACHE_DIR,
        MLRUNS_DIR,
    ):
        d.mkdir(parents=True, exist_ok=True)


# --- per-run output layout ---------------------------------------------------
_RUN_DIR_RE = re.compile(r"^run(\d+)_")


@dataclass(frozen=True)
class RunPaths:
    """Filesystem layout for one self-contained run under ``outputs/<run_id>/``.

    A run bundles everything produced for a single conversion pass: the LLM
    Rimay files, the JSONL manifest, the scoring reports, and a metadata
    sidecar. Stage 2 reads and writes inside the same folder, so a run is a
    complete, portable record.
    """

    run_id: str

    @property
    def root(self) -> Path:
        return OUTPUTS_DIR / self.run_id

    @property
    def llm_rimay_dir(self) -> Path:
        return self.root / "llm_rimay"

    @property
    def conversions_dir(self) -> Path:
        return self.root / "conversions"

    @property
    def manifest_path(self) -> Path:
        return self.conversions_dir / "manifest.jsonl"

    @property
    def scoring_dir(self) -> Path:
        return self.root / "scoring"

    @property
    def meta_path(self) -> Path:
        return self.root / "run_meta.json"

    def ensure(self) -> None:
        for d in (self.llm_rimay_dir, self.conversions_dir, self.scoring_dir):
            d.mkdir(parents=True, exist_ok=True)


def next_run_id(strategy: str, n_fsl_examples: int | None = None) -> str:
    """Allocate the next run id, e.g. ``run3_fsl-n3``.

    The numeric prefix auto-increments past the highest existing ``runN_*``
    folder under ``outputs/`` (across all strategies), so runs stay ordered
    and never clobber each other. The suffix records the strategy (and, for
    FSL, the exemplar count) so the folder name is self-describing.
    """
    highest = 0
    if OUTPUTS_DIR.is_dir():
        for p in OUTPUTS_DIR.iterdir():
            m = _RUN_DIR_RE.match(p.name)
            if p.is_dir() and m:
                highest = max(highest, int(m.group(1)))
    suffix = strategy
    if strategy == "fsl" and n_fsl_examples is not None:
        suffix += f"-n{n_fsl_examples}"
    return f"run{highest + 1}_{suffix}"
