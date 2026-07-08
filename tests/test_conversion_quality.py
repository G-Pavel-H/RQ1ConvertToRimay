"""Tests for src/scoring/conversion_quality.py (Track 2).

Written together with the scoring implementation after the Stage 1
schema checkpoint (build order step 5). Planned coverage: text
normalization (case, whitespace, placeholder stripping), the
SequenceMatcher and token-Jaccard similarity behaviours on known
pairs, pairwise human-human aggregation, and the Paska pass-rate /
smell-frequency summaries from manifest fixtures.
"""
