package com.copperironalloy;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.ForgeMod;

/** 装/倒共用的逻辑：瞄准液体方块时由 useOn 调用。 */
public final class BucketLogic {
    /** 返回 true 表示这次交互已经被处理。 */
    public static boolean scoop(Level level, Player player, ItemStack stack, BlockPos pos, int capacity) {
        FluidState state = level.getFluidState(pos);
        if (state.isEmpty() || !state.isSource()) {
            return false;
        }
        Fluid fluid = state.getType();
        Fluid current = BucketContents.fluid(stack);
        int amount = BucketContents.amount(stack);
        boolean same = current == null || current == fluid;
        int space = capacity - amount;
        if (!same || space <= 0) {
            return false;
        }
        int add = BucketContents.bulk(fluid) ? space : Math.min(1000, space);
        if (!level.isClientSide) {
            BucketContents.set(stack, fluid, amount + add);
            BucketOverlay.refresh(stack);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }

    public static boolean isMilk(Fluid fluid) {
        return fluid == ForgeMod.MILK.get() || fluid == ForgeMod.FLOWING_MILK.get();
    }

    public static boolean isLava(Fluid fluid) {
        return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
    }

    /** Throw the contents away. Used when the fluid has no block, and for a forced empty. */
    public static boolean discard(Level level, Player player, ItemStack stack) {
        if (BucketContents.amount(stack) <= 0) {
            return false;
        }
        if (!level.isClientSide) {
            BucketContents.set(stack, null, 0);
            player.playSound(SoundEvents.BUCKET_EMPTY, 1.0F, 1.0F);
        }
        return true;
    }

    public static boolean pour(Level level, Player player, ItemStack stack, BlockPlaceContext ctx) {
        return pour(level, player, stack, ctx, true);
    }

    public static boolean pour(Level level, Player player, ItemStack stack, BlockPlaceContext ctx, boolean allowLava) {
        Fluid current = BucketContents.fluid(stack);
        int stored = BucketContents.amount(stack);
        if (current == null || stored <= 0) {
            return false;
        }
        boolean noWorld = isMilk(current) || (!allowLava && isLava(current));
        if (!level.isClientSide) {
            var be = level.getBlockEntity(ctx.getClickedPos());
            if (be != null) {
                var handler = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
                        ctx.getClickedFace()).orElse(null);
                if (handler != null) {
                    int amount = player.isShiftKeyDown() ? stored : Math.min(1000, stored);
                    int accepted = handler.fill(new net.minecraftforge.fluids.FluidStack(current, amount),
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    if (accepted > 0) {
                        int left = stored - accepted;
                        BucketContents.set(stack, left > 0 ? current : null, Math.max(0, left));
                        BucketOverlay.refresh(stack);
                        level.playSound(null, ctx.getClickedPos(), SoundEvents.BUCKET_EMPTY,
                                SoundSource.PLAYERS, 1.0F, 1.0F);
                        return true;
                    }
                }
            }
            if (noWorld) {
                if (player.isShiftKeyDown()) {
                    return discard(level, player, stack);
                }
                return false;
            }
            int amount = player.isShiftKeyDown() ? stored : Math.min(1000, stored);
            BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            level.setBlockAndUpdate(pos, current.defaultFluidState().createLegacyBlock());
            BucketContents.set(stack, stored - amount > 0 ? current : null, Math.max(0, stored - amount));
            BucketOverlay.refresh(stack);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        if (noWorld) {
            return player.isShiftKeyDown();
        }
        return true;
    }

    private BucketLogic() {
    }
}
