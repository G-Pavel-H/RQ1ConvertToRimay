# Experiment status — 2026-10-25 (v2)

Details and changes: `EXPERIMENTS.md`. Results: `bin/dashboard.sh` (opens `outputs/dashboard.html`). Embeddings need Ollama running (`ollama serve`); without it cosine is blank and units are matched by seq_ratio.

| # | Data | Prompt says | State |
|---|---|---|---|
| 1 | 35 atomic | — | v2 done, 9/9 |
| 2 | 15 non-atomic → 42 units | non-atomic + unit count | v2 done, 9/9 |
| 3 | all 50 (35 + 15) → 77 units | atomicity defined; neither which nor how many | v2 done, 9/9 |

v2 = answer block `<rimay>…</rimay>`, Rimay-line filter, atomicity definition, anomaly tracking, sectioned verdicts. Converted 2026-10-24/25; re-scored with cosine 2026-10-25 20:00. v1 archived in `outputs/_archive/2026-10-24_v1/`.

## v2 headline (agreement / slot accuracy / median cosine / Paska pass)

Human ↔ human median cosine: Exp 1 .82 · Exp 2 .89 · Exp 3 .88.

| | zsl | fsl | cot |
|---|---|---|---|
| Exp 1 haiku | .91 / .71 / .72 / .80 | .89 / .87 / .83 / .71 | .89 / .67 / .71 / .71 |
| Exp 1 sonnet | .66 / .57 / .74 / .46 | .91 / .94 / .85 / .83 | .54 / .59 / .74 / .43 |
| Exp 1 opus | .51 / .46 / .71 / .40 | .89 / .93 / .81 / .89 | .17 / .59 / .71 / .00 |
| Exp 2 haiku | .71 / .69 / .74 / .69 | 1.0 / .93 / .88 / .79 | 1.0 / .73 / .75 / .60 |
| Exp 2 sonnet | .64 / .62 / .78 / .64 | 1.0 / .95 / .89 / .95 | .57 / .60 / .78 / .45 |
| Exp 2 opus | .64 / .62 / .79 / .64 | 1.0 / .99 / .91 / .88 | .19 / .60 / .77 / .21 |
| Exp 3 haiku | .83 / .69 / .73 / .80 | .95 / .94 / .87 / .83 | .82 / .65 / .73 / .68 |
| Exp 3 sonnet | .30 / .52 / .75 / .27 | .96 / .97 / .87 / .88 | .44 / .57 / .75 / .41 |
| Exp 3 opus | .32 / .48 / .73 / .30 | .91 / .93 / .84 / .88 | .19 / .61 / .74 / .18 |

## v2 Exp 3 — atomicity decisions (v1 in brackets)

| | exact count /50 | units /77 | atomic split /35 | non-atomic merged /15 | detection F1 |
|---|---|---|---|---|---|
| haiku zsl/fsl/cot | 42/35/40 (28/30/37) | 84/98/84 | 1/9/3 | 1/0/4 | .93/.77/.76 |
| sonnet zsl/fsl/cot | 38/39/36 (27/27/34) | 100/99/100 | 4/7/4 | 1/0/1 | .85/.81/.85 |
| opus zsl/fsl/cot | 40/42/41 (33/37/35) | 96/83/91 | 5/4/5 | 1/1/1 | .82/.85/.82 |

## Intricacies

- **v1 → v2:** the answer block and the definition cut atomic over-splitting (v1 5–15/35 → v2 1–9) and raised exact counts. Exp 2: 9/9 runs exact on 15/15 (v1 Sonnet zsl 11/15 was chatter, now gone).
- **FSL wins everywhere** (agreement .89–1.0, cosine at or near the human ceiling). Sonnet/opus zsl and cot mark *implied* actor/modal verb as `<MISSING_*>`, so complete requirements turn incomplete (opus cot .17–.19). Haiku does not.
- **Haiku zsl is the best atomicity detector** (F1 .93) but breaks the format most: text outside the block on 37/50 (parsed out, listed as anomalies).
- **Format anomalies:** opus skips the block occasionally (Exp 1 cot 1, Exp 3 cot 2); every model adds `<NON_ATOMIC>` to 1–2 atomic requirements in Exp 1.
- **FSL splits more in Exp 3** (haiku 9, sonnet 7 atomic split): the 3 split examples likely bias it towards splitting.
- **Exp 1 incomplete-F1 is unstable** (4/35 gold-incomplete). **Exp 2 incomplete-F1 is undefined** (all 42 units complete).
- **Scope** is optional and gold-missing almost everywhere, so it dominates the missing-slot numbers.
- **Unit matching** is by cosine to the annotators' units; requirements off the gold count are in the dashboard's unit-count table and each run's anomalies.

## Pending

- Verdicts regenerating with the cosine data (`outputs/_logs/regen_verdicts.sh`, log `outputs/_logs/experiments.log`); the dashboard rebuilds when done.
- Curation tool: press **Run Paska** to re-check the 59 reworded CL units.
