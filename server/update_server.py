"""Read-only Update-Dienst des Overlay-Players; nur Loopback, HTTPS davor über Tailscale."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import html
import json
import os
import re

APK = re.compile(r'/overlay-player-[0-9]+\.apk')


class Handler(BaseHTTPRequestHandler):
    def setup(self):
        super().setup()
        self.connection.settimeout(30)

    def log_message(self, fmt, *args):
        pass

    def do_HEAD(self):
        self.do_GET()

    def do_GET(self):
        if self.path == '/health':
            self.reply(b'{"status":"ok"}', 'application/json')
            return
        if self.path == '/':
            self.page()
            return
        if self.path != '/latest.json' and not APK.fullmatch(self.path):
            self.send_error(404)
            return
        path = self.server.root / self.path[1:]
        if path.is_symlink() or not path.is_file():
            self.send_error(404)
            return
        is_manifest = self.path.endswith('.json')
        # Geöffnet bleiben: latest.json wird atomar ersetzt und zuletzt veröffentlicht.
        with path.open('rb') as source:
            self.send_response(200)
            self.send_header('Content-Type', 'application/json' if is_manifest else 'application/vnd.android.package-archive')
            self.send_header('Content-Length', str(os.fstat(source.fileno()).st_size))
            self.send_header('Cache-Control', 'no-store' if is_manifest else 'public, max-age=31536000, immutable')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.end_headers()
            if self.command != 'HEAD':
                while data := source.read(65536):
                    self.wfile.write(data)

    def page(self):
        try:
            release = json.loads((self.server.root / 'latest.json').read_text(encoding='utf-8'))
            apk = release['path'][1:]
            if not APK.fullmatch(release['path']):
                raise ValueError('Invalid release path')
            body = ('<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width">'
                    '<title>Overlay-Player</title><h1>Overlay-Player</h1><p>Version ' + html.escape(release['versionName']) + '</p>'
                    '<p><a href="' + apk + '">Android-App installieren</a></p>'
                    '<p>Spätere Updates bietet die App selbst an.</p></html>')
            self.reply(body.encode(), 'text/html; charset=utf-8')
        except (OSError, ValueError, KeyError):
            self.send_error(503, 'No release published')

    def reply(self, body, content_type):
        self.send_response(200)
        self.send_header('Content-Type', content_type)
        self.send_header('Content-Length', str(len(body)))
        self.send_header('Cache-Control', 'no-store')
        self.end_headers()
        if self.command != 'HEAD':
            self.wfile.write(body)


def make_server(root, port):
    server = ThreadingHTTPServer((os.environ.get('BIND', '127.0.0.1'), port), Handler)
    server.root = Path(root)
    return server


if __name__ == '__main__':
    make_server(os.environ.get('RELEASE_DIR', '/srv/overlay-player/releases'), int(os.environ.get('PORT', '5240'))).serve_forever()
