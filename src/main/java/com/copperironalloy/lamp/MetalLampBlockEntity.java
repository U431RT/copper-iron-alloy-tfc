package com.copperironalloy.lamp;

import com.copperironalloy.ModBlockEntities;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.LampFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/** Tank accepts only fuels {@link LampFuel} allows for this block. Lava is infinite on white steel. */
public class MetalLampBlockEntity extends BlockEntity {
    private final FluidTank tank;
    private final LazyOptional<IFluidHandler> holder;
    private long litAt;

    public MetalLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP.get(), pos, state);
        int capacity = TFCConfig.SERVER.lampCapacity.get();
        this.tank = new FluidTank(capacity, this::accepts) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                if (level != null && !level.isClientSide) {
                    BlockState state = getBlockState();
                    level.sendBlockUpdated(worldPosition, state, state, 3);
                    if (state.getValue(MetalLampBlock.LIT) && fuel() == null) {
                        extinguish();
                    }
                }
            }
        };
        this.holder = LazyOptional.of(() -> tank);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MetalLampBlockEntity be) {
        if (!state.getValue(MetalLampBlock.LIT)) {
            return;
        }
        LampFuel fuel = be.fuel();
        if (fuel == null) {
            be.extinguish();
            return;
        }
        int rate = fuel.getBurnRate();
        if (rate < 0) {
            return;
        }
        if (be.litAt <= 0) {
            be.litAt = level.getGameTime();
            be.setChanged();
            return;
        }
        long elapsed = level.getGameTime() - be.litAt;
        int consume = (int) (elapsed / rate);
        if (consume < 1) {
            return;
        }
        be.litAt += (long) consume * rate;
        FluidStack drained = be.tank.drain(consume, IFluidHandler.FluidAction.EXECUTE);
        if (be.tank.isEmpty() || drained.getAmount() < consume) {
            be.extinguish();
        }
        be.setChanged();
    }

    public boolean light() {
        if (level == null || level.isClientSide || getBlockState().getValue(MetalLampBlock.LIT)) {
            return false;
        }
        if (fuel() == null) {
            return false;
        }
        litAt = level.getGameTime();
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(MetalLampBlock.LIT, true));
        setChanged();
        return true;
    }

    public void extinguish() {
        if (level == null || !getBlockState().getValue(MetalLampBlock.LIT)) {
            return;
        }
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(MetalLampBlock.LIT, false));
        setChanged();
    }

    private boolean accepts(FluidStack stack) {
        return LampFuel.get(stack.getFluid(), getBlockState()) != null;
    }

    private @Nullable LampFuel fuel() {
        if (tank.isEmpty()) {
            return null;
        }
        return LampFuel.get(tank.getFluid().getFluid(), getBlockState());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("Tank"));
        litAt = tag.getLong("LitAt");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNBT(new CompoundTag()));
        tag.putLong("LitAt", litAt);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER || cap == Capabilities.FLUID) {
            return holder.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        holder.invalidate();
    }
}
