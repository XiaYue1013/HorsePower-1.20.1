package se.gory_moon.horsepower.compat.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.item.IItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.openzen.zencode.java.ZenCodeType;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.recipes.PressRecipe;

/**
 * {@code mods.horsepower.Press}, same functions as 1.12.2 (ILiquidStack became IFluidStack):
 * <pre>
 * mods.horsepower.Press.add(IIngredient input, IItemStack output, [int inputAmount]);
 * mods.horsepower.Press.add(IIngredient input, IFluidStack output, [int inputAmount]);
 * mods.horsepower.Press.remove(IIngredient output);
 * </pre>
 * 1.12.2 took the input amount from the stack size of the input, here it can also be given explicitly.
 */
@ZenRegister
@ZenCodeType.Name("mods.horsepower.Press")
public final class PressTweaker {

    private PressTweaker() {}

    private static int amount(IIngredient input, int inputAmount) {
        if (inputAmount > 0)
            return Math.min(inputAmount, 64);
        IItemStack[] items = input.getItems();
        return items.length > 0 ? Math.max(1, Math.min(items[0].getAmount(), 64)) : 1;
    }

    @ZenCodeType.Method
    public static void add(IIngredient input, IItemStack output, @ZenCodeType.OptionalInt int inputAmount) {
        PressRecipe recipe = new PressRecipe(CTHelper.autoName("pressing", output, input), input.asVanillaIngredient(), amount(input, inputAmount),
                output.getInternal().copy(), FluidStack.EMPTY);
        CTHelper.add(CTHelper.manager(Registration.PRESSING_TYPE.get()), recipe, r -> r.getOutput().toString());
    }

    @ZenCodeType.Method
    public static void add(IIngredient input, IFluidStack output, @ZenCodeType.OptionalInt int inputAmount) {
        FluidStack fluid = output.<FluidStack>getImmutableInternal().copy();
        PressRecipe recipe = new PressRecipe(CTHelper.name("pressing", String.valueOf(ForgeRegistries.FLUIDS.getKey(fluid.getFluid())).replace(':', '_') + "_from_" + Integer.toHexString(input.getCommandString().hashCode())),
                input.asVanillaIngredient(), amount(input, inputAmount), ItemStack.EMPTY, fluid);
        CTHelper.add(CTHelper.manager(Registration.PRESSING_TYPE.get()), recipe, r -> r.getOutputFluid().getAmount() + "mb " + r.getOutputFluid().getDisplayName().getString());
    }

    /**
     * Removes item recipes with a matching output (1.12.2 behaviour); use {@link #removeFluid} for fluid recipes.
     */
    @ZenCodeType.Method
    public static void remove(IIngredient output) {
        CTHelper.remove(CTHelper.manager(Registration.PRESSING_TYPE.get()), recipe -> CTHelper.outputMatches(output, recipe.getOutput()));
    }

    @ZenCodeType.Method
    public static void removeFluid(IFluidStack output) {
        CTHelper.remove(CTHelper.manager(Registration.PRESSING_TYPE.get()),
                recipe -> recipe.isLiquidRecipe() && recipe.getOutputFluid().getFluid() == output.getFluid());
    }
}
