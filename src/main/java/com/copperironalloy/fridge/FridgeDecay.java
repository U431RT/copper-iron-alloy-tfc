package com.copperironalloy.fridge;

import net.dries007.tfc.common.capabilities.food.FoodCapability;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.common.capabilities.food.IFood;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Cabinet fridges use a normal TFC trait. {@code applyTrait} stretches the
 * creation date by {@code 1 / modifier}, and {@code removeTrait} writes it back.
 * A zero modifier cannot go through that pair: TFC divides by the modifier.
 * The stacked white-steel fridge instead keeps the age in {@code ciaFrozenAge}
 * and pins a zero-modifier trait on the list so the rotten date stays infinite
 * until the food leaves.
 */
public final class FridgeDecay {
    public static final String FROZEN_AGE = "ciaFrozenAge";

    public static void chill(ItemStack stack, FoodTrait trait) {
        if (!stack.isEmpty() && trait != null) {
            FoodCapability.applyTrait(stack, trait);
        }
    }

    public static void restore(ItemStack stack, FoodTrait trait) {
        if (stack.isEmpty()) {
            return;
        }
        thaw(stack);
        if (trait != null) {
            FoodCapability.removeTrait(stack, trait);
        }
    }

    /** Stop the calendar while this stack is in a vertical white-steel fridge. */
    public static void freeze(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        FoodTrait frozen = FridgeTier.WHITE_STEEL.frozenTrait();
        FoodTrait single = FridgeTier.WHITE_STEEL.trait();
        IFood food = FoodCapability.get(stack);
        if (food == null || frozen == null || food.isRotten() || food.isTransientNonDecaying()) {
            return;
        }
        if (single != null && food.hasTrait(single)) {
            FoodCapability.removeTrait(stack, single);
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(FROZEN_AGE)) {
            long now = Calendars.get().getTicks();
            long created = food.getCreationDate();
            long age = created < 0L ? 0L : Math.max(0L, now - created);
            tag.putLong(FROZEN_AGE, age);
        }
        if (!food.hasTrait(frozen)) {
            food.getTraits().add(frozen);
        }
    }

    /** Put the saved age back on the calendar and drop the frozen trait. */
    public static void thaw(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        FoodTrait frozen = FridgeTier.WHITE_STEEL.frozenTrait();
        IFood food = FoodCapability.get(stack);
        CompoundTag tag = stack.getTag();
        boolean marked = tag != null && tag.contains(FROZEN_AGE);
        if (food != null && frozen != null && food.hasTrait(frozen)) {
            food.getTraits().remove(frozen);
        }
        if (!marked || food == null) {
            return;
        }
        long age = tag.getLong(FROZEN_AGE);
        tag.remove(FROZEN_AGE);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
        long now = Calendars.get().getTicks();
        food.setCreationDate(now - age);
    }

    private FridgeDecay() {
    }
}
