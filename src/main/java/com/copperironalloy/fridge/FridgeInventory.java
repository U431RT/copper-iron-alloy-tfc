package com.copperironalloy.fridge;

import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/** Chills food as it enters and restores the original decay as it leaves. */
public final class FridgeInventory extends ItemStackHandler {
    private final FoodTrait trait;
    private final Runnable onChange;
    private boolean frozen;
    private boolean modeKnown;
    private boolean loading;

    public FridgeInventory(FridgeTier tier, Runnable onChange) {
        super(tier.slots());
        this.trait = tier.trait();
        this.onChange = onChange;
    }

    public boolean frozen() {
        return frozen;
    }

    /** Switch between the 0.15 trait and a full stop. Items already inside are converted. */
    public void setFrozen(boolean frozen) {
        if (modeKnown && this.frozen == frozen) {
            return;
        }
        modeKnown = true;
        this.frozen = frozen;
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (frozen) {
                FridgeDecay.freeze(stack);
            } else {
                FridgeDecay.thaw(stack);
                FridgeDecay.chill(stack, trait);
            }
        }
        onChange.run();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack prepared = stack.copy();
        if (frozen) {
            FridgeDecay.freeze(prepared);
        } else {
            FridgeDecay.chill(prepared, trait);
        }
        ItemStack leftover = super.insertItem(slot, prepared, simulate);
        if (!leftover.isEmpty()) {
            FridgeDecay.restore(leftover, trait);
        }
        return leftover;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack out = super.extractItem(slot, amount, simulate);
        if (!simulate) {
            FridgeDecay.restore(out, trait);
        }
        return out;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (!loading && !stack.isEmpty()) {
            if (frozen) {
                FridgeDecay.freeze(stack);
            } else {
                FridgeDecay.thaw(stack);
                FridgeDecay.chill(stack, trait);
            }
        }
        super.setStackInSlot(slot, stack);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        loading = true;
        super.deserializeNBT(nbt);
        loading = false;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }

    public void restoreAndClear() {
        for (int i = 0; i < getSlots(); i++) {
            FridgeDecay.restore(getStackInSlot(i), trait);
        }
    }

    /** Used when the block is broken. Restores decay, then detaches the stacks. */
    public NonNullList<ItemStack> takeRestored() {
        restoreAndClear();
        NonNullList<ItemStack> copy = NonNullList.withSize(getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < getSlots(); i++) {
            copy.set(i, getStackInSlot(i).copy());
            super.setStackInSlot(i, ItemStack.EMPTY);
        }
        return copy;
    }
}
