param(
    [string]$ModsDir = "",

    [Parameter(Mandatory = $true)]
    [string]$BuildDir,

    [string]$ReportPath = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Read-PropertiesFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    $properties = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }
        $index = $trimmed.IndexOf("=")
        if ($index -lt 1) {
            continue
        }
        $key = $trimmed.Substring(0, $index).Trim()
        $value = $trimmed.Substring($index + 1).Trim()
        $properties[$key] = $value
    }
    return $properties
}

function Read-NeoForgeMetadata {
    param([Parameter(Mandatory = $true)][string]$JarPath)

    $metadata = [ordered]@{
        modId = $null
        version = $null
        displayName = $null
    }

    $toml = Read-JarMetadata -JarPath $JarPath -EntryPath 'META-INF/neoforge.mods.toml'
    if ($null -eq $toml -or $toml.Count -eq 0) {
        return $metadata
    }

    foreach ($line in $toml) {
        if ($null -eq $metadata.modId -and $line -match '^\s*modId\s*=\s*"([^"]+)"') {
            $metadata.modId = $Matches[1]
        }
        if ($null -eq $metadata.version -and $line -match '^\s*version\s*=\s*"([^"]+)"') {
            $metadata.version = $Matches[1]
        }
        if ($null -eq $metadata.displayName -and $line -match '^\s*displayName\s*=\s*"([^"]+)"') {
            $metadata.displayName = $Matches[1]
        }
        if ($metadata.modId -and $metadata.version -and $metadata.displayName) {
            break
        }
    }

    return $metadata
}

function Read-JarMetadata {
    param(
        [Parameter(Mandatory = $true)][string]$JarPath,
        [Parameter(Mandatory = $true)][string]$EntryPath
    )

    $lines = @()
    $zip = $null

    try {
        $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
        $entry = $zip.Entries | Where-Object { $_.FullName -eq $EntryPath } | Select-Object -First 1
        if ($entry) {
            $stream = $entry.Open()
            try {
                $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
                try {
                    $lines = $reader.ReadToEnd() -split "`r?`n"
                } finally {
                    $reader.Dispose()
                }
            } finally {
                $stream.Dispose()
            }
        }
    } catch {
        # Fall back to tar if direct ZIP inspection fails.
    } finally {
        if ($zip) {
            $zip.Dispose()
        }
    }

    if ($lines.Count -gt 0) {
        return $lines
    }

    $tarOutput = & tar -xOf "$JarPath" "$EntryPath" 2>$null
    if ($tarOutput) {
        return @($tarOutput -split "`r?`n")
    }

    return @()
}

function Read-LocalEnvFile {
    param([Parameter(Mandatory = $true)][string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path)) {
        return $values
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#")) {
            continue
        }
        $index = $trimmed.IndexOf("=")
        if ($index -lt 1) {
            continue
        }
        $key = $trimmed.Substring(0, $index).Trim()
        $value = $trimmed.Substring($index + 1).Trim().Trim('"').Trim("'")
        $values[$key] = $value
    }

    return $values
}

function Resolve-ModsDir {
    param(
        [string]$ExplicitModsDir,
        [Parameter(Mandatory = $true)][string]$Root
    )

    if (-not [string]::IsNullOrWhiteSpace($ExplicitModsDir)) {
        $normalizedModsDir = $ExplicitModsDir.Trim().Trim('"').Trim("'")
        return $normalizedModsDir
    }
    if (-not [string]::IsNullOrWhiteSpace($env:CODEX_MINECRAFT_MODS_DIR)) {
        return $env:CODEX_MINECRAFT_MODS_DIR.Trim().Trim('"').Trim("'")
    }

    $localEnv = Read-LocalEnvFile -Path (Join-Path (Join-Path $Root ".codex") "local.env")
    if ($localEnv.ContainsKey("CODEX_MINECRAFT_MODS_DIR") -and -not [string]::IsNullOrWhiteSpace($localEnv["CODEX_MINECRAFT_MODS_DIR"])) {
        return $localEnv["CODEX_MINECRAFT_MODS_DIR"].Trim().Trim('"').Trim("'")
    }

    throw "No mods directory was provided. Pass -ModsDir, set CODEX_MINECRAFT_MODS_DIR, or add .codex/local.env."
}

function Resolve-RootRelativePath {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Root
    )

    $normalizedPath = $Path.Trim().Trim('"').Trim("'")

    if ([System.IO.Path]::IsPathRooted($normalizedPath)) {
        return (Resolve-Path -LiteralPath $normalizedPath).Path
    }
    return (Resolve-Path -LiteralPath (Join-Path $Root $normalizedPath)).Path
}

function Resolve-ReportPath {
    param(
        [string]$ExplicitReportPath,
        [Parameter(Mandatory = $true)][string]$Root
    )

    if ([string]::IsNullOrWhiteSpace($ExplicitReportPath)) {
        return Join-Path (Join-Path $Root "build") "install-report.json"
    }
    $normalizedReportPath = $ExplicitReportPath.Trim().Trim('"').Trim("'")
    if ([System.IO.Path]::IsPathRooted($normalizedReportPath)) {
        return $normalizedReportPath
    }
    return Join-Path $Root $normalizedReportPath
}

function Resolve-BuildDir {
    param(
        [Parameter(Mandatory = $true)][string]$ExplicitBuildDir,
        [Parameter(Mandatory = $true)][string]$Root
    )

    $normalizedBuildDir = $ExplicitBuildDir.Trim().Trim('"').Trim("'")
    if ([System.IO.Path]::IsPathRooted($normalizedBuildDir)) {
        return (Resolve-Path -LiteralPath $normalizedBuildDir).Path
    }
    return (Resolve-Path -LiteralPath (Join-Path $Root $normalizedBuildDir)).Path
}

function Get-ModJarMatches {
    param(
        [Parameter(Mandatory = $true)][string]$Directory,
        [Parameter(Mandatory = $true)][string[]]$ModIds,
        [Parameter(Mandatory = $true)][string[]]$JarNamePrefixes
    )

    $results = @()
    foreach ($jar in Get-ChildItem -LiteralPath $Directory -File -Filter "*.jar") {
        $metadata = Read-NeoForgeMetadata -JarPath $jar.FullName
        $matchedModId = $null
        foreach ($candidateModId in $ModIds) {
            if ($metadata.modId -eq $candidateModId) {
                $matchedModId = $candidateModId
                break
            }
        }

        $matchedJarNamePrefix = $null
        foreach ($candidatePrefix in $JarNamePrefixes) {
            $filenamePattern = "(?i)^" + [Regex]::Escape($candidatePrefix) + "[-_.](?:\d+\.)*\d+\b.*\.jar$"
            $filenameMatches = $jar.Name -match $filenamePattern
            if ($filenameMatches) {
                if ($metadata.modId -and -not $matchedModId) {
                    continue
                }
                $matchedJarNamePrefix = $candidatePrefix
                break
            }
        }

        if ($matchedModId -or $matchedJarNamePrefix) {
            $results += [pscustomobject]@{
                Path = $jar.FullName
                Name = $jar.Name
                Size = $jar.Length
                ModId = $metadata.modId
                Version = $metadata.version
                MatchedModId = $matchedModId
                MatchedJarNamePrefix = $matchedJarNamePrefix
            }
        }
    }
    return $results
}

$root = $PSScriptRoot
$resolvedModsDirInput = Resolve-ModsDir -ExplicitModsDir $ModsDir -Root $root
$propertiesPath = Join-Path $root "gradle.properties"
$properties = Read-PropertiesFile -Path $propertiesPath
$modId = $properties["mod_id"]
$legacyModIds = @()
if ($properties.ContainsKey("mod_legacy_ids") -and -not [string]::IsNullOrWhiteSpace($properties["mod_legacy_ids"])) {
    $legacyModIds = @($properties["mod_legacy_ids"].Split(",") |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}
$matchModIds = @($modId) + $legacyModIds
$modName = $properties["mod_name"]
$modArchiveName = $properties["mod_archive_name"]
$version = $properties["mod_version"]
$jarNamePrefixes = @($modId) + $legacyModIds
if (-not [string]::IsNullOrWhiteSpace($modArchiveName)) {
    $jarNamePrefixes += $modArchiveName
}
if (-not [string]::IsNullOrWhiteSpace($modName)) {
    $jarNamePrefixes += $modName
}
$jarNamePrefixes = @($jarNamePrefixes | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)

if ([string]::IsNullOrWhiteSpace($modId) -or [string]::IsNullOrWhiteSpace($version)) {
    throw "Could not read mod_id and mod_version from $propertiesPath"
}

$resolvedModsDir = Resolve-RootRelativePath -Path $resolvedModsDirInput -Root $root
if ((Split-Path -Leaf $resolvedModsDir) -ne "mods") {
    throw "Refusing to install outside a directory named mods: $resolvedModsDir"
}

$resolvedBuildDir = Resolve-BuildDir -ExplicitBuildDir $BuildDir -Root $root
$libsDir = Join-Path $resolvedBuildDir "libs"
if (-not (Test-Path -LiteralPath $libsDir)) {
    throw "Build libs directory does not exist: $libsDir"
}

$excludedJarName = '(?i)(sources|javadoc|dev|plain|test|api)\.jar$'
$runtimeJars = @(Get-ChildItem -LiteralPath $libsDir -File -Filter "*.jar" |
    Where-Object { $_.Name -notmatch $excludedJarName })

if ($runtimeJars.Count -ne 1) {
    throw "Expected exactly one runtime jar in $libsDir, found $($runtimeJars.Count): $($runtimeJars.Name -join ', ')"
}

$sourceJar = $runtimeJars[0]
$sourceMetadata = Read-NeoForgeMetadata -JarPath $sourceJar.FullName
if ($sourceMetadata.modId -ne $modId -or $sourceMetadata.version -ne $version) {
    throw "Built jar metadata mismatch. Expected ${modId}/${version}, found $($sourceMetadata.modId)/$($sourceMetadata.version)"
}

$oldMatches = @(Get-ModJarMatches -Directory $resolvedModsDir -ModIds $matchModIds -JarNamePrefixes $jarNamePrefixes)
$deletedOldJars = @()
foreach ($match in $oldMatches) {
    Remove-Item -LiteralPath $match.Path -Force
    $deletedOldJars += $match.Path
}

$targetJar = Join-Path $resolvedModsDir $sourceJar.Name
Copy-Item -LiteralPath $sourceJar.FullName -Destination $targetJar -Force

$sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $sourceJar.FullName).Hash
$targetHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $targetJar).Hash
$sourceSize = (Get-Item -LiteralPath $sourceJar.FullName).Length
$targetSize = (Get-Item -LiteralPath $targetJar).Length
$remainingMatches = @(Get-ModJarMatches -Directory $resolvedModsDir -ModIds $matchModIds -JarNamePrefixes $jarNamePrefixes)
$targetMetadata = Read-NeoForgeMetadata -JarPath $targetJar

$report = [ordered]@{
    timestamp = (Get-Date).ToString("o")
    modId = $modId
    legacyModIds = $legacyModIds
    jarNamePrefixes = $jarNamePrefixes
    modName = $modName
    version = $version
    buildDir = $resolvedBuildDir
    modsDir = $resolvedModsDir
    sourceJar = $sourceJar.FullName
    installedJar = $targetJar
    deletedOldJars = $deletedOldJars
    sourceSize = $sourceSize
    installedSize = $targetSize
    sourceSha256 = $sourceHash
    installedSha256 = $targetHash
    hashMatch = ($sourceHash -eq $targetHash)
    remainingJarsForMod = $remainingMatches.Count
    installedMetadata = $targetMetadata
}

if (-not $report.hashMatch) {
    throw "Installed jar hash does not match source jar"
}
if ($report.remainingJarsForMod -ne 1) {
    throw "Expected exactly one installed jar for $modId in $resolvedModsDir, found $($report.remainingJarsForMod)"
}
if ($targetMetadata.modId -ne $modId -or $targetMetadata.version -ne $version) {
    throw "Installed jar metadata mismatch. Expected ${modId}/${version}, found $($targetMetadata.modId)/$($targetMetadata.version)"
}

$reportPath = Resolve-ReportPath -ExplicitReportPath $ReportPath -Root $root
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $reportPath) | Out-Null
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportPath -Encoding UTF8
$report | ConvertTo-Json -Depth 8
