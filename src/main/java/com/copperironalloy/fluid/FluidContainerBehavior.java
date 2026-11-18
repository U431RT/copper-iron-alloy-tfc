package com.copperironalloy.fluid;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/** How one fluid container is filled, emptied, and drunk. Storage is always the fluid capability. */
public interface FluidContainerBehavior {
    int capacity();

    boolean accepts(ItemStack stack, Fluid fluid);

    InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand);

    InteractionResult useOn(UseOnContext ctx);

    default InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    default void appendHoverText(ItemStack stack, List<Component> tooltip) {
    }

    /** 0 means a click finishes immediately. Drink containers use the drink animation instead. */
    default int useDuration(ItemStack stack) {
        return 0;
    }

    default UseAnim useAnim(ItemStack stack) {
        return UseAnim.NONE;
    }

    default ItemStack finishUsing(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }
}
