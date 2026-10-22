package se.gory_moon.horsepower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import se.gory_moon.horsepower.recipes.HPRecipe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code TileEntityHPBase}.
 * Slot 0 is the input, slots 1..{@link #getOutputSlot()} are outputs.
 * Item capability: no side = everything, DOWN = outputs, other sides = input (same as 1.12.2).
 */
public abstract class HPBaseBlockEntity extends BlockEntity {

    protected NonNullList<ItemStack> itemStacks;
    protected final HPItemHandler inventory;

    private final LazyOptional<IItemHandler> handlerNull;
    private final LazyOptional<IItemHandler> handlerBottom;
    private final LazyOptional<IItemHandler> handlerIn;

    public HPBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        itemStacks = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
        inventory = new HPItemHandler(this);
        handlerNull = LazyOptional.of(() -> inventory);
        handlerBottom = LazyOptional.of(() -> new RangedWrapper(inventory, 1, getOutputSlot() + 1));
        handlerIn = LazyOptional.of(() -> new RangedWrapper(inventory, 0, 1));
    }

    @Nullable
    public abstract HPRecipe getRecipe();

    public ItemStack getRecipeItemStack() {
        HPRecipe recipe = getRecipe();
        return recipe != null ? recipe.getOutput() : ItemStack.EMPTY;
    }

    public abstract int getInventoryStackLimit();

    public int getInventoryStackLimit(ItemStack stack) {
        return getInventoryStackLimit();
    }

    public abstract boolean isItemValidForSlot(int index, ItemStack stack);

    public abstract int getOutputSlot();

    public int getField(int id) {
        return 0;
    }

    public void setField(int id, int value) {}

    public int getFieldCount() {
        return 0;
    }

    public int getContainerSize() {
        return itemStacks.size();
    }

    public HPItemHandler getInventory() {
        return inventory;
    }

    public ItemStack getStackInSlot(int index) {
        if (index < 0 || index >= itemStacks.size())
            return ItemStack.EMPTY;
        return itemStacks.get(index);
    }

    /**
     * Removes and returns the whole stack, the caller is responsible for calling {@link #markDirty()}.
     */
    public ItemStack removeStackFromSlot(int index) {
        if (index < 0 || index >= itemStacks.size())
            return ItemStack.EMPTY;
        ItemStack stack = itemStacks.get(index);
        itemStacks.set(index, ItemStack.EMPTY);
        return stack;
    }

    /**
     * If automation may pull items out of the slot.
     */
    public boolean canExtract(int slot) {
        return true;
    }

    public void setInventorySlotContents(int index, ItemStack stack) {
        setSlotContent(index, stack);
    }

    protected void setSlotContent(int index, ItemStack stack) {
        if (index < 0 || index >= itemStacks.size())
            return;
        itemStacks.set(index, stack);
        if (index == 0 && stack.getCount() > getInventoryStackLimit(stack))
            stack.setCount(getInventoryStackLimit(stack));
    }

    public boolean isInventoryEmpty() {
        for (ItemStack stack : itemStacks) {
            if (!stack.isEmpty())
                return false;
        }
        return true;
    }

    /**
     * Port of 1.12.2 {@code markDirty}: saves and sends the new state to the clients.
     */
    public void markDirty() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 2);
        }
    }

    public boolean canWork() {
        if (getStackInSlot(0).isEmpty())
            return false;

        HPRecipe recipe = getRecipe();
        if (recipe == null)
            return false;

        ItemStack itemstack = recipe.getOutput();
        ItemStack secondary = recipe.getSecondary();

        if (getStackInSlot(0).getCount() < recipe.getInputCount())
            return false;
        if (itemstack.isEmpty())
            return false;

        ItemStack output = getStackInSlot(1);
        ItemStack outputSecondary = secondary.isEmpty() ? ItemStack.EMPTY : getStackInSlot(2);
        if (!secondary.isEmpty() && !outputSecondary.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(outputSecondary, secondary))
                return false;
            if (outputSecondary.getCount() + secondary.getCount() > secondary.getMaxStackSize())
                return false;
        }
        return output.isEmpty() || ItemStack.isSameItemSameTags(output, itemstack) && output.getCount() + itemstack.getCount() <= output.getMaxStackSize();
    }

    /**
     * Adds a produced stack into an output slot. Only called after {@link #canWork()} confirmed that it fits.
     */
    protected void addToOutput(int slot, ItemStack result) {
        ItemStack output = getStackInSlot(slot);
        if (output.isEmpty())
            setInventorySlotContents(slot, result.copy());
        else if (ItemStack.isSameItemSameTags(output, result))
            output.grow(result.getCount());
    }

    public static boolean canCombine(ItemStack stack1, ItemStack stack2) {
        return ItemStack.isSameItemSameTags(stack1, stack2) && stack1.getCount() <= stack1.getMaxStackSize();
    }

    public Direction getForward() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return Direction.NORTH;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < itemStacks.size(); i++) {
            ItemStack stack = itemStacks.get(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                itemStacks.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        itemStacks = NonNullList.withSize(itemStacks.size(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound, itemStacks);
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        ContainerHelper.saveAllItems(compound, itemStacks, true);
    }

    /**
     * Extra data that is only sent to clients.
     */
    protected void writeClientData(CompoundTag tag) {}

    protected void readClientData(CompoundTag tag) {}

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = saveWithoutMetadata();
        writeClientData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        readClientData(tag);
        onClientDataUpdated();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null)
            handleUpdateTag(tag);
    }

    protected void onClientDataUpdated() {}

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && !remove) {
            if (side == null)
                return handlerNull.cast();
            else if (side == Direction.DOWN)
                return handlerBottom.cast();
            else
                return handlerIn.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerNull.invalidate();
        handlerBottom.invalidate();
        handlerIn.invalidate();
    }
}
