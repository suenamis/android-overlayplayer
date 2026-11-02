# Overlay Player für Android — Aufgabenliste

**Regel:** Jede Aufgabe von Sue kommt ZUERST wörtlich mit Datum hierher, dann wird sie
bearbeitet, dann abgehakt. Diese Liste ist Sues Prüfstein, nicht Claudes Notizzettel.

## Übersicht

| Aufgabe | Datum | Stand |
|---|---|---|
| Projekt nach `D:\Programme` verschieben | 2026-11-02 | 🟡 Kopie fertig und geprüft, altes Original wartet auf Sues Freigabe |
| Automatische Update-Funktion wie bei Airhold | 2026-11-02 | 🟡 gebaut und getestet, Server wartet auf Sues Ansage zum Ausrollen |
| Geschlossene Overlay-Datei bleibt in der Anzeige | 2026-11-02 | ✅ erledigt |
| App-Icon | 2026-11-02 | 🟡 gebaut, Aussehen wartet auf Sues Urteil |
| Overlay frei auf dem Bildschirm verschieben | 2026-11-02 | ✅ erledigt |

## Aufgaben

### ✅ Overlay verschieben (Sue, 2026-11-02)

Wörtlich: „Außerdem wäre es cool, wenn ich das Overlay auf dem Bildschirm verschieben könnte,
damit ich auch an Sachen dran komme, die an der Stelle sind, wo gerade der Overlay-Player ist."

Vorher: nur auf und ab, an einem 4 dp dünnen Griff, immer bildschirmbreit. Jetzt: Fenster mit
höchstens 340 dp Breite, in alle Richtungen ziehbar an Griff, Titel und allen freien Flächen.
Es bleibt ganz auf dem Bildschirm und merkt sich seine Position.
Belege (Android 10): Rahmen verschoben von y 1753 auf 1057; Ziehen über den Rand endet bei
(188, 1879, 1080, 2274) auf 1080 × 2400; Position nach Stop und Neustart unverändert.

### 🟡 Automatische Update-Funktion, geschlossene Datei bleibt, App-Icon (Sue, 2026-11-02)

Wörtlich: „Dann bauen wir eine automatische Update-Funktion ein, so wie bei der App Airold.
Und außerdem lösen wir das Problem, dass, wenn ich eine Overlay-Datei schließe, sie trotzdem
nicht aus dem Overlay-Player verschwindet und ich sie nicht wieder als Audiodateien hinzufügen
muss, sondern dass sie dort bleibt. Momentan verschwindet sie nämlich immer wieder aus der
Anzeige. Wenn ich sie dann neu lade, spielt sie trotzdem an der Stelle weiter, wo sie
aufgehört hat, aber ich finde sie eben nicht mehr in der Ansicht. Außerdem brauchen wir ein
App-Icon."

**Playlist (✅):** Ursache: Die Liste stand nur im Arbeitsspeicher (`remember`), die Hörstelle
dagegen dauerhaft. Fehler auf dem Emulator nachgestellt: 1.0 zeigte nach dem Schließen „Keine
Audiodateien geladen". Jetzt liegt die Liste dauerhaft in den Einstellungen; Entfernen geht nur
noch über den Papierkorb-Knopf. Beim ersten Start von 1.1 kommt die alte Liste aus Androids
Lesefreigaben zurück, Test: beide Dateien aus 1.0 erschienen wieder.

**Update (🟡):** Sucht beim Öffnen, höchstens alle 15 Minuten, und per Knopf „Auf Updates prüfen".
Getestet auf Android 10: Angebot, Download, beschädigte Datei abgewiesen, Erlaubnis-Dialog,
Android-Installer, Installation von 1.1.0 auf 1.1.1, Liste bleibt. Prüfregeln und Betrieb:
`D:\Programme\OverlayPlayer\docs\DEPLOYMENT.md`. Der Dienst auf bums ist noch nicht ausgerollt.

**Icon (🟡):** Zwei versetzte Fenster mit Musiknote, Indigo bis Violett, als Adaptive Icon, mit
Monochrom-Fassung und PNGs für Android 7. Vorschau:
`D:\Programme\OverlayPlayer\docs\app-icon-vorschau.png`. Claude konnte das Bild nicht ansehen,
weil Bilder die Maschine nicht verlassen dürfen; das Aussehen ist ungeprüft.

### 🟡 Projekt nach `D:\Programme` verschieben (Sue, 2026-11-02)

Wörtlich: „Ok, zuerst sollten wir es verschieben in D:\Programme"

Kopiert nach `D:\Programme\OverlayPlayer`, 1.230 Dateien mit `diff -r` identisch, Testbau am
neuen Ort erfolgreich. Das alte Original `C:\Users\s-has\AndroidStudioProjects\OverlayPlayer`
liegt noch dort: Die Lösch-Sperre hat es angehalten, freigeben kann nur Sue.

## Nebenbei behobene Fehler

- App und Overlay zeigten nach dem Wiederöffnen oder beim Pausieren im Overlay unterschiedliche
  Stände. Die App liest den Stand jetzt direkt vom Dienst.
- Nach einem Stopp am Dateiende startete die Datei beim nächsten Mal am Ende und war sofort
  fertig. Die Stelle wird jetzt am Ende gelöscht.
- Die Hörstelle wurde nur beim Pausieren gesichert. Jetzt alle 5 Sekunden und vor einem Update.
- Eine nicht mehr lesbare Datei konnte die App abstürzen lassen. Jetzt kommt eine Meldung, der
  Eintrag bleibt.
- War das Overlay geschlossen und die Musik lief weiter, kam man nicht mehr daran. Neu: Knopf
  „Overlay zeigen" in der App.
- Eigener Fehler, im Test gefunden: Der Android-Installer ließ sich anfangs nicht starten.

## Entscheidungen ohne Rückfrage

| Entscheidung | Begründung | Was ein Umwerfen kostet |
|---|---|---|
| Vorhandenen Signaturschlüssel behalten statt neuem Schlüssel wie bei Airhold | Die App auf dem Handy trägt ihn seit März (Zertifikat verglichen). Ein neuer Schlüssel erzwingt eine Deinstallation, die Liste und Hörstellen löscht | Später hoch: Wechsel heißt einmal deinstallieren. Der Schlüssel ist ein Debug-Schlüssel und inhaltsgleich mit Schlaufon; das Passwort ist der Standardwert, geschützt ist er nur durch die Ablage |
| Paketname `com.example.overlay_player` bleibt | Änderung wäre für Android eine neue App | Einmal neu installieren |
| Updates nur im Tailnet über Port 5241, Dienst auf 5240 | Muster von Rezeptküche und Pottwal, kein offener Internetzugang; Ports am 02.11.2026 frei gemessen | Gering: Adresse steht in `app/build.gradle.kts`, ändert aber nur neue Versionen |
| Kein Pflicht-Update | Wie bei Airhold: „Später" ist möglich | Gering |
| Overlay 340 dp breit | Schmal genug, um Inhalte daneben zu erreichen; alle Knöpfe passen hinein | Gering: eine Konstante |
| Reihenfolge: Update-Test zuletzt und auf Android 10 | Die Android-16-Testgeräte starteten nicht oder blieben gesperrt. Das Android-10-Gerät prüft zusätzlich den älteren Signaturweg | Keiner |

## Bewusst nicht gebaut

- **Kein Ausrollen auf bums:** Das braucht Sues Ansage.
- **Kein Overlay in der Größe veränderbar:** nicht gewünscht.
- **Kein automatischer Download ohne Rückfrage:** Android verlangt ohnehin die Bestätigung.

## Grenzen der Prüfung

- Getestet nur auf Android 10 im Emulator, nicht auf einem echten Handy und nicht auf Android 13+.
- Ton wurde nicht gehört; geprüft sind Wiedergabe-Status und gespeicherte Stelle.
- Der Emulator schnitt Downloads ab, wenn der Testserver die Verbindung sofort schloss (Server
  sendete nachweislich alle Bytes). Das ist ein Emulator-Effekt; auf bums ungemessen.
- Wer von 1.0 kommt, kann bis zu einem Neustart des Handys „Update-Server nicht erreichbar" sehen,
  weil die Internet-Berechtigung neu ist (auf dem Emulator gemessen, EPERM verschwand nach Neustart).

## Datenquellen

Das Icon ist selbst gezeichnet (`D:\Programme\OverlayPlayer\scripts\make-icon.py`); es enthält
kein fremdes Material. Der Update-Server stammt aus eigenen Projekten. Fremde Daten nutzt die
App nicht.
