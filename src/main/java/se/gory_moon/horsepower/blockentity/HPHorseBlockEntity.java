package se.gory_moon.horsepower.blockentity;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import se.gory_moon.horsepower.entity.HorsePowerWorkGoal;
import se.gory_moon.horsepower.entity.Workers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Port of the 1.12.2 {@code TileEntityHPHorseBase}: the animal powered machines.
 * <p>
 * The worker walks along 8 path points around the block ({@link #PATH}, scaled by 2). Every time its bounding box
 * reaches the current point the machine gets one "point" of work ({@link #targetReached()}), so one lap is 8 points.
 * No movement means no progress; the worker is never teleported.
 * <p>
 * Lifecycle safety on top of the 1.12.2 logic:
 * <ul>
 *     <li>the worker is stored by UUID and found again after chunk/world reloads; an unloaded worker is not "lost"</li>
 *     <li>a worker is "claimed" by exactly one machine (entity persistent data), a second machine can't share it</li>
 *     <li>the lead that attached the worker is stored by the machine and dropped exactly once when the worker is
 *     lost (death, dimension change, leashed by a player, walked away) or when the machine is removed</li>
 * </ul>
 */
public abstract class HPHorseBlockEntity extends HPBaseBlockEntity {

    protected static final double[][] PATH = {{-1.5, -1.5}, {0, -1.5}, {1, -1.5}, {1, 0}, {1, 1}, {0, 1}, {-1.5, 1}, {-1.5, 0}};
    private static final double MAX_WORKER_DISTANCE_SQ = 45;
    private static final int SEARCH_RANGE = 7;

    protected final AABB[] searchAreas = new AABB[8];
    protected List<BlockPos> searchPos = null;
    protected int origin = -1;
    protected int target = origin;

    protected boolean hasWorker = false;
    @Nullable
    protected PathfinderMob worker;
    @Nullable
    protected UUID workerUUID;

    protected boolean valid = false;
    protected int validationTimer = 0;
    protected int locateHorseTimer = 0;
    protected boolean running = true;
    protected boolean wasRunning = false;

    private int clientWorkerId = -1;

    public HPHorseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state, inventorySize);
    }

    public abstract boolean validateArea();

    public abstract boolean targetReached();

    public abstract int getPositionOffset();

    /**
     * The 7x7 area (minus the 3x3 center) on the two given layers must be free, as in 1.12.2.
     */
    protected boolean validateArea(int firstY, int secondY) {
        if (level == null)
            return false;
        if (searchPos == null) {
            searchPos = Lists.newArrayList();
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if ((x <= 1 && x >= -1) && (z <= 1 && z >= -1))
                        continue;
                    searchPos.add(worldPosition.offset(x, firstY, z));
                    searchPos.add(worldPosition.offset(x, secondY, z));
                }
            }
        }
        for (BlockPos pos : searchPos) {
            if (!level.isLoaded(pos) || !level.getBlockState(pos).canBeReplaced())
                return false;
        }
        return true;
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        target = compound.getInt("target");
        origin = compound.getInt("origin");
        hasWorker = compound.getBoolean("hasWorker");
        if (hasWorker && compound.contains("leash", 10) && compound.getCompound("leash").hasUUID("UUID"))
            workerUUID = compound.getCompound("leash").getUUID("UUID");
        else
            workerUUID = null;
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        compound.putInt("target", target);
        compound.putInt("origin", origin);
        // Also saved while the worker is unloaded so it is found again later (1.12.2 dropped it in that case)
        compound.putBoolean("hasWorker", workerUUID != null);
        if (workerUUID != null) {
            CompoundTag leash = new CompoundTag();
            leash.putUUID("UUID", workerUUID);
            compound.put("leash", leash);
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putInt("workerId", worker != null && !worker.isRemoved() ? worker.getId() : -1);
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        clientWorkerId = tag.contains("workerId") ? tag.getInt("workerId") : -1;
    }

    private Vec3 machineCenter() {
        return Vec3.atCenterOf(worldPosition);
    }

    private AABB searchBox() {
        return new AABB(worldPosition).inflate(SEARCH_RANGE);
    }

    /**
     * Port of {@code findWorker()}: looks the stored worker up again after a reload.
     */
    private boolean findWorker() {
        if (!(level instanceof ServerLevel serverLevel) || workerUUID == null)
            return false;
        Entity entity = serverLevel.getEntity(workerUUID);
        if (!(entity instanceof PathfinderMob mob) || entity.isRemoved() || !searchBox().contains(entity.position()))
            return false;
        if (!Workers.isValidWorker(mob)) {
            releaseWorker(true);
            return true;
        }
        if (!Workers.isClaimedBy(mob, level, worldPosition) && Workers.hasClaim(mob)) {
            // Taken over by another machine while this one was unloaded: give the stored lead back
            releaseWorker(true);
            return true;
        }
        setWorker(mob);
        return true;
    }

    public void setWorker(PathfinderMob newWorker) {
        if (level == null || level.isClientSide)
            return;
        if (worker != null && worker != newWorker)
            releaseWorker(true);
        else if (worker == null && workerUUID != null && !workerUUID.equals(newWorker.getUUID()))
            dropLead(); // the lead of the unloaded previous worker

        hasWorker = true;
        worker = newWorker;
        workerUUID = newWorker.getUUID();
        worker.restrictTo(worldPosition, 3);
        Workers.claim(worker, level, worldPosition);
        worker.goalSelector.removeAllGoals(goal -> goal instanceof HorsePowerWorkGoal);
        worker.goalSelector.addGoal(HorsePowerWorkGoal.PRIORITY, new HorsePowerWorkGoal(worker, this));
        target = getClosestTarget();
        markDirty();
    }

    /**
     * Port of {@code setWorkerToPlayer}: right clicking with an empty hand gives the worker back on a lead.
     */
    public void setWorkerToPlayer(Player player) {
        if (hasWorker() && worker != null && worker.canBeLeashed(player)) {
            PathfinderMob mob = worker;
            detachFromMob(mob);
            hasWorker = false;
            worker = null;
            workerUUID = null;
            mob.setLeashedTo(player, true);
            markDirty();
        }
    }

    private void detachFromMob(PathfinderMob mob) {
        mob.goalSelector.removeAllGoals(goal -> goal instanceof HorsePowerWorkGoal hp && hp.getMachine() == this);
        if (level != null && (Workers.isClaimedBy(mob, level, worldPosition) || !Workers.hasClaim(mob))) {
            mob.clearRestriction();
            Workers.unclaim(mob, level, worldPosition);
        }
    }

    /**
     * Forgets the worker, the lead the machine was holding is dropped if {@code dropLead}.
     */
    public void releaseWorker(boolean dropLead) {
        if (level == null || level.isClientSide)
            return;
        boolean had = worker != null || workerUUID != null;
        if (worker != null && !worker.isRemoved())
            detachFromMob(worker);
        worker = null;
        workerUUID = null;
        hasWorker = false;
        if (had) {
            if (dropLead)
                dropLead();
            markDirty();
        }
    }

    private void dropLead() {
        if (level != null && !level.isClientSide)
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), new ItemStack(Items.LEAD));
    }

    /**
     * Called by the block when it is removed (broken, exploded, replaced).
     */
    public void onMachineRemoved() {
        releaseWorker(true);
    }

    public boolean hasWorker() {
        if (level == null)
            return false;
        if (level.isClientSide)
            return getWorker() != null;

        if (worker != null) {
            if (worker.isRemoved()) {
                Entity.RemovalReason reason = worker.getRemovalReason();
                if (reason != null && !reason.shouldDestroy() && reason != Entity.RemovalReason.CHANGED_DIMENSION) {
                    // Only unloaded with its chunk: keep the UUID and find it again when it comes back
                    worker = null;
                    hasWorker = false;
                    return false;
                }
                releaseWorker(true);
                return false;
            }
            if (worker.isAlive() && !worker.isLeashed() && worker.distanceToSqr(machineCenter()) < MAX_WORKER_DISTANCE_SQ
                    && Workers.isClaimedBy(worker, level, worldPosition)) {
                hasWorker = true;
                return true;
            }
            releaseWorker(true);
            return false;
        }
        hasWorker = false;
        return false;
    }

    /**
     * True when there is a lead stored in the machine, also when the worker itself is currently unloaded.
     */
    public boolean hasStoredWorker() {
        return workerUUID != null;
    }

    @Nullable
    public PathfinderMob getWorker() {
        if (level != null && level.isClientSide) {
            if (clientWorkerId < 0)
                return null;
            Entity entity = level.getEntity(clientWorkerId);
            return entity instanceof PathfinderMob mob && !entity.isRemoved() ? mob : null;
        }
        return worker;
    }

    public boolean isValid() {
        return valid;
    }

    /**
     * If the worker should currently be walking its path.
     */
    public boolean isWorking() {
        return valid && running && worker != null && !worker.isRemoved();
    }

    private Vec3 getPathPosition(int i) {
        double x = worldPosition.getX() + PATH[i][0] * 2;
        double y = worldPosition.getY() + getPositionOffset();
        double z = worldPosition.getZ() + PATH[i][1] * 2;
        return new Vec3(x, y, z);
    }

    @Nullable
    public Vec3 getCurrentTargetPosition() {
        if (target < 0 || target >= PATH.length)
            return null;
        return getPathPosition(target);
    }

    private AABB getSearchArea(int index) {
        if (searchAreas[index] == null) {
            Vec3 pos = getPathPosition(index);
            searchAreas[index] = new AABB(pos.x - 0.5D, pos.y - 0.5D, pos.z - 0.5D, pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D);
        }
        return searchAreas[index];
    }

    /**
     * If the worker touches the current path point.
     */
    public boolean isWorkerAtTarget() {
        return worker != null && target >= 0 && target < PATH.length && worker.getBoundingBox().intersects(getSearchArea(target));
    }

    protected int getClosestTarget() {
        if (hasWorker() && worker != null) {
            double dist = Double.MAX_VALUE;
            int closest = 0;
            for (int i = 0; i < PATH.length; i++) {
                double tmp = worker.position().distanceToSqr(getPathPosition(i));
                if (tmp < dist) {
                    dist = tmp;
                    closest = i;
                }
            }
            return closest;
        }
        return 0;
    }

    /**
     * Big enough for the area preview, the name plates and the leash to the worker.
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(SEARCH_RANGE + 1);
    }

    public void clientTick() {
        validate();
    }

    private void validate() {
        validationTimer--;
        if (validationTimer <= 0) {
            valid = validateArea();
            validationTimer = valid ? 220 : 60;
        }
    }

    public void serverTick() {
        validate();
        boolean flag = false;

        boolean has = hasWorker();
        if (!has)
            locateHorseTimer--;
        if (!has && workerUUID != null && locateHorseTimer <= 0)
            flag = findWorker();
        if (locateHorseTimer <= 0)
            locateHorseTimer = 20;

        if (valid) {
            if (!running && canWork())
                running = true;
            else if (running && !canWork())
                running = false;

            if (running != wasRunning) {
                target = getClosestTarget();
                wasRunning = running;
            }

            if (hasWorker() && worker != null && running) {
                if (target < 0 || target >= PATH.length)
                    target = getClosestTarget();

                if (isWorkerAtTarget()) {
                    int next = target + 1;
                    int previous = target - 1;
                    if (next >= PATH.length)
                        next = 0;
                    if (previous < 0)
                        previous = PATH.length - 1;

                    if (origin != target && target != previous) {
                        origin = target;
                        flag = targetReached();
                    }
                    target = next;
                }

                if (worker instanceof AbstractHorse horse && horse.isEating())
                    horse.setEating(false);
            }
        }

        if (flag)
            markDirty();
    }
}
