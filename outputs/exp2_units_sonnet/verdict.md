# Cross-strategy verdict — exp2_units_sonnet

Model: `claude-opus-5-5` · generated 2026-10-25T20:10:24+00:00 · runs: cot, fsl, zsl

## Summary
FSL is clearly best on every dimension. It flags 89% of genuinely missing slots, against 14% for CoT and 0% for ZSL, and it reaches roughly 90% of the human–human similarity ceiling with a 95% Paska pass rate. These gaps are far larger than noise, even at 42 units from 15 requirements. CoT and ZSL cannot be told apart on conversion quality.

## Missing-slot detection
Track 1 can be measured only for scope (42 gold-missing) and condition (14). Actor, modalVerb and action have zero support, so their 0.0 scores mean nothing. The overall-verdict F1 is undefined because all 42 gold units are complete.

The strategies fail in opposite directions:
- **FSL** flags scope as missing almost perfectly (40/42) and condition reasonably (10/14). It fills all 84 implied actor and modal slots.
- **ZSL and CoT** reverse this. They fill nearly every genuinely missing scope and condition slot (ZSL all 56, CoT 48), yet leave out the implied actor or modal verb in 15 (ZSL) and 18 (CoT) units. Those units are then marked incomplete even though the gold calls them complete.

Without examples, the model does not seem to understand the Rimay slot convention. CoT reasoning makes this worse, not better. A 0% flag rate on scope may partly be a scoring artefact if ZSL left scope empty without an explicit "missing" marker. That needs checking before anyone calls it fabrication.

## Conversion quality
On all three similarity measures, FSL comes within about 0.05–0.08 of the human–human ceiling (sequence ratio 0.71 vs 0.79). ZSL and CoT trail FSL by about 0.07–0.15 and differ from each other by less than one standard deviation.

Most of the Paska gap traces back to the dropped actor and modal slots. CoT's incomplete outputs pass 0/18, mostly with "Not requirement" and "Incomplete requirement" smells. Restricted to outputs the LLM itself judged complete, the pass rates are close: CoT 0.79, ZSL 0.81, FSL 0.95.

## Units and atomicity
All strategies returned exactly the requested number of units for all 15 requirements. Non-atomic detection is trivially perfect because the prompt states the count, so it tells us nothing here. There are no count disagreements between strategies.

The strategies differ only in unit ordering: CoT reordered units in 4 requirements, FSL in 3, ZSL in 1.

## Anomalies and inconsistencies
- **Missing actor/modal (ZSL, CoT):** 15 and 18 units respectively lack an implied actor or modal verb. The output does not identify which reqIds, so these need to be extracted.
- **Unit reordering:** CoT 4, FSL 3, ZSL 1. If unit-to-gold alignment is positional, this could depress similarity scores, so verify how matching is done.
- **Possible scoring artefact:** ZSL's 0% scope flag rate may reflect an empty-but-unmarked scope being scored as filled.
- No format breaches or Paska errors in any strategy.

## Next step
- Read the ZSL and CoT outputs for the 15 and 18 incomplete units. Confirm whether "silently filled" scope means invented text or simply an empty field.
- Check that unit matching is content-based rather than positional, then rescore if it is positional.
- Add to the ZSL and CoT prompts the instruction that actor and modal are always required and scope is optional, then re-run to see whether the gap to FSL closes.
