package com.emmanueltremblay.wimc.crate;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record CratePreviewContents(
        List<ItemStack> slots,
        int columns,
        int rows,
        int totalSlots,
        int hiddenSlots,
        boolean showEmptySlots,
        CratePreviewStatus status
) {
    public static final int PREFERRED_COLUMNS = 9;
    public static final int DEFAULT_CRATE_SLOTS = 27;
    public static final int SLOT_SIZE = 18;

    record CompactPreviewPlan(int visibleSlots, int columns, int rows, int hiddenSlots) {
    }

    public static CratePreviewContents create(
            List<ItemStack> sourceSlots,
            int expectedSlots,
            int maxRows,
            int columns,
            boolean showEmptySlots,
            CratePreviewStatus status
    ) {
        int safeColumns = clamp(columns, 1, PREFERRED_COLUMNS);
        int safeRows = clamp(maxRows, 1, 12);
        int slotCapacity = safeColumns * safeRows;
        int totalSlots = Math.max(0, Math.max(expectedSlots, sourceSlots.size()));

        if (!showEmptySlots) {
            return createCompact(sourceSlots, totalSlots, slotCapacity, safeColumns, status);
        }

        int clampedSlots = Math.min(totalSlots, slotCapacity);
        int visibleSlots = clampedSlots;

        int rows = visibleSlots == 0 ? 0 : (int)Math.ceil(visibleSlots / (double)safeColumns);
        List<ItemStack> visible = new ArrayList<>(visibleSlots);
        for (int slot = 0; slot < visibleSlots; slot++) {
            ItemStack stack = slot < sourceSlots.size() ? sourceSlots.get(slot) : ItemStack.EMPTY;
            visible.add(stack.copy());
        }

        int hiddenSlots = hiddenSlots(sourceSlots, totalSlots, clampedSlots, showEmptySlots, status);
        return new CratePreviewContents(List.copyOf(visible), safeColumns, rows, totalSlots, hiddenSlots, showEmptySlots, status);
    }

    public boolean hasRenderableGrid() {
        return rows > 0 && (showEmptySlots || hasAnyItems());
    }

    public boolean hasAnyItems() {
        for (ItemStack stack : slots) {
            if (!stack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int hiddenSlots(
            List<ItemStack> sourceSlots,
            int totalSlots,
            int clampedSlots,
            boolean showEmptySlots,
            CratePreviewStatus status
    ) {
        if (totalSlots <= clampedSlots || (!showEmptySlots && status == CratePreviewStatus.EMPTY)) {
            return 0;
        }
        if (showEmptySlots || hasNonEmptySlotsAfter(sourceSlots, clampedSlots)) {
            return totalSlots - clampedSlots;
        }
        return 0;
    }

    private static boolean hasNonEmptySlotsAfter(List<ItemStack> slots, int startSlot) {
        for (int slot = Math.max(0, startSlot); slot < slots.size(); slot++) {
            if (!slots.get(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static CratePreviewContents createCompact(
            List<ItemStack> sourceSlots,
            int totalSlots,
            int slotCapacity,
            int safeColumns,
            CratePreviewStatus status
    ) {
        if (status == CratePreviewStatus.EMPTY) {
            return new CratePreviewContents(List.of(), safeColumns, 0, totalSlots, 0, false, status);
        }

        List<ItemStack> occupied = new ArrayList<>();
        for (ItemStack stack : sourceSlots) {
            if (!stack.isEmpty()) {
                occupied.add(stack.copy());
            }
        }

        CompactPreviewPlan plan = compactPreviewPlan(occupied.size(), slotCapacity, safeColumns, status);
        List<ItemStack> visible = new ArrayList<>(plan.visibleSlots());
        for (int slot = 0; slot < plan.visibleSlots(); slot++) {
            visible.add(occupied.get(slot).copy());
        }

        return new CratePreviewContents(
                List.copyOf(visible),
                plan.columns(),
                plan.rows(),
                totalSlots,
                plan.hiddenSlots(),
                false,
                status
        );
    }

    static CompactPreviewPlan compactPreviewPlan(
            int occupiedSlots,
            int slotCapacity,
            int safeColumns,
            CratePreviewStatus status
    ) {
        if (status == CratePreviewStatus.EMPTY) {
            return new CompactPreviewPlan(0, safeColumns, 0, 0);
        }

        int safeOccupiedSlots = Math.max(0, occupiedSlots);
        int visibleSlots = Math.min(safeOccupiedSlots, Math.max(0, slotCapacity));
        int columns = visibleSlots == 0 ? safeColumns : Math.min(Math.max(1, safeColumns), visibleSlots);
        int rows = visibleSlots == 0 ? 0 : (int)Math.ceil(visibleSlots / (double)columns);
        int hiddenSlots = Math.max(0, safeOccupiedSlots - visibleSlots);
        return new CompactPreviewPlan(visibleSlots, columns, rows, hiddenSlots);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
