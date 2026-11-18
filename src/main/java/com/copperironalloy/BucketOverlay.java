package com.copperironalloy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/** 桶里装什么 -> 对应一个 custom_model_data，客户端据此切换"液面圆圈"的贴图。 */
public final class BucketOverlay {
    public static final int NONE = 0;
    public static final int WATER = 1;
    public static final int MILK = 2;
    public static final int LAVA = 3;
    public static final int IRON_COPPER_ALLOY = 4;
    public static final int STEEL_GOLD = 5;
    public static final int BRITTLE_WHITE_STEEL = 6;
    public static final int WHITE_STEEL = 7;

    public static void refresh(ItemStack stack) {
        Fluid fluid = BucketContents.fluid(stack);
        int id = fluid == null ? NONE : idOf(fluid);
        CompoundTag tag = stack.getOrCreateTag();
        if (id == NONE) {
            tag.remove("CustomModelData");
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        } else {
            tag.putInt("CustomModelData", id);
        }
    }

    private static int idOf(Fluid fluid) {
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return WATER;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return LAVA;
        }
        if (fluid == ForgeMod.MILK.get() || fluid == ForgeMod.FLOWING_MILK.get()) {
            return MILK;
        }
        ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid);
        if (key != null && key.getNamespace().equals(CopperIronAlloy.MODID) && key.getPath().startsWith("metal/")) {
            return switch (key.getPath().substring("metal/".length())) {
                case ModFluids.IRON_COPPER_ALLOY -> IRON_COPPER_ALLOY;
                case ModFluids.STEEL_GOLD -> STEEL_GOLD;
                case ModFluids.BRITTLE_WHITE_STEEL -> BRITTLE_WHITE_STEEL;
                default -> WHITE_STEEL;
            };
        }
        return NONE;
    }

    private BucketOverlay() {
    }
}
