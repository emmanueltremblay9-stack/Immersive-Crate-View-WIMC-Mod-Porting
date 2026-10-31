package com.emmanueltremblay.wimc.qa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InstallScriptQa {
    private InstallScriptQa() {
    }

    public static void main(String[] args) throws IOException {
        Path root = args.length > 0 ? Path.of(args[0]) : Path.of("").toAbsolutePath();
        Path scriptPath = root.resolve("install-mod.ps1");
        Path serverSmokePath = root.resolve("scripts/smoke-runserver.ps1");
        Path clientSmokePath = root.resolve("scripts/smoke-runclient.ps1");
        Path localEnvExamplePath = root.resolve(".codex/local.env.example");
        Path gitignorePath = root.resolve(".gitignore");
        String script = Files.readString(scriptPath);
        String gitignore = Files.readString(gitignorePath);

        require(Files.isRegularFile(scriptPath), "install-mod.ps1 is missing");
        validateSmokeScript(serverSmokePath, "dedicated-server");
        validateSmokeScript(clientSmokePath, "client");
        require(Files.isRegularFile(localEnvExamplePath), ".codex/local.env.example is missing");
        require(Files.isRegularFile(gitignorePath), ".gitignore is missing");
        require(
                gitignore.lines().anyMatch(line -> line.trim().equals(".codex/local.env")),
                ".gitignore must ignore machine-local .codex/local.env install overrides"
        );
        require(script.contains("[Parameter(Mandatory = $true)]\r\n    [string]$BuildDir")
                || script.contains("[Parameter(Mandatory = $true)]\n    [string]$BuildDir"), "BuildDir must be mandatory to avoid stale jar installs");
        require(!script.contains("build-codex-022"), "install script must not default to a stale redirected build directory");
        require(script.contains("CODEX_MINECRAFT_MODS_DIR"), "install script must support CODEX_MINECRAFT_MODS_DIR");
        require(
                script.lines().anyMatch(line ->
                        line.contains("$env:CODEX_MINECRAFT_MODS_DIR")
                                && line.contains("Trim().Trim")
                                && (line.contains("Trim('\"')") || line.contains("Trim(\"\\\"\")"))
                                && line.contains("Trim(\"'\")")
                ),
                "install script should trim quoted CODEX_MINECRAFT_MODS_DIR values"
        );
        require(script.contains(".codex"), "install script must support .codex/local.env");
        require(script.contains("Resolve-ReportPath"), "install script must support explicit report paths for QA");
        require(script.contains("Resolve-BuildDir"), "install script must resolve BuildDir through a dedicated helper");
        require(script.contains("[System.IO.Path]::IsPathRooted($normalizedBuildDir)"), "install script must support absolute BuildDir values");
        require(script.contains("Resolve-RootRelativePath"), "install script must resolve relative paths from the script root");
        require(script.contains("Resolve-RootRelativePath -Path $resolvedModsDirInput -Root $root"), "ModsDir resolution must not depend on caller working directory");
        require(!script.contains("$resolvedModsDir = (Resolve-Path -LiteralPath $resolvedModsDirInput).Path"), "ModsDir must not resolve relative to the caller working directory");
        require(!script.contains("Join-Path (Join-Path $root $BuildDir) \"libs\""), "install script must not nest absolute BuildDir values under the repo root");
        require(!script.contains("Resolve-Path -LiteralPath (Join-Path $root $BuildDir)"), "install report must not nest absolute BuildDir values under the repo root");
        require(script.contains("mod_legacy_ids"), "install script must remove old jars that used legacy mod ids");
        require(script.contains("[string[]]$ModIds"), "install script jar matching must accept current and legacy mod ids");
        require(script.contains("mod_archive_name"), "install script must read the archive name used for renamed runtime jars");
        require(script.contains("[string[]]$JarNamePrefixes"), "install script jar matching must accept archive-name filename prefixes");
        require(script.contains("(?:\\d+\\.)*\\d+\\b.*\\.jar$"), "install script must require a version-like suffix for filename-prefix cleanup matches");
        require(script.contains("Read-JarMetadata"), "install script should read metadata through a reusable helper.");
        require(script.contains("[System.IO.Compression.ZipFile]::OpenRead($JarPath)"), "install script should prefer direct jar inspection over external tar tooling.");
        require(script.contains("tar -xOf \"$JarPath\" \"$EntryPath\""), "install script should keep tar fallback for environments where ZIP API reads are unavailable.");
        require(script.contains("Trim('\"').Trim(\"'\")"), "install script should normalize quoted path arguments before resolving paths.");
        require(script.contains("MatchedJarNamePrefix"), "install reports should show filename-prefix cleanup matches");
        require(
                script.contains("if ($metadata.modId -and -not $matchedModId)"),
                "filename-prefix cleanup must not delete jars whose metadata proves a different mod id"
        );
        require(script.contains("$runtimeJars = @("), "runtime jar discovery must be array-wrapped under Set-StrictMode");
        require(!script.contains("$matches = @()"), "script must not shadow PowerShell's automatic $Matches variable");
        require(!script.contains("$matches +="), "script must not append to PowerShell's automatic $Matches variable");
        require(script.contains("(?i)^"), "case-insensitive WIMC jar filename regex should apply from the start of the pattern");
    }

    private static void validateSmokeScript(Path scriptPath, String label) throws IOException {
        require(Files.isRegularFile(scriptPath), label + " smoke script is missing");
        String script = Files.readString(scriptPath);

        require(script.contains("$logIsFresh"), label + " smoke script must track latest.log freshness");
        require(
                script.contains("$logItem.LastWriteTime -ge $startedAt.AddSeconds(-2)"),
                label + " smoke script must compare latest.log freshness against its own start time"
        );
        require(
                script.contains("logIsFresh = $logIsFresh"),
                label + " smoke script summary must expose latest.log freshness evidence"
        );
        require(
                script.contains("-not $logIsFresh"),
                label + " smoke script must fail instead of passing on stale latest.log content"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
