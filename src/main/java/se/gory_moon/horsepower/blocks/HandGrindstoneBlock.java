package se.gory_moon.horsepower.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.util.Localization;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of the 1.12.2 {@code BlockHandGrindstone}: the player powered grindstone.
 */
public class HandGrindstoneBlock extends HPBaseBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    public HandGrindstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Which item slot the player clicked on, based on where on the top of the grindstone the hit was.
     */
    @Override
    public int getSlot(BlockState state, Vec3 hit) {
        double hitX = hit.x, hitY = hit.y, hitZ = hit.z;
        Direction f = state.getValue(FACING).getOpposite();
        if (hitX >= 0.3125 && hitX <= 0.6875 && hitY >= 0.52 && hitZ >= 0.625 && hitZ <= 0.9375)
            return f == Direction.NORTH ? 2 : f == Direction.SOUTH ? -2 : f == Direction.EAST ? 1 : 0;
        else if (hitX >= 0.3125 && hitX <= 0.6875 && hitY >= 0.52 && hitZ >= 0.0625 && hitZ <= 0.375)
            return f == Direction.NORTH ? -2 : f == Direction.SOUTH ? 2 : f == Direction.EAST ? 0 : 1;
        else if (hitX >= 0.0625 && hitX <= 0.375 && hitY >= 0.52 && hitZ >= 0.3125 && hitZ <= 0.6875)
            return f == Direction.NORTH ? 0 : f == Direction.SOUTH ? 1 : f == Direction.EAST ? 2 : -2;
        else if (hitX >= 0.625 && hitX <= 0.9375 && hitY >= 0.52 && hitZ >= 0.3125 && hitZ <= 0.6875)
            return f == Direction.NORTH ? 1 : f == Direction.SOUTH ? 0 : f == Direction.EAST ? -2 : 2;

        return -2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HandGrindstoneBlockEntity(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof FakePlayer)
            return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof HandGrindstoneBlockEntity tile && tile.canWork() && !player.isShiftKeyDown()) {
            if (!level.isClientSide && tile.turn())
                player.causeFoodExhaustion(Configs.general.grindstoneExhaustion.get().floatValue());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void emptiedOutput(Level level, BlockPos pos) {}

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(Localization.lines("item.horsepower:hand_grindstone.description.info", "\n" + ChatFormatting.GRAY));
    }
}
