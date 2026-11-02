param([Parameter(Mandatory)][string]$Notes)
$ErrorActionPreference = 'Stop'
$projectDir = Split-Path $PSScriptRoot -Parent
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
# Pfad mit Umlaut nicht abtippen: PowerShell 5.1 liest das Skript als ANSI.
$keyDir = (Get-ChildItem -LiteralPath 'E:\OneDrive\Dokumente' -Directory -Filter 'AppSchl*' | Select-Object -First 1).FullName
$keyFile = Join-Path $keyDir 'overlay-player-debug.keystore'
if (!(Test-Path -LiteralPath $keyFile)) { throw 'Signaturschluessel fehlt. Abbruch, sonst waeren keine Updates mehr moeglich.' }
$expected = ([IO.File]::ReadAllText((Join-Path $keyDir 'overlay-player-certificate-sha256.txt'))).Trim()
Push-Location $projectDir
try {
    $env:OVERLAY_KEYSTORE = $keyFile
    & "$projectDir\gradlew.bat" --no-daemon testDebugUnitTest assembleRelease lintRelease
    if ($LASTEXITCODE -ne 0) { throw 'Tests, Bau oder Lint fehlgeschlagen' }
    $apk = Join-Path $projectDir 'app\build\outputs\apk\release\app-release.apk'
    $signer = "$env:ANDROID_HOME\build-tools\36.1.0\apksigner.bat"
    $certs = & $signer verify --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw 'Signaturpruefung fehlgeschlagen' }
    $fingerprint = ($certs | Select-String 'Signer #1 certificate SHA-256 digest:').ToString().Split(':')[-1].Trim()
    if ($fingerprint -ne $expected) { throw 'Signaturschluessel passt nicht zur installierten App' }
    $gradle = Get-Content -Raw -Encoding UTF8 -LiteralPath 'app/build.gradle.kts'
    $code = [int][regex]::Match($gradle, '"overlayVersionCode"\) as String\?\)\?\.toInt\(\) \?: (\d+)').Groups[1].Value
    $name = [regex]::Match($gradle, '"overlayVersionName"\) as String\? \?: "([^"]+)"').Groups[1].Value
    if ($code -lt 1 -or !$name) { throw 'Version nicht lesbar' }
    New-Item -ItemType Directory -Force -Path 'artifacts' | Out-Null
    $release = Join-Path $projectDir "artifacts\overlay-player-$code.apk"
    Copy-Item -LiteralPath $apk -Destination $release -Force
    $manifest = @{versionCode=$code;versionName=$name;path="/overlay-player-$code.apk";size=(Get-Item -LiteralPath $release).Length;sha256=(Get-FileHash -LiteralPath $release -Algorithm SHA256).Hash.ToLowerInvariant();notes=$Notes}
    [IO.File]::WriteAllText((Join-Path $projectDir 'artifacts\latest.json'), ($manifest | ConvertTo-Json), (New-Object Text.UTF8Encoding $false))
    & node scripts/snapshot-build.js $release
    if ($LASTEXITCODE -ne 0) { throw 'Snapshot fehlgeschlagen' }
    Write-Output "Signierte APK: $release"
    Write-Output "Zertifikat SHA-256: $fingerprint"
} finally {
    Pop-Location
    Remove-Item Env:OVERLAY_KEYSTORE -ErrorAction SilentlyContinue
}
