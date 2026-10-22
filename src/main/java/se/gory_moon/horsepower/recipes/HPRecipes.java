package se.gory_moon.horsepower.recipes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.Registration;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Port of the 1.12.2 {@code HPRecipes} lookup. Recipes now live in the level's {@link net.minecraft.world.item.crafting.RecipeManager},
 * which means they come from data packs, are synced to clients automatically and are reloaded with /reload.
 */
public final class HPRecipes {

    private HPRecipes() {}

    @Nullable
    private static <T extends HPRecipe> T find(@Nullable Level level, RecipeType<T> type, ItemStack stack) {
        if (level == null || stack.isEmpty())
            return null;
        List<T> recipes = level.getRecipeManager().getAllRecipesFor(type);
        for (T recipe : recipes) {
            if (recipe.matches(stack))
                return recipe;
        }
        return null;
    }

    public static RecipeType<GrindstoneRecipe> grindstoneType(boolean hand) {
        return hand && Configs.recipes.useSeperateGrindstoneRecipes.get() ? Registration.HAND_GRINDING_TYPE.get() : Registration.GRINDING_TYPE.get();
    }

    public static RecipeType<ChoppingRecipe> choppingType(boolean hand) {
        return hand && Configs.recipes.useSeperateChoppingRecipes.get() ? Registration.MANUAL_CHOPPING_TYPE.get() : Registration.CHOPPING_TYPE.get();
    }

    @Nullable
    public static GrindstoneRecipe getGrindstoneRecipe(@Nullable Level level, ItemStack stack, boolean hand) {
        return find(level, grindstoneType(hand), stack);
    }

    @Nullable
    public static ChoppingRecipe getChoppingBlockRecipe(@Nullable Level level, ItemStack stack, boolean hand) {
        return find(level, choppingType(hand), stack);
    }

    @Nullable
    public static PressRecipe getPressRecipe(@Nullable Level level, ItemStack stack) {
        return find(level, Registration.PRESSING_TYPE.get(), stack);
    }

    public static boolean hasGrindstoneRecipe(@Nullable Level level, ItemStack stack, boolean hand) {
        return getGrindstoneRecipe(level, stack, hand) != null;
    }

    public static boolean hasChopperRecipe(@Nullable Level level, ItemStack stack, boolean hand) {
        return getChoppingBlockRecipe(level, stack, hand) != null;
    }

    public static boolean hasPressRecipe(@Nullable Level level, ItemStack stack) {
        return getPressRecipe(level, stack) != null;
    }

    public static int getGrindstoneTime(@Nullable Level level, ItemStack stack, boolean hand) {
        GrindstoneRecipe recipe = getGrindstoneRecipe(level, stack, hand);
        return recipe != null ? recipe.getTime() : 16;
    }

    public static int getChoppingTime(@Nullable Level level, ItemStack stack, boolean hand) {
        ChoppingRecipe recipe = getChoppingBlockRecipe(level, stack, hand);
        return getChoppingMultiplier(hand) * (recipe != null ? recipe.getTime() : 1);
    }

    /**
     * The 1.12.2 rule: when chopping recipes are shared, manual chopping needs "choppMultiplier" times the horse recipe time.
     */
    public static int getChoppingMultiplier(boolean hand) {
        return Configs.recipes.useSeperateChoppingRecipes.get() ? 1 : (hand ? Configs.general.choppMultiplier.get() : 1);
    }
}
