package com.emmanueltremblay.wimc.crate;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class CrateInventoryExtractor {
    private static final int MAX_CAPABILITY_SLOTS_TO_SCAN = 256;
    private static final int MAX_STACK_CODEC_COUNT = 99;
    private static final ResourceLocation AIR_ITEM_ID = ResourceLocation.fromNamespaceAndPath("minecraft", "air");

    private record PreviewExtraction(Optional<CratePreviewContents> preview, boolean incomplete) {
    }

    private CrateInventoryExtractor() {
    }

    public static Optional<CratePreviewContents> extract(
            ItemStack stack,
            @Nullable HolderLookup.Provider registries,
            int maxRows,
            int columns,
            boolean showEmptySlots
    ) {
        if (!CratePreviewDetector.isSupportedCrate(stack)) {
            return Optional.empty();
        }

        int expectedSlots = CratePreviewDetector.expectedSlotCount(stack);

        Optional<CratePreviewContents> dataComponentPreview = fromContainerComponent(stack, expectedSlots, maxRows, columns, showEmptySlots);
        if (!shouldProbeFallback(dataComponentPreview)) {
            return dataComponentPreview;
        }

        PreviewExtraction blockEntityPreview = fromBlockEntityData(
                stack,
                registries,
                expectedSlots,
                maxRows,
                columns,
                showEmptySlots
        );
        if (!blockEntityPreview.incomplete() && !shouldProbeFallback(blockEntityPreview.preview())) {
            return blockEntityPreview.preview();
        }

        PreviewExtraction capabilityPreview = fromItemCapability(stack, maxRows, columns, showEmptySlots);
        Optional<CratePreviewContents> selectedPreview = selectPreview(
                dataComponentPreview,
                blockEntityPreview.preview(),
                capabilityPreview.preview(),
                stack.get(DataComponents.CONTAINER_LOOT) != null || hasBlockEntityLootTable(stack, registries, expectedSlots),
                maxRows,
                columns
        );
        if (selectedPreview.isPresent() && selectedPreview.get().status() != CratePreviewStatus.EMPTY) {
            return selectedPreview;
        }

        if (blockEntityPreview.incomplete() || capabilityPreview.incomplete()) {
            return Optional.empty();
        }

        if (selectedPreview.isPresent()) {
            return selectedPreview;
        }

        if (showEmptySlots && expectedSlots > 0) {
            return Optional.of(CratePreviewContents.create(
                    List.of(),
                    expectedSlots,
                    maxRows,
                    columns,
                    true,
                    CratePreviewStatus.EMPTY
            ));
        }

        return Optional.empty();
    }

    static boolean shouldProbeFallback(Optional<CratePreviewContents> preview) {
        return preview.isEmpty() || preview.get().status() != CratePreviewStatus.READY;
    }

    static Optional<CratePreviewContents> selectPreview(
            Optional<CratePreviewContents> dataComponentPreview,
            Optional<CratePreviewContents> blockEntityPreview,
            Optional<CratePreviewContents> capabilityPreview,
            boolean hasUnresolvedLoot,
            int maxRows,
            int columns
    ) {
        Optional<CratePreviewContents> readyPreview = firstReady(
                dataComponentPreview,
                blockEntityPreview,
                capabilityPreview
        );
        if (readyPreview.isPresent()) {
            return readyPreview;
        }

        if (hasUnresolvedLoot) {
            return Optional.of(unresolvedLoot(maxRows, columns));
        }

        if (dataComponentPreview.isPresent()) {
            return dataComponentPreview;
        }
        if (blockEntityPreview.isPresent()) {
            return blockEntityPreview;
        }
        return capabilityPreview;
    }

    private static CratePreviewContents unresolvedLoot(int maxRows, int columns) {
        return CratePreviewContents.create(
                List.of(),
                0,
                maxRows,
                columns,
                false,
                CratePreviewStatus.LOOT_UNRESOLVED
        );
    }

    @SafeVarargs
    private static Optional<CratePreviewContents> firstReady(Optional<CratePreviewContents>... previews) {
        for (Optional<CratePreviewContents> preview : previews) {
            if (preview.isPresent() && preview.get().status() == CratePreviewStatus.READY) {
                return preview;
            }
        }
        return Optional.empty();
    }

    private static Optional<CratePreviewContents> fromContainerComponent(
            ItemStack stack,
            int expectedSlots,
            int maxRows,
            int columns,
            boolean showEmptySlots
    ) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return Optional.empty();
        }

        try {
            int componentSlots = contents.getSlots();
            int totalSlots = Math.max(expectedSlots, componentSlots);
            NonNullList<ItemStack> slots = NonNullList.withSize(totalSlots, ItemStack.EMPTY);
            contents.copyInto(slots);

            CratePreviewStatus status = hasAnyItem(slots) ? CratePreviewStatus.READY : CratePreviewStatus.EMPTY;
            return Optional.of(CratePreviewContents.create(slots, totalSlots, maxRows, columns, showEmptySlots, status));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static PreviewExtraction fromBlockEntityData(
            ItemStack stack,
            @Nullable HolderLookup.Provider registries,
            int expectedSlots,
            int maxRows,
            int columns,
            boolean showEmptySlots
    ) {
        if (registries == null || expectedSlots <= 0) {
            return new PreviewExtraction(Optional.empty(), false);
        }

        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || !blockEntityData.contains("Items")) {
            return new PreviewExtraction(Optional.empty(), false);
        }

        try {
            CompoundTag tag = blockEntityData.copyTag();
            if (!tag.contains("Items", Tag.TAG_LIST)) {
                return new PreviewExtraction(Optional.empty(), false);
            }
            if (!hasOnlyInRangeItemSlots(tag, expectedSlots)) {
                return new PreviewExtraction(Optional.empty(), true);
            }

            NonNullList<ItemStack> slots = NonNullList.withSize(expectedSlots, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, slots, registries);
            CratePreviewStatus status = hasAnyItem(slots) ? CratePreviewStatus.READY : CratePreviewStatus.EMPTY;
            return new PreviewExtraction(
                    Optional.of(CratePreviewContents.create(slots, expectedSlots, maxRows, columns, showEmptySlots, status)),
                    false
            );
        } catch (RuntimeException exception) {
            return new PreviewExtraction(Optional.empty(), true);
        }
    }

    private static boolean hasBlockEntityLootTable(ItemStack stack, @Nullable HolderLookup.Provider registries, int expectedSlots) {
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null) {
            return false;
        }

        try {
            CompoundTag tag = blockEntityData.copyTag();
            return hasBlockEntityLootTableWithoutKnownItems(tag, registries, expectedSlots);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    static boolean hasBlockEntityLootTableWithoutKnownItems(CompoundTag tag) {
        return hasBlockEntityLootTableWithoutKnownItems(tag, null, Integer.MAX_VALUE);
    }

    static boolean hasBlockEntityLootTableWithoutKnownItems(CompoundTag tag, int expectedSlots) {
        return hasBlockEntityLootTableWithoutKnownItems(tag, null, expectedSlots);
    }

    static boolean hasBlockEntityLootTableWithoutKnownItems(
            CompoundTag tag,
            @Nullable HolderLookup.Provider registries,
            int expectedSlots
    ) {
        Predicate<CompoundTag> itemValidator = registries == null
                ? null
                : item -> isLoadableItemStack(item, registries);
        return hasBlockEntityLootTableWithoutKnownItems(tag, expectedSlots, itemValidator);
    }

    static boolean hasBlockEntityLootTableWithoutKnownItems(
            CompoundTag tag,
            int expectedSlots,
            @Nullable Predicate<CompoundTag> itemValidator
    ) {
        return hasLootTableKey(tag) && !hasKnownItemEntries(tag, itemValidator, expectedSlots);
    }

    private static boolean hasLootTableKey(CompoundTag tag) {
        return tag.contains("LootTable")
                || tag.contains("loot_table")
                || tag.contains("lootTable");
    }

    private static boolean hasKnownItemEntries(CompoundTag tag, @Nullable Predicate<CompoundTag> itemValidator, int expectedSlots) {
        if (!tag.contains("Items", Tag.TAG_LIST)) {
            return false;
        }

        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag item = items.getCompound(index);
            if (isKnownItemEntry(item, itemValidator, expectedSlots)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasOnlyInRangeItemSlots(CompoundTag tag, int expectedSlots) {
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            if (!isKnownPreviewSlot(items.getCompound(index), expectedSlots)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isKnownItemEntry(CompoundTag item, @Nullable Predicate<CompoundTag> itemValidator, int expectedSlots) {
        if (!item.contains("id", Tag.TAG_STRING)) {
            return false;
        }

        String itemId = item.getString("id");
        ResourceLocation itemLocation = ResourceLocation.tryParse(itemId);
        if (itemLocation == null || AIR_ITEM_ID.equals(itemLocation)) {
            return false;
        }

        if (!isKnownPreviewSlot(item, expectedSlots)) {
            return false;
        }

        if (item.contains("count")) {
            if (!item.contains("count", Tag.TAG_ANY_NUMERIC) || !isValidStackCount(item.getInt("count"))) {
                return false;
            }
        } else if (item.contains("Count")) {
            if (!item.contains("Count", Tag.TAG_ANY_NUMERIC) || !isValidStackCount(item.getInt("Count"))) {
                return false;
            }
        } else {
            return false;
        }

        if (itemValidator == null) {
            return true;
        }
        return itemValidator.test(item);
    }

    private static boolean isValidStackCount(int count) {
        return count > 0 && count <= MAX_STACK_CODEC_COUNT;
    }

    private static boolean isLoadableItemStack(CompoundTag item, HolderLookup.Provider registries) {
        return ItemStack.parse(registries, item)
                .filter(parsedStack -> !parsedStack.isEmpty())
                .isPresent();
    }

    private static boolean isKnownPreviewSlot(CompoundTag item, int expectedSlots) {
        if (expectedSlots <= 0) {
            return false;
        }
        if (!item.contains("Slot", Tag.TAG_BYTE)
                && !item.contains("Slot", Tag.TAG_SHORT)
                && !item.contains("Slot", Tag.TAG_INT)
                && !item.contains("Slot", Tag.TAG_LONG)) {
            return false;
        }
        long slot = item.getLong("Slot");
        return slot >= 0 && slot < expectedSlots;
    }

    private static PreviewExtraction fromItemCapability(
            ItemStack stack,
            int maxRows,
            int columns,
            boolean showEmptySlots
    ) {
        try {
            IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            if (handler == null) {
                return new PreviewExtraction(Optional.empty(), false);
            }

            int handlerSlots = handler.getSlots();
            if (handlerSlots <= 0) {
                return new PreviewExtraction(Optional.empty(), false);
            }

            if (!canFullyScanCapability(handler)) {
                return new PreviewExtraction(Optional.empty(), true);
            }

            List<ItemStack> slots = new ArrayList<>(handlerSlots);
            for (int slot = 0; slot < handlerSlots; slot++) {
                slots.add(handler.getStackInSlot(slot).copy());
            }

            CratePreviewStatus status = hasAnyItem(slots) ? CratePreviewStatus.READY : CratePreviewStatus.EMPTY;
            return new PreviewExtraction(
                    Optional.of(CratePreviewContents.create(slots, handlerSlots, maxRows, columns, showEmptySlots, status)),
                    false
            );
        } catch (RuntimeException exception) {
            return new PreviewExtraction(Optional.empty(), true);
        }
    }

    static boolean canFullyScanCapability(IItemHandler handler) {
        int handlerSlots = handler.getSlots();
        return handlerSlots > 0 && handlerSlots <= MAX_CAPABILITY_SLOTS_TO_SCAN;
    }

    private static boolean hasAnyItem(List<ItemStack> slots) {
        for (ItemStack slot : slots) {
            if (!slot.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
