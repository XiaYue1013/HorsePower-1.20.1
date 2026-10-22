package se.gory_moon.horsepower.compat.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import org.openzen.zencode.java.ZenCodeType;
import se.gory_moon.horsepower.recipes.GrindstoneRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;

/**
 * {@code mods.horsepower.Grindstone}, same functions as 1.12.2:
 * <pre>
 * mods.horsepower.Grindstone.add(IIngredient input, IItemStack output, int time, [bool hand], [IItemStack secondary], [int secondaryChance]);
 * mods.horsepower.Grindstone.remove(IIngredient output, [bool hand]);
 * </pre>
 * {@code hand} targets the hand grindstone recipes when they are separated in the config.
 */
@ZenRegister
@ZenCodeType.Name("mods.horsepower.Grindstone")
public final class GrindstoneTweaker {

    private GrindstoneTweaker() {}

    @ZenCodeType.Method
    public static void add(IIngredient input, IItemStack output, int time, @ZenCodeType.OptionalBoolean boolean hand,
                           @ZenCodeType.Optional("<item:minecraft:air>") IItemStack secondary, @ZenCodeType.OptionalInt int secondaryChance) {
        boolean handType = hand && HPRecipes.grindstoneType(true) != HPRecipes.grindstoneType(false);
        boolean hasSecondary = secondary != null && !secondary.isEmpty();
        GrindstoneRecipe recipe = new GrindstoneRecipe(CTHelper.autoName(handType ? "hand_grinding" : "grinding", output, input), handType,
                input.asVanillaIngredient(), output.getInternal().copy(), hasSecondary ? secondary.getInternal().copy() : net.minecraft.world.item.ItemStack.EMPTY,
                hasSecondary ? (secondaryChance <= 0 ? 100 : secondaryChance) : 0, time);
        CTHelper.add(CTHelper.manager(HPRecipes.grindstoneType(hand)), recipe, r -> r.getOutput().toString());
    }

    @ZenCodeType.Method
    public static void remove(IIngredient output, @ZenCodeType.OptionalBoolean boolean hand) {
        CTHelper.remove(CTHelper.manager(HPRecipes.grindstoneType(hand)), recipe -> CTHelper.outputMatches(output, recipe.getOutput()));
    }
}
