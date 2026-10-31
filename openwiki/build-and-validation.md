# Build And Validation

## Build System

- Wrapper: `gradlew.bat` on Windows.
- Gradle version: 8.8, configured in `build.gradle`.
- Java toolchain: Java 21.
- NeoForge ModDev plugin: `net.neoforged.moddev` version `2.0.141`.

## Common Commands

Use PowerShell from the repository root.

```powershell
.\gradlew.bat -PbuildDir=build-codex check --no-daemon
.\gradlew.bat -PbuildDir=build-codex clean build --no-daemon
```

The `-PbuildDir=build-codex` pattern is preferred when the default `build/` directory is locked or affected by OneDrive/reparse-point behavior.

## QA Tasks

`check` depends on:

- `qaPreviewModel`
- `qaManualResources`
- `qaTooltipLayout`
- `qaTooltipEvents`
- `qaInstallScript`
- `qaReleaseDocs`

These are JavaExec QA harnesses under `src/qa/java`.

## Runtime Runs

`build.gradle` configures NeoForge runs for:

- `client`
- `server`
- `gameTestServer`
- `data`

The data run emits generated resources to `src/generated/resources/`.

## Jar Output

Runtime jars are written under the active Gradle build directory, usually:

```text
build-codex/libs/
```

Production jar selection must exclude sources, javadoc, dev, plain, test, tests, and API jars unless project documentation explicitly says otherwise.

## Install Script

`install-mod.ps1` installs a verified runtime jar into a configured Minecraft `mods` directory. It reads mod id, legacy ids, archive name, display name, and version from `gradle.properties`; selects exactly one runtime jar from the provided build directory; removes old same-mod jars; copies one new jar; compares SHA-256 hashes; and writes `build/install-report.json` by default.

For OpenWiki-only documentation setup, do not perform clean-install validation proof unless Emmanuel explicitly requests a Minecraft runtime install pass.
