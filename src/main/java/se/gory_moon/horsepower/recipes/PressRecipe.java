package se.gory_moon.horsepower.recipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;
import se.gory_moon.horsepower.Registration;

/**
 * Recipe of the horse press ({@code horsepower:pressing}). The output is either an item or a fluid.
 * The press time is global and set by the "pointsForPress" config, as in 1.12.2.
 */
public class PressRecipe extends HPRecipe {

    private final FluidStack outputFluid;

    public PressRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack output, FluidStack outputFluid) {
        super(id, input, inputCount, output, ItemStack.EMPTY, 0, 0);
        this.outputFluid = outputFluid;
    }

    public boolean isLiquidRecipe() {
        return !outputFluid.isEmpty();
    }

    @Override
    public FluidStack getOutputFluid() {
        return outputFluid;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registration.PRESSING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return Registration.PRESSING_TYPE.get();
    }
}
