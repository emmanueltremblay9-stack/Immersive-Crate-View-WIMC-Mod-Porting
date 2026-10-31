# Engineer's Crate Preview 0.1.39

Initial NeoForge 1.21.1 / Java 21 release candidate of Engineer's Crate Preview, a modern client-only tooltip inventory preview inspired by What's In My Crate (WIMC).

## Added

- Client-only crate inventory previews in item tooltips.
- Support for Immersive Engineering crates and reinforced crates when Immersive Engineering is installed.
- Support for opt-in compatible industrial crates through the `immersive_crate_view_wimc_mod_porting:industrial_crates` item tag.
- Modern NeoForge tooltip rendering using `RenderTooltipEvent.GatherComponents`, `TooltipComponent`, `ClientTooltipComponent`, `RegisterClientTooltipComponentFactoriesEvent`, and `GuiGraphics`.
- 9-wide grid preview with item icons, stack counts, empty slot display, and configurable row limit.
- Client config options:
  - `enableCratePreview`
  - `requireShiftForPreview`
  - `maxPreviewRows`
  - `showEmptySlots`
  - `enableImmersiveEngineeringCrates`
  - `enableOwnModCrates`

## Safety And Compatibility

- No server packets are used for previews.
- Dedicated server startup is safe; client GUI classes are isolated behind the client-only entrypoint.
- Immersive Engineering is optional. The mod will not crash if IE is absent.
- Empty, malformed, missing, loot-table-only, and unusual crate data are handled defensively.
- Loot-table crate contents are shown as unresolved instead of being generated client-side.

## Fixes Included Before First Upload

- Fixed Shift-required mode so the hint appears only on supported crate stacks.
- Fixed hidden-slot counting when empty slots are hidden.
- Fixed vanilla container extraction by using `ItemContainerContents.copyInto(...)`.
- Fixed empty container data hiding unresolved loot data.
- Fixed block-entity loot-table detection when an empty or malformed `Items` tag is present.
- Fixed block-entity loot-table detection when an item entry has an id and slot but no stack count.
- Fixed fallback precedence so non-empty block-entity or item-capability inventory can override an empty container component.
- Bounded item capability scans to avoid excessive hover-time allocations from malformed handlers.
- Prevented broken item icon rendering from crashing the whole tooltip.

## Tested

- `./gradlew.bat -PbuildDir=build-codex check --no-daemon`
- `./gradlew.bat -PbuildDir=build-codex clean build --no-daemon`
- Lightweight QA tasks: `qaPreviewModel`, `qaTooltipLayout`, `qaTooltipEvents`, `qaInstallScript`, `qaReleaseDocs`, and `qaManualResources`.
- Prism LAB install verification uses `install-mod.ps1` plus source/installed SHA-256 comparison and one remaining same-mod jar.

## Attribution

Inspired by What's In My Crate (WIMC), originally by redcatone.

Original WIMC license: MIT.

No original WIMC source code or assets are copied in this release.
