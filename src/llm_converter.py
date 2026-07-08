"""NL -> Rimay conversion via the Anthropic SDK.

Also exposes :func:`strip_missing_placeholders`, which removes the
``<MISSING_*>`` placeholders and the ``<NON_ATOMIC>`` flag so the raw
LLM Rimay can be fed to Paska without the literal tokens tripping the
tokeniser.
"""
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
    raw: str
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


_FINAL_MARKERS = (
    "final rimay:",
    "final answer:",
    "rimay output:",
    "final:",
)


def extract_final_rimay(text: str) -> str:
    """Extract the single-line final Rimay from a model response.

    CoT models sometimes leak their scratchpad despite the instruction to
    emit only the final sentence. Rimay is always one sentence on one
    line, so we recover it defensively:

    * If a final-answer marker ("Final Rimay:", etc.) is present, take the
      first non-empty line after the *last* such marker.
    * Otherwise take the last non-empty line (a clean single-line ZSL/FSL
      output is returned unchanged — this pass is idempotent on them).

    Surrounding markdown bold (``**...**``) and blank lines are stripped.
    The untouched full response is preserved separately as ``raw``.
    """
    s = text.strip()
    low = s.lower()
    marker_end = -1
    for m in _FINAL_MARKERS:
        j = low.rfind(m)
        if j != -1:
            marker_end = max(marker_end, j + len(m))

    if marker_end != -1:
        tail = s[marker_end:]
        lines = [ln.strip().strip("*").strip() for ln in tail.splitlines()]
        lines = [ln for ln in lines if ln]
        if lines:
            return lines[0]

    lines = [ln.strip().strip("*").strip() for ln in s.splitlines()]
    lines = [ln for ln in lines if ln]
    return lines[-1] if lines else s


def _strip_to_rimay(text: str) -> str:
    """Strip the few wrappers a model occasionally emits despite instructions.

    Conservative on purpose — we want to faithfully capture model output
    for the analysis, only removing pure formatting noise (markdown
    fences, leading/trailing blank lines).
    """
    s = text.strip()
    if s.startswith("```"):
        first_newline = s.find("\n")
        if first_newline != -1:
            s = s[first_newline + 1 :]
        if s.endswith("```"):
            s = s[:-3]
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

    raw = _strip_to_rimay(content_text)
    rimay = extract_final_rimay(raw)

    usage = getattr(msg, "usage", None)
    return LLMResponse(
        rimay=rimay,
        raw=raw,
        prompt=prompt,
        model=msg.model,
        input_tokens=getattr(usage, "input_tokens", None) if usage else None,
        output_tokens=getattr(usage, "output_tokens", None) if usage else None,
        latency_ms=latency_ms,
        stop_reason=getattr(msg, "stop_reason", None),
    )


def main() -> None:
    from src.gold_loader import load_conversion_inputs

    items = load_conversion_inputs()
    if not items:
        raise SystemExit("No requirements found in the gold CSV.")
    req_id, text = items[0]
    cfg = config.RunConfig(strategy="zsl")
    response = convert(text, run_cfg=cfg)
    print(f"req_id: {req_id}")
    print(f"latency_ms: {response.latency_ms}")
    print(f"input_tokens={response.input_tokens} output_tokens={response.output_tokens}")
    print("--- Rimay ---")
    print(response.rimay)
    print("--- stripped ---")
    print(strip_missing_placeholders(response.rimay))


if __name__ == "__main__":
    main()
