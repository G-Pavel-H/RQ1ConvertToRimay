# Cross-strategy verdict — exp1_atomic_sonnet

Model: `claude-opus-5-5` · generated 2026-10-25T20:04:55+00:00 · runs: cot, fsl, zsl

## Summary
Few-shot (fsl) beats cot and zsl on every measure: it flags 93% of genuinely missing slots against 30% (cot) and 15% (zsl), and it reaches the human-human ceiling on conversion similarity. The fsl margin is well outside noise even at n=35. cot and zsl differ only slightly from each other, and that difference is mostly within noise. One weakness offsets fsl's lead: it detects only 1 of the 4 gold-incomplete requirements.

## Missing-slot detection
Scope drives the headline gap. fsl flags scope missing on all 35 requirements, including the one where gold has a scope. That pattern cannot be told apart from a constant "missing" default, possibly copied from the few-shot examples. Excluding scope, fsl still leads clearly: micro P≈0.82 and R≈0.85, against roughly 10 true positives and 29 false positives for cot. On condition, fsl's advantage is real discrimination, with 20 of 21 missing slots flagged and 5 false flags.

cot and zsl make an inverted error. They silently fill missing scope and condition, yet flag implied actor and modal verb as missing (12–15 false positives each, implied fill rate around 0.6). Those spurious flags inflate their requirement-level recall to 0.75 at a precision of 0.17–0.21.

fsl's real failure is the action slot. It silently filled 3 of 4 missing actions, which is why its requirement-level recall is 0.25. With only 4 gold-incomplete requirements, each one moves that recall by 0.25, so the comparison is fragile. Even so, this is fabrication on the slot that matters most.

## Conversion quality
fsl sits at about 96% of the human ceiling on sequence ratio and 93% on Jaccard, and slightly exceeds the ceiling on cosine. That is plausible for an output that resembles a consensus of the annotators. cot and zsl reach about 78–83% of the ceiling. The fsl–cot gap of 0.09 in sequence ratio is about 2.5 standard errors; the cot–zsl gap of 0.03 is noise.

The Paska gap (0.83 against 0.43–0.46) is mostly a side effect of how cot and zsl handle missing slots. Their outputs carrying missing-slot placeholders fail almost completely (16 of 18 and 14 of 14), dominated by the "Not requirement" smell. On outputs the model judged complete, all three strategies pass at 0.76–0.85.

## Anomalies and inconsistencies
- **zsl format breach:** text outside the answer block on 175, 4031, 4230, 496, 5555, 6077 and 757-Signal. This is cosmetic if extraction succeeded.
- **757-Signal in cot:** text outside the block plus a stray `<rimay>` line removed. 757 breaks format under both cot and zsl, which suggests something in its source text triggers commentary.
- **Spurious `<NON_ATOMIC>` flags:** 2055-Signal under cot and zsl, and 1504-Signal under cot. fsl never flags these, but Paska reports two non-atomic smells in fsl output. 2055 may be borderline in gold.
- **fsl:** no anomalies.

## Next step
- Rerun Track 1 with scope excluded, or with a scope-present control, to confirm that fsl's scope flagging is not a template default.
- Pull the 3 gold-incomplete requirements where fsl filled the action and inspect what it invented. Consider a few-shot example with a missing action.
- Re-adjudicate 2055-Signal's atomicity and inspect 757-Signal's source for the format trigger.
