"""Render the scoring results into one self-contained HTML report.

Every scored run writes ``scoring/results.json`` (see
``scripts/run_scoring.py``). This module collects those files and injects
them into ``templates/report.html``, which is a static page that renders
the data client-side — no server, no build step, no dependencies. Open
the output file in a browser.

The template also renders a run's ``verdict`` field when it is non-null,
which is where the future LLM analysis stage will write its conclusion.
"""
from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path
from typing import List, Optional

from src import config

_DATA_PLACEHOLDER = "__RESULTS_DATA__"


def collect_results(outputs_dir: Optional[Path] = None) -> List[dict]:
    """Every ``scoring/results.json`` under ``outputs/``, sorted by run path."""
    root = Path(outputs_dir) if outputs_dir else config.OUTPUTS_DIR
    results = []
    for path in sorted(root.rglob("scoring/results.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        # Trust the folder over the recorded run id: a run folder stays
        # readable after it is renamed or copied elsewhere.
        data["run"] = path.parent.parent.relative_to(root).as_posix()
        results.append(data)
    return results


def render_report(results: List[dict], template_path: Optional[Path] = None) -> str:
    """Inject the collected results into the HTML template."""
    template = (template_path or config.REPORT_TEMPLATE).read_text(encoding="utf-8")
    payload = {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "runs": results,
    }
    # `</` would close the inline <script> early; escaping it keeps the JSON valid.
    data = json.dumps(payload, ensure_ascii=False).replace("</", "<\\/")
    return template.replace(_DATA_PLACEHOLDER, data)
