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

function Get-MptInstallation {
    # The official portable v1.3.5 layout is deterministic. Do not recurse:
    # Python's own library tree contains unrelated main.py files (for example lib2to3).
    $sourceRoot = Join-Path $MptRoot "MoneyPrinterTurbo"
    $installation = [pscustomobject]@{
        PortableRoot = $MptRoot
        SourceRoot = $sourceRoot
        StartBat = Join-Path $MptRoot "start.bat"
        Python = Join-Path $MptRoot "lib\python\python.exe"
        ApiMain = Join-Path $sourceRoot "main.py"
        WebUiMain = Join-Path $sourceRoot "webui\Main.py"
        Config = Join-Path $sourceRoot "config.toml"
    }
    foreach ($path in @($installation.StartBat, $installation.Python, $installation.ApiMain, $installation.WebUiMain, $installation.Config)) {
        if (!(Test-Path -LiteralPath $path)) { throw "Geçerli MoneyPrinterTurbo v1.3.5 taşınabilir kurulumu bulunamadı. Eksik dosya: $path" }
    }
    return $installation
}

function Test-MptHealth([int]$Port) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/ping" -TimeoutSec 3 -UseBasicParsing
        return $response.StatusCode -eq 200 -and $response.Content.Trim().Trim('"') -eq "pong"
    } catch { return $false }
}

function Test-MptWebUi {
    return [bool](Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object { $_.LocalPort -ge 8501 -and $_.LocalPort -le 8599 } | Select-Object -First 1)
}

function Get-MptApiPort($installation) {
    $candidatePorts = [System.Collections.Generic.List[int]]::new()
    $candidatePorts.Add(8080)
    if (Test-Path -LiteralPath $installation.Config) {
        $match = Select-String -LiteralPath $installation.Config -Pattern "^\s*listen_port\s*=\s*(\d+)" | Select-Object -First 1
        if ($match) { $candidatePorts.Add([int]$match.Matches[0].Groups[1].Value) }
    }
    foreach ($port in ($candidatePorts | Select-Object -Unique)) { if (Test-MptHealth $port) { return $port } }
    return 0
}

function Start-MptWebUi($installation) {
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", "call `"$($installation.StartBat)`"" -WorkingDirectory $installation.PortableRoot
}

function Start-MptApi($installation) {
    # main.py is the API entrypoint at <portable>\MoneyPrinterTurbo\main.py.
    # Working from SourceRoot avoids the lib2to3 relative-import failure.
    $command = "set `"PYTHONPATH=$($installation.SourceRoot)`"&& call `"$($installation.Python)`" main.py"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $command -WorkingDirectory $installation.SourceRoot
}

function Wait-MptApi($installation) {
    $deadline = (Get-Date).AddSeconds(90)
    do {
        $port = Get-MptApiPort $installation
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
    $installation = Get-MptInstallation
    $mptPort = Get-MptApiPort $installation
    if ($mptPort -eq 0) {
        Write-Host "MoneyPrinterTurbo WebUI ve API başlatılıyor…"
        if (!(Test-MptWebUi)) { Start-MptWebUi $installation }
        Start-MptApi $installation
        $mptPort = Wait-MptApi $installation
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
