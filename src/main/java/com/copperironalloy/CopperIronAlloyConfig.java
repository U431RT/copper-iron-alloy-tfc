package com.copperironalloy;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CopperIronAlloyConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue MILKING_ENABLED;
    public static final ForgeConfigSpec.BooleanValue BYPASS_MILKING_RESTRICTION;
    public static final ForgeConfigSpec.BooleanValue ALLOW_MAID_MILKING;
    public static final ForgeConfigSpec.BooleanValue MAID_MILKING_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MAID_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> MAID_MODEL_WHITELIST;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("milking");
        MILKING_ENABLED = builder.comment("Allow the iron-copper bucket to milk animals.")
                .define("milking_enabled", true);
        BYPASS_MILKING_RESTRICTION = builder.comment(
                        "Bypass TFC's milking restrictions and always allow milking.")
                .define("bypass_milking_restriction", false);
        ALLOW_MAID_MILKING = builder.comment(
                        "Allow milking Touhou Little Maid maids (soft dependency, off = vanilla behaviour).")
                .define("allow_maid_milking", true);
        MAID_MILKING_COOLDOWN = builder.comment(
                        "Whether a milked maid goes on cooldown (a lactation period).")
                .define("maid_milking_cooldown", true);
        MAID_COOLDOWN_TICKS = builder.comment("Maid milking cooldown in ticks (24000 = one in-game day).")
                .defineInRange("maid_cooldown_ticks", 24000, 0, 1000000);
        MAID_MODEL_WHITELIST = builder.comment(
                        "Model ids that may be milked; empty = any maid model.",
                        "Sake fox model id goes here if you want to restrict it.")
                .defineListAllowEmpty(java.util.List.of("maid_model_whitelist"),
                        java.util.List::of, o -> o instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private CopperIronAlloyConfig() {
    }
}
