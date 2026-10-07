#!/usr/bin/env python3
"""Local curation tool for the human annotations — opens in your browser.

    python scripts/curate.py            # then http://127.0.0.1:8765

Edits ``data/curation.json`` directly (autosaved, with a ``.bak`` of the previous
version). Nothing touches the database. From the page you can also:

  * run Paska over every annotator unit (only changed texts are re-checked);
  * export the curated corpus to ``data/curated/`` as CSV.

Local only: it binds to 127.0.0.1 and serves nothing but this repo's curation.
Standard library only.
"""
from __future__ import annotations

import argparse
import json
import sys
import threading
import traceback
import webbrowser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from src import curation  # noqa: E402

PAGE = ROOT / "templates" / "curation.html"
DOWNLOADABLE = {"gold_atomic.csv", "gold_units.csv", "gold_all_units.csv", "paska_annotations.json", "agreement.json"}

# One Paska run at a time: it is slow and writes a shared results file.
_paska_lock = threading.Lock()


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):  # keep the console readable
        if "/api/" in (self.path or "") and self.command != "GET":
            sys.stderr.write(f"[curate] {self.command} {self.path}\n")

    def _send(self, code: int, body: bytes, ctype: str, extra: dict | None = None) -> None:
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        for k, v in (extra or {}).items():
            self.send_header(k, v)
        self.end_headers()
        self.wfile.write(body)

    def _json(self, obj, code: int = 200) -> None:
        self._send(code, json.dumps(obj, ensure_ascii=False).encode("utf-8"), "application/json; charset=utf-8")

    def _body(self):
        n = int(self.headers.get("Content-Length") or 0)
        return json.loads(self.rfile.read(n).decode("utf-8")) if n else None

    def do_GET(self):
        try:
            if self.path in ("/", "/index.html"):
                return self._send(200, PAGE.read_bytes(), "text/html; charset=utf-8")
            if self.path == "/api/state":
                paska = None
                if curation.PASKA_PATH.exists():
                    full = json.loads(curation.PASKA_PATH.read_text(encoding="utf-8"))
                    paska = {k: full[k] for k in ("ranAt", "byUnit", "summary") if k in full}
                agreement = None
                if curation.AGREEMENT_PATH.exists():
                    agreement = json.loads(curation.AGREEMENT_PATH.read_text(encoding="utf-8"))
                return self._json({"state": curation.load_state(), "paska": paska, "agreement": agreement})
            if self.path.startswith("/download/"):
                name = self.path.rsplit("/", 1)[-1]
                path = curation.EXPORT_DIR / name
                if name not in DOWNLOADABLE or not path.is_file():
                    return self._json({"error": "not found"}, 404)
                ctype = "text/csv; charset=utf-8" if name.endswith(".csv") else "application/json"
                return self._send(200, path.read_bytes(), ctype,
                                  {"Content-Disposition": f'attachment; filename="{name}"'})
            self._json({"error": "not found"}, 404)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": f"{type(exc).__name__}: {exc}"}, 500)

    def do_PUT(self):
        try:
            if self.path == "/api/state":
                state = self._body()
                curation.save_state(state)
                return self._json({"savedAt": state["savedAt"]})
            self._json({"error": "not found"}, 404)
        except ValueError as exc:
            self._json({"error": str(exc)}, 400)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": f"{type(exc).__name__}: {exc}"}, 500)

    def do_POST(self):
        try:
            if self.path == "/api/export":
                return self._json(curation.export(curation.load_state()))
            if self.path == "/api/agreement":
                return self._json(curation.agreement_report(curation.load_state()))
            if self.path == "/api/paska":
                if not _paska_lock.acquire(blocking=False):
                    return self._json({"error": "A Paska run is already in progress."}, 409)
                try:
                    out = curation.run_paska_on_units(curation.load_state())
                    return self._json({k: out[k] for k in ("ranAt", "byUnit", "summary")})
                finally:
                    _paska_lock.release()
            self._json({"error": "not found"}, 404)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": f"{type(exc).__name__}: {exc}"}, 500)


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--port", type=int, default=8765)
    p.add_argument("--no-browser", action="store_true")
    args = p.parse_args(argv)

    if not curation.STATE_PATH.exists():
        print(f"No {curation.STATE_PATH} yet — run scripts/build_curation.py first.", file=sys.stderr)
        return 1

    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    url = f"http://127.0.0.1:{args.port}"
    shown = curation.STATE_PATH
    if shown.is_relative_to(ROOT):
        shown = shown.relative_to(ROOT)
    print(f"Curation tool: {url}   (editing {shown}; Ctrl+C to stop)")
    if not args.no_browser:
        threading.Timer(0.6, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
