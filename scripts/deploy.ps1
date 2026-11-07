param([string]$HostAlias = 'bums')
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path $PSScriptRoot -Parent
Push-Location $projectDir
try {
    $manifest = Get-Content -Raw -LiteralPath 'artifacts/latest.json' | ConvertFrom-Json
    $apkName = "overlay-player-$($manifest.versionCode).apk"
    if ($manifest.path -ne "/$apkName") { throw 'Ungueltiger APK-Pfad' }
    if ((Get-FileHash -LiteralPath "artifacts/$apkName" -Algorithm SHA256).Hash.ToLowerInvariant() -ne $manifest.sha256) { throw 'APK-Pruefsumme stimmt nicht' }
    $stage = Join-Path $projectDir ('.scratch/deploy-' + $manifest.versionCode)
    foreach ($dir in @('server','artifacts','docs','scripts')) {
        New-Item -ItemType Directory -Force -Path (Join-Path $stage $dir) | Out-Null
    }
    foreach ($file in @('server/update_server.py','artifacts/latest.json',"artifacts/$apkName",'docs/DEPLOYMENT.md','scripts/publish-release.py')) {
        Copy-Item -LiteralPath $file -Destination (Join-Path $stage $file)
    }
    Copy-Item -LiteralPath 'scripts/deploy.sh' -Destination (Join-Path $stage 'deploy.sh')
    & tar.exe -czf artifacts/deploy.tgz -C $stage server artifacts docs scripts deploy.sh
    if ($LASTEXITCODE -ne 0) { throw 'Paket fehlgeschlagen' }
    & ssh.exe $HostAlias 'mkdir -p /tmp/ea-overlay-player-deploy'
    if ($LASTEXITCODE -ne 0) { throw 'Vorbereitung fehlgeschlagen' }
    & scp.exe artifacts/deploy.tgz "${HostAlias}:/tmp/ea-overlay-player-deploy/deploy.tgz"
    if ($LASTEXITCODE -ne 0) { throw 'Uebertragung fehlgeschlagen' }
    # Die Gesundheitspruefung meldet ihre Wiederholungen auf stderr, solange der Dienst startet.
    # PowerShell 5.1 hielte das bei 'Stop' fuer einen Fehler; massgeblich ist der Rueckgabewert.
    $ErrorActionPreference = 'Continue'
    & ssh.exe $HostAlias 'cd /tmp/ea-overlay-player-deploy && tar -xzf deploy.tgz && sudo timeout 120 bash deploy.sh; result=$?; rm -rf /tmp/ea-overlay-player-deploy; exit $result' 2>&1 | ForEach-Object { "$_" }
    $result = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($result -ne 0) { throw 'Ausrollen fehlgeschlagen' }
} finally { Pop-Location }
