[CmdletBinding()]
param(
    [string]$InstallRoot = "C:\PostFlow\MoneyPrinterTurbo"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Version = "1.3.5"
$ArchiveUrl = "https://github.com/harry0703/MoneyPrinterTurbo/releases/download/v1.3.5/MoneyPrinterTurbo-Portable-Windows-1.3.5.7z"
$ExpectedBytes = 944516031L
$ExpectedSha256 = "cdb136b0ebacb1cdc46d1853fbc99b8c67ee715a407c46147631922aabacb9d7"
$DownloadRoot = "C:\PostFlow\downloads"
$LogRoot = "C:\PostFlow\logs"
$ArchivePath = Join-Path $DownloadRoot "MoneyPrinterTurbo-Portable-Windows-$Version.7z"

New-Item -ItemType Directory -Force -Path $DownloadRoot, $LogRoot | Out-Null
$LogPath = Join-Path $LogRoot "mpt-install-$((Get-Date).ToString('yyyyMMdd-HHmmss')).log"
Start-Transcript -Path $LogPath -Append | Out-Null

function Get-ArchiveTool {
    $command = Get-Command 7z.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $command = Get-Command 7z -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $knownPath = "C:\Program Files\7-Zip\7z.exe"
    if (Test-Path -LiteralPath $knownPath) { return $knownPath }
    return $null
}

function Test-ArchiveFile([string]$Path) {
    if (!(Test-Path -LiteralPath $Path)) { return $false }
    $item = Get-Item -LiteralPath $Path
    if ($item.Length -ne $ExpectedBytes) { throw "İndirilen MPT arşiv boyutu doğrulanamadı (beklenen $ExpectedBytes, bulunan $($item.Length))." }
    $header = [System.IO.File]::ReadAllBytes($Path)[0..5]
    $sevenZipMagic = 0x37,0x7A,0xBC,0xAF,0x27,0x1C
    if (-not (@(Compare-Object $header $sevenZipMagic -SyncWindow 0).Count -eq 0)) { throw "İndirilen dosya geçerli bir 7z arşivi değil." }
    $hash = (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $ExpectedSha256) { throw "MPT arşiv SHA-256 doğrulaması başarısız oldu." }
    $sevenZip = Get-ArchiveTool
    if ($sevenZip) {
        & $sevenZip t $Path | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "7z arşiv testi başarısız oldu." }
        return $true
    }
    $tar = Get-Command tar.exe -ErrorAction SilentlyContinue
    if ($tar) {
        & $tar.Source -tf $Path | Out-Null
        if ($LASTEXITCODE -eq 0) { return $true }
    }
    throw "Arşivi test etmek için 7-Zip veya Windows tar.exe bulunamadı. 7-Zip kurun ve scripti yeniden başlatın."
}

try {
    if (Test-Path -LiteralPath $InstallRoot) {
        $existingStart = Get-ChildItem -LiteralPath $InstallRoot -Filter start.bat -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($existingStart) {
            Write-Host "MoneyPrinterTurbo zaten kurulu: $($existingStart.FullName)"
            exit 0
        }
        throw "Kurulum klasörü mevcut ancak start.bat bulunamadı: $InstallRoot. Güvenli olarak üzerine yazılmadı."
    }

    if (!(Test-Path -LiteralPath $ArchivePath)) {
        $partialPath = "$ArchivePath.part"
        Write-Host "MoneyPrinterTurbo v$Version indiriliyor…"
        try { Start-BitsTransfer -Source $ArchiveUrl -Destination $partialPath -ErrorAction Stop }
        catch { Invoke-WebRequest -Uri $ArchiveUrl -OutFile $partialPath -UseBasicParsing }
        Move-Item -LiteralPath $partialPath -Destination $ArchivePath
    }
    Test-ArchiveFile $ArchivePath | Out-Null

    $sevenZip = Get-ArchiveTool
    $stageRoot = Join-Path "C:\PostFlow" "MoneyPrinterTurbo-staging-$([guid]::NewGuid().ToString('N'))"
    New-Item -ItemType Directory -Force -Path $stageRoot | Out-Null
    if ($sevenZip) {
        & $sevenZip x $ArchivePath "-o$stageRoot" -y | Out-Host
        if ($LASTEXITCODE -ne 0) { throw "MPT arşivi çıkarılamadı." }
    } else {
        $tar = Get-Command tar.exe -ErrorAction Stop
        & $tar.Source -xf $ArchivePath -C $stageRoot
        if ($LASTEXITCODE -ne 0) { throw "MPT arşivi tar.exe ile çıkarılamadı." }
    }

    $start = Get-ChildItem -LiteralPath $stageRoot -Filter start.bat -File -Recurse | Select-Object -First 1
    if (!$start) { throw "Çıkarılan MPT paketinde start.bat bulunamadı." }
    $packageRoot = $start.Directory.FullName
    Move-Item -LiteralPath $packageRoot -Destination $InstallRoot
    $manifest = @{ version = $Version; archiveSha256 = $ExpectedSha256; installedAt = (Get-Date).ToString("o"); startBat = (Join-Path $InstallRoot "start.bat") } | ConvertTo-Json
    Set-Content -LiteralPath (Join-Path $InstallRoot "postflow-mpt-install.json") -Value $manifest -Encoding utf8
    Write-Host "MoneyPrinterTurbo hazır: $InstallRoot"
} catch {
    Write-Error "MoneyPrinterTurbo kurulumu tamamlanamadı: $($_.Exception.Message) Log: $LogPath"
    exit 1
} finally {
    Stop-Transcript | Out-Null
}
