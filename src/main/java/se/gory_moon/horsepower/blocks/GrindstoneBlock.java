package se.gory_moon.horsepower.blocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import se.gory_moon.horsepower.advancements.HPTriggers;
import se.gory_moon.horsepower.blockentity.GrindstoneBlockEntity;
import se.gory_moon.horsepower.util.Localization;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of the 1.12.2 {@code BlockGrindstone}: the horse powered grindstone.
 * FILLED shows the ground output layer on top of the stone.
 */
public class GrindstoneBlock extends HPBaseBlock {

    public static final BooleanProperty FILLED = BooleanProperty.create("filled");

    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape BOUNDING = Block.box(0, 0, 0, 16, 13, 16);

    public GrindstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BOUNDING;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    public static void setState(boolean filled, @Nullable Level level, BlockPos pos) {
        if (level == null || level.isClientSide)
            return;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GrindstoneBlock && state.getValue(FILLED) != filled)
            level.setBlock(pos, state.setValue(FILLED, filled), 3);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrindstoneBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        String white = ChatFormatting.WHITE.toString(), gray = ChatFormatting.GRAY.toString();
        tooltip.add(Localization.component("item.horsepower:grindstone.description.size", white, gray).copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Localization.component("item.horsepower:grindstone.description.location", white, gray).copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Localization.component("item.horsepower:grindstone.description.use").copy().withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void onWorkerAttached(Player player, PathfinderMob creature) {
        if (player instanceof ServerPlayer serverPlayer)
            HPTriggers.USE_GRINDSTONE.trigger(serverPlayer);
    }

    @Override
    public void emptiedOutput(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GrindstoneBlockEntity te && te.getStackInSlot(1).isEmpty() && te.getStackInSlot(2).isEmpty())
            GrindstoneBlock.setState(false, level, pos);
    }
}
