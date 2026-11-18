package com.copperironalloy.fridge;

import com.copperironalloy.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class FridgeMenu extends AbstractContainerMenu {
    private final FridgeBlockEntity blockEntity;
    private final FridgeBlockEntity partner;
    private final boolean stacked;
    private final int fridgeSlots;

    public FridgeMenu(int id, Inventory playerInventory, FridgeBlockEntity blockEntity) {
        super(ModMenus.FRIDGE.get(), id);
        this.blockEntity = blockEntity;
        this.partner = blockEntity.partner();
        this.stacked = partner != null;
        FridgeBlockEntity upper = blockEntity;
        FridgeBlockEntity lower = null;
        if (stacked) {
            FridgeBlock.Part part = FridgeBlock.part(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getBlockState());
            if (part == FridgeBlock.Part.UPPER) {
                upper = blockEntity;
                lower = partner;
            } else {
                upper = partner;
                lower = blockEntity;
            }
        }
        this.fridgeSlots = upper.inventory().getSlots() + (lower == null ? 0 : lower.inventory().getSlots());
        blockEntity.startOpen(playerInventory.player);
        int rows = this.fridgeSlots / 9;
        int yOffset = (rows - 4) * 18;
        addRows(upper, 0);
        if (lower != null) {
            addRows(lower, upper.tier().rows());
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOffset));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 161 + yOffset));
        }
    }

    private void addRows(FridgeBlockEntity fridge, int rowOffset) {
        int rows = fridge.tier().rows();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                int yRow = rowOffset + row;
                addSlot(new SlotItemHandler(fridge.inventory(), row * 9 + col, 8 + col * 18, 18 + yRow * 18));
            }
        }
    }

    public static FridgeMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buf) {
        BlockEntity be = inventory.player.level().getBlockEntity(buf.readBlockPos());
        if (be instanceof FridgeBlockEntity fridge) {
            return new FridgeMenu(id, inventory, fridge);
        }
        throw new IllegalStateException("Fridge is missing");
    }

    public FridgeBlockEntity blockEntity() {
        return blockEntity;
    }

    public int rows() {
        return fridgeSlots / 9;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!blockEntity.stillValid(player)) {
            return false;
        }
        FridgeBlockEntity now = blockEntity.partner();
        if (stacked) {
            return now != null && now.stillValid(player);
        }
        return now == null;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        blockEntity.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < fridgeSlots) {
            FridgeDecay.restore(stack, blockEntity.tier().trait());
            if (!moveItemStackTo(stack, fridgeSlots, slots.size(), true)) {
                putBack(stack);
                return ItemStack.EMPTY;
            }
            if (!stack.isEmpty()) {
                putBack(stack);
            }
        } else {
            FridgeDecay.chill(stack, blockEntity.tier().trait());
            if (!moveItemStackTo(stack, 0, fridgeSlots, false)) {
                FridgeDecay.restore(stack, blockEntity.tier().trait());
                return ItemStack.EMPTY;
            }
            if (!stack.isEmpty()) {
                FridgeDecay.restore(stack, blockEntity.tier().trait());
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /** A failed shift-click has to put the stack back in the mode this fridge is using now. */
    private void putBack(ItemStack stack) {
        if (blockEntity.inventory().frozen()) {
            FridgeDecay.freeze(stack);
        } else {
            FridgeDecay.chill(stack, blockEntity.tier().trait());
        }
    }
}
