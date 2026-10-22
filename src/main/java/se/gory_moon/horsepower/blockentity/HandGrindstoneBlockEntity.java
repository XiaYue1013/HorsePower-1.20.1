package se.gory_moon.horsepower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.recipes.HPRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;

import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code TileEntityHandGrindstone}.
 * Every right click adds one crank rotation (18 ticks), every finished rotation adds "pointsPerRotation" points.
 */
public class HandGrindstoneBlockEntity extends HPBaseBlockEntity {

    public static final int TICKS_PER_ROTATION = 18;

    private int currentItemMillTime;
    private int totalItemMillTime;

    private float visibleRotation = 0;
    private float prevVisibleRotation = 0;
    private int currentTicks = 0;
    private int rotation = 0;

    public HandGrindstoneBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.HAND_GRINDSTONE_BE.get(), pos, state, 3);
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("millTime", currentItemMillTime);
        compound.putInt("totalMillTime", totalItemMillTime);
        compound.putInt("currentRotation", rotation);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (getStackInSlot(0).getCount() > 0) {
            currentItemMillTime = compound.getInt("millTime");
            totalItemMillTime = compound.getInt("totalMillTime");
            rotation = compound.getInt("currentRotation");
        } else {
            currentItemMillTime = 0;
            totalItemMillTime = 1;
            rotation = 0;
        }
    }

    @Nullable
    @Override
    public HPRecipe getRecipe() {
        return HPRecipes.getGrindstoneRecipe(level, getStackInSlot(0), true);
    }

    private void millItem() {
        if (level != null && !level.isClientSide && canWork()) {
            HPRecipe recipe = getRecipe();
            if (recipe == null)
                return;
            addToOutput(1, recipe.getOutput());
            processSecondaries(level, recipe, this);
            getStackInSlot(0).shrink(recipe.getInputCount());
        }
    }

    public static void processSecondaries(Level level, HPRecipe recipe, HPBaseBlockEntity be) {
        ItemStack secondary = recipe.getSecondary();
        if (!secondary.isEmpty()) {
            int recipeChance = recipe.getSecondaryChance();
            if (recipeChance >= 100 || level.random.nextInt(100) < recipeChance)
                be.addToOutput(2, secondary);
        }
    }

    @Override
    public void markDirty() {
        if (getStackInSlot(0).isEmpty())
            currentItemMillTime = 0;
        super.markDirty();
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack itemstack = getStackInSlot(index);
        super.setInventorySlotContents(index, stack);

        boolean flag = !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack);
        if (index == 0 && !flag) {
            totalItemMillTime = HPRecipes.getGrindstoneTime(level, stack, true);
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
        return index == 0 && HPRecipes.hasGrindstoneRecipe(level, stack, true);
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

    public boolean turn() {
        if (level == null || level.isClientSide)
            return false;

        if (rotation < 3 && canWork()) {
            rotation += TICKS_PER_ROTATION;
            markDirty();
            return true;
        }
        return false;
    }

    public void tick() {
        prevVisibleRotation = visibleRotation;
        if (rotation > 0) {
            visibleRotation = (visibleRotation - 360F / TICKS_PER_ROTATION) % -360;
            currentTicks++;
            if (currentTicks >= TICKS_PER_ROTATION) {
                currentTicks -= TICKS_PER_ROTATION;

                currentItemMillTime += Configs.general.pointsPerRotation.get();

                if (currentItemMillTime >= totalItemMillTime) {
                    currentItemMillTime = 0;

                    millItem();
                    totalItemMillTime = HPRecipes.getGrindstoneTime(level, getStackInSlot(0), true);
                }
                markDirty();
            }

            rotation--;
        } else {
            visibleRotation = 0;
            prevVisibleRotation = 0;
        }
    }

    /**
     * Interpolated crank rotation in degrees (0 to -360).
     */
    public float getVisibleRotation(float partialTicks) {
        float prev = prevVisibleRotation;
        float current = visibleRotation;
        if (current > prev)
            current -= 360F; // wrapped around
        return prev + (current - prev) * partialTicks;
    }
}
