package se.gory_moon.horsepower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.blocks.GrindstoneBlock;
import se.gory_moon.horsepower.recipes.HPRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;

import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code TileEntityGrindstone}, the horse powered grindstone.
 * The grindstone sits one block above the ground the worker walks on.
 */
public class GrindstoneBlockEntity extends HPHorseBlockEntity {

    private int currentItemMillTime;
    private int totalItemMillTime;

    /**
     * Client only: the output the grind color was computed for and the color itself (see the renderer).
     */
    public ItemStack renderStack = ItemStack.EMPTY;
    public int grindColor = -1;

    public GrindstoneBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.GRINDSTONE_BE.get(), pos, state, 3);
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("millTime", currentItemMillTime);
        compound.putInt("totalMillTime", totalItemMillTime);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (getStackInSlot(0).getCount() > 0) {
            currentItemMillTime = compound.getInt("millTime");
            totalItemMillTime = compound.getInt("totalMillTime");
        } else {
            currentItemMillTime = 0;
            totalItemMillTime = 1;
        }
    }

    private void updateFilledState() {
        if (getStackInSlot(1).isEmpty() && getStackInSlot(2).isEmpty())
            GrindstoneBlock.setState(false, level, worldPosition);
    }

    @Override
    public void markDirty() {
        updateFilledState();
        if (getStackInSlot(0).isEmpty())
            currentItemMillTime = 0;
        super.markDirty();
    }

    @Override
    public boolean validateArea() {
        return validateArea(0, -1);
    }

    @Override
    public boolean targetReached() {
        currentItemMillTime++;

        if (currentItemMillTime >= totalItemMillTime) {
            currentItemMillTime = 0;

            totalItemMillTime = HPRecipes.getGrindstoneTime(level, getStackInSlot(0), false);
            millItem();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public HPRecipe getRecipe() {
        return HPRecipes.getGrindstoneRecipe(level, getStackInSlot(0), false);
    }

    @Override
    public int getPositionOffset() {
        return -1;
    }

    private void millItem() {
        if (level != null && canWork()) {
            HPRecipe recipe = getRecipe();
            if (recipe == null)
                return;
            addToOutput(1, recipe.getOutput());
            HandGrindstoneBlockEntity.processSecondaries(level, recipe, this);
            getStackInSlot(0).shrink(recipe.getInputCount());
            GrindstoneBlock.setState(true, level, worldPosition);
        }
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack itemstack = getStackInSlot(index);
        super.setInventorySlotContents(index, stack);

        if ((index == 1 || index == 2))
            updateFilledState();

        boolean flag = !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack);
        if (index == 0 && !flag) {
            totalItemMillTime = HPRecipes.getGrindstoneTime(level, stack, false);
            currentItemMillTime = 0;
        }
        markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && HPRecipes.hasGrindstoneRecipe(level, stack, false);
    }

    @Override
    public int getField(int id) {
        return switch (id) {
            case 0 -> totalItemMillTime;
            case 1 -> currentItemMillTime;
            default -> 0;
        };
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0 -> totalItemMillTime = value;
            case 1 -> currentItemMillTime = value;
        }
    }

    @Override
    public int getFieldCount() {
        return 2;
    }

    @Override
    public int getOutputSlot() {
        return 2;
    }
}
