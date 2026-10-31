package com.emmanueltremblay.wimc.client;

import com.mojang.logging.LogUtils;
import com.emmanueltremblay.wimc.crate.CratePreviewContents;
import com.emmanueltremblay.wimc.crate.CratePreviewStatus;
import com.emmanueltremblay.wimc.tooltip.CratePreviewTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ClientCratePreviewTooltipComponent implements ClientTooltipComponent {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> LOGGED_RENDER_FAILURES = ConcurrentHashMap.newKeySet();
    private static final ResourceLocation IE_CRATE_GUI = ResourceLocation.fromNamespaceAndPath(
            "immersiveengineering",
            "textures/gui/crate.png"
    );
    private static final int MESSAGE_TOP_PADDING = 3;
    private static final int MESSAGE_HEIGHT = 10;
    private static final int IE_CRATE_TEXTURE_WIDTH = 176;
    private static final int PANEL_OUTLINE = 0xFF3A3328;
    private static final int PANEL_EDGE_LIGHT = 0xFFC7BCA5;
    private static final int PANEL_FILL = 0xFFC6BCA6;
    private static final int PANEL_INNER_SHADOW = 0xFF8E806D;
    private static final int SLOT_SHADOW = 0xFF5D554B;
    private static final int SLOT_HIGHLIGHT = 0xFFE0D6BF;
    private static final int SLOT_FILL = 0xFF8D8472;
    private static final int EMPTY_SLOT_FILL = 0xFF9E9480;
    private static final int TEXT_COLOR = 0xFFBFC7C9;

    private final CratePreviewContents contents;

    public ClientCratePreviewTooltipComponent(CratePreviewTooltipComponent component) {
        this.contents = component.contents();
    }

    @Override
    public int getHeight() {
        int gridHeight = contents.hasRenderableGrid() ? panelHeight() : 0;
        return gridHeight + (hasMessage() ? MESSAGE_TOP_PADDING + MESSAGE_HEIGHT : 0);
    }

    @Override
    public int getWidth(Font font) {
        int gridWidth = contents.hasRenderableGrid() ? panelWidth() : 0;
        return Math.max(gridWidth, font.width(message()));
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int messageY = y;
        if (contents.hasRenderableGrid()) {
            renderGrid(font, x, y, graphics);
            messageY += CratePreviewTooltipLayout.messageYOffset(contents.rows());
        }

        if (hasMessage()) {
            graphics.drawString(font, message(), x, messageY + MESSAGE_TOP_PADDING, TEXT_COLOR, false);
        }
    }

    private void renderGrid(Font font, int x, int y, GuiGraphics graphics) {
        boolean useCrateTexture = shouldUseCrateTexture();
        renderCratePanel(graphics, x, y, useCrateTexture);

        for (int slot = 0; slot < contents.slots().size(); slot++) {
            int slotX = CratePreviewTooltipLayout.slotX(x, slot, contents.columns());
            int slotY = CratePreviewTooltipLayout.slotY(y, slot, contents.columns());
            ItemStack stack = contents.slots().get(slot);

            if (!useCrateTexture && (contents.showEmptySlots() || !stack.isEmpty())) {
                renderSlotBackground(graphics, slotX, slotY, stack.isEmpty());
            }

            if (!stack.isEmpty()) {
                renderStackSafely(
                        font,
                        graphics,
                        stack,
                        CratePreviewTooltipLayout.itemX(x, slot, contents.columns()),
                        CratePreviewTooltipLayout.itemY(y, slot, contents.columns())
                );
            }
        }
    }

    private void renderCratePanel(GuiGraphics graphics, int x, int y, boolean useCrateTexture) {
        if (useCrateTexture) {
            graphics.blit(IE_CRATE_GUI, x, y, 0, 0, IE_CRATE_TEXTURE_WIDTH, panelHeight());
            return;
        }

        int width = panelWidth();
        int height = panelHeight();
        graphics.fill(x, y, x + width, y + height, PANEL_OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_EDGE_LIGHT);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PANEL_FILL);
        graphics.fill(x + 3, y + 3, x + width - 3, y + 4, PANEL_INNER_SHADOW);
        graphics.fill(x + 3, y + height - 4, x + width - 3, y + height - 3, PANEL_INNER_SHADOW);
    }

    private static void renderStackSafely(Font font, GuiGraphics graphics, ItemStack stack, int x, int y) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        runRendererSafely(itemId, () -> {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        });
    }

    private static void runRendererSafely(ResourceLocation itemId, Runnable renderer) {
        runRendererSafely(itemId, renderer, LOGGER::warn);
    }

    static void runRendererSafely(ResourceLocation itemId, Runnable renderer, Consumer<String> diagnosticSink) {
        try {
            renderer.run();
        } catch (RuntimeException exception) {
            if (shouldLogRendererFailure(itemId, exception)) {
                diagnosticSink.accept(rendererFailureDiagnostic(itemId, exception));
            }
        }
    }

    static boolean shouldLogRendererFailure(ResourceLocation itemId, RuntimeException exception) {
        return LOGGED_RENDER_FAILURES.add(itemId + "|" + exception.getClass().getName());
    }

    static String rendererFailureDiagnostic(ResourceLocation itemId, RuntimeException exception) {
        return "Skipped crate preview render for item " + itemId
                + " after " + exception.getClass().getSimpleName()
                + ": " + String.valueOf(exception.getMessage());
    }

    static void clearLoggedRendererFailuresForTest() {
        LOGGED_RENDER_FAILURES.clear();
    }

    private static void renderSlotBackground(GuiGraphics graphics, int x, int y, boolean empty) {
        graphics.fill(x, y, x + CratePreviewTooltipLayout.SLOT_SIZE, y + CratePreviewTooltipLayout.SLOT_SIZE, SLOT_SHADOW);
        graphics.fill(x + 1, y + 1, x + CratePreviewTooltipLayout.SLOT_SIZE, y + CratePreviewTooltipLayout.SLOT_SIZE, SLOT_HIGHLIGHT);
        graphics.fill(
                x + 1,
                y + 1,
                x + CratePreviewTooltipLayout.SLOT_SIZE - 1,
                y + CratePreviewTooltipLayout.SLOT_SIZE - 1,
                empty ? EMPTY_SLOT_FILL : SLOT_FILL
        );
        graphics.fill(x + 1, y + 1, x + CratePreviewTooltipLayout.SLOT_SIZE - 1, y + 2, SLOT_SHADOW);
        graphics.fill(x + 1, y + 1, x + 2, y + CratePreviewTooltipLayout.SLOT_SIZE - 1, SLOT_SHADOW);
    }

    private boolean shouldUseCrateTexture() {
        return CratePreviewTooltipLayout.canUseFullCrateTexture(
                        contents.columns(),
                        contents.rows(),
                        contents.showEmptySlots()
                )
                && hasIeCrateTexture();
    }

    private static boolean hasIeCrateTexture() {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(IE_CRATE_GUI).isPresent();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private int panelWidth() {
        return CratePreviewTooltipLayout.panelWidth(contents.columns());
    }

    private int panelHeight() {
        return CratePreviewTooltipLayout.panelHeight(contents.rows());
    }

    private boolean hasMessage() {
        return contents.status() != CratePreviewStatus.READY || contents.hiddenSlots() > 0;
    }

    private Component message() {
        if (contents.hiddenSlots() > 0) {
            return Component.translatable("immersive_crate_view_wimc_mod_porting.tooltip.crate_preview.truncated", contents.hiddenSlots());
        }
        if (contents.status() == CratePreviewStatus.LOOT_UNRESOLVED) {
            return Component.translatable("immersive_crate_view_wimc_mod_porting.tooltip.crate_preview.loot");
        }
        if (contents.status() == CratePreviewStatus.EMPTY) {
            return Component.translatable("immersive_crate_view_wimc_mod_porting.tooltip.crate_preview.empty");
        }
        return Component.empty();
    }
}
