# Scoring metrics — strategy `fsl`

Gold reqs: 10 · converted: 10 · evaluated: 10 · skipped: 0

## Track 1 — field accuracy (missing-detection)

Binary collapse: gold `present`+`implied` → not-missing; `missing` kept.
P/R/F1 are for the **missing** class (positive = missing).

### Per-slot

| Slot | TP | FP | FN | TN | Precision | Recall | F1 | gold-missing support |
|------|----|----|----|----|-----------|--------|----|----------------------|
| scope | 0 | 0 | 0 | 10 | 0.000 | 0.000 | 0.000 | 0 |
| condition | 0 | 0 | 6 | 4 | 0.000 | 0.000 | 0.000 | 6 |
| actor | 0 | 0 | 0 | 10 | 0.000 | 0.000 | 0.000 | 0 |
| modalVerb | 0 | 0 | 0 | 10 | 0.000 | 0.000 | 0.000 | 0 |
| action | 0 | 0 | 0 | 10 | 0.000 | 0.000 | 0.000 | 0 |

**Micro**: P=0.000 R=0.000 F1=0.000  ·  **Macro**: P=0.000 R=0.000 F1=0.000

### Secondary lenses

- Gold **implied** slots (n=27): LLM filled 27 (fill-rate 1.000), over-flagged missing 0.
- Gold **missing** slots (n=6): LLM correctly flagged 0 (flag-rate 0.000), silently filled 6 (possible compensation).

### Overall-incomplete verdict

LLM overall-incomplete = any mandatory slot (actor, modalVerb, action) flagged missing.

- Agreement rate: **1.000** (n=10)
- P/R/F1 (incomplete class): 0.000 / 0.000 / 0.000

| | gold incomplete | gold complete |
|---|---|---|
| **LLM incomplete** | 0 | 0 |
| **LLM complete** | 0 | 10 |

## Track 2 — conversion quality

Similarity metric v0 (placeholder): SequenceMatcher ratio + token Jaccard. LLM-vs-gold evaluated on 10 reqs (skipped 0 with empty canonical).

### Similarity distributions (side by side)

| Distribution | n | mean | median | min | max | std |
|--------------|---|------|--------|-----|-----|-----|
| LLM-vs-gold · seq_ratio | 10 | 0.593 | 0.566 | 0.509 | 0.891 | 0.104 |
| LLM-vs-gold · jaccard | 10 | 0.460 | 0.479 | 0.292 | 0.706 | 0.133 |
| human-human · seq_ratio | 60 | 0.598 | 0.560 | 0.185 | 0.952 | 0.188 |
| human-human · jaccard | 60 | 0.468 | 0.458 | 0.194 | 0.882 | 0.183 |

> The LLM-vs-gold number is only interpretable against the human-human ceiling (how much annotators vary among themselves).

### Paska validation (independent structural check)

- Pass rate: **0.900** (9/10 scored; failed 1, errors 0)
- Requirements with ≥1 smell: 1

| Smell type | Count |
|------------|-------|
| Incomplete condition | 1 |
