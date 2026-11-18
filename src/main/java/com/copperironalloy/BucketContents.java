package com.copperironalloy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Fluid stored on the item stack. New stacks use Forge's {@code Fluid} compound,
 * which {@code IFluidHandlerItem} reads. Stacks from older worlds used
 * {@code CopperIronAlloyFluid} with a fluid id string; that form is still read and
 * rewritten alongside the Forge compound so a save keeps its contents.
 */
public final class BucketContents {
    static final String LEGACY = "CopperIronAlloyFluid";
    private static final String FORGE_FLUID = "Fluid";
    private static final String LEGACY_FLUID = "Fluid";
    private static final String AMOUNT = "Amount";

    public static int capacity(ItemStack stack) {
        if (stack.is(ModItems.STEEL_GOLD_BUCKET.get())) {
            return ModConstants.STEEL_GOLD_BUCKET_CAPACITY;
        }
        if (stack.is(ModTools.KETTLE.get())) {
            return ModConstants.KETTLE_CAPACITY;
        }
        if (stack.is(ModTools.CUP.get())) {
            return ModConstants.CUP_CAPACITY;
        }
        return ModConstants.IRON_COPPER_BUCKET_CAPACITY;
    }

    public static @Nullable Fluid fluid(ItemStack stack) {
        FluidStack stored = read(stack);
        return stored.isEmpty() ? null : stored.getFluid();
    }

    public static int amount(ItemStack stack) {
        return read(stack).getAmount();
    }

    public static FluidStack read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return FluidStack.EMPTY;
        }
        if (tag.contains(FORGE_FLUID, Tag.TAG_COMPOUND)) {
            FluidStack loaded = FluidStack.loadFluidStackFromNBT(tag.getCompound(FORGE_FLUID));
            if (!loaded.isEmpty()) {
                return loaded;
            }
        }
        if (!tag.contains(LEGACY, Tag.TAG_COMPOUND)) {
            return FluidStack.EMPTY;
        }
        CompoundTag data = tag.getCompound(LEGACY);
        if (!data.contains(LEGACY_FLUID, Tag.TAG_STRING)) {
            return FluidStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(data.getString(LEGACY_FLUID));
        if (id == null) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        int stored = data.getInt(AMOUNT);
        if (fluid == null || fluid == Fluids.EMPTY || stored <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, stored);
    }

    public static void set(ItemStack stack, @Nullable Fluid fluid, int amount) {
        set(stack, fluid == null || amount <= 0 ? FluidStack.EMPTY : new FluidStack(fluid, amount));
    }

    public static void set(ItemStack stack, FluidStack fluid) {
        CompoundTag tag = stack.getOrCreateTag();
        if (fluid.isEmpty()) {
            tag.remove(LEGACY);
            tag.remove(FORGE_FLUID);
        } else {
            CompoundTag legacy = new CompoundTag();
            ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
            legacy.putString(LEGACY_FLUID, id == null ? "" : id.toString());
            legacy.putInt(AMOUNT, fluid.getAmount());
            tag.put(LEGACY, legacy);
            tag.put(FORGE_FLUID, fluid.writeToNBT(new CompoundTag()));
        }
        BucketOverlay.refresh(stack);
    }

    /** Water (including TFC river, spring and salt water), lava, milk, and molten metal for the steel-gold bucket only. */
    public static boolean accepts(ItemStack stack, Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER
                || fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA
                || fluid == ForgeMod.MILK.get() || fluid == ForgeMod.FLOWING_MILK.get()) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
        if (key == null) {
            return false;
        }
        if ("tfc".equals(key.getNamespace())) {
            return switch (key.getPath()) {
                case "salt_water", "spring_water", "river_water",
                        "flowing_salt_water", "flowing_spring_water", "flowing_river_water" -> true;
                default -> false;
            };
        }
        if (CopperIronAlloy.MODID.equals(key.getNamespace()) && key.getPath().startsWith("metal/")) {
            return stack.is(ModItems.STEEL_GOLD_BUCKET.get());
        }
        return false;
    }

    /** Water and lava fill the remaining space in one scoop. Everything else moves {@link ModConstants#SCOOP_STEP}. */
    public static boolean bulk(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER
                || fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.FLUIDS.getKey(fluid);
        return key != null && "tfc".equals(key.getNamespace())
                && (key.getPath().contains("salt_water") || key.getPath().contains("spring_water")
                || key.getPath().contains("river_water"));
    }

    private BucketContents() {
    }
}
