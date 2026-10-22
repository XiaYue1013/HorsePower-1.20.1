package se.gory_moon.horsepower.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Port of {@code ModItems.removeRecipes()}: with "removeVanillaRecipes" enabled the vanilla crafting recipes that
 * the grindstone replaces are removed (Wheat -> Bread, Sugar Cane -> Sugar, Bone/Bone Block -> Bone Meal,
 * Beetroot and the 1.12.2 flowers -> Dye).
 * Runs after every data pack (re)load, before the recipes are sent to the clients.
 */
public final class VanillaRecipeRemover {

    private static final Set<ResourceLocation> REMOVED = Set.of(
            mc("bread"),
            mc("sugar_from_sugar_cane"),
            mc("bone_meal"),
            mc("bone_meal_from_bone_block"),
            mc("red_dye_from_beetroot"),
            mc("yellow_dye_from_dandelion"),
            mc("red_dye_from_poppy"),
            mc("light_blue_dye_from_blue_orchid"),
            mc("magenta_dye_from_allium"),
            mc("light_gray_dye_from_azure_bluet"),
            mc("red_dye_from_tulip"),
            mc("orange_dye_from_orange_tulip"),
            mc("light_gray_dye_from_white_tulip"),
            mc("pink_dye_from_pink_tulip"),
            mc("light_gray_dye_from_oxeye_daisy"),
            mc("yellow_dye_from_sunflower"),
            mc("magenta_dye_from_lilac"),
            mc("red_dye_from_rose_bush"),
            mc("pink_dye_from_peony")
    );

    private VanillaRecipeRemover() {}

    private static ResourceLocation mc(String path) {
        return new ResourceLocation("minecraft", path);
    }

    public static void apply(RecipeManager manager) {
        if (Configs.general.removeVanillaRecipes.get())
            removeFrom(manager);
    }

    public static void removeFrom(RecipeManager manager) {
        List<Recipe<?>> kept = new ArrayList<>();
        int removed = 0;
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (REMOVED.contains(recipe.getId()))
                removed++;
            else
                kept.add(recipe);
        }
        if (removed > 0) {
            manager.replaceRecipes(kept);
            HorsePower.LOGGER.info("Removed {} vanilla recipes replaced by the grindstone", removed);
        }
    }
}
