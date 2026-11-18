package com.copperironalloy;

import com.copperironalloy.fluid.AlloyFluidContainerItem;
import com.copperironalloy.fluid.CupBehavior;
import com.copperironalloy.fluid.KettleBehavior;
import net.dries007.tfc.common.items.TFCHoeItem;
import net.dries007.tfc.common.items.ToolItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tool heads (cast or forged, then assembled onto a wooden rod) and the finished tools. */
public final class ModTools {
    public static final String[] TOOL_TYPES = {"pickaxe", "axe", "shovel", "hoe", "sword",
            "knife", "hammer", "chisel", "mace", "propick", "scythe"};

    /** "metal/tool" -> head item */
    public static final Map<String, RegistryObject<Item>> HEADS = new LinkedHashMap<>();
    /** "metal/tool" -> finished tool */
    public static final Map<String, RegistryObject<Item>> TOOLS = new LinkedHashMap<>();

    private static final TagKey<Fluid> DRINKABLES =
            TagKey.create(Registries.FLUID, new ResourceLocation("tfc", "any_drinkables"));

    public static final RegistryObject<Item> KETTLE = ModItems.ITEMS.register("steel_gold_kettle",
            () -> new AlloyFluidContainerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), new KettleBehavior()));

    /** 小杯：装了东西就不能堆叠，所以统一 1 个一叠（空杯也一样，简单可靠）。 */
    /** 白钢链：脆蓝钢粉 / 脆红钢粉 / 预混合钢料 */
    public static final Map<String, RegistryObject<Item>> CHAIN = new LinkedHashMap<>();
    static {
        CHAIN.put("brittle_blue_steel_powder",
                ModItems.ITEMS.register("brittle_blue_steel_powder", () -> new Item(new Item.Properties())));
        CHAIN.put("brittle_red_steel_powder",
                ModItems.ITEMS.register("brittle_red_steel_powder", () -> new Item(new Item.Properties())));
        CHAIN.put("premixed_steel_charge",
                ModItems.ITEMS.register("premixed_steel_charge", () -> new Item(new Item.Properties())));
        CHAIN.put("diamond_catalyst",
                ModItems.ITEMS.register("diamond_catalyst", () -> new Item(new Item.Properties())));
    }

    public static final RegistryObject<Item> CUP = ModItems.ITEMS.register("steel_gold_cup",
            () -> new AlloyFluidContainerItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON), new CupBehavior()));

    static {
        for (String metal : ModItems.METALS) {
            Tier tier = tierOf(metal);
            for (String tool : TOOL_TYPES) {
                String key = metal + "/" + tool;
                HEADS.put(key, ModItems.ITEMS.register(metal + "_" + tool + "_head",
                        () -> new Item(new Item.Properties())));
                TOOLS.put(key, ModItems.ITEMS.register(metal + "_" + tool, () -> switch (tool) {
                    case "pickaxe" -> new ToolItem(tier, 1.0F, -2.8F, BlockTags.MINEABLE_WITH_PICKAXE, new Item.Properties());
                    case "axe" -> new ToolItem(tier, 6.0F, -3.0F, BlockTags.MINEABLE_WITH_AXE, new Item.Properties());
                    case "shovel" -> new ToolItem(tier, 1.5F, -3.0F, BlockTags.MINEABLE_WITH_SHOVEL, new Item.Properties());
                    case "sword" -> new ToolItem(tier, 3.0F, -2.4F, BlockTags.SWORD_EFFICIENT, new Item.Properties());
                    case "knife" -> new ToolItem(tier, 2.0F, -1.8F, BlockTags.SWORD_EFFICIENT, new Item.Properties());
                    case "hammer" -> new net.dries007.tfc.common.items.HammerItem(tier, 4.0F, -3.2F, new Item.Properties());
                    case "chisel" -> new net.dries007.tfc.common.items.ChiselItem(tier, 1.0F, -2.6F, new Item.Properties());
                    case "mace" -> new net.dries007.tfc.common.items.MaceItem(tier, 5, -3.0F, new Item.Properties());
                    case "propick" -> new net.dries007.tfc.common.items.PropickItem(tier, 1.0F, -2.8F, new Item.Properties());
                    case "scythe" -> new net.dries007.tfc.common.items.ScytheItem(tier, 4.0F, -3.0F, BlockTags.MINEABLE_WITH_HOE, new Item.Properties());
                    default -> new TFCHoeItem(tier, 1, -3.0F, new Item.Properties());
                }));
            }
        }
    }

    /** Forces the static initialiser to run during mod construction. */
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

    private ModTools() {
    }
}
