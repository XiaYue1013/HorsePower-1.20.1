package se.gory_moon.horsepower.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.blockentity.ChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.GrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.HPBaseBlockEntity;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.ManualChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.PressBlockEntity;
import se.gory_moon.horsepower.blocks.ChopperBlock;
import se.gory_moon.horsepower.blocks.GrindstoneBlock;
import se.gory_moon.horsepower.blocks.HandGrindstoneBlock;
import se.gory_moon.horsepower.blocks.PressBlock;
import se.gory_moon.horsepower.util.Localization;

/**
 * Ports of the 1.12.2 TESRs. The moving parts (grindstone crank, chopper blade, press plate, ground layer) are
 * separate block models rendered here; the static base is a normal block model.
 */
public final class MachineRenderers {

    public static final ResourceLocation HAND_GRINDSTONE_CENTER = new ResourceLocation(HorsePower.MODID, "block/hand_grindstone_center");
    public static final ResourceLocation GRINDSTONE_FILLED = new ResourceLocation(HorsePower.MODID, "block/grindstone_filled");
    public static final ResourceLocation CHOPPER_BLADE = new ResourceLocation(HorsePower.MODID, "block/chopper_blade");
    public static final ResourceLocation PRESS_TOP = new ResourceLocation(HorsePower.MODID, "block/press_top");

    private MachineRenderers() {}

    private static void renderItemWithFacing(HPBaseBlockEntity te, ItemStack stack, PoseStack pose, MultiBufferSource buffers, float x, float y, float z, float scale, int light) {
        if (stack.isEmpty() || te.getLevel() == null)
            return;
        pose.pushPose();
        HPRenderHelper.rotateFacing(pose, te.getForward());
        HPRenderHelper.renderItem(te.getLevel(), stack, pose, buffers, x, y, z, scale, true, light);
        pose.popPose();
        HPRenderHelper.drawStringWithFacing(te, pose, buffers, String.valueOf(stack.getCount()), x, y + 0.3F, z, light);
    }

    public static class HandGrindstone implements BlockEntityRenderer<HandGrindstoneBlockEntity> {
        public HandGrindstone(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(HandGrindstoneBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            BlockState state = te.getBlockState();
            if (!(state.getBlock() instanceof HandGrindstoneBlock))
                return;

            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(HPRenderHelper.facingAngle(te.getForward())));
            pose.mulPose(Axis.YP.rotationDegrees(te.getVisibleRotation(partialTicks)));
            pose.translate(-0.5, -0.5, -0.5);
            HPRenderHelper.renderModel(pose, buffers, state, HAND_GRINDSTONE_CENTER, light, overlay);
            pose.popPose();

            renderItemWithFacing(te, te.getStackInSlot(0), pose, buffers, 0.8F, 0.7F, 0.5F, 0.7F, light);
            renderItemWithFacing(te, te.getStackInSlot(1), pose, buffers, 0.2F, 0.7F, 0.5F, 0.7F, light);
            renderItemWithFacing(te, te.getStackInSlot(2), pose, buffers, 0.5F, 0.7F, 0.2F, 0.7F, light);
        }
    }

    public static class Grindstone implements BlockEntityRenderer<GrindstoneBlockEntity> {
        public Grindstone(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(GrindstoneBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            BlockState state = te.getBlockState();
            Level level = te.getLevel();
            if (!(state.getBlock() instanceof GrindstoneBlock) || level == null)
                return;
            ItemStack outputStack = te.getStackInSlot(1);
            ItemStack secondaryStack = te.getStackInSlot(2);
            if (outputStack.getCount() < secondaryStack.getCount())
                outputStack = secondaryStack;

            if (state.getValue(GrindstoneBlock.FILLED) && !outputStack.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(te.renderStack, outputStack)) {
                    te.renderStack = outputStack.copy();
                    te.grindColor = ColorGetter.getColor(outputStack);
                }
                int color = te.grindColor;
                float r = color == -1 ? 1F : (color >> 16 & 0xFF) / 255F;
                float g = color == -1 ? 1F : (color >> 8 & 0xFF) / 255F;
                float b = color == -1 ? 1F : (color & 0xFF) / 255F;

                float maxStackSize = outputStack.getMaxStackSize() > 0 ? outputStack.getMaxStackSize() : 1F;
                float fillState = 0.23F * (((float) outputStack.getCount()) / maxStackSize);
                pose.pushPose();
                pose.translate(0, -0.187 + fillState, 0);
                HPRenderHelper.renderModel(pose, buffers, state, GRINDSTONE_FILLED, r, g, b, light, overlay);
                pose.popPose();
            } else if (outputStack.isEmpty()) {
                te.renderStack = ItemStack.EMPTY;
                te.grindColor = -1;
            }

            PathfinderMob worker = te.getWorker();
            if (worker != null)
                HPRenderHelper.renderLeash(worker, te.getBlockPos(), 0.76, partialTicks, pose, buffers);

            ItemStack input = te.getStackInSlot(0);
            if (!input.isEmpty()) {
                HPRenderHelper.renderItem(level, input, pose, buffers, 0.5F, 1F, 0.5F, 1F, true, light);
                if (level.isEmptyBlock(te.getBlockPos().above()))
                    HPRenderHelper.drawString(te, pose, buffers, String.valueOf(input.getCount()), 0.5, 1.35, 0.5, light);
            }

            if (!te.isValid()) {
                HPRenderHelper.drawInvalidText(te, pose, buffers, 0, Localization.GRINDSTONE_INVALID);
                HPRenderHelper.renderInvalidArea(level, te.getBlockPos(), -1, pose, buffers);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(GrindstoneBlockEntity te) {
            return true;
        }
    }

    public static class ChoppingBlock implements BlockEntityRenderer<ManualChopperBlockEntity> {
        public ChoppingBlock(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(ManualChopperBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (te.getLevel() == null)
                return;
            HPRenderHelper.renderItem(te.getLevel(), te.getStackInSlot(0), pose, buffers, 0.5F, 0.63F, 0.5F, 2F, false, light);
            HPRenderHelper.renderItem(te.getLevel(), te.getStackInSlot(1), pose, buffers, 0.5F, 0.63F, 0.5F, 2F, false, light);
        }
    }

    public static class Chopper implements BlockEntityRenderer<ChopperBlockEntity> {
        public Chopper(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(ChopperBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            BlockState state = te.getBlockState();
            Level level = te.getLevel();
            if (!(state.getBlock() instanceof ChopperBlock) || level == null)
                return;
            float windup = te.getVisualWindup(partialTicks);

            pose.pushPose();
            pose.translate(0, windup, 0);
            HPRenderHelper.rotateFacing(pose, te.getForward());
            HPRenderHelper.renderModel(pose, buffers, state, CHOPPER_BLADE, light, overlay);
            pose.popPose();

            HPRenderHelper.renderRope(pose, buffers, 0.5, 2.9 + windup, 0.5, 0.5, 0.2, 0.5, 0.5, 1.7, 0.5, light);

            PathfinderMob worker = te.getWorker();
            if (worker != null)
                HPRenderHelper.renderLeash(worker, te.getBlockPos(), 1.86, partialTicks, pose, buffers);

            HPRenderHelper.renderItem(level, te.getStackInSlot(0), pose, buffers, 0.5F, 0.54F, 0.5F, 1.3F, false, light);
            HPRenderHelper.renderItem(level, te.getStackInSlot(1), pose, buffers, 0.5F, 0.54F, 0.5F, 1.3F, false, light);

            if (!te.isValid()) {
                HPRenderHelper.drawInvalidText(te, pose, buffers, 1, Localization.CHOPPING_INVALID);
                HPRenderHelper.renderInvalidArea(level, te.getBlockPos(), 0, pose, buffers);
            }
        }

        @Override
        public boolean shouldRenderOffScreen(ChopperBlockEntity te) {
            return true;
        }
    }

    public static class Press implements BlockEntityRenderer<PressBlockEntity> {
        public Press(BlockEntityRendererProvider.Context context) {}

        @Override
        public void render(PressBlockEntity te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            BlockState state = te.getBlockState();
            Level level = te.getLevel();
            if (!(state.getBlock() instanceof PressBlock) || level == null)
                return;
            float move = te.getMove(partialTicks);

            pose.pushPose();
            pose.translate(0, -(0.58 * move), 0);
            HPRenderHelper.rotateFacing(pose, te.getForward());
            HPRenderHelper.renderModel(pose, buffers, state, PRESS_TOP, light, overlay);
            pose.popPose();

            PathfinderMob worker = te.getWorker();
            if (worker != null)
                HPRenderHelper.renderLeash(worker, te.getBlockPos(), 1.16, partialTicks, pose, buffers);

            if (move <= 0.25) {
                for (int slot = 0; slot <= 1; slot++) {
                    ItemStack stack = te.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        HPRenderHelper.renderItem(level, stack, pose, buffers, 0.5F, 0.5F, 0.5F, 1F, true, light);
                        HPRenderHelper.drawString(te, pose, buffers, String.valueOf(stack.getCount()), 0.5, 0.85, 0.5, light);
                    }
                }

                FluidStack fluid = te.getTank().getFluid();
                if (!fluid.isEmpty())
                    renderFluid(fluid, te.getTank().getCapacity(), pose, buffers, light);
            }

            if (!te.isValid()) {
                HPRenderHelper.drawInvalidText(te, pose, buffers, 1, Localization.PRESS_INVALID);
                HPRenderHelper.renderInvalidArea(level, te.getBlockPos(), 0, pose, buffers);
            }
        }

        private static void renderFluid(FluidStack stack, int capacity, PoseStack pose, MultiBufferSource buffers, int light) {
            IClientFluidTypeExtensions fluidType = IClientFluidTypeExtensions.of(stack.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidType.getStillTexture(stack));
            int color = fluidType.getTintColor(stack);
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            float a = Math.max((color >>> 24) / 255.0F, 0.75F);
            float height = 0.07F + (0.75F / Math.max(capacity, 1)) * Math.min(stack.getAmount(), capacity);

            VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
            Matrix4f matrix = pose.last().pose();
            float min = 0.1F, max = 0.9F;
            float u0 = sprite.getU(1.6), u1 = sprite.getU(14.4), v0 = sprite.getV(1.6), v1 = sprite.getV(14.4);
            consumer.vertex(matrix, min, height, min).color(r, g, b, a).uv(u0, v0).uv2(light).normal(0, 1, 0).endVertex();
            consumer.vertex(matrix, min, height, max).color(r, g, b, a).uv(u0, v1).uv2(light).normal(0, 1, 0).endVertex();
            consumer.vertex(matrix, max, height, max).color(r, g, b, a).uv(u1, v1).uv2(light).normal(0, 1, 0).endVertex();
            consumer.vertex(matrix, max, height, min).color(r, g, b, a).uv(u1, v0).uv2(light).normal(0, 1, 0).endVertex();
        }

        @Override
        public boolean shouldRenderOffScreen(PressBlockEntity te) {
            return true;
        }
    }
}
