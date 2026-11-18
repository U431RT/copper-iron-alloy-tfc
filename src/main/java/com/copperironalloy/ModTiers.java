package com.copperironalloy;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * Tool tiers. Brittle white steel sits between TFC black steel
 * (level 5, 4200 uses, speed 11, damage 7, enchant 17) and red/blue steel
 * (level 6, 6500, 12, 9, 22). White steel stays above red/blue steel.
 */
public enum ModTiers implements Tier {
    IRON_COPPER_ALLOY(3, 900, 7.0F, 2.0F, 12),
    STEEL_GOLD(4, 1500, 8.0F, 2.5F, 16),
    BRITTLE_WHITE_STEEL(6, 5350, 11.5F, 8.0F, 19),
    WHITE_STEEL(6, 8000, 13.0F, 10.0F, 24);

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;

    ModTiers(int level, int uses, float speed, float damage, int enchantmentValue) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public TagKey<Block> getTag() {
        return switch (this) {
            case IRON_COPPER_ALLOY -> BlockTags.NEEDS_STONE_TOOL;
            case STEEL_GOLD -> BlockTags.NEEDS_IRON_TOOL;
            case BRITTLE_WHITE_STEEL, WHITE_STEEL -> BlockTags.NEEDS_DIAMOND_TOOL;
        };
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.INGOTS.get(name().toLowerCase()).get());
    }

    public String metalId() {
        return name().toLowerCase();
    }
}
