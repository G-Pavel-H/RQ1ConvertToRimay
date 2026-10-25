# Cross-strategy verdict — exp3_blind_sonnet

Model: `claude-opus-5-5` · generated 2026-10-24T22:27:49+01:00 · runs: cot, fsl, zsl

Your study note says non-atomic requirements were excluded, but this data does not match that description. All three runs are the blind task. Fifteen of the 50 source requirements have more than one gold unit (328-Signal has 5, 802-Signal has 6), and 77 units come from 50 requirements. Either the wrong batch was attached or the filter was not applied. I interpret the data as it stands, but the "already-atomic subset" framing does not apply to these numbers.

On conversion quality, few-shot is clearly ahead. Its sequence similarity to the humans is 0.69, close to the human-human ceiling of 0.72, while CoT and zero-shot both sit around 0.56. The Paska pass rate shows the same split: 0.92 for few-shot against roughly 0.33 for the other two. The gaps are far larger than the standard deviations would allow noise to explain. Few-shot examples may resemble the test items, though, which would inflate similarity, so check for overlap.

CoT and zero-shot share a failure mode that few-shot does not have. They flag the actor and modal verb as missing on about half the requirements where the gold says these are only implied. As a result, 48 to 49 of 77 outputs are judged incomplete, and Paska reports "Not requirement" 29 to 33 times. Among the outputs those strategies did complete, the Paska pass rate (0.81 to 0.88) is close to few-shot's 0.92. So almost the entire Paska gap comes from structurally truncated Rimay, not from bad phrasing. Look at the raw outputs before treating this as model behaviour. A pattern this regular could easily be a parsing or schema problem, such as the actor and modal fields not being emitted in the expected format.

Read Track 1 with care. Scope is gold-missing in 76 of 77 units, so a policy of always flagging scope as missing scores nearly perfectly. Few-shot appears to do exactly that: it even flagged the one present scope. Its micro-F1 of 0.96 is therefore mostly a learned template, not real discrimination. Actor and modal verb each have a support of 1, and action has 4, so those per-slot figures are uninterpretable.

Condition is the honest comparison, because it has 35 missing and 42 present. Few-shot flagged 34 of 35 missing conditions, CoT 22, and zero-shot 11, with similar false-positive counts. That is a real ordering. Zero-shot silently invented conditions in 24 cases and scope in 62, which makes it the strategy that fabricates the most.

On segmentation, all three strategies over-split badly, returning 112 to 134 units against 77 expected. Non-atomic recall is near perfect but precision is only 0.5 to 0.64. CoT is somewhat better: 34 of 50 exact counts versus 27, and 8 false positives versus 14 to 15 among 35 atomic requirements. With 50 requirements and a single run per strategy, that difference is suggestive but not established. Several items are over-split by all three strategies: 6077, 7345, 175, 2055 and 8-Mastodon, each gold-atomic, plus 328-Signal, which returns 14 to 19 units against 5. Persistent cross-strategy errors like these point to either genuinely compound source text or a granularity disagreement with the gold, so they are worth re-adjudicating. Similarity scores also depend on how mismatched units were aligned, which adds uncertainty for those items. The cosine entries are all zero with an n of 3 or 6, and the embedding model was unavailable, so ignore them.

Next steps, in order:
1. Confirm whether the dropped actor and modal fields in CoT and zero-shot are a parsing artifact.
2. Rerun the atomic task separately so that decomposition is not mixed with conversion.
3. Add requirements with an explicit scope, so the scope slot can actually test discrimination.
4. Repeat each strategy over three or more seeds to get variance estimates.
5. Try a few-shot-plus-CoT variant aimed at the over-splitting problem.
