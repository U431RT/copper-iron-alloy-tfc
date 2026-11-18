package com.copperironalloy;

/**
 * Design numbers. These are not config entries: changing them would retune the mod.
 * Tool tier stats stay on {@link ModTiers} because they are one table.
 */
public final class ModConstants {
    public static final int IRON_COPPER_BUCKET_CAPACITY = 2000;
    public static final int STEEL_GOLD_BUCKET_CAPACITY = 5000;
    public static final int KETTLE_CAPACITY = 2500;
    public static final int CUP_CAPACITY = 100;

    /** Non-bulk fluids move 1000 mB at a time. Shift pours the whole container. */
    public static final int SCOOP_STEP = 1000;
    public static final int KETTLE_DRINK_AMOUNT = 100;
    /** A 100 mB sip counts as this much thirst. */
    public static final int KETTLE_DRINK_STRENGTH = 125;
    public static final int CUP_DRINK_STRENGTH = 25;

    public static final int MELT_IRON_COPPER = 1100;
    public static final int MELT_STEEL_GOLD = 1450;
    public static final int MELT_BRITTLE_WHITE_STEEL = 1500;
    public static final int MELT_WHITE_STEEL = 1600;

    public static final int ANVIL_TIER_IRON_COPPER = 3;
    public static final int ANVIL_TIER_STEEL_GOLD = 5;

    public static final int FRIDGE_SLOTS_IRON_COPPER = 18;
    public static final int FRIDGE_SLOTS_STEEL_GOLD = 27;
    /** Same grid as white steel. The upgrade is the decay rate, not another row. */
    public static final int FRIDGE_SLOTS_BRITTLE = 36;
    public static final int FRIDGE_SLOTS_WHITE_STEEL = 36;

    /** Multiplies TFC food decay while the trait is on the item. 1 is normal speed. */
    public static final float FRIDGE_DECAY_IRON_COPPER = 0.75F;
    public static final float FRIDGE_DECAY_STEEL_GOLD = 0.5F;
    public static final float FRIDGE_DECAY_BRITTLE = 0.25F;
    /** Single white-steel freezer. The stacked vertical fridge does not use a trait. */
    public static final float FRIDGE_DECAY_WHITE_STEEL = 0.15F;

    private ModConstants() {
    }
}
