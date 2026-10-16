$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$pidFile = Join-Path $PSScriptRoot 'logs/server.pid'
if (-not (Test-Path -LiteralPath $pidFile)) { Write-Host 'No ERP process record found.'; exit 0 }
$erpPid = [int](Get-Content -LiteralPath $pidFile -Raw).Trim()
$erpProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $erpPid" -ErrorAction SilentlyContinue
$expectedJar = Join-Path $PSScriptRoot 'target/erp-training-1.0.0.jar'
if ($null -eq $erpProcess) { Write-Host 'ERP is already stopped.'; exit 0 }
if ($erpProcess.Name -ne 'java.exe' -or -not $erpProcess.CommandLine.Contains($expectedJar)) {
    throw 'The recorded process is not this ERP service. Nothing was stopped.'
}
Stop-Process -Id $erpPid
Write-Host 'ERP stopped. Business data remains in the data folder.'
