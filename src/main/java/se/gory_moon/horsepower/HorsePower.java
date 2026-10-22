package se.gory_moon.horsepower;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import se.gory_moon.horsepower.advancements.HPTriggers;
import se.gory_moon.horsepower.compat.top.HorsePowerTOPProvider;
import se.gory_moon.horsepower.recipes.ConfigCondition;
import se.gory_moon.horsepower.recipes.VanillaRecipeRemover;

/**
 * Horse Power, originally by Gory_Moon (LGPL v3 or later), ported from 1.12.2 (2.6.4) to Forge 1.20.1.
 */
@Mod(HorsePower.MODID)
public class HorsePower {

    public static final String MODID = "horsepower";
    public static final String NAME = "Horse Power";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HorsePower() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Configs.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Configs.CLIENT_SPEC);

        Registration.register(modBus);
        modBus.addListener(this::onRegister);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::enqueueIMC);

        MinecraftForge.EVENT_BUS.addListener(this::onServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::onDatapackSync);
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.RECIPE_SERIALIZER))
            CraftingHelper.register(ConfigCondition.Serializer.INSTANCE);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(HPTriggers::register);
    }

    private void enqueueIMC(InterModEnqueueEvent event) {
        if (ModList.get().isLoaded("theoneprobe"))
            InterModComms.sendTo("theoneprobe", "getTheOneProbe", HorsePowerTOPProvider::new);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        VanillaRecipeRemover.apply(event.getServer().getRecipeManager());
    }

    /**
     * Fired after every /reload (and on login) right before the recipes are sent to the players.
     */
    private void onDatapackSync(OnDatapackSyncEvent event) {
        VanillaRecipeRemover.apply(event.getPlayerList().getServer().getRecipeManager());
    }
}
