[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$LogRoot = "C:\PostFlow\logs"
$DefaultPortableRoot = "C:\PostFlow\MoneyPrinterTurbo"
$KnownSourceRoot = "C:\PostFlow\MoneyPrinterTurbo-PostFlow"
$OllamaModel = "qwen2.5:3b"
New-Item -ItemType Directory -Force -Path $LogRoot | Out-Null
$LogPath = Join-Path $LogRoot "postflow-local-$((Get-Date).ToString('yyyyMMdd-HHmmss')).log"
Start-Transcript -Path $LogPath -Append | Out-Null

function Test-MptHealth([int]$Port) {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/ping" -TimeoutSec 3 -UseBasicParsing
        return $response.StatusCode -eq 200 -and $response.Content.Trim().Trim('"') -eq "pong"
    } catch { return $false }
}

function Get-RunningMptPort {
    foreach ($port in @(8080, 8081, 8000)) {
        if (Test-MptHealth $port) { return $port }
    }
    return 0
}

function Resolve-Python([string]$SourceRoot, [string]$PortableRoot) {
    $candidates = @(
        (Join-Path $PortableRoot "lib\python\python.exe"),
        (Join-Path $SourceRoot ".venv\Scripts\python.exe"),
        (Join-Path $SourceRoot "venv\Scripts\python.exe")
    )
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate }
    }
    $python = Get-Command python.exe -ErrorAction SilentlyContinue
    if ($python) { return $python.Source }
    $py = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($py) { return $py.Source }
    return $null
}

function Get-MptInstallation {
    $roots = [System.Collections.Generic.List[string]]::new()
    if ($env:POSTFLOW_MPT_ROOT) { $roots.Add($env:POSTFLOW_MPT_ROOT) }
    $roots.Add($KnownSourceRoot)
    $roots.Add((Join-Path $DefaultPortableRoot "MoneyPrinterTurbo"))
    $roots.Add($DefaultPortableRoot)

    foreach ($sourceRoot in ($roots | Select-Object -Unique)) {
        if (!(Test-Path -LiteralPath $sourceRoot)) { continue }
        $main = Join-Path $sourceRoot "main.py"
        if (!(Test-Path -LiteralPath $main)) { continue }
        $portableRoot = if ($sourceRoot -like "$DefaultPortableRoot*") { $DefaultPortableRoot } else { $sourceRoot }
        $python = Resolve-Python -SourceRoot $sourceRoot -PortableRoot $portableRoot
        if (!$python) { continue }
        return [pscustomobject]@{
            SourceRoot = $sourceRoot
            PortableRoot = $portableRoot
            Python = $python
            ApiMain = $main
            Config = Join-Path $sourceRoot "config.toml"
        }
    }
    return $null
}

function Start-MptApi($installation) {
    Write-Host "MoneyPrinterTurbo API başlatılıyor: $($installation.SourceRoot)"
    $pythonArgs = if ([System.IO.Path]::GetFileName($installation.Python).ToLowerInvariant() -eq "py.exe") { "-3 main.py" } else { "main.py" }
    $command = "set `"PYTHONPATH=$($installation.SourceRoot)`"&& `"$($installation.Python)`" $pythonArgs"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $command -WorkingDirectory $installation.SourceRoot
}

function Wait-MptApi {
    $deadline = (Get-Date).AddSeconds(120)
    do {
        $port = Get-RunningMptPort
        if ($port -gt 0) { return $port }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "MPT API 120 saniye içinde hazır olmadı. Açık MPT terminalini ve $LogPath dosyasını kontrol edin."
}

function Test-OllamaHealth {
    try {
        $response = Invoke-WebRequest -Uri "http://127.0.0.1:11434/api/tags" -TimeoutSec 3 -UseBasicParsing
        return $response.StatusCode -eq 200
    } catch { return $false }
}

function Start-OllamaIfNeeded {
    if (Test-OllamaHealth) { return }
    $ollama = Get-Command ollama.exe -ErrorAction SilentlyContinue
    if (!$ollama) {
        $known = Join-Path $env:LOCALAPPDATA "Programs\Ollama\ollama.exe"
        if (Test-Path -LiteralPath $known) { $ollama = Get-Item -LiteralPath $known }
    }
    if (!$ollama) { throw "Ollama bulunamadı. Ollama'yı kurun veya PATH'e ekleyin." }
    Start-Process -FilePath $ollama.Source -ArgumentList "serve" -WindowStyle Hidden
    $deadline = (Get-Date).AddSeconds(30)
    do {
        if (Test-OllamaHealth) { return }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Ollama 11434 portunda hazır olmadı."
}

function Ensure-OllamaModel {
    $tags = Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/tags" -TimeoutSec 5
    $names = @($tags.models | ForEach-Object { $_.name })
    if ($names -contains $OllamaModel -or $names | Where-Object { $_ -like "$OllamaModel*" }) {
        Write-Host "Ollama modeli hazır: $OllamaModel"
        return
    }
    $ollama = Get-Command ollama.exe -ErrorAction SilentlyContinue
    if (!$ollama) {
        $known = Join-Path $env:LOCALAPPDATA "Programs\Ollama\ollama.exe"
        if (Test-Path -LiteralPath $known) { $ollama = Get-Item -LiteralPath $known }
    }
    if (!$ollama) { throw "Ollama modeli eksik ve ollama.exe bulunamadı." }
    Write-Host "$OllamaModel modeli indiriliyor. İlk kurulumda birkaç dakika sürebilir…"
    & $ollama.Source pull $OllamaModel
    if ($LASTEXITCODE -ne 0) { throw "$OllamaModel modeli indirilemedi." }
}

function Ensure-NodeModules {
    $nodeModules = Join-Path $RepoRoot "node_modules"
    if (Test-Path -LiteralPath $nodeModules) { return }
    Write-Host "PostFlow web bağımlılıkları kuruluyor…"
    Push-Location $RepoRoot
    try {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw "npm ci başarısız oldu." }
    } finally { Pop-Location }
}

try {
    Write-Host "PostFlow yerel üretim zinciri hazırlanıyor…"
    Start-OllamaIfNeeded
    Ensure-OllamaModel

    $mptPort = Get-RunningMptPort
    if ($mptPort -eq 0) {
        $installation = Get-MptInstallation
        if (!$installation) {
            if (!(Test-Path -LiteralPath $DefaultPortableRoot)) {
                Write-Host "MoneyPrinterTurbo bulunamadı; resmi taşınabilir paket kuruluyor…"
                $installer = Start-Process -FilePath "powershell.exe" -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $PSScriptRoot "Install-MoneyPrinterTurbo.ps1"), "-InstallRoot", $DefaultPortableRoot -Wait -PassThru
                if ($installer.ExitCode -ne 0) { throw "MPT kurulumu başarısız oldu." }
            }
            $installation = Get-MptInstallation
        }
        if (!$installation) { throw "MoneyPrinterTurbo kurulumu bulundu ancak çalıştırılabilir main.py/Python eşleşmesi çözülemedi." }
        Start-MptApi $installation
        $mptPort = Wait-MptApi
    }

    Ensure-NodeModules
    Write-Host "MPT hazır: http://127.0.0.1:$mptPort"
    Write-Host "Ollama hazır: http://127.0.0.1:11434 · $OllamaModel"

    $nextCommand = "set POSTFLOW_LOCAL_RUNTIME=1&& set POSTFLOW_MPT_API=http://127.0.0.1:$mptPort&& npm.cmd run dev"
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k", $nextCommand -WorkingDirectory $RepoRoot
    $deadline = (Get-Date).AddSeconds(60)
    do {
        try {
            $postflow = Invoke-WebRequest -Uri "http://127.0.0.1:3000" -TimeoutSec 3 -UseBasicParsing
            if ($postflow.StatusCode -eq 200) {
                Start-Process "http://127.0.0.1:3000"
                Write-Host "PostFlow hazır: http://127.0.0.1:3000"
                exit 0
            }
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
