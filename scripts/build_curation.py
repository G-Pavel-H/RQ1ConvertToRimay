#!/usr/bin/env python3
"""Seed ``data/curation.json``: the starting point for local curation.

Run once. It combines
  * the 30 atomic requirements, with Claude's refined conversions
    (data/gold_annotations_main_atomic_refined.csv) — one unit per annotator;
  * the 20 non-atomic requirements, decomposed by Claude into atomic units per
    annotator (the DECOMPOSITIONS table below).

From then on ``data/curation.json`` is the source of truth and is edited in the
browser tool (``scripts/curate.py``). This script refuses to overwrite it unless
``--force`` is given, so a re-run can never wipe your curation.

Decomposition rule
------------------
Distinct system responses become separate units; alternatives or examples of one
behaviour stay a single unit (e.g. 937's "Idea 1/2/3" are three candidate
implementations of one requirement). By that rule 5 of the 20 are atomic.

Provenance, shown on every unit in the tool
  annotator         the annotator's own text, unchanged
  annotator-refined the annotator's text, edited by Claude (the atomic 30)
  annotator-split   the annotator's text, split into units by Claude
  claude            written by Claude — the annotator gave no usable conversion
                    for this part (e.g. only "<NON_ATOMIC>")
Where every unit of a requirement is "claude" for all three annotators, the
three texts are identical: there is no independent human baseline for it.
"""
from __future__ import annotations

import argparse
import csv
import json
import sys
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from src import config  # noqa: E402
from src.curation import STATE_PATH, new_unit_id, now, save_state  # noqa: E402

ANNOTATORS = ["Arthur", "Mko", "Rafo"]
S = "<MISSING_SCOPE>, "
C = "<MISSING_CONDITION>, "
ALL = "<MISSING_SCOPE>, <MISSING_CONDITION>, <MISSING_ACTOR> <MISSING_MODAL_VERB> <MISSING_ACTION>."
CODE = {"m": "missing", "i": "implied", "p": "present"}
KEYS = ["scope", "condition", "actor", "modalVerb", "action"]


def sl(code: str) -> dict:
    """'mmiip' -> {scope: missing, condition: missing, actor: implied, ...}."""
    return {k: CODE[c] for k, c in zip(KEYS, code)}


def U(text, slots, ctype="none", prov="claude"):
    return {"rimayText": text, "slots": sl(slots), "conditionType": ctype, "provenance": prov}


# --- Claude's canonical decomposition of each non-atomic requirement ---------
# (reqId) -> (decision note, [units])
CANON = OrderedDict([
 ("937-Signal", ("1 unit. Ideas 1-3 (sent timestamp, sequence numbers, vector clocks) are alternative implementations of one requirement: a consistent message order.", [
   U(S+C+'the App must display the "messages" in the "conversation view" in the same order for all "chat participants".', "mmiip")])),
 ("42-Mastodon", ("1 unit. \"Hot trending\" is something the author already likes; the request is the timeline view. (Arthur read it as two feeds - add a unit if you agree.)", [
   U(S+C+'the App must provide a view of the posts in the "public timeline".', "mmiip")])),
 ("1055-Signal", ("2 units: move drafts to the top; mark them visually.", [
   U(S+C+'the App must automatically move the "message drafts" to the top of the "message list".', "mmiip"),
   U(S+C+'the App must visually mark the "message drafts", such as with a different color or a "Draft" label.', "mmiip")])),
 ("1692-Signal", ("2 units: save the received time; show it in the message details.", [
   U(S+'when the App receives an "SMS delivery report", then the App must save the time at which the recipient received the message.', "mpiip", "trigger"),
   U(S+C+'the App must show the saved "received time" in the "message details".', "mmiip")])),
 ("6390-Signal", ("2 units: notify the sender after a timeout; offer a resend via insecure SMS.", [
   U(S+'if the recipient does not receive a sent "Signal message" within a "timeout", then the App must notify the "sender".', "mpiip", "precondition"),
   U(S+'if the recipient does not receive a sent "Signal message" within a "timeout", then the App must offer the "sender" an option to resend the message via "insecure SMS".', "mpiip", "precondition")])),
 ("7345-Signal", ("1 unit. \"Detect it is saved\" is the precondition and \"otherwise show the save button\" is today's behaviour; the only new response is the icon.", [
   U(S+'if the user has already saved a "media item", then the App must show a "view" or "share" icon instead of the "save" button.', "mpiip", "precondition")])),
 ("222-Signal", ("3 units: block a number; delete its messages on receipt; don't notify.", [
   U(S+C+'the App must let the user block a user-specified "phone number".', "mmiip"),
   U(S+'when the App receives a message from a "blocked number", then the App must delete the message.', "mpiip", "trigger"),
   U(S+'when the App receives a message from a "blocked number", then the App must not show a "notification".', "mpiip", "trigger")])),
 ("175-Signal", ("1 unit. \"A button combo + confirm dialogue\" is offered as an example (\"say...\") of the quick way, not a separate requirement.", [
   U(S+C+'the App must provide a quick way, such as a "button combo" with a "confirm dialogue", to wipe the "text message database".', "mmiip")])),
 ("1065-Signal", ("2 units: ask for the mode on first start; explain the modes on request.", [
   U(S+'when the user starts the App for the first time, then the App must ask the user to choose between "only use push", "use SMS fallback", and "use as default SMS/MMS app".', "mpiip", "trigger"),
   U(S+'when the user presses the "what does that mean?" button, then the App must show a "pop-up" that explains each mode.', "mpiip", "trigger")])),
 ("328-Signal", ("5 units for the core request. The \"possible extensions\" paragraph (opt-out, non-TextSecure recipients, circulating numbers) is left out as speculative.", [
   U(S+'if the user has enabled "dummy messaging", then the App must send "dummy messages" to random TextSecure contacts at random times.', "mpiip", "precondition"),
   U(S+'when the App receives a "dummy message", then the App must discard it automatically.', "mpiip", "trigger"),
   U(S+C+'the App must let the user define the quantity of "dummy messages".', "mmiip"),
   U(S+'if the device is "roaming", then the App must not send "dummy messages" unless the user has enabled it.', "mpiip", "precondition"),
   U(S+'if the "battery level" is below a "threshold", then the App must not send "dummy messages".', "mpiip", "precondition")])),
 ("6111-Signal", ("3 units: multi-delete in the gallery; delete in the opened view; confirm before deleting. The note about the attached text message is discussion, not a request.", [
   U(S+'when the user opens the "image gallery", then the App must let the user select multiple "images/media" and delete them.', "mpiip", "trigger"),
   U(S+'when the user opens an "image/media", then the App must let the user delete it.', "mpiip", "trigger"),
   U(S+'when the user deletes "images/media", then the App must show a "confirmation dialog".', "mpiip", "trigger")])),
 ("226-Signal", ("2 units: edit a message; insert a new one. The second is tentative in the NL (\"Maybe also...\") - remove it if you read it as not requested.", [
   U(S+C+'the App must let the user edit any message from the "Android context menu".', "mmiip"),
   U(S+C+'the App must let the user insert a new message next to an existing message.', "mmiip")])),
 ("4821-Signal", ("2 units: offer \"acknowledge\" on a received message; replace the indicator once acknowledged.", [
   U(S+'when the user long-presses a "received message", then the App must offer an "acknowledge message" option.', "mpiip", "trigger"),
   U(S+'when the user selects "acknowledge message", then the App must replace the "received" indicator with an "acknowledgement" indicator.', "mpiip", "trigger")])),
 ("2055-Signal", ("1 unit. Renaming the label to \"Sent\" is offered only as a fallback \"when this is rejected\" - an alternative, not an addition.", [
   U(S+'if a message is outgoing, then the App must show its "received time", taken from the "delivery receipt", in the "message details".', "mpiip", "precondition")])),
 ("6362-Signal", ("4 units. The second (encrypted-message count) and fourth (gamification) are softer in the NL (\"Or...\", \"probably also room for...\") - remove if you read them as not requested.", [
   U(S+C+'the App must show a "pie chart" of how many of the user\'s contacts are on Signal.', "mmiip"),
   U(S+C+'the App must show how many of the user\'s sent messages are encrypted.', "mmiip"),
   U(S+'when the user taps an element of the "pie chart", then the App must show the list of people in that category.', "mpiip", "trigger"),
   U(S+'if most but not all of the user\'s contacts use Signal, then the App must display a "progress message" such as "Almost there!".', "mpiip", "precondition")])),
 ("6424-Signal", ("3 units: accept any number format; try the user's calling code; try the device region's calling code.", [
   U(S+'when the user searches for a non-contact by "phone number", then the App must find the number whether it is entered with the "+" prefix, without it, or without the "calling code".', "mpiip", "trigger"),
   U(S+'when the user searches by a "phone number" without a "calling code", then the App must try the "calling code" of the user\'s own phone number.', "mpiip", "trigger"),
   U(S+'when the user searches by a "phone number" without a "calling code", then the App must try the "calling code" of the device\'s default region.', "mpiip", "trigger")])),
 ("7567-Signal", ("2 units: Android profile for regular SMS/MMS; Signal settings for secure messages and calls.", [
   U(S+'if the App is the "default SMS app", then the App must use the "Android notification profile" for regular "SMS/MMS".', "mpiip", "precondition"),
   U(S+'if the App is the "default SMS app", then the App must use the "Signal notification settings" for "secure messages" and "calls".', "mpiip", "precondition")])),
 ("802-Signal", ("6 units: import and export, for the private key and public keys, by file; public keys also by Bluetooth/NFC/QR. Splitting each channel apart would give 10.", [
   U(S+C+'the App must let the user export the "private key" to a "file".', "mmiip"),
   U(S+C+'the App must let the user import the "private key" from a "file".', "mmiip"),
   U(S+C+'the App must let the user export "public keys" to a "file".', "mmiip"),
   U(S+C+'the App must let the user import "public keys" from a "file".', "mmiip"),
   U(S+C+'the App must let the user export "public keys" via "Bluetooth", "NFC", and "QR code".', "mmiip"),
   U(S+C+'the App must let the user import "public keys" via "Bluetooth", "NFC", and "QR code".', "mmiip")])),
 ("5751-Signal", ("2 units: keep a swiped-away notification dismissed; notify on a new message from the same contact.", [
   U(S+'when the user swipes away a "notification", then the App must not display that "notification" again.', "mpiip", "trigger"),
   U(S+'when the App receives a new message from the same contact, then the App must show a new "notification" with the new message.', "mpiip", "trigger")])),
 ("5904-Signal", ("2 units: include the flags when backing up; restore them when importing.", [
   U(S+'when the App creates an ".xml backup", then the App must include the "archive" and "blocked" flags of the messages.', "mpiip", "trigger"),
   U(S+'when the App imports messages from an ".xml backup", then the App must restore the "archive" and "blocked" flags.', "mpiip", "trigger")])),
])

# --- where an annotator wrote a real conversion, their units come from it ----
# (reqId, annotator) -> units in the SAME positions as the canonical ones.
# `None` in a list means "use the canonical unit at this position".
A, AS = "annotator", "annotator-split"
FROM_ANNOTATOR = {
 ("937-Signal", "Arthur"): [U(S+C+'the App must sort the "messages" in the "conversation view" by "sent timestamp", "sequence numbers" or "vector clocks".', "mmiip", prov=A)],
 ("937-Signal", "Mko"):    [U(S+C+'the App must sort the "conversation view" by "sent" time.', "mmiip", prov=A)],
 ("42-Mastodon", "Mko"):   [U(S+C+'the App must provide a view of posts from other people.', "mmiip", prov=A)],
 ("42-Mastodon", "Rafo"):  [U(ALL, "mmmmm", prov=A)],
 ("1692-Signal", "Arthur"): [
   U(S+'when an SMS delivery report is received, then the App must save the time the message was actually received.', "mpiip", "trigger", AS),
   U(S+'when an SMS delivery report is received, then the App must show that time in the message details.', "mpiip", "trigger", AS)],
 ("7345-Signal", "Arthur"): [U(S+'if the "media item" has already been saved, then the App must change the "save icon" to a "view/share icon".', "mpiip", "precondition", A)],
 ("7345-Signal", "Mko"):    [U(S+'if a picture has already been saved, then the App must show a view or share icon instead of the save button.', "mpiip", "precondition", A)],
 ("222-Signal", "Mko"): [None,
   U(S+'when a message from a blocked number is received, then the App must delete it.', "miiip", "trigger", AS),
   U(S+'when a message from a blocked number is received, then the App must not show a notification.', "miiip", "trigger", AS)],
 ("175-Signal", "Arthur"): [U(S+'when the user triggers a "specific button combo" and confirms the "confirm dialogue", then the App must wipe the "text message database".', "mpiip", "trigger", A)],
 ("175-Signal", "Mko"):    [U(S+'if the user is being coerced to disclose their password, then the App must let them quickly wipe the message database.', "mpiip", "precondition", A)],
 ("4821-Signal", "Arthur"): [None,
   U(S+'when the user selects "acknowledge message" after long-pressing a "received message", then the App must replace the "received or seen indicator" with an "indicator for acknowledgement".', "mpiip", "trigger", A)],
 ("4821-Signal", "Mko"): [U(S+C+'the App must let the user acknowledge a received message.', "mmiip", prov=A), None],
 ("2055-Signal", "Mko"): [U(S+'if a message is outgoing, then the App must show its received time in the message details.', "mpiip", "precondition", A)],
 ("6424-Signal", "Mko"): [U(S+C+'the App must find a user by phone number whether or not the calling code is included.', "mmipp", prov=A), None, None],
 ("7567-Signal", "Arthur"): [
   U(S+'if the "App" is the "default SMS manager", then the App must provide an option to use the "Android notification profile" for "regular SMS/MMS".', "mpiip", "precondition", AS),
   U(S+'if the "App" is the "default SMS manager", then the App must provide an option to use the "Signal settings" for "secure messages".', "mpiip", "precondition", AS)],
 ("5751-Signal", "Mko"): [U(S+'when a notification is swiped away, then the App must not show it again.', "mpipp", "trigger", A), None],
 ("5904-Signal", "Arthur"): [None, U(S+'after importing "messages" from an ".xml backup", then the App must preserve the "archive" and "blocked" flags.', "mpiip", "temporal", A)],
 ("5904-Signal", "Mko"):    [None, U(S+'when messages are imported from an .xml backup, then the App must keep the archive and blocked flags.', "mpiii", "trigger", A)],
}


def read_csv(path: Path):
    with path.open(newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        return reader.fieldnames, list(reader)


def with_id(unit: dict) -> dict:
    return {"id": new_unit_id(), **unit}


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--force", action="store_true", help="Overwrite an existing curation.json.")
    args = p.parse_args(argv)
    if STATE_PATH.exists() and not args.force:
        print(f"{STATE_PATH} already exists — refusing to overwrite your curation. Use --force.", file=sys.stderr)
        return 1

    data = config.DATA_DIR
    columns, original = read_csv(data / "gold_annotations_main_atomic.csv")
    _, refined = read_csv(data / "gold_annotations_main_atomic_refined.csv")
    _, full = read_csv(data / "gold_annotations_main.csv")
    edits = {(e["reqId"], e["annotator"]): e for e in json.loads((data / "refinements.json").read_text())["edits"]}
    excluded = [l.split("\t")[0] for l in (data / "gold_annotations_main_atomic_excluded.txt").read_text().splitlines()
                if l.strip() and not l.startswith("#")]

    llm = {}
    for s in ("zsl", "fsl", "cot"):
        d = config.OUTPUTS_DIR / "main30opus" / s / "llm_rimay"
        if d.is_dir():
            for f in d.glob("*.txt"):
                llm.setdefault(f.stem, {})[s] = f.read_text(encoding="utf-8").strip()

    orig_by = {(r["reqId"], r["annotatorUsername"]): r for r in original}
    requirements = []

    # --- the 30 atomic ----------------------------------------------------------
    ref_by = OrderedDict()
    for r in refined:
        ref_by.setdefault(r["reqId"], {})[r["annotatorUsername"]] = r
    for rid, rows in ref_by.items():
        anns = OrderedDict()
        for ann in ANNOTATORS:
            r, o, e = rows[ann], orig_by[(rid, ann)], edits.get((rid, ann), {})
            anns[ann] = {
                "original": o["rimayText"],
                "originalSlots": {k: o[f"slot_{k}"] for k in KEYS},
                "originalNotes": o["notes"],
                "base": {c: r.get(c, "") for c in columns},
                "units": [with_id({
                    "rimayText": r["rimayText"],
                    "slots": {k: r[f"slot_{k}"] for k in KEYS},
                    "conditionType": r["conditionType"] or "none",
                    "provenance": "annotator-refined",
                    "reasons": e.get("reasons", []),
                    "editNote": e.get("note", ""),
                })],
            }
        requirements.append({"reqId": rid, "set": "atomic", "nlText": rows[ANNOTATORS[0]]["nlText"],
                             "llm": llm.get(rid), "decisionNote": "", "annotations": anns})

    # --- the 20 non-atomic ------------------------------------------------------
    full_by = {(r["reqId"], r["annotatorUsername"]): r for r in full if r["annotationStatus"] == "submitted"}
    missing = [rid for rid in excluded if rid not in CANON]
    if missing:
        print(f"No decomposition written for: {missing}", file=sys.stderr)
        return 1
    for rid in excluded:
        note, canon = CANON[rid]
        anns = OrderedDict()
        for ann in ANNOTATORS:
            o = full_by[(rid, ann)]
            own = FROM_ANNOTATOR.get((rid, ann))
            units = [dict(own[i]) if own and i < len(own) and own[i] is not None else dict(canon[i])
                     for i in range(len(canon))]
            anns[ann] = {
                "original": o["rimayText"],
                "originalSlots": {k: o[f"slot_{k}"] for k in KEYS},
                "originalNotes": o["notes"],
                "base": {c: o.get(c, "") for c in columns},
                "units": [with_id(u) for u in units],
            }
        requirements.append({"reqId": rid, "set": "non-atomic", "nlText": full_by[(rid, ANNOTATORS[0])]["nlText"],
                             "llm": None, "decisionNote": note, "annotations": anns})

    state = {
        "about": "Local curation of the human annotations. The database is never modified. Edit in scripts/curate.py; export to data/curated/.",
        "createdAt": now(),
        "annotators": ANNOTATORS,
        "columns": columns,
        "scopeConvention": "Unstated scope is written <MISSING_SCOPE> (slot: missing). Open decision - see the scope discussion.",
        "requirements": requirements,
    }
    save_state(state)
    n_units = sum(len(a["units"]) for r in requirements for a in r["annotations"].values())
    split = [r for r in requirements if r["set"] == "non-atomic"]
    print(f"Wrote {STATE_PATH}")
    print(f"  {len(requirements)} requirements ({len(requirements) - len(split)} atomic, {len(split)} non-atomic)")
    print(f"  non-atomic -> {sum(len(CANON[r['reqId']][1]) for r in split)} units; "
          f"{sum(1 for r in split if len(CANON[r['reqId']][1]) == 1)} judged atomic after all")
    print(f"  {n_units} annotator units in total")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
