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
    & scp.exe artifacts/deploy.tgz "${HostAlias}:/tmp/overlay-player-deploy.tgz"
    if ($LASTEXITCODE -ne 0) { throw 'Uebertragung fehlgeschlagen' }
    & ssh.exe $HostAlias 'mkdir -p /tmp/overlay-player-deploy && tar -xzf /tmp/overlay-player-deploy.tgz -C /tmp/overlay-player-deploy && sudo timeout 120 bash /tmp/overlay-player-deploy/deploy.sh'
    if ($LASTEXITCODE -ne 0) { throw 'Ausrollen fehlgeschlagen' }
} finally { Pop-Location }
