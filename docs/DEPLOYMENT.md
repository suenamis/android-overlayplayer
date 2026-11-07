# Overlay-Player: Updates auf bums

## Zugang

- Adresse: `https://bums.tail1c1957.ts.net:5241/` (nur im Tailnet, kein Funnel)
- Versionsangabe: `/latest.json`, App: `/overlay-player-<versionCode>.apk`
- Dienst: `overlay-player-updates.service`, Nutzer `overlay-player`, nur `127.0.0.1:5240`
- Code: `/srv/overlay-player/server/update_server.py`, Releases: `/srv/overlay-player/releases/`
- Muster: Airhold (Prüfregeln) und `rezeptapp-updates` (Lese-Dienst)

## Neue Version veröffentlichen

1. In `D:\Programme\OverlayPlayer\app\build.gradle.kts` `versionCode` erhöhen und `versionName` ändern.
2. `powershell -File D:\Programme\OverlayPlayer\scripts\build-release.ps1 -Notes "Was neu ist"`
   führt Tests und Lint aus, prüft den Signaturschlüssel und legt APK, `latest.json` und einen
   Snapshot an.
3. `powershell -File D:\Programme\OverlayPlayer\scripts\deploy.ps1` überträgt und veröffentlicht.
   ⚠ Nur auf Sues Ansage.
4. In der App „Auf Updates prüfen“. Beim Öffnen sucht sie von selbst, höchstens alle 15 Minuten.

## Dateien fürs Handy bereitstellen

`powershell -File D:\Programme\OverlayPlayer\scripts\dateien-bereitstellen.ps1 -Files "<Pfad1>","<Pfad2>"`
legt Audio- und Videodateien nach `/srv/overlay-player/dateien/`. Sie erscheinen unter
`https://bums.tail1c1957.ts.net:5241/dateien/` (nur Tailnet) zum Herunterladen. Ausgeliefert
werden nur `.mp4`, `.mp3`, `.m4a` und `.wav` aus diesem Ordner.

## Was die App prüft

Nur HTTPS auf dem fest eingebauten Host, keine Weiterleitungen, nur der vorgesehene APK-Pfad,
SHA-256 und Byteanzahl, Paketname, höherer Versionscode, derselbe Signierer. Die Installation
bestätigt Android; beim ersten Mal fragt Android nach der Erlaubnis für diese Quelle.

## Signatur

`E:\OneDrive\Dokumente\AppSchlüssel\overlay-player-debug.keystore`, erwarteter Fingerabdruck in
`E:\OneDrive\Dokumente\AppSchlüssel\overlay-player-certificate-sha256.txt`. Fehlt die Datei oder
passt der Fingerabdruck nicht, bricht der Bau ab.

## Rückbau

```bash
sudo systemctl disable --now overlay-player-updates
sudo tailscale serve --https=5241 off
sudo rm /etc/bums-panel/apps.d/overlay-player.json
```

`/srv/overlay-player/releases` aufbewahren. Andere Serve-Zuordnungen nicht ändern.
