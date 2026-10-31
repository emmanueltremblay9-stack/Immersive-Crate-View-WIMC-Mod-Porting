# Registries And Data

## Version And Mod Metadata

Authoritative project properties live in `gradle.properties`.

- `minecraft_version=1.21.1`
- `neo_version=21.1.230`
- `mod_id=immersive_crate_view_wimc_mod_porting`
- `mod_legacy_ids=wimc`
- `mod_name=Engineer's Crate Preview`
- `mod_archive_name=Immersive Crate View (WIMC) Mod Porting`
- `mod_version=0.1.37`

`build.gradle` expands these properties into `src/main/templates/META-INF/neoforge.mods.toml` through the `generateModMetadata` task.

## Tags

The industrial crate opt-in tag is:

```text
immersive_crate_view_wimc_mod_porting:industrial_crates
```

The corresponding source constant is `WimcTags.INDUSTRIAL_CRATES`, and the resource file is:

```text
src/main/resources/data/immersive_crate_view_wimc_mod_porting/tags/item/industrial_crates.json
```

Current optional entries include Immersive Engineering crates and the Immersive Engineer Decor & Controls labeled crate. Re-read the JSON before changing or documenting the exact set.

## NeoForge Metadata

The metadata template declares required dependencies on NeoForge and Minecraft, plus optional client-side integrations for:

- `immersiveengineering`
- `immersive_engineer_decor_controls_tool_reforged`
- `engineers_decor_reforged`

Do not invent dependencies. Read `src/main/templates/META-INF/neoforge.mods.toml` before editing metadata or describing runtime assumptions.

## Resources

Primary runtime resources are under:

```text
src/main/resources/assets/immersive_crate_view_wimc_mod_porting/
src/main/resources/data/immersive_crate_view_wimc_mod_porting/
```

The current metadata logo file is:

```text
src/main/resources/immersive_crate_view_wimc_mod_porting_logo.png
```

## Engineer's Manual Integration

The Immersive Engineering manual hook is:

```text
src/main/resources/assets/immersiveengineering/manual/autoload.json
```

The WIMC manual entries live under:

```text
src/main/resources/assets/immersive_crate_view_wimc_mod_porting/manual/
```

Manual content is validated by `qaManualResources`.

## Data Generation

`build.gradle` configures the NeoForge `data` run to write generated resources to:

```text
src/generated/resources/
```

It uses `src/main/resources/` as existing input. The main resources source set includes `src/generated/resources` and excludes generated cache paths and Blockbench model files.
