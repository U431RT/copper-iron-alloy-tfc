package com.copperironalloy.client;

import com.copperironalloy.fridge.FridgeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FridgeScreen extends AbstractContainerScreen<FridgeMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private final int rows;

    public FridgeScreen(FridgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.rows = menu.rows();
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, 17);
        for (int row = 0; row < rows; row++) {
            graphics.blit(TEXTURE, x, y + 17 + row * 18, 0, 17, imageWidth, 18);
        }
        graphics.blit(TEXTURE, x, y + 17 + rows * 18, 0, 126, imageWidth, 96);
    }
}
