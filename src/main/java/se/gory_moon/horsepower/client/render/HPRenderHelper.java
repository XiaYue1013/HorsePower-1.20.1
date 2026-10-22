package se.gory_moon.horsepower.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.blockentity.HPBaseBlockEntity;
import se.gory_moon.horsepower.blocks.FillerBlock;
import se.gory_moon.horsepower.util.Localization;

/**
 * Port of the drawing helpers of the 1.12.2 {@code TileEntityHPBaseRenderer} and {@code RenderUtils}
 * to PoseStack / MultiBufferSource rendering.
 */
public final class HPRenderHelper {

    private HPRenderHelper() {}

    public static float facingAngle(Direction forward) {
        // FacingToRotation from 1.12.2: north 0, south 180, west 90, east -90 (degrees around Y)
        return switch (forward) {
            case SOUTH -> 180F;
            case WEST -> 90F;
            case EAST -> -90F;
            default -> 0F;
        };
    }

    public static void rotateFacing(PoseStack pose, Direction forward) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facingAngle(forward)));
        pose.translate(-0.5, -0.5, -0.5);
    }

    public static void renderModel(PoseStack pose, MultiBufferSource buffers, BlockState state, ResourceLocation model, int light, int overlay) {
        renderModel(pose, buffers, state, model, 1F, 1F, 1F, light, overlay);
    }

    public static void renderModel(PoseStack pose, MultiBufferSource buffers, BlockState state, ResourceLocation model, float r, float g, float b, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel baked = mc.getModelManager().getModel(model);
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()), state, baked, r, g, b, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /**
     * Renders an item like an item frame would, optionally spinning (1.12.2 renderItem / renderStillItem).
     */
    public static void renderItem(Level level, ItemStack stack, PoseStack pose, MultiBufferSource buffers, float x, float y, float z, float scale, boolean rotate, int light) {
        if (stack.isEmpty())
            return;
        pose.pushPose();
        pose.translate(x, y, z);
        if (rotate) {
            float rotation = (float) (720.0 * (System.currentTimeMillis() & 0x3FFFL) / 0x3FFFL);
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
        }
        pose.scale(0.5F * scale, 0.5F * scale, 0.5F * scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
        pose.popPose();
    }

    public static boolean isLookingAt(HPBaseBlockEntity te) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK)
            return false;
        BlockPos pos = te.getBlockPos();
        BlockPos hitPos = blockHit.getBlockPos();
        if (hitPos.equals(pos))
            return true;
        Level level = te.getLevel();
        return level != null && hitPos.equals(pos.above()) && level.getBlockState(pos.above()).getBlock() instanceof FillerBlock;
    }

    public static boolean canShowAmount(HPBaseBlockEntity te) {
        return Configs.client.renderItemAmount.get() && (!Configs.client.mustLookAtBlock.get() || isLookingAt(te));
    }

    /**
     * Draws a camera facing text at the given position (relative to the block), like 1.12.2 drawString.
     */
    public static void drawString(HPBaseBlockEntity te, PoseStack pose, MultiBufferSource buffers, String str, double x, double y, double z, int light) {
        if (!canShowAmount(te))
            return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (Vec3.atCenterOf(te.getBlockPos()).distanceToSqr(camera) > 14 * 14)
            return;
        Font font = mc.font;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.015F, -0.015F, 0.015F);
        font.drawInBatch(str, -font.width(str) / 2F, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /**
     * Draws the item count of a stack rendered at (x, y, z) in the machine's facing frame.
     */
    public static void drawStringWithFacing(HPBaseBlockEntity te, PoseStack pose, MultiBufferSource buffers, String str, float x, float y, float z, int light) {
        Vec3 local = new Vec3(x - 0.5, y, z - 0.5).yRot((float) Math.toRadians(facingAngle(te.getForward())));
        drawString(te, pose, buffers, str, local.x + 0.5, local.y, local.z + 0.5, light);
    }

    /**
     * The "invalid area" name plate and the "hold a lead" hint (1.12.2 drawDisplayText).
     */
    public static void drawInvalidText(HPBaseBlockEntity te, PoseStack pose, MultiBufferSource buffers, double y, String invalidKey) {
        if (!isLookingAt(te))
            return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        if (Vec3.atCenterOf(te.getBlockPos()).distanceToSqr(camera) > 12 * 12)
            return;
        drawNameplate(pose, buffers, Component.literal(Localization.translate(invalidKey)).withStyle(net.minecraft.ChatFormatting.RED), y + 1.5);
        drawNameplate(pose, buffers, Component.literal(Localization.translate(Localization.ITEM_REVEAL)).withStyle(net.minecraft.ChatFormatting.RED), y + 1.25);
    }

    private static void drawNameplate(PoseStack pose, MultiBufferSource buffers, Component text, double y) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        pose.pushPose();
        pose.translate(0.5, y, 0.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        float x = -font.width(text) / 2F;
        int background = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, 0, 0x20FFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT);
        font.drawInBatch(text, x, 0, -1, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /**
     * Renders a leash from the worker to the machine. Ported from the vanilla mob leash renderer, the machine at
     * (0.5, attachY, 0.5) of the block takes the place of the leash holder.
     */
    public static void renderLeash(Mob entity, BlockPos machinePos, double attachY, float partialTicks, PoseStack pose, MultiBufferSource buffers) {
        Vec3 holder = new Vec3(machinePos.getX() + 0.5, machinePos.getY() + attachY, machinePos.getZ() + 0.5);
        double bodyAngle = (double) (Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot) * ((float) Math.PI / 180F)) + (Math.PI / 2D);
        Vec3 leashOffset = entity.getLeashOffset(partialTicks);
        double dx = Math.cos(bodyAngle) * leashOffset.z + Math.sin(bodyAngle) * leashOffset.x;
        double dz = Math.sin(bodyAngle) * leashOffset.z - Math.cos(bodyAngle) * leashOffset.x;
        double ex = Mth.lerp(partialTicks, entity.xo, entity.getX()) + dx;
        double ey = Mth.lerp(partialTicks, entity.yo, entity.getY()) + leashOffset.y;
        double ez = Mth.lerp(partialTicks, entity.zo, entity.getZ()) + dz;

        pose.pushPose();
        pose.translate(ex - machinePos.getX(), ey - machinePos.getY(), ez - machinePos.getZ());
        float fx = (float) (holder.x - ex);
        float fy = (float) (holder.y - ey);
        float fz = (float) (holder.z - ez);
        VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
        Matrix4f matrix = pose.last().pose();
        float scale = Mth.invSqrt(fx * fx + fz * fz) * 0.025F / 2.0F;
        float sz = fz * scale;
        float sx = fx * scale;
        Level level = entity.level();
        BlockPos entityEye = BlockPos.containing(entity.getEyePosition(partialTicks));
        BlockPos holderPos = BlockPos.containing(holder);
        int blockLight0 = entity.isOnFire() ? 15 : level.getBrightness(LightLayer.BLOCK, entityEye);
        int blockLight1 = level.getBrightness(LightLayer.BLOCK, holderPos);
        int skyLight0 = level.getBrightness(LightLayer.SKY, entityEye);
        int skyLight1 = level.getBrightness(LightLayer.SKY, holderPos);

        for (int i = 0; i <= 24; ++i)
            addLeashVertexPair(consumer, matrix, fx, fy, fz, blockLight0, blockLight1, skyLight0, skyLight1, 0.025F, 0.025F, sz, sx, i, false);
        for (int i = 24; i >= 0; --i)
            addLeashVertexPair(consumer, matrix, fx, fy, fz, blockLight0, blockLight1, skyLight0, skyLight1, 0.025F, 0.0F, sz, sx, i, true);
        pose.popPose();
        endStrip(buffers);
    }

    private static void addLeashVertexPair(VertexConsumer consumer, Matrix4f matrix, float dx, float dy, float dz, int blockLight0, int blockLight1, int skyLight0, int skyLight1,
                                           float y0, float y1, float sz, float sx, int index, boolean dark) {
        float progress = (float) index / 24.0F;
        int block = (int) Mth.lerp(progress, (float) blockLight0, (float) blockLight1);
        int sky = (int) Mth.lerp(progress, (float) skyLight0, (float) skyLight1);
        int light = LightTexture.pack(block, sky);
        float shade = index % 2 == (dark ? 1 : 0) ? 0.7F : 1.0F;
        float r = 0.5F * shade;
        float g = 0.4F * shade;
        float b = 0.3F * shade;
        float x = dx * progress;
        float y = dy > 0.0F ? dy * progress * progress : dy - dy * (1.0F - progress) * (1.0F - progress);
        float z = dz * progress;
        consumer.vertex(matrix, x - sz, y + y1, z + sx).color(r, g, b, 1.0F).uv2(light).endVertex();
        consumer.vertex(matrix, x + sz, y + y0 - y1, z - sx).color(r, g, b, 1.0F).uv2(light).endVertex();
    }

    /**
     * The decorative rope of the chopper (1.12.2 renderLeach), between two points relative to the block.
     */
    public static void renderRope(PoseStack pose, MultiBufferSource buffers, double x1, double y1, double z1, double ox, double oy, double oz, double x2, double y2, double z2, int light) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
        Matrix4f matrix = pose.last().pose();
        float dx = (float) (x1 - x2);
        float dy = (float) (y1 - y2);
        float dz = (float) (z1 - z2);
        for (int j = 0; j <= 24; ++j)
            addRopeVertexPair(consumer, matrix, ox, oy, oz, dx, dy, dz, j, light, false);
        for (int j = 24; j >= 0; --j)
            addRopeVertexPair(consumer, matrix, ox, oy, oz, dx, dy, dz, j, light, true);
        endStrip(buffers);
    }

    private static void addRopeVertexPair(VertexConsumer consumer, Matrix4f matrix, double ox, double oy, double oz, float dx, float dy, float dz, int j, int light, boolean second) {
        float shade = j % 2 == 0 ? 0.7F : 1F;
        float r = 0.5F * shade, g = 0.4F * shade, b = 0.3F * shade;
        float f = (float) j / 24.0F;
        float px = (float) (ox + dx * f);
        float py = (float) (oy + dy * (f * f + f) * 0.5D + ((24.0F - (float) j) / 18.0F + 0.125F));
        float pz = (float) (oz + dz * f);
        if (!second) {
            consumer.vertex(matrix, px, py, pz).color(r, g, b, 1F).uv2(light).endVertex();
            consumer.vertex(matrix, px + 0.025F, py + 0.025F, pz).color(r, g, b, 1F).uv2(light).endVertex();
        } else {
            consumer.vertex(matrix, px, py + 0.025F, pz).color(r, g, b, 1F).uv2(light).endVertex();
            consumer.vertex(matrix, px + 0.025F, py, pz + 0.025F).color(r, g, b, 1F).uv2(light).endVertex();
        }
    }

    /**
     * Leashes are triangle strips: flush them so two ropes are never joined into one strip.
     */
    private static void endStrip(MultiBufferSource buffers) {
        if (buffers instanceof MultiBufferSource.BufferSource source)
            source.endBatch(RenderType.leash());
    }

    public static boolean isHoldingLead() {
        var player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().is(Items.LEAD) || player.getOffhandItem().is(Items.LEAD));
    }

    /**
     * Port of renderInvalidArea: shows the 7x7x2 area when the player holds a lead.
     */
    public static void renderInvalidArea(Level level, BlockPos blockPos, int yOffset, PoseStack pose, MultiBufferSource buffers) {
        if (isHoldingLead())
            renderUsedArea(level, blockPos, yOffset, 0.55F, 0.15F, pose, buffers);
    }

    /**
     * Port of renderUsedArea. The pose's origin must be at {@code blockPos}.
     * Blocking blocks get a red outline plus a red filled shape, free spots a green outline.
     */
    public static void renderUsedArea(Level level, BlockPos blockPos, int yOffset, float invalidAlpha, float validAlpha, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (int xo = -3; xo <= 3; xo++) {
            for (int yo = yOffset; yo <= 1 + yOffset; yo++) {
                for (int zo = -3; zo <= 3; zo++) {
                    if ((xo <= 1 && xo >= -1) && (zo <= 1 && zo >= -1))
                        continue;
                    BlockPos pos = blockPos.offset(xo, yo, zo);
                    if (level.isOutsideBuildHeight(pos))
                        continue;
                    BlockState state = level.getBlockState(pos);
                    AABB box = new AABB(xo, yo, zo, xo + 1, yo + 1, zo + 1);
                    if (!state.canBeReplaced()) {
                        LevelRenderer.renderLineBox(pose, lines, box, 1F, 0F, 0F, invalidAlpha);
                        VoxelShape shape = state.getShape(level, pos);
                        if (!shape.isEmpty()) {
                            AABB bounds = shape.bounds().move(xo, yo, zo);
                            VertexConsumer filled = buffers.getBuffer(RenderType.debugFilledBox());
                            LevelRenderer.addChainedFilledBoxVertices(pose, filled, bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ, 1F, 0F, 0F, invalidAlpha * 0.5F);
                            lines = buffers.getBuffer(RenderType.lines());
                        }
                    } else {
                        LevelRenderer.renderLineBox(pose, lines, box, 0F, 1F, 0F, Math.max(validAlpha, 0.15F));
                    }
                }
            }
        }
    }
}
