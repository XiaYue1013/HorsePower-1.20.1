package se.gory_moon.horsepower.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import se.gory_moon.horsepower.blocks.TallMachines;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Port of the 1.12.2 {@code ItemBlockDouble}: places a machine plus its invisible filler block above it.
 * Like 1.12.2, clicking the bottom of a block or a spot without room above places the machine one block lower.
 */
public class DoubleBlockItem extends BlockItem {

    private final Supplier<? extends Block> filler;

    public DoubleBlockItem(Block block, Supplier<? extends Block> filler, Properties properties) {
        super(block, properties);
        this.filler = filler;
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (context.getClickedFace() == Direction.DOWN || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).canBeReplaced(context))
                return BlockPlaceContext.at(context, below, context.getClickedFace());
        }
        return context;
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos above = context.getClickedPos().above();
        BlockState previous = level.getBlockState(above);
        // The filler goes in first so the machine never exists without its top half
        level.setBlock(above, TallMachines.fillerState(filler.get()), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (super.placeBlock(context, state))
            return true;
        level.setBlock(above, previous, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return false;
    }
}
