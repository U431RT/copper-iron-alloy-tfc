package com.copperironalloy;

import com.copperironalloy.fridge.FridgeTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CopperIronAlloy.MODID)
public class CopperIronAlloy {
    public static final String MODID = "copper_iron_alloy";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.copper_iron_alloy"))
            .icon(() -> new ItemStack(ModItems.INGOTS.get(ModFluids.WHITE_STEEL).get()))
            .displayItems((params, output) -> {
                ModItems.INGOTS.values().forEach(i -> output.accept(i.get()));
                ModItems.DOUBLE_INGOTS.values().forEach(i -> output.accept(i.get()));
                ModItems.SHEETS.values().forEach(i -> output.accept(i.get()));
                ModItems.DOUBLE_SHEETS.values().forEach(i -> output.accept(i.get()));
                ModTools.HEADS.values().forEach(i -> output.accept(i.get()));
                ModTools.TOOLS.values().forEach(i -> output.accept(i.get()));
                ModGear.SHEARS.values().forEach(i -> output.accept(i.get()));
                ModGear.FISH_HOOKS.values().forEach(i -> output.accept(i.get()));
                ModGear.FISHING_RODS.values().forEach(i -> output.accept(i.get()));
                ModGear.UNFINISHED_LAMPS.values().forEach(i -> output.accept(i.get()));
                ModTools.CHAIN.values().forEach(i -> output.accept(i.get()));
                ModArmor.PIECES.values().forEach(i -> output.accept(i.get()));
                output.accept(ModItems.BUCKET.get());
                output.accept(ModItems.STEEL_GOLD_BUCKET.get());
                output.accept(ModTools.KETTLE.get());
                output.accept(ModTools.CUP.get());
                ModBlocks.BLOCK_ITEMS.getEntries().forEach(i -> output.accept(i.get()));
            })
            .build());

    /** Temporary smoke-test item so the first build proves the toolchain works. */
    public static final RegistryObject<Item> TEST_INGOT =
            ITEMS.register("test_ingot", () -> new Item(new Item.Properties()));

    public CopperIronAlloy() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(
                net.minecraftforge.fml.config.ModConfig.Type.COMMON, CopperIronAlloyConfig.SPEC);
        ITEMS.register(bus);
        ModFluids.FLUID_TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        ModFluids.FLUID_BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModTools.init();            // must run before the registry events fire
        ModGear.init();
        ModArmor.init();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.BLOCK_ITEMS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModMenus.MENUS.register(bus);
        FridgeTier.registerTraits();
        TABS.register(bus);
    }
}
