package se.gory_moon.horsepower.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import se.gory_moon.horsepower.Registration;

/**
 * Recipe of the horse chopping block ({@code horsepower:chopping}) and of the manual chopping block when the
 * separate manual recipes are enabled ({@code horsepower:manual_chopping}).
 * As in 1.12.2 the input amount is always one and there is no secondary output.
 */
public class ChoppingRecipe extends HPRecipe {

    private final boolean manual;

    public ChoppingRecipe(ResourceLocation id, boolean manual, Ingredient input, ItemStack output, int time) {
        super(id, input, 1, output, ItemStack.EMPTY, 0, time);
        this.manual = manual;
    }

    public boolean isManual() {
        return manual;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return manual ? Registration.MANUAL_CHOPPING_SERIALIZER.get() : Registration.CHOPPING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return manual ? Registration.MANUAL_CHOPPING_TYPE.get() : Registration.CHOPPING_TYPE.get();
    }
}
