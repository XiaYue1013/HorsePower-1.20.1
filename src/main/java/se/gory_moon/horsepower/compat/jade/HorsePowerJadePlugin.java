package se.gory_moon.horsepower.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.blockentity.ChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.FillerBlockEntity;
import se.gory_moon.horsepower.blockentity.GrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.HPBaseBlockEntity;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.ManualChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.PressBlockEntity;
import se.gory_moon.horsepower.blocks.FillerBlock;
import se.gory_moon.horsepower.blocks.HPBaseBlock;
import se.gory_moon.horsepower.util.Localization;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Port of the 1.12.2 Hwyla {@code Provider}: shows the grinding, windup, chopping and pressing progress.
 * Inventories and the press tank are shown by Jade's own item/fluid storage providers through the capabilities.
 * Only loaded by Jade.
 */
@WailaPlugin
public class HorsePowerJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID = new ResourceLocation(HorsePower.MODID, "progress");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.INSTANCE, HPBaseBlockEntity.class);
        registration.registerBlockDataProvider(Provider.INSTANCE, FillerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, HPBaseBlock.class);
        registration.registerBlockComponent(Provider.INSTANCE, FillerBlock.class);
        registration.usePickedResult(Registration.CHOPPER_FILLER.get());
        registration.usePickedResult(Registration.PRESS_FILLER.get());
    }

    private static double percent(double current, double total) {
        return Math.round(((current / Math.max(total, 1D)) * 100D) * 100D) / 100D;
    }

    public enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity be = accessor.getBlockEntity();
            if (be instanceof FillerBlockEntity filler)
                be = filler.getFilledTileEntity();
            CompoundTag tag = new CompoundTag();
            if (be instanceof GrindstoneBlockEntity || be instanceof HandGrindstoneBlockEntity) {
                HPBaseBlockEntity te = (HPBaseBlockEntity) be;
                tag.putString("type", "grindstone");
                tag.putInt("total", te.getField(0));
                tag.putInt("current", te.getField(1));
            } else if (be instanceof ChopperBlockEntity chopper) {
                tag.putString("type", "chopper");
                tag.putInt("total", chopper.getField(0));
                tag.putInt("current", chopper.getField(1));
                tag.putInt("windup", chopper.getField(2));
                tag.putInt("totalWindup", chopper.getPointsForWindup());
            } else if (be instanceof ManualChopperBlockEntity chopper) {
                tag.putString("type", "manual_chopper");
                tag.putInt("total", chopper.getField(0));
                tag.putInt("current", chopper.getField(1));
            } else if (be instanceof PressBlockEntity press) {
                tag.putString("type", "press");
                tag.putInt("current", press.getField(0));
                tag.putInt("total", press.getPointsForPress());
            }
            if (!tag.isEmpty())
                data.put(HorsePower.MODID, tag);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData().getCompound(HorsePower.MODID);
            double current = tag.getInt("current");
            double total = tag.getInt("total");
            switch (tag.getString("type")) {
                case "grindstone" -> tooltip.add(line(Localization.WAILA_GRINDSTONE_PROGRESS, percent(current, total)));
                case "chopper" -> {
                    tooltip.add(line(Localization.WAILA_WINDUP_PROGRESS, percent(tag.getInt("windup"), tag.getInt("totalWindup"))));
                    if (total > 1)
                        tooltip.add(line(Localization.WAILA_CHOPPING_PROGRESS, percent(current, total)));
                }
                case "manual_chopper" -> tooltip.add(line(Localization.WAILA_CHOPPING_PROGRESS, percent(current, total)));
                case "press" -> tooltip.add(line(Localization.WAILA_PRESS_PROGRESS, percent(current, total)));
                default -> {}
            }
        }

        private static Component line(String key, double progress) {
            return Component.literal(Localization.translate(key, progress));
        }
    }
}
