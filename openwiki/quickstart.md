# OpenWiki Quickstart For Codex

## Future Session Order

1. Read `AGENTS.md`.
2. Read `openwiki/index.md`.
3. Read this quickstart.
4. Check CodeGraph with `codegraph status --json`.
5. Use `codegraph explore "<focused question>"` for symbol-level navigation before broad `rg`.
6. Read targeted source/config/resource files directly before editing.
7. Validate with the narrowest documented Gradle or script command that matches the change.

## OpenWiki Update Rule

Run `openwiki --update --print` after changes that affect architecture, registries, workflows, constraints, public behavior, important file layout, generated metadata, or onboarding instructions.

Do not run OpenWiki for trivial formatting-only changes.

## Current OpenWiki CLI State

The requested `npm install -g openwiki` and `openwiki --init` could not complete in this shell because the `npm` and `openwiki` commands are not available on `PATH`. See [OpenWiki Setup](openwiki-setup.md) for the exact setup requirement.

## Useful Repository Commands

From the repository root:

```powershell
codegraph status --json
codegraph explore "Identify the architecture and impact area for <change>"
.\gradlew.bat -PbuildDir=build-codex check --no-daemon
.\gradlew.bat -PbuildDir=build-codex clean build --no-daemon
.\install-mod.ps1 -BuildDir build-codex
```

Use redirected build directories such as `build-codex` if the default `build/` directory is locked or affected by the Windows OneDrive/reparse-point caveat in `AGENTS.md`.
