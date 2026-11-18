package com.copperironalloy.fluid;

import com.copperironalloy.BucketContents;
import com.copperironalloy.BucketLogic;
import com.copperironalloy.ModConstants;
import net.dries007.tfc.util.Drinkable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.ForgeMod;

/** Steel-gold cup: 100 mB of any fluid. Drinking empties it. */
public final class CupBehavior implements FluidContainerBehavior {
    @Override
    public int capacity() {
        return ModConstants.CUP_CAPACITY;
    }

    @Override
    public boolean accepts(ItemStack stack, Fluid fluid) {
        return fluid != null && !fluid.isSame(net.minecraft.world.level.material.Fluids.EMPTY);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = ctx.getItemInHand();
        FluidState state = level.getFluidState(ctx.getClickedPos());
        if (!player.isShiftKeyDown() && !state.isEmpty() && state.isSource()) {
            Fluid fluid = state.getType();
            Fluid current = BucketContents.fluid(stack);
            int amount = BucketContents.amount(stack);
            if ((current == null || current == fluid) && amount < capacity()) {
                if (!level.isClientSide) {
                    BucketContents.set(stack, fluid, capacity());
                    level.setBlockAndUpdate(ctx.getClickedPos(), Blocks.AIR.defaultBlockState());
                    level.playSound(null, ctx.getClickedPos(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (player.isShiftKeyDown() && BucketLogic.discard(level, player, stack)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (BucketLogic.pour(level, player, stack, new BlockPlaceContext(ctx), false)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = AlloyFluidContainerItem.raytrace(level, player, ClipContext.Fluid.SOURCE_ONLY);
        FluidState state = level.getFluidState(hit.getBlockPos());
        if (!player.isShiftKeyDown() && !state.isEmpty() && state.isSource() && fillFrom(level, stack, state, hit.getBlockPos())) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        Fluid held = BucketContents.fluid(stack);
        if (held == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.isShiftKeyDown() && BucketLogic.discard(level, player, stack)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (Drinkable.get(held) == null && !BucketLogic.isMilk(held)) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int useDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim useAnim(ItemStack stack) {
        return BucketContents.fluid(stack) == null ? UseAnim.NONE : UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, Level level, LivingEntity entity) {
        Fluid fluid = BucketContents.fluid(stack);
        if (fluid == null || !(entity instanceof Player player)) {
            return stack;
        }
        Drinkable drinkable = Drinkable.get(fluid);
        if (drinkable != null) {
            drinkable.onDrink(player, ModConstants.CUP_DRINK_STRENGTH);
        } else if (BucketLogic.isMilk(fluid)) {
            player.removeAllEffects();
        } else {
            return stack;
        }
        BucketContents.set(stack, null, 0);
        player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, java.util.List<net.minecraft.network.chat.Component> tooltip) {
        if (BucketContents.amount(stack) > 0) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.copper_iron_alloy.shift_empty"));
        }
    }

    private boolean fillFrom(Level level, ItemStack stack, FluidState state, net.minecraft.core.BlockPos pos) {
        Fluid fluid = state.getType();
        if (!accepts(stack, fluid)) {
            return false;
        }
        Fluid current = BucketContents.fluid(stack);
        if (current != null && current != fluid) {
            return false;
        }
        if (BucketContents.amount(stack) >= capacity()) {
            return false;
        }
        if (!level.isClientSide) {
            BucketContents.set(stack, fluid, capacity());
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }
}
