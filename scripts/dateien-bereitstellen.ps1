param([Parameter(Mandatory)][string[]]$Files, [string]$HostAlias = 'bums')
# Legt Audio- und Videodateien auf bums ab: https://bums.tail1c1957.ts.net:5241/dateien/
$ErrorActionPreference = 'Stop'
$stage = '/tmp/ea-overlay-player-files'
foreach ($file in $Files) {
    if (!(Test-Path -LiteralPath $file)) { throw "Datei fehlt: $file" }
}
& ssh.exe $HostAlias "mkdir -p $stage"
if ($LASTEXITCODE -ne 0) { throw 'Vorbereitung fehlgeschlagen' }
foreach ($file in $Files) {
    & scp.exe $file "${HostAlias}:$stage/"
    if ($LASTEXITCODE -ne 0) { throw "Uebertragung fehlgeschlagen: $file" }
}
& ssh.exe $HostAlias "sudo install -d -o root -g overlay-player -m 750 /srv/overlay-player/dateien && sudo install -o root -g overlay-player -m 640 $stage/* /srv/overlay-player/dateien/; result=`$?; rm -rf $stage; exit `$result"
if ($LASTEXITCODE -ne 0) { throw 'Ablegen fehlgeschlagen' }
