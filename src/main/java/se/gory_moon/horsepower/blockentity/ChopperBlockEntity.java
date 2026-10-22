package se.gory_moon.horsepower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.recipes.HPRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;

import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code TileEntityChopper}, the horse chopping block.
 * Every path point winds the blade up; after "pointsForWindup" points the blade drops and does one chop.
 */
public class ChopperBlockEntity extends HPHorseBlockEntity implements ChoppingTextureHolder {

    private int currentWindup;
    private int currentItemChopTime;
    private int totalItemChopTime;
    private float visualWindup = 0;
    private float prevVisualWindup = 0;
    /**
     * The server's windup setting, synced so the animation matches a server with a different config.
     */
    private int syncedPointsForWindup = -1;
    private final TextureData texture = new TextureData(this);

    public ChopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.CHOPPER_BE.get(), pos, state, 2);
    }

    @Override
    public TextureData getTextureData() {
        return texture;
    }

    @Override
    public ModelData getModelData() {
        return texture.getModelData();
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("currentWindup", currentWindup);
        compound.putInt("chopTime", currentItemChopTime);
        compound.putInt("totalChopTime", totalItemChopTime);
        texture.save(compound);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        texture.load(compound);
        currentWindup = compound.getInt("currentWindup");

        if (getStackInSlot(0).getCount() > 0) {
            currentItemChopTime = compound.getInt("chopTime");
            totalItemChopTime = compound.getInt("totalChopTime");
        } else {
            currentItemChopTime = 0;
            totalItemChopTime = 1;
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        super.writeClientData(tag);
        tag.putInt("pointsForWindup", Configs.general.pointsForWindup.get());
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        super.readClientData(tag);
        syncedPointsForWindup = tag.getInt("pointsForWindup");
    }

    @Override
    protected void onClientDataUpdated() {
        texture.onClientUpdate();
    }

    public int getPointsForWindup() {
        int points = syncedPointsForWindup > 0 && level != null && level.isClientSide ? syncedPointsForWindup : Configs.general.pointsForWindup.get();
        return Math.max(points, 1);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && HPRecipes.hasChopperRecipe(level, stack, false) && getStackInSlot(1).isEmpty() && getStackInSlot(0).isEmpty();
    }

    @Override
    public boolean validateArea() {
        return validateArea(0, 1);
    }

    @Override
    public void clientTick() {
        super.clientTick();
        updateVisualWindup();
    }

    @Override
    public void serverTick() {
        super.serverTick();
        updateVisualWindup();
    }

    private void updateVisualWindup() {
        prevVisualWindup = visualWindup;
        float windup = getPointsForWindup();
        visualWindup = windup <= 1 ? 0 : -0.74F + (0.74F * (((float) currentWindup) / (windup - 1)));
    }

    @Override
    public boolean targetReached() {
        currentWindup++;

        if (currentWindup >= Configs.general.pointsForWindup.get()) {
            currentWindup = 0;
            currentItemChopTime++;

            if (currentItemChopTime >= totalItemChopTime) {
                currentItemChopTime = 0;

                totalItemChopTime = HPRecipes.getChoppingTime(level, getStackInSlot(0), false);
                chopItem();
                return true;
            }
        }
        markDirty();
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
            totalItemChopTime = HPRecipes.getChoppingTime(level, stack, false);
            currentItemChopTime = 0;
            currentWindup = 0;
            markDirty();
        }
    }

    private void chopItem() {
        if (canWork()) {
            HPRecipe recipe = getRecipe();
            if (recipe == null)
                return;
            addToOutput(1, recipe.getOutput());
            getStackInSlot(0).shrink(recipe.getInputCount());
            markDirty();
        }
    }

    @Nullable
    @Override
    public HPRecipe getRecipe() {
        return HPRecipes.getChoppingBlockRecipe(level, getStackInSlot(0), false);
    }

    @Override
    public int getPositionOffset() {
        return 0;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public int getField(int id) {
        return switch (id) {
            case 0 -> totalItemChopTime;
            case 1 -> currentItemChopTime;
            case 2 -> currentWindup;
            default -> 0;
        };
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0 -> totalItemChopTime = value;
            case 1 -> currentItemChopTime = value;
            case 2 -> currentWindup = value;
        }
    }

    @Override
    public int getFieldCount() {
        return 3;
    }

    @Override
    public int getOutputSlot() {
        return 1;
    }

    public float getVisualWindup(float partialTicks) {
        return prevVisualWindup + (visualWindup - prevVisualWindup) * partialTicks;
    }
}
