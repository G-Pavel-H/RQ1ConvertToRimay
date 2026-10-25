# Chain-of-Thought Prompt Template — atomicity and number of units not given

The following natural language requirement may contain one or several system responses.

Decide how many system responses it contains and convert each into its own atomic Rimay requirement. Follow the Rimay grammar and conversion rules from the system prompt. Reason step-by-step in a private scratchpad before producing output:

1. Identify the distinct system responses in the source NL and decide how many there are.
2. For each one, identify its **Scope** (otherwise `<MISSING_SCOPE>`), **Condition** (otherwise `<MISSING_CONDITION>`), **Actor** (otherwise `<MISSING_ACTOR>`), **Modal verb** (otherwise `<MISSING_MODAL_VERB>`) and **Action phrase** (otherwise `<MISSING_ACTION>`).
3. Decide the Rimay pattern (1–10 from the system prompt) for each one.

After your private reasoning, write the `<rimay>` … `</rimay>` block with one Rimay requirement per line. Do not put the scratchpad, the component list, or any commentary inside the block.

## NL Requirement

{nl_text}

## Rimay Output
