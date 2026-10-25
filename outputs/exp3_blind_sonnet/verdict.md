# Cross-strategy verdict — exp3_blind_sonnet

Model: `claude-opus-5-5` · generated 2026-10-25T20:16:06+00:00 · runs: cot, fsl, zsl

## Summary
Few-shot is clearly best on this blind run. It flags 97% of genuinely missing slots, against about 28% for zsl and cot. Its similarity to the humans sits at the human-human ceiling, and Paska passes 88% of its outputs, against 27–41% for the others. The gap is far larger than the noise at 77 units. Zero-shot and chain-of-thought are statistically indistinguishable from each other.

## Missing-slot detection
Zero-shot and chain-of-thought fail in two opposite directions at once:
- **Missing slots get filled.** They fill 61–63 of the 76 gold-missing scopes and about 20 of the 35 missing conditions. That is the fabrication risk this experiment probes.
- **Implied slots get flagged.** They mark implied actor and modalVerb as missing (31–54 false positives per slot). As a result, they call 43 (cot) and 54 (zsl) gold-complete units incomplete.

That combination looks more like a slot-attribution or output-template problem than a reasoning one. A likely cause is placeholders being parsed into the wrong field, so check this before interpreting it as model behaviour.

Few-shot's only weakness is the action slot, where it silently filled 3 of 4 missing actions. Zero-shot caught 3 of 4. A support of 4 is too small to rank strategies on this slot.

## Conversion quality
Few-shot matches the human ceiling on every similarity measure (sequence ratio 0.70 vs 0.72; Jaccard and cosine effectively equal). Zero-shot and chain-of-thought trail the ceiling by about 0.18 on sequence ratio and Jaccard.

Paska shows where the difference sits:
- Units the LLM itself judged complete pass at about 90% under all three strategies.
- Zero-shot and chain-of-thought collapse only on the units they marked incomplete, with "Not requirement" smells at 32 each.

So their low scores come from emitting fragments rather than full Rimay sentences.

## Units and atomicity
All three strategies over-split, returning 99–100 units against 77 expected. Of that excess, 328-Signal alone contributes 9–13 units.

Few-shot fits the gold count best on non-atomic requirements. It gets 802, 222, 6390, 6111, 4821 and 6362 right, and detects non-atomicity with perfect recall. Its cost is the most atomic splits (7). Zero-shot and chain-of-thought produce nearly identical count patterns, including merging 6390-Signal.

Six requirements are mis-counted by every strategy: 6077, 8-Mastodon, 7345, 1065, 328 and 6424. That consistency points to source ambiguity or a gold boundary worth re-checking, not to prompting.

## Anomalies and inconsistencies
- **Format breach, zsl:** text outside the answer block on 1283-Signal and 4245-Signal.
- **Paska errors:** 3 in cot and 2 in zsl; the data does not give their reqIds.
- **Splits unique to fsl:** 107-Mastodon, 175, 316 and 801.
- **Splits unique to other strategies:** 2055 (cot) and 5529 (zsl).
- **Merge, zsl and cot:** both return 6390-Signal as one unit; cot additionally under-counts 6362.
- **Extreme over-split, all strategies:** 328-Signal returned as 14–18 units against 5 in the gold. With no alignment check, its similarity scores are probably misattributed.

## Next step
- Inspect raw zsl and cot outputs for actor/modal placeholders and check how the parser assigns slots, before reporting their Track 1 results.
- Adjudicate the six unanimously mis-counted requirements against the gold unit boundaries.
- Re-run Track 2 with 328-Signal excluded, or with explicit unit alignment, to confirm that few-shot's at-ceiling result is not inflated.
