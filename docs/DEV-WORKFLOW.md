# Entwicklung: die Begriffe und Befehle

- **Zweig (Branch):** eine abgetrennte Arbeitskopie im Git. Geht ein Versuch schief, bleibt `main` heil.
- **Tag:** ein fester Name für einen stabilen Stand, jederzeit mit `git checkout <tag>` erreichbar.
- **Snapshot:** Kopie der gebauten APK mit Angaben in `dist/snapshots/<datum>_<hash>/`, höchstens zehn.
  - Anzeigen: `node scripts/list-snapshots.js`
  - Zurückholen: `node scripts/restore-snapshot.js <name>` (der aktuelle Stand bleibt als `.before-restore`)
- **Prüfung vor dem Bau:** `scripts/build-release.ps1` baut nichts Fertiges, wenn Tests oder Lint scheitern.
- **Diagnose:** Fünf Sekunden nach dem Start schreibt die App
  `Android/data/com.example.overlay_player/files/Diagnose/diagnose.txt` auf das Handy.
  Sie enthält Version, Gerät, Berechtigungen und Anzahlen, keine Dateinamen.
- **Icon:** `python scripts/make-icon.py` schreibt alle Fassungen aus einer Quelle.
- **Testfassung:** `assembleUpdateTest` sucht Updates unter `http://10.0.2.2:5299` und nur dort;
  diese Fassung ist nie für ein echtes Gerät gedacht.
