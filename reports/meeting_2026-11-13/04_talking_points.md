# Talking points: what I did since the last meeting

The task was three pieces of analysis on the 27 runs: the anomalies, the contribution, and the comparison with Pragyan et al. Say them in this order. Each has one message, the evidence behind it, and the detail to keep ready if asked.

## Opening (one sentence)

> "I went through all 27 runs to separate real model failures from problems in our setup, worked out what the results contribute, and compared our incompleteness labels with Pragyan's on the same 50 requests."

## 1. Anomalies: most of what went wrong was our setup, not the models (file 01)

- **Message:** once you look inside the replies, most anomalies trace back to how we ran the experiment. The one real model weakness is deciding how many units a requirement has.
- **Evidence:**
  - When told the unit count (Exp 2), every run returns exactly 42 of 42 units. When not told (Exp 3), 8–15 of 50 requirements get the wrong count in every run.
  - The few-shot examples never show a missing required slot, so few-shot almost never flags incompleteness (0–1 of 4).
  - The prompt never says what "implied" means, so Sonnet and Opus mark obvious actors and modal verbs as missing.
  - Paska has quirks (quotes, a fixed verb list such as "support"), so its pass rate partly measures something else.
  - 3 replies were silent safety-classifier interruptions on harmless requests.
- **If asked, "so what now?":** 5 targeted fixes, then re-run only the affected runs.

## 2. Contribution: a usable definition of incompleteness, and a cheap model is enough (file 02)

- **Message:** tying incompleteness to Rimay's required slots gives a definition that can be tested, and the cheapest model applies it best.
- **Evidence:**
  - Definition: incomplete = a required slot (actor, modal verb, action) is missing, and implied counts as present.
  - "Implied" is essential: feature requests almost never name the actor or say "must". Without it, nearly everything is incomplete, which is exactly what the bigger models did.
  - Haiku ties Sonnet and Opus on conversion quality (at the human ceiling), is the best-calibrated detector of both incompleteness and atomicity, and costs about ¼ of Opus.
  - For industry: a cheap triage step in which every flag names the missing slot and turns into a question for the requester.
- **If asked about annotator agreement:**
  - Incompleteness: κ .32 but 80% raw agreement and AC1 .72. The label is rare, which pulls κ down; disagreement sits in the action slot.
  - Atomicity: κ .60, moderate.

## 3. Pragyan comparison: their definition is too vague to be useful (file 03)

- **Message:** Pragyan et al.'s "lacks crucial information" label is unbounded. It mostly measures open design questions, not missing requirements.
- **Evidence:**
  - They flag 23 of 35 atomic requests; we flag 4, and all 4 of ours are inside their 23.
  - About 75% of their clarification questions ask for design detail (where in the UI, which default), not missing requirement content.
  - Their label has no span and no type, so disagreement can't be located; their own κ swings from .40 to .87.
  - LLMs that judge freely behave like Pragyan's annotators, and LLMs given our slot definition behave like us.
- **Constructive framing:** layered incompleteness, so we are not just criticising them:
  - L1: a required slot is missing (ours)
  - L2: optional context is missing
  - L3: the design is underspecified (theirs)

## Say before they ask

- Only 4 gold-incomplete requirements, so F1 is unstable; I report hits and false alarms instead.
- Haiku is the 4.5 generation and the others are 5.5. The costs are estimates.
- The coding of Pragyan's questions is a first pass that I still need to confirm.

## Close: next steps to propose

1. Apply the fixes and re-run only the affected runs; add Haiku 5.5.
2. Confirm the Pragyan coding and write up the layered model.
3. A data paper on the annotated dataset; a draft already exists.
