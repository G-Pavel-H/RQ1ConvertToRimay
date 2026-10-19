# Cross-strategy verdict — exp2_units_haiku

Model: `claude-opus-5-5` · generated 2026-10-18T23:44:05+01:00 · runs: cot, fsl, zsl

Track 1 is only partly measurable here. Actor, modalVerb and action have zero gold-missing support, so their 0.0 scores mean nothing, and the macro averages are diluted by those empty slots. Use the micro figures and the per-slot results for scope (42 missing) and condition (14 missing). One oddity is worth checking: scope is gold-missing in every one of the 42 requirements, yet the overall verdict calls all 42 complete. That is consistent with scope and condition being optional in Rimay, but confirm it reflects an annotation decision rather than an empty column.

On the measurable slots, the strategies separate sharply. Few-shot flagged 31 of 56 missing slots (55%), zero-shot flagged 5 (9%), and chain-of-thought flagged none, inventing a scope for all 42 requirements and a condition for all 14. The slots are clustered within requirements, so they are not fully independent. Even so, 31 versus 5 versus 0 is far outside noise, and 8/14 versus 0/14 on condition is notable despite the small count. Few-shot is clearly the least prone to fabrication. It is still far from safe, though, since it silently filled 19 scopes and 6 conditions.

CoT shows a failure mode the others do not. Reasoning appears to push the model to rationalise a complete structure rather than leave a slot empty. It does worse than plain zero-shot on the experiment's central risk. Its Paska pass rate is the highest (37/42), and that is not a contradiction. Fluently invented scope and condition text is exactly what a smell detector cannot see. Paska measures well-formedness, not faithfulness, so CoT's Paska score should not be read as quality.

On similarity, few-shot sits closest to the human ceiling. Its cosine is 0.845 against a human-human ceiling of 0.889, and its sequence ratio is 0.673 against 0.786. CoT (0.735) and zero-shot (0.756) trail by a clear margin, but are indistinguishable from each other given per-pair standard deviations around 0.10. The 126 pairs come from only 42 requirements, so the effective sample is smaller than it looks. Few-shot also reaches a maximum of 1.0, an exact match with a human conversion. Check whether any few-shot exemplar overlaps a test requirement before trusting that gap.

On Paska, zero-shot is weakest (29/42), driven mostly by passive voice (7). The few-shot versus CoT difference is two requirements and is noise. Zero-shot versus CoT is suggestive but not conclusive at n=42. Both zero-shot (3) and few-shot (2) produced outputs flagged as non-atomic, even though the inputs were pre-filtered as atomic. That suggests the model sometimes introduces coordination itself, which is worth inspecting.

The results contain no reqIds, so I cannot point to specific requirements. The next step is to pull them for few-shot's 25 silent fills, CoT's 14 invented conditions, the zero-shot actor flagged as missing, and the non-atomic outputs.

Beyond that, I would run in this order:
1. Repeat each strategy over several seeds and bootstrap confidence intervals by requirement, to get real variance.
2. Test few-shot combined with an explicit "leave unstated slots empty" instruction, and few-shot plus CoT, to see whether reasoning still erodes flagging when exemplars are present.
3. Audit the exemplars for leakage.
4. Re-run once the non-atomic requirements can be decomposed, since none of these conclusions transfer to the unfiltered set.
