# Few-Shot Prompt Template — non-atomic requirement, known number of units

The following natural language requirement is **non-atomic**: it contains **{n_units}** system responses.

Convert it into exactly **{n_units}** atomic Rimay requirements, one per system response. Follow the Rimay grammar and conversion rules from the system prompt. Force conversion: in each unit, use `<MISSING_*>` placeholders for any structural slot the source NL does not specify. Do not invent content.

## Examples

{examples_block}

## NL Requirement

{nl_text}

## Rimay Output ({n_units} lines, one Rimay requirement per line)
