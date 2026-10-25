"""NL -> Rimay conversion via the configured LLM backend (see llm_backend).

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
from typing import List, Optional

import mlflow
from src import llm_backend

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
    units: Optional[List[str]] = None  # the units task only: one Rimay per unit
    format: Optional[dict] = None  # how well the response kept to the answer block (see parse_answer)


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


_LIST_MARK_RE = re.compile(r"^\s*(?:\d+[.):]|[-*•])\s+")


def extract_units(text: str) -> List[str]:
    """Every Rimay line of a units-task response.

    After the last "Final Rimay:"-style marker when there is one (CoT), else
    every non-empty line. List numbering, bullets and bold are removed; the
    count is not forced — a wrong count is something the scorer reports.
    """
    s = text.strip()
    low = s.lower()
    marker_end = max((low.rfind(m) + len(m) for m in _FINAL_MARKERS if low.rfind(m) != -1), default=-1)
    if marker_end != -1:
        s = s[marker_end:]
    out = []
    for ln in s.splitlines():
        ln = _LIST_MARK_RE.sub("", ln.strip().strip("*").strip()).strip().strip("*").strip()
        if ln and not ln.startswith("#"):
            out.append(ln)
    return out


_BLOCK_RE = re.compile(r"<rimay>(.*?)(?:</rimay>|\Z)", re.S | re.I)
# A Rimay requirement always has a modal verb (or its placeholder); commentary,
# headings and "Wait, correcting…" lines almost never do.
_RIMAY_LINE_RE = re.compile(r"<MISSING_MODAL_VERB>|\b(must|shall|should|will|may|can|could|would)\b", re.I)


def parse_answer(text: str) -> tuple[List[str], dict]:
    """The Rimay lines of a response, and a record of what was not clean.

    Reads the last ``<rimay>`` block (an unclosed one runs to the end). Without a
    block, falls back to the older readers: the text after a "Final Rimay:"
    marker, else the whole response. Each non-empty line, stripped of list marks
    and bold, is kept when it looks like Rimay (has a modal verb or its
    placeholder); the others are returned in ``format["dropped"]``.

    ``format``: ``block`` (an answer block was found), ``closed`` (it was
    closed), ``outside`` (characters of text after the block — before it is the
    CoT reasoning and allowed), ``dropped`` (non-Rimay lines removed),
    ``before`` (characters before the block, for zsl/fsl a format breach).
    """
    s = text.strip()
    blocks = list(_BLOCK_RE.finditer(s))
    fmt = {"block": bool(blocks), "closed": False, "before": 0, "outside": 0, "dropped": []}
    if blocks:
        m = blocks[-1]
        body = m.group(1)
        fmt["closed"] = s[m.start():m.end()].lower().endswith("</rimay>")
        fmt["before"] = len(_BLOCK_RE.sub("", s[:m.start()]).strip())
        fmt["outside"] = len(s[m.end():].strip())
    else:
        low = s.lower()
        marker_end = max((low.rfind(mk) + len(mk) for mk in _FINAL_MARKERS if low.rfind(mk) != -1), default=-1)
        body = s[marker_end:] if marker_end != -1 else s
    lines = []
    for ln in body.splitlines():
        ln = _LIST_MARK_RE.sub("", ln.strip().strip("*").strip()).strip().strip("*").strip().strip("`").strip()
        if not ln:
            continue
        (lines if _RIMAY_LINE_RE.search(ln) and not ln.startswith("#") else fmt["dropped"]).append(ln)
    return lines, fmt


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
    n_units: Optional[int] = None,
    blind: bool = False,
) -> LLMResponse:
    """``n_units`` set (or ``blind``) → the units task; the response's ``units`` holds each line."""
    prompt = build_prompt(nl_text, run_cfg=run_cfg, n_units=n_units, blind=blind)

    start = time.monotonic()
    # cache_system: the system prompt is identical across requirements in a
    # batch, so the api backend caches it. Ignored by the subscription backend.
    completion = llm_backend.complete(
        system=prompt.system,
        prompt=prompt.user,
        model=run_cfg.model,
        max_tokens=run_cfg.max_tokens,
        temperature=run_cfg.temperature,
        backend=run_cfg.backend,
        cache_system=True,
    )
    latency_ms = int((time.monotonic() - start) * 1000)

    raw = _strip_to_rimay(completion.text)
    lines, fmt = parse_answer(raw)
    fmt["lines"] = len(lines)
    if n_units is not None or blind:
        units = lines
        rimay = "\n".join(units)
    else:
        units = None
        # One requirement expected: the first Rimay line of the block; a second
        # line is recorded as a format issue (fmt["lines"] > 1).
        rimay = lines[0] if lines else extract_final_rimay(raw)

    return LLMResponse(
        rimay=rimay,
        raw=raw,
        prompt=prompt,
        model=completion.model,
        input_tokens=completion.input_tokens,
        output_tokens=completion.output_tokens,
        latency_ms=latency_ms,
        stop_reason=completion.stop_reason,
        units=units,
        format=fmt,
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
