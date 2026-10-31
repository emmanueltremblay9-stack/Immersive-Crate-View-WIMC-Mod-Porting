param(
    [string]$BuildDir = "build-codex-phase1",
    [int]$TimeoutSeconds = 180,
    [string]$LogDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
if ([string]::IsNullOrWhiteSpace($LogDir)) {
    $LogDir = Join-Path $root "logs\phase1"
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$stdoutPath = Join-Path $LogDir "runclient-smoke-$timestamp.stdout.log"
$stderrPath = Join-Path $LogDir "runclient-smoke-$timestamp.stderr.log"
$summaryPath = Join-Path $LogDir "runclient-smoke-$timestamp.summary.json"
$minecraftLogPath = Join-Path $root "run\logs\latest.log"
$startedAt = Get-Date

function Get-ChildProcessIds {
    param([Parameter(Mandatory = $true)][int]$ParentProcessId)

    $children = @(Get-CimInstance Win32_Process | Where-Object { $_.ParentProcessId -eq $ParentProcessId })
    foreach ($child in $children) {
        Get-ChildProcessIds -ParentProcessId $child.ProcessId
        $child.ProcessId
    }
}

function Stop-SmokeProcessTree {
    param([Parameter(Mandatory = $true)][System.Diagnostics.Process]$Process)

    $ids = @(Get-ChildProcessIds -ParentProcessId $Process.Id) + $Process.Id
    foreach ($id in ($ids | Select-Object -Unique)) {
        try {
            Stop-Process -Id $id -Force -ErrorAction Stop
        } catch {
            # The process may already have exited after the startup state was observed.
        }
    }
}

$gradlew = Join-Path $root "gradlew.bat"
$arguments = @("-PbuildDir=$BuildDir", "runClient", "--no-daemon")
$process = Start-Process -FilePath $gradlew `
    -ArgumentList $arguments `
    -WorkingDirectory $root `
    -RedirectStandardOutput $stdoutPath `
    -RedirectStandardError $stderrPath `
    -WindowStyle Hidden `
    -PassThru

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$startupEvidence = $false
$modLoadedEvidence = $false
$clientTargetEvidence = $false
$clientRuntimeEvidence = $false
$hasErrors = $false
$logIsFresh = $false
$logText = ""

while ((Get-Date) -lt $deadline) {
    if ($process.HasExited) {
        break
    }

    if (Test-Path -LiteralPath $minecraftLogPath) {
        $logItem = Get-Item -LiteralPath $minecraftLogPath
        $logIsFresh = $logItem.LastWriteTime -ge $startedAt.AddSeconds(-2)
        if ($logIsFresh) {
            $logText = Get-Content -LiteralPath $minecraftLogPath -Raw
            $modLoadedEvidence = $logText -match "Engineer's Crate Preview"
            $clientTargetEvidence = $logText -match 'Launching target ''forgeclientdev''|Env=CLIENT'
            $clientRuntimeEvidence = $logText -match 'Backend library: LWJGL|Created: [0-9]+x[0-9]+x[0-9]+|OpenAL initialized|Sound engine started'
            $startupEvidence = $modLoadedEvidence -and $clientTargetEvidence -and $clientRuntimeEvidence
            $hasErrors = $logText -match 'ERROR|Exception|Caused by'

            if ($startupEvidence) {
                break
            }
        }
    }

    Start-Sleep -Milliseconds 500
}

if (-not $process.HasExited) {
    Stop-SmokeProcessTree -Process $process
}
$process.WaitForExit(30000) | Out-Null

if (Test-Path -LiteralPath $minecraftLogPath) {
    $logItem = Get-Item -LiteralPath $minecraftLogPath
    $logIsFresh = $logItem.LastWriteTime -ge $startedAt.AddSeconds(-2)
    if ($logIsFresh) {
        $logText = Get-Content -LiteralPath $minecraftLogPath -Raw
        $modLoadedEvidence = $logText -match "Engineer's Crate Preview"
        $clientTargetEvidence = $logText -match 'Launching target ''forgeclientdev''|Env=CLIENT'
        $clientRuntimeEvidence = $logText -match 'Backend library: LWJGL|Created: [0-9]+x[0-9]+x[0-9]+|OpenAL initialized|Sound engine started'
        $startupEvidence = $modLoadedEvidence -and $clientTargetEvidence -and $clientRuntimeEvidence
        $hasErrors = $logText -match 'ERROR|Exception|Caused by'
    } else {
        $startupEvidence = $false
        $modLoadedEvidence = $false
        $clientTargetEvidence = $false
        $clientRuntimeEvidence = $false
        $hasErrors = $false
        $logText = ""
    }
} else {
    $logIsFresh = $false
}

$summary = [ordered]@{
    command = ".\gradlew.bat $($arguments -join ' ')"
    script = $PSCommandPath
    buildDir = $BuildDir
    timeoutSeconds = $TimeoutSeconds
    stdoutPath = $stdoutPath
    stderrPath = $stderrPath
    minecraftLogPath = $minecraftLogPath
    processExitCodeAfterCleanup = if ($process.HasExited) { $process.ExitCode } else { $null }
    logIsFresh = $logIsFresh
    startupEvidence = $startupEvidence
    modLoadedEvidence = $modLoadedEvidence
    clientTargetEvidence = $clientTargetEvidence
    clientRuntimeEvidence = $clientRuntimeEvidence
    tooltipPixelProofAutomated = $false
    manualTooltipProofRequired = $true
    hasErrors = $hasErrors
    relevantLogLines = @(
        $logText -split "`r?`n" |
            Where-Object { $_ -match "Engineer's Crate Preview|forgeclientdev|Env=CLIENT|Backend library: LWJGL|Created: [0-9]+x[0-9]+x[0-9]+|OpenAL initialized|Sound engine started|ERROR|Exception|Caused by" }
    )
}

$summary | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $summaryPath -Encoding UTF8
$summary | ConvertTo-Json -Depth 5

if (-not $logIsFresh -or -not $startupEvidence -or $hasErrors) {
    exit 1
}

exit 0
