package com.copperironalloy.fridge;

import com.copperironalloy.CopperIronAlloy;
import com.copperironalloy.ModConstants;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.minecraft.resources.ResourceLocation;

public enum FridgeTier {
    IRON_COPPER("iron_copper_alloy_fridge", ModConstants.FRIDGE_SLOTS_IRON_COPPER, ModConstants.FRIDGE_DECAY_IRON_COPPER),
    STEEL_GOLD("steel_gold_fridge", ModConstants.FRIDGE_SLOTS_STEEL_GOLD, ModConstants.FRIDGE_DECAY_STEEL_GOLD),
    BRITTLE_WHITE_STEEL("brittle_white_steel_fridge", ModConstants.FRIDGE_SLOTS_BRITTLE, ModConstants.FRIDGE_DECAY_BRITTLE),
    WHITE_STEEL("white_steel_fridge", ModConstants.FRIDGE_SLOTS_WHITE_STEEL, ModConstants.FRIDGE_DECAY_WHITE_STEEL);

    private final String blockName;
    private final int slots;
    private final float decayModifier;
    private FoodTrait trait;
    /** Modifier 0. Never passed to {@code applyTrait}: that divides by the modifier. */
    private FoodTrait frozenTrait;

    FridgeTier(String blockName, int slots, float decayModifier) {
        this.blockName = blockName;
        this.slots = slots;
        this.decayModifier = decayModifier;
    }

    public String blockName() {
        return blockName;
    }

    public int slots() {
        return slots;
    }

    public int rows() {
        return slots / 9;
    }

    public float decayModifier() {
        return decayModifier;
    }

    public FoodTrait trait() {
        return trait;
    }

    /** Present only for white steel. Attached by hand so decay math treats the rate as zero. */
    public FoodTrait frozenTrait() {
        return frozenTrait;
    }

    public static void registerTraits() {
        for (FridgeTier tier : values()) {
            tier.trait = FoodTrait.register(
                    new ResourceLocation(CopperIronAlloy.MODID, tier.blockName),
                    new FoodTrait(tier.decayModifier, "tooltip.copper_iron_alloy.food_trait." + tier.blockName));
        }
        WHITE_STEEL.frozenTrait = FoodTrait.register(
                new ResourceLocation(CopperIronAlloy.MODID, "white_steel_fridge_frozen"),
                new FoodTrait(0.0F, "tooltip.copper_iron_alloy.food_trait.white_steel_fridge_frozen"));
    }
}
