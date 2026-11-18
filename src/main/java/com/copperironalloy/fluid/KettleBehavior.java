package com.copperironalloy.fluid;

import com.copperironalloy.BucketContents;
import com.copperironalloy.BucketLogic;
import com.copperironalloy.ModConstants;
import net.dries007.tfc.util.Drinkable;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/** Steel-gold kettle: fills with anything TFC marks drinkable, then sips it. */
public final class KettleBehavior implements FluidContainerBehavior {
    @Override
    public int capacity() {
        return ModConstants.KETTLE_CAPACITY;
    }

    @Override
    public boolean accepts(ItemStack stack, Fluid fluid) {
        return Drinkable.get(fluid) != null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown() && BucketLogic.discard(level, player, ctx.getItemInHand())) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        FluidState state = level.getFluidState(ctx.getClickedPos());
        if (!state.isEmpty() && state.isSource() && scoop(level, ctx.getItemInHand(), state, ctx.getClickedPos())) {
            if (!level.isClientSide) {
                level.playSound(null, ctx.getClickedPos(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && BucketLogic.discard(level, player, stack)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        BlockHitResult hit = AlloyFluidContainerItem.raytrace(level, player, ClipContext.Fluid.SOURCE_ONLY);
        FluidState state = level.getFluidState(hit.getBlockPos());
        if (!state.isEmpty() && state.isSource() && scoop(level, stack, state, hit.getBlockPos())) {
            if (!level.isClientSide) {
                player.playSound(SoundEvents.BUCKET_FILL, 1.0F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        Fluid fluid = BucketContents.fluid(stack);
        if (fluid == null || Drinkable.get(fluid) == null) {
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
        Drinkable drinkable = fluid == null ? null : Drinkable.get(fluid);
        if (drinkable == null || !(entity instanceof Player player)) {
            return stack;
        }
        int amount = BucketContents.amount(stack);
        int drink = Math.min(ModConstants.KETTLE_DRINK_AMOUNT, amount);
        int strength = Math.max(1, drink * ModConstants.KETTLE_DRINK_STRENGTH / ModConstants.KETTLE_DRINK_AMOUNT);
        drinkable.onDrink(player, strength);
        BucketContents.set(stack, amount - drink > 0 ? fluid : null, Math.max(0, amount - drink));
        player.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
        return stack;
    }

    private boolean scoop(Level level, ItemStack stack, FluidState state, net.minecraft.core.BlockPos pos) {
        Fluid fluid = state.getType();
        if (!accepts(stack, fluid)) {
            return false;
        }
        Fluid current = BucketContents.fluid(stack);
        int amount = BucketContents.amount(stack);
        int space = capacity() - amount;
        if ((current != null && current != fluid) || space <= 0) {
            return false;
        }
        if (!level.isClientSide) {
            BucketContents.set(stack, fluid, amount + space);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, List<Component> tooltip) {
        Fluid fluid = BucketContents.fluid(stack);
        if (fluid == null) {
            tooltip.add(Component.translatable("tooltip.copper_iron_alloy.kettle.empty", capacity()));
        } else {
            tooltip.add(Component.translatable("tooltip.copper_iron_alloy.bucket.filled",
                    fluid.getFluidType().getDescription(), BucketContents.amount(stack), capacity()));
            tooltip.add(Component.translatable("tooltip.copper_iron_alloy.shift_empty"));
        }
    }
}
