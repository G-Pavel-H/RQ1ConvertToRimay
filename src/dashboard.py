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
import sqlite3
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
            "runs": runs, "batchVerdicts": verdicts, "prompts": collect_prompts()}


_EXP_PREFIX = {"exp1_atomic": 1, "exp2_units": 2, "exp3_blind": 3}


def collect_prompts() -> dict:
    """The prompts each experiment sent, read back from MLflow's per-requirement artifacts.

    {exp: {strategy: {"system": str, "users": {reqId: str}, "from": batch}}}. One
    model's batch per experiment is enough: the prompts do not depend on the
    model. The latest MLflow run per requirement wins, so prompts from older,
    archived conversions (same batch name) are ignored.
    """
    db = config.MLFLOW_TRACKING_DB
    if not db.exists():
        return {}
    con = sqlite3.connect(f"file:{db}?mode=ro", uri=True)
    try:
        rows = con.execute(
            """select r.artifact_uri, r.start_time, o.value, q.value from runs r
               join tags o on o.run_uuid = r.run_uuid and o.key = 'output_run_id'
               join tags q on q.run_uuid = r.run_uuid and q.key = 'req_id'""").fetchall()
    finally:
        con.close()
    latest = {}
    for uri, start, out, req in rows:
        if (out, req) not in latest or start > latest[(out, req)][0]:
            latest[(out, req)] = (start, uri)
    prompts: dict = {}
    for (out, req), (start, uri) in sorted(latest.items()):
        batch, _, strat = out.partition("/")
        prefix, _, model = batch.rpartition("_")
        exp = _EXP_PREFIX.get(prefix)
        if not exp or model != "haiku":
            continue
        art = Path(uri.replace("file://", ""))
        sys_p, user_p = art / "prompt_system.md", art / "prompt_user.md"
        if not (sys_p.exists() and user_p.exists()):
            continue
        slot = prompts.setdefault(str(exp), {}).setdefault(strat, {"system": "", "users": {}, "from": out})
        slot["system"] = slot["system"] or sys_p.read_text(encoding="utf-8")
        slot["users"][req] = user_p.read_text(encoding="utf-8")
    return prompts


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
