package se.gory_moon.horsepower.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import se.gory_moon.horsepower.Registration;

/**
 * Recipe of the horse grindstone ({@code horsepower:grinding}) and of the hand grindstone when the
 * separate hand recipes are enabled ({@code horsepower:hand_grinding}).
 */
public class GrindstoneRecipe extends HPRecipe {

    private final boolean hand;

    public GrindstoneRecipe(ResourceLocation id, boolean hand, Ingredient input, ItemStack output, ItemStack secondary, int secondaryChance, int time) {
        super(id, input, 1, output, secondary, secondaryChance, time);
        this.hand = hand;
    }

    public boolean isHand() {
        return hand;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return hand ? Registration.HAND_GRINDING_SERIALIZER.get() : Registration.GRINDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return hand ? Registration.HAND_GRINDING_TYPE.get() : Registration.GRINDING_TYPE.get();
    }
}
