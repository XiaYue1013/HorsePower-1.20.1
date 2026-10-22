package se.gory_moon.horsepower.dev;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.compat.jei.HPCategories;
import se.gory_moon.horsepower.compat.jei.HorsePowerJeiPlugin;

import java.util.List;

/**
 * Development only: opens the JEI recipe views for the visual test screenshots.
 */
final class JeiShots {

    static final int STEPS = 6;

    private JeiShots() {}

    static void show(int step) {
        IJeiRuntime runtime = HorsePowerJeiPlugin.getRuntime();
        if (runtime == null)
            return;
        switch (step) {
            case 0 -> runtime.getRecipesGui().showTypes(List.of(HPCategories.GRINDING));
            case 1 -> runtime.getRecipesGui().showTypes(List.of(HPCategories.CHOPPING));
            case 2 -> runtime.getRecipesGui().showTypes(List.of(HPCategories.MANUAL_CHOPPING));
            case 3 -> runtime.getRecipesGui().showTypes(List.of(HPCategories.PRESS_ITEM));
            case 4 -> runtime.getRecipesGui().showTypes(List.of(HPCategories.PRESS_FLUID));
            case 5 -> {
                // The textured chopping block crafting extension: log how JEI lays out the chopper recipe
                var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, HPCategories.choppingBlockIcon(new ItemStack(Registration.CHOPPER_ITEM.get())));
                var category = runtime.getRecipeManager().getRecipeCategory(RecipeTypes.CRAFTING);
                runtime.getRecipeManager().createRecipeLookup(RecipeTypes.CRAFTING).limitFocus(List.of(focus)).get().forEach(recipe ->
                        runtime.getRecipeManager().createRecipeLayoutDrawable(category, recipe, runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).ifPresent(layout -> {
                            var outputs = layout.getRecipeSlotsView().getSlotViews(RecipeIngredientRole.OUTPUT);
                            long variants = outputs.isEmpty() ? 0 : outputs.get(0).getItemStacks().count();
                            HorsePower.LOGGER.info("Visual test: JEI crafting {} -> {} textured output variants, first {}", recipe.getId(), variants,
                                    outputs.isEmpty() ? null : outputs.get(0).getItemStacks().findFirst().map(s -> s.getTag()).orElse(null));
                        }));
                runtime.getRecipesGui().show(focus);
            }
            default -> {}
        }
    }
}
