# Cross-strategy verdict — exp1_atomic_haiku

Model: `claude-opus-5-5` · generated 2026-10-18T23:42:17+01:00 · runs: cot, fsl, zsl

Few-shot (fsl) is the strongest strategy on almost every metric. The advantage is clear over zero-shot (zsl) but only modest over chain-of-thought (cot). And no strategy solves the problem this experiment is designed to probe: all three still silently fill most genuinely missing slots.

Track 1 is only partly measurable here. Gold-missing support is reasonable for scope (34) and condition (21) but negligible for actor (1), modalVerb (1) and action (4). The perfect and zero per-slot scores on those three slots are single-item coin flips, and the macro averages, which weight them equally, should be ignored. On the two measurable slots, fsl flags 9/34 missing scopes and 10/21 missing conditions. The other strategies flag far fewer: cot 5 and 4, zsl 5 and 2. The condition gap between fsl and zsl (10 vs 2 of 21) is the only Track 1 difference likely to survive a paired test. The scope differences are within noise.

The headline is the same for every strategy: 42 to 52 of 61 missing slots get invented content. Nearly every requirement (34/35) lacks a scope, so it is worth confirming whether the gold treats scope and condition as optional Rimay elements before calling these fills fabrication. Looking at the content the model inserted would settle this quickly.

Each strategy has a distinct failure mode. Fsl never declares a requirement incomplete: it fills every implied slot, never flags actor, modalVerb or action, and misses all 4 gold-incomplete requirements at the verdict level. The likely cause is that the few-shot examples only show complete outputs, which teaches the model that completeness is always expected. Cot errs the other way. It flags 10 implied actor or modalVerb slots as missing, and all 7 requirements it judges incomplete fail Paska, mostly as "Not requirement". When cot abstains, it seems to emit something that is no longer a requirement at all. Zsl's problem is format. It is furthest from the human ceiling on every similarity measure (cosine 0.49 against a human-human 0.74), it has the widest spread (std 0.35), it passes only 18/35 on Paska, and it carries 10 "Not requirement" smells. Many zsl outputs look off-template rather than slightly wrong.

On Track 2, fsl's cosine similarity (0.744) matches the human-human ceiling (0.737). Its lexical scores remain below the ceiling (sequence ratio 0.57 vs 0.64, Jaccard 0.40 vs 0.49). So fsl gets the meaning right but not the exact Rimay wording. Cot sits about 0.10 lower in cosine, which is probably real but needs a paired test, because the 105 pairs are three humans times 35 items, not 105 independent observations. The Paska gap between fsl and cot (27 vs 23 of 35) is noise. The gap between fsl and zsl (27 vs 18) is suggestive but not conclusive at this sample size. The 0.94 verdict agreement for zsl rests on only 4 gold-incomplete items and means little.

Several caveats apply. This is the atomic-only subset with one model and a single run per strategy. The results file also contains no per-requirement IDs, so I cannot point to specific failing reqIds.

The next steps, in order:
1. Export per-item outputs and run McNemar tests on the flag and Paska outcomes and Wilcoxon tests on similarity.
2. Read the invented scope and condition text to decide whether it is fabrication or a convention mismatch.
3. Add an incomplete-input example to the fsl prompt.
4. Require cot to emit a valid Rimay statement with an explicit missing-slot marker instead of free text.
5. Repeat each strategy three to five times to estimate run-to-run variance before comparing models or adding non-atomic requirements.
