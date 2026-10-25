from src.llm_converter import parse_answer
from src.scoring import anomalies as A


def test_last_block_and_dropped_commentary():
    lines, fmt = parse_answer("reasoning\n<rimay>\nthe App must A.\n</rimay>\nWait, correcting:\n"
                              "<rimay>\n1. the App must B.\nCorrected output:\n</rimay>")
    assert lines == ["the App must B."]
    assert fmt["block"] and fmt["closed"] and fmt["dropped"] == ["Corrected output:"]


def test_no_block_falls_back_to_whole_response():
    lines, fmt = parse_answer("Rimay requirements:\nthe App must A.\nthe App must B.")
    assert lines == ["the App must A.", "the App must B."] and not fmt["block"]


def test_anomalies_blind_split_and_outside_text():
    manifest = {"r1": {"units": [{"rimay": "x"}], "format": {"block": True, "closed": True, "before": 50,
                                                             "outside": 0, "dropped": [], "lines": 2}}}
    units = {"perRequirement": [{"reqId": "r1", "expected": 1, "returned": 2}]}
    a = A.find(manifest, task="blind", strategy="zsl", units_info=units)
    assert a["counts"] == {"text_outside": 1, "split_atomic": 1} and a["affected"] == 1
    # the same text before the block is allowed for CoT
    assert "text_outside" not in A.find(manifest, task="blind", strategy="cot", units_info=units)["counts"]
