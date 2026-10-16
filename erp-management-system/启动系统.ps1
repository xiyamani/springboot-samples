param([int]$Port = 8087)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$javaExe = 'C:\Users\admin\Tools\jdk-21.0.7+6\bin\java.exe'
$mavenExe = 'C:\Users\admin\Tools\apache-maven-3.9.9\bin\mvn.cmd'
if (-not (Test-Path -LiteralPath $javaExe)) { $javaExe = (Get-Command java -ErrorAction Stop).Source }
if (-not (Test-Path -LiteralPath 'target/erp-training-1.0.0.jar')) {
    if (-not (Test-Path -LiteralPath $mavenExe)) { $mavenExe = (Get-Command mvn -ErrorAction Stop).Source }
    & $mavenExe -B package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. See the output above.' }
}
$occupied = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
if ($occupied) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/health" -TimeoutSec 3
        if ($health.status -eq 'UP' -and $health.application -eq 'hengxu-erp') {
            Start-Process "http://127.0.0.1:$Port"
            Write-Host "ERP already running: http://127.0.0.1:$Port"
            exit 0
        }
    } catch {}
    throw "Port $Port is occupied. Run this script with -Port 8088."
}
New-Item -ItemType Directory -Path 'logs' -Force | Out-Null
$jarPath = Join-Path $PSScriptRoot 'target/erp-training-1.0.0.jar'
$process = Start-Process -FilePath $javaExe -ArgumentList @('-Dfile.encoding=UTF-8','-jar',"`"$jarPath`"","--server.port=$Port") -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -RedirectStandardOutput 'logs/server.log' -RedirectStandardError 'logs/server-error.log' -PassThru
$process.Id | Set-Content -LiteralPath 'logs/server.pid'
$ready = $false
for ($attempt = 0; $attempt -lt 45; $attempt++) {
    if ($process.HasExited) { throw 'ERP failed to start. Check logs/server.log.' }
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/health" -TimeoutSec 2
        if ($health.status -eq 'UP') { $ready = $true; break }
    } catch {}
    Start-Sleep -Milliseconds 1000
}
if (-not $ready) { throw 'Startup timed out. Check logs/server.log.' }
Start-Process "http://127.0.0.1:$Port"
Write-Host "ERP running: http://127.0.0.1:$Port (PID $($process.Id))"
