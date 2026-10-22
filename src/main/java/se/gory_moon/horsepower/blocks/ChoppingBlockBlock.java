package se.gory_moon.horsepower.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.blockentity.ManualChopperBlockEntity;
import se.gory_moon.horsepower.util.ChoppingAxes;

import javax.annotation.Nullable;

/**
 * Port of the 1.12.2 {@code BlockChoppingBlock}, the manual chopping block.
 * Put a log on it with right click, then hit it (left click) with an axe. While it can chop, it can't be broken
 * with an axe.
 */
public class ChoppingBlockBlock extends HPChoppingBaseBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public ChoppingBlockBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ManualChopperBlockEntity(pos, state);
    }

    @Nullable
    @Override
    protected <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level) {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player instanceof FakePlayer)
            return InteractionResult.SUCCESS;
        return super.use(state, level, pos, player, hand, hit);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide || player instanceof FakePlayer)
            return;

        if (level.getBlockEntity(pos) instanceof ManualChopperBlockEntity te) {
            ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (ChoppingAxes.isValidChoppingTool(held)) {
                if (te.chop(player, held)) {
                    player.causeFoodExhaustion(Configs.general.choppingblockExhaustion.get().floatValue());
                    if (Configs.general.shouldDamageAxe.get())
                        held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ManualChopperBlockEntity te) {
            ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (ChoppingAxes.isValidChoppingTool(heldItem) && te.canWork())
                return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public void emptiedOutput(Level level, BlockPos pos) {}
}
