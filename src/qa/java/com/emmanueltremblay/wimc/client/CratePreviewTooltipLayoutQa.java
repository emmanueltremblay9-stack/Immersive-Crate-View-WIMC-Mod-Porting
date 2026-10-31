package com.emmanueltremblay.wimc.client;

public final class CratePreviewTooltipLayoutQa {
    private CratePreviewTooltipLayoutQa() {
    }

    public static void main(String[] args) {
        requireEqual(176, CratePreviewTooltipLayout.panelWidth(9), "9-wide crate panel width");
        requireEqual(79, CratePreviewTooltipLayout.panelHeight(3), "3-row crate panel height");
        requireEqual(
                CratePreviewTooltipLayout.panelHeight(3),
                CratePreviewTooltipLayout.messageYOffset(3),
                "message offset after 3-row panel"
        );
        requireEqual(9, CratePreviewTooltipLayout.columnsForScreenWidth(192, 16), "full-width column boundary");
        requireEqual(8, CratePreviewTooltipLayout.columnsForScreenWidth(191, 16), "sub-full-width column boundary");
        requireEqual(1, CratePreviewTooltipLayout.columnsForScreenWidth(0, 16), "minimum column boundary");
        requireEqual(7, CratePreviewTooltipLayout.slotX(0, 0, 9), "first slot x");
        requireEqual(17, CratePreviewTooltipLayout.slotY(0, 0, 9), "first slot y");
        requireEqual(8, CratePreviewTooltipLayout.itemX(0, 0, 9), "first item x");
        requireEqual(18, CratePreviewTooltipLayout.itemY(0, 0, 9), "first item y");
        requireEqual(152, CratePreviewTooltipLayout.itemX(0, 8, 9), "last first-row item x");
        requireTrue(CratePreviewTooltipLayout.canUseFullCrateTexture(9, 3, true), "full crate texture shape");
        requireFalse(CratePreviewTooltipLayout.canUseFullCrateTexture(9, 2, true), "cropped crate texture rows");
        requireFalse(CratePreviewTooltipLayout.canUseFullCrateTexture(8, 3, true), "cropped crate texture columns");
        requireFalse(CratePreviewTooltipLayout.canUseFullCrateTexture(9, 3, false), "hidden empty slot texture");
    }

    private static void requireEqual(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
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
