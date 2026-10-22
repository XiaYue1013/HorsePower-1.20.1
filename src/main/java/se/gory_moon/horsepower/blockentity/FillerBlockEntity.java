package se.gory_moon.horsepower.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.blocks.FillerBlock;
import se.gory_moon.horsepower.blocks.HPBaseBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code TileEntityFiller}: the top half of the 2 block high machines.
 * It has no state of its own, capabilities are forwarded to the machine below so pipes and hoppers can use
 * either block.
 */
public class FillerBlockEntity extends BlockEntity {

    public FillerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.FILLER_BE.get(), pos, state);
    }

    public BlockPos getFilledPos() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof FillerBlock))
            return worldPosition;
        return worldPosition.relative(state.getValue(FillerBlock.FACING));
    }

    @Nullable
    public HPBaseBlockEntity getFilledTileEntity() {
        if (level == null)
            return null;
        BlockPos pos = getFilledPos();
        if (pos.equals(worldPosition) || !(level.getBlockState(pos).getBlock() instanceof HPBaseBlock))
            return null;
        return level.getBlockEntity(pos) instanceof HPBaseBlockEntity be ? be : null;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        HPBaseBlockEntity te = getFilledTileEntity();
        if (te != null && !remove)
            return te.getCapability(cap, side);
        return super.getCapability(cap, side);
    }
}
