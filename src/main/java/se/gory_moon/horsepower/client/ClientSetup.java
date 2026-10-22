package se.gory_moon.horsepower.client;

import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.client.model.RetexturedChoppingModel;
import se.gory_moon.horsepower.client.render.MachineRenderers;

import java.util.Map;

/**
 * Client only mod bus setup (port of the 1.12.2 {@code ClientProxy} and {@code ModModelManager}).
 * This class is never loaded on a dedicated server.
 */
@Mod.EventBusSubscriber(modid = HorsePower.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Registration.HAND_GRINDSTONE_BE.get(), MachineRenderers.HandGrindstone::new);
        event.registerBlockEntityRenderer(Registration.GRINDSTONE_BE.get(), MachineRenderers.Grindstone::new);
        event.registerBlockEntityRenderer(Registration.MANUAL_CHOPPER_BE.get(), MachineRenderers.ChoppingBlock::new);
        event.registerBlockEntityRenderer(Registration.CHOPPER_BE.get(), MachineRenderers.Chopper::new);
        event.registerBlockEntityRenderer(Registration.PRESS_BE.get(), MachineRenderers.Press::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(MachineRenderers.HAND_GRINDSTONE_CENTER);
        event.register(MachineRenderers.GRINDSTONE_FILLED);
        event.register(MachineRenderers.CHOPPER_BLADE);
        event.register(MachineRenderers.PRESS_TOP);
    }

    @SubscribeEvent
    public static void modifyModels(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();
        wrapChoppingModels(models, Registration.CHOPPER.get());
        wrapChoppingModels(models, Registration.CHOPPING_BLOCK.get());
    }

    private static void wrapChoppingModels(Map<ResourceLocation, BakedModel> models, Block block) {
        for (BlockState state : block.getStateDefinition().getPossibleStates())
            wrap(models, BlockModelShaper.stateToModelLocation(state));
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id != null)
            wrap(models, new ModelResourceLocation(id, "inventory"));
    }

    private static void wrap(Map<ResourceLocation, BakedModel> models, ModelResourceLocation location) {
        BakedModel model = models.get(location);
        if (model != null && !(model instanceof RetexturedChoppingModel))
            models.put(location, new RetexturedChoppingModel(model));
    }
}
