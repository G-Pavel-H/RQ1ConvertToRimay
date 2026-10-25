# Cross-strategy verdict — exp1_atomic_opus

Model: `claude-opus-5-5` · generated 2026-10-25T20:06:44+00:00 · runs: cot, fsl, zsl

## Summary
Few-shot (fsl) gives the best conversions by a clear margin: it reaches the human-human ceiling and passes Paska on 31 of 35. It is also the only strategy that misses every gold-incomplete requirement, so it fills gaps silently rather than flagging them. The quality gap is larger than the noise; the completeness gap depends on four requirements and is not conclusive.

## Missing-slot detection
Scope and condition make up 55 of the 61 gold-missing slots, so they drive micro-F1. Actor, modalVerb and action have support of 1, 1 and 4, which makes per-slot and macro F1 on those three uninterpretable.

- **fsl** marks scope and condition missing almost perfectly. It never flags actor, modal or action, and it fills all four gold-missing actions. This is the central risk the experiment probes: on exactly the four requirements that lack an action, fsl writes one. Its 100% fill rate on implied slots fits a model that never says "missing" outside optional slots, possibly because the few-shot examples show only complete core slots.
- **cot** errs the other way. It flags the implied modal in 32 of 34 cases and the implied actor in 15 of 29, and calls 33 of 35 requirements incomplete. It still writes a scope in 11 cases.
- **zsl** writes a scope in all 34 gold-missing cases and a condition in 20 of 21. At the same time it flags implied actors and modals about half the time. A strategy that never marks an optional slot missing may be following a different absence convention, not fabricating content.

Both cot and zsl catch all four gold-incomplete requirements, but only by over-flagging (precision 0.12 and 0.19).

## Conversion quality
fsl matches the human ceiling on seq_ratio (0.640 vs 0.644) and Jaccard, and exceeds it on cosine. cot and zsl sit about 0.10 seq_ratio below the ceiling, and they are indistinguishable from each other. The per-requirement standard error is roughly 0.035, so fsl's lead is real.

Paska results follow the same over-flagging pattern. cot's 0% pass rate is mostly "Not requirement" and "Incomplete" smells, which is what Paska reports when outputs contain missing-markers rather than modal verbs. zsl passes 71% of the outputs it calls complete, close to fsl.

## Anomalies and inconsistencies
- **Same requirement flagged by two strategies:** cot and zsl both flag 2055-Signal as `<NON_ATOMIC>`. Check its gold atomicity label.
- **Single-strategy flag:** zsl also flags 7345-Signal as `<NON_ATOMIC>`.
- **Missing answer block:** cot, 107-Mastodon, which was parsed with the older readers.
- **Text outside the block:** zsl, 828-Signal (61 characters).

These affect at most 3 of 35 requirements per run and do not change any headline metric.

## Next step
- Read fsl's outputs for the four gold-incomplete requirements. Check whether the few-shot exemplars ever demonstrate a missing core slot; if none do, add one.
- Fix cot's handling of implied modals so it writes "shall" instead of flagging, then re-run. Separately, confirm how zsl marks absent optional slots before scoring its scope and condition misses as fabrication.
- Report paired tests (McNemar on the incomplete verdicts, Wilcoxon on similarity) instead of point estimates, given only four gold-incomplete cases.
