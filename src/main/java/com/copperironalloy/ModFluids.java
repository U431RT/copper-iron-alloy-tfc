package com.copperironalloy;

import net.dries007.tfc.common.fluids.MoltenFluid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Molten metal fluids, registered TFC-style (metal/&lt;name&gt; + metal/flowing_&lt;name&gt;). */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, CopperIronAlloy.MODID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, CopperIronAlloy.MODID);
    public static final DeferredRegister<Block> FLUID_BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CopperIronAlloy.MODID);

    public static final String IRON_COPPER_ALLOY = "iron_copper_alloy";
    public static final String STEEL_GOLD = "steel_gold";
    public static final String BRITTLE_WHITE_STEEL = "brittle_white_steel";
    public static final String WHITE_STEEL = "white_steel";

    private static final Map<String, Integer> TINTS = Map.of(
            IRON_COPPER_ALLOY, 0xBA7C3E,
            STEEL_GOLD, 0xA3B43F,
            BRITTLE_WHITE_STEEL, 0xD8E2EA,
            WHITE_STEEL, 0xEFF5FA);

    /** name -> registered set (stable, no map lookups from inside suppliers) */
    public static final Map<String, Set> SETS = new LinkedHashMap<>();

    static {
        molten(IRON_COPPER_ALLOY, ModConstants.MELT_IRON_COPPER);
        molten(STEEL_GOLD, ModConstants.MELT_STEEL_GOLD);
        molten(BRITTLE_WHITE_STEEL, ModConstants.MELT_BRITTLE_WHITE_STEEL);
        molten(WHITE_STEEL, ModConstants.MELT_WHITE_STEEL);
    }

    private static void molten(String name, int temperature) {
        Set set = new Set();
        set.name = name;
        int tint = TINTS.getOrDefault(name, 0xFFFFFF);
        set.type = FLUID_TYPES.register(name, () -> new TintedFluidType(FluidType.Properties.create()
                .density(2000).viscosity(3000).temperature(temperature).lightLevel(8)
                .canConvertToSource(false).supportsBoating(false), tint, name));
        set.still = FLUIDS.register("metal/" + name,
                () -> new MoltenSource(set.properties()));
        set.flowing = FLUIDS.register("metal/flowing_" + name,
                () -> new MoltenFlowing(set.properties()));
        set.block = FLUID_BLOCKS.register("fluid/metal/" + name,
                () -> new LiquidBlock(() -> (FlowingFluid) set.still.get(),
                        BlockBehaviour.Properties.of().noCollission().strength(100.0F)
                                .noLootTable().liquid().replaceable()));
        SETS.put(name, set);
    }

    public static final class Set {
        private String name;
        private RegistryObject<FluidType> type;
        private RegistryObject<net.minecraft.world.level.material.Fluid> still;
        private RegistryObject<net.minecraft.world.level.material.Fluid> flowing;
        private RegistryObject<Block> block;

        public RegistryObject<net.minecraft.world.level.material.Fluid> still() {
            return still;
        }

        public RegistryObject<net.minecraft.world.level.material.Fluid> flowing() {
            return flowing;
        }

        ForgeFlowingFluid.Properties properties() {
            return new ForgeFlowingFluid.Properties(type,
                    () -> (FlowingFluid) still.get(),
                    () -> (FlowingFluid) flowing.get())
                    .block(() -> (LiquidBlock) block.get());
        }

        public ResourceLocation id() {
            return new ResourceLocation(CopperIronAlloy.MODID, "metal/" + name);
        }
    }

    public static class MoltenSource extends MoltenFluid {
        public MoltenSource(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }
    }

    public static class MoltenFlowing extends MoltenFluid {
        public MoltenFlowing(Properties properties) {
            super(properties);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }
    }

    private ModFluids() {
    }
}
