package se.gory_moon.horsepower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.recipes.HPRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;
import se.gory_moon.horsepower.util.ChoppingAxes;

import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code TileEntityManualChopper}, the hand chopping block.
 * Each left click with an axe is one chop; the amount of output depends on the axe.
 */
public class ManualChopperBlockEntity extends HPBaseBlockEntity implements ChoppingTextureHolder {

    private int currentItemChopAmount;
    private int totalItemChopAmount;
    private final TextureData texture = new TextureData(this);

    public ManualChopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MANUAL_CHOPPER_BE.get(), pos, state, 2);
    }

    @Override
    public TextureData getTextureData() {
        return texture;
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("chopTime", currentItemChopAmount);
        compound.putInt("totalChopTime", totalItemChopAmount);
        texture.save(compound);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        texture.load(compound);
        if (getStackInSlot(0).getCount() > 0) {
            currentItemChopAmount = compound.getInt("chopTime");
            totalItemChopAmount = compound.getInt("totalChopTime");
        } else {
            currentItemChopAmount = 0;
            totalItemChopAmount = 1;
        }
    }

    @Override
    protected void onClientDataUpdated() {
        texture.onClientUpdate();
    }

    @Override
    public ModelData getModelData() {
        return texture.getModelData();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && HPRecipes.hasChopperRecipe(level, stack, true) && getStackInSlot(1).isEmpty() && getStackInSlot(0).isEmpty();
    }

    public boolean chop(Player player, ItemStack held) {
        if (canWork()) {
            currentItemChopAmount++;

            if (currentItemChopAmount >= totalItemChopAmount) {
                currentItemChopAmount = 0;

                totalItemChopAmount = HPRecipes.getChoppingTime(level, getStackInSlot(0), true);
                chopItem(held);
                return true;
            }
            markDirty();
        }
        return false;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack itemstack = getStackInSlot(index);
        super.setInventorySlotContents(index, stack);

        if (index == 1 && getStackInSlot(1).isEmpty())
            markDirty();

        boolean flag = !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack);
        if (index == 0 && !flag) {
            totalItemChopAmount = HPRecipes.getChoppingTime(level, stack, true);
            currentItemChopAmount = 0;
            markDirty();
        }
    }

    private void chopItem(ItemStack held) {
        if (level == null || !canWork())
            return;
        ItemStack input = getStackInSlot(0);
        if (!level.isClientSide) {
            ItemStack result = getRecipeItemStack().copy();

            double baseAmount = ((double) ChoppingAxes.getBaseAmount(held)) / 100D;
            int chance = ChoppingAxes.getChance(held);

            result.setCount((int) Math.ceil((double) result.getCount() * baseAmount));
            if (chance >= 100 || level.random.nextInt(100) < chance)
                result.grow(1);

            if (!result.isEmpty()) {
                if (Configs.general.choppingBlockDrop.get()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 0.5, worldPosition.getZ(), result);
                } else {
                    ItemStack output = getStackInSlot(1);
                    if (output.isEmpty() || ItemStack.isSameItemSameTags(output, result)) {
                        int space = output.isEmpty() ? result.getMaxStackSize() : output.getMaxStackSize() - output.getCount();
                        int moved = Mth.clamp(result.getCount(), 0, space);
                        if (output.isEmpty())
                            setInventorySlotContents(1, result.copyWithCount(moved));
                        else
                            output.grow(moved);
                        result.shrink(moved);
                    }
                    // More than a stack (high tier axe bonus): never void items
                    if (!result.isEmpty())
                        Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 0.5, worldPosition.getZ(), result);
                }
            }
            level.playSound(null, worldPosition, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        input.shrink(1);
        markDirty();
    }

    @Nullable
    @Override
    public HPRecipe getRecipe() {
        return HPRecipes.getChoppingBlockRecipe(level, getStackInSlot(0), true);
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public int getField(int id) {
        return switch (id) {
            case 0 -> totalItemChopAmount;
            case 1 -> currentItemChopAmount;
            default -> 0;
        };
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0 -> totalItemChopAmount = value;
            case 1 -> currentItemChopAmount = value;
        }
    }

    @Override
    public int getFieldCount() {
        return 2;
    }

    @Override
    public int getOutputSlot() {
        return 1;
    }
}
