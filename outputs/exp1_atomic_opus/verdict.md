# Cross-strategy verdict — exp1_atomic_opus

Model: `claude-opus-5-5` · generated 2026-10-19T00:24:47+01:00 · runs: cot, fsl, zsl

Few-shot (fsl) is clearly the strongest strategy on most metrics, but it fails on the narrow question this experiment exists to probe. Neither zero-shot (zsl) nor chain-of-thought (cot) is a usable alternative as they stand.

Track 1 is measurable here, but only partly. Support is solid for scope (34 genuinely missing) and condition (21). It is tiny for actor (1), modalVerb (1) and action (4). The micro scores are therefore almost entirely about scope and condition. Macro F1, which puts cot (0.45) ahead of fsl (0.38), is driven by slots with one or four examples and should not be trusted.

On scope and condition, fsl flags nearly everything correctly: 33/34 and 21/21. Cot is noticeably weaker on scope, flagging 25/34 and silently filling 9. Zsl flags none of the 55. That zero is suspicious. A complete failure on two slots, while zsl does emit "missing" for actor and modalVerb, looks more like an output-format or parsing problem than real fabrication. Check the raw zsl outputs before reporting it as invented content.

The more interesting result is that the strategies fail in opposite directions. Fsl fills everything. It filled all 66 implied slots (65/66), which is fine. It also filled all six genuinely missing actor, modalVerb and action slots, including all four missing actions. As a result, it judged all four gold-incomplete requirements complete, so its overall-verdict F1 is 0.

Inventing a system response that the source never stated is the central risk this study probes, and fsl does it every time the opportunity arises. Four cases is far too few to estimate a rate, but 0/4 against 4/4 for the other two strategies is a pattern worth chasing.

Cot and zsl fail the other way. They catch those four, but they flag implied actor and modalVerb as missing in most requirements. Gold says implied in 29 and 34 cases, while their fill rates are only 0.28/0.06 for cot and 0.38/0.18 for zsl. Because of this they declare 26–29 gold-complete requirements incomplete. They are literalist rather than fabricating.

That literalism also explains their poor Paska results. The dominant smell for both is "Not requirement" (25 for cot, 18 for zsl), which is what you would expect if the output lacks an actor or modal. So the gap between fsl's 91% pass rate and their 6–11% is real in size, but it is downstream of the flagging behaviour, not independent evidence of better prose.

On similarity, fsl sits at the human ceiling. Its seq ratio and Jaccard are just under human-human (0.628 vs 0.644, 0.469 vs 0.493), and its cosine is slightly above (0.768 vs 0.737). Cot and zsl trail by roughly 0.06–0.12. With only 35 independent requirements and standard deviations around 0.2, the fsl-vs-cot seq-ratio gap is borderline. The Jaccard gap is more convincing. Cot and zsl are indistinguishable from each other on similarity. Part of fsl's advantage may simply be that its examples teach the annotators' house style.

The summary contains no per-requirement data, so I cannot name the reqIds behind these patterns. Recall that this is also the atomic-only subset, so none of this generalises to unfiltered requirements.

Next steps, in order:

1. Pull the raw zsl outputs to settle whether the scope/condition zeros are a parser artifact.
2. Identify the four gold-incomplete reqIds and the four missing-action cases, and read all three strategies' outputs side by side. These eight-ish items are where the real finding lives.
3. Add an fsl example that explicitly flags a missing actor or action, then re-run to see whether fill-everything is learned from the current examples.
4. Run paired bootstrap tests on similarity, and repeat each strategy a few times to measure run-to-run variance.
5. Grow the pool of genuinely incomplete requirements. With four positives, no verdict-level claim is defensible.
