"""Where LLM calls are sent: the metered API, or the local Claude subscription.

Two backends, one interface:

* ``api`` — the Anthropic SDK (``anthropic``) against ``ANTHROPIC_API_KEY``.
  Billed as API credits. Supports ``temperature`` and prompt caching.
* ``subscription`` — the Claude Agent SDK (``claude-agent-sdk``), which drives
  the locally installed ``claude`` CLI and therefore bills the logged-in Claude
  account rather than API credits.

**The subscription backend cannot set ``temperature``.** The Agent SDK exposes
no sampling controls, so a request that relies on ``temperature=0`` for
reproducibility does not get it here. That matters for Stage 1 conversions
(where determinism is part of the experiment) and not for Stage 3 analysis.
``complete()`` warns once when a temperature is requested but cannot be honoured.

Two things are done deliberately on the subscription path so that what runs is
a plain model call rather than a coding agent:

* ``system_prompt`` is passed as a bare string, which *replaces* the Claude Code
  preset instead of extending it;
* tools are disabled and ``setting_sources=[]`` so no CLAUDE.md, project
  settings or built-in tools enter the context, and ``max_turns=1`` forbids an
  agentic loop.

``ANTHROPIC_API_KEY`` is also scrubbed from the subprocess environment: if the
CLI can see a key it will spend API credits, silently defeating the point.
"""
from __future__ import annotations

import asyncio
import os
import warnings
from dataclasses import dataclass
from typing import Optional

BACKEND_API = "api"
BACKEND_SUBSCRIPTION = "subscription"
BACKENDS = (BACKEND_API, BACKEND_SUBSCRIPTION)

#: Env var that picks the default backend for every stage.
BACKEND_ENV = "RIMAY_LLM_BACKEND"

_temperature_warned = False


@dataclass(frozen=True)
class Completion:
    """One model response, normalised across backends."""

    text: str
    model: str
    input_tokens: Optional[int]
    output_tokens: Optional[int]
    stop_reason: Optional[str]
    backend: str

    @property
    def truncated(self) -> bool:
        """True when the response was cut off at the token ceiling."""
        return self.stop_reason == "max_tokens"


def resolve_backend(name: Optional[str] = None) -> str:
    """Explicit argument, else ``RIMAY_LLM_BACKEND``, else ``subscription``."""
    chosen = (name or os.environ.get(BACKEND_ENV) or BACKEND_SUBSCRIPTION).strip().lower()
    if chosen not in BACKENDS:
        raise ValueError(f"unknown backend {chosen!r}; expected one of {', '.join(BACKENDS)}")
    return chosen


# --- api backend -------------------------------------------------------------


def _complete_api(
    *, system: str, prompt: str, model: str, max_tokens: int,
    temperature: Optional[float], cache_system: bool,
) -> Completion:
    from anthropic import Anthropic

    if not os.environ.get("ANTHROPIC_API_KEY"):
        raise RuntimeError(
            "ANTHROPIC_API_KEY is not set, so the 'api' backend cannot run. "
            f"Add it to .env, or use the subscription backend ({BACKEND_ENV}=subscription)."
        )

    system_block = {"type": "text", "text": system}
    if cache_system:
        # Prefix match, 5-min TTL: sequential requirements in a batch reuse it.
        system_block["cache_control"] = {"type": "ephemeral"}

    kwargs = {}
    if temperature is not None:
        kwargs["temperature"] = temperature

    msg = Anthropic().messages.create(
        model=model,
        max_tokens=max_tokens,
        system=[system_block],
        messages=[{"role": "user", "content": prompt}],
        **kwargs,
    )
    text = "".join(b.text for b in msg.content if getattr(b, "type", None) == "text")
    usage = getattr(msg, "usage", None)
    return Completion(
        text=text,
        model=msg.model,
        input_tokens=getattr(usage, "input_tokens", None) if usage else None,
        output_tokens=getattr(usage, "output_tokens", None) if usage else None,
        stop_reason=getattr(msg, "stop_reason", None),
        backend=BACKEND_API,
    )


# --- subscription backend ----------------------------------------------------


async def _run_subscription(*, system: str, prompt: str, model: str, max_tokens: int) -> Completion:
    from claude_agent_sdk import (
        AssistantMessage,
        ClaudeAgentOptions,
        ResultMessage,
        TextBlock,
        query,
    )

    options = ClaudeAgentOptions(
        # A bare string REPLACES the claude_code preset — without this the
        # coding-agent system prompt would sit in front of the experiment's own.
        system_prompt=system,
        model=model,
        allowed_tools=[],
        setting_sources=[],
        max_turns=1,
        # Blank rather than absent: the CLI must not fall back to a key it can
        # see in the parent environment (that would bill API credits).
        env={"ANTHROPIC_API_KEY": "", "MAX_THINKING_TOKENS": "0"},
    )

    chunks: list[str] = []
    result: Optional[object] = None
    async for message in query(prompt=prompt, options=options):
        if isinstance(message, AssistantMessage):
            chunks += [b.text for b in message.content if isinstance(b, TextBlock)]
        elif isinstance(message, ResultMessage):
            result = message

    if result is not None and getattr(result, "is_error", False):
        raise RuntimeError(
            f"claude CLI returned an error: {getattr(result, 'result', None) or result}"
        )

    usage = getattr(result, "usage", None) or {}
    if not isinstance(usage, dict):
        usage = {}
    return Completion(
        text="".join(chunks),
        model=model,
        input_tokens=usage.get("input_tokens"),
        output_tokens=usage.get("output_tokens"),
        stop_reason=getattr(result, "stop_reason", None),
        backend=BACKEND_SUBSCRIPTION,
    )


def _complete_subscription(
    *, system: str, prompt: str, model: str, max_tokens: int, temperature: Optional[float],
) -> Completion:
    global _temperature_warned
    if temperature is not None and not _temperature_warned:
        _temperature_warned = True
        warnings.warn(
            f"temperature={temperature} cannot be set on the '{BACKEND_SUBSCRIPTION}' "
            "backend — the Claude Agent SDK exposes no sampling controls. Results "
            "will not be deterministic. Use the 'api' backend where reproducibility "
            "matters.",
            RuntimeWarning,
            stacklevel=3,
        )
    # The CLI reads the ambient environment; a visible key means API billing.
    previous = os.environ.pop("ANTHROPIC_API_KEY", None)
    try:
        return asyncio.run(
            _run_subscription(system=system, prompt=prompt, model=model, max_tokens=max_tokens)
        )
    finally:
        if previous is not None:
            os.environ["ANTHROPIC_API_KEY"] = previous


# --- public entry point ------------------------------------------------------


def complete(
    *,
    system: str,
    prompt: str,
    model: str,
    max_tokens: int,
    temperature: Optional[float] = None,
    backend: Optional[str] = None,
    cache_system: bool = False,
) -> Completion:
    """Send one prompt and return the response, via the selected backend."""
    chosen = resolve_backend(backend)
    if chosen == BACKEND_API:
        return _complete_api(
            system=system, prompt=prompt, model=model, max_tokens=max_tokens,
            temperature=temperature, cache_system=cache_system,
        )
    return _complete_subscription(
        system=system, prompt=prompt, model=model, max_tokens=max_tokens,
        temperature=temperature,
    )
