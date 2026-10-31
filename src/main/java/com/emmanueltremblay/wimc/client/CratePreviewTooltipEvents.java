package com.emmanueltremblay.wimc.client;

import com.emmanueltremblay.wimc.WimcClientConfig;
import com.emmanueltremblay.wimc.crate.CrateInventoryExtractor;
import com.emmanueltremblay.wimc.crate.CratePreviewContents;
import com.emmanueltremblay.wimc.crate.CratePreviewDetector;
import com.emmanueltremblay.wimc.tooltip.CratePreviewTooltipComponent;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import javax.annotation.Nullable;
import java.util.Optional;

public final class CratePreviewTooltipEvents {
    private static final int TOOLTIP_SCREEN_MARGIN = 16;

    private CratePreviewTooltipEvents() {
    }

    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CratePreviewTooltipComponent.class, ClientCratePreviewTooltipComponent::new);
    }

    public static void gatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        if (!WimcClientConfig.enableCratePreview()) {
            return;
        }

        boolean requireShift = WimcClientConfig.requireShiftForPreview();
        boolean shiftDown = Screen.hasShiftDown();
        if (!shouldExtractPreview(requireShift, shiftDown)) {
            if (shouldShowShiftHint(requireShift, shiftDown, CratePreviewDetector.isSupportedCrate(event.getItemStack()))) {
                event.getTooltipElements().add(Either.left(Component.translatable("immersive_crate_view_wimc_mod_porting.tooltip.crate_preview.shift")));
            }
            return;
        }

        int columns = columnsForScreen(event.getScreenWidth());
        Optional<CratePreviewContents> preview = CrateInventoryExtractor.extract(
                event.getItemStack(),
                registryAccess(),
                WimcClientConfig.maxPreviewRows(),
                columns,
                WimcClientConfig.showEmptySlots()
        );

        preview.ifPresent(contents -> {
            event.getTooltipElements().add(Either.right(new CratePreviewTooltipComponent(contents)));
            if (event.getMaxWidth() != -1) {
                event.setMaxWidth(Math.max(
                        event.getMaxWidth(),
                        CratePreviewTooltipLayout.panelWidth(contents.columns())
                ));
            }
        });
    }

    static boolean shouldExtractPreview(boolean requireShiftForPreview, boolean shiftDown) {
        return !requireShiftForPreview || shiftDown;
    }

    static boolean shouldShowShiftHint(boolean requireShiftForPreview, boolean shiftDown, boolean supportedCrate) {
        return requireShiftForPreview && !shiftDown && supportedCrate;
    }

    private static int columnsForScreen(int screenWidth) {
        return CratePreviewTooltipLayout.columnsForScreenWidth(screenWidth, TOOLTIP_SCREEN_MARGIN);
    }

    @Nullable
    private static HolderLookup.Provider registryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            return minecraft.level.registryAccess();
        }
        if (minecraft.getConnection() != null) {
            return minecraft.getConnection().registryAccess();
        }
        return null;
    }
}
