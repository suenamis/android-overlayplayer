# Overlay Player für Android — Aufgabenliste

**Regel:** Jede Aufgabe von Sue kommt ZUERST wörtlich mit Datum hierher, dann wird sie
bearbeitet, dann abgehakt. Diese Liste ist Sues Prüfstein, nicht Claudes Notizzettel.

## Übersicht

| Aufgabe | Datum | Stand |
|---|---|---|
| Projekt nach `D:\Programme` verschieben | 2026-11-02 | ✅ erledigt; altes Original im Papierkorb, nur der leere Ordner bleibt bis zum Ende der Sitzung |
| Automatische Update-Funktion wie bei Airhold | 2026-11-02 | ✅ erledigt, Server läuft seit 2026-11-02 auf bums |
| Geschlossene Overlay-Datei bleibt in der Anzeige | 2026-11-02 | ✅ erledigt |
| App-Icon | 2026-11-02 | ✅ von Sue abgenommen |
| Overlay frei auf dem Bildschirm verschieben | 2026-11-02 | ✅ erledigt |
| Antworten auf die drei Entscheidungen, Sicherung auf GitHub | 2026-11-02 | ✅ erledigt |
| Installationslink über bums per Tailscale | 2026-11-02 | ✅ erledigt, Link nachgeprüft |
| Importierte Datei zeigt Buchstaben-Zahlen-Kombination statt Dateiname | 2026-11-02 | 🟡 Version 1.1.1 gebaut und getestet, Ausrollen wartet auf Sues Ansage |
| Kosten einer Veröffentlichung im Play Store | 2026-11-02 | ✅ beantwortet |

## Aufgaben

### 🟡 Falscher Dateiname nach dem Import (Sue, 2026-11-02)

Wörtlich: „Ok, also das Update sieht schon mal ganz gut aus und auch viel, viel besser. Die Sachen
wurden umgesetzt, aber die Datei, die ich importiert habe, hat nicht den Namen der Datei, die ich
importiert habe, sondern eine ganz komische Buchstaben- und Zahlenkombination. Dadurch weiß ich ja
dann gar nicht wirklich, ob ich gerade die richtige Datei habe. Wann liegt das denn? Kannst du das
bitte lösen?"

Die Meldung kam in der Sitzung der Webseite sue-prise.de an. Zugeordnet zum Overlay Player, weil
dort zuletzt ein Update ausgeliefert wurde und nur diese App Dateien importiert.

Ursache: Die App übernimmt den Namen, den die Dateiquelle meldet, und fiel bei fehlender Angabe auf
die interne Kennung zurück. Welche Quelle es bei Sue war, ist nicht nachgewiesen und nicht
nachgestellt; Androids Dateiauswahl liefert sonst den echten Namen. Deshalb gibt es zwei Wege:

- **Erkennung (von der Sitzung Sue-Prise.de, geprüft und übernommen):** Sieht ein Name wie eine
  Kennung aus, nimmt die App den Titel aus der Audiodatei, auch für schon gespeicherte Einträge.
- **Umbenennen (neu):** Stift-Knopf je Eintrag. Ändert nur den Namen in der Liste, nie die Datei.
  Greift auch dann, wenn die Erkennung nicht zutrifft oder die Datei keinen Titel enthält.

Belege (Android 10, Emulator, von 1.1.0 aktualisiert): Datei mit Kennungsnamen und Titel zeigt
„Hypnose Teil 2"; Datei ohne Titel behält die Kennung; Umbenennen während der Wiedergabe ändert
Liste, App-Anzeige und Benachrichtigung, die Wiedergabe läuft weiter; der Name übersteht den
Neustart. Nicht geprüft: der Titel im Overlay selbst (Testwerkzeug liest das Fenster nicht; gesetzt
wird er in `AudioPlayerService.kt` im selben Schritt wie die geprüfte Benachrichtigung), echtes
Handy, Sues ursprüngliche Quelle. Bekannte Grenze: Ein von Hand vergebener Name, der selbst wie
eine Kennung aussieht (etwa nur Ziffern), wird beim Start durch den Titel ersetzt, falls es einen gibt.

### ✅ Play Store (Sue, 2026-11-02)

Wörtlich: „Würde es was kosten, wenn wir das im Android Store veröffentlichen?"

Sue dazu, ebenfalls wörtlich: „Ich hatte aus Versehen eine andere Session informiert, aber du
machst jetzt weiter." Die Sitzung Sue-Prise.de hat ihre Arbeit an diese Sitzung übergeben.

Antwort: Einmalig 25 US-Dollar, keine Jahresgebühr. Neue private Konten brauchen vorher 12 Tester
über 14 Tage. Für diese App käme dazu: Das eigene Update über bums müsste für die Play-Fassung
raus (Google verbietet Selbst-Updates außerhalb von Play), außerdem ein eigener Paketname und ein
eigener Schlüssel, also einmal Neuinstallation. Abgerufen 2026-11-02; Quellen:
https://support.google.com/googleplay/android-developer/answer/14151465 und
https://support.google.com/googleplay/android-developer/answer/12085295 ; die Gebühr nur aus
Drittquellen wie https://magora-systems.com/google-play-developer-account-cost/ . Der Punkt
Paketname und Schlüssel ist Claudes Kenntnis, in dieser Suche nicht belegt.

### ✅ Installationslink (Sue, 2026-11-02)

Wörtlich: „Den Installationslink benötige ich über einen Server-BOOMS-Link über Tailscale-Login"

Bestand schon durch das Ausrollen: `https://bums.tail1c1957.ts.net:5241/` zeigt eine Seite mit
Installationsknopf, direkt: `https://bums.tail1c1957.ts.net:5241/overlay-player-2.apk`. Erreichbar
nur mit angemeldetem Tailscale. Keine zusätzliche Anmeldung und nichts Neues gebaut.

### ✅ Entscheidungen und GitHub (Sue, 2026-11-02)

Wörtlich: „2 altes löschen / 3 icon ist gut! / 1 update auf bums / Können wir auch sichern auf
https://github.com/suenamis/android-overlayplayer nicht wundern, ich habe es public gemacht. Das
ist für mich in Ordnung."

**Update-Dienst:** Am 2026-11-02 auf bums ausgerollt, Ports 5240 und 5241 im Port-Register von
`D:\Programme\Synology\bums-DEPLOYMENT-GUIDE.md` eingetragen. Gemessen von Blacky über das Tailnet:
`/health` ok, `/latest.json` zeigt Version 1.1.0, die APK kam mit 10.826.445 Byte in 0,9 Sekunden
und passender Prüfsumme an. Speicherbedarf des Dienstes: rund 10 MB.

**Altes Original:** Inhalt (1.230 Dateien) liegt im Papierkorb, nicht endgültig gelöscht. Vorher
mit dem Tag `v1.0-ausgangsstand-stable` verglichen: Bis auf `.gitignore`, IDE-Zwischendateien und
Zeilenenden war es gleich; `local.properties` liegt auch am neuen Ort. Der leere Ordner bleibt,
weil ein laufender Prozess ihn offen hält, vermutlich diese Sitzung (ungemessen).

**GitHub:** `https://github.com/suenamis/android-overlayplayer`, öffentlich auf Sues Wunsch. Vorher
geprüft: kein Schlüssel, keine APK, keine Audiodateien im Verlauf. Einziges Passwort im Code ist
der allgemein bekannte Debug-Standardwert `android`; die Schlüsseldatei selbst liegt nicht im Repo.

### ✅ Overlay verschieben (Sue, 2026-11-02)

Wörtlich: „Außerdem wäre es cool, wenn ich das Overlay auf dem Bildschirm verschieben könnte,
damit ich auch an Sachen dran komme, die an der Stelle sind, wo gerade der Overlay-Player ist."

Vorher: nur auf und ab, an einem 4 dp dünnen Griff, immer bildschirmbreit. Jetzt: Fenster mit
höchstens 340 dp Breite, in alle Richtungen ziehbar an Griff, Titel und allen freien Flächen.
Es bleibt ganz auf dem Bildschirm und merkt sich seine Position.
Belege (Android 10): Rahmen verschoben von y 1753 auf 1057; Ziehen über den Rand endet bei
(188, 1879, 1080, 2274) auf 1080 × 2400; Position nach Stop und Neustart unverändert.

### ✅ Automatische Update-Funktion, geschlossene Datei bleibt, App-Icon (Sue, 2026-11-02)

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

**Update (✅):** Sucht beim Öffnen, höchstens alle 15 Minuten, und per Knopf „Auf Updates prüfen".
Getestet auf Android 10: Angebot, Download, beschädigte Datei abgewiesen, Erlaubnis-Dialog,
Android-Installer, Installation von 1.1.0 auf 1.1.1, Liste bleibt. Prüfregeln und Betrieb:
`D:\Programme\OverlayPlayer\docs\DEPLOYMENT.md`. Der Dienst läuft seit 2026-11-02 auf bums.

**Icon (✅, von Sue abgenommen 2026-11-02):** Zwei versetzte Fenster mit Musiknote, Indigo bis Violett, als Adaptive Icon, mit
Monochrom-Fassung und PNGs für Android 7. Vorschau:
`D:\Programme\OverlayPlayer\docs\app-icon-vorschau.png`. Claude konnte das Bild nicht ansehen,
weil Bilder die Maschine nicht verlassen dürfen; das Aussehen ist ungeprüft.

### ✅ Projekt nach `D:\Programme` verschieben (Sue, 2026-11-02)

Wörtlich: „Ok, zuerst sollten wir es verschieben in D:\Programme"

Kopiert nach `D:\Programme\OverlayPlayer`, 1.230 Dateien mit `diff -r` identisch, Testbau am
neuen Ort erfolgreich. Das alte Original `C:\Users\s-has\AndroidStudioProjects\OverlayPlayer`
hielt die Lösch-Sperre zunächst an; nach Sues Freigabe liegt sein Inhalt seit 2026-11-02 im Papierkorb.

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
| Altes Original in den Papierkorb statt endgültig löschen | Sue sagte „altes löschen"; der Papierkorb erfüllt das und bleibt umkehrbar | Keiner: wiederherstellen oder Papierkorb leeren |
| Deploy-Skripte nutzen jetzt `/tmp/ea-overlay-player-deploy` und räumen ihn selbst weg | Die Lösch-Sperre erlaubt das Wegräumen nur in solchen Ordnern; Neu-Ausrollen damit geprüft | Gering |
| `.claude/settings.local.json` und `__pycache__` aus Git genommen, Verlauf nicht umgeschrieben | Gehören nicht ins Repo. Die alte Fassung im ersten Commit enthält nur eine harmlose Befehlsfreigabe | Gering |
| Umbenennen zusätzlich zur Erkennung gebaut | Die Erkennung ist eine Vermutung; ohne Titel in der Datei hilft sie nicht, und Sue muss ihre Datei sicher erkennen | Gering: ein Knopf und ein Dialog |
| Reihenfolge: Update-Test zuletzt und auf Android 10 | Die Android-16-Testgeräte starteten nicht oder blieben gesperrt. Das Android-10-Gerät prüft zusätzlich den älteren Signaturweg | Keiner |

## Bewusst nicht gebaut

- **Kein Overlay in der Größe veränderbar:** nicht gewünscht.
- **Kein automatischer Download ohne Rückfrage:** Android verlangt ohnehin die Bestätigung.

## Grenzen der Prüfung

- Getestet nur auf Android 10 im Emulator, nicht auf einem echten Handy und nicht auf Android 13+.
- Ton wurde nicht gehört; geprüft sind Wiedergabe-Status und gespeicherte Stelle.
- Der Emulator schnitt Downloads ab, wenn der Testserver die Verbindung sofort schloss (Server
  sendete nachweislich alle Bytes). Das ist ein Emulator-Effekt. Von Blacky über bums kam die Datei vollständig an; vom Handy aus ist das ungemessen.
- Wer von 1.0 kommt, kann bis zu einem Neustart des Handys „Update-Server nicht erreichbar" sehen,
  weil die Internet-Berechtigung neu ist (auf dem Emulator gemessen, EPERM verschwand nach Neustart).

## Datenquellen

Das Icon ist selbst gezeichnet (`D:\Programme\OverlayPlayer\scripts\make-icon.py`); es enthält
kein fremdes Material. Der Update-Server stammt aus eigenen Projekten. Fremde Daten nutzt die
App nicht.
