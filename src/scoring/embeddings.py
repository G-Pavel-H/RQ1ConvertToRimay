"""Sentence embeddings for the semantic similarity metric.

Rimay is boilerplate-heavy — nearly every conversion opens "For all users," and
carries "shall"/"must" — so the lexical metrics in ``conversion_quality`` have a
high floor and a narrow band: two *unrelated* requirements score ~0.35 on
SequenceMatcher purely from shared scaffolding. They also cannot see that two
differently-worded conversions mean the same thing, which is the normal case
when three annotators write the same requirement independently.

Embeddings fix both. This runs **locally through Ollama** (no API key, no
credits, deterministic for a given text and model), defaulting to
``all-minilm`` — all-MiniLM-L6-v2, 384 dimensions, 45 MB.

Degrading gracefully is deliberate: if Ollama is not running the cosine metric
is simply absent from the report and every other metric still computes. Scoring
must never fail because a side-car service is down.

Setup, once:
    brew install ollama && ollama serve &
    ollama pull all-minilm
"""
from __future__ import annotations

import hashlib
import json
import os
import urllib.error
import urllib.request
from pathlib import Path
from typing import Dict, List, Optional

from src import config

MODEL = os.environ.get("RIMAY_EMBED_MODEL", "all-minilm")
ENDPOINT = os.environ.get("RIMAY_EMBED_URL", "http://localhost:11434/api/embeddings")
TIMEOUT_S = 60

# Texts repeat constantly — each annotator conversion is compared against the
# LLM's and against every other annotator's — so embedding is memoised in
# process, and on disk so re-scoring a run costs nothing.
_CACHE_PATH = config.OUTPUTS_DIR / "_embeddings" / f"{MODEL.replace(':', '_')}.json"
_memory: Dict[str, List[float]] = {}
_disk_loaded = False
_disk_dirty = False
_unavailable = False  # set once Ollama has proven unreachable; stops retry storms


def _key(text: str) -> str:
    return hashlib.sha1(text.encode("utf-8")).hexdigest()


def _load_disk_cache() -> None:
    global _disk_loaded
    if _disk_loaded:
        return
    _disk_loaded = True
    try:
        _memory.update(json.loads(_CACHE_PATH.read_text(encoding="utf-8")))
    except (OSError, ValueError):
        pass


def save_cache() -> None:
    """Persist newly computed embeddings. Safe to call when nothing changed."""
    global _disk_dirty
    if not _disk_dirty:
        return
    try:
        _CACHE_PATH.parent.mkdir(parents=True, exist_ok=True)
        _CACHE_PATH.write_text(json.dumps(_memory), encoding="utf-8")
        _disk_dirty = False
    except OSError:
        pass


def available() -> bool:
    """Whether the embedding service answered at least once this session."""
    return not _unavailable


def embed(text: str) -> Optional[List[float]]:
    """Embed one string, or ``None`` when the service is unreachable."""
    global _disk_dirty, _unavailable
    if _unavailable:
        return None
    text = (text or "").strip()
    if not text:
        return None

    _load_disk_cache()
    key = _key(text)
    if key in _memory:
        return _memory[key]

    request = urllib.request.Request(
        ENDPOINT,
        data=json.dumps({"model": MODEL, "prompt": text}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT_S) as response:
            vector = json.load(response).get("embedding")
    except (urllib.error.URLError, OSError, ValueError, TimeoutError):
        # One failure is taken as "the service is not there" for this run: the
        # alternative is a multi-second timeout on every one of ~200 pairs.
        _unavailable = True
        return None

    if not vector:
        _unavailable = True
        return None

    _memory[key] = vector
    _disk_dirty = True
    return vector


def cosine(a: List[float], b: List[float]) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    na = sum(x * x for x in a) ** 0.5
    nb = sum(y * y for y in b) ** 0.5
    if not na or not nb:
        return 0.0
    # Clamp: floating-point drift can push an identical pair a hair over 1.0.
    return max(-1.0, min(1.0, dot / (na * nb)))
