#!/usr/bin/env node
// Listet die vorhandenen Snapshots mit ihren Angaben.
const fs = require('fs');
const path = require('path');

const snapshots = path.resolve(__dirname, '..', 'dist', 'snapshots');
if (!fs.existsSync(snapshots)) {
  console.log('Noch keine Snapshots.');
  process.exit(0);
}
for (const name of fs.readdirSync(snapshots).sort().reverse()) {
  console.log(`\n${name}`);
  console.log(fs.readFileSync(path.join(snapshots, name, 'MANIFEST.txt'), 'utf8').trim());
}
