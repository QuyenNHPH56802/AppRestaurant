# PHASE 11: Build, align, sign, and verify the Restaurant Staff APK.
# Requires environment variables:
#   ANDROID_HOME                - Android SDK root
#   KEYSTORE_PATH               - path to release keystore (.jks)
#   KEYSTORE_PASSWORD           - keystore password
#   KEY_ALIAS                   - signing key alias (default: restaurant)
#   KEY_PASSWORD                - signing key password
#
# Usage:
#   powershell -File installer\android\sign-and-align.ps1 -BuildType release
#   powershell -File installer\android\sign-and-align.ps1 -BuildType debug

param(
    [ValidateSet('debug', 'release')] [string]$BuildType = 'release'
)

$ErrorActionPreference = 'Stop'

if (-not $env:ANDROID_HOME) {
    Write-Error "ANDROID_HOME is not set."
    exit 1
}

$apksigner = Join-Path $env:ANDROID_HOME 'build-tools' '*' 'apksigner.bat'
$zipalign  = Join-Path $env:ANDROID_HOME 'build-tools' '*' 'zipalign.exe'

# Pick the highest build-tools version
$apksigner = Get-ChildItem -Path $apksigner | Sort-Object -Descending | Select-Object -First 1 -ExpandProperty FullName
$zipalign  = Get-ChildItem -Path $zipalign  | Sort-Object -Descending | Select-Object -First 1 -ExpandProperty FullName

Write-Host "Using apksigner: $apksigner"
Write-Host "Using zipalign:  $zipalign"

Set-Location (Join-Path $PSScriptRoot '..\..')

# Build
& .\gradlew.bat ":app:assemble$($BuildType.Substring(0,1).ToUpper())$($BuildType.Substring(1))" --no-daemon
if ($LASTEXITCODE -ne 0) { exit 1 }

$apkIn  = "android/app/build/outputs/apk/$BuildType/app-$BuildType.apk"
$apkOut = "android/app/build/outputs/apk/$BuildType/app-$BuildType-aligned.apk"
$apkSigned = "installer/android/RestaurantStaff-$BuildType.apk"

if ($BuildType -eq 'release') {
    if (-not $env:KEYSTORE_PATH) { Write-Error "KEYSTORE_PATH is not set for release builds."; exit 1 }
    & $zipalign -p -f 4 $apkIn $apkOut
    if ($LASTEXITCODE -ne 0) { exit 1 }
    & $apksigner sign --ks $env:KEYSTORE_PATH `
                     --ks-key-alias ($env:KEY_ALIAS ?? 'restaurant') `
                     --ks-pass "pass:$env:KEYSTORE_PASSWORD" `
                     --key-pass "pass:$($env:KEY_PASSWORD ?? $env:KEYSTORE_PASSWORD)" `
                     --out $apkSigned $apkOut
    if ($LASTEXITCODE -ne 0) { exit 1 }
    & $apksigner verify --verbose $apkSigned
    if ($LASTEXITCODE -ne 0) { exit 1 }
    Write-Host "Signed APK: $apkSigned"
} else {
    & $zipalign -p -f 4 $apkIn $apkOut
    Copy-Item $apkOut $apkSigned
    Write-Host "Aligned APK (debug, unsigned): $apkSigned"
}

# Print final size
$size = (Get-Item $apkSigned).Length
Write-Host ("APK size: {0:N2} MB" -f ($size / 1MB))