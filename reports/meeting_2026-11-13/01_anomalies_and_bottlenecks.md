# Anomalies: what went wrong, and who is to blame

Scope: all 27 v2 runs (2,700 calls). Sources: each run's `results.json → anomalies`, the full replies in the manifests, the Paska source code.

## 1. Overview

| Anomaly | Count | Where |
|---|---|---|
| Text outside the `<rimay>` answer block | 90 | 79 by Haiku (Exp 3 zsl alone: 37 of 50); parsed out, no effect on scores |
| Unit count off the gold (other) | 45 | Exp 3 only |
| Atomic requirement split into several units | 42 | Exp 3 only; most in few-shot (Haiku 9, Sonnet 7) |
| Non-atomic requirement returned as one unit | 10 | Exp 3 only |
| `<NON_ATOMIC>` added to an atomic requirement | 9 | Exp 1, every model, 1–2 per run |
| No answer block | 3 | All 3 are Opus CoT, all safety-classifier interruptions (§3.1) |
| Non-Rimay lines dropped inside the block | 2 | Sonnet / Opus CoT |
| Truncated, empty, or Paska crash | 0 | — |

- Exp 2 (unit count given): **all 9 runs returned exactly 42 of 42 units**. Splitting is solved once the count is known.
- Exp 3 (count not given), exact count out of 50:
  - Haiku: 42 / 35 / 40
  - Sonnet: 38 / 39 / 36
  - Opus: 40 / 42 / 41

  This is where the models actually fall short.

## 2. Model shortfalls

- **Deciding how many units a requirement has.** 8–15 of 50 requirements get the wrong count in every run.
  - Over-splitting atomic requirements: 1–9 per run. Few-shot is worst (Haiku 9, Sonnet 7), likely because 3 of its 6 examples are split ones.
  - Merging non-atomic requirements: 0–4 per run.
- **Format discipline (Haiku).** Haiku writes analysis around the answer block (Exp 3 zsl: 37 of 50). The parser removes it, so scores are unaffected, but a production pipeline would need the same guard.
- **Granularity is often a judgement call, not an error.** All 24 cases where the dashboard said "no unit returned" (6 requirements: 802, 6390, 222, 6424, 4821, 6362) were checked:
  - 23 are merges: the content is there, just on fewer lines. Example: Opus wrote "import the private key and public keys from a file", where the gold has separate private-key and public-key units.
  - 1 looks dropped (6362).

## 3. Our setup

These look like model errors in the tables, but our design caused them.

### 3.1 Safety-classifier interruptions (3 replies, all Opus CoT)
- Exp 1 107-Mastodon, Exp 3 6459-Signal and 7310-Signal. All three requests are harmless (image descriptions, a share intent, a URL scheme).
- In each, the reply breaks off mid-reasoning ("Sc…") and continues as "Understood. I won't produce that response again…".
- Still `stop_reason: end_turn`, so nothing signalled it.
- Our Rimay-line filter accepted "…I can help with a different requirement…" as a Rimay line because "can" is a modal verb, so **a refusal was scored as a conversion**.
- Fix: detect refusal text and `stop_reason: refusal`, then retry or exclude. The API offers fallbacks; our subscription backend did not.

### 3.2 Few-shot examples never show a missing required slot
- Both example sets contain only `<MISSING_SCOPE>` and `<MISSING_CONDITION>`, and never a missing actor, modal verb or action.
- Result: few-shot finds 0–1 of the 4 gold-incomplete requirements in every run (Exp 1: Haiku 0, Sonnet 1, Opus 0). Its high agreement (.89–1.0) comes from the 89–100% complete majority.
- **"Few-shot wins everywhere" (earlier `STATUS.md`) is true for conversion quality, not for incompleteness detection.**

### 3.3 The prompt never defines "implied"
- The prompt says to use placeholders "for any unspecified structural slots". It never says that an obvious actor ("the app") or modal verb counts as present.
- Sonnet and Opus read this literally. Exp 3 zsl false alarms by slot, against the gold label:

  | Run | Modal verb (gold implied) | Actor (gold implied) | Action (gold present) |
  |---|---|---|---|
  | Opus zsl | 46 | 34 | 4 |
  | Sonnet zsl | 51 | 35 | 3 |
  | Haiku zsl | 8 | 8 | 6 |

- So the over-flagging is a definitional gap in the prompt, not a capability limit. It is also the strongest argument that an incompleteness definition must handle implied explicitly (file 02).

### 3.4 Paska measures something different from what the headline "Paska pass" suggests
- **Placeholders:** we strip `<MISSING_*>` before Paska.
  - With a required-slot placeholder, Paska pass is 10% (mostly "Not requirement" or "Incomplete").
  - Without one, it is 80–86%.

  The LLM Paska pass rate is therefore mostly a proxy for "the model wrote a required placeholder".
- **Quotes:** Paska's preprocessing merges quoted spans into one token, but only for curly quotes `“ ”`. We send straight `" "`, so the feature never fires.
  - The parser then reads names as verbs. "keep the archive and blocked flags" is flagged non-atomic; `"archive" and "blocked"` passes.
  - 23% of responses with unquoted and/or are flagged non-atomic, against 1% with none.
- **Fixed verb list:** "support" is hard-coded as "not precise" (27 of 28 flagged), along with define, process, perform, make, get, do, read and others.
  - These are style smells; Rimay itself allows the verbs.
  - Style-only failures are 15% of LLM Paska failures (90 of 602) and 0 of 11 annotator failures.

### 3.5 Evaluation design
- **Class imbalance:** 4 incomplete of 35, 4 of 77, and 0 of 42. Report hits and false alarms, not only agreement and F1.
- **Haiku 4.5 against 5.5 models:** a generation confound in the cost comparison.
- **Saved replies:** Exp 1 full replies exist only in MLflow (`mlruns/`); Exp 2 and 3 keep them in the manifests.

## 4. What I'd change before a re-run

Only the affected runs need re-running.

1. Add one few-shot example with a missing required slot (e.g. `<MISSING_ACTION>`).
2. Define implied in the system prompt: an actor or modal verb that is obvious from context counts as present.
3. Detect refusals; retry or exclude them.
4. Send curly quotes to Paska, and report structural and style smells separately.
5. Optionally, add Haiku 5.5 to remove the generation confound.
