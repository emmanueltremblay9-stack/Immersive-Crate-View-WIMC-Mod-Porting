package com.emmanueltremblay.wimc.client;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CratePreviewTooltipEventsQa {
    private CratePreviewTooltipEventsQa() {
    }

    public static void main(String[] args) {
        requireFalse(
                CratePreviewTooltipEvents.shouldExtractPreview(true, false),
                "shift-gated preview should not extract inventory before Shift is held"
        );
        requireTrue(
                CratePreviewTooltipEvents.shouldShowShiftHint(true, false, true),
                "shift-gated supported crates should show the Shift hint"
        );
        requireFalse(
                CratePreviewTooltipEvents.shouldShowShiftHint(true, false, false),
                "shift-gated unsupported items should not show the Shift hint"
        );
        requireTrue(
                CratePreviewTooltipEvents.shouldExtractPreview(true, true),
                "shift-gated preview should extract once Shift is held"
        );
        requireTrue(
                CratePreviewTooltipEvents.shouldExtractPreview(false, false),
                "non-shift-gated preview should extract without Shift"
        );
        requireFalse(
                CratePreviewTooltipEvents.shouldShowShiftHint(false, false, true),
                "non-shift-gated preview should not show the Shift hint"
        );
        rendererFailuresAreContainedAndDeduplicated();
    }

    private static void rendererFailuresAreContainedAndDeduplicated() {
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath("minecraft", "stone");
        RuntimeException failure = new IllegalStateException("synthetic renderer failure");
        AtomicBoolean continued = new AtomicBoolean(false);
        List<String> diagnostics = new ArrayList<>();

        ClientCratePreviewTooltipComponent.clearLoggedRendererFailuresForTest();
        ClientCratePreviewTooltipComponent.runRendererSafely(itemId, () -> {
            throw failure;
        }, diagnostics::add);
        ClientCratePreviewTooltipComponent.runRendererSafely(itemId, () -> {
            throw failure;
        }, diagnostics::add);
        ClientCratePreviewTooltipComponent.runRendererSafely(itemId, () -> continued.set(true), diagnostics::add);

        String diagnostic = diagnostics.getFirst();
        requireTrue(continued.get(), "renderer failure should not stop later tooltip rendering");
        requireTrue(diagnostics.size() == 1, "repeated renderer failures should emit one diagnostic");
        requireTrue(diagnostic.contains("Skipped crate preview render"), "renderer failure diagnostic should identify the skipped render");
        requireTrue(diagnostic.contains(itemId.toString()), "renderer failure diagnostic should include the item id");
        requireTrue(diagnostic.contains("IllegalStateException"), "renderer failure diagnostic should include the exception class");
        requireTrue(diagnostic.contains("synthetic renderer failure"), "renderer failure diagnostic should include the exception message");
        ClientCratePreviewTooltipComponent.clearLoggedRendererFailuresForTest();
    }

    private static void requireTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + ": expected true");
        }
    }

    private static void requireFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label + ": expected false");
        }
    }
}
