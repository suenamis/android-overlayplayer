#!/usr/bin/env node
// Holt die APK eines Snapshots nach artifacts/ zurück; der aktuelle Stand bleibt als .before-restore.
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const name = process.argv[2];
const source = name && path.join(root, 'dist', 'snapshots', name);
if (!source || !fs.existsSync(source)) {
  console.error('Aufruf: node scripts/restore-snapshot.js <snapshot-name>');
  process.exit(1);
}
fs.mkdirSync(path.join(root, 'artifacts'), { recursive: true });
for (const file of fs.readdirSync(source).filter((entry) => entry.endsWith('.apk'))) {
  const target = path.join(root, 'artifacts', file);
  if (fs.existsSync(target)) fs.copyFileSync(target, `${target}.before-restore`);
  fs.copyFileSync(path.join(source, file), target);
  console.log(`Wiederhergestellt: ${target}`);
}
