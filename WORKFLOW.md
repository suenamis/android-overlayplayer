# Overlay-Player: Ablauf einer Veröffentlichung

1. Aufgabe wörtlich in `D:\Programme\OverlayPlayer\FEATURES.md` notieren.
2. Eigener Zweig `feature/<kurzname>`; `main` bleibt unberührt.
3. Bauen und auf dem Emulator prüfen.
4. Zusammenführen mit `git merge --no-ff`, Zweig löschen, Tag `v<version>-<name>-stable` setzen.
5. `scripts\build-release.ps1` (Tests, Lint, Signaturprüfung, Snapshot).
6. `scripts\deploy.ps1` nur auf Sues Ansage.

Einzelheiten: `D:\Programme\OverlayPlayer\docs\DEV-WORKFLOW.md`,
`D:\Programme\OverlayPlayer\docs\DEPLOYMENT.md`.
