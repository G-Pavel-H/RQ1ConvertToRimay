# Our incompleteness definition vs Pragyan et al. (2025)

Data: the same 50 feature requests (35 atomic, 15 non-atomic), from their reconciled corpus (`data/pragyan/`). Interactive version: the Rimay Pragyan app.

## 1. The two definitions

| | Pragyan et al. | Ours |
|---|---|---|
| Definition | Requests that "lack crucial information needed to fully describe the intended system behavior" | A required Rimay slot (actor, modal verb, action) is missing; implied counts as present |
| Unit | Whole request | Each atomic unit (non-atomic requests split first) |
| Annotation | Binary label, a free-text "what is missing" note and clarification questions | Present / implied / missing for each of 5 slots |
| Span or type | None | Slot-level, so every flag names what is missing |
| Their reported κ | .65 / .40 / .87 across three batches | Fleiss .32 → .37 (curated), 77% unanimous |

## 2. Head to head

| Set | Both incomplete | Pragyan only | Ours only | Neither | Agreement | κ |
|---|---|---|---|---|---|---|
| Atomic (35) | 4 | **19** | 0 | 12 | 46% | .13 |
| Non-atomic (15) | 0 | 6 | 0 | 9 | 60% | .00 |
| All (50) | 4 | **25** | 0 | 21 | 50% | .12 |

- They flag **29 of 50 (58%)**; we flag 4.
- **Every requirement we call incomplete, they do too.** Ours is a strict, operational core inside their broad label. The whole disagreement is 25 requests they call incomplete and we call complete.
- Where both agree, the request contains no request at all:
  - 592: "this guy on this issue requested it and I figure I'll help him/you out"
  - 1504: "how about we edit this one too? or just delete it"
  - 496: "I have seen this feature on other … apps it'd be great"
  - 83: "Better identity key synchronization"

## 3. What their "missing information" actually is

Their 67 clarification questions on the 29 flagged requests were each coded by what they ask for. This is CL's first-pass coding and needs Pavel's confirmation.

| Level | Share | Codes |
|---|---|---|
| **Design detail** | **50 (75%)** | how it works 22 · where in the UI 12 · defaults and settings 9 · look 3 · edge cases 3 · values 1 |
| Optional slot (scope: who or what it applies to) | 6 (9%) | — |
| Required slot (what is requested at all) | 6 (9%) | — |
| Not behaviour | 5 (7%) | rationale 2 · term meaning 2 · requester context 1 |

- Per request: 25 of 29 flagged requests are incomplete only because of design detail; 4 are genuinely incomplete.
- Typical "incomplete" requests and the question behind the label:
  - **4031** "Add option to cancel running media downloads" → "From where in the UI should this be accessible?" and "Should incomplete downloads be resumable?"
  - **6031** "Fingerprint unlock as an alternative to passphrase" → "Default or opt-in?" and "Recovery if fingerprint fails?"
  - **1363** "Indicate whether the last message is yours or your partner's" → "What should the indicator look like?"
  - **42-Mastodon** → "Where do you need this view to show?"

  All four are clear, implementable requirements. The questions are legitimate design questions, but they belong to design time, not to whether the requirement is complete.

## 4. Why their definition is the problem

1. **Unbounded.** "Fully describe the intended behavior" can never be satisfied: a design can always be more detailed. So incompleteness becomes the default (58% of requests), and the label stops discriminating.
2. **Can't be located.** There is no span and no type, so disagreement can't be diagnosed or reproduced. Their own κ swings from .40 to .87 between batches.
3. **Mixes levels.** Missing requirement content, design decisions, rationale and terminology all collapse into one binary label; 75% is design detail.
4. **It's what an unguided reader does.** LLMs left to judge "missing information" broadly behave like Pragyan's annotators; constrained by the slot definition, they behave like us. Exp 1, 35 atomic requests:

   | Run | Requests flagged | κ vs ours | κ vs Pragyan |
   |---|---|---|---|
   | Opus zsl (literal reading) | 21 | .16 | **.39** |
   | Sonnet zsl / CoT | 14 / 18 | .19 / .11 | **.30 / .25** |
   | Haiku zsl / CoT (slot-faithful) | 3 / 6 | **.52 / .54** | .09 / .10 |

   The open definition is the naive notion. A real definition has to constrain it.

## 5. Fair points against us

Raise these before someone else does.

- **Our human κ isn't higher than theirs** (.32–.37). Raw agreement is 80–84% and Gwet AC1 is .72–.79: κ is deflated because incompleteness is rare under our definition (about 15%), while Pragyan's label covers 58% of requests, which inflates their κ. The claim is that our disagreement can be located (mostly the action slot, one outlier rater), not that agreement is higher.
- **Our definition is deliberately narrow.** It calls "Add X" complete even when the design is open. That's a scope choice, and design underspecification is a real, separate concern.
- **One case they catch and we don't:** 245, where they asked "Is this a feature request or a support question?". Request validity is outside a slot definition.
- **Different units:** they label whole requests, we label units. For non-atomic requests, "incomplete" here means any unit is incomplete.
- **Coding needs confirmation:** the question codes are CL's first pass, 0 of 29 confirmed. Confirm them in the Pragyan app before citing the percentages.

## 6. Proposal: layered incompleteness

Constructive framing for the paper:

| Layer | Meaning | Who measures it |
|---|---|---|
| **L1 Structural** | A required slot (actor, modal verb, action) is missing | Our definition; automatable, located by slot |
| **L2 Contextual** | Optional context is missing: scope, condition | Our optional slots |
| **L3 Design** | Behaviour is underspecified: UI, defaults, parameters, edge cases | What Pragyan mostly labels; belongs to design time |

- **One-line message:** Pragyan et al. measure L3 and call it incompleteness. Without separating the layers, "incomplete" covers most feature requests and stops being useful. Our L1 definition is a reproducible starting point.
