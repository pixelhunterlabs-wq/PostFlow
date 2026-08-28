[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$MptRoot = "C:\PostFlow\MoneyPrinterTurbo"
$LogRoot = "C:\PostFlow\logs"
New-Item -ItemType Directory -Force -Path $LogRoot | Out-Null
$LogPath = Join-Path $LogRoot "postflow-local-$((Get-Date).ToString('yyyyMMdd-HHmmss')).log"
Start-Transcript -Path $LogPath -Append | Out-Null

function Test-MptHealth([int]$Port) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/ping" -TimeoutSec 3 -UseBasicParsing
        return $response.StatusCode -eq 200 -and $response.Content.Trim() -eq "pong"
    } catch { return $false }
}

function Get-MptApiPort {
    $candidatePorts = [System.Collections.Generic.List[int]]::new()
    $candidatePorts.Add(8080)
    $config = Get-ChildItem -LiteralPath $MptRoot -Filter *.toml -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($config) {
        $match = Select-String -LiteralPath $config.FullName -Pattern "^\s*listen_port\s*=\s*(\d+)" | Select-Object -First 1
        if ($match) { $candidatePorts.Add([int]$match.Matches[0].Groups[1].Value) }
    }
    foreach ($port in ($candidatePorts | Select-Object -Unique)) { if (Test-MptHealth $port) { return $port } }
    return 0
}

function Start-Mpt {
    $startBat = Get-ChildItem -LiteralPath $MptRoot -Filter start.bat -File -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (!$startBat) { throw "MPT start.bat bulunamadı: $MptRoot" }
    $command = "call `"$($startBat.FullName)`""
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $command -WorkingDirectory $startBat.Directory.FullName
}

function Wait-MptApi {
    $deadline = (Get-Date).AddSeconds(90)
    do {
        $port = Get-MptApiPort
        if ($port -gt 0) { return $port }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "MPT API /ping 90 saniye içinde hazır olmadı. Açık MPT terminalini ve $LogPath dosyasını kontrol edin."
}

try {
    if (!(Test-Path -LiteralPath $MptRoot)) {
        $installer = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "Install-MoneyPrinterTurbo.ps1"), "-InstallRoot", $MptRoot -Wait -PassThru -WindowStyle Hidden
        if ($installer.ExitCode -ne 0) { throw "MPT kurulumu başarısız oldu." }
    }
    $mptPort = Get-MptApiPort
    if ($mptPort -eq 0) {
        Write-Host "MoneyPrinterTurbo başlatılıyor…"
        Start-Mpt
        $mptPort = Wait-MptApi
    }

    $nextCommand = "set POSTFLOW_LOCAL_RUNTIME=1&& set POSTFLOW_MPT_API=http://127.0.0.1:$mptPort&& npm.cmd run dev"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $nextCommand -WorkingDirectory $RepoRoot
    $deadline = (Get-Date).AddSeconds(45)
    do {
        try {
            $postflow = Invoke-WebRequest -Uri "http://127.0.0.1:3000" -TimeoutSec 3 -UseBasicParsing
            if ($postflow.StatusCode -eq 200) { Start-Process "http://127.0.0.1:3000"; Write-Host "PostFlow hazır."; exit 0 }
        } catch {}
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "PostFlow 3000 portunda hazır olmadı. Açık PostFlow terminalini ve $LogPath dosyasını kontrol edin."
} catch {
    Write-Error "Yerel PostFlow başlatılamadı: $($_.Exception.Message)"
    exit 1
} finally {
    Stop-Transcript | Out-Null
}
