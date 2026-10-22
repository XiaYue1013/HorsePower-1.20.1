package se.gory_moon.horsepower.compat.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import org.openzen.zencode.java.ZenCodeType;
import se.gory_moon.horsepower.recipes.ChoppingRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;

/**
 * {@code mods.horsepower.ChoppingBlock}, same functions as 1.12.2:
 * <pre>
 * mods.horsepower.ChoppingBlock.add(IIngredient input, IItemStack output, int time, [bool hand]);
 * mods.horsepower.ChoppingBlock.remove(IIngredient output, [bool hand]);
 * </pre>
 * {@code hand} targets the manual chopping block recipes when they are separated in the config.
 */
@ZenRegister
@ZenCodeType.Name("mods.horsepower.ChoppingBlock")
public final class ChoppingTweaker {

    private ChoppingTweaker() {}

    @ZenCodeType.Method
    public static void add(IIngredient input, IItemStack output, int time, @ZenCodeType.OptionalBoolean boolean hand) {
        boolean manual = hand && HPRecipes.choppingType(true) != HPRecipes.choppingType(false);
        ChoppingRecipe recipe = new ChoppingRecipe(CTHelper.autoName(manual ? "manual_chopping" : "chopping", output, input), manual,
                input.asVanillaIngredient(), output.getInternal().copy(), time);
        CTHelper.add(CTHelper.manager(HPRecipes.choppingType(hand)), recipe, r -> r.getOutput().toString());
    }

    @ZenCodeType.Method
    public static void remove(IIngredient output, @ZenCodeType.OptionalBoolean boolean hand) {
        CTHelper.remove(CTHelper.manager(HPRecipes.choppingType(hand)), recipe -> CTHelper.outputMatches(output, recipe.getOutput()));
    }
}
