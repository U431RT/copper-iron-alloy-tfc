package com.copperironalloy.fluid;

import com.copperironalloy.BucketContents;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** One tank on the item stack. The whitelist is the container's own rule. */
public final class AlloyFluidHandler implements IFluidHandlerItem, ICapabilityProvider {
    private final ItemStack stack;
    private final int capacity;
    private final Predicate<Fluid> accepts;
    private final LazyOptional<IFluidHandlerItem> holder = LazyOptional.of(() -> this);

    public AlloyFluidHandler(ItemStack stack, int capacity, Predicate<Fluid> accepts) {
        this.stack = stack;
        this.capacity = capacity;
        this.accepts = accepts;
    }

    @Override
    public ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return BucketContents.read(stack);
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack fluid) {
        return !fluid.isEmpty() && accepts.test(fluid.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !accepts.test(resource.getFluid())) {
            return 0;
        }
        FluidStack stored = BucketContents.read(stack);
        if (!stored.isEmpty() && !stored.isFluidEqual(resource)) {
            return 0;
        }
        int filled = Math.min(resource.getAmount(), capacity - stored.getAmount());
        if (filled <= 0) {
            return 0;
        }
        if (action.execute()) {
            BucketContents.set(stack, new FluidStack(resource, stored.getAmount() + filled));
        }
        return filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack stored = BucketContents.read(stack);
        if (resource.isEmpty() || stored.isEmpty() || !stored.isFluidEqual(resource)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack stored = BucketContents.read(stack);
        if (stored.isEmpty() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int drained = Math.min(maxDrain, stored.getAmount());
        FluidStack out = new FluidStack(stored, drained);
        if (action.execute()) {
            int left = stored.getAmount() - drained;
            BucketContents.set(stack, left > 0 ? new FluidStack(stored, left) : FluidStack.EMPTY);
        }
        return out;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM || cap == ForgeCapabilities.FLUID_HANDLER) {
            return holder.cast();
        }
        return LazyOptional.empty();
    }
}
