package com.copperironalloy;

import com.copperironalloy.fridge.FridgeBlockEntity;
import com.copperironalloy.lamp.MetalLampBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CopperIronAlloy.MODID);

    public static final RegistryObject<BlockEntityType<FridgeBlockEntity>> FRIDGE = BLOCK_ENTITIES.register("fridge",
            () -> BlockEntityType.Builder.of(FridgeBlockEntity::new,
                    ModBlocks.IRON_COPPER_FRIDGE.get(),
                    ModBlocks.STEEL_GOLD_FRIDGE.get(),
                    ModBlocks.BRITTLE_WHITE_STEEL_FRIDGE.get(),
                    ModBlocks.WHITE_STEEL_FRIDGE.get()).build(null));

    public static final RegistryObject<BlockEntityType<MetalLampBlockEntity>> LAMP = BLOCK_ENTITIES.register("lamp",
            () -> {
                Block[] blocks = ModBlocks.LAMPS.values().stream().map(RegistryObject::get).toArray(Block[]::new);
                return BlockEntityType.Builder.of(MetalLampBlockEntity::new, blocks).build(null);
            });

    private ModBlockEntities() {
    }
}
