# Phase 1 Runtime Proof

This repository is a client tooltip utility for NeoForge 1.21.1. Phase 1 proof must preserve that shape: no blocks, items, menus, screens, custom registries, or mixins.

## Dedicated Server Smoke

Use:

```powershell
.\scripts\smoke-runserver.ps1 -BuildDir build-codex-phase1 -TimeoutSeconds 180
```

Pass criteria:

- `runServer` reaches the dedicated-server ready line.
- The mod list includes `Engineer's Crate Preview`.
- The log does not contain WIMC client package names, `ClientCratePreviewTooltipComponent`, `CratePreviewTooltipEvents`, or `net.minecraft.client`.
- The log does not contain `ERROR`, `Exception`, or `Caused by`.

The script stops only the process tree it started after readiness is observed. This is a smoke gate for classloading and server startup, not a GameTest substitute.

## Client Startup Smoke

Use:

```powershell
.\scripts\smoke-runclient.ps1 -BuildDir build-codex-phase1 -TimeoutSeconds 180
```

Pass criteria:

- `runClient` reaches client launch/resource startup evidence.
- The mod list includes `Engineer's Crate Preview`.
- The log does not contain `ERROR`, `Exception`, or `Caused by`.

This proves client startup with the tooltip code on the classpath. It does not prove an actual hover tooltip pixel state.

## Manual Tooltip Proof Matrix

Fully automated tooltip proof is not available in this repo because the Gradle `runClient` task does not create a supported crate stack, set client config variants, hover the stack, or capture/render-assert pixels. Use the Prism LAB instance after installing the verified jar.

Manual matrix:

1. Launch Prism instance `1.21.1 TesT LaB`.
2. Open a world with Immersive Engineering available.
3. Obtain or inspect an `immersiveengineering:crate` or `immersiveengineering:reinforced_crate` stack that has carried inventory data.
4. With default config, hover the crate stack and confirm the crate preview renders.
5. Set `requireShiftForPreview=true`, hover without Shift, and confirm the Shift hint appears without rendering the grid.
6. Hold Shift and confirm the preview renders.
7. Toggle `showEmptySlots=false` and confirm occupied-slot compact rendering still works.
8. Use a narrow client window and confirm the preview reduces columns or clamps width instead of overflowing.
9. Test with Immersive Engineering absent or its crate texture unavailable, if practical, and confirm the fallback painted panel renders instead of crashing.

Record the screenshot path or log path in this file after manual execution.
