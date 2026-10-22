package se.gory_moon.horsepower.compat.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.openzen.zencode.java.ZenCodeType;
import se.gory_moon.horsepower.recipes.ChoppingCraftingRecipes;

/**
 * {@code mods.horsepower.Recipes}: crafting recipes whose result takes the texture of the ingredient matching
 * {@code ore} (used for the chopping blocks), same functions as 1.12.2:
 * <pre>
 * mods.horsepower.Recipes.addShaped([String name], IIngredient ore, IItemStack output, IIngredient[][] ingredients);
 * mods.horsepower.Recipes.addShapeless([String name], IIngredient ore, IItemStack output, IIngredient[] ingredients);
 * </pre>
 */
@ZenRegister
@ZenCodeType.Name("mods.horsepower.Recipes")
public final class RecipesTweaker {

    private RecipesTweaker() {}

    @ZenCodeType.Method
    public static void addShaped(String name, IIngredient ore, IItemStack output, IIngredient[][] ingredients) {
        int height = ingredients.length;
        int width = 0;
        for (IIngredient[] row : ingredients)
            width = Math.max(width, row.length);
        NonNullList<Ingredient> list = NonNullList.withSize(width * height, Ingredient.EMPTY);
        for (int y = 0; y < height; y++)
            for (int x = 0; x < ingredients[y].length; x++)
                if (ingredients[y][x] != null && !ingredients[y][x].isEmpty())
                    list.set(y * width + x, ingredients[y][x].asVanillaIngredient());

        ShapedRecipe base = new ShapedRecipe(CTHelper.name("crafting", name), "", CraftingBookCategory.MISC, width, height, list, output.getInternal().copy());
        CTHelper.add(CTHelper.<CraftingRecipe>manager(RecipeType.CRAFTING), new ChoppingCraftingRecipes.Shaped(base, ore.asVanillaIngredient()),
                r -> output.getCommandString());
    }

    @ZenCodeType.Method
    public static void addShaped(IIngredient ore, IItemStack output, IIngredient[][] ingredients) {
        addShaped(CTHelper.autoName("crafting", output, ore).getPath(), ore, output, ingredients);
    }

    @ZenCodeType.Method
    public static void addShapeless(String name, IIngredient ore, IItemStack output, IIngredient[] ingredients) {
        NonNullList<Ingredient> list = NonNullList.create();
        for (IIngredient ingredient : ingredients)
            if (ingredient != null && !ingredient.isEmpty())
                list.add(ingredient.asVanillaIngredient());
        ShapelessRecipe base = new ShapelessRecipe(CTHelper.name("crafting", name), "", CraftingBookCategory.MISC, output.getInternal().copy(), list);
        CTHelper.add(CTHelper.<CraftingRecipe>manager(RecipeType.CRAFTING), new ChoppingCraftingRecipes.Shapeless(base, ore.asVanillaIngredient()),
                r -> output.getCommandString());
    }

    @ZenCodeType.Method
    public static void addShapeless(IIngredient ore, IItemStack output, IIngredient[] ingredients) {
        addShapeless(CTHelper.autoName("crafting", output, ore).getPath(), ore, output, ingredients);
    }
}
