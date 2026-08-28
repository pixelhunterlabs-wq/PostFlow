[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$StartScript = Join-Path $PSScriptRoot "Start-PostFlow-Local.ps1"

function Assert-GitCleanOrExplain {
    Push-Location $RepoRoot
    try {
        $inside = (& git rev-parse --is-inside-work-tree 2>$null).Trim()
        if ($inside -ne "true") {
            throw "Bu PostFlow klasörü Git deposu değil. GitHub sürümünü bu klasöre klonlayın veya mevcut repo içinden çalıştırın."
        }

        $changes = @(& git status --porcelain)
        if ($changes.Count -gt 0) {
            Write-Host "Yerel değişiklikler bulundu. Kaybolmamaları için otomatik güncelleme durduruldu." -ForegroundColor Yellow
            Write-Host "Aşağıdaki dosyaları önce commit/push edin veya yedekleyin:" -ForegroundColor Yellow
            $changes | ForEach-Object { Write-Host "  $_" }
            throw "Yerel değişiklikler varken git pull yapılmadı."
        }
    } finally {
        Pop-Location
    }
}

try {
    if (-not (Get-Command git.exe -ErrorAction SilentlyContinue)) {
        throw "Git bulunamadı. Git for Windows kurulu olmalı."
    }

    Assert-GitCleanOrExplain

    Push-Location $RepoRoot
    try {
        Write-Host "PostFlow güncelleniyor..." -ForegroundColor Cyan
        & git fetch origin main
        if ($LASTEXITCODE -ne 0) { throw "GitHub güncellemesi alınamadı." }

        & git pull --ff-only origin main
        if ($LASTEXITCODE -ne 0) { throw "PostFlow fast-forward güncellenemedi." }

        Write-Host "PostFlow güncel." -ForegroundColor Green
    } finally {
        Pop-Location
    }

    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $StartScript
    exit $LASTEXITCODE
} catch {
    Write-Error "PostFlow güncellenip başlatılamadı: $($_.Exception.Message)"
    exit 1
}
