package com.copperironalloy.fridge;

import com.copperironalloy.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class FridgeBlockEntity extends BaseContainerBlockEntity implements LidBlockEntity {
    private final FridgeTier tier;
    private final FridgeInventory items;
    private final LazyOptional<IItemHandler> itemCap;
    private final ChestLidController lid = new ChestLidController();
    private int localOpen;
    private boolean partnerOpen;
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            play(level, pos, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            play(level, pos, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int previous) {
            localOpen = count;
            broadcastOpen();
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof FridgeMenu menu && menu.blockEntity() == FridgeBlockEntity.this;
        }
    };

    public FridgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRIDGE.get(), pos, state);
        this.tier = ((FridgeBlock) state.getBlock()).tier();
        this.items = new FridgeInventory(tier, this::setChanged);
        this.itemCap = LazyOptional.of(() -> items);
    }

    public FridgeTier tier() {
        return tier;
    }

    public FridgeInventory inventory() {
        return items;
    }

    public @Nullable FridgeBlockEntity partner() {
        if (level == null || tier != FridgeTier.WHITE_STEEL) {
            return null;
        }
        BlockState state = getBlockState();
        FridgeBlock.Part part = FridgeBlock.part(level, worldPosition, state);
        if (part == FridgeBlock.Part.SINGLE) {
            return null;
        }
        BlockPos otherPos = part == FridgeBlock.Part.LOWER ? worldPosition.above() : worldPosition.below();
        BlockEntity other = level.getBlockEntity(otherPos);
        if (!(other instanceof FridgeBlockEntity fridge) || fridge.tier != FridgeTier.WHITE_STEEL) {
            return null;
        }
        FridgeBlock.Part otherPart = FridgeBlock.part(level, otherPos, fridge.getBlockState());
        boolean paired = (part == FridgeBlock.Part.LOWER && otherPart == FridgeBlock.Part.UPPER)
                || (part == FridgeBlock.Part.UPPER && otherPart == FridgeBlock.Part.LOWER);
        return paired ? fridge : null;
    }

    private void reconcilePreservation() {
        if (level == null || level.isClientSide || tier != FridgeTier.WHITE_STEEL) {
            return;
        }
        boolean stacked = FridgeBlock.part(level, worldPosition, getBlockState()) != FridgeBlock.Part.SINGLE;
        items.setFrozen(stacked);
    }

    private void broadcastOpen() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean open = localOpen > 0 || partnerOpen;
        BlockState state = getBlockState();
        if (state.getValue(FridgeBlock.OPEN) != open) {
            level.setBlock(worldPosition, state.setValue(FridgeBlock.OPEN, open), 3);
            state = getBlockState();
        }
        level.blockEvent(worldPosition, state.getBlock(), 1, open ? 1 : 0);
        FridgeBlockEntity other = partner();
        if (other != null && other.partnerOpen != (localOpen > 0)) {
            other.partnerOpen = localOpen > 0;
            other.broadcastOpen();
        }
    }

    public float getOpenNess(float partialTick) {
        return lid.getOpenness(partialTick);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FridgeBlockEntity be) {
        be.lid.tickLid();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FridgeBlockEntity be) {
        be.openers.recheckOpeners(level, pos, state);
        be.reconcilePreservation();
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) {
            lid.shouldBeOpen(type > 0);
            return true;
        }
        return super.triggerEvent(id, type);
    }

    @Override
    public void startOpen(Player player) {
        if (!isRemoved() && !player.isSpectator() && level != null) {
            openers.incrementOpeners(player, level, worldPosition, getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!isRemoved() && !player.isSpectator() && level != null) {
            openers.decrementOpeners(player, level, worldPosition, getBlockState());
        }
    }

    private void play(Level level, BlockPos pos, boolean open) {
        if (level.isClientSide) {
            return;
        }
        boolean stacked = tier == FridgeTier.WHITE_STEEL
                && FridgeBlock.part(level, pos, level.getBlockState(pos)) != FridgeBlock.Part.SINGLE;
        boolean lid = tier == FridgeTier.WHITE_STEEL && !stacked;
        level.playSound(null, pos,
                open ? (stacked ? SoundEvents.IRON_DOOR_OPEN : lid ? SoundEvents.CHEST_OPEN : SoundEvents.BARREL_OPEN)
                        : (stacked ? SoundEvents.IRON_DOOR_CLOSE : lid ? SoundEvents.CHEST_CLOSE : SoundEvents.BARREL_CLOSE),
                SoundSource.BLOCKS, lid ? 0.5F : 0.7F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        NonNullList<ItemStack> stacks = items.takeRestored();
        for (ItemStack stack : stacks) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.copper_iron_alloy." + tier.blockName());
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new FridgeMenu(id, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return items.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < items.getSlots(); i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return items.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = items.getStackInSlot(slot);
        items.setStackInSlot(slot, ItemStack.EMPTY);
        FridgeDecay.restore(stack, tier.trait());
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.setStackInSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.takeRestored();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCap.invalidate();
    }
}
