# Cross-strategy verdict — exp3_blind_haiku

Model: `claude-opus-5-5` · generated 2026-10-25T20:14:00+00:00 · runs: cot, fsl, zsl

## Summary
Few-shot (FSL) is clearly best on slot flagging and conversion quality. Zero-shot (ZSL) is best at deciding atomicity and unit counts. Chain-of-thought (CoT) leads on nothing except format compliance. The FSL lead on Track 1 and Track 2 is large enough to survive a 50-requirement, single-run sample. The atomicity ranking rests on a handful of requirements and needs replication.

## Missing-slot detection
Track 1 is measurable here because scope (76) and condition (35) carry real gold-missing support. FSL flags 104 of 117 missing slots. ZSL flags 27 and CoT flags 21, so ZSL and CoT silently invent scope and condition in roughly 80% of cases. That fabrication is the risk this experiment probes, and the examples in the FSL prompt appear to teach the model to leave optional slots empty.

ZSL and CoT also fail in the opposite direction. They wrongly flag implied actor and modal verb as missing in 9–14 units each, while FSL fills implied slots at 0.98.

Actor, modal verb and action have support of 1–4, so their per-slot F1 values are noise. The overall incomplete verdict rests on only 4 gold-incomplete units. FSL misses 3 of them, and ZSL catches all 4 at the cost of 13 false alarms.

## Conversion quality
FSL reaches the human-human ceiling on cosine (0.83 vs 0.82) and sits close to it on sequence ratio and Jaccard. ZSL and CoT sit well below the ceiling on every measure (cosine 0.69 and 0.65). FSL also has the best Paska pass rate.

CoT is weakest on Paska, with 6 errors, 9 "Not requirement" and 9 "Passive voice" smells. Its units that it judged incomplete pass only 1 time in 10, which suggests those outputs are malformed rather than deliberately partial. It is unclear how the 21 extra FSL units are aligned to gold, so the FSL similarity figures may be inflated or deflated by that alignment.

## Units and atomicity
- **ZSL:** matches the gold unit count on 42 of 50 requirements, with non-atomic detection F1 of 0.93.
- **FSL:** never misses a non-atomic requirement, but splits 9 atomic ones and returns 98 units against 77 expected.
- **CoT:** errs both ways, merging 4 non-atomic requirements and splitting 3 atomic ones.

Several disagreements are shared across strategies:
- All three over-split 328-Signal, 802-Signal and 6111-Signal.
- All three split 2055-Signal.
- All three under-split 222-Signal.
- 6424-Signal shows the widest spread: CoT returns 1 unit, while FSL and ZSL return 4 each.

## Anomalies and inconsistencies
- **Text outside the answer block:** ZSL on 37 requirements, FSL on 15, CoT on none. This is a format breach only, provided extraction succeeded.
- **FSL atomic splits:** 5529-Signal, 107-Mastodon, 801-Signal, 316-Signal, 8-Mastodon, 175-Signal, 42-Mastodon, 7345-Signal, 2055-Signal. This looks like a systematic bias from the examples, not random error.
- **CoT merges:** 222-Signal, 4821-Signal, 6390-Signal, 6424-Signal.
- **Consensus deviations:** 2055-Signal, 328-Signal, 802-Signal, 6111-Signal and 222-Signal deviate from gold under all three strategies. The gold granularity on these may be the problem rather than the model.

## Next step
- Re-adjudicate the gold unit splits for 2055-Signal, 328-Signal, 802-Signal, 6111-Signal and 222-Signal.
- Add atomic counter-examples to the FSL prompt to curb over-splitting.
- Verify how surplus units are aligned in the similarity scoring.
- Repeat each strategy 3 or more times to separate the atomicity differences from run-to-run variance.
