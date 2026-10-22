package se.gory_moon.horsepower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.recipes.HPRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;
import se.gory_moon.horsepower.recipes.PressRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code TileEntityPress}, the horse press.
 * The whole input stack (the recipe amount) is pressed at once into an item or into the internal tank.
 * The tank can only be drained from the outside (no side or DOWN), like 1.12.2's {@code setCanFill(false)}.
 */
public class PressBlockEntity extends HPHorseBlockEntity {

    private final PressTank tank = new PressTank(Configs.general.pressFluidTankSize.get());
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);
    private int currentPressStatus;
    /**
     * The server's press setting, synced so the animation matches a server with a different config.
     */
    private int syncedPointsForPress = -1;
    private float prevMove = 0;
    private float move = 0;

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.PRESS_BE.get(), pos, state, 2);
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("currentPressStatus", currentPressStatus);
        compound.put("fluid", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        tank.setFluid(FluidStack.EMPTY);
        tank.readFromNBT(compound.getCompound("fluid"));

        if (getStackInSlot(0).getCount() > 0)
            currentPressStatus = compound.getInt("currentPressStatus");
        else
            currentPressStatus = 0;
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        super.writeClientData(tag);
        tag.putInt("pointsForPress", Configs.general.pointsForPress.get());
        tag.putInt("tankCapacity", tank.getCapacity());
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        super.readClientData(tag);
        syncedPointsForPress = tag.getInt("pointsForPress");
        if (tag.getInt("tankCapacity") > 0)
            tank.setCapacity(tag.getInt("tankCapacity"));
    }

    public int getPointsForPress() {
        int points = syncedPointsForPress > 0 && level != null && level.isClientSide ? syncedPointsForPress : Configs.general.pointsForPress.get();
        return Math.max(points, 1);
    }

    @Override
    public void markDirty() {
        if (getStackInSlot(0).isEmpty())
            currentPressStatus = 0;
        super.markDirty();
    }

    @Override
    public boolean validateArea() {
        return validateArea(0, 1);
    }

    @Override
    public boolean targetReached() {
        currentPressStatus++;

        if (currentPressStatus >= getPointsForPress()) {
            currentPressStatus = 0;

            pressItem();
            return true;
        }
        markDirty();
        return false;
    }

    @Nullable
    @Override
    public HPRecipe getRecipe() {
        return HPRecipes.getPressRecipe(level, getStackInSlot(0));
    }

    @Override
    public int getPositionOffset() {
        return 0;
    }

    private void pressItem() {
        if (canWork() && getRecipe() instanceof PressRecipe recipe) {
            if (recipe.isLiquidRecipe())
                tank.fillInternal(recipe.getOutputFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
            else
                addToOutput(1, recipe.getOutput());

            getStackInSlot(0).shrink(recipe.getInputCount());
            markDirty();
        }
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        ItemStack itemstack = getStackInSlot(index);
        super.setInventorySlotContents(index, stack);

        boolean flag = !stack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack);
        if (index == 0 && !flag)
            currentPressStatus = 0;

        markDirty();
    }

    @Override
    public boolean canWork() {
        if (getStackInSlot(0).isEmpty())
            return false;
        if (!(getRecipe() instanceof PressRecipe recipe))
            return false;

        ItemStack itemstack = recipe.getOutput();
        FluidStack fluidOutput = recipe.getOutputFluid();

        if (getStackInSlot(0).getCount() < recipe.getInputCount())
            return false;
        if (itemstack.isEmpty() && !recipe.isLiquidRecipe())
            return false;

        ItemStack output = getStackInSlot(1);
        if (recipe.isLiquidRecipe()) {
            return output.isEmpty() && (tank.getFluidAmount() == 0 || tank.fillInternal(fluidOutput, IFluidHandler.FluidAction.SIMULATE) >= fluidOutput.getAmount())
                    && tank.getCapacity() >= fluidOutput.getAmount();
        } else {
            return tank.getFluidAmount() == 0 && (output.isEmpty() || ItemStack.isSameItemSameTags(output, itemstack) && output.getCount() + itemstack.getCount() <= output.getMaxStackSize());
        }
    }

    @Override
    public int getInventoryStackLimit(ItemStack stack) {
        PressRecipe recipe = HPRecipes.getPressRecipe(level, stack);
        if (recipe == null)
            return getInventoryStackLimit();
        return recipe.getInputCount();
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return currentPressStatus == 0 ? super.removeStackFromSlot(index) : ItemStack.EMPTY;
    }

    @Override
    public boolean canExtract(int slot) {
        return currentPressStatus == 0;
    }

    @Override
    public int getInventoryStackLimit() {
        PressRecipe recipe = HPRecipes.getPressRecipe(level, getStackInSlot(0));
        if (recipe == null)
            return 64;
        return recipe.getInputCount();
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && HPRecipes.hasPressRecipe(level, stack) && currentPressStatus == 0 && getStackInSlot(1).isEmpty();
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    public int getField(int id) {
        return id == 0 ? currentPressStatus : 0;
    }

    @Override
    public void setField(int id, int value) {
        if (id == 0)
            currentPressStatus = value;
    }

    @Override
    public int getFieldCount() {
        return 1;
    }

    @Override
    public int getOutputSlot() {
        return 1;
    }

    @Override
    public void clientTick() {
        super.clientTick();
        prevMove = move;
        float targetMove = currentPressStatus / (float) getPointsForPress();
        // Ease the plate towards the synced press state instead of jumping a step per path point
        move = targetMove < move ? targetMove : move + (targetMove - move) * 0.25F;
    }

    /**
     * How far down the press plate is, 0 to 1.
     */
    public float getMove(float partialTicks) {
        return prevMove + (move - prevMove) * partialTicks;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && !remove && (side == null || side == Direction.DOWN))
            return fluidHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    /**
     * Drain only tank, filled by the press itself through {@link #fillInternal}.
     */
    private class PressTank extends FluidTank {
        PressTank(int capacity) {
            super(capacity);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        public int fillInternal(FluidStack resource, FluidAction action) {
            return super.fill(resource, action);
        }

        @Override
        protected void onContentsChanged() {
            PressBlockEntity.this.markDirty();
        }
    }
}
