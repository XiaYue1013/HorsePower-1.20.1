package se.gory_moon.horsepower.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import se.gory_moon.horsepower.blocks.HPChoppingBaseBlock;

import javax.annotation.Nullable;

/**
 * Helpers for the "textureBlock" data that makes chopping blocks look like the log they were crafted from.
 * The item NBT layout ({@code {textureBlock:{id:"minecraft:oak_log",Count:1b}}}) is the same as in 1.12.2.
 */
public final class ChoppingTexture {

    public static final String TAG = "textureBlock";

    private ChoppingTexture() {}

    /**
     * Port of {@code BlockHPChoppingBase.createItemStack}.
     */
    public static ItemStack createItemStack(ItemLike table, int amount, ItemStack blockItem) {
        ItemStack stack = new ItemStack(table, amount);
        if (blockItem.isEmpty() || !(blockItem.getItem() instanceof BlockItem item))
            return stack;
        Block block = item.getBlock();
        if (block == Blocks.AIR)
            return stack;

        CompoundTag subTag;
        if (block instanceof HPChoppingBaseBlock) {
            CompoundTag existing = blockItem.getTagElement(TAG);
            subTag = existing != null ? existing.copy() : new CompoundTag();
        } else {
            subTag = new ItemStack(blockItem.getItem()).save(new CompoundTag());
        }
        stack.getOrCreateTag().put(TAG, subTag);
        return stack;
    }

    /**
     * If a log should get its own chopping block entry in the creative tab and JEI.
     * {@code #minecraft:logs} also holds the stripped logs and the wood/hyphae blocks, which would show every wood
     * type four times; 1.12.2 only listed the plain logs. Crafting still accepts (and keeps the texture of) all of them.
     */
    public static boolean isDisplayLog(ResourceLocation key, boolean dynamic) {
        if (!dynamic && !"minecraft".equals(key.getNamespace()))
            return false;
        String path = key.getPath();
        return !path.contains("stripped") && !path.endsWith("_wood") && !path.endsWith("_hyphae");
    }

    public static ItemStack getTextureStack(@Nullable CompoundTag tag) {
        if (tag == null || tag.isEmpty())
            return ItemStack.EMPTY;
        return ItemStack.of(tag);
    }

    public static ItemStack getTextureStack(ItemStack chopper) {
        return getTextureStack(chopper.getTagElement(TAG));
    }

    @Nullable
    public static BlockState getTextureState(ItemStack textureStack) {
        if (textureStack.isEmpty() || !(textureStack.getItem() instanceof BlockItem item))
            return null;
        Block block = item.getBlock();
        if (block == Blocks.AIR)
            return null;
        return block.defaultBlockState();
    }
}
