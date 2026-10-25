# Cross-strategy verdict — exp3_blind_haiku

Model: `claude-opus-5-5` · generated 2026-10-24T22:09:06+01:00 · runs: cot, fsl, zsl

One thing needs settling first. The study note says non-atomic requirements were excluded, but these runs are the "blind" task. Fifty source requirements expand to 77 gold units, and 15 of the 50 are gold non-atomic (tp+fn = 15 in every run). This data is not the filtered atomic subset. Either the wrong config was attached or the filter was not applied. Please check before writing this up, because the unit-count findings below exist only because non-atomic items are present.

Track 1 is measurable here, but only for two slots. Scope has 76 gold-missing cases and condition has 35. Actor, modalVerb and action have 1, 1 and 4, which is too few to say anything about. On the two usable slots, fsl stands apart. It flags 76/76 missing scopes and 34/35 missing conditions, while cot flags 7 and 4 and zsl flags 12 and 7. A 0.94 versus 0.11 to 0.17 flag rate is far outside noise. Taken at face value, zero-shot and CoT fabricate scope and condition content most of the time, and few-shot largely does not.

There is a caveat on scope. Gold has only one present scope, and fsl flagged it missing as well (fp 1, tn 0). So the data cannot distinguish "fsl detects missing scope" from "fsl always writes scope as missing because the few-shot examples did." Condition is more convincing, with 34 true negatives alongside 34 true positives. That looks like genuine discrimination.

On conversion quality, fsl also leads. Its sequence ratio is 0.68 against a human-human ceiling of 0.72, and its Jaccard is 0.53 against 0.55. Both are close to as good as annotator agreement allows. Cot (0.51) and zsl (0.52) are indistinguishable from each other. The roughly 0.16 gap to fsl is several standard errors even allowing for only 77 independent items. Paska pass rate is fsl 0.84, zsl 0.74, cot 0.58. The fsl–zsl gap of about 10 points on around 75 items is suggestive rather than decisive. Cot is clearly worse. The cosine fields are all 0.0 on tiny n with embeddings unavailable, so ignore them.

Each of the two weaker strategies has its own failure mode. CoT produces structurally broken output. It labels 26 conversions incomplete, 24 of which gold considers complete. It drops actor and modalVerb in 13 and 20 implied cases, has 6 Paska errors, and draws 14 "Not requirement" smells. Its incomplete outputs pass Paska only 15% of the time. In effect, it flags the wrong slots as missing while inventing the right ones.

Zsl over-fragments badly, returning 152 units for 77 expected. Extreme cases include 316-Signal (1→18), 328-Signal (5→18), 4821-Signal (2→14) and 1504-Signal (1→13). That is enumerating sub-steps, not decomposing.

Fsl over-splits too (108 units, 14 false-positive non-atomic calls, e.g. 937-Signal and 42-Mastodon 1→3). Cot has the best non-atomic precision (0.71, 5 false positives). That difference over 35 atomic items is moderate but plausible.

Next steps, in order:

1. **Resolve the dataset question** above.
2. **Test whether fsl's scope flagging is real.** Inspect the few-shot examples for a "scope: missing" default, and add some atomic items that state scope explicitly.
3. **Hand-read the 26 CoT "incomplete" outputs** to see whether the reasoning trace is leaking into, or truncating, the final Rimay.
4. **Rerun each strategy two or three times.** Single runs at n=77 cannot separate the zsl/fsl Paska difference from sampling variance.
5. **Adjudicate more actor, modalVerb and action cases** if those slots are meant to be scored at all.
