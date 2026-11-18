package com.copperironalloy;

import com.copperironalloy.fridge.FridgeBlock;
import com.copperironalloy.fridge.FridgeBlockItem;
import com.copperironalloy.fridge.FridgeTier;
import com.copperironalloy.lamp.MetalLampBlock;
import com.copperironalloy.lamp.MetalLampBlockItem;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.devices.AnvilBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** TFC anvils: iron-copper = tier 3 (wrought iron), steel-gold = tier 5 (black steel). */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CopperIronAlloy.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CopperIronAlloy.MODID);

    public static final RegistryObject<Block> IRON_COPPER_ANVIL = anvil("iron_copper_alloy_anvil", ModConstants.ANVIL_TIER_IRON_COPPER);
    public static final RegistryObject<Block> STEEL_GOLD_ANVIL = anvil("steel_gold_anvil", ModConstants.ANVIL_TIER_STEEL_GOLD);

    public static final RegistryObject<Block> IRON_COPPER_FRIDGE = fridge(FridgeTier.IRON_COPPER, SoundType.WOOD);
    public static final RegistryObject<Block> STEEL_GOLD_FRIDGE = fridge(FridgeTier.STEEL_GOLD, SoundType.METAL);
    public static final RegistryObject<Block> BRITTLE_WHITE_STEEL_FRIDGE = fridge(FridgeTier.BRITTLE_WHITE_STEEL, SoundType.METAL);
    public static final RegistryObject<Block> WHITE_STEEL_FRIDGE = fridge(FridgeTier.WHITE_STEEL, SoundType.METAL);

    /** Sheet-and-plank blocks, the TFC "plated block" form. */
    public static final Map<String, RegistryObject<Block>> PLATED = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Block>> LAMPS = new LinkedHashMap<>();

    static {
        for (String metal : ModItems.METALS) {
            PLATED.put(metal, plated(metal));
            LAMPS.put(metal, lamp(metal));
        }
    }

    private static RegistryObject<Block> fridge(FridgeTier tier, SoundType sound) {
        var properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.5F, 6.0F)
                .sound(sound)
                .requiresCorrectToolForDrops();
        RegistryObject<Block> block = BLOCKS.register(tier.blockName(), () -> new FridgeBlock(properties, tier));
        BLOCK_ITEMS.register(tier.blockName(), () -> tier == FridgeTier.WHITE_STEEL
                ? new FridgeBlockItem(block.get(), new Item.Properties())
                : new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> plated(String metal) {
        String name = metal + "_block";
        RegistryObject<Block> block = BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()));
        BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> lamp(String metal) {
        String name = metal + "_lamp";
        RegistryObject<Block> block = BLOCKS.register(name, () -> new MetalLampBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F, 6.0F)
                .sound(SoundType.LANTERN)
                .noOcclusion()
                .lightLevel(state -> state.getValue(MetalLampBlock.LIT) ? 15 : 0)
                .requiresCorrectToolForDrops()));
        BLOCK_ITEMS.register(name, () -> new MetalLampBlockItem(block.get(), new Item.Properties().stacksTo(1)));
        return block;
    }

    private static RegistryObject<Block> anvil(String name, int tier) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new AnvilBlock(
                ExtendedProperties.of(BlockBehaviour.Properties.of()
                                .mapColor(MapColor.METAL)
                                .strength(5.0F, 1200.0F)
                                .sound(SoundType.METAL)
                                .requiresCorrectToolForDrops())
                        .blockEntity(() -> TFCBlockEntities.ANVIL.get()),
                tier));
        BLOCK_ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private ModBlocks() {
    }
}
