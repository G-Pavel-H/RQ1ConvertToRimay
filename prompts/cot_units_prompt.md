# Chain-of-Thought Prompt Template — non-atomic requirement, known number of units

The following natural language requirement is **non-atomic**: it contains **{n_units}** system responses.

Convert it into exactly **{n_units}** atomic Rimay requirements, one per system response. Follow the Rimay grammar and conversion rules from the system prompt. Reason step-by-step in a private scratchpad before producing output:

1. Identify the **{n_units}** distinct system responses in the source NL.
2. For each one, identify its **Scope** (otherwise `<MISSING_SCOPE>`), **Condition** (otherwise `<MISSING_CONDITION>`), **Actor** (otherwise `<MISSING_ACTOR>`), **Modal verb** (otherwise `<MISSING_MODAL_VERB>`) and **Action phrase** (otherwise `<MISSING_ACTION>`).
3. Decide the Rimay pattern (1–10 from the system prompt) for each unit.

After your private reasoning, write the line `Final Rimay:` followed by exactly {n_units} lines, one Rimay requirement per line. Do not include the scratchpad, the component list, or any commentary after `Final Rimay:`.

## NL Requirement

{nl_text}

## Rimay Output
