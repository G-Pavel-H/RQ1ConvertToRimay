"""The experiment dashboard: every scored run in one self-contained HTML page.

Collects each ``outputs/<batch>/<strategy>/scoring/results.json`` and each
batch's ``verdict.md``, and injects them into ``templates/dashboard.html``,
which renders everything client-side (no server, no dependencies). Folders
whose name starts with ``_`` (logs, caches, smoke tests) are ignored.

Rebuilt by ``scripts/run_scoring.py`` after every scoring and by
``scripts/build_dashboard.py`` on demand.
"""
from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path
from typing import Optional

from src import config

_DATA_PLACEHOLDER = "__DASHBOARD_DATA__"


def collect(outputs_dir: Optional[Path] = None) -> dict:
    root = Path(outputs_dir) if outputs_dir else config.OUTPUTS_DIR
    runs = []
    for path in sorted(root.rglob("scoring/results.json")):
        rel = path.parent.parent.relative_to(root)
        if any(part.startswith("_") for part in rel.parts):
            continue
        data = json.loads(path.read_text(encoding="utf-8"))
        # Trust the folder over the recorded id: a run stays readable after a rename.
        data["run"] = rel.as_posix()
        runs.append(data)
    verdicts = {}
    for path in sorted(root.glob("*/verdict.md")):
        if not path.parent.name.startswith("_"):
            verdicts[path.parent.name] = path.read_text(encoding="utf-8")
    return {"generatedAt": datetime.now().astimezone().isoformat(timespec="seconds"),
            "runs": runs, "batchVerdicts": verdicts}


def render(payload: dict, template_path: Optional[Path] = None) -> str:
    template = (template_path or config.DASHBOARD_TEMPLATE).read_text(encoding="utf-8")
    # `</` would close the inline <script> early; escaping it keeps the JSON valid.
    data = json.dumps(payload, ensure_ascii=False).replace("</", "<\\/")
    return template.replace(_DATA_PLACEHOLDER, data)


def build(out: Optional[Path] = None, outputs_dir: Optional[Path] = None) -> Path:
    out = Path(out) if out else config.DASHBOARD_HTML
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(render(collect(outputs_dir)), encoding="utf-8")
    return out
