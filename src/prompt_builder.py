"""Strategy-aware prompt assembly for NL → Rimay conversion.

Adding a new prompting strategy means:
  1. Drop a `<name>_prompt.md` file into `prompts/`.
  2. Register it in STRATEGIES below.

Nothing else. The pipeline picks up the new strategy automatically.

The system prompt is shared across strategies. We assert that the
`[RIMAY_GRAMMAR_PLACEHOLDER]` line in `system_prompt.md` has been
replaced before any prompt is built — running the pipeline against an
empty grammar would produce meaningless Rimay.
"""
from __future__ import annotations

import json
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Callable, Dict, List, Optional

from src import config

GRAMMAR_PLACEHOLDER = "[RIMAY_GRAMMAR_PLACEHOLDER]"


@dataclass(frozen=True)
class BuiltPrompt:
    system: str
    user: str


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


@lru_cache(maxsize=1)
def _load_system_prompt() -> str:
    text = _read_text(config.PROMPTS_DIR / "system_prompt.md")
    if GRAMMAR_PLACEHOLDER in text:
        raise RuntimeError(
            f"{config.PROMPTS_DIR / 'system_prompt.md'} still contains the "
            f"`{GRAMMAR_PLACEHOLDER}` placeholder. Paste the Rimay grammar "
            "reference into the file before running."
        )
    return text


def _load_user_template(strategy: str) -> str:
    path = config.PROMPTS_DIR / f"{strategy}_prompt.md"
    if not path.is_file():
        raise FileNotFoundError(
            f"No prompt template for strategy={strategy!r}. "
            f"Expected file: {path}"
        )
    return _read_text(path)


def _load_fsl_examples(n: int) -> List[dict]:
    path = config.PROMPTS_DIR / "examples" / "fsl_examples.json"
    if not path.is_file():
        raise FileNotFoundError(f"FSL examples file missing: {path}")
    examples = json.loads(_read_text(path))
    if not isinstance(examples, list):
        raise ValueError(f"{path} must contain a JSON array")
    if len(examples) < n:
        raise ValueError(
            f"FSL strategy needs at least {n} examples in {path}, found {len(examples)}. "
            "Populate the file or lower --n-fsl-examples."
        )
    return examples[:n]


def _format_examples(examples: List[dict]) -> str:
    lines: List[str] = []
    for i, ex in enumerate(examples, start=1):
        nl = (ex.get("nl") or "").strip()
        rimay = (ex.get("rimay") or "").strip()
        lines.append(f"Example {i}:")
        lines.append(f"NL: {nl}")
        lines.append(f"Rimay: {rimay}")
        lines.append("")
    return "\n".join(lines).rstrip()


# --- per-strategy builders ---------------------------------------------------


def _build_zsl(nl_text: str, *, run_cfg: config.RunConfig) -> str:
    template = _load_user_template("zsl")
    return template.format(nl_text=nl_text)


def _build_fsl(nl_text: str, *, run_cfg: config.RunConfig) -> str:
    template = _load_user_template("fsl")
    examples = _load_fsl_examples(run_cfg.n_fsl_examples)
    return template.format(
        nl_text=nl_text,
        examples_block=_format_examples(examples),
    )


def _build_cot(nl_text: str, *, run_cfg: config.RunConfig) -> str:
    template = _load_user_template("cot")
    return template.format(nl_text=nl_text)


STRATEGIES: Dict[str, Callable[..., str]] = {
    "zsl": _build_zsl,
    "fsl": _build_fsl,
    "cot": _build_cot,
}


def build_prompt(nl_text: str, *, run_cfg: config.RunConfig) -> BuiltPrompt:
    builder = STRATEGIES.get(run_cfg.strategy)
    if builder is None:
        raise ValueError(
            f"Unknown strategy {run_cfg.strategy!r}. "
            f"Known: {sorted(STRATEGIES)}"
        )
    user = builder(nl_text, run_cfg=run_cfg)
    return BuiltPrompt(system=_load_system_prompt(), user=user)
