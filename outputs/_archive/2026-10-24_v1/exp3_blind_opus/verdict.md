# Cross-strategy verdict — exp3_blind_opus

Model: `claude-opus-5-5` · generated 2026-10-24T22:51:11+01:00 · runs: cot, fsl, zsl

One thing first: the study context says non-atomic requirements were excluded, but these runs don't match that. All three are the blind task over 50 requirements expanding to 77 gold units, and 15 of those requirements are gold non-atomic (tp+fn = 15). Either the filter wasn't applied or the description belongs to a different run. I read the results as a mixed atomic/non-atomic set. The decomposition numbers below are real data, not an absent problem.

Few-shot is clearly ahead on conversion quality. Its sequence ratio against humans (0.689) and Jaccard (0.538) sit just under the human-human ceiling (0.721, 0.555). Zero-shot and CoT trail by roughly 0.12 to 0.15. With per-pair std near 0.18 and 77 items, the standard error is around 0.02, so this gap is well outside noise. Zero-shot and CoT are indistinguishable from each other on every similarity measure. Paska agrees: few-shot passes 90%, the other two 14%.

The Track 1 lead for few-shot (missing-slot flag rate 0.89 vs 0.41 CoT and 0.04 zero-shot) needs care. Scope carries 76 of the 117 missing labels, and gold marks scope missing in 76 of 77 units, so it is effectively a constant. Few-shot's perfect scope score most likely means its examples omit scope and the model copied that template. That is convention-matching, not evidence that it resists fabrication. Condition (support 35) is the only slot that really discriminates. There few-shot flags 28 of 35, CoT 16 and zero-shot 1. That gap is meaningful. Actor, modalVerb and action have support of 1, 1 and 4, so their precision and recall say nothing.

Zero-shot and CoT share a distinct failure mode, and it is not caution. They leave implied slots empty: modalVerb is unfilled in about 60 of 76 implied cases and actor in about 30. This matches Paska's dominant smells, "Not requirement" and "Incomplete requirement". These outputs are mostly not in Rimay shape, with no "The system shall". That is why 61 to 63 units are judged incomplete when gold says complete. Their apparent catch of the 4 gold-incomplete units is a side effect of flagging almost everything.

Few-shot's failure mode is the opposite. It never declares anything incomplete, and it filled the missing action in all 4 gold-incomplete units. These are the cases the experiment most cares about. Four is too few to quantify the risk, but the direction is the worrying one.

On decomposition, all three over-split, returning 101 to 115 units against 77 expected. Detection F1 (0.74 to 0.78) and exact-count rates (33 to 37 of 50) are within noise of each other. The over-splits are strikingly consistent across strategies: 5529, 6077, 316, 8-Mastodon, 7345, 175 and 2055-Signal are split by every run, and 328-Signal balloons from 5 to 14–20 units. Consistent disagreement like this suggests these gold labels may deserve re-examination, not only the model. It is also worth confirming how surplus units are aligned before scoring, since "evaluated" stays at 77. The cosine entries (n=3, all zero, embeddings unavailable) are an artifact and should be ignored.

Next steps:
- Check whether the few-shot examples omit scope.
- Give the zero-shot and CoT prompts an explicit default actor and modal verb, then rerun to separate format compliance from fabrication.
- Assemble more units with genuinely missing action and actor slots so the fabrication question becomes measurable.
- Run the atomic-only task the context describes.
- Use several seeds per strategy before reading anything into the CoT-versus-zero-shot comparison.
