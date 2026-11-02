#!/usr/bin/env node
// Legt nach einem Release-Bau einen Snapshot an: APK + MANIFEST.txt, höchstens 10 Stück.
const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const snapshots = path.join(root, 'dist', 'snapshots');
const apk = process.argv[2];
if (!apk || !fs.existsSync(apk)) {
  console.error('Aufruf: node scripts/snapshot-build.js <apk>');
  process.exit(1);
}

const git = (command) => execSync(`git ${command}`, { cwd: root, encoding: 'utf8' }).trim();
const hash = git('rev-parse --short HEAD');
const dirty = git('status --porcelain') !== '';
const stamp = new Date().toISOString().slice(0, 16).replace(/[-:T]/g, '');
const target = path.join(snapshots, `${stamp}_${hash}${dirty ? '_dirty' : ''}`);

fs.mkdirSync(target, { recursive: true });
fs.copyFileSync(apk, path.join(target, path.basename(apk)));
fs.writeFileSync(
  path.join(target, 'MANIFEST.txt'),
  `Datum: ${new Date().toISOString()}\nGit: ${hash}\nArbeitsbaum: ${dirty ? 'geändert' : 'sauber'}\nDatei: ${path.basename(apk)}\n`
);

// Die Rotation entfernt nur einzeln benannte Snapshot-Ordner innerhalb von dist/snapshots.
const all = fs.readdirSync(snapshots).sort();
for (const old of all.slice(0, Math.max(0, all.length - 10))) {
  fs.rmSync(path.join(snapshots, old), { recursive: true });
}
console.log(`Snapshot: ${target}`);
