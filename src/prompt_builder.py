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


def _load_fsl_examples(n: int, filename: str = "fsl_examples.json") -> List[dict]:
    path = config.PROMPTS_DIR / "examples" / filename
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
        lines.append("Rimay:")
        lines.extend(["<rimay>", rimay, "</rimay>"])
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


# --- units task: a non-atomic requirement converted into N atomic units ----------
#
# The system prompt's atomicity rule (pick one action, flag <NON_ATOMIC>) is the
# opposite of what this task asks, so for it those three passages are swapped
# for a units rule. Everything else — grammar, placeholders — is shared.

_ATOMIC_PASSAGES = {
    "Use `<NON_ATOMIC>` at the end of the Rimay output if the source NL contains multiple system responses. "
    "Rimay enforces atomicity (one system response per requirement); when the source has more than one action, "
    "select the most concrete primary action and append `<NON_ATOMIC>`.":
    "The source NL may contain several system responses. When the task says how many, convert each into its own "
    "atomic Rimay requirement — one system response per requirement — so the output has exactly that many "
    "requirements. Do not use `<NON_ATOMIC>`.",

    "Write the Rimay requirement on one line inside a single answer block: a line `<rimay>`, the requirement, "
    "then a line `</rimay>`. Only the block is read. Do not write commentary, explanations, headings or corrections "
    "inside it, and no markdown. If you are asked to reason first, the reasoning goes before the block; otherwise "
    "write nothing but the block. End the requirement with a period (the `<NON_ATOMIC>` flag, if used, comes after "
    "the period).":
    "Write the Rimay requirements inside a single answer block: a line `<rimay>`, one Rimay requirement per line, "
    "then a line `</rimay>`. Only the block is read. Do not write commentary, explanations, headings, numbering, "
    "bullets or corrections inside it, and no markdown. If you are asked to reason first, the reasoning goes before "
    "the block; otherwise write nothing but the block. End each requirement with a period.",

    "If the source NL violates atomicity, follow the convention above (`<NON_ATOMIC>` flag). "
    "Do not silently merge multiple actions into one Rimay action phrase.":
    "Each output requirement holds exactly one system response. Do not merge several actions into one Rimay "
    "action phrase, and do not split one action across requirements.",
}


# Experiment 3 (blind): the count is not given, so the first passage asks the
# model to decide it; the other two passages are shared with the units task.
_BLIND_FIRST = (
    "The source NL may be atomic or non-atomic (see \"What counts as one system response\" below). Decide how "
    "many system responses it contains and convert each into its own atomic Rimay requirement — one system "
    "response per requirement. An atomic source gives a single requirement. Do not use `<NON_ATOMIC>`."
)


@lru_cache(maxsize=2)
def _load_units_system_prompt(blind: bool = False) -> str:
    text = _load_system_prompt()
    passages = dict(_ATOMIC_PASSAGES)
    if blind:
        first = next(iter(passages))
        passages[first] = _BLIND_FIRST
    for old, new in passages.items():
        if old not in text:
            raise RuntimeError(f"system_prompt.md changed; cannot find the atomicity passage: {old[:60]}…")
        text = text.replace(old, new)
    return text


def _format_units_examples(examples: List[dict], show_count: bool = True) -> str:
    lines: List[str] = []
    for i, ex in enumerate(examples, start=1):
        lines.append(f"Example {i}:")
        lines.append(f"NL: {(ex.get('nl') or '').strip()}")
        if show_count:
            lines.append(f"Number of system responses: {len(ex['units'])}")
        lines.append("Rimay:")
        lines.append("<rimay>")
        lines.extend(u.strip() for u in ex["units"])
        lines.append("</rimay>")
        lines.append("")
    return "\n".join(lines).rstrip()


def _build_units(nl_text: str, *, run_cfg: config.RunConfig, n_units: int) -> str:
    template = _load_user_template(f"{run_cfg.strategy}_units")
    fields = {"nl_text": nl_text, "n_units": n_units}
    if run_cfg.strategy == "fsl":
        examples = _load_fsl_examples(run_cfg.n_fsl_examples, "fsl_units_examples.json")
        fields["examples_block"] = _format_units_examples(examples)
    return template.format(**fields)


def _blind_examples(n: int) -> List[dict]:
    """n atomic and n non-atomic examples, alternating, as ``{nl, units}``."""
    atomic = [{"nl": ex.get("nl"), "units": [ex["rimay"]]} for ex in _load_fsl_examples(n)]
    split = _load_fsl_examples(n, "fsl_units_examples.json")
    return [ex for pair in zip(atomic, split) for ex in pair]


def _build_blind(nl_text: str, *, run_cfg: config.RunConfig) -> str:
    template = _load_user_template(f"{run_cfg.strategy}_blind")
    fields = {"nl_text": nl_text}
    if run_cfg.strategy == "fsl":
        fields["examples_block"] = _format_units_examples(_blind_examples(run_cfg.n_fsl_examples), show_count=False)
    return template.format(**fields)


STRATEGIES: Dict[str, Callable[..., str]] = {
    "zsl": _build_zsl,
    "fsl": _build_fsl,
    "cot": _build_cot,
}


def build_prompt(nl_text: str, *, run_cfg: config.RunConfig, n_units: Optional[int] = None,
                 blind: bool = False) -> BuiltPrompt:
    """``n_units`` set → the units task: convert into exactly that many requirements.
    ``blind`` → experiment 3: the model decides atomicity and the number of units."""
    if n_units is not None or blind:
        if run_cfg.strategy not in STRATEGIES:
            raise ValueError(f"Unknown strategy {run_cfg.strategy!r}. Known: {sorted(STRATEGIES)}")
        if blind:
            return BuiltPrompt(system=_load_units_system_prompt(blind=True),
                               user=_build_blind(nl_text, run_cfg=run_cfg))
        return BuiltPrompt(system=_load_units_system_prompt(),
                           user=_build_units(nl_text, run_cfg=run_cfg, n_units=n_units))
    builder = STRATEGIES.get(run_cfg.strategy)
    if builder is None:
        raise ValueError(
            f"Unknown strategy {run_cfg.strategy!r}. "
            f"Known: {sorted(STRATEGIES)}"
        )
    user = builder(nl_text, run_cfg=run_cfg)
    return BuiltPrompt(system=_load_system_prompt(), user=user)
