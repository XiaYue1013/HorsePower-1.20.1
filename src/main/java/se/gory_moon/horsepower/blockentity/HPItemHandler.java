package se.gory_moon.horsepower.blockentity;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;

/**
 * Replacement of the 1.12.2 {@code IHPInventory} + {@code InvWrapper} pair.
 * All validation goes through the block entity so automation follows the exact same rules as players:
 * only valid recipe inputs go in slot 0, output slots are extract only and the per recipe stack limits apply.
 */
public class HPItemHandler implements IItemHandlerModifiable {

    private final HPBaseBlockEntity owner;

    public HPItemHandler(HPBaseBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public int getSlots() {
        return owner.getContainerSize();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return owner.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        owner.setInventorySlotContents(slot, stack);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= getSlots() || !owner.isItemValidForSlot(slot, stack))
            return stack;

        ItemStack existing = owner.getStackInSlot(slot);
        int limit = Math.min(owner.getInventoryStackLimit(stack), stack.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing))
                return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0)
            return stack;

        int moved = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty())
                owner.setInventorySlotContents(slot, ItemHandlerHelper.copyStackWithSize(stack, moved));
            else
                existing.grow(moved);
            owner.markDirty();
        }
        return moved >= stack.getCount() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - moved);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || slot < 0 || slot >= getSlots() || !owner.canExtract(slot))
            return ItemStack.EMPTY;
        ItemStack existing = owner.getStackInSlot(slot);
        if (existing.isEmpty())
            return ItemStack.EMPTY;

        int toExtract = Math.min(Math.min(amount, existing.getMaxStackSize()), existing.getCount());
        if (simulate)
            return ItemHandlerHelper.copyStackWithSize(existing, toExtract);

        ItemStack result = existing.split(toExtract);
        if (existing.isEmpty())
            owner.setInventorySlotContents(slot, ItemStack.EMPTY);
        owner.markDirty();
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return owner.isItemValidForSlot(slot, stack);
    }
}
