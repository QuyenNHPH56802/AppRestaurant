# PHASE 11: Build a printable PDF of the user/admin manual.
# Requires one of: pandoc, wkhtmltopdf, or Microsoft Edge (headless mode).
# Markdown source: docs/restaurant-user-guide.md (Vietnamese, with embedded images).

param([string]$Engine = 'auto')

$ErrorActionPreference = 'Stop'
$md = Join-Path $PSScriptRoot '..\..\docs\restaurant-user-guide.md'
$pdf = Join-Path $PSScriptRoot '..\..\README.pdf'

if (-not (Test-Path $md)) {
    Write-Error "Missing source: $md"
    exit 1
}

function Get-Tool([string]$name) {
    return (Get-Command $name -ErrorAction SilentlyContinue)
}

if ($Engine -eq 'auto') {
    if (Get-Tool pandoc) { $Engine = 'pandoc' }
    elseif (Get-Tool wkhtmltopdf) { $Engine = 'wkhtmltopdf' }
    elseif (Get-Tool 'msedge') { $Engine = 'edge' }
    else {
        Write-Error "No PDF engine found. Install pandoc, wkhtmltopdf, or set Engine explicitly."
        exit 1
    }
}

Write-Host "Using engine: $Engine"

switch ($Engine) {
    'pandoc' { & pandoc $md -o $pdf --pdf-engine=xelatex --toc -V geometry:margin=2cm }
    'wkhtmltopdf' { & wkhtmltopdf $md $pdf }
    'edge' { & msedge --headless --disable-gpu --print-to-pdf=$pdf "file:///$md" }
    default { Write-Error "Unknown engine: $Engine"; exit 1 }
}

if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host "Wrote $pdf"