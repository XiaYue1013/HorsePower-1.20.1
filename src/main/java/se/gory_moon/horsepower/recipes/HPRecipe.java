package se.gory_moon.horsepower.recipes;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

/**
 * Data driven replacement of the 1.12.2 {@code HPRecipeBase}.
 * All machine recipes share the same shape: one input (with an amount), an item or fluid output,
 * an optional secondary output with a chance and a time in "points".
 */
public abstract class HPRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final Ingredient input;
    private final int inputCount;
    private final ItemStack output;
    private final ItemStack secondary;
    private final int secondaryChance;
    private final int time;

    protected HPRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack output, ItemStack secondary, int secondaryChance, int time) {
        this.id = id;
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.output = output;
        this.secondary = secondary;
        this.secondaryChance = secondary.isEmpty() ? 0 : Math.max(0, Math.min(100, secondaryChance));
        this.time = time;
    }

    public Ingredient getInput() {
        return input;
    }

    public int getInputCount() {
        return inputCount;
    }

    public ItemStack getOutput() {
        return output;
    }

    public FluidStack getOutputFluid() {
        return FluidStack.EMPTY;
    }

    public ItemStack getSecondary() {
        return secondary;
    }

    public int getSecondaryChance() {
        return secondaryChance;
    }

    public int getTime() {
        return time;
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && input.test(stack);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return matches(container.getItem(0));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return output;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, input);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    /**
     * Machine recipes never show up in the vanilla recipe book.
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public abstract RecipeSerializer<?> getSerializer();

    @Override
    public abstract RecipeType<?> getType();

    @Override
    public String toString() {
        return id + "{" + inputCount + "x" + input.toJson() + " -> " + output + (time > -1 ? " = " + time : "") +
                (!secondary.isEmpty() ? " {" + secondary + "->" + secondaryChance + "%}" : "") + "}";
    }
}
