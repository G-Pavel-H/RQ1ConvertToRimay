# Cross-strategy verdict — exp2_units_opus

Model: `claude-opus-5-5` · generated 2026-10-25T20:12:14+00:00 · runs: cot, fsl, zsl

## Summary
Few-shot (fsl) is clearly best on every measurable dimension. It flags 54 of 56 gold-missing slots, reaches the human-human ceiling on similarity, and passes Paska on 88% of units. Zero-shot (zsl) and chain-of-thought (cot) trail by margins far larger than noise, even at 42 units from 15 requirements.

## Missing-slot detection
Track 1 is measurable only for scope (42 gold-missing) and condition (14 gold-missing). Actor, modalVerb and action have no gold-missing support, so their 0.0 F1 is an artifact. Any "missing" call on those slots is a false alarm on an implied slot. The overall-verdict F1 of 0.0 is also undefined, because all 42 gold units are complete.

- **fsl** keeps the two behaviours correct: it flags absent scope/condition and fills every implied actor and modal verb.
- **zsl** flags no missing slot at all. It writes a scope into all 42 units and a condition into all 14 that lack one, which is the fabrication risk this experiment probes.
- **cot** fails in both directions:
  - It invents scope in 25 units.
  - It marks implied slots as missing: modal verb in 30 units, actor in 18.
  - As a result, 34 of 42 units are judged incomplete, against gold that is entirely complete.

The reasoning step appears to make the model literal about what the source states, but it applies this to the wrong slots.

## Conversion quality
fsl matches the human-human ceiling on cosine (0.894 vs 0.889) and Jaccard. It falls slightly short on sequence ratio, so its outputs are as close to annotators as annotators are to each other. zsl and cot are indistinguishable from each other, at about 0.77 cosine, roughly 0.12 below the ceiling.

Paska tracks the incompleteness calls closely. Units that cot judged incomplete pass only 9% of the time, and the dominant smells for zsl and cot are "Not requirement" and "Incomplete requirement". Dropping the modal verb or actor therefore produces malformed Rimay, not just a different label.

## Units and atomicity
All three strategies return exactly the requested unit count for all 15 requirements, and no count disagreements occur. Non-atomic detection is trivially perfect because the prompt states the count. Reordering is minor (cot 2, fsl 1, zsl 3) and should not affect the headline metrics if units are matched by content.

## Anomalies and inconsistencies
- No format breaches or count errors in any strategy.
- **cot and zsl:** implied actor and modal verb are marked missing (cot 18/30, zsl 10/13). The report gives no reqIds, so it is unknown whether these cluster in particular requirements.
- **zsl:** no scope is flagged missing in any unit, which is either systematic scope invention or a parsing gap in how zsl output encodes empty scope.
- **fsl:** Paska flags 2 units as "Non-atomic requirement" despite the split, meaning residual coordination in specific units.

## Next step
- Manually inspect zsl output for how scope is represented, to rule out a scorer or parser artifact before reporting fabrication.
- Emit per-unit reqIds for slot-level false positives and Paska smells, to locate the cot modal-verb drops and the 2 fsl non-atomic units.
- Run the blind task to see whether fsl's advantage survives once unit counts are not given.
