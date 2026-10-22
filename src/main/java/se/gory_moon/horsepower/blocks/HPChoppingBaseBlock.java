package se.gory_moon.horsepower.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import se.gory_moon.horsepower.blockentity.ChoppingTextureHolder;
import se.gory_moon.horsepower.util.ChoppingTexture;

import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code BlockHPChoppingBase}: chopping blocks keep the texture of the log they were crafted
 * from, on the item, the placed block, the drop (loot table copy_nbt) and the pick block stack.
 */
public abstract class HPChoppingBaseBlock extends HPBaseBlock {

    protected HPChoppingBaseBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof ChoppingTextureHolder holder) {
            CompoundTag tag = stack.getTagElement(ChoppingTexture.TAG);
            holder.getTextureData().setTag(tag);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        writeDataOntoItemstack(stack, level, pos);
        return stack;
    }

    public static void writeDataOntoItemstack(ItemStack item, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ChoppingTextureHolder holder) {
            CompoundTag data = holder.getTextureData().getTag();
            if (!data.isEmpty())
                item.getOrCreateTag().put(ChoppingTexture.TAG, data.copy());
        }
    }
}
