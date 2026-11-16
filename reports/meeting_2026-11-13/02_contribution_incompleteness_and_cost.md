# Contribution: a working definition of incompleteness, and the cheap-model result

## 1. The goal

- RE has no shared operational definition of requirement incompleteness. Studies label it ad hoc (see file 03).
- This work proposes a **starting point that can be tested**: tie incompleteness to the slots of a controlled natural language (Rimay).
- It also asks whether LLMs can apply that definition, and whether a cheap model is enough.

## 2. The definition

- Each atomic requirement (one system response) has five slots:
  - scope (optional)
  - condition (optional)
  - **actor** (required)
  - **modal verb** (required)
  - **action** (required)
- Each slot is **present**, **implied** (obvious from context, e.g. "the app", "must") or **missing**.
- **Incomplete** means a *required* slot is missing; implied counts as present. Missing optional slots never make a requirement incomplete.
- **Atomicity comes first:** non-atomic requests are split into units, and each unit is judged on its own.

## 3. What the data says about the definition

- **It separates incomplete requirements from underspecified design.** On the same 35 atomic requests we flag 4 and Pragyan flags 23. Our 4 are requests with no recoverable action, e.g. "how about we edit this one too? or just delete it" (file 03).
- **"Implied" can't be left out.**
  - Feature requests almost never name the actor or a modal verb: the gold marks the modal verb implied in 90 of 99 atomic units and the actor in 73 of 99.
  - A definition without implied makes almost everything incomplete, which is exactly what Sonnet and Opus did with our prompt: 43–62 of 73 complete units flagged in Exp 3 zsl and CoT (file 01, §3.3).
- **Disagreement can be located.**
  - Human Fleiss κ on overall incompleteness is fair: .32 before curation and .37 after, with 77% unanimous on 30 requirements.
  - Disagreement concentrates in the action slot (κ .34–.37) and in implied vs present for the actor, which doesn't change the verdict.
  - With a free-text label such as Pragyan's, nobody can tell where annotators disagree.
### 3.1 Annotator agreement

Original annotations, before curation.

| Label | Flags (Arthur / Mko / Rafo) | Raw pairwise agreement | Unanimous | Fleiss κ | Gwet AC1 |
|---|---|---|---|---|---|
| Incomplete (30 atomic) | 5 / 7 / 4 | 80% | 70% | .32 (fair) | .72 |
| Incomplete, after curation | — | 84% | 77% | .37 | .79 |
| Non-atomic (50) | 13 / 10 / 19 | 84% | 76% | .60 (moderate) | .73 |

- **Why incompleteness κ is low while agreement is 80%+:** the label is rare (14–18%). With rare labels, κ's chance correction punishes every disagreement heavily (the "kappa paradox"). AC1 corrects for this, and it is substantial.
- **Atomicity:** moderate, with no outlier rater. Rafo marked non-atomic with a `<NON_ATOMIC>` tag in the conversion text and in his notes, never the checkbox, so a flag is the checkbox, the tag or the notes (the annotation tool's rule). Pairwise κ .58–.64 (Arthur–Mko .61, Arthur–Rafo .64, Mko–Rafo .58). 20 requests flagged: 8 by all 3, 6 by 2, 6 by 1. Curation adjudicated them into 15 non-atomic.
- **Incompleteness:** Arthur–Rafo κ .61 (substantial). Mko is the outlier (κ .17 / .23), and the disagreements sit in the action slot.
- **How to report it:** give raw agreement, κ and AC1 per pair, and state that the gold is curated.
- **Same definition, applied by LLMs:** non-atomic detection F1 is .76–.93 against the gold, so the definition is usable once it is written down.

- **Once units are known, conversion is reliable.**
  - Exp 2: all 9 runs return exactly 42 of 42 units.
  - Few-shot cosine to the annotators is at the human↔human ceiling:

    | Experiment | Haiku | Sonnet | Opus | Human ceiling |
    |---|---|---|---|---|
    | Exp 2 | .88 | .89 | .91 | .89 |
    | Exp 3 | .87 | .87 | .84 | .88 |

## 4. Cheaper model, same quality?

Estimated API cost per 1,000 requests. Measured output tokens and about 2.3–3.3k input tokens per call, priced at the published per-million rates: Haiku 4.5 $1 / $5, Sonnet 5.5 $2 / $10, Opus 5.5 $4 / $20.

| | Zero-shot | Few-shot | CoT | Whole 300-call sweep |
|---|---|---|---|---|
| Haiku 4.5 | $3.66 | $3.41 | $5.23 | $1.23 |
| Sonnet 5.5 | $9.14 | $6.75 | $11.26 | $2.71 |
| Opus 5.5 | $16.76 | $13.97 | $25.82 | $5.66 |

- **Conversion quality is a tie.**
  - Few-shot slot accuracy: Exp 1 .87 / .94 / .93; Exp 3 .94 / .97 / .93 (Haiku / Sonnet / Opus).
  - Cosine is within .03 of each other and of the human ceiling.
- **Incompleteness detection favours Haiku.** Hits out of the 4 gold-incomplete, and false alarms on complete units:

  | Run | Exp 1 (4 inc. / 31 comp.) | Exp 3 (4 inc. / 73 comp.) |
  |---|---|---|
  | Haiku zsl | 2 hits / 1 false alarm | 4 / 13 |
  | Haiku CoT | 3 / 3 | 3 / 13 |
  | Sonnet zsl | 3 / 11 | 4 / 54 |
  | Opus zsl | 4 / 17 | 4 / 52 |
  | Opus CoT | 4 / 29 | 4 / 62 |
  | All few-shot | 0–1 / 0 | 0–1 / 0–3 (examples never show a missing slot) |

- **Atomicity detection favours Haiku too:** Exp 3 F1 for non-atomic detection is .93 for Haiku zsl, against .81–.85 for every Sonnet and Opus run.
- **Speed:** median latency is 2.3–5.7 s for Haiku, 2.6–6.0 s for Sonnet and 3.6–8.6 s for Opus. Opus CoT writes about 690 output tokens per call, against about 440 for Haiku CoT.
- **Verdict:** on this task the cheapest model does as well or better, at about ¼ of Opus's cost and ½ of Sonnet's. The bigger models aren't worse at language; they read "missing" more literally. Haiku 5.5 (listed at $0.10 / $0.50) would likely cut cost by another 10×; untested.

## 5. What industry gets from it

- **A cheap, automatable triage step:** NL feature request → split into units → Rimay with `<MISSING_*>` placeholders → incompleteness flagged by slot.
- **Placeholders become actionable questions for the requester:** `<MISSING_ACTION>` means "what should the system actually do?", and `<MISSING_ACTOR>` means "which component?". Design questions (where in the UI, defaults) are left for design time, where they belong.
- **A consistent bar across a backlog:** the same definition flags the same things whoever reads the request, and each flag points at a slot.
- **Model choice:** a small model with a clear definition beats a large model with a vague one, and costs about 4× less per request.

## 6. Limits and next steps

- **Limits:**
  - Only 4 gold-incomplete requirements. A larger incomplete sample is needed for stable precision and recall.
  - Single domain (messaging apps); Haiku 4.5 against 5.5 models; costs estimated, since runs used the subscription backend.
  - Gold is a majority vote, and CL reworded 59 units to diversify phrasing (slots unchanged).
- **Next steps:**
  1. Fix the few-shot examples and define implied in the prompt, then re-run the affected runs.
  2. Add Haiku 5.5.
  3. Extend to the layered model of file 03: L1 required slot (this work), L2 optional context, L3 design underspecification.
