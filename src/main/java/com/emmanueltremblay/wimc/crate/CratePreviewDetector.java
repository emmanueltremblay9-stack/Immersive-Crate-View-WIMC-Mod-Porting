package com.emmanueltremblay.wimc.crate;

import com.emmanueltremblay.wimc.Wimc;
import com.emmanueltremblay.wimc.WimcClientConfig;
import com.emmanueltremblay.wimc.WimcTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class CratePreviewDetector {
    private static final String IE_MOD_ID = "immersiveengineering";
    private static final String EDCT_MOD_ID = "immersive_engineer_decor_controls_tool_reforged";
    private static final String LEGACY_EDCT_MOD_ID = "engineers_decor_reforged";
    private static final ResourceLocation IE_CRATE = ResourceLocation.fromNamespaceAndPath(IE_MOD_ID, "crate");
    private static final ResourceLocation IE_REINFORCED_CRATE = ResourceLocation.fromNamespaceAndPath(IE_MOD_ID, "reinforced_crate");
    private static final ResourceLocation EDCT_LABELED_CRATE = ResourceLocation.fromNamespaceAndPath(EDCT_MOD_ID, "labeled_crate");
    private static final ResourceLocation LEGACY_EDCT_LABELED_CRATE = ResourceLocation.fromNamespaceAndPath(LEGACY_EDCT_MOD_ID, "labeled_crate");

    private CratePreviewDetector() {
    }

    public static boolean isSupportedCrate(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (isImmersiveEngineeringCrate(itemId)) {
            return WimcClientConfig.enableImmersiveEngineeringCrates() && ModList.get().isLoaded(IE_MOD_ID);
        }
        if (isEngineersDecorLabeledCrate(itemId)) {
            return WimcClientConfig.enableOwnModCrates() && isEngineersDecorLoaded();
        }

        return WimcClientConfig.enableOwnModCrates()
                && (isOwnModCrate(itemId) || stack.is(WimcTags.INDUSTRIAL_CRATES));
    }

    public static int expectedSlotCount(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (isImmersiveEngineeringCrate(itemId)
                || isEngineersDecorLabeledCrate(itemId)
                || isOwnModCrate(itemId)
                || stack.is(WimcTags.INDUSTRIAL_CRATES)) {
            return CratePreviewContents.DEFAULT_CRATE_SLOTS;
        }
        return 0;
    }

    private static boolean isImmersiveEngineeringCrate(ResourceLocation itemId) {
        return IE_CRATE.equals(itemId) || IE_REINFORCED_CRATE.equals(itemId);
    }

    private static boolean isEngineersDecorLabeledCrate(ResourceLocation itemId) {
        return EDCT_LABELED_CRATE.equals(itemId) || LEGACY_EDCT_LABELED_CRATE.equals(itemId);
    }

    private static boolean isEngineersDecorLoaded() {
        return ModList.get().isLoaded(EDCT_MOD_ID) || ModList.get().isLoaded(LEGACY_EDCT_MOD_ID);
    }

    private static boolean isOwnModCrate(ResourceLocation itemId) {
        return Wimc.MOD_ID.equals(itemId.getNamespace()) && itemId.getPath().contains("crate");
    }
}
