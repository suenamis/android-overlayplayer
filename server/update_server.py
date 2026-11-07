"""Read-only Update-Dienst des Overlay-Players; nur Loopback, HTTPS davor über Tailscale."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import html
import json
import os
import re
import urllib.parse

APK = re.compile(r'/overlay-player-[0-9]+\.apk')
FILES = '/dateien/'
TYPES = {'.mp4': 'video/mp4', '.mp3': 'audio/mpeg', '.m4a': 'audio/mp4', '.wav': 'audio/wav'}


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
        if self.path == FILES:
            self.file_list()
            return
        if self.path.startswith(FILES):
            self.shared_file(urllib.parse.unquote(self.path[len(FILES):]))
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

    def shared_names(self):
        folder = self.server.files
        if not folder.is_dir():
            return []
        return sorted(p.name for p in folder.iterdir()
                      if p.is_file() and not p.is_symlink() and p.suffix.lower() in TYPES)

    def file_list(self):
        rows = ''.join('<li><a href="' + urllib.parse.quote(name) + '" download>' + html.escape(name) + '</a> ('
                       + str(round((self.server.files / name).stat().st_size / 1e6)) + ' MB)</li>'
                       for name in self.shared_names())
        body = ('<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width">'
                '<title>Overlay-Player: Dateien</title><h1>Dateien zum Herunterladen</h1>'
                '<ul>' + (rows or '<li>Keine Dateien bereitgestellt.</li>') + '</ul>'
                '<p>Nach dem Herunterladen liegen sie im Ordner „Download“ und lassen sich im Overlay-Player hinzufügen.</p></html>')
        self.reply(body.encode(), 'text/html; charset=utf-8')

    def shared_file(self, name):
        # Nur Namen aus der Liste: Kein Pfad, kein Ausweg aus dem Ordner.
        if name not in self.shared_names():
            self.send_error(404)
            return
        path = self.server.files / name
        with path.open('rb') as source:
            size = os.fstat(source.fileno()).st_size
            start, end = 0, size - 1
            match = re.fullmatch(r'bytes=(\d*)-(\d*)', self.headers.get('Range', ''))
            if match and (match.group(1) or match.group(2)):
                if match.group(1):
                    start = int(match.group(1))
                    end = min(int(match.group(2)), size - 1) if match.group(2) else size - 1
                else:
                    start = max(0, size - int(match.group(2)))
                if start > end:
                    self.send_response(416)
                    self.send_header('Content-Range', f'bytes */{size}')
                    self.end_headers()
                    return
                self.send_response(206)
                self.send_header('Content-Range', f'bytes {start}-{end}/{size}')
            else:
                self.send_response(200)
            self.send_header('Content-Type', TYPES[path.suffix.lower()])
            self.send_header('Content-Length', str(end - start + 1))
            self.send_header('Accept-Ranges', 'bytes')
            self.send_header('Content-Disposition', "attachment; filename*=UTF-8''" + urllib.parse.quote(name))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.end_headers()
            if self.command == 'HEAD':
                return
            source.seek(start)
            left = end - start + 1
            while left > 0 and (data := source.read(min(65536, left))):
                self.wfile.write(data)
                left -= len(data)

    def page(self):
        try:
            release = json.loads((self.server.root / 'latest.json').read_text(encoding='utf-8'))
            apk = release['path'][1:]
            if not APK.fullmatch(release['path']):
                raise ValueError('Invalid release path')
            body = ('<!doctype html><html lang="de"><meta charset="utf-8"><meta name="viewport" content="width=device-width">'
                    '<title>Overlay-Player</title><h1>Overlay-Player</h1><p>Version ' + html.escape(release['versionName']) + '</p>'
                    '<p><a href="' + apk + '">Android-App installieren</a></p>'
                    '<p>Spätere Updates bietet die App selbst an.</p>'
                    '<p><a href="dateien/">Dateien zum Herunterladen</a></p></html>')
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
    server.files = Path(os.environ.get('FILES_DIR', Path(root).parent / 'dateien'))
    return server


if __name__ == '__main__':
    make_server(os.environ.get('RELEASE_DIR', '/srv/overlay-player/releases'), int(os.environ.get('PORT', '5240'))).serve_forever()
