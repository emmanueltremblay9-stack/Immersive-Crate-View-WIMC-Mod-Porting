package com.emmanueltremblay.wimc.client;

import com.emmanueltremblay.wimc.crate.CratePreviewContents;

final class CratePreviewTooltipLayout {
    static final int PREFERRED_COLUMNS = CratePreviewContents.PREFERRED_COLUMNS;
    static final int SLOT_SIZE = CratePreviewContents.SLOT_SIZE;
    static final int GUI_SLOT_LEFT = 7;
    static final int GUI_SLOT_TOP = 17;
    static final int GUI_SLOT_RIGHT_PADDING = 7;
    static final int GUI_SLOT_BOTTOM_PADDING = 8;
    static final int ITEM_OFFSET = 1;
    static final int IE_CRATE_TEXTURE_ROWS = 3;

    private CratePreviewTooltipLayout() {
    }

    static int panelWidth(int columns) {
        return GUI_SLOT_LEFT + columns * SLOT_SIZE + GUI_SLOT_RIGHT_PADDING;
    }

    static int panelHeight(int rows) {
        return GUI_SLOT_TOP + rows * SLOT_SIZE + GUI_SLOT_BOTTOM_PADDING;
    }

    static int messageYOffset(int rows) {
        return panelHeight(rows);
    }

    static int slotX(int tooltipX, int slot, int columns) {
        return tooltipX + GUI_SLOT_LEFT + column(slot, columns) * SLOT_SIZE;
    }

    static int slotY(int tooltipY, int slot, int columns) {
        return tooltipY + GUI_SLOT_TOP + row(slot, columns) * SLOT_SIZE;
    }

    static int itemX(int tooltipX, int slot, int columns) {
        return slotX(tooltipX, slot, columns) + ITEM_OFFSET;
    }

    static int itemY(int tooltipY, int slot, int columns) {
        return slotY(tooltipY, slot, columns) + ITEM_OFFSET;
    }

    static int columnsForScreenWidth(int screenWidth, int screenMargin) {
        int availablePanelWidth = Math.max(panelWidth(1), screenWidth - Math.max(0, screenMargin));
        int availableSlotWidth = Math.max(SLOT_SIZE, availablePanelWidth - GUI_SLOT_LEFT - GUI_SLOT_RIGHT_PADDING);
        return clamp(availableSlotWidth / SLOT_SIZE, 1, PREFERRED_COLUMNS);
    }

    static boolean canUseFullCrateTexture(int columns, int rows, boolean showEmptySlots) {
        return showEmptySlots && columns == PREFERRED_COLUMNS && rows == IE_CRATE_TEXTURE_ROWS;
    }

    private static int column(int slot, int columns) {
        return Math.max(0, slot) % clamp(columns, 1, PREFERRED_COLUMNS);
    }

    private static int row(int slot, int columns) {
        return Math.max(0, slot) / clamp(columns, 1, PREFERRED_COLUMNS);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
