package com.copperironalloy.lamp;

import net.dries007.tfc.common.capabilities.ItemStackFluidHandler;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.LampFuel;
import net.dries007.tfc.util.Tooltips;
import net.dries007.tfc.util.loot.CopyFluidFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Keeps lamp fuel on the item so breaking and placing does not spill it. */
public class MetalLampBlockItem extends BlockItem {
    public MetalLampBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ItemStackFluidHandler(stack, this::accepts, () -> TFCConfig.SERVER.lampCapacity.get());
    }

    private boolean accepts(Fluid fluid) {
        return LampFuel.get(fluid, getBlock().defaultBlockState()) != null;
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, @Nullable Player player, ItemStack stack, BlockState state) {
        boolean placed = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            CopyFluidFunction.copyFromItem(stack, be);
        }
        return placed;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            if (!handler.getFluidInTank(0).isEmpty()) {
                tooltip.add(Tooltips.fluidUnitsOf(handler.getFluidInTank(0)));
            }
        });
    }
}
