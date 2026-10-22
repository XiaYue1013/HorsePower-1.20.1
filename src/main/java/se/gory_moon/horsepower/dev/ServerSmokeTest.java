package se.gory_moon.horsepower.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;

/**
 * Development only (excluded from the release jar): with {@code -Dhorsepower.serverSmokeTest=true} a dedicated
 * server logs what Horse Power loaded, runs for 200 ticks and stops itself.
 */
@Mod.EventBusSubscriber(modid = HorsePower.MODID)
public final class ServerSmokeTest {

    private static final boolean ENABLED = Boolean.getBoolean("horsepower.serverSmokeTest");
    private static int ticks = -1;

    private ServerSmokeTest() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!ENABLED)
            return;
        MinecraftServer server = event.getServer();
        var recipes = server.getRecipeManager();
        HorsePower.LOGGER.info("Server smoke test: dedicated={} grinding={} hand_grinding={} chopping={} manual_chopping={} pressing={}",
                server.isDedicatedServer(),
                recipes.getAllRecipesFor(Registration.GRINDING_TYPE.get()).size(),
                recipes.getAllRecipesFor(Registration.HAND_GRINDING_TYPE.get()).size(),
                recipes.getAllRecipesFor(Registration.CHOPPING_TYPE.get()).size(),
                recipes.getAllRecipesFor(Registration.MANUAL_CHOPPING_TYPE.get()).size(),
                recipes.getAllRecipesFor(Registration.PRESSING_TYPE.get()).size());
        ticks = 0;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED || ticks < 0 || event.phase != TickEvent.Phase.END)
            return;
        if (++ticks == 200) {
            HorsePower.LOGGER.info("Server smoke test: ran 200 ticks, stopping");
            event.getServer().halt(false);
        }
    }
}
