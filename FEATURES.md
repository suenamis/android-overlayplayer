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
| Importierte Datei zeigt Buchstaben-Zahlen-Kombination statt Dateiname | 2026-11-02 | 🟡 Version 1.1.1 seit 2026-11-02 auf bums, Test auf Sues Handy steht aus |
| Kosten einer Veröffentlichung im Play Store | 2026-11-02 | ✅ beantwortet |
| MP4-Videos von Dropbox und Telefon, Stelle merken, 10 s spulen, Playlist, direkt ausrollen | 2026-11-07 | ✅ Version 1.2.0 seit 2026-11-07 auf bums; Video spielt laut Sue am 2026-11-07 auf ihrem Handy |
| Zwei Videos als Tailscale-Download-Link fürs Handy | 2026-11-07 | ✅ erledigt, Prüfsummen gleich |
| „Die 2 Videos kannst du auf bums löschen. Habe ich runtergeladen.“ | 2026-11-07 | ✅ 2026-11-07 gelöscht, Ordner leer, bums danach 64 GB frei |
| Overlay Player beim Öffnen einer Datei als App vorschlagen | 2026-11-07 | 🟡 Version 1.3.0 seit 2026-11-07 auf bums; Test auf Sues Handy steht aus |
| Handy zeigt noch „Audio-Dateien hinzufügen“, Videos blass; Android meldete „App nicht installiert“ | 2026-11-07 | ✅ Update laut Sue danach durchgegangen; Grund des ersten Fehlschlags nicht mehr feststellbar. Ab 1.3.0 nennt die App den Grund |
| Video in der App größer, Bedienung darüber und ausblendbar, Querformat; Hochformat zurück zur jetzigen Größe | 2026-11-07 | 🟡 Version 1.3.0 seit 2026-11-07 auf bums; Test auf Sues Handy steht aus |
| „Ummmmeerrrr aaussroolllenn uunnd ppussheenn“ (1.3.0 ausrollen, GitHub pushen) | 2026-11-07 | ✅ 2026-11-07: 1.3.0 auf bums, Download über Tailscale mit gleicher Prüfsumme (`6f7bc0a0…`); GitHub-Stand gepusht |

## Aufgaben

### 🟡 Video groß und quer, Bedienung darüber (Sue, 2026-11-07, Version 1.3.0)

Wörtlich: „Jetzt scheint es geklappt zu haben. Das Video wird jetzt aber nur in der App ganz klein
abgespielt. Kann ich das nicht größer innerhalb von der App sehen und den Bedienbutton da drüber,
damit die so ausblenden, oder geht das nur so? Also, wenn ich auf Play drücke, wird es größer und
geht vielleicht sogar in Querformat. Wenn ich zurück in das normale Hochkantformat gehe, geht es
dann wieder in diese jetzige Größe zurück. Wäre das eine Möglichkeit?"

Gebaut (2026-11-07):
- **Vollbild** öffnet sich bei Play auf ein Video, beim Öffnen eines Videos aus einer anderen App,
  beim Antippen des Bildes und beim Drehen des Handys ins Querformat.
- Ein Querformat-Video dreht die Anzeige ins Querformat, auch wenn die automatische Drehung aus
  ist. Ein Hochformat-Video bleibt hochkant.
- **Bedienung über dem Bild:** Titel, 30 s und 10 s zurück, Play/Pause, 10 s und 30 s vor,
  Zeitleiste zum Springen, Knopf „Vollbild verlassen“. Beim Abspielen verschwindet sie nach vier
  Sekunden; Antippen zeigt sie wieder. Pausiert bleibt sie stehen.
- **Zurück:** Handy aufrichten, Zurück-Taste oder Knopf. Danach die normale Ansicht.
- Im Vollbild verschwinden die Leisten oben und unten; der Bildschirm bleibt an, solange das Video
  läuft. Beim ersten Mal zeigt Android einmalig einen Hinweis zum Vollbild.
- Auch die normale Ansicht ist größer: Das Bild darf 45 % der Bildschirmhöhe nutzen statt
  höchstens 320 dp. Ein Symbol unten rechts zeigt, dass Antippen ins Vollbild führt.
- Wechsel in eine andere App: weiterhin das kleine Fenster.

Belege (Android 10, Emulator, `D:\Programme\OverlayPlayer\.scratch\test10.py`): 26 Prüfungen grün.
Zusätzlich von Hand mit dem Lagesensor des Emulators: Handy quer gedreht → Vollbild, aufgerichtet →
normale Ansicht; aus dem erzwungenen Querformat aufgerichtet → hochkant und normale Ansicht. Das
Videobild im Vollbild ist über Farbzählung nachgewiesen (137 Farbstufen), nicht angesehen.

### 🟡 Beim Öffnen einer Datei vorgeschlagen werden (Sue, 2026-11-07, Version 1.3.0)

Wörtlich: „Wenn ich die Datei versuche zu öffnen, werden mir Apps vorgeschlagen, aber nicht der
Overlay-Player. Gibt es eine Möglichkeit, das einzustellen, dass er dann direkt auch vorgeschlagen
wird?"

Ursache: Die App hatte sich bei Android nur als startbare App angemeldet, nicht als App, die Audio
und Video öffnen kann.

Gebaut (2026-11-07):
- Android bietet den Player an bei **„Öffnen mit“** (Downloads, Dateimanager, Browser, Dropbox)
  und bei **„Teilen“**, auch für mehrere Dateien. Nur bei Audio und Video.
- Die Datei kommt in die Liste, wird ausgewählt und spielt sofort; ein Video öffnet im Vollbild.
- Dauerhafter Zugriff, damit der Eintrag einen Neustart übersteht, in dieser Reihenfolge:
  1. dauerhafte Freigabe der Quelle, wenn sie eine anbietet;
  2. dieselbe Datei in Androids Medienverzeichnis (gleicher Name, gleiche Größe); dafür fragt die
     App einmal nach dem Leserecht für Audio und Video;
  3. sonst eine Kopie im App-Speicher (`Android/data/com.example.overlay_player/files/Importe`).
     Vorher prüft die App den Platz; die Kopie entsteht als Teil-Datei und bekommt ihren Namen erst,
     wenn sie vollständig ist. Der Fortschritt steht in Prozent unter dem Knopf. Entfernen aus der
     Liste löscht die Kopie, das Original bleibt.
- Dieselbe Datei zweimal öffnen ergibt keinen zweiten Eintrag und keine zweite Kopie.

Belege (Android 10, Emulator, `D:\Programme\OverlayPlayer\.scratch\test9.py`): 25 Prüfungen grün,
darunter: angeboten für `video/mp4` und `audio/mpeg`, nicht für Bilder; Video aus den Downloads mit
Dateinamen in der Liste und spielt; Kopie vollständig (920.407 von 920.407 Bytes), keine Teil-Datei
übrig; nach Neustart alle Einträge da, die Kopie spielt; Entfernen löscht die Kopie.

Eigener Fehler, im Test gefunden und behoben: Eine nur kurz gültige Freigabe hätte einen Medien-Link
in die Liste gebracht, der nach einem Neustart nicht mehr lesbar gewesen wäre. Medien-Links werden
jetzt nur mit eigenem Leserecht übernommen, sonst wird kopiert.

### ✅ „App nicht installiert“ beim Update (Sue, 2026-11-07)

Wörtlich: „Es funktioniert aber noch nicht. Ich kann im Overlay Player immer noch keine
Videodateien hinzufügen. Da steht als Button: „Audio-Dateien hinzufügen“, und die Videos, wenn ich
die auswählen möchte, sind hier quasi blass.“ — und: „Vielleicht hat aber auch das erste Update
nicht geklappt. Da steht: „App nicht installiert.““

Befund: Der Knopftext gehört zur alten Version, das Update war also nicht installiert. Geprüft und
ausgeschlossen: falscher Schlüssel (alle Update-Dateien und der Schlüssel vom März haben den
Fingerabdruck `3656253a…549b`; die App prüft das vor dem Installieren und hätte mit eigener Meldung
abgebrochen) und eine falsche Datei auf bums (`latest.json` nennt 1.2.0, Download über Tailscale
vollständig). Kurz darauf meldete Sue, dass es geklappt hat. Den Grund des ersten Fehlschlags kann
man nicht mehr feststellen; der alte Installationsweg bekam von Android keine Antwort.

Für künftige Updates behoben (1.3.0): Die App installiert über Androids Installationssitzung und
bekommt die Antwort zurück. Scheitert es, steht der Grund unten in der App, etwa „zu wenig freier
Speicher auf dem Handy“, „von Android blockiert“ oder „abgebrochen“. Belege: Ein Abbruch im Emulator
ergab „Update nicht installiert: abgebrochen (INSTALL_FAILED_ABORTED …)“; danach lief das Update von
versionCode 5 auf 6 durch.

### ✅ Videos zum Herunterladen (Sue, 2026-11-07)

Wörtlich: „@"E:\Dropbox\_Exchange\Christo Showhypnose ｜ Die Unfassbaren.mp4"
@"E:\Dropbox\_Exchange\SchifHypno 1.mp4" Kannst du mir dann diese beiden Videos als
Tailscale-Link zum Download für Android zur Verfügung stellen? Ich krieg die sonst irgendwie nicht
aufs Telefon. Wäre super, wenn du das ohne Rückfrage erledigst."

Gebaut am 2026-11-07: Der Update-Dienst auf bums hat eine Seite
`https://bums.tail1c1957.ts.net:5241/dateien/` mit Download-Links (nur im Tailnet). Er liefert
nur Audio- und Videodateien, die in `/srv/overlay-player/dateien/` liegen, kein Pfad aus der
Adresse, mit Teilabrufen, damit ein abgebrochener Download weiterlaufen kann. Neue Dateien legt
`D:\Programme\OverlayPlayer\scripts\dateien-bereitstellen.ps1 -Files <Pfade>` ab.
Übertragen: beide Videos, 1.498 MB und 1.066 MB, in 200 Sekunden; SHA-256 auf bums und am PC
gleich. Claude hat die Videos nicht geöffnet, nur kopiert. Auf bums sind danach noch 60 GB frei
(97 % belegt).

Entscheidung ohne Rückfrage: Bereitstellung im vorhandenen Update-Dienst statt in einem neuen
Dienst mit neuem Port. Grund: kein zusätzlicher Port, dieselbe Adresse wie für die App, derselbe
Schutz (nur Tailnet). Umwerfen kostet wenig: die Route in `server/update_server.py` entfernen.
Die Dateien bleiben liegen, bis Sue sie nicht mehr braucht; Löschen geht nur auf ihre Ansage.

### 🟡 Videos (Sue, 2026-11-07)

Wörtlich: „Kannst du bitte den Overlay Player so erweitern, dass er auch MP4-Dateien von der
Dropbox oder vom Telefon abspielt und sich auch dort genau die Stelle merkt, wo man aufgehört hat,
und dass man da 10 Sekunden zurück- und vorspulen kann, so wie bei den Audios? Und dass man auch
eine Playlist anlegen kann oder mehrere Videos einladen kann. Bitte direkt ausrollen und so, dass
ich automatisch das Update bekomme."

Nachtrag, wörtlich: „Ja, beim Overlay muss das Video aber klein werden, so wie bei YouTube, wenn
man dann in ein anderes Fenster wechselt."

**Gebaut (Version 1.2.0):**

- Dateiauswahl nimmt Audio und Video (`audio/*`, `video/*`), mehrere auf einmal. Videos stehen mit
  Filmsymbol in derselben Liste wie die Audios.
- Solange die App vorn ist, läuft ein Video groß in der App (statt der Überschrift, höchstens
  320 dp hoch, im echten Seitenverhältnis). Wechselt man in eine andere App, wird es zum kleinen,
  verschiebbaren Fenster (220 dp breit): Bild, Titel, 10 s zurück, Play/Pause, 10 s vor,
  Schließen. Zurück in der App verschwindet das kleine Fenster, das Bild ist wieder groß.
- Audio verhält sich wie bisher (breites Fenster mit Regler und 30-s-Knöpfen, auch in der App).
- Stelle merken und 10/30 s spulen gelten für Videos genauso, mit demselben Speicher je Datei.
- Playlist: Ist eine Datei zu Ende, spielt die nächste der Liste von selbst; nach der letzten ist
  Schluss.
- Dateien werden im Hintergrund geöffnet. Grund: Eine Datei aus der Dropbox muss die Dropbox-App
  erst herunterladen; im Hauptfaden hätte das die App einfrieren lassen. So lange steht „Lädt: …"
  im Fenster und in der Benachrichtigung.

**Belege (Android 10, Emulator, Update von 1.1.1 ohne Deinstallation, 18 Prüfungen grün):** vier
Dateien auf einmal geladen; Querformat-Video groß in der App und kein Fenster; nach dem Ende des
ersten Videos startet das nächste; beim Wechsel zum Startbildschirm kleines Fenster mit
577 px = 220 dp; Bild läuft dort weiter; zurück in der App wieder groß; zweimal 10 s vor ergibt
33,3 s gespeichert; nach Neustart der App geht es dort weiter, 10 s zurück wirkt (23,4 s);
Hochformat-Video (gedreht gespeichert) erscheint hochkant, das kleine Fenster bleibt ganz im Bild;
keine Abstürze im Protokoll. Dass ein Bild zu sehen ist, misst der Test über die Zahl der Farben
im Bildbereich (Testbild mit Farbbalken: 53 bis 71 Farben gegen höchstens 5 ohne Bild); niemand
hat die Bildschirmfotos angesehen, sie verlassen den Rechner nicht.

**Nicht geprüft:** Dropbox (auf dem Emulator nicht installiert), echtes Handy, Ton gehört, große
Videodateien. Offenes Risiko bei Dropbox: Gibt die Dropbox-App keinen dauerhaften Zugriff, meldet
die App das beim Hinzufügen („Diese Quelle erlaubt keinen dauerhaften Zugriff"); der Eintrag
funktioniert dann womöglich nach einem Neustart des Handys nicht mehr.

### 🟡 Falscher Dateiname nach dem Import (Sue, 2026-11-02)

Ansage zu Version 1.1.1, wörtlich: „ausrollen" (Antwort auf die Frage nach Ausrollen und Sicherung
auf GitHub).

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

- Das Ausrollen meldete am 2026-11-07 einen Fehler, obwohl es geklappt hatte: PowerShell 5.1 wertete
  die Wiederholungsmeldungen der Gesundheitsprüfung als Abbruch. `scripts/deploy.ps1` prüft jetzt nur
  den Rückgabewert; zweites Ausrollen damit sauber durchgelaufen.

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
| Video groß in der App, klein in anderen Apps; Audio unverändert | Sues Wunsch „so wie bei YouTube“; bei Audio gibt es kein Bild, dort bleibt das gewohnte Fenster | Gering: eine Abfrage im Dienst (`wantsOverlay`) |
| Kleines Fenster 220 dp breit, ohne Regler und 30-s-Knöpfe, Bild höchstens 40 % der Bildschirmhöhe | Klein genug, um Inhalte dahinter zu erreichen; Sue wollte 10 s spulen, das bleibt | Gering: Konstanten in `AudioPlayerService.kt` |
| Playlist spielt von selbst weiter, auch bei Audio | „Playlist anlegen“ heißt nacheinander abspielen; vorher hielt der Player nach jeder Datei an | Gering: eine Funktion (`onCompleted`) |
| Version 1.2.0 statt 1.1.2 | Neue Funktion, nicht nur Fehlerbehebung | Keiner |
| Reihenfolge: Update-Test zuletzt und auf Android 10 | Die Android-16-Testgeräte starteten nicht oder blieben gesperrt. Das Android-10-Gerät prüft zusätzlich den älteren Signaturweg | Keiner |
| Play auf ein Video öffnet sofort das Vollbild | Sues Vorschlag „wenn ich auf Play drücke, wird es größer“ | Gering: eine Zeile (`vollbildGewuenscht`) |
| Bedienung verschwindet nach vier statt drei Sekunden | Drei Sekunden waren im Test zu knapp, um nach dem Einblenden einen Knopf zu treffen | Gering: eine Zahl |
| Nach „Vollbild verlassen“ bei quer gehaltenem Handy bleibt die Ansicht hochkant, bis das Handy aufgerichtet ist | Sonst spränge die Anzeige sofort wieder ins Vollbild | Gering |
| Ohne Lagemeldung löst sich die Hochkant-Sperre nach zwei Sekunden | Sonst bliebe die Drehung dauerhaft gesperrt (im Emulator beobachtet, der den Sensor nur bei Änderung meldet) | Gering |
| Kopie im App-Speicher nur als letzter Ausweg | Ein 1,5-GB-Video soll nicht doppelt Platz belegen | Gering; Kopien verschwinden mit der App |
| App fragt beim „Öffnen mit“ einmal nach dem Leserecht für Audio und Video | Damit findet sie heruntergeladene Dateien ohne Kopie wieder | Gering; Ablehnen führt zur Kopie |
| Updates über Installationssitzung statt Übergabe der Datei an den Installer | Nur so meldet Android den Grund eines Fehlschlags | Mittel; der alte Weg steht im Git-Verlauf |
| Version 1.3.0 | Neue Funktionen | Keiner |

## Bewusst nicht gebaut

- **Kein Overlay in der Größe veränderbar:** nicht gewünscht.
- ~~Kein Vollbild für Videos~~: seit 1.3.0 gebaut, auf Sues Wunsch vom 2026-11-07.
- **Keine Gesten im Vollbild** (Wischen für Lautstärke oder Helligkeit, Doppeltippen zum Spulen):
  nicht gewünscht; die Knöpfe decken 10 s und 30 s ab.
- **Keine Knöpfe für nächste und vorige Datei:** nicht gewünscht; man wählt in der Liste.
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
