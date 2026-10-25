# Cross-strategy verdict — exp2_units_sonnet

Model: `claude-opus-5-5` · generated 2026-10-19T00:01:25+01:00 · runs: cot, fsl, zsl

Few-shot is the clear winner on every metric that has support, and on the main ones the gap is too large to be noise. The headline numbers still overstate how much is known, for reasons specific to this gold set.

Track 1 can be measured only for scope (42 genuinely missing) and condition (14 missing). Actor, modalVerb and action have zero gold-missing support, so their 0.0 scores mean nothing. Scope is degenerate: it is missing in all 42 requirements, so there are no true negatives. Flagging scope as missing every time is a constant behaviour, which FSL may have copied from its examples without any real judgement. Because scope makes up 42 of the 56 missing cases, it dominates the micro F1 of 0.96. Condition is the only slot that actually discriminates. There FSL flags 12 of 14 with 2 false positives, CoT flags 4 of 14 and ZSL flags none. Even at n=14, 12 versus 0 is decisive and 12 versus 4 is very unlikely to be chance. ZSL is the clearest case of the fabrication risk: it silently fills 55 of 56 missing slots.

The overall-verdict P/R/F1 of zero is also an artifact. The gold marks all 42 requirements complete, so only the agreement rate is informative there.

On Track 2, FSL gets close to the human ceiling: cosine 0.85 against 0.89, and seq_ratio 0.71 against 0.79. CoT and ZSL cannot be told apart (cosine 0.770 vs 0.769). The FSL gain of about 0.08 cosine is roughly five standard errors at 42 requirements, so it is real, even though the 126 pairs are not independent. One caveat matters, though: lexical similarity rewards matching the annotators' phrasing. Few-shot examples written in the same house style will inflate it whether or not the content is right. FSL also hits a maximum of 1.0, meaning at least one output is identical to a human conversion. Check that no few-shot example overlaps with the 42 test items before trusting this.

On Paska, FSL (38/42) against ZSL (34/42) is within noise.

CoT has a distinct failure mode the others lack. It flags implied slots as missing (actor 15 times, modalVerb 21 times) while missing most of the genuinely absent scope, so it is cautious about the wrong slots. Its 21 "incomplete" verdicts coincide with all 21 modalVerb flags and every one of them fails Paska. The resulting smells are "Not requirement", "Incomplete requirement" and "Incomplete system response". This looks like an output-contract problem: when CoT decides something is missing, it seems to drop "shall" and emit a non-requirement. On the 21 outputs it considered complete, its Paska pass rate (86%) matches the other strategies. The reqIds behind these patterns are not in the data provided, so I cannot point to specific examples; pulling the 21 CoT incomplete cases is the obvious first inspection.

FSL and ZSL each produce two "Non-atomic requirement" smells, even though the inputs were filtered to be atomic. That suggests the conversion itself sometimes adds coordinated content, which is worth checking, though two cases is too few to claim anything. None of this generalises to unfiltered requirements.

Next steps:
1. Audit the gold labels. A uniform "scope missing, actor and modal implied" pattern on every item looks like a default rather than per-item judgement.
2. Check for few-shot leakage into the test set.
3. Change the CoT output contract so it always emits a well-formed Rimay requirement plus a separate missing-slot field.
4. Rerun with several seeds and bootstrap confidence intervals.
5. Add items where actor or modalVerb is genuinely missing, so those slots become measurable.
