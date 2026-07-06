"""Tests for src/scoring/field_accuracy.py (Track 1).

Written together with the scoring implementation after the Stage 1
schema checkpoint (build order step 5). Planned coverage: the
present/implied → not-missing collapse, per-slot P/R/F1 on hand-built
confusions, micro/macro averaging, the implied/compensation secondary
rates, and the mandatory-slot overall verdict + 2x2 confusion.
"""
