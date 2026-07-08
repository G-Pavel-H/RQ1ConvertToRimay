"""Pre-flight check for the NL -> Rimay -> Paska pipeline.

Reports pass/fail for each prerequisite and exits non-zero if any
required check fails.

Required:
  - macOS or Linux (stanza's deps are not set up for Windows here)
  - Java 1.8 on PATH (smell_detector.jar)
  - Python deps: anthropic, mlflow, pandas, dotenv, stanza
  - Stanza English constituency model downloaded
  - paska/smell_detector.jar present
  - PASKA_POS_TAGGER_PATH points to an existing tagger file
  - gold_annotations.csv present

Warned (not failed):
  - ANTHROPIC_API_KEY missing (needed at run time, not at verify time)
"""
from __future__ import annotations

import importlib
import os
import platform
import re
import shutil
import subprocess
import sys
from pathlib import Path

THIS_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(THIS_DIR.parent))

from src import config  # noqa: E402

PASS = "PASS"
FAIL = "FAIL"
WARN = "WARN"

POS_TAGGER_DOWNLOAD_URL = "https://nlp.stanford.edu/software/tagger.shtml#Download"


def _print(status: str, label: str, detail: str = "") -> None:
    pad = f"[{status}]".ljust(7)
    print(f"{pad} {label}" + (f" — {detail}" if detail else ""))


def check_platform() -> bool:
    system = platform.system()
    if system in {"Darwin", "Linux"}:
        _print(PASS, "Platform", f"{system} (stanza-compatible)")
        return True
    _print(
        FAIL,
        "Platform",
        f"{system}: stanza's dependencies are not configured for Windows here; "
        "run on macOS or Linux (or WSL).",
    )
    return False


def check_java() -> bool:
    java = shutil.which("java")
    if not java:
        _print(FAIL, "Java runtime", "`java` not on PATH; install Java 1.8")
        return False
    try:
        out = subprocess.run(
            ["java", "-version"], capture_output=True, text=True, timeout=10
        )
    except (subprocess.SubprocessError, OSError) as exc:
        _print(FAIL, "Java runtime", f"could not invoke java: {exc}")
        return False
    text = (out.stderr or "") + (out.stdout or "")
    match = re.search(r'version "(?:1\.)?(\d+)', text)
    if not match:
        _print(WARN, "Java runtime", f"unparseable version string: {text!r}")
        return True
    major = int(match.group(1))
    if major == 8 or "1.8" in text:
        _print(PASS, "Java 1.8", text.splitlines()[0].strip())
        return True
    _print(
        WARN,
        "Java version",
        f"detected major version {major}; Paska expects Java 1.8 — may still run",
    )
    return True


def check_python_deps() -> bool:
    required = ["anthropic", "mlflow", "pandas", "dotenv", "stanza"]
    missing = []
    for name in required:
        try:
            importlib.import_module(name)
        except ImportError:
            missing.append(name)
    if missing:
        _print(
            FAIL,
            "Python dependencies",
            f"missing: {', '.join(missing)} — run `pip install -r requirements.txt`",
        )
        return False
    _print(PASS, "Python dependencies", ", ".join(required))
    return True


def check_stanza_model() -> bool:
    """Confirm the English constituency model is downloaded locally."""
    try:
        import stanza  # noqa: F401
        from stanza.resources.common import DEFAULT_MODEL_DIR
    except ImportError:
        _print(FAIL, "Stanza model", "stanza not installed (see Python deps check)")
        return False
    home = Path(os.environ.get("STANZA_RESOURCES_DIR") or DEFAULT_MODEL_DIR)
    constituency_dir = home / "en" / "constituency"
    if not constituency_dir.is_dir() or not any(constituency_dir.iterdir()):
        _print(
            FAIL,
            "Stanza English constituency model",
            f"not found under {home / 'en'}. Run: "
            "python -c \"import stanza; stanza.download('en', "
            "processors='tokenize,pos,constituency')\"",
        )
        return False
    _print(PASS, "Stanza English constituency model", str(constituency_dir))
    return True


def check_paska_files() -> bool:
    if not config.PASKA_JAR.is_file():
        _print(FAIL, "Paska JAR", f"missing: {config.PASKA_JAR}")
        return False
    _print(PASS, "Paska JAR", str(config.PASKA_JAR.relative_to(config.PROJECT_ROOT)))
    return True


def check_pos_tagger() -> bool:
    path = config.PASKA_POS_TAGGER_PATH
    if not path:
        _print(
            FAIL,
            "Stanford POS tagger",
            f"PASKA_POS_TAGGER_PATH not set. Download "
            f"english-left3words-distsim.tagger from {POS_TAGGER_DOWNLOAD_URL} "
            f"and set the env var to its absolute path.",
        )
        return False
    p = Path(path)
    if not p.exists():
        _print(
            FAIL,
            "Stanford POS tagger",
            f"path does not exist: {path}. Download from {POS_TAGGER_DOWNLOAD_URL}.",
        )
        return False
    _print(PASS, "Stanford POS tagger", str(p))
    return True


def check_dataset() -> bool:
    if not config.GOLD_CSV.is_file():
        _print(FAIL, "Gold annotations CSV", f"missing: {config.GOLD_CSV}")
        return False
    _print(
        PASS,
        "Gold annotations CSV",
        str(config.GOLD_CSV.relative_to(config.PROJECT_ROOT)),
    )
    return True


def check_api_key() -> None:
    if not os.environ.get("ANTHROPIC_API_KEY"):
        _print(
            WARN,
            "ANTHROPIC_API_KEY",
            "not set in environment / .env (required to run the LLM step)",
        )
    else:
        _print(PASS, "ANTHROPIC_API_KEY", "present")


def main() -> int:
    print("Pre-flight checks for NL -> Rimay -> Paska pipeline")
    print("=" * 60)
    results = [
        check_platform(),
        check_java(),
        check_python_deps(),
        check_stanza_model(),
        check_paska_files(),
        check_dataset(),
        check_pos_tagger(),
    ]
    check_api_key()
    print("=" * 60)
    if all(results):
        print("All required checks passed.")
        return 0
    print("One or more required checks failed. See [FAIL] lines above.")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
