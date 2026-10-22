package se.gory_moon.horsepower.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import se.gory_moon.horsepower.blockentity.HPBaseBlockEntity;
import se.gory_moon.horsepower.blockentity.HPHorseBlockEntity;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.entity.Workers;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of the 1.12.2 {@code BlockHPBase}: the shared right click handling of all machines.
 * <p>
 * Right click order (unchanged from 1.12.2):
 * <ol>
 *     <li>horse machines: attach a valid mob leashed to the player within 7 blocks</li>
 *     <li>insert the held item if it is a valid input</li>
 *     <li>take items out (the hit slot, or output/secondary/input)</li>
 *     <li>empty hand and nothing to take: give the worker back to the player on a lead</li>
 * </ol>
 * The client runs the same decision without changing anything so both sides agree on the result.
 */
public abstract class HPBaseBlock extends BaseEntityBlock {

    protected HPBaseBlock(Properties properties) {
        super(properties);
    }

    public abstract void emptiedOutput(Level level, BlockPos pos);

    /**
     * The slot a click at the given position (relative to the block) takes from. -1 = default order, -2 = nothing.
     */
    public int getSlot(BlockState state, Vec3 hit) {
        return -1;
    }

    public void onWorkerAttached(Player player, PathfinderMob creature) {}

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    public static PathfinderMob findLeashedCreature(Level level, BlockPos pos, Player player) {
        AABB area = new AABB(pos.getX() - 7.0D, pos.getY() - 7.0D, pos.getZ() - 7.0D, pos.getX() + 7.0D, pos.getY() + 7.0D, pos.getZ() + 7.0D);
        List<PathfinderMob> mobs = level.getEntitiesOfClass(PathfinderMob.class, area,
                mob -> mob.isLeashed() && mob.getLeashHolder() == player && Workers.isValidWorker(mob));
        return mobs.isEmpty() ? null : mobs.get(0);
    }

    private static ItemStack takeFromSlot(HPBaseBlockEntity te, int slot, boolean client) {
        if (client)
            return te.canExtract(slot) ? te.getStackInSlot(slot).copy() : ItemStack.EMPTY;
        return te.removeStackFromSlot(slot);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level.getBlockEntity(pos) instanceof HPBaseBlockEntity te))
            return InteractionResult.PASS;
        HPHorseBlockEntity teH = te instanceof HPHorseBlockEntity horse ? horse : null;
        boolean client = level.isClientSide;

        PathfinderMob creature = teH != null ? findLeashedCreature(level, pos, player) : null;
        if (teH != null && creature != null) {
            boolean busy = client ? teH.getWorker() != null : teH.hasWorker();
            if (busy)
                return InteractionResult.PASS;
            if (!client) {
                creature.dropLeash(true, false);
                teH.setWorker(creature);
                onWorkerAttached(player, creature);
            }
            return InteractionResult.sidedSuccess(client);
        } else if (!stack.isEmpty() && te.isItemValidForSlot(0, stack)) {
            ItemStack itemStack = te.getStackInSlot(0);
            boolean flag = false;

            if (itemStack.isEmpty()) {
                int moved = Math.min(stack.getCount(), te.getInventoryStackLimit(stack));
                if (moved > 0) {
                    flag = true;
                    if (!client) {
                        te.setInventorySlotContents(0, stack.copyWithCount(moved));
                        stack.shrink(moved);
                    }
                }
            } else if (HPBaseBlockEntity.canCombine(itemStack, stack)) {
                int i = Math.min(te.getInventoryStackLimit(stack), stack.getMaxStackSize()) - itemStack.getCount();
                int j = Math.min(stack.getCount(), i);
                flag = j > 0;
                if (flag && !client) {
                    stack.shrink(j);
                    itemStack.grow(j);
                }
            }

            if (flag) {
                if (!client)
                    te.markDirty();
                return InteractionResult.sidedSuccess(client);
            }
        }

        int slot = getSlot(state, hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
        ItemStack result = ItemStack.EMPTY;
        if (slot > -1) {
            result = takeFromSlot(te, slot, client);
        } else if (slot > -2) {
            result = takeFromSlot(te, 1, client);
            if (result.isEmpty()) {
                result = takeFromSlot(te, 2, client);
                if (result.isEmpty() && stack.isEmpty() && hand != InteractionHand.OFF_HAND)
                    result = takeFromSlot(te, 0, client);
            }
            if (!result.isEmpty() && !client)
                emptiedOutput(level, pos);
        }

        if (result.isEmpty()) {
            if (!stack.isEmpty())
                return InteractionResult.PASS;
            if (teH != null && !client)
                teH.setWorkerToPlayer(player);
        }

        if (!client) {
            if (!result.isEmpty())
                ItemHandlerHelper.giveItemToPlayer(player, result);
            te.markDirty();
        }
        return InteractionResult.sidedSuccess(client);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof HPBaseBlockEntity te) {
                if (te instanceof HPHorseBlockEntity horse)
                    horse.onMachineRemoved();
                te.dropContents(level, pos);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTicker(level);
    }

    @Nullable
    protected <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level) {
        if (level.isClientSide)
            return (lvl, pos, st, be) -> {
                if (be instanceof HPHorseBlockEntity horse)
                    horse.clientTick();
                else if (be instanceof HandGrindstoneBlockEntity hand)
                    hand.tick();
            };
        return (lvl, pos, st, be) -> {
            if (be instanceof HPHorseBlockEntity horse)
                horse.serverTick();
            else if (be instanceof HandGrindstoneBlockEntity hand)
                hand.tick();
        };
    }
}
