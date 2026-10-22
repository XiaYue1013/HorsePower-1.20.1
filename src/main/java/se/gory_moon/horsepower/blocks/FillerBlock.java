package se.gory_moon.horsepower.blocks;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;
import se.gory_moon.horsepower.blockentity.FillerBlockEntity;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Port of the 1.12.2 {@code BlockFiller}: the invisible top half of the chopper and the press.
 * Everything (shape, interaction, breaking, sounds, particles, pick block, capabilities) is delegated to the
 * machine it points at; without that machine it removes itself.
 */
public class FillerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public FillerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FillerBlockEntity(pos, state);
    }

    public static BlockPos getMainPos(BlockState state, BlockPos pos) {
        return pos.relative(state.getValue(FACING));
    }

    private static boolean isValidMain(BlockState mainState) {
        return mainState.getBlock() instanceof HPBaseBlock;
    }

    /**
     * Shape of the machine (which may be more than one block high) seen from this block.
     */
    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (!isValidMain(mainState))
            return Shapes.block();
        Direction d = state.getValue(FACING);
        return mainState.getShape(level, mainPos, context).move(d.getStepX(), d.getStepY(), d.getStepZ());
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (!isValidMain(mainState))
            return Shapes.empty();
        Direction d = state.getValue(FACING);
        VoxelShape shape = mainState.getBlock() instanceof TallMachineBlock tall ? tall.getFullCollisionShape(mainState) : mainState.getCollisionShape(level, mainPos, context);
        return Shapes.join(shape.move(d.getStepX(), d.getStepY(), d.getStepZ()), Shapes.block(), BooleanOp.AND);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING) && !isValidMain(neighborState))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isValidMain(level.getBlockState(getMainPos(state, pos)));
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (!level.isClientSide && isValidMain(mainState)) {
            boolean drop = !player.isCreative() && player.hasCorrectToolForDrops(mainState);
            level.destroyBlock(mainPos, drop, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (isValidMain(mainState))
            return mainState.getDestroyProgress(player, level, mainPos);
        return super.getDestroyProgress(state, player, level, pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (isValidMain(mainState))
            return mainState.use(level, player, hand, hit.withPosition(mainPos));
        return InteractionResult.PASS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (isValidMain(mainState))
            mainState.attack(level, mainPos, player);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (isValidMain(mainState))
            return mainState.getCloneItemStack(target, level, mainPos, player);
        return ItemStack.EMPTY;
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockPos mainPos = getMainPos(state, pos);
        BlockState mainState = level.getBlockState(mainPos);
        if (isValidMain(mainState))
            return mainState.getSoundType(level, mainPos, entity);
        return super.getSoundType(state, level, pos, entity);
    }

    @Override
    public void initializeClient(Consumer<IClientBlockExtensions> consumer) {
        consumer.accept(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
                if (target instanceof BlockHitResult blockHit) {
                    BlockPos mainPos = getMainPos(state, blockHit.getBlockPos());
                    if (isValidMain(level.getBlockState(mainPos)))
                        manager.crack(mainPos, blockHit.getDirection());
                }
                return true;
            }

            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                BlockPos mainPos = getMainPos(state, pos);
                BlockState mainState = level.getBlockState(mainPos);
                if (isValidMain(mainState))
                    manager.destroy(mainPos, mainState);
                return true;
            }
        });
    }

    /**
     * Machines that are two blocks high and need a filler above them.
     */
    public interface TallMachineBlock {
        /**
         * The collision of the whole machine, which may reach into the filler block.
         */
        VoxelShape getFullCollisionShape(BlockState state);
    }
}
