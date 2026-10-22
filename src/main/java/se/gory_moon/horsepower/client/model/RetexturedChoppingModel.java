package se.gory_moon.horsepower.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import se.gory_moon.horsepower.blockentity.TextureData;
import se.gory_moon.horsepower.util.ChoppingTexture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Port of the 1.12.2 {@code BakedChopperModel}: the chopping block models use the oak log side/top textures as
 * placeholders, they are swapped for the textures of the log the block was crafted from.
 * The block gets the log from its block entity ({@link TextureData#TEXTURE}), the item from its "textureBlock" NBT.
 */
public class RetexturedChoppingModel extends BakedModelWrapper<BakedModel> {

    private static final ResourceLocation SIDE_PLACEHOLDER = new ResourceLocation("minecraft", "block/oak_log");
    private static final ResourceLocation TOP_PLACEHOLDER = new ResourceLocation("minecraft", "block/oak_log_top");
    private static final Object NULL_SIDE = new Object();

    private final Map<BlockState, Map<Object, List<BakedQuad>>> quadCache = new ConcurrentHashMap<>();
    private final Map<BlockState, BakedModel> itemModels = new ConcurrentHashMap<>();
    private final ItemOverrides overrides = new ChoppingItemOverrides();

    public RetexturedChoppingModel(BakedModel original) {
        super(original);
    }

    private static TextureAtlasSprite sideSprite(BlockState texture) {
        return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(texture);
    }

    private static TextureAtlasSprite topSprite(BlockState texture) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(texture);
        List<BakedQuad> quads = model.getQuads(texture, Direction.UP, RandomSource.create(42L), ModelData.EMPTY, null);
        return !quads.isEmpty() ? quads.get(0).getSprite() : sideSprite(texture);
    }

    private List<BakedQuad> retextured(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, BlockState texture, @Nullable RenderType renderType) {
        Map<Object, List<BakedQuad>> perSide = quadCache.computeIfAbsent(texture, t -> new ConcurrentHashMap<>());
        return perSide.computeIfAbsent(side == null ? NULL_SIDE : side, s -> {
            TextureAtlasSprite sideSprite = sideSprite(texture);
            TextureAtlasSprite topSprite = topSprite(texture);
            List<BakedQuad> original = originalModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);
            List<BakedQuad> out = new ArrayList<>(original.size());
            for (BakedQuad quad : original) {
                ResourceLocation name = quad.getSprite().contents().name();
                if (SIDE_PLACEHOLDER.equals(name))
                    out.add(retexture(quad, sideSprite));
                else if (TOP_PLACEHOLDER.equals(name))
                    out.add(retexture(quad, topSprite));
                else
                    out.add(quad);
            }
            return out;
        });
    }

    private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
        TextureAtlasSprite old = quad.getSprite();
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            int offset = i * stride;
            float u = Float.intBitsToFloat(vertices[offset + 4]);
            float v = Float.intBitsToFloat(vertices[offset + 5]);
            vertices[offset + 4] = Float.floatToRawIntBits(sprite.getU(old.getUOffset(u)));
            vertices[offset + 5] = Float.floatToRawIntBits(sprite.getV(old.getVOffset(v)));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade(), quad.hasAmbientOcclusion());
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData data, @Nullable RenderType renderType) {
        BlockState texture = data.get(TextureData.TEXTURE);
        if (texture == null)
            return originalModel.getQuads(state, side, rand, data, renderType);
        return retextured(state, side, rand, texture, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@Nonnull ModelData data) {
        BlockState texture = data.get(TextureData.TEXTURE);
        return texture != null ? sideSprite(texture) : originalModel.getParticleIcon(data);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    /**
     * BakedModelWrapper returns the original (oak) model from both of these, which would undo the retexturing
     * for items (the item renderer calls applyTransform and then getRenderPasses on the result).
     */
    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        originalModel.applyTransform(context, poseStack, leftHand);
        return this;
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return List.of(this);
    }

    private class ChoppingItemOverrides extends ItemOverrides {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            BlockState texture = ChoppingTexture.getTextureState(ChoppingTexture.getTextureStack(stack));
            if (texture == null)
                return RetexturedChoppingModel.this;
            return itemModels.computeIfAbsent(texture, Fixed::new);
        }
    }

    /**
     * The item model for one texture.
     */
    private class Fixed extends BakedModelWrapper<BakedModel> {
        private final BlockState texture;

        Fixed(BlockState texture) {
            super(RetexturedChoppingModel.this.originalModel);
            this.texture = texture;
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand) {
            return retextured(state, side, rand, texture, null);
        }

        @Nonnull
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand, @Nonnull ModelData data, @Nullable RenderType renderType) {
            return retextured(state, side, rand, texture, renderType);
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return sideSprite(texture);
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
            originalModel.applyTransform(context, poseStack, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }
    }
}
