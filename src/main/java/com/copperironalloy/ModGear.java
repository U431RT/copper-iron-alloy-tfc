package com.copperironalloy;

import net.dries007.tfc.common.items.TFCFishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.Tier;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Plated-block companions: shears, fish hooks, fishing rods, and unfinished lamps. */
public final class ModGear {
    public static final Map<String, RegistryObject<Item>> SHEARS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> FISH_HOOKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> FISHING_RODS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> UNFINISHED_LAMPS = new LinkedHashMap<>();

    static {
        for (String metal : ModItems.METALS) {
            Tier tier = tierOf(metal);
            SHEARS.put(metal, ModItems.ITEMS.register(metal + "_shears",
                    () -> new ShearsItem(new Item.Properties().durability(tier.getUses()))));
            FISH_HOOKS.put(metal, ModItems.ITEMS.register(metal + "_fish_hook",
                    () -> new Item(new Item.Properties())));
            FISHING_RODS.put(metal, ModItems.ITEMS.register(metal + "_fishing_rod",
                    () -> new TFCFishingRodItem(new Item.Properties().durability(tier.getUses()), tier)));
            UNFINISHED_LAMPS.put(metal, ModItems.ITEMS.register(metal + "_unfinished_lamp",
                    () -> new Item(new Item.Properties())));
        }
    }

    public static void init() {
    }

    private static Tier tierOf(String metal) {
        return switch (metal) {
            case ModFluids.IRON_COPPER_ALLOY -> ModTiers.IRON_COPPER_ALLOY;
            case ModFluids.STEEL_GOLD -> ModTiers.STEEL_GOLD;
            case ModFluids.BRITTLE_WHITE_STEEL -> ModTiers.BRITTLE_WHITE_STEEL;
            default -> ModTiers.WHITE_STEEL;
        };
    }

    private ModGear() {
    }
}
