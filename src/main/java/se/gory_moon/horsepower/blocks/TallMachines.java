package se.gory_moon.horsepower.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Shared placement/removal rules of the two block high machines (chopper and press) and their filler.
 * 1.12.2 handled this in {@code ItemBlockDouble} and {@code onNeighborChange}.
 */
public final class TallMachines {

    private TallMachines() {}

    public static boolean canPlaceAt(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        return pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context);
    }

    public static BlockState fillerState(Block filler) {
        return filler.defaultBlockState().setValue(FillerBlock.FACING, Direction.DOWN);
    }

    /**
     * Places the filler above a machine that was placed without one (e.g. with /setblock).
     */
    public static void ensureFiller(Level level, BlockPos pos, Block filler) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (!(aboveState.getBlock() instanceof FillerBlock) && aboveState.canBeReplaced())
            level.setBlock(above, fillerState(filler), Block.UPDATE_ALL);
    }

    /**
     * The machine goes away when its filler goes away.
     */
    @Nullable
    public static BlockState checkFiller(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level) {
        if (direction == Direction.UP && !level.isClientSide() && !(neighborState.getBlock() instanceof FillerBlock))
            return Blocks.AIR.defaultBlockState();
        return null;
    }

    public static VoxelShape clipToBlock(VoxelShape shape) {
        return Shapes.join(shape, Shapes.block(), BooleanOp.AND);
    }
}
