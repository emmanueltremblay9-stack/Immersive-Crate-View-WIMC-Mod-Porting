package com.emmanueltremblay.wimc.crate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class CratePreviewContentsQa {
    private CratePreviewContentsQa() {
    }

    public static void main(String[] args) {
        trimmedEmptySlotsAreNotReportedAsHidden();
        sparseOccupiedSlotsFitInsideCompactPreview();
        compactOccupiedSlotsReportOnlyHiddenOccupiedStacks();
        concreteFallbackInventoryBeatsLootMarker();
        unresolvedLootBeatsEmptyFallback();
        readyPreviewStopsFallbackProbing();
        emptyPreviewAllowsFallbackProbing();
        malformedBlockEntityItemsDoNotHideLootTable();
        airBlockEntityItemsDoNotHideLootTable();
        zeroCountBlockEntityItemsDoNotHideLootTable();
        missingCountBlockEntityItemsDoNotHideLootTable();
        overMaxCountBlockEntityItemsDoNotHideLootTable();
        legacyOverMaxCountBlockEntityItemsDoNotHideLootTable();
        invalidNamespacedBlockEntityItemsDoNotHideLootTable();
        missingSlotBlockEntityItemsDoNotHideLootTable();
        outOfRangeBlockEntityItemsDoNotHideLootTable();
        wideNumericSlotBlockEntityItemsDoNotHideLootTable();
        missingRegistryBlockEntityItemsDoNotHideLootTable();
        registeredRegistryBlockEntityItemsBeatLootTable();
        knownBlockEntityItemsBeatLootTable();
        oversizedCapabilityWithOnlyLastSlotOccupiedIsNotFullyScannable();
        oversizedOptInCrateDataIsRejectedBeforePartialPreview();
        malformedWideNbtSlotIsRejectedBeforeStandardLoading();
    }

    private static void trimmedEmptySlotsAreNotReportedAsHidden() {
        CratePreviewContents contents = CratePreviewContents.create(
                List.of(),
                27,
                6,
                9,
                false,
                CratePreviewStatus.EMPTY
        );

        requireEqual(0, contents.slots().size(), "trimmed slot count");
        requireEqual(0, contents.rows(), "trimmed row count");
        requireEqual(0, contents.hiddenSlots(), "trimmed hidden slots");
        requireFalse(contents.hasRenderableGrid(), "trimmed grid renderability");
    }

    private static void sparseOccupiedSlotsFitInsideCompactPreview() {
        CratePreviewContents.CompactPreviewPlan plan = CratePreviewContents.compactPreviewPlan(
                1,
                9,
                9,
                CratePreviewStatus.READY
        );

        requireEqual(1, plan.visibleSlots(), "compact sparse slot count");
        requireEqual(1, plan.columns(), "compact sparse column count");
        requireEqual(1, plan.rows(), "compact sparse row count");
        requireEqual(0, plan.hiddenSlots(), "compact sparse hidden slots");
    }

    private static void compactOccupiedSlotsReportOnlyHiddenOccupiedStacks() {
        CratePreviewContents.CompactPreviewPlan plan = CratePreviewContents.compactPreviewPlan(
                14,
                9,
                9,
                CratePreviewStatus.READY
        );

        requireEqual(9, plan.visibleSlots(), "compact overflow slot count");
        requireEqual(9, plan.columns(), "compact overflow column count");
        requireEqual(1, plan.rows(), "compact overflow row count");
        requireEqual(5, plan.hiddenSlots(), "compact overflow hidden occupied stacks");
    }

    private static void concreteFallbackInventoryBeatsLootMarker() {
        CratePreviewContents emptyComponent = preview(CratePreviewStatus.EMPTY);
        CratePreviewContents readyBlockEntity = preview(CratePreviewStatus.READY);
        CratePreviewContents emptyCapability = preview(CratePreviewStatus.EMPTY);

        Optional<CratePreviewContents> selected = CrateInventoryExtractor.selectPreview(
                Optional.of(emptyComponent),
                Optional.of(readyBlockEntity),
                Optional.of(emptyCapability),
                true,
                6,
                9
        );

        requireTrue(selected.isPresent(), "selected fallback preview");
        requireEqual(CratePreviewStatus.READY, selected.get().status(), "selected fallback status");
    }

    private static void unresolvedLootBeatsEmptyFallback() {
        CratePreviewContents emptyComponent = preview(CratePreviewStatus.EMPTY);
        CratePreviewContents emptyBlockEntity = preview(CratePreviewStatus.EMPTY);

        Optional<CratePreviewContents> selected = CrateInventoryExtractor.selectPreview(
                Optional.of(emptyComponent),
                Optional.of(emptyBlockEntity),
                Optional.empty(),
                true,
                6,
                9
        );

        requireTrue(selected.isPresent(), "selected unresolved loot preview");
        requireEqual(CratePreviewStatus.LOOT_UNRESOLVED, selected.get().status(), "selected unresolved loot status");
    }

    private static void readyPreviewStopsFallbackProbing() {
        requireFalse(
                CrateInventoryExtractor.shouldProbeFallback(Optional.of(preview(CratePreviewStatus.READY))),
                "ready preview fallback probing"
        );
    }

    private static void emptyPreviewAllowsFallbackProbing() {
        requireTrue(
                CrateInventoryExtractor.shouldProbeFallback(Optional.of(preview(CratePreviewStatus.EMPTY))),
                "empty preview fallback probing"
        );
        requireTrue(
                CrateInventoryExtractor.shouldProbeFallback(Optional.empty()),
                "missing preview fallback probing"
        );
    }

    private static void malformedBlockEntityItemsDoNotHideLootTable() {
        CompoundTag malformedItem = new CompoundTag();
        malformedItem.putByte("Slot", (byte)0);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(lootTableWithItem(malformedItem)),
                "malformed block-entity items should not hide unresolved loot"
        );
    }

    private static void airBlockEntityItemsDoNotHideLootTable() {
        CompoundTag airItem = itemEntry("minecraft:air", 1);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(lootTableWithItem(airItem)),
                "air block-entity items should not hide unresolved loot"
        );
    }

    private static void zeroCountBlockEntityItemsDoNotHideLootTable() {
        CompoundTag zeroCountItem = itemEntry("minecraft:stone", 0);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(lootTableWithItem(zeroCountItem)),
                "zero-count block-entity items should not hide unresolved loot"
        );
    }

    private static void missingCountBlockEntityItemsDoNotHideLootTable() {
        CompoundTag missingCountItem = itemEntryWithoutCount("minecraft:stone", 0);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(
                        lootTableWithItem(missingCountItem),
                        CratePreviewContents.DEFAULT_CRATE_SLOTS
                ),
                "missing-count block-entity items should not hide unresolved loot"
        );
    }

    private static void overMaxCountBlockEntityItemsDoNotHideLootTable() {
        CompoundTag overMaxCountItem = itemEntry("minecraft:stone", 100);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(lootTableWithItem(overMaxCountItem)),
                "over-max-count block-entity items should not hide unresolved loot"
        );
    }

    private static void legacyOverMaxCountBlockEntityItemsDoNotHideLootTable() {
        CompoundTag overMaxCountItem = legacyCountItemEntry("minecraft:stone", 100, 0);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(lootTableWithItem(overMaxCountItem)),
                "legacy over-max-count block-entity items should not hide unresolved loot"
        );
    }

    private static void invalidNamespacedBlockEntityItemsDoNotHideLootTable() {
        CompoundTag invalidItem = itemEntry("minecraft:bad item", 1);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(lootTableWithItem(invalidItem)),
                "invalid namespaced block-entity items should not hide unresolved loot"
        );
    }

    private static void missingSlotBlockEntityItemsDoNotHideLootTable() {
        CompoundTag missingSlotItem = itemEntryWithoutSlot("minecraft:stone", 1);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(
                        lootTableWithItem(missingSlotItem),
                        CratePreviewContents.DEFAULT_CRATE_SLOTS
                ),
                "missing-slot block-entity items should not hide unresolved loot"
        );
    }

    private static void outOfRangeBlockEntityItemsDoNotHideLootTable() {
        CompoundTag outOfRangeItem = itemEntry("minecraft:stone", 1, 99);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(
                        lootTableWithItem(outOfRangeItem),
                        CratePreviewContents.DEFAULT_CRATE_SLOTS
                ),
                "out-of-range block-entity items should not hide unresolved loot"
        );
    }

    private static void wideNumericSlotBlockEntityItemsDoNotHideLootTable() {
        CompoundTag wideSlotItem = itemEntryWithIntSlot("minecraft:stone", 1, 256);

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(
                        lootTableWithItem(wideSlotItem),
                        CratePreviewContents.DEFAULT_CRATE_SLOTS
                ),
                "wide numeric slot block-entity items should not wrap into preview range"
        );
    }

    private static void missingRegistryBlockEntityItemsDoNotHideLootTable() {
        CompoundTag missingItem = itemEntry("missingmod:ghost_item", 1);
        Predicate<CompoundTag> knownItems = item -> !"missingmod:ghost_item".equals(item.getString("id"));

        requireTrue(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(
                        lootTableWithItem(missingItem),
                        CratePreviewContents.DEFAULT_CRATE_SLOTS,
                        knownItems
                ),
                "valid but unregistered block-entity items should not hide unresolved loot"
        );
    }

    private static void registeredRegistryBlockEntityItemsBeatLootTable() {
        CompoundTag knownItem = itemEntry("minecraft:stone", 1);
        Predicate<CompoundTag> knownItems = item -> "minecraft:stone".equals(item.getString("id"));

        requireFalse(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(
                        lootTableWithItem(knownItem),
                        CratePreviewContents.DEFAULT_CRATE_SLOTS,
                        knownItems
                ),
                "registered block-entity items should beat stale loot markers"
        );
    }

    private static void knownBlockEntityItemsBeatLootTable() {
        CompoundTag knownItem = itemEntry("minecraft:stone", 1);

        requireFalse(
                CrateInventoryExtractor.hasBlockEntityLootTableWithoutKnownItems(lootTableWithItem(knownItem)),
                "known block-entity items should beat stale loot markers"
        );
    }

    private static void oversizedCapabilityWithOnlyLastSlotOccupiedIsNotFullyScannable() {
        LastSlotOnlyHandler handler = new LastSlotOnlyHandler(257, 256);

        requireFalse(
                CrateInventoryExtractor.canFullyScanCapability(handler),
                "257-slot handler must not be treated as fully inspected"
        );
        requireTrue(handler.hasOnlyItemAt(256), "slot 256 should be the only occupied test slot");
    }

    private static void oversizedOptInCrateDataIsRejectedBeforePartialPreview() {
        CompoundTag crateData = itemList(itemEntry("minecraft:stone", 1, 0), itemEntry("minecraft:dirt", 1, 53));

        requireFalse(
                CrateInventoryExtractor.hasOnlyInRangeItemSlots(crateData, CratePreviewContents.DEFAULT_CRATE_SLOTS),
                "54-slot opt-in crate data must not be represented as a complete 27-slot preview"
        );
    }

    private static void malformedWideNbtSlotIsRejectedBeforeStandardLoading() {
        CompoundTag crateData = itemList(itemEntryWithIntSlot("minecraft:stone", 1, 256));

        requireFalse(
                CrateInventoryExtractor.hasOnlyInRangeItemSlots(crateData, CratePreviewContents.DEFAULT_CRATE_SLOTS),
                "Slot=256 must be rejected instead of being truncated to slot 0"
        );
    }

    private static CompoundTag lootTableWithItem(CompoundTag item) {
        CompoundTag tag = new CompoundTag();
        tag.putString("LootTable", "minecraft:chests/simple_dungeon");
        ListTag items = new ListTag();
        items.add(item);
        tag.put("Items", items);
        return tag;
    }

    private static CompoundTag itemList(CompoundTag... itemsToAdd) {
        CompoundTag tag = new CompoundTag();
        ListTag items = new ListTag();
        for (CompoundTag item : itemsToAdd) {
            items.add(item);
        }
        tag.put("Items", items);
        return tag;
    }

    private static CompoundTag itemEntry(String id, int count) {
        return itemEntry(id, count, 0);
    }

    private static CompoundTag itemEntry(String id, int count, int slot) {
        CompoundTag item = new CompoundTag();
        item.putByte("Slot", (byte)slot);
        item.putString("id", id);
        item.putInt("count", count);
        return item;
    }

    private static CompoundTag itemEntryWithIntSlot(String id, int count, int slot) {
        CompoundTag item = new CompoundTag();
        item.putInt("Slot", slot);
        item.putString("id", id);
        item.putInt("count", count);
        return item;
    }

    private static CompoundTag itemEntryWithoutSlot(String id, int count) {
        CompoundTag item = new CompoundTag();
        item.putString("id", id);
        item.putInt("count", count);
        return item;
    }

    private static CompoundTag itemEntryWithoutCount(String id, int slot) {
        CompoundTag item = new CompoundTag();
        item.putByte("Slot", (byte)slot);
        item.putString("id", id);
        return item;
    }

    private static CompoundTag legacyCountItemEntry(String id, int count, int slot) {
        CompoundTag item = new CompoundTag();
        item.putByte("Slot", (byte)slot);
        item.putString("id", id);
        item.putInt("Count", count);
        return item;
    }

    private static CratePreviewContents preview(CratePreviewStatus status) {
        return CratePreviewContents.create(List.of(), 0, 6, 9, false, status);
    }

    private static final class LastSlotOnlyHandler implements IItemHandler {
        private final int slots;
        private final int occupiedSlot;

        private LastSlotOnlyHandler(int slots, int occupiedSlot) {
            this.slots = slots;
            this.occupiedSlot = occupiedSlot;
        }

        @Override
        public int getSlots() {
            return slots;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            throw new AssertionError("oversized handler must not be scanned before it is rejected");
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            throw new AssertionError("oversized handler must not be scanned before it is rejected");
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        private boolean hasOnlyItemAt(int slot) {
            return slot == occupiedSlot;
        }
    }

    private static void requireEqual(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void requireEqual(CratePreviewStatus expected, CratePreviewStatus actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void requireFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label + ": expected false");
        }
    }

    private static void requireTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + ": expected true");
        }
    }
}
