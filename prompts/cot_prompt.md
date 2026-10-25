# Chain-of-Thought Prompt Template

Convert the following natural language requirement to Rimay.

Follow the Rimay grammar and conversion rules from the system prompt. Reason step-by-step in a private scratchpad before producing output:

1. Identify the **Scope**, if any (for-each / quantifier phrase). Otherwise use `<MISSING_SCOPE>`.
2. Identify the **Condition**, if any (trigger, precondition, or temporal). Otherwise use `<MISSING_CONDITION>`.
3. Identify the **Actor** (the system or user that performs the action). Otherwise use `<MISSING_ACTOR>`.
4. Identify the **Modal verb** (must / shall / will). Otherwise use `<MISSING_MODAL_VERB>`.
5. Identify the **Action phrase** (verb-led structure: VERB MODIFIER? ARTICLE? THEME (preposition + ACTOR/TEXT)*). Otherwise use `<MISSING_ACTION>`.
6. Decide the Rimay pattern (1–10 from the system prompt) that best matches the structural content.
7. Check **atomicity**: if the source NL contains multiple system responses, select the most concrete primary action and flag the requirement with `<NON_ATOMIC>` at the end.

After your private reasoning, write the final Rimay sentence on a single line inside the `<rimay>` … `</rimay>` block. Do not put the scratchpad, the component list, or any commentary inside the block.

## NL Requirement

{nl_text}

## Rimay Output
