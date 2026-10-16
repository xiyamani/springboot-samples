param(
    [ValidateRange(1024, 65515)][int]$Port = 8087,
    [switch]$NoBrowser
)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$javaExe = Join-Path $PSScriptRoot 'runtime\bin\java.exe'
$jarPath = Join-Path $PSScriptRoot 'target\erp-training-1.0.0.jar'
$statePath = Join-Path $PSScriptRoot 'logs\server.json'
if (-not (Test-Path -LiteralPath $javaExe)) {
    throw 'Bundled Java is missing. Extract the entire ZIP before starting.'
}
if (-not (Test-Path -LiteralPath $jarPath)) {
    throw 'Application JAR is missing. Extract the entire ZIP before starting.'
}

function Test-Health([int]$ServerPort) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$ServerPort/api/health" -TimeoutSec 2
        return ($health.status -eq 'UP' -and $health.application -eq 'hengxu-erp')
    } catch { return $false }
}

if (Test-Path -LiteralPath $statePath) {
    $state = $null
    try { $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json } catch {}
    if ($null -ne $state) {
        $existing = Get-CimInstance Win32_Process -Filter "ProcessId = $([int]$state.pid)" -ErrorAction SilentlyContinue
        if ($null -ne $existing -and $existing.Name -eq 'java.exe' -and
            $existing.CommandLine -and $existing.CommandLine.Contains($jarPath)) {
            if (-not (Test-Health ([int]$state.port))) {
                throw 'This copy is already starting or unavailable. Check logs\server.log.'
            }
            $url = "http://127.0.0.1:$($state.port)/"
            if (-not $NoBrowser) { Start-Process $url }
            Write-Host "ERP already running: $url"
            exit 0
        }
    }
}

# Each extracted copy owns its database. Use a free port when another copy is running.
$selectedPort = $null
foreach ($candidate in $Port..($Port + 20)) {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $candidate)
    try {
        $listener.Start()
        $selectedPort = $candidate
        break
    } catch [System.Net.Sockets.SocketException] {
    } finally { $listener.Stop() }
}
if ($null -eq $selectedPort) { throw 'No free local port. Close other ERP copies and try again.' }

New-Item -ItemType Directory -Path (Join-Path $PSScriptRoot 'logs') -Force | Out-Null
$arguments = @(
    '-Dfile.encoding=UTF-8', '-jar', "`"$jarPath`"",
    "--server.port=$selectedPort", '--server.address=127.0.0.1',
    '--spring.datasource.url=jdbc:h2:file:./data/erp;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE',
    '--spring.datasource.username=sa', '--spring.datasource.password=',
    '--erp.kingdee.mode=MOCK', '--erp.integrations.base-url='
)
$server = Start-Process -FilePath $javaExe -ArgumentList $arguments -WorkingDirectory $PSScriptRoot `
    -WindowStyle Hidden -RedirectStandardOutput (Join-Path $PSScriptRoot 'logs\server.log') `
    -RedirectStandardError (Join-Path $PSScriptRoot 'logs\server-error.log') -PassThru
@{ pid = $server.Id; port = $selectedPort; jar = $jarPath } |
    ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding UTF8
$ready = $false
for ($attempt = 0; $attempt -lt 45; $attempt++) {
    $server.Refresh()
    if ($server.HasExited) { throw 'ERP failed to start. Check logs\server.log and logs\server-error.log.' }
    if (Test-Health $selectedPort) { $ready = $true; break }
    Start-Sleep -Milliseconds 700
}
if (-not $ready) { throw 'Startup timed out. Check logs\server.log, then run start again.' }
$url = "http://127.0.0.1:$selectedPort/"
if (-not $NoBrowser) { Start-Process $url }
Write-Host "ERP running: $url"
Write-Host 'Keep the data folder to preserve your business records.'
