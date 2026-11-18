package com.copperironalloy;

import net.minecraft.world.entity.LivingEntity;

/** Milking rules: vanilla-style by default, TFC-friendly hook kept in one place. */
public final class ModMilk {
    public static boolean isMilkable(LivingEntity entity) {
        // TFC 3.2.12 has no dedicated milking mixin; animals are milkable unless a TFC extension says otherwise.
        return true;
    }

    private ModMilk() {
    }
}
