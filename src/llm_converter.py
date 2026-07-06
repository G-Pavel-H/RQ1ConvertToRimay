"""NL → Rimay conversion via the Anthropic SDK."""
from __future__ import annotations

import os
import re
import time
from dataclasses import dataclass
from typing import Optional

import mlflow
from anthropic import Anthropic

from src import config
from src.prompt_builder import BuiltPrompt, build_prompt

_PLACEHOLDER_RE = re.compile(r"<MISSING_[A-Z_]+>")
_NON_ATOMIC_RE = re.compile(r"<NON_ATOMIC>")
_LEADING_PUNCT_RE = re.compile(r"^[\s,;:.]+")
_DUP_PUNCT_RE = re.compile(r"([,;:])\s*\1+")
_SPACE_BEFORE_PUNCT_RE = re.compile(r"\s+([,.;:!?])")
_MULTI_SPACE_RE = re.compile(r"[ \t]{2,}")
_LEFTOVER_COMMA_DOT_RE = re.compile(r",\s*\.")


@dataclass(frozen=True)
class LLMResponse:
    rimay: str
    prompt: BuiltPrompt
    model: str
    input_tokens: Optional[int]
    output_tokens: Optional[int]
    latency_ms: int
    stop_reason: Optional[str]


_client: Optional[Anthropic] = None


def _get_client() -> Anthropic:
    global _client
    if _client is None:
        if not os.environ.get("ANTHROPIC_API_KEY"):
            raise RuntimeError(
                "ANTHROPIC_API_KEY is not set. Add it to .env or export it."
            )
        _client = Anthropic()
    return _client


def strip_missing_placeholders(rimay: str) -> str:
    """Remove ``<MISSING_*>`` placeholders and the ``<NON_ATOMIC>`` flag.

    Used to feed the LLM-generated Rimay into Paska without the literal
    placeholder tokens triggering parser failures or misclassification.

    Each placeholder is replaced with an empty string. Stranded
    punctuation that the placeholder used to anchor (leading commas,
    doubled commas, comma-then-period, space-before-comma, etc.) is
    cleaned up. Stripping is conservative: it does not try to repair the
    grammar, just to keep Paska's tokeniser happy.
    """
    s = _PLACEHOLDER_RE.sub("", rimay)
    s = _NON_ATOMIC_RE.sub("", s)
    s = _SPACE_BEFORE_PUNCT_RE.sub(r"\1", s)
    s = _DUP_PUNCT_RE.sub(r"\1", s)
    s = _LEFTOVER_COMMA_DOT_RE.sub(".", s)
    s = _MULTI_SPACE_RE.sub(" ", s)
    s = _LEADING_PUNCT_RE.sub("", s)
    return s.strip()


def derive_llm_slots(rimay: str) -> dict:
    """Map the raw LLM Rimay to the per-slot missing/filled signal.

    A slot is ``"missing"`` iff its ``<MISSING_*>`` placeholder appears
    in the (unstripped) Rimay, else ``"filled"``. This is the Track 1
    signal handed to the scorer via the manifest.
    """
    return {
        slot: ("missing" if placeholder in rimay else "filled")
        for slot, placeholder in config.SLOT_PLACEHOLDERS.items()
    }


def _strip_to_rimay(text: str) -> str:
    """Strip the few wrappers a model occasionally emits despite instructions.

    Keep this conservative — we want to faithfully capture model output for
    the analysis, only removing pure formatting noise (markdown fences,
    leading/trailing blank lines).
    """
    s = text.strip()
    if s.startswith("```"):
        first_newline = s.find("\n")
        if first_newline != -1:
            s = s[first_newline + 1 :]
        if s.endswith("```"):
            s = s[: -3]
    return s.strip()


@mlflow.trace(name="llm.convert", span_type="LLM")
def convert(
    nl_text: str,
    *,
    run_cfg: config.RunConfig,
) -> LLMResponse:
    prompt = build_prompt(nl_text, run_cfg=run_cfg)
    client = _get_client()

    start = time.monotonic()
    msg = client.messages.create(
        model=run_cfg.model,
        max_tokens=run_cfg.max_tokens,
        temperature=run_cfg.temperature,
        system=prompt.system,
        messages=[{"role": "user", "content": prompt.user}],
    )
    latency_ms = int((time.monotonic() - start) * 1000)

    content_text = ""
    for block in msg.content:
        if getattr(block, "type", None) == "text":
            content_text += block.text

    rimay = _strip_to_rimay(content_text)

    usage = getattr(msg, "usage", None)
    return LLMResponse(
        rimay=rimay,
        prompt=prompt,
        model=msg.model,
        input_tokens=getattr(usage, "input_tokens", None) if usage else None,
        output_tokens=getattr(usage, "output_tokens", None) if usage else None,
        latency_ms=latency_ms,
        stop_reason=getattr(msg, "stop_reason", None),
    )


def main() -> None:
    from src.gold_loader import load_gold

    gold = load_gold()
    if not gold:
        raise SystemExit("Gold CSV is empty.")
    req = next(iter(gold.values()))
    cfg = config.RunConfig(strategy="zsl")
    response = convert(req.nl_text, run_cfg=cfg)
    print(f"req_id: {req.req_id}")
    print(f"latency_ms: {response.latency_ms}")
    print(f"input_tokens={response.input_tokens} output_tokens={response.output_tokens}")
    print("--- Rimay ---")
    print(response.rimay)
    print("--- llm_slots ---")
    print(derive_llm_slots(response.rimay))


if __name__ == "__main__":
    main()
