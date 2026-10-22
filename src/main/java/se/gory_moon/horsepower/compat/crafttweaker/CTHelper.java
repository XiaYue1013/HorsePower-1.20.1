package se.gory_moon.horsepower.compat.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.action.recipe.ActionRemoveRecipe;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import se.gory_moon.horsepower.HorsePower;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared plumbing of the CraftTweaker integration (port of the 1.12.2 {@code tweaker} package).
 * Recipes added from scripts go into the normal recipe manager, so they behave exactly like data pack recipes.
 * Besides the classic {@code mods.horsepower.*} functions below, every recipe type is also available through
 * CraftTweaker's generic {@code <recipetype:horsepower:...>} managers (removeByName, removeAll, addJsonRecipe...).
 */
final class CTHelper {

    private CTHelper() {}

    static <T extends Recipe<?>> IRecipeManager<T> manager(RecipeType<T> type) {
        return () -> type;
    }

    static ResourceLocation name(String type, String name) {
        String clean = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        return new ResourceLocation("crafttweaker", HorsePower.MODID + "/" + type + "/" + clean);
    }

    static ResourceLocation autoName(String type, IItemStack output, IIngredient input) {
        return name(type, output.getInternal().getItem() + "_from_" + Integer.toHexString(input.getCommandString().hashCode()));
    }

    static <T extends Recipe<?>> void add(IRecipeManager<T> manager, T recipe, Function<T, String> describer) {
        CraftTweakerAPI.apply(new ActionAddRecipe<>(manager, recipe).outputDescriber(describer));
    }

    static <T extends Recipe<?>> void remove(IRecipeManager<T> manager, Predicate<T> predicate) {
        CraftTweakerAPI.apply(new ActionRemoveRecipe<>(manager, predicate));
    }

    static boolean outputMatches(IIngredient output, ItemStack stack) {
        return !stack.isEmpty() && output.matches(IItemStack.of(stack));
    }
}
