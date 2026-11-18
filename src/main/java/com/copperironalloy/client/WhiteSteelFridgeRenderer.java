package com.copperironalloy.client;

import com.copperironalloy.CopperIronAlloy;
import com.copperironalloy.fridge.FridgeBlock;
import com.copperironalloy.fridge.FridgeBlockEntity;
import com.copperironalloy.fridge.FridgeTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import com.mojang.math.Axis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.function.Consumer;

/**
 * Closed white steel is one full cube: an 11-pixel body and a 5-pixel lid.
 * The lid hinges on the top back edge, so it needs the block above.
 * Two stacked blocks draw one front door instead, hinged on the left.
 * Light is taken from the neighbouring blocks because a solid cube stores none.
 */
public final class WhiteSteelFridgeRenderer implements BlockEntityRenderer<FridgeBlockEntity> {
    public static final ResourceLocation TEXTURE =
            new ResourceLocation(CopperIronAlloy.MODID, "textures/entity/fridge/white_steel.png");

    private static final float PX = 1.0F / 16.0F;
    private static final float LID = 5.0F * PX;
    private static final float DOOR = 3.0F * PX;

    public WhiteSteelFridgeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FridgeBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (be.tier() != FridgeTier.WHITE_STEEL || be.getLevel() == null) {
            return;
        }
        BlockState state = be.getBlockState();
        FridgeBlock.Part part = FridgeBlock.part(be.getLevel(), be.getBlockPos(), state);
        float open = eased(be.getOpenNess(partialTick));
        FridgeBlockEntity other = be.partner();
        if (other != null) {
            open = Math.max(open, eased(other.getOpenNess(partialTick)));
        }
        int packed = lightAround(be, light);
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(FridgeBlock.FACING).toYRot()));
        pose.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        if (part == FridgeBlock.Part.SINGLE) {
            drawFreezer(pose, consumer, packed, overlay, open);
        } else {
            drawDoor(pose, consumer, packed, overlay, open, part == FridgeBlock.Part.UPPER);
        }
        pose.popPose();
    }

    private static float eased(float openness) {
        float closed = 1.0F - openness;
        return 1.0F - closed * closed * closed;
    }

    /** A full cube samples light 0. The brightest open side is what the shell should use. */
    private static int lightAround(FridgeBlockEntity be, int fallback) {
        Level level = be.getLevel();
        if (level == null) {
            return fallback;
        }
        BlockPos pos = be.getBlockPos();
        int sky = 0;
        int block = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            sky = Math.max(sky, level.getBrightness(LightLayer.SKY, neighbor));
            block = Math.max(block, level.getBrightness(LightLayer.BLOCK, neighbor));
        }
        if (sky == 0 && block == 0) {
            return fallback == 0 ? LevelRenderer.getLightColor(level, pos.above()) : fallback;
        }
        return LightTexture.pack(block, sky);
    }

    private static void drawFreezer(PoseStack pose, VertexConsumer vc, int light, int overlay, float open) {
        float lidBottom = 1.0F - LID;
        cuboid(pose, vc, light, overlay, 0.0F, 0.0F, 0.0F, 1.0F, lidBottom, 1.0F,
                uv(80, 0, 16, 16), uv(64, 0, 16, 16), uv(32, 5, 16, 11), uv(16, 5, 16, 11),
                uv(0, 5, 16, 11), uv(0, 5, 16, 11));
        pose.pushPose();
        pose.translate(0.0F, 1.0F, 0.0F);
        pose.mulPose(Axis.XP.rotation(-open * ((float) Math.PI / 2.0F)));
        pose.translate(0.0F, -1.0F, 0.0F);
        cuboid(pose, vc, light, overlay, 0.0F, lidBottom, 0.0F, 1.0F, 1.0F, 1.0F,
                uv(64, 0, 16, 16), uv(48, 0, 16, 16), uv(32, 0, 16, 5), uv(16, 0, 16, 5),
                uv(0, 0, 16, 5), uv(0, 0, 16, 5));
        pose.popPose();
    }

    private static void drawDoor(PoseStack pose, VertexConsumer vc, int light, int overlay, float open, boolean upper) {
        float[] side = upper ? uv(32, 32, 16, 16) : uv(48, 32, 16, 16);
        float[] door = upper ? uv(0, 32, 16, 16) : uv(16, 32, 16, 16);
        float[] cap = upper ? uv(64, 32, 16, 16) : uv(80, 0, 16, 16);
        cuboid(pose, vc, light, overlay, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F - DOOR,
                upper ? null : cap, upper ? cap : null, side, uv(64, 0, 16, 16), side, side);
        pose.pushPose();
        pose.translate(1.0F, 0.0F, 1.0F);
        pose.mulPose(Axis.YP.rotation(open * ((float) Math.PI / 2.0F)));
        pose.translate(-1.0F, 0.0F, -1.0F);
        cuboid(pose, vc, light, overlay, 0.0F, 0.0F, 1.0F - DOOR, 1.0F, 1.0F, 1.0F,
                upper ? null : cap, upper ? cap : null, side, door, side, side);
        pose.popPose();
    }

    /** uv is x, y, width, height on the 128 sheet. A null face is not drawn. */
    private static float[] uv(int x, int y, int w, int h) {
        return new float[]{x / 128.0F, y / 128.0F, (x + w) / 128.0F, (y + h) / 128.0F};
    }

    private static void cuboid(PoseStack pose, VertexConsumer vc, int light, int overlay,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               float[] down, float[] up, float[] north, float[] south, float[] west, float[] east) {
        if (down != null) {
            face(pose, vc, light, overlay, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, down, 0.0F, -1.0F, 0.0F);
        }
        if (up != null) {
            face(pose, vc, light, overlay, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, up, 0.0F, 1.0F, 0.0F);
        }
        if (north != null) {
            face(pose, vc, light, overlay, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, north, 0.0F, 0.0F, -1.0F);
        }
        if (south != null) {
            face(pose, vc, light, overlay, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, south, 0.0F, 0.0F, 1.0F);
        }
        if (west != null) {
            face(pose, vc, light, overlay, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, west, -1.0F, 0.0F, 0.0F);
        }
        if (east != null) {
            face(pose, vc, light, overlay, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, east, 1.0F, 0.0F, 0.0F);
        }
    }

    private static void face(PoseStack pose, VertexConsumer vc, int light, int overlay,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float[] uv, float nx, float ny, float nz) {
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        Matrix3f normal = last.normal();
        vertex(vc, matrix, normal, ax, ay, az, uv[0], uv[3], nx, ny, nz, light, overlay);
        vertex(vc, matrix, normal, bx, by, bz, uv[2], uv[3], nx, ny, nz, light, overlay);
        vertex(vc, matrix, normal, cx, cy, cz, uv[2], uv[1], nx, ny, nz, light, overlay);
        vertex(vc, matrix, normal, dx, dy, dz, uv[0], uv[1], nx, ny, nz, light, overlay);
    }

    private static void vertex(VertexConsumer vc, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light, int overlay) {
        vc.vertex(matrix, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(normal, nx, ny, nz).endVertex();
    }

    public static final class ItemClient implements IClientItemExtensions {
        private BlockEntityWithoutLevelRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) {
                renderer = new ItemRendererHook();
            }
            return renderer;
        }

        public static void register(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new ItemClient());
        }
    }

    public static final class ItemRendererHook extends BlockEntityWithoutLevelRenderer {
        public ItemRendererHook() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                 MultiBufferSource buffer, int light, int overlay) {
            pose.pushPose();
            pose.translate(0.5F, 0.5F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.translate(-0.5F, -0.5F, -0.5F);
            VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            drawFreezer(pose, consumer, light, overlay, 0.0F);
            pose.popPose();
        }
    }
}
