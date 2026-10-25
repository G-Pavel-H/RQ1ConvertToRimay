# Cross-strategy verdict — exp3_blind_opus

Model: `claude-opus-5-5` · generated 2026-10-25T20:18:03+00:00 · runs: cot, fsl, zsl

## Summary
Few-shot is clearly best on conversion quality and on flagging optional missing slots. It is also the only strategy that marks all four gold-incomplete units as complete, so it hides the incompleteness this experiment is designed to catch. Zero-shot and chain-of-thought are indistinguishable from each other. The quality gap between fsl and the others is about 0.09 cosine, roughly four standard errors over 77 units, so it is real. The atomicity differences are within noise.

## Missing-slot detection
Support is adequate for scope (76) and condition (35). It is negligible for actor (1), modalVerb (1) and action (4), so per-slot F1 on those three moves in steps of one unit.

Fsl's high micro-F1 comes almost entirely from scope. On the slots that make a requirement incomplete, fsl filled all 4 missing actions and the single missing actor and modalVerb. Its overall-verdict recall is 0, so every gold-incomplete unit got invented content.

Zsl and cot catch those 4 units (recall 1.0), but only because they label nearly everything incomplete:
- They flag 52 and 62 complete units respectively as incomplete.
- They mark half or more of the implied actors and modal verbs as missing.
- They insert scope and condition the source never stated. Zsl silently fills 73 of 76 missing scopes.

This pattern suggests zsl and cot have not learned the slot-marking convention: they treat "implied" as "missing". It is a calibration failure, not a detection ability. None of the three strategies reliably flags a missing action: fsl 0/4, cot 0/4, zsl 1/4.

## Conversion quality
Fsl comes close to the human-human ceiling on every measure:

| Measure | fsl | Human-human ceiling |
|---|---|---|
| Cosine | 0.78 | 0.82 |
| Sequence ratio | 0.64 | 0.72 |
| Jaccard | 0.50 | 0.55 |

Zsl and cot sit well below the ceiling. Their Paska pass rates (0.30, 0.18 against fsl's 0.88) are driven by their "incomplete" outputs, which mostly fail as "Not requirement" or "Incomplete requirement". When the model judged a unit complete, all three strategies pass Paska at 0.81 to 0.91. The quality gap is therefore mostly the placeholder outputs, not worse Rimay.

## Units and atomicity
Exact-count compliance (40–42 of 50) and non-atomic detection (F1 0.82–0.85) are effectively tied across strategies. Every strategy merges 6390-Signal, the only false negative.

The disagreements in unit counts come from two sources:
- **Requirements every strategy gets wrong the same way.** These are 5529, 7345 and 2055 (split by all), 802 (4 instead of 6) and 328 (9–18 instead of 5). Consistent behaviour across strategies points at the source text or the gold, not the prompt.
- **Requirements one strategy handles differently.** These are 6077, 6459, 8-Mastodon, 7310, 1065, 6111 and 6424. Each differs by about one unit and shows no strategy-specific pattern.

## Anomalies and inconsistencies
- **cot, 6459-Signal and 7310-Signal:** no answer block, so a fallback reader was used. For 6459, scratchpad lines were dropped and the requirement was split. Leaked reasoning may have caused the split.
- **All strategies, 328-Signal:** severe over-splitting. It accounts for 11 of cot's 14 and 13 of zsl's 19 excess units, which inflates the returned-unit totals.
- **All strategies, 5529, 7345, 2055 and 802:** the same wrong unit counts.
- **All strategies:** 3 Paska errors each. The reqIds were not reported, which makes them untraceable.
- **fsl, the 4 action-missing units:** the actions were fabricated. This is the most serious finding, because these failures are invisible in fsl's headline metrics.

## Next step
- Read the fsl outputs for the 4 action-missing units, and the zsl/cot handling of implied actors and modals. Then add an "implied ≠ missing" example to the zsl and cot prompts, and a "do not invent an action" example to fsl.
- Re-adjudicate the gold for 328, 802, 6390, 5529, 7345 and 2055 before treating those count errors as model failures.
- Log reqIds for Paska errors. Require the answer block under cot, or strip scratchpad content before scoring.
