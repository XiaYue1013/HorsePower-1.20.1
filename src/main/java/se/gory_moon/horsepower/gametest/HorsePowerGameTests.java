package se.gory_moon.horsepower.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.blockentity.ChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.GrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.ManualChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.PressBlockEntity;
import se.gory_moon.horsepower.blocks.FillerBlock;
import se.gory_moon.horsepower.entity.HorsePowerWorkGoal;
import se.gory_moon.horsepower.entity.Workers;
import se.gory_moon.horsepower.recipes.VanillaRecipeRemover;
import se.gory_moon.horsepower.util.ChoppingTexture;

import java.util.List;

/**
 * In-game verification of the machines, run with {@code gradlew runGameTestServer}.
 * The arena is an 11x5x11 structure with a stone floor; GameTest places it one block up so the floor is at
 * relative y=1 and machines stand on it in the center (5, 2, 5).
 */
@GameTestHolder(HorsePower.MODID)
@PrefixGameTestTemplate(false)
public class HorsePowerGameTests {

    private static final String ARENA = "arena";
    private static final BlockPos CENTER = new BlockPos(5, 2, 5);
    private static final BlockPos GRINDSTONE = new BlockPos(5, 3, 5);

    // ---------------------------------------------------------------- helpers

    private static <T extends BlockEntity> T be(GameTestHelper helper, BlockPos pos, Class<T> type) {
        BlockEntity be = helper.getBlockEntity(pos);
        if (!type.isInstance(be))
            throw new IllegalStateException("Expected " + type.getSimpleName() + " at " + pos + " but was " + be);
        return type.cast(be);
    }

    private static int countItems(GameTestHelper helper, Item item) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(11, 7, 11).inflate(2);
        List<ItemEntity> entities = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item));
        return entities.stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static Horse spawnHorse(GameTestHelper helper, BlockPos relative) {
        Horse horse = helper.spawn(EntityType.HORSE, relative);
        horse.setAge(0);
        return horse;
    }

    private static GrindstoneBlockEntity placeHorseGrindstone(GameTestHelper helper) {
        helper.setBlock(CENTER, Blocks.STONE);
        helper.setBlock(GRINDSTONE, Registration.GRINDSTONE.get());
        return be(helper, GRINDSTONE, GrindstoneBlockEntity.class);
    }

    /**
     * Fails if the worker ever moves more than a block in a single tick (no teleporting).
     */
    private static void trackMovement(GameTestHelper helper, Entity worker, Vec3[] last) {
        Vec3 now = worker.position();
        if (last[0] != null && last[0].distanceTo(now) > 1.0)
            helper.fail("Worker moved " + last[0].distanceTo(now) + " blocks in one tick", worker);
        last[0] = now;
    }

    private static String debug(GameTestHelper helper, se.gory_moon.horsepower.blockentity.HPHorseBlockEntity be, Horse horse) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        Vec3 target = be.getCurrentTargetPosition();
        var path = horse.getNavigation().getPath();
        return " | horse " + horse.position().subtract(Vec3.atLowerCornerOf(origin)) + " target " + (target == null ? null : target.subtract(Vec3.atLowerCornerOf(origin)))
                + " navDone " + horse.getNavigation().isDone() + " path " + (path == null ? null : path.getTarget().subtract(origin) + " reached " + path.canReach())
                + " goals " + horse.goalSelector.getRunningGoals().map(g -> g.getGoal().getClass().getSimpleName()).toList()
                + " immobile " + horse.isImmobile() + " standing " + horse.isStanding();
    }

    // ---------------------------------------------------------------- manual machines

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void handGrindstoneGrindsWheat(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.HAND_GRINDSTONE.get());
        HandGrindstoneBlockEntity be = be(helper, CENTER, HandGrindstoneBlockEntity.class);
        ItemStack rest = be.getInventory().insertItem(0, new ItemStack(Items.WHEAT, 2), false);
        helper.assertTrue(rest.isEmpty(), "Wheat should be accepted");
        helper.assertTrue(be.getInventory().insertItem(0, new ItemStack(Items.STONE), false).getCount() == 1, "Stone has no recipe and must be rejected");

        helper.succeedWhen(() -> {
            be.turn(); // a player clicking every tick; only accepted when the previous rotation is done
            ItemStack out = be.getStackInSlot(1);
            helper.assertTrue(out.is(Registration.FLOUR.get()) && out.getCount() == 2, "Expected 2 flour, got " + out);
            helper.assertTrue(be.getStackInSlot(0).isEmpty(), "Input should be used up");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void handGrindstoneNeedsTurning(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.HAND_GRINDSTONE.get());
        HandGrindstoneBlockEntity be = be(helper, CENTER, HandGrindstoneBlockEntity.class);
        be.getInventory().insertItem(0, new ItemStack(Items.WHEAT), false);
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(be.getStackInSlot(1).isEmpty(), "Nothing may be ground without turning the crank");
            helper.assertTrue(be.getStackInSlot(0).getCount() == 1, "Input must stay");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void choppingBlockChopsWithAxe(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.CHOPPING_BLOCK.get());
        ManualChopperBlockEntity be = be(helper, CENTER, ManualChopperBlockEntity.class);
        helper.assertTrue(be.getInventory().insertItem(0, new ItemStack(Items.OAK_LOG, 2), false).getCount() == 1, "Only one log fits");

        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(CENTER.above())));
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        BlockPos abs = helper.absolutePos(CENTER);
        BlockState state = helper.getLevel().getBlockState(abs);

        // Not separated: horse recipe time 1 x choppMultiplier 4 = 4 chops
        int chops = 1 * Configs.general.choppMultiplier.get();
        helper.assertTrue(state.getDestroyProgress(player, helper.getLevel(), abs) == 0F, "Can't be broken with an axe while it can chop");
        for (int i = 0; i < chops - 1; i++)
            state.attack(helper.getLevel(), abs, player);
        helper.assertTrue(be.getStackInSlot(0).is(Items.OAK_LOG), "Log must still be there before the last chop");
        state.attack(helper.getLevel(), abs, player);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(be.getStackInSlot(0).isEmpty(), "Log should be chopped");
            int planks = countItems(helper, Items.OAK_PLANKS);
            // Iron axe = harvest level 2: 75% of 4 rounded up = 3, +1 with 25% chance
            helper.assertTrue(planks == 3 || planks == 4, "Expected 3-4 planks, got " + planks);
            helper.assertTrue(axe.getDamageValue() == 1, "Axe should take 1 damage per chopped item, was " + axe.getDamageValue());
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- horse powered machines

    @GameTest(template = ARENA, timeoutTicks = 1600)
    public static void horseGrindstoneIsPoweredByWalkingHorse(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        be.getInventory().insertItem(0, new ItemStack(Items.WHEAT, 2), false);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        helper.assertTrue(horse.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof HorsePowerWorkGoal), "Work goal should be added");
        Vec3[] last = new Vec3[1];

        helper.succeedWhen(() -> {
            trackMovement(helper, horse, last);
            helper.assertTrue(be.isValid(), "Area should be valid");
            ItemStack out = be.getStackInSlot(1);
            helper.assertTrue(out.is(Registration.FLOUR.get()) && out.getCount() == 2, "Expected 2 flour, got " + out + " progress " + be.getField(1) + "/" + be.getField(0) + debug(helper, be, horse));
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(GRINDSTONE)).getValue(se.gory_moon.horsepower.blocks.GrindstoneBlock.FILLED), "Grindstone should show the filled layer");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void horseGrindstoneDoesNothingWithoutHorse(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        be.getInventory().insertItem(0, new ItemStack(Items.WHEAT, 2), false);
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(be.getField(1) == 0 && be.getStackInSlot(1).isEmpty(), "No progress without a worker");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void horseGrindstoneInvalidAreaStopsWork(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        helper.setBlock(new BlockPos(2, 2, 5), Blocks.OAK_LOG); // blocks the path
        be.getInventory().insertItem(0, new ItemStack(Items.WHEAT, 2), false);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        helper.runAfterDelay(180, () -> {
            helper.assertFalse(be.isValid(), "Area must be invalid");
            helper.assertTrue(be.getField(1) == 0 && be.getStackInSlot(1).isEmpty(), "No progress with an invalid area");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1200)
    public static void chopperChopsLogs(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.CHOPPER.get());
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(CENTER.above())).getBlock() instanceof FillerBlock, "Filler must be placed above");
        ChopperBlockEntity be = be(helper, CENTER, ChopperBlockEntity.class);
        IItemHandler top = helper.getBlockEntity(CENTER.above()).getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(IllegalStateException::new);
        helper.assertTrue(top.insertItem(0, new ItemStack(Items.SPRUCE_LOG), false).isEmpty(), "Log inserted through the filler");
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        Vec3[] last = new Vec3[1];

        helper.succeedWhen(() -> {
            trackMovement(helper, horse, last);
            ItemStack out = be.getStackInSlot(1);
            helper.assertTrue(out.is(Items.SPRUCE_PLANKS) && out.getCount() == 4, "Expected 4 spruce planks, got " + out + " windup " + be.getField(2));
            IItemHandler bottom = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(IllegalStateException::new);
            helper.assertTrue(bottom.extractItem(0, 64, true).getCount() == 4, "Output extractable from below");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1600)
    public static void pressMakesDirt(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.PRESS.get());
        PressBlockEntity be = be(helper, CENTER, PressBlockEntity.class);
        ItemStack rest = be.getInventory().insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 20), false);
        helper.assertTrue(rest.getCount() == 8, "Only the recipe amount (12) fits, rest " + rest);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);

        helper.succeedWhen(() -> {
            ItemStack out = be.getStackInSlot(1);
            helper.assertTrue(out.is(Items.DIRT) && out.getCount() == 1, "Expected dirt, got " + out + " press " + be.getField(0));
            helper.assertTrue(be.getStackInSlot(0).isEmpty(), "Seeds used up");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1600)
    public static void pressMakesWaterAndOnlyDrains(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.PRESS.get());
        PressBlockEntity be = be(helper, CENTER, PressBlockEntity.class);
        be.getInventory().insertItem(0, new ItemStack(Items.OAK_LEAVES, 8), false);
        IFluidHandler tank = be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).orElseThrow(IllegalStateException::new);
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE) == 0, "Tank can't be filled from outside");
        helper.assertFalse(be.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.NORTH).isPresent(), "No fluid access from the sides");
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);

        helper.succeedWhen(() -> {
            helper.assertTrue(be.getTank().getFluidAmount() == 1000, "Expected 1000mb water, got " + be.getTank().getFluidAmount());
            helper.assertTrue(be.getStackInSlot(0).isEmpty(), "Leaves used up");
            FluidStack drained = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(drained.getFluid() == Fluids.WATER && drained.getAmount() == 1000, "Drain 1000mb");
            helper.assertTrue(be.getTank().isEmpty(), "Tank empty after draining");
        });
    }

    // ---------------------------------------------------------------- worker lifecycle and duplication safety

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void workerDeathReturnsLeadOnce(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        helper.runAfterDelay(5, horse::kill);
        helper.runAfterDelay(30, () -> {
            helper.assertFalse(be.hasWorker(), "Worker must be gone");
            helper.assertFalse(be.hasStoredWorker(), "Stored worker must be cleared");
            helper.assertTrue(countItems(helper, Items.LEAD) == 1, "Exactly one lead must drop, got " + countItems(helper, Items.LEAD));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void breakingMachineReleasesWorker(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        be.getInventory().insertItem(0, new ItemStack(Items.WHEAT, 5), false);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        helper.runAfterDelay(5, () -> helper.destroyBlock(GRINDSTONE));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(countItems(helper, Items.LEAD) == 1, "Exactly one lead must drop, got " + countItems(helper, Items.LEAD));
            helper.assertTrue(countItems(helper, Items.WHEAT) == 5, "Inventory must drop, got " + countItems(helper, Items.WHEAT));
            helper.assertFalse(horse.hasRestriction(), "Horse must be free to wander again");
            helper.assertFalse(Workers.hasClaim(horse), "Claim must be removed");
            helper.assertFalse(horse.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof HorsePowerWorkGoal), "Work goal must be removed");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void leashingWorkerAwayReturnsMachineLead(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3))));
        helper.runAfterDelay(5, () -> horse.setLeashedTo(player, true));
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(be.hasWorker(), "Leashed worker must be released");
            helper.assertTrue(countItems(helper, Items.LEAD) == 1, "The machine's lead must drop, got " + countItems(helper, Items.LEAD));
            helper.assertTrue(horse.isLeashed(), "Horse stays on the player's lead");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void workerReturnedToPlayer(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3))));
        helper.runAfterDelay(5, () -> be.setWorkerToPlayer(player));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(horse.isLeashed() && horse.getLeashHolder() == player, "Horse must be leashed to the player");
            helper.assertFalse(be.hasWorker(), "Machine has no worker");
            helper.assertTrue(countItems(helper, Items.LEAD) == 0, "No lead item may drop (it is on the horse)");
            helper.assertFalse(Workers.hasClaim(horse), "Claim removed");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void workerCanNotPowerTwoMachines(GameTestHelper helper) {
        GrindstoneBlockEntity a = placeHorseGrindstone(helper);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 3, 1), Registration.GRINDSTONE.get());
        GrindstoneBlockEntity b = be(helper, new BlockPos(1, 3, 1), GrindstoneBlockEntity.class);
        Horse horse = spawnHorse(helper, new BlockPos(3, 2, 3));
        a.setWorker(horse);
        helper.runAfterDelay(5, () -> b.setWorker(horse));
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(a.hasWorker(), "First machine must lose the worker");
            helper.assertTrue(b.hasWorker(), "Second machine owns the worker");
            helper.assertTrue(Workers.isClaimedBy(horse, helper.getLevel(), helper.absolutePos(new BlockPos(1, 3, 1))), "Claimed by the second machine");
            helper.assertTrue(countItems(helper, Items.LEAD) == 1, "First machine returns its lead, got " + countItems(helper, Items.LEAD));
            long goals = horse.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof HorsePowerWorkGoal).count();
            helper.assertTrue(goals == 1, "Exactly one work goal, got " + goals);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void unloadedWorkerIsKept(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        helper.runAfterDelay(5, () -> horse.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK));
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(be.hasWorker(), "Unloaded worker is not active");
            helper.assertTrue(be.hasStoredWorker(), "But it is remembered to be found again");
            helper.assertTrue(countItems(helper, Items.LEAD) == 0, "No lead may drop for an unloaded worker");
            CompoundTag saved = be.saveWithoutMetadata();
            helper.assertTrue(saved.getBoolean("hasWorker") && saved.getCompound("leash").hasUUID("UUID")
                    && saved.getCompound("leash").getUUID("UUID").equals(horse.getUUID()), "Worker UUID is saved");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void workerFoundAgainAfterMachineReload(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        be.getInventory().insertItem(0, new ItemStack(Items.WHEAT, 3), false);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        helper.runAfterDelay(5, () -> {
            // Simulates a chunk unload + load of the machine: a fresh block entity from the saved data
            CompoundTag saved = be.saveWithFullMetadata();
            BlockPos abs = helper.absolutePos(GRINDSTONE);
            BlockEntity loaded = BlockEntity.loadStatic(abs, helper.getLevel().getBlockState(abs), saved);
            helper.assertTrue(loaded instanceof GrindstoneBlockEntity, "Reloaded block entity");
            helper.getLevel().setBlockEntity(loaded);
        });
        helper.runAfterDelay(40, () -> {
            GrindstoneBlockEntity reloaded = be(helper, GRINDSTONE, GrindstoneBlockEntity.class);
            helper.assertTrue(reloaded != be, "New block entity instance");
            helper.assertTrue(reloaded.getStackInSlot(0).getCount() == 3, "Inventory restored");
            helper.assertTrue(reloaded.hasWorker() && reloaded.getWorker() == horse, "Worker found again by UUID");
            helper.assertTrue(countItems(helper, Items.LEAD) == 0, "No lead dropped");
            long goals = horse.goalSelector.getAvailableGoals().stream().filter(g -> g.getGoal() instanceof HorsePowerWorkGoal).count();
            helper.assertTrue(goals == 1, "Exactly one work goal after reload, got " + goals);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void workerWalkingAwayIsReleased(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        be.setWorker(horse);
        helper.runAfterDelay(5, () -> horse.teleportTo(horse.getX() + 30, horse.getY(), horse.getZ()));
        helper.runAfterDelay(15, () -> {
            helper.assertFalse(be.hasWorker(), "Too far away worker is released");
            helper.assertTrue(countItems(helper, Items.LEAD) == 1, "Lead dropped once, got " + countItems(helper, Items.LEAD));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void attachAndReturnWorkerByRightClick(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        Horse horse = spawnHorse(helper, new BlockPos(2, 2, 2));
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3))));
        horse.setLeashedTo(player, true);
        helper.runAfterDelay(2, () -> helper.useBlock(GRINDSTONE, player));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.hasWorker() && be.getWorker() == horse, "Right click with a leashed horse attaches it");
            helper.assertFalse(horse.isLeashed(), "The lead moved into the machine");
            helper.assertTrue(countItems(helper, Items.LEAD) == 0, "No lead item may appear when attaching");
            helper.useBlock(GRINDSTONE, player); // empty hand, nothing inside: worker goes back to the player
        });
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(be.hasWorker(), "Worker returned");
            helper.assertTrue(horse.isLeashed() && horse.getLeashHolder() == player, "Horse back on the player's lead");
            helper.assertTrue(countItems(helper, Items.LEAD) == 0, "Still no loose lead");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void rightClickInsertsAndTakesItems(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3))));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 10));
        helper.useBlock(GRINDSTONE, player);
        helper.assertTrue(be.getStackInSlot(0).getCount() == 10 && player.getMainHandItem().isEmpty(), "Held wheat inserted");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 3));
        helper.useBlock(GRINDSTONE, player);
        helper.assertTrue(player.getMainHandItem().getCount() == 3 && be.getStackInSlot(0).is(Items.WHEAT), "Invalid items are not inserted");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.getInventory().selected = 8; // taken items go to the first free slot, keep the hand empty
        be.setInventorySlotContents(1, new ItemStack(Registration.FLOUR.get(), 4));
        helper.useBlock(GRINDSTONE, player);
        helper.assertTrue(player.getInventory().countItem(Registration.FLOUR.get()) == 4 && be.getStackInSlot(1).isEmpty(), "Output taken first");
        helper.useBlock(GRINDSTONE, player);
        helper.assertTrue(player.getInventory().countItem(Items.WHEAT) == 10 && be.getStackInSlot(0).isEmpty(), "Then the input with an empty hand");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void handGrindstoneByRightClicks(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.HAND_GRINDSTONE.get());
        HandGrindstoneBlockEntity be = be(helper, CENTER, HandGrindstoneBlockEntity.class);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(5, 2, 3))));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
        helper.useBlock(CENTER, player);
        helper.assertTrue(be.getStackInSlot(0).is(Items.BONE), "Bone inserted by right click");
        helper.succeedWhen(() -> {
            helper.useBlock(CENTER, player); // keep cranking
            helper.assertTrue(be.getStackInSlot(1).is(Items.BONE_MEAL) && be.getStackInSlot(1).getCount() == 3, "3 bone meal from a bone, got " + be.getStackInSlot(1));
            helper.assertTrue(player.getFoodData().getExhaustionLevel() > 0, "Cranking exhausts the player");
        });
    }

    // ---------------------------------------------------------------- blocks, automation, data

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void removingFillerBreaksMachineWithoutDupes(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.CHOPPER.get());
        ChopperBlockEntity be = be(helper, CENTER, ChopperBlockEntity.class);
        be.getTextureData().setTag(new ItemStack(Items.BIRCH_LOG).save(new CompoundTag()));
        be.getInventory().insertItem(0, new ItemStack(Items.OAK_LOG), false);
        helper.setBlock(CENTER.above(), Blocks.AIR);
        helper.runAfterDelay(2, () -> {
            helper.assertBlockPresent(Blocks.AIR, CENTER);
            AABB box = new AABB(helper.absolutePos(CENTER)).inflate(3);
            List<ItemEntity> choppers = helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(Registration.CHOPPER_ITEM.get()));
            helper.assertTrue(choppers.size() == 1 && choppers.get(0).getItem().getCount() == 1, "Exactly one chopper drops, got " + choppers.size());
            ItemStack drop = choppers.get(0).getItem();
            helper.assertTrue(ChoppingTexture.getTextureStack(drop).is(Items.BIRCH_LOG), "Drop keeps its birch texture");
            helper.assertTrue(countItems(helper, Items.OAK_LOG) == 1, "Inventory dropped once");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void automationRules(GameTestHelper helper) {
        GrindstoneBlockEntity be = placeHorseGrindstone(helper);
        IItemHandler side = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).orElseThrow(IllegalStateException::new);
        IItemHandler bottom = be.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElseThrow(IllegalStateException::new);
        IItemHandler all = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElseThrow(IllegalStateException::new);

        helper.assertTrue(side.getSlots() == 1 && bottom.getSlots() == 2 && all.getSlots() == 3, "Slot layout");
        helper.assertTrue(side.insertItem(0, new ItemStack(Items.DIRT), false).getCount() == 1, "Invalid input rejected");
        helper.assertTrue(all.insertItem(1, new ItemStack(Registration.FLOUR.get()), false).getCount() == 1, "Outputs can't be filled from outside");
        helper.assertTrue(side.insertItem(0, new ItemStack(Items.WHEAT, 64), false).isEmpty(), "Valid input accepted");
        helper.assertTrue(side.insertItem(0, new ItemStack(Items.WHEAT, 1), true).getCount() == 1, "Full stack rejects more");
        helper.assertTrue(bottom.extractItem(0, 64, false).isEmpty(), "Nothing to extract from empty outputs");

        helper.setBlock(new BlockPos(1, 2, 1), Registration.PRESS.get());
        PressBlockEntity press = be(helper, new BlockPos(1, 2, 1), PressBlockEntity.class);
        IItemHandler pressSide = press.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.EAST).orElseThrow(IllegalStateException::new);
        helper.assertTrue(pressSide.insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 64), false).getCount() == 52, "Press input limited to recipe amount");
        press.setField(0, 3);
        helper.assertTrue(press.getInventory().extractItem(0, 64, false).isEmpty(), "Input locked while pressing");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void machineDataSurvivesSaveAndLoad(GameTestHelper helper) {
        helper.setBlock(CENTER, Registration.PRESS.get());
        PressBlockEntity press = be(helper, CENTER, PressBlockEntity.class);
        press.getInventory().insertItem(0, new ItemStack(Items.OAK_LEAVES, 8), false);
        press.setField(0, 5);
        CompoundTag saved = press.saveWithFullMetadata();
        BlockPos abs = helper.absolutePos(CENTER);
        BlockEntity loaded = BlockEntity.loadStatic(abs, helper.getLevel().getBlockState(abs), saved);
        helper.assertTrue(loaded instanceof PressBlockEntity, "Press reloaded");
        PressBlockEntity copy = (PressBlockEntity) loaded;
        helper.assertTrue(copy.getStackInSlot(0).getCount() == 8 && copy.getField(0) == 5, "Press items and progress restored");

        helper.setBlock(new BlockPos(1, 2, 1), Registration.HAND_GRINDSTONE.get());
        HandGrindstoneBlockEntity hand = be(helper, new BlockPos(1, 2, 1), HandGrindstoneBlockEntity.class);
        hand.getInventory().insertItem(0, new ItemStack(Items.BONE, 3), false);
        hand.setField(1, 6);
        HandGrindstoneBlockEntity handCopy = (HandGrindstoneBlockEntity) BlockEntity.loadStatic(hand.getBlockPos(), hand.getBlockState(), hand.saveWithFullMetadata());
        helper.assertTrue(handCopy != null && handCopy.getStackInSlot(0).getCount() == 3 && handCopy.getField(1) == 6 && handCopy.getField(0) == 12, "Hand grindstone progress restored");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void recipesAreLoaded(GameTestHelper helper) {
        RecipeManager manager = helper.getLevel().getRecipeManager();
        int grinding = manager.getAllRecipesFor(Registration.GRINDING_TYPE.get()).size();
        int chopping = manager.getAllRecipesFor(Registration.CHOPPING_TYPE.get()).size();
        int manual = manager.getAllRecipesFor(Registration.MANUAL_CHOPPING_TYPE.get()).size();
        int pressing = manager.getAllRecipesFor(Registration.PRESSING_TYPE.get()).size();
        helper.assertTrue(grinding == 19, "19 grinding recipes (2.6.4 defaults), got " + grinding);
        helper.assertTrue(chopping == 10 && manual == 10, "10 chopping recipes, got " + chopping + "/" + manual);
        helper.assertTrue(pressing == 2, "2 pressing recipes, got " + pressing);
        helper.assertTrue(manager.byKey(new net.minecraft.resources.ResourceLocation(HorsePower.MODID, "chopper")).isPresent(), "Chopper crafting recipe");
        helper.assertTrue(manager.byKey(new net.minecraft.resources.ResourceLocation(HorsePower.MODID, "manual_chopping")).isPresent(), "Chopping block crafting recipe");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void vanillaRecipeRemoval(GameTestHelper helper) {
        RecipeManager copy = new RecipeManager(ICondition.IContext.EMPTY);
        copy.replaceRecipes(helper.getLevel().getRecipeManager().getRecipes());
        net.minecraft.resources.ResourceLocation bread = new net.minecraft.resources.ResourceLocation("minecraft", "bread");
        helper.assertTrue(copy.byKey(bread).isPresent(), "Bread recipe present by default");
        VanillaRecipeRemover.removeFrom(copy);
        helper.assertFalse(copy.byKey(bread).isPresent(), "Bread recipe removed");
        helper.assertFalse(copy.byKey(new net.minecraft.resources.ResourceLocation("minecraft", "bone_meal")).isPresent(), "Bone meal recipe removed");
        helper.assertTrue(copy.byKey(new net.minecraft.resources.ResourceLocation("minecraft", "oak_planks")).isPresent(), "Unrelated recipes kept");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void onlyValidMobsCanWork(GameTestHelper helper) {
        helper.assertTrue(Workers.isValidWorker(spawnHorse(helper, new BlockPos(2, 2, 2))), "Horse is a valid worker");
        helper.assertTrue(Workers.isValidWorker(helper.spawn(EntityType.DONKEY, new BlockPos(3, 2, 2))), "Donkey is a valid worker");
        helper.assertFalse(Workers.isValidWorker(helper.spawn(EntityType.COW, new BlockPos(4, 2, 2))), "Cow is not valid by default");
        helper.succeed();
    }
}
