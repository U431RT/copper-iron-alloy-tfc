package com.copperironalloy;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Fluid type that renders with a per-metal tint (client extension loaded lazily). */
public class TintedFluidType extends FluidType {
    private final int tint;
    private final ResourceLocation still;
    private final ResourceLocation flowing;

    public TintedFluidType(Properties properties, int tint, String name) {
        super(properties);
        this.tint = tint;
        this.still = new ResourceLocation(CopperIronAlloy.MODID, "block/fluid/" + name);
        this.flowing = new ResourceLocation(CopperIronAlloy.MODID, "block/fluid/" + name + "_flow");
    }

    @Override
    public void initializeClient(java.util.function.Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public int getTintColor() {
                return 0xFF000000 | tint;
            }

            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }
        });
    }
}
