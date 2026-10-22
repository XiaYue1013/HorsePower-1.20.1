package se.gory_moon.horsepower.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import se.gory_moon.horsepower.blockentity.HPHorseBlockEntity;

import java.util.EnumSet;

/**
 * Drives an attached worker around its machine.
 * <p>
 * The waypoint logic is the 1.12.2 one and lives in {@link HPHorseBlockEntity#serverTick()}: the machine decides which
 * of the 8 path points is the current target and advances it when the worker's bounding box reaches it. This goal
 * only does what the 1.12.2 tile entity did with the navigator ({@code if (noPath) tryMoveToXYZ(target, 1.0)}),
 * but as a proper AI goal with the MOVE flag so wandering goals don't fight the machine for the navigator.
 * Higher priority goals (panic) still interrupt it, like in 1.12.2.
 */
public class HorsePowerWorkGoal extends Goal {

    public static final int PRIORITY = 2;
    private static final double FINAL_APPROACH_SQ = 2.5 * 2.5;

    private final PathfinderMob worker;
    private final HPHorseBlockEntity machine;
    private int repathCooldown = 0;

    public HorsePowerWorkGoal(PathfinderMob worker, HPHorseBlockEntity machine) {
        this.worker = worker;
        this.machine = machine;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public HPHorseBlockEntity getMachine() {
        return machine;
    }

    private boolean shouldWork() {
        return !machine.isRemoved() && machine.getWorker() == worker && machine.isWorking();
    }

    @Override
    public boolean canUse() {
        return shouldWork();
    }

    @Override
    public boolean canContinueToUse() {
        return shouldWork();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        moveToTarget();
    }

    @Override
    public void stop() {
        worker.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }
        if (!worker.getNavigation().isDone())
            return;
        Vec3 target = machine.getCurrentTargetPosition();
        if (target != null && !machine.isWorkerAtTarget() && worker.position().distanceToSqr(target.x, worker.getY(), target.z) < FINAL_APPROACH_SQ) {
            // The 1.20 path follower considers a node reached ~0.7 blocks early for wide mobs; walk the last bit
            // straight to the path point (1.12.2's navigator ended on it).
            worker.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.0D);
        } else {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        Vec3 target = machine.getCurrentTargetPosition();
        if (target == null)
            return;
        // Accuracy 0 like 1.12.2's tryMoveToXYZ: moveTo(x, y, z) would accept a node one block short, which leaves a
        // wide mob like a horse outside the path point's hit box forever.
        Path path = worker.getNavigation().createPath(BlockPos.containing(target), 0);
        // No path (blocked area): don't run the path finder every tick, the machine simply makes no progress.
        if (!worker.getNavigation().moveTo(path, 1.0D))
            repathCooldown = 10;
    }
}
