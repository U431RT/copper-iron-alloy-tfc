package com.copperironalloy;

import com.copperironalloy.fluid.AlloyFluidContainerItem;
import com.copperironalloy.fluid.BucketBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Metal forms (TFC metal accounting: ingot 100, double ingot 200, sheet 200, double sheet 400)
 * plus the fluid containers.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CopperIronAlloy.MODID);

    public static final String[] METALS = {
            ModFluids.IRON_COPPER_ALLOY, ModFluids.STEEL_GOLD,
            ModFluids.BRITTLE_WHITE_STEEL, ModFluids.WHITE_STEEL
    };

    public static final java.util.Map<String, RegistryObject<Item>> INGOTS = new java.util.HashMap<>();
    public static final java.util.Map<String, RegistryObject<Item>> DOUBLE_INGOTS = new java.util.HashMap<>();
    public static final java.util.Map<String, RegistryObject<Item>> SHEETS = new java.util.HashMap<>();
    public static final java.util.Map<String, RegistryObject<Item>> DOUBLE_SHEETS = new java.util.HashMap<>();

    private static final TagKey<Fluid> STEEL_GOLD_BUCKET_CONTENTS =
            TagKey.create(Registries.FLUID, new ResourceLocation(CopperIronAlloy.MODID, "steel_gold_bucket_contents"));

    public static final TagKey<Fluid> DRINKABLES =
            TagKey.create(Registries.FLUID, new ResourceLocation("tfc", "any_drinkables"));

    public static final TagKey<Fluid> BUCKET_CONTENTS =
            TagKey.create(Registries.FLUID, new ResourceLocation(CopperIronAlloy.MODID, "bucket_contents"));

    /** 铁铜合金桶：2000 mB，倒出来不形成水源（和 TFC 木桶一样会流干）。 */
    public static final RegistryObject<Item> BUCKET = ITEMS.register("iron_copper_alloy_bucket",
            () -> new AlloyFluidContainerItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON),
                    new BucketBehavior(ModConstants.IRON_COPPER_BUCKET_CAPACITY)));

    /** 钢金桶：5000 mB，唯一能装熔融金属（以及岩浆）的容器。 */
    public static final RegistryObject<Item> STEEL_GOLD_BUCKET = ITEMS.register("steel_gold_bucket",
            () -> new AlloyFluidContainerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON),
                    new BucketBehavior(ModConstants.STEEL_GOLD_BUCKET_CAPACITY)));


    static {
        for (String metal : METALS) {
            Rarity rarity = metal.equals(ModFluids.WHITE_STEEL) || metal.equals(ModFluids.BRITTLE_WHITE_STEEL)
                    ? Rarity.UNCOMMON : Rarity.COMMON;
            INGOTS.put(metal, ITEMS.register(metal + "_ingot", () -> new Item(new Item.Properties().rarity(rarity))));
            DOUBLE_INGOTS.put(metal, ITEMS.register(metal + "_double_ingot", () -> new Item(new Item.Properties().rarity(rarity))));
            SHEETS.put(metal, ITEMS.register(metal + "_sheet", () -> new Item(new Item.Properties().rarity(rarity))));
            DOUBLE_SHEETS.put(metal, ITEMS.register(metal + "_double_sheet", () -> new Item(new Item.Properties().rarity(rarity))));
        }
    }

    private ModItems() {
    }
}
