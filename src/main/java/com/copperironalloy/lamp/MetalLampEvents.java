package com.copperironalloy.lamp;

import com.copperironalloy.CopperIronAlloy;
import net.dries007.tfc.util.events.DouseFireEvent;
import net.dries007.tfc.util.events.StartFireEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Firestarter and extinguishing go through TFC's events, which only know TFC lamps. */
@Mod.EventBusSubscriber(modid = CopperIronAlloy.MODID)
public final class MetalLampEvents {
    @SubscribeEvent
    public static void startFire(StartFireEvent event) {
        if (!(event.getState().getBlock() instanceof MetalLampBlock)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof MetalLampBlockEntity lamp && lamp.light()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void douse(DouseFireEvent event) {
        if (event.getState() == null || !(event.getState().getBlock() instanceof MetalLampBlock)) {
            return;
        }
        if (!event.getState().getValue(MetalLampBlock.LIT)) {
            return;
        }
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof MetalLampBlockEntity lamp) {
            lamp.extinguish();
            event.setCanceled(true);
        }
    }

    private MetalLampEvents() {
    }
}
