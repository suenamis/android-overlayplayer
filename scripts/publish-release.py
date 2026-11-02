"""Prüft eine Veröffentlichung und stellt sie atomar bereit (Muster: Airhold)."""
import hashlib
import json
import os
import re
import sys
from pathlib import Path


def atomic_write(path, data):
    pending = path.with_suffix(path.suffix + '.pending')
    with pending.open('wb') as out:
        out.write(data)
        out.flush()
        os.fsync(out.fileno())
    os.chmod(pending, 0o644)
    os.replace(pending, path)


def publish(source, destination):
    source, destination = Path(source), Path(destination)
    manifest = json.loads((source / 'latest.json').read_text(encoding='utf-8-sig'))
    version = manifest.get('versionCode')
    if type(version) is not int or version < 1 or manifest.get('path') != f'/overlay-player-{version}.apk':
        raise ValueError('Invalid version or artifact path')
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', str(manifest.get('versionName'))):
        raise ValueError('Invalid version name')
    apk = source / manifest['path'][1:]
    content = apk.read_bytes()
    if len(content) != manifest.get('size') or hashlib.sha256(content).hexdigest() != manifest.get('sha256'):
        raise ValueError('APK size/hash mismatch')
    current = destination / 'latest.json'
    if current.exists() and json.loads(current.read_text(encoding='utf-8'))['versionCode'] > version:
        raise ValueError('Refusing release downgrade')
    target = destination / apk.name
    if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest() != manifest['sha256']:
        raise ValueError('Published version is immutable; increment versionCode')
    destination.mkdir(parents=True, exist_ok=True)
    if not target.exists():
        atomic_write(target, content)
    atomic_write(current, json.dumps(manifest, ensure_ascii=False, indent=2).encode())
    return version


if __name__ == '__main__':
    print('Veröffentlicht: versionCode', publish(sys.argv[1], sys.argv[2]))
