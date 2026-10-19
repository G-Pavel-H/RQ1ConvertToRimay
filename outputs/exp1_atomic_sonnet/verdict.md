# Cross-strategy verdict — exp1_atomic_sonnet

Model: `claude-opus-5-5` · generated 2026-10-18T23:59:23+01:00 · runs: cot, fsl, zsl

Few-shot is the clear winner on almost every metric, and the margin is large enough to survive the small sample. Zero-shot and chain-of-thought are statistically indistinguishable from each other. Few-shot's headline numbers flatter it somewhat, though, and its one weakness sits on the slot that matters most.

Track 1 is measurable here, since every slot has non-zero missing support. The gap on missing-slot flagging is wide. Few-shot flags 57 of 61 genuinely missing slots, against 24 for CoT and 17 for ZSL, and it passes Paska on 30 of 35 outputs against 18 and 15. Differences of that size on 35 items are not noise. Its similarity to humans is also closer to the human-human ceiling. Cosine actually slightly exceeds the human ceiling (0.77 vs 0.74), and token metrics sit about 0.05 below it. The advantage over the other two strategies on similarity, about 0.07–0.09, is plausible but modest, given that the 105 pairs come from only 35 requirements and are not independent.

Much of the scope result is a base-rate effect. Gold marks scope missing in 34 of 35 requirements, and few-shot flags it missing in all 35. That is indistinguishable from a constant "scope: missing" policy, probably copied from the examples. Condition looks similar, with 27 flagged against 21 gold. What few-shot has really learned is the annotators' convention, not necessarily a judgement about each item. A trivial constant baseline would show how much of the 0.90 micro-F1 is genuine.

ZSL and CoT share a distinct failure mode that is two-sided miscalibration. They silently fill scope and condition when the source lacks them, which is the fabrication risk this experiment probes. At the same time, they flag actor and modal verb as missing when humans judged them implied (11–15 false positives each). ZSL calls 19 requirements incomplete where gold says 4. These incomplete outputs also degrade the Rimay sentence itself: Paska pass rates for LLM-incomplete outputs are 0.16–0.25, and "Not requirement" dominates the smells. That suggests placeholder text breaks the rule checker. CoT's small edge over ZSL (24 vs 17 flags, 18 vs 15 Paska passes) comes from the same items and should not be read as an effect of reasoning.

Few-shot's own failure is the action slot. It silently filled 3 of the 4 requirements missing an action, so its overall-verdict recall is 0.25: it called 3 of 4 truly incomplete requirements complete. A missing system response is the most consequential gap, and ZSL caught 3 of these 4. With n=4 this is suggestive rather than established, but it is exactly the fabrication the study is about.

No per-item examples were included, so I can't cite specific reqIds. Pulling the outputs for the three few-shot action fabrications, and the ZSL/CoT "Not requirement" cases, is the first thing to do.

These results apply only to the atomic subset. The two "Non-atomic" Paska smells in few-shot outputs are worth a quick look as possible LLM-introduced compounding, but nothing more.

For next steps, I'd run three things:
- **Constant baseline:** "scope missing, condition missing" for every item, to put the few-shot scores in context.
- **Example ablation:** a few-shot variant whose examples include present scope and a missing action, to test whether flagging is conditional or copied.
- **Repeated runs:** several seeds per strategy to measure run-to-run variance before claiming the CoT/ZSL ordering.

Expanding the set of requirements with missing actions beyond four is the only way to resolve the most important open question.
