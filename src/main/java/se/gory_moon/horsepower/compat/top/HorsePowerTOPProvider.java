package se.gory_moon.horsepower.compat.top;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.blockentity.ChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.GrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.HPBaseBlockEntity;
import se.gory_moon.horsepower.blockentity.ManualChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.PressBlockEntity;
import se.gory_moon.horsepower.blocks.FillerBlock;
import se.gory_moon.horsepower.util.Localization;

import java.util.function.Function;

/**
 * Port of the 1.12.2 {@code IProbeInfoAccessor} implementations of the machines (and the filler forwarding to them).
 * Registered through IMC only when The One Probe is installed.
 */
public class HorsePowerTOPProvider implements IProbeInfoProvider, Function<ITheOneProbe, Void> {

    private static final ResourceLocation ID = new ResourceLocation(HorsePower.MODID, "machines");

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerProvider(this);
        return null;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    private static void progress(IProbeInfo info, String prefixKey, double current, double total) {
        long value = (long) ((current / Math.max(total, 1D)) * 100L);
        info.progress(value, 100L, info.defaultProgressStyle()
                .prefix(Component.translatable(prefixKey).append(" "))
                .suffix(Component.literal("%")));
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState state, IProbeHitData data) {
        BlockPos pos = data.getPos();
        if (state.getBlock() instanceof FillerBlock)
            pos = FillerBlock.getMainPos(state, pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HPBaseBlockEntity te))
            return;

        if (te instanceof HandGrindstoneBlockEntity || te instanceof GrindstoneBlockEntity) {
            progress(info, Localization.TOP_GRINDSTONE_PROGRESS, te.getField(1), te.getField(0));
        } else if (te instanceof ChopperBlockEntity chopper) {
            progress(info, Localization.TOP_WINDUP_PROGRESS, chopper.getField(2), chopper.getPointsForWindup());
            if (chopper.getField(0) > 1)
                progress(info, Localization.TOP_CHOPPING_PROGRESS, chopper.getField(1), chopper.getField(0));
        } else if (te instanceof ManualChopperBlockEntity chopper) {
            progress(info, Localization.TOP_CHOPPING_PROGRESS, chopper.getField(1), chopper.getField(0));
        } else if (te instanceof PressBlockEntity press) {
            progress(info, Localization.TOP_PRESS_PROGRESS, press.getField(0), press.getPointsForPress());
        }
    }
}
