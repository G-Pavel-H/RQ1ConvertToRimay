# Meeting pack — 2026-11-13

RQ1: can LLMs convert NL feature requests into Rimay and flag incompleteness, and does a cheap model do as well as an expensive one?
Data: 50 Signal/Mastodon feature requests (35 atomic, 15 non-atomic → 77 units), 3 annotators, curated majority gold. 3 models (Haiku 4.5, Sonnet 5.5, Opus 5.5) × 3 strategies (zero-shot, few-shot, chain-of-thought) × 3 experiments = 27 runs.

| File | What it covers |
|---|---|
| `01_anomalies_and_bottlenecks.md` | What went wrong in the runs, and whether the model or our setup was to blame |
| `02_contribution_incompleteness_and_cost.md` | What this gives research and industry: a working definition of incompleteness, and the cheap-model result |
| `03_pragyan_comparison.md` | Our definition against Pragyan et al.'s, side by side, with the evidence that theirs is too vague |

## Six points to say in the meeting

1. **Definition:** a requirement is incomplete when a slot the Rimay grammar requires (actor, modal verb, action) is missing. Scope and condition are optional, and *implied* counts as present.
2. **"Implied" is the crux.** Feature requests almost never state the actor or "must", so the gold marks the modal verb implied in 90 of 99 atomic units. Sonnet and Opus, left without a definition of implied, flag 43–62 of the 73 complete units in Experiment 3 (zero-shot and CoT).
3. **A cheap model is enough.** Haiku matches Sonnet and Opus on conversion quality (few-shot cosine within .03 of the human↔human ceiling). It is the best-calibrated incompleteness and atomicity detector, at roughly ¼ of Opus's API cost and ½ of Sonnet's.
4. **Most anomalies come from our setup, not the models:** the few-shot examples, the prompt wording, Paska's preprocessing, and 3 safety-classifier interruptions. The real model weakness is choosing how many units a requirement has when not told.
5. **Pragyan's label:** they call 23 of 35 atomic requests incomplete and we call 4, and all 4 of ours are inside their 23 (κ .13). About 75% of their "missing information" is design detail, not missing requirement content.
6. **Proposal:** a layered view of incompleteness. L1 is a missing required slot (ours), L2 missing optional context, L3 design underspecification (mostly what Pragyan measures).

## Caveats to raise before someone else does

- Only 4 gold-incomplete requirements (4 of 35 atomic, 4 of 77 units, 0 of 42 split units). Agreement scores are inflated by the majority class, and F1 is unstable.
- **Annotator agreement:** raw agreement is 80–84% everywhere; Gwet's AC1, which is robust to rare labels, is .72–.79.
  - Incompleteness: Fleiss κ .32 → .37 after curation, low because the label is rare plus one outlier. Arthur–Rafo κ .61; Mko flags more (7 of 30).
  - Atomicity: Fleiss κ .60 (moderate), 84% agreement, pairwise κ .58–.64. Flags 13 / 10 / 19 (Arthur / Mko / Rafo); Rafo marked it with a `<NON_ATOMIC>` tag and notes instead of the checkbox.

  Curation adjudicated all of it into the gold. Details in file 02, §3.1.
- Haiku is the older 4.5 generation; Sonnet and Opus are 5.5. The runs used the subscription backend, so costs are estimated from the published per-token prices.
- The coding of Pragyan's 67 questions is CL's first pass and still needs Pavel's confirmation (0 of 29 confirmed).

## Tools (Dock apps)

Rimay Results (dashboard and prompts), Rimay Anomalies (Experiment 3 unit viewer with full replies), Rimay Curation, Rimay Pragyan. To rebuild them: `bin/make_apps.sh`.
