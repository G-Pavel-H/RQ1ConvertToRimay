#!/usr/bin/env python3
"""Compare our incompleteness verdicts with Pragyan et al.'s — opens in your browser.

    python scripts/pragyan.py            # then http://127.0.0.1:8766

Shows both verdicts per requirement, Pragyan's missing-information notes and
questions, our gold slots, and experiment 1's model verdicts. You code each of
their questions by what it asks for; edits autosave to
``data/pragyan_coding.json`` (with a ``.bak``). Export gives one CSV row per
question. Local only (127.0.0.1); standard library plus the repo's pandas.
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

from src import pragyan  # noqa: E402

PAGE = ROOT / "templates" / "pragyan.html"


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        if self.command != "GET":
            sys.stderr.write(f"[pragyan] {self.command} {self.path}\n")

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

    def do_GET(self):
        try:
            if self.path in ("/", "/index.html"):
                return self._send(200, PAGE.read_bytes(), "text/html; charset=utf-8")
            if self.path == "/api/data":
                return self._json(pragyan.view())
            if self.path == "/api/export.csv":
                return self._send(200, pragyan.export_csv().encode("utf-8"), "text/csv; charset=utf-8",
                                  {"Content-Disposition": 'attachment; filename="pragyan_comparison.csv"'})
            self._json({"error": "not found"}, 404)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": f"{type(exc).__name__}: {exc}"}, 500)

    def do_PUT(self):
        try:
            if self.path == "/api/coding":
                n = int(self.headers.get("Content-Length") or 0)
                state = json.loads(self.rfile.read(n).decode("utf-8"))
                pragyan.save_coding(state)
                return self._json({"savedAt": state["savedAt"]})
            self._json({"error": "not found"}, 404)
        except ValueError as exc:
            self._json({"error": str(exc)}, 400)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": f"{type(exc).__name__}: {exc}"}, 500)


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--port", type=int, default=8766)
    p.add_argument("--no-browser", action="store_true")
    args = p.parse_args(argv)
    pragyan.load_coding()  # creates it from CL's first pass on the first run
    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    url = f"http://127.0.0.1:{args.port}"
    print(f"Pragyan comparison: {url}   (coding in {pragyan.CODING_PATH.relative_to(ROOT)}; Ctrl+C to stop)")
    if not args.no_browser:
        threading.Timer(0.6, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
