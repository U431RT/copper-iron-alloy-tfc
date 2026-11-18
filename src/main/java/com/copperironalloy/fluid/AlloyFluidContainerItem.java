package com.copperironalloy.fluid;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AlloyFluidContainerItem extends Item {
    private final FluidContainerBehavior behavior;

    public AlloyFluidContainerItem(Properties properties, FluidContainerBehavior behavior) {
        super(properties);
        this.behavior = behavior;
    }

    public FluidContainerBehavior behavior() {
        return behavior;
    }

    public static BlockHitResult raytrace(Level level, Player player, ClipContext.Fluid fluid) {
        return getPlayerPOVHitResult(level, player, fluid);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new AlloyFluidHandler(stack, behavior.capacity(), fluid -> behavior.accepts(stack, fluid));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return behavior.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        return behavior.useOn(ctx);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return behavior.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, TooltipFlag flag) {
        behavior.appendHoverText(stack, tooltip);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return behavior.useDuration(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return behavior.useAnim(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return behavior.finishUsing(stack, level, entity);
    }
}
