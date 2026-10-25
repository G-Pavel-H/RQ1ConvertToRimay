# Cross-strategy verdict — exp1_atomic_haiku

Model: `claude-opus-5-5` · generated 2026-10-25T20:03:09+00:00 · runs: cot, fsl, zsl

## Summary

Few-shot is the strongest strategy on conversion quality and on flagging the optional slots, scope and condition. It reaches the human-human ceiling, while zero-shot and chain-of-thought are indistinguishable from each other and well below it. Few-shot has one serious blind spot: it never judges a requirement incomplete. Chain-of-thought is the best at that requirement-level judgement, but only 4 requirements are gold-incomplete, so that ranking is fragile.

## Missing-slot detection

Track 1 is measurable here for scope (34 gold-missing) and condition (21). Actor, modalVerb and action have 1, 1 and 4 gold-missing slots, which is too few to rank strategies.

On scope and condition the gap is large and not noise:
- Few-shot flags 25 of 34 missing scopes and 17 of 21 missing conditions.
- Zero-shot and chain-of-thought flag 5 of 34 scopes, and 3 and 5 of 21 conditions. They fill the empty optional slots with invented content, which is the fabrication risk this experiment probes.

Micro-F1 mostly measures this scope behaviour, so it overstates few-shot's advantage.

The picture reverses on the core slots:
- Few-shot silently fills all 4 missing actions and the single missing actor and modal. Its requirement-level verdict is "complete" for all 35 requirements, giving incomplete-F1 = 0.
- Chain-of-thought catches 3 of the 4 gold-incomplete requirements. It over-flags, though: it marks 10 implied slots as missing (4 actors, 5 modals) and has 3 false incompletes.
- Zero-shot sits between the two, catching 2 of 4 with 1 false alarm.

The likely cause is the few-shot examples. They seem to model empty scope and condition but never an absent core slot.

## Conversion quality

Few-shot matches the human ceiling:
- Sequence ratio 0.60 against a human-human 0.64.
- Jaccard 0.45 against 0.49.
- Cosine 0.77, which exceeds the human-human 0.74.

Zero-shot and chain-of-thought trail the ceiling by about 0.15 on sequence ratio and 0.17 on Jaccard. With roughly 0.035 standard error per requirement mean, this gap is real.

Paska pass rates (zero-shot 0.80, others 0.71) differ by 3 requirements and are within noise. All 6 of chain-of-thought's incomplete outputs fail as "Not requirement", so Paska penalises honest flagging. Among the outputs chain-of-thought judged complete, it has the highest pass rate (0.86). Few-shot's failures are mostly smells in the converted text itself (imprecise verb ×5, incorrect order ×3).

## Anomalies and inconsistencies

- **Text outside the answer block:** zero-shot ×6 (316, 42-Mastodon, 5775, 6457, 6459, 7345) and few-shot ×3 (316, 5775, 7345). The overlap of 316, 5775 and 7345 suggests these sources provoke commentary. All 35 were still evaluated, so the headline metrics are not distorted.
- **Atomic requirements flagged `<NON_ATOMIC>`:** chain-of-thought on 316-Signal and 937-Signal; zero-shot on 6077-Signal. These likely feed the "Non-atomic"/"Not requirement" Paska smells.
- **316-Signal is anomalous under every strategy.** Check whether it is genuinely atomic.

## Next step

- Add one few-shot example with a missing actor or action and an explicit incomplete verdict, then rerun few-shot.
- Re-adjudicate 316-Signal, and review 5775 and 7345 for ambiguity.
- Report Track 1 with scope separated from the core slots, so micro-F1 is not read as fabrication-avoidance on actions.
