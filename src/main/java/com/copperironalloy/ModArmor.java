package com.copperironalloy;

import net.dries007.tfc.util.PhysicalDamageType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Armour made from our metals: sheets on the crafting table (no welding needed, no forging bonus). */
public final class ModArmor {
    public static final Map<String, ArmorMaterial> BY_METAL = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> PIECES = new LinkedHashMap<>();

    private static final String[] TYPES = {"helmet", "chestplate", "leggings", "boots"};

    static {
        for (String metal : ModItems.METALS) {
            ArmorStats stats = statsFor(metal);
            ArmorMaterial material = new OurArmorMaterial(
                    defenseFor(metal), stats.durability(), SoundEvents.ARMOR_EQUIP_IRON,
                    stats.toughness(), stats.knockback(),
                    stats.crushing(), stats.piercing(), stats.slashing(), metal);
            BY_METAL.put(metal, material);
            for (String type : TYPES) {
                ArmorItem.Type t = switch (type) {
                    case "helmet" -> ArmorItem.Type.HELMET;
                    case "chestplate" -> ArmorItem.Type.CHESTPLATE;
                    case "leggings" -> ArmorItem.Type.LEGGINGS;
                    default -> ArmorItem.Type.BOOTS;
                };
                PIECES.put(metal + "/" + type, ModItems.ITEMS.register(metal + "_" + type,
                        () -> new ArmorItem(material, t, new Item.Properties())));
            }
        }
    }

    /**
     * Resistances are the same raw numbers TFC passes into its armor materials
     * (they are scaled by 0.25 before the damage curve). White steel is above both
     * red steel and blue steel on every type. Brittle white steel sits between
     * black steel and the colored steels.
     */
    private record ArmorStats(int durability, float toughness, float knockback,
                              float crushing, float piercing, float slashing) {
    }

    private static ArmorStats statsFor(String metal) {
        return switch (metal) {
            case ModFluids.IRON_COPPER_ALLOY -> new ArmorStats(33, 0.0F, 0.0F, 20.0F, 20.0F, 13.2F);
            case ModFluids.STEEL_GOLD -> new ArmorStats(40, 1.0F, 0.0F, 25.0F, 30.0F, 16.5F);
            case ModFluids.BRITTLE_WHITE_STEEL -> new ArmorStats(58, 2.5F, 0.08F, 56.0F, 54.0F, 42.0F);
            default -> new ArmorStats(80, 4.0F, 0.15F, 75.0F, 75.0F, 65.0F);
        };
    }

    private static Map<ArmorItem.Type, Integer> defenseFor(String metal) {
        return switch (metal) {
            case ModFluids.IRON_COPPER_ALLOY -> Map.of(
                    ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 6,
                    ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 2);
            case ModFluids.STEEL_GOLD -> Map.of(
                    ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                    ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3);
            case ModFluids.BRITTLE_WHITE_STEEL -> Map.of(
                    ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 7,
                    ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 2);
            default -> Map.of(
                    ArmorItem.Type.HELMET, 4, ArmorItem.Type.CHESTPLATE, 9,
                    ArmorItem.Type.LEGGINGS, 7, ArmorItem.Type.BOOTS, 4);
        };
    }

    public record OurArmorMaterial(Map<ArmorItem.Type, Integer> defense, int durabilityMultiplier,
                                   net.minecraft.sounds.SoundEvent sound,
                                   float toughness, float knockback,
                                   float crushingStat, float piercingStat, float slashingStat,
                                   String name) implements ArmorMaterial, PhysicalDamageType.Multiplier {
        private static int baseDurability(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
                default -> 11;
            };
        }

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return baseDurability(type) * durabilityMultiplier;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return defense.getOrDefault(type, 0);
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }

        @Override
        public net.minecraft.sounds.SoundEvent getEquipSound() {
            return sound;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.INGOTS.get(name).get());
        }

        @Override
        public String getName() {
            return CopperIronAlloy.MODID + ":" + name;
        }

        @Override
        public float getToughness() {
            return toughness;
        }

        @Override
        public float getKnockbackResistance() {
            return knockback;
        }

        @Override
        public float crushing() {
            return crushingStat * 0.25F;
        }

        @Override
        public float piercing() {
            return piercingStat * 0.25F;
        }

        @Override
        public float slashing() {
            return slashingStat * 0.25F;
        }

    }

    /** 强制静态初始化：必须在注册事件之前跑。 */
    public static void init() {
    }

    private ModArmor() {
    }
}
