"""Generate Penn-Treebank-format constituency trees for Paska's Java step.

Replaces ``paska/source/get-parsing-trees/get_cparsingtrees.py``, which
required Python 3.8 + allennlp 2.10.1 + a 2020 ELMo model. We use
stanza (Stanford NLP) instead, which works on modern Python (3.10+),
ships PTB-format trees out of the box, and is one ``pip install``.

The Java side (``smell_detector.jar``) reads a CSV produced here with
exactly the same shape Paska's original script produced:

    "req_id";"req";"constituency_tree";"req_tokenized"

so the smell detector is unchanged.
"""
from __future__ import annotations

import csv
import re
from functools import lru_cache
from pathlib import Path
from typing import Iterable, Sequence, Tuple

import stanza


def _replace_quoted(match: re.Match) -> str:
    inner = match.group(1).strip()
    inner = re.sub(r"[“”]", "", inner)
    inner = re.sub(r"\s+", "_", inner.strip())
    return inner


def _preprocess(req: str) -> str:
    """Mirror the cleanups Paska's original Python script applied.

    The Java smell detector was tuned against this preprocessing, so we
    keep the same normalisations (collapse smart quotes, strip
    parentheses, fold "<lb>" line breaks into commas, etc.) before
    parsing.
    """
    req = re.sub(r"\x3A", "", req)
    req = re.sub(r"((“|”).*?(”|“))", _replace_quoted, req)
    req = re.sub(r"[()]", "", req)
    req = re.sub(r"\x27", "", req)
    req = re.sub(r"-", "_", req)
    req = re.sub(r"\x2c{2,4}", ",", req)
    req = re.sub(r";", "", req)
    req = re.sub(r"<lb>(Note|Notes|NOTE).+([a-zA-Z]|[0-9]|\.)$", "", req)
    req = re.sub(r"<lb>(To illustrate).+\.", "", req)
    req = re.sub(r"(<lb><bullet>)(.*)(,|;)", r"\1\2", req)
    req = re.sub(r"<lb><bullet>", r"\\*", req)
    req = re.sub(r"<lb>", ", ", req)
    req = re.sub(r"(\w+)(\x20)(,)", r"\1\3", req)
    req = re.sub(r",\x20*,", ",", req)
    req = re.sub(r"(lock\w+)\s{1,3}(in)", r"\1_\2", req)
    req = re.sub(r"\s{2,}", " ", req)
    return req


@lru_cache(maxsize=1)
def _pipeline() -> stanza.Pipeline:
    return stanza.Pipeline(
        lang="en",
        processors="tokenize,pos,constituency",
        verbose=False,
        download_method=None,
    )


def _format_tree(tree) -> str:
    """Stanza's Tree __str__ already produces PTB-style "(ROOT (S ...))"."""
    return str(tree)


def _parse_one(req_text: str) -> Tuple[str, str]:
    """Return (constituency_tree_str, tokenized_text) for one requirement.

    Multi-sentence requirements are concatenated under a synthetic
    ``(MULTISENT ...)`` root so the smell detector sees a single tree
    per row, matching the original script's one-row-per-requirement
    convention.
    """
    nlp = _pipeline()
    doc = nlp(req_text)
    sentences = list(doc.sentences)
    if not sentences:
        return "(ROOT)", ""
    if len(sentences) == 1:
        tree_str = _format_tree(sentences[0].constituency)
    else:
        joined = " ".join(_format_tree(s.constituency) for s in sentences)
        tree_str = f"(MULTISENT {joined})"
    tokens = []
    for sent in sentences:
        for word in sent.words:
            tokens.append(word.text)
    return tree_str, " ".join(tokens)


def write_parsing_trees_csv(
    items: Sequence[Tuple[str, str]],
    output_csv: Path,
) -> Path:
    """Write Paska's ``parsing-trees.csv`` for the given requirements."""
    output_csv.parent.mkdir(parents=True, exist_ok=True)
    with output_csv.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter=";", quoting=csv.QUOTE_ALL)
        for req_id, raw in items:
            preprocessed = _preprocess(raw)
            tree_str, tokenized = _parse_one(preprocessed)
            writer.writerow([req_id, preprocessed, tree_str, tokenized])
    return output_csv
