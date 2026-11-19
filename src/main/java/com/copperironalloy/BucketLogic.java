package com.copperironalloy;

import net.dries007.tfc.common.fluids.FluidHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
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

    public static boolean pour(Level level, Player player, ItemStack stack, UseOnContext ctx) {
        return pour(level, player, stack, ctx, true);
    }

    public static boolean pour(Level level, Player player, ItemStack stack, UseOnContext ctx, boolean allowLava) {
        Fluid current = BucketContents.fluid(stack);
        int stored = BucketContents.amount(stack);
        if (current == null || stored <= 0) {
            return false;
        }
        boolean noWorld = isMilk(current) || (!allowLava && isLava(current));
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        if (!level.isClientSide) {
            var be = level.getBlockEntity(clicked);
            if (be != null) {
                var handler = be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER,
                        face).orElse(null);
                if (handler != null) {
                    int amount = player.isShiftKeyDown() ? stored : Math.min(1000, stored);
                    int accepted = handler.fill(new net.minecraftforge.fluids.FluidStack(current, amount),
                            net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                    if (accepted > 0) {
                        int left = stored - accepted;
                        BucketContents.set(stack, left > 0 ? current : null, Math.max(0, left));
                        BucketOverlay.refresh(stack);
                        level.playSound(null, clicked, SoundEvents.BUCKET_EMPTY,
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
            BlockPos pos = fluidPos(level, clicked, face, current);
            if (!placeFluid(level, pos, current)) {
                return false;
            }
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

    /**
     * Default pour matches TerraFirmaCraft emptying less than a bucket: a level-1 flow that runs off.
     * A source block is only placed when {@code place_fluid_sources} is turned on.
     * Returns false when nothing was placed, so the container keeps its fluid.
     */
    /**
     * {@link BlockPlaceContext#getClickedPos()} is already the cell next to a solid face.
     * Offsetting by the face again puts the fluid one block too far: above the air on a top click,
     * and inside the next solid block on a side click.
     */
    public static BlockPos fluidPos(Level level, BlockPos clicked, Direction face, Fluid fluid) {
        BlockState state = level.getBlockState(clicked);
        if (state.canBeReplaced(fluid)) {
            return clicked;
        }
        return clicked.relative(face);
    }

    public static boolean placeFluid(Level level, BlockPos pos, Fluid fluid) {
        if (!level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        boolean source = CopperIronAlloyConfig.PLACE_FLUID_SOURCES.get();
        BlockState state;
        if (source) {
            state = fluid.defaultFluidState().createLegacyBlock();
        } else if (fluid instanceof FlowingFluid flowing) {
            state = flowing.getFlowing(1, false).createLegacyBlock();
        } else {
            return false;
        }
        level.setBlockAndUpdate(pos, state);
        if (!source) {
            FluidHelpers.tickFluid(level, pos, level.getBlockState(pos));
        }
        return true;
    }

    private BucketLogic() {
    }
}
