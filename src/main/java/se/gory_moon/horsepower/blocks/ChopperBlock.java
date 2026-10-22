package se.gory_moon.horsepower.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.advancements.HPTriggers;
import se.gory_moon.horsepower.blockentity.ChopperBlockEntity;
import se.gory_moon.horsepower.util.Localization;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of the 1.12.2 {@code BlockChopper}, the horse powered chopping block (two blocks high).
 */
public class ChopperBlock extends HPChoppingBaseBlock implements FillerBlock.TallMachineBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape FULL_SHAPE = Block.box(0, 0, 0, 16, 32, 16);
    private static final VoxelShape COLLISION = TallMachines.clipToBlock(FULL_SHAPE);

    public ChopperBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!TallMachines.canPlaceAt(context))
            return null;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !oldState.is(this))
            TallMachines.ensureFiller(level, pos, Registration.CHOPPER_FILLER.get());
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState result = TallMachines.checkFiller(state, direction, neighborState, level);
        return result != null ? result : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FULL_SHAPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Override
    public VoxelShape getFullCollisionShape(BlockState state) {
        return FULL_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChopperBlockEntity(pos, state);
    }

    @Override
    public void emptiedOutput(Level level, BlockPos pos) {}

    @Override
    public void onWorkerAttached(Player player, PathfinderMob creature) {
        if (player instanceof ServerPlayer serverPlayer)
            HPTriggers.USE_CHOPPER.trigger(serverPlayer);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        String white = ChatFormatting.WHITE.toString(), gray = ChatFormatting.GRAY.toString();
        tooltip.add(Localization.component("item.horsepower:chopping.description.size", white, gray).copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Localization.component("item.horsepower:chopping.description.location").copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Localization.component("item.horsepower:chopping.description.use").copy().withStyle(ChatFormatting.GRAY));
    }
}
