package se.gory_moon.horsepower.blockentity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import se.gory_moon.horsepower.util.ChoppingTexture;

import javax.annotation.Nullable;

/**
 * The "textureBlock" of a chopping block (1.12.2 stored it in the tile's forge data).
 * On the client it is exposed to the retexturing model through {@link ModelData}.
 */
public class TextureData {

    public static final ModelProperty<BlockState> TEXTURE = new ModelProperty<>();

    private final BlockEntity owner;
    private CompoundTag tag = new CompoundTag();
    @Nullable
    private BlockState state;
    private boolean resolved = false;

    public TextureData(BlockEntity owner) {
        this.owner = owner;
    }

    public CompoundTag getTag() {
        return tag;
    }

    public void setTag(@Nullable CompoundTag tag) {
        this.tag = tag != null ? tag.copy() : new CompoundTag();
        this.resolved = false;
        owner.setChanged();
    }

    public ItemStack getTextureStack() {
        return ChoppingTexture.getTextureStack(tag);
    }

    @Nullable
    public BlockState getState() {
        if (!resolved) {
            state = ChoppingTexture.getTextureState(getTextureStack());
            resolved = true;
        }
        return state;
    }

    public void save(CompoundTag compound) {
        if (!tag.isEmpty())
            compound.put(ChoppingTexture.TAG, tag.copy());
    }

    public void load(CompoundTag compound) {
        CompoundTag loaded = compound.getCompound(ChoppingTexture.TAG);
        if (!loaded.equals(tag)) {
            tag = loaded.copy();
            resolved = false;
        }
    }

    public ModelData getModelData() {
        BlockState textureState = getState();
        return textureState == null ? ModelData.EMPTY : ModelData.builder().with(TEXTURE, textureState).build();
    }

    public void onClientUpdate() {
        if (owner.getLevel() != null && owner.getLevel().isClientSide) {
            owner.requestModelDataUpdate();
            BlockState blockState = owner.getBlockState();
            owner.getLevel().sendBlockUpdated(owner.getBlockPos(), blockState, blockState, 8);
        }
    }
}
