# Repository Architecture

This repository is a client-only NeoForge 1.21.1 mod that adds tooltip inventory previews for Immersive Engineering crates and compatible industrial crate items. It is inspired by redcatone's MIT-licensed What's In My Crate (WIMC), but the current implementation uses modern NeoForge tooltip component APIs.

## Entry Points

- `src/main/java/com/emmanueltremblay/wimc/Wimc.java`
  - Declares `Wimc.MOD_ID`.
  - Registers the client config only when the mod is loaded on the client distribution.
- `src/main/java/com/emmanueltremblay/wimc/client/WimcClient.java`
  - Client-only `@Mod(..., dist = Dist.CLIENT)` entrypoint.
  - Registers tooltip component factories on the mod event bus.
  - Registers tooltip gather behavior on `NeoForge.EVENT_BUS`.

## Main Packages

- `com.emmanueltremblay.wimc`
  - Mod constants, tags, and client config.
- `com.emmanueltremblay.wimc.client`
  - Tooltip events, layout math, and client rendering component.
- `com.emmanueltremblay.wimc.crate`
  - Crate detection, inventory extraction, preview contents, and status modeling.
- `com.emmanueltremblay.wimc.tooltip`
  - Common tooltip component payload passed from event logic to client rendering.
- `src/qa/java/com/emmanueltremblay/wimc`
  - Lightweight JavaExec QA entrypoints wired into Gradle `check`.

## Tooltip Flow

1. `WimcClient` registers client tooltip handlers.
2. `CratePreviewTooltipEvents.gatherTooltipComponents` checks client config and Shift-gating.
3. `CratePreviewDetector` determines whether the hovered stack is a supported crate.
4. `CrateInventoryExtractor` reads known inventory data from supported item/container representations.
5. `CratePreviewContents` models rows, columns, visible stacks, hidden counts, and status.
6. `CratePreviewTooltipComponent` carries the preview model.
7. `ClientCratePreviewTooltipComponent` renders the grid, stack icons, counts, empty slots, and status text.
8. `CratePreviewTooltipLayout` centralizes pixel geometry for the crate panel and slot/item offsets.

## Boundaries

- Client rendering classes must stay behind the client-only entrypoint.
- Optional integration must not introduce hard compile-time dependencies on Immersive Engineering or Engineer's Decor variants.
- Actual registry IDs, recipe IDs, and item IDs must be read from source/resources/runtime metadata before use.
- CodeGraph is useful for Java/properties symbols, but Markdown, TOML templates, PowerShell scripts, and resource JSON still require direct reads.
