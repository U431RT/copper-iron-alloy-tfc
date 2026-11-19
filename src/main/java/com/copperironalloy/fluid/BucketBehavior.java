package com.copperironalloy.fluid;

import com.copperironalloy.BucketContents;
import com.copperironalloy.BucketLogic;
import com.copperironalloy.CopperIronAlloyConfig;
import com.copperironalloy.ModConstants;
import net.dries007.tfc.common.entities.TFCEntities;
import net.dries007.tfc.common.entities.livestock.DairyAnimal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ForgeMod;

import java.util.List;

/** Iron-copper bucket and steel-gold bucket: scoop, pour, and milk. */
public final class BucketBehavior implements FluidContainerBehavior {
    private static final String MAID_CLASS = "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";
    private static final String MAID_MILKED_KEY = "copper_iron_alloy_maid_milked";

    private final int capacity;

    public BucketBehavior(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public boolean accepts(ItemStack stack, Fluid fluid) {
        return BucketContents.accepts(stack, fluid);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = AlloyFluidContainerItem.raytrace(level, player, ClipContext.Fluid.SOURCE_ONLY);
        BlockPos pos = hit.getBlockPos();
        FluidState state = level.getFluidState(pos);
        Fluid fluid = state.getType();
        Fluid current = BucketContents.fluid(stack);
        int amount = BucketContents.amount(stack);
        boolean shift = player.isShiftKeyDown();

        if (!shift && state.isSource() && !state.isEmpty() && accepts(stack, fluid)) {
            boolean same = current == null || current == fluid;
            int space = capacity - amount;
            if (same && space > 0) {
                int add = BucketContents.bulk(fluid) ? space : Math.min(ModConstants.SCOOP_STEP, space);
                if (!level.isClientSide) {
                    BucketContents.set(stack, fluid, amount + add);
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        if (current != null && amount > 0 && shift && BucketLogic.isMilk(current)) {
            BucketLogic.discard(level, player, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (current != null && amount > 0 && !BucketLogic.isMilk(current)) {
            if (!level.isClientSide) {
                BlockPos target = BucketLogic.fluidPos(level, hit.getBlockPos(), hit.getDirection(), current);
                if (!BucketLogic.placeFluid(level, target, current)) {
                    return InteractionResultHolder.fail(stack);
                }
                BucketContents.set(stack, null, 0);
                level.playSound(null, target, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            FluidState state = level.getFluidState(ctx.getClickedPos());
            if (!state.isEmpty() && state.isSource() && accepts(stack, state.getType())) {
                if (BucketLogic.scoop(level, player, stack, ctx.getClickedPos(), capacity)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        if (BucketLogic.pour(level, player, stack, ctx)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, List<Component> tooltip) {
        Fluid fluid = BucketContents.fluid(stack);
        int amount = BucketContents.amount(stack);
        if (fluid == null) {
            tooltip.add(Component.translatable("tooltip.copper_iron_alloy.bucket.empty", capacity));
        } else {
            tooltip.add(Component.translatable("tooltip.copper_iron_alloy.bucket.filled",
                    fluid.getFluidType().getDescription(), amount, capacity));
            if (BucketLogic.isMilk(fluid)) {
                tooltip.add(Component.translatable("tooltip.copper_iron_alloy.shift_empty"));
            }
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!CopperIronAlloyConfig.MILKING_ENABLED.get()) {
            return InteractionResult.PASS;
        }
        if (isTfcDairyAnimal(target) && target instanceof DairyAnimal dairy) {
            boolean ready = CopperIronAlloyConfig.BYPASS_MILKING_RESTRICTION.get() || dairy.hasProduct();
            if (!ready) {
                return InteractionResult.PASS;
            }
            InteractionResult result = fillMilk(stack, player);
            if (result != InteractionResult.PASS && !player.level().isClientSide) {
                dairy.setProductsCooldown();
            }
            return result;
        }
        if (isMaid(target)) {
            return milkMaid(stack, player, target);
        }
        if (target instanceof Cow && !(target instanceof DairyAnimal)) {
            return fillMilk(stack, player);
        }
        return InteractionResult.PASS;
    }

    private static boolean isMaid(LivingEntity entity) {
        return CopperIronAlloyConfig.ALLOW_MAID_MILKING.get() && MAID_CLASS.equals(entity.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private InteractionResult milkMaid(ItemStack stack, Player player, LivingEntity maid) {
        List<? extends String> whitelist =
                (List<? extends String>) CopperIronAlloyConfig.MAID_MODEL_WHITELIST.get();
        if (!whitelist.isEmpty()) {
            String modelId = readModelId(maid);
            if (modelId == null || whitelist.stream().noneMatch(modelId::equals)) {
                return InteractionResult.PASS;
            }
        }
        if (CopperIronAlloyConfig.MAID_MILKING_COOLDOWN.get()) {
            long last = maid.getPersistentData().getLong(MAID_MILKED_KEY);
            long now = maid.level().getGameTime();
            if (last > 0 && now - last < CopperIronAlloyConfig.MAID_COOLDOWN_TICKS.get()) {
                return InteractionResult.PASS;
            }
            InteractionResult result = fillMilk(stack, player);
            if (result != InteractionResult.PASS && !player.level().isClientSide) {
                maid.getPersistentData().putLong(MAID_MILKED_KEY, now);
            }
            return result;
        }
        return fillMilk(stack, player);
    }

    private static String readModelId(LivingEntity maid) {
        try {
            return (String) maid.getClass().getMethod("getModelId").invoke(maid);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean isTfcDairyAnimal(LivingEntity entity) {
        return entity.getType() == TFCEntities.COW.get()
                || entity.getType() == TFCEntities.GOAT.get()
                || entity.getType() == TFCEntities.YAK.get();
    }

    private InteractionResult fillMilk(ItemStack stack, Player player) {
        Fluid milk = ForgeMod.MILK.get();
        Fluid current = BucketContents.fluid(stack);
        int amount = BucketContents.amount(stack);
        if (current != null && current != milk) {
            return InteractionResult.PASS;
        }
        if (amount >= capacity) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide) {
            BucketContents.set(stack, milk, capacity);
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
