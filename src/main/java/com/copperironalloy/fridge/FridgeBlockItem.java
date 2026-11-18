package com.copperironalloy.fridge;

import com.copperironalloy.client.WhiteSteelFridgeRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** White-steel fridge renders as a chest, so the item needs the same renderer. */
public class FridgeBlockItem extends BlockItem {
    public FridgeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        WhiteSteelFridgeRenderer.ItemClient.register(consumer);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.copper_iron_alloy.white_steel_fridge").withStyle(ChatFormatting.GRAY));
    }
}
