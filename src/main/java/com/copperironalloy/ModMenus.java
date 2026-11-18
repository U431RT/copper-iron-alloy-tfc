package com.copperironalloy;

import com.copperironalloy.fridge.FridgeMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CopperIronAlloy.MODID);

    public static final RegistryObject<MenuType<FridgeMenu>> FRIDGE =
            MENUS.register("fridge", () -> IForgeMenuType.create(FridgeMenu::fromNetwork));

    private ModMenus() {
    }
}
