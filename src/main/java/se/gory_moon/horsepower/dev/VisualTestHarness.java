package se.gory_moon.horsepower.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import java.lang.reflect.Method;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.blockentity.ChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.GrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.ManualChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.PressBlockEntity;
import se.gory_moon.horsepower.util.ChoppingTexture;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Development only (excluded from the release jar): with {@code -Dhorsepower.visualTest=true} the client creates a
 * superflat world, builds every machine with working horses, takes screenshots into run/screenshots and quits.
 */
@Mod.EventBusSubscriber(modid = HorsePower.MODID, value = Dist.CLIENT)
public final class VisualTestHarness {

    private static final boolean ENABLED = Boolean.getBoolean("horsepower.visualTest");
    private static final String WORLD = "hp_visual_test";
    private static boolean worldRequested = false;
    private static int ticks = -1;
    private static BlockPos base;

    private VisualTestHarness() {}

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (!ENABLED || worldRequested || !(event.getScreen() instanceof TitleScreen))
            return;
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        try {
            Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
            if (Files.exists(dir))
                try (Stream<Path> walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                }
        } catch (Exception e) {
            HorsePower.LOGGER.error("Visual test: could not delete old world", e);
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(42L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (!ENABLED || event.phase != TickEvent.Phase.END)
            return;
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null)
            return;
        ticks++;
        if (mc.screen instanceof PauseScreen)
            mc.setScreen(null);
        mc.getToasts().clear();
        if (ticks == 60)
            server.execute(() -> build(server));
        // Overview, then close ups looking at each machine (within reach so Jade/TOP and the name plates show)
        else if (ticks == 120)
            look(server, new Vec3(10, 10, -10), new Vec3(10, 0, 4), ItemStack.EMPTY);
        else if (ticks == 160)
            shot(mc, "hp_01_overview");
        else if (ticks == 300)
            look(server, new Vec3(-1.5, 2.6, -1.5), new Vec3(0.5, 1.6, 0.5), ItemStack.EMPTY);
        else if (ticks == 340)
            shot(mc, "hp_02_grindstone");
        else if (ticks == 360)
            look(server, new Vec3(13.5, 1.4, -2.5), new Vec3(10.5, 1.0, 0.5), ItemStack.EMPTY);
        else if (ticks == 400)
            shot(mc, "hp_03_chopper");
        else if (ticks == 420)
            look(server, new Vec3(21.5, 2.4, -1.5), new Vec3(20.5, 0.4, 0.5), ItemStack.EMPTY);
        else if (ticks == 460)
            shot(mc, "hp_04_press");
        else if (ticks == 480)
            look(server, new Vec3(2.5, 1.6, 7.5), new Vec3(0.5, 0.6, 10.5), ItemStack.EMPTY);
        else if (ticks == 520)
            shot(mc, "hp_05_hand_grindstone");
        else if (ticks == 530)
            look(server, new Vec3(4.5, 2.4, 7.5), new Vec3(4.5, 0.3, 10.5), ItemStack.EMPTY);
        else if (ticks == 560)
            shot(mc, "hp_05b_chopping_blocks");
        else if (ticks == 570)
            look(server, new Vec3(30.5, 2.8, -3.5), new Vec3(30.5, 1.5, 0.5), new ItemStack(Items.LEAD));
        else if (ticks == 610)
            shot(mc, "hp_06_invalid_area");
        else if (ticks == 620)
            look(server, new Vec3(42.5, 1.5, -1.5), new Vec3(42.5, 0, 1.5), new ItemStack(Registration.GRINDSTONE_ITEM.get()));
        else if (ticks == 660)
            shot(mc, "hp_07_placement_preview");
        else if (ticks == 665)
            openCreativeTab(mc);
        else if (ticks == 675) {
            shot(mc, "hp_08_creative_tab");
            mc.setScreen(null);
        } else if (ticks >= 680 && ModList.get().isLoaded("jei") && ticks < 680 + JeiShots.STEPS * 40) {
            int step = (ticks - 680) / 40;
            if ((ticks - 680) % 40 == 0)
                JeiShots.show(step);
            else if ((ticks - 680) % 40 == 30)
                shot(mc, "hp_jei_" + step);
        } else if (ticks == 680 + (ModList.get().isLoaded("jei") ? JeiShots.STEPS * 40 : 0)) {
            HorsePower.LOGGER.info("Visual test: done, stopping the client");
            mc.stop();
        }
    }

    /**
     * Opens the Horse Power creative tab and logs its entries (selectTab is private, this is a dev tool only).
     */
    private static void openCreativeTab(Minecraft mc) {
        var screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), mc.options.operatorItemsTab().get());
        mc.setScreen(screen);
        CreativeModeTab tab = Registration.TAB.get();
        try {
            Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, tab);
        } catch (ReflectiveOperationException e) {
            HorsePower.LOGGER.error("Visual test: could not select the creative tab", e);
        }
        StringBuilder entries = new StringBuilder();
        for (ItemStack stack : tab.getDisplayItems())
            entries.append("\n  ").append(stack.getItem()).append(stack.hasTag() ? " " + stack.getTag() : "");
        HorsePower.LOGGER.info("Visual test: creative tab has {} entries:{}", tab.getDisplayItems().size(), entries);
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> HorsePower.LOGGER.info("Visual test: {}", msg.getString()));
    }

    /**
     * Puts the player's eyes at {@code eye} looking at {@code target} (both relative to the scene base).
     */
    private static void look(MinecraftServer server, Vec3 eye, Vec3 target, ItemStack held) {
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        view(server, eye.subtract(0, 1.62, 0), yaw, pitch, held);
    }

    private static void view(MinecraftServer server, Vec3 offset, float yaw, float pitch, ItemStack held) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            Vec3 pos = Vec3.atLowerCornerOf(base).add(offset);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.teleportTo(player.serverLevel(), pos.x, pos.y, pos.z, yaw, pitch);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
        });
    }

    private static ItemStack textured(Item block, Item log) {
        return ChoppingTexture.createItemStack(block, 1, new ItemStack(log));
    }

    private static Horse horse(ServerLevel level, BlockPos pos) {
        Horse horse = EntityType.HORSE.create(level);
        horse.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        horse.setAge(0);
        level.addFreshEntity(horse);
        return horse;
    }

    private static void build(MinecraftServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        base = player.blockPosition();
        level.setDayTime(6000);

        // Horse grindstone (on a block, in the middle of a 7x7 area)
        BlockPos grind = base.offset(0, 1, 0);
        level.setBlockAndUpdate(grind.below(), Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(grind, Registration.GRINDSTONE.get().defaultBlockState());
        if (level.getBlockEntity(grind) instanceof GrindstoneBlockEntity be) {
            be.getInventory().insertItem(0, new ItemStack(Items.WHEAT, 32), false);
            be.setWorker(horse(level, grind.offset(-3, -1, -3)));
        }

        // Horse chopping block
        BlockPos chopper = base.offset(10, 0, 0);
        level.setBlockAndUpdate(chopper, Registration.CHOPPER.get().defaultBlockState().setValue(se.gory_moon.horsepower.blocks.ChopperBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(chopper) instanceof ChopperBlockEntity be) {
            be.getTextureData().setTag(new ItemStack(Items.BIRCH_LOG).save(new CompoundTag()));
            be.getInventory().insertItem(0, new ItemStack(Items.SPRUCE_LOG), false);
            be.setWorker(horse(level, chopper.offset(-3, 0, -3)));
            be.markDirty();
        }

        // Horse press with water in the tank
        BlockPos press = base.offset(20, 0, 0);
        level.setBlockAndUpdate(press, Registration.PRESS.get().defaultBlockState());
        if (level.getBlockEntity(press) instanceof PressBlockEntity be) {
            be.getTank().setFluid(new FluidStack(Fluids.WATER, 2000));
            be.setWorker(horse(level, press.offset(-3, 0, -3)));
            be.markDirty();
        }

        // Hand grindstone with input, output and a secondary, and textured chopping blocks with logs on them
        BlockPos hand = base.offset(0, 0, 10);
        level.setBlockAndUpdate(hand, Registration.HAND_GRINDSTONE.get().defaultBlockState());
        if (level.getBlockEntity(hand) instanceof HandGrindstoneBlockEntity be) {
            be.setInventorySlotContents(0, new ItemStack(Items.WHEAT, 5));
            be.setInventorySlotContents(1, new ItemStack(Registration.FLOUR.get(), 3));
            be.setInventorySlotContents(2, new ItemStack(Items.BONE_MEAL, 2));
        }
        Item[] logs = {Items.OAK_LOG, Items.BIRCH_LOG, Items.SPRUCE_LOG, Items.DARK_OAK_LOG, Items.MANGROVE_LOG, Items.CRIMSON_STEM};
        for (int i = 0; i < logs.length; i++) {
            BlockPos pos = base.offset(2 + i, 0, 10);
            level.setBlockAndUpdate(pos, Registration.CHOPPING_BLOCK.get().defaultBlockState());
            if (level.getBlockEntity(pos) instanceof ManualChopperBlockEntity be) {
                be.getTextureData().setTag(new ItemStack(logs[i]).save(new CompoundTag()));
                if (i % 2 == 0)
                    be.getInventory().insertItem(0, new ItemStack(Items.OAK_LOG), false);
                be.markDirty();
            }
        }

        // Invalid area: a grindstone with a tree trunk in its path
        BlockPos invalid = base.offset(30, 1, 0);
        level.setBlockAndUpdate(invalid.below(), Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(invalid, Registration.GRINDSTONE.get().defaultBlockState());
        level.setBlockAndUpdate(invalid.offset(2, -1, 1), Blocks.OAK_LOG.defaultBlockState());
        level.setBlockAndUpdate(invalid.offset(2, 0, 1), Blocks.OAK_LOG.defaultBlockState());

        // Placement preview target with an obstruction
        level.setBlockAndUpdate(base.offset(42, 0, 2), Blocks.OAK_FENCE.defaultBlockState());

        // Hotbar: item models of every block
        ItemStack[] hotbar = {new ItemStack(Registration.HAND_GRINDSTONE_ITEM.get()), new ItemStack(Registration.GRINDSTONE_ITEM.get()),
                textured(Registration.CHOPPING_BLOCK_ITEM.get(), Items.JUNGLE_LOG), textured(Registration.CHOPPER_ITEM.get(), Items.ACACIA_LOG),
                new ItemStack(Registration.PRESS_ITEM.get()), new ItemStack(Registration.FLOUR.get()), new ItemStack(Registration.DOUGH.get()),
                textured(Registration.CHOPPER_ITEM.get(), Items.CHERRY_LOG)};
        for (int i = 0; i < hotbar.length; i++)
            player.getInventory().setItem(i + 1, hotbar[i]);
        HorsePower.LOGGER.info("Visual test: scene built at {}", base);
    }
}
