package com.copperironalloy.client;

import com.copperironalloy.CopperIronAlloy;
import com.copperironalloy.ModBlockEntities;
import com.copperironalloy.ModGear;
import com.copperironalloy.ModMenus;
import net.dries007.tfc.common.items.TFCFishingRodItem;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CopperIronAlloy.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void client(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.FRIDGE.get(), FridgeScreen::new);
            ResourceLocation cast = new ResourceLocation("tfc", "cast");
            ModGear.FISHING_RODS.values().forEach(rod -> ItemProperties.register(rod.get(), cast,
                    (stack, level, entity, seed) -> entity instanceof Player player
                            && TFCFishingRodItem.isThisTheHeldRod(player, stack) ? 1.0F : 0.0F));
        });
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FRIDGE.get(), WhiteSteelFridgeRenderer::new);
    }

    private ClientEvents() {
    }
}
