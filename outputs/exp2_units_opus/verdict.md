# Cross-strategy verdict — exp2_units_opus

Model: `claude-opus-5-5` · generated 2026-10-19T00:26:44+01:00 · runs: cot, fsl, zsl

Few-shot is the clear winner on every metric that can be measured here. The gaps are large enough to survive the small sample, but they come from a single run on 42 already-atomic requirements, so they should not be read as general.

On Track 1, only scope and condition can be measured. The gold marks scope missing in all 42 items and condition missing in 14. Actor and modalVerb have zero gold-missing support, so their 0.0 scores are artifacts and say nothing about fabrication. On the measurable slots the strategies separate sharply. Zero-shot never flagged a missing slot: it invented scope 42/42 times and condition 14/14 times, which is exactly the fabrication risk this experiment probes. Chain-of-thought was only partly better, flagging 18/42 scopes and 3/14 conditions. Few-shot flagged every missing scope and 10 of 14 conditions. Few-shot against chain-of-thought on scope (42/42 vs 18/42) is not noise. On condition (10/14 vs 3/14) the evidence is weaker but still unlikely to be chance (roughly p≈0.02 by Fisher's exact test). The 4 conditions few-shot still fabricated are the residual risk worth inspecting.

Zero-shot and chain-of-thought share a distinct failure mode that is more interesting than their low flag rate: they flag the wrong slots. They fill missing scope but mark actor (15–18 times) and modalVerb (29–30 times) as missing, where the gold calls both implied. That is why 34 of 42 outputs are declared incomplete against a gold standard that calls every item complete. This looks less like reasoning and more like a convention the models have not learned: they won't supply "shall" or "the system" unless told to, yet they happily invent a scope. Few-shot examples fix both behaviours at once, which suggests the examples teach the labelling convention rather than better judgement. The overall-verdict F1 of 0.0 in all runs is also an artifact: the gold has no incomplete requirements.

On Track 2, few-shot is effectively at the human ceiling. Its cosine is 0.888 against 0.889 for human–human, and its Jaccard is 0.609 against 0.607. Zero-shot and chain-of-thought sit about 0.13 cosine below, and with 42 items that gap is far outside noise. Zero-shot and chain-of-thought are indistinguishable from each other on similarity. The Paska gap (88% vs 31%) is not independent evidence. Every output the LLM called complete passed, and the dominant smells ("Incomplete requirement", "Not requirement") are what you would expect from outputs missing actor or modal. So Paska is largely re-measuring the slot-flagging behaviour above.

Several things are suspicious. Zero-shot and chain-of-thought have identical Paska counts (13/29) and identical verdict confusion (34/8) despite different Track 1 numbers. Check whether these are the same 8 requirements and whether the CoT prompt actually differed in practice. Few-shot reaches a similarity of 1.0 on some pairs, so check that no few-shot exemplar overlaps a test requirement. Few-shot also drew 3 "Non-atomic" smells on a set filtered to atomic items. Either the model merged clauses or Paska disagrees with the annotators, and either is worth a look. The results contain no per-requirement output, so I can't attach reqIds to any of these patterns; that per-item export is the first thing to pull.

Next steps, in order:
1. Inspect the zero-shot and CoT outputs where actor or modalVerb was flagged, to confirm whether this is a format issue.
2. Rerun each strategy with several seeds to get variance.
3. Audit the few-shot exemplars for leakage.
4. Add gold-incomplete requirements so that the verdict and the actor/modal metrics become measurable.

Only after that is it worth moving to the non-atomic set with a decomposition step.
