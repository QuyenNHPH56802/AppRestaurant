# PHASE 11: Generate server.ico (256x256) and android adaptive icon PNGs.
# Requires ImageMagick (`magick` or `convert`) on the build machine. The icons
# themselves come from the customer's brand or a stock placeholder.
#
# Usage:
#   powershell -File scripts\generate-icons.ps1 -Source "C:\path\to\logo.png"
#
# Output:
#   installer/server/icons/server.ico         (used by Inno Setup)
#   android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml stays (vector adaptive)
#   android/app/src/main/res/drawable/ic_launcher_compat.xml stays (vector fallback)

param([string]$Source)
if (-not (Test-Path $Source)) {
    Write-Error "Source not found: $Source"
    exit 1
}

$ico = "installer/server/icons/server.ico"
$pngSizes = "16,32,48,64,128,256"

if (Get-Command magick -ErrorAction SilentlyContinue) {
    & magick "$Source" -define icon:auto-resize=$pngSizes "$ico"
    Write-Host "Wrote $ico"
} elseif (Get-Command convert -ErrorAction SilentlyContinue) {
    & convert "$Source" -define icon:auto-resize=$pngSizes "$ico"
    Write-Host "Wrote $ico"
} else {
    Write-Error "ImageMagick not found (need 'magick' or 'convert' on PATH)."
    exit 1
}