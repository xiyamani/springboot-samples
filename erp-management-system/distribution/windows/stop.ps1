$ErrorActionPreference = 'Stop'
$statePath = Join-Path $PSScriptRoot 'logs\server.json'
$jarPath = Join-Path $PSScriptRoot 'target\erp-training-1.0.0.jar'
if (-not (Test-Path -LiteralPath $statePath)) {
    Write-Host 'This copy is not running.'
    exit 0
}
$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$server = Get-CimInstance Win32_Process -Filter "ProcessId = $([int]$state.pid)" -ErrorAction SilentlyContinue
if ($null -eq $server) {
    Write-Host 'ERP is already stopped.'
    exit 0
}
if ($server.Name -ne 'java.exe' -or -not $server.CommandLine -or
    -not $server.CommandLine.Contains($jarPath)) {
    throw 'The recorded process belongs to another application. Nothing was stopped.'
}
Stop-Process -Id ([int]$state.pid) -ErrorAction Stop
$process = Get-Process -Id ([int]$state.pid) -ErrorAction SilentlyContinue
if ($null -ne $process -and -not $process.WaitForExit(10000)) {
    throw 'ERP is still stopping. Try again before moving the data folder.'
}
Write-Host 'ERP stopped. Business records remain in the data folder.'
