# Scoring metrics — strategy `cot`

Gold reqs: 10 · converted: 10 · evaluated: 10 · skipped: 0

## Track 1 — field accuracy (missing-detection)

Binary collapse: gold `present`+`implied` → not-missing; `missing` kept.
P/R/F1 are for the **missing** class (positive = missing).

### Per-slot

| Slot | TP | FP | FN | TN | Precision | Recall | F1 | gold-missing support |
|------|----|----|----|----|-----------|--------|----|----------------------|
| scope | 0 | 0 | 0 | 10 | 0.000 | 0.000 | 0.000 | 0 |
| condition | 0 | 0 | 6 | 4 | 0.000 | 0.000 | 0.000 | 6 |
| actor | 0 | 1 | 0 | 9 | 0.000 | 0.000 | 0.000 | 0 |
| modalVerb | 0 | 1 | 0 | 9 | 0.000 | 0.000 | 0.000 | 0 |
| action | 0 | 0 | 0 | 10 | 0.000 | 0.000 | 0.000 | 0 |

**Micro**: P=0.000 R=0.000 F1=0.000  ·  **Macro**: P=0.000 R=0.000 F1=0.000

### Secondary lenses

- Gold **implied** slots (n=27): LLM filled 25 (fill-rate 0.926), over-flagged missing 2.
- Gold **missing** slots (n=6): LLM correctly flagged 0 (flag-rate 0.000), silently filled 6 (possible compensation).

### Overall-incomplete verdict

LLM overall-incomplete = any mandatory slot (actor, modalVerb, action) flagged missing.

- Agreement rate: **0.900** (n=10)
- P/R/F1 (incomplete class): 0.000 / 0.000 / 0.000

| | gold incomplete | gold complete |
|---|---|---|
| **LLM incomplete** | 0 | 1 |
| **LLM complete** | 0 | 9 |

## Track 2 — conversion quality

Similarity metric v0 (placeholder): SequenceMatcher ratio + token Jaccard. LLM-vs-gold evaluated on 10 reqs (skipped 0 with empty canonical).

### Similarity distributions (side by side)

| Distribution | n | mean | median | min | max | std |
|--------------|---|------|--------|-----|-----|-----|
| LLM-vs-gold · seq_ratio | 10 | 0.485 | 0.448 | 0.233 | 0.724 | 0.152 |
| LLM-vs-gold · jaccard | 10 | 0.271 | 0.265 | 0.111 | 0.500 | 0.115 |
| human-human · seq_ratio | 60 | 0.598 | 0.560 | 0.185 | 0.952 | 0.188 |
| human-human · jaccard | 60 | 0.468 | 0.458 | 0.194 | 0.882 | 0.183 |

> The LLM-vs-gold number is only interpretable against the human-human ceiling (how much annotators vary among themselves).

### Paska validation (independent structural check)

- Pass rate: **0.700** (7/10 scored; failed 3, errors 0)
- Requirements with ≥1 smell: 3

| Smell type | Count |
|------------|-------|
| Not precise verb | 1 |
| Non-atomic requirement | 1 |
| Not requirement | 1 |
