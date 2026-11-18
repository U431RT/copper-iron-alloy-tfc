package com.copperironalloy.compat;

import com.copperironalloy.BucketContents;
import com.copperironalloy.CopperIronAlloy;
import com.copperironalloy.ModBlocks;
import com.copperironalloy.ModItems;
import com.copperironalloy.ModTools;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Optional. Datapack recipes already show up in TFC's JEI categories. */
@JeiPlugin
public class CiaJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(CopperIronAlloy.MODID, "jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.BUCKET.get(), CiaJeiPlugin::fluidKey);
        registration.registerSubtypeInterpreter(ModItems.STEEL_GOLD_BUCKET.get(), CiaJeiPlugin::fluidKey);
        registration.registerSubtypeInterpreter(ModTools.KETTLE.get(), CiaJeiPlugin::fluidKey);
        registration.registerSubtypeInterpreter(ModTools.CUP.get(), CiaJeiPlugin::fluidKey);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(new ItemStack(ModBlocks.IRON_COPPER_FRIDGE.get()),
                Component.translatable("jei.copper_iron_alloy.fridge.iron_copper"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.STEEL_GOLD_FRIDGE.get()),
                Component.translatable("jei.copper_iron_alloy.fridge.steel_gold"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.BRITTLE_WHITE_STEEL_FRIDGE.get()),
                Component.translatable("jei.copper_iron_alloy.fridge.brittle_white_steel"));
        registration.addItemStackInfo(new ItemStack(ModBlocks.WHITE_STEEL_FRIDGE.get()),
                Component.translatable("jei.copper_iron_alloy.fridge.white_steel"));
    }

    private static String fluidKey(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
        FluidStack fluid = BucketContents.read(stack);
        if (fluid.isEmpty()) {
            return "empty";
        }
        ResourceLocation id = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return (id == null ? "unknown" : id.toString()) + ":" + fluid.getAmount();
    }
}
