package com.emmanueltremblay.wimc;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WimcClientConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue ENABLE_CRATE_PREVIEW;
    private static final ModConfigSpec.BooleanValue REQUIRE_SHIFT_FOR_PREVIEW;
    private static final ModConfigSpec.IntValue MAX_PREVIEW_ROWS;
    private static final ModConfigSpec.BooleanValue SHOW_EMPTY_SLOTS;
    private static final ModConfigSpec.BooleanValue ENABLE_IMMERSIVE_ENGINEERING_CRATES;
    private static final ModConfigSpec.BooleanValue ENABLE_OWN_MOD_CRATES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("cratePreview");

        ENABLE_CRATE_PREVIEW = builder
                .comment("Enables client-only inventory previews for supported crate items.")
                .define("enableCratePreview", true);
        REQUIRE_SHIFT_FOR_PREVIEW = builder
                .comment("When true, the preview is shown only while Shift is held.")
                .define("requireShiftForPreview", false);
        MAX_PREVIEW_ROWS = builder
                .comment("Maximum grid rows to render in a tooltip preview.")
                .defineInRange("maxPreviewRows", 6, 1, 12);
        SHOW_EMPTY_SLOTS = builder
                .comment("Draws empty slot backgrounds so crate shape remains visible.")
                .define("showEmptySlots", true);
        ENABLE_IMMERSIVE_ENGINEERING_CRATES = builder
                .comment("Enables exact detection for Immersive Engineering crates and reinforced crates.")
                .define("enableImmersiveEngineeringCrates", true);
        ENABLE_OWN_MOD_CRATES = builder
                .comment("Enables WIMC-compatible crate detectors and tag-based industrial crate opt-ins.")
                .define("enableOwnModCrates", true);

        builder.pop();
        SPEC = builder.build();
    }

    private WimcClientConfig() {
    }

    public static boolean enableCratePreview() {
        return ENABLE_CRATE_PREVIEW.get();
    }

    public static boolean requireShiftForPreview() {
        return REQUIRE_SHIFT_FOR_PREVIEW.get();
    }

    public static int maxPreviewRows() {
        return MAX_PREVIEW_ROWS.get();
    }

    public static boolean showEmptySlots() {
        return SHOW_EMPTY_SLOTS.get();
    }

    public static boolean enableImmersiveEngineeringCrates() {
        return ENABLE_IMMERSIVE_ENGINEERING_CRATES.get();
    }

    public static boolean enableOwnModCrates() {
        return ENABLE_OWN_MOD_CRATES.get();
    }
}
