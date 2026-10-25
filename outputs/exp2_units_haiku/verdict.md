# Cross-strategy verdict — exp2_units_haiku

Model: `claude-opus-5-5` · generated 2026-10-25T20:08:40+00:00 · runs: cot, fsl, zsl

## Summary
Few-shot is best on every measurable axis: it flags 43 of 56 genuinely missing slots against 8 for zero-shot and 0 for chain-of-thought, and it comes closest to the human ceiling on similarity. On Track 1 the gap is far too large to be noise, even with 42 units clustered in 15 requirements. On Paska pass rate (33 vs 25 of 42 against CoT), the gap is suggestive but not conclusive.

## Missing-slot detection
Only scope (42 gold-missing) and condition (14) have support. Actor, modalVerb and action scores of 0.0 are artifacts of zero support. The overall-verdict F1 of 0.0 is also an artifact, because all 42 gold units are complete.

- **CoT** fills every missing slot, including all 14 missing conditions. These are invented triggers the source never stated, which is the fabrication risk this experiment probes. A flag count of exactly zero with no false positives may also mean the CoT prompt lacks the missing-slot marker convention, so check that before attributing it to the model.
- **ZSL** misses most missing slots, fabricating 12 of the 14 conditions. It also mislabels the wrong slots: it flags 8 implied actors and 4 implied modal verbs as missing. That makes 12 complete units look incomplete, which lowers its agreement rate on the overall verdict.
- **FSL** is the only strategy that follows the convention. It produces 2 false-positive conditions and fabricates 4 conditions.

## Conversion quality
FSL reaches about 97% of the human-human cosine ceiling and about 90% of the Jaccard ceiling. CoT and ZSL are indistinguishable from each other at about 82% of the cosine ceiling and about 60% of the Jaccard ceiling.

FSL's lexical gain may partly reflect example phrasing that matches the annotators' house style. Its similarity maximum is 1.0, meaning at least one output exactly matches a human conversion.

Paska smells differ by strategy:
- **CoT**'s smells are dominated by "Non-atomic requirement" (8), so it produces units that are still compound despite being told the unit count.
- **ZSL**'s "Incomplete requirement" and "Incomplete system response" smells (4 each) track its spurious actor and modal-verb flags.

## Units and atomicity
All three strategies return exactly the right number of units for all 15 requirements, and there are no count disagreements. Non-atomic detection is trivially perfect because the prompt states the unit count. Units are returned out of gold order for 4 requirements (CoT, FSL) and 5 (ZSL). If unit scoring is positional, this depresses similarity for those requirements.

## Anomalies and inconsistencies
- **FSL, text outside the answer block** (10 of 15 requirements): 1692, 226, 328, 4821, 5751, 6111, 6362, 6390, 6424, 802. The longest are 802 (1721 characters) and 6390 (1135 characters).
- **ZSL, text outside the answer block** (8 requirements): 1065, 222, 328, 4821, 6111, 6390, 6424, 7567.
- **Shared by both:** 328, 4821, 6111, 6390 and 6424 breach under FSL and ZSL. This points to requirement-driven verbosity, likely from the more complex requirements, rather than a strategy effect.
- **CoT** has no format breaches.
- **Effect on metrics:** the breaches affect headline metrics only if the parser drops content or the outside text carries the model's missing-slot reasoning.

## Next step
- Manually inspect CoT outputs for the 14 condition-missing units to confirm the conditions are fabricated rather than an output-convention mismatch.
- Verify that unit matching is order-invariant, and rescore the 4–5 reordered requirements if it is not.
- Read the text outside the answer block for 328, 4821, 6111, 6390 and 6424 to see whether it holds caveats that should have been flags.
