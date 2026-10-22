package se.gory_moon.horsepower.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.registries.ForgeRegistries;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.recipes.ChoppingCraftingRecipes;
import se.gory_moon.horsepower.recipes.HPRecipes;
import se.gory_moon.horsepower.recipes.PressRecipe;
import se.gory_moon.horsepower.util.ChoppingAxes;
import se.gory_moon.horsepower.util.ChoppingTexture;
import se.gory_moon.horsepower.util.Localization;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Port of the 1.12.2 {@code HorsePowerPlugin}. Only loaded by JEI, so Horse Power works without JEI installed.
 */
@JeiPlugin
public class HorsePowerJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation(HorsePower.MODID, "jei");
    private static IJeiRuntime runtime;

    @Nullable
    public static IJeiRuntime getRuntime() {
        return runtime;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        IIngredientSubtypeInterpreter<ItemStack> texture = (stack, context) -> {
            CompoundTag tag = stack.getTagElement(ChoppingTexture.TAG);
            return tag == null || tag.isEmpty() ? IIngredientSubtypeInterpreter.NONE : tag.toString();
        };
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Registration.CHOPPER_ITEM.get(), texture);
        registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, Registration.CHOPPING_BLOCK_ITEM.get(), texture);
    }

    private static List<ItemStack> findAxes(IRecipeCategoryRegistration registration) {
        List<ItemStack> axes = new ArrayList<>();
        for (ItemStack stack : registration.getJeiHelpers().getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            if (ChoppingAxes.isValidChoppingTool(stack))
                axes.add(stack);
        }
        return axes;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        if (Configs.recipes.useSeperateGrindstoneRecipes.get())
            registration.addRecipeCategories(new HPCategories.Grinding(guiHelper, true));
        registration.addRecipeCategories(new HPCategories.Grinding(guiHelper, false));
        if (Configs.general.enableHandChoppingBlock.get())
            registration.addRecipeCategories(new HPCategories.ManualChoppingCategory(guiHelper, findAxes(registration)));
        registration.addRecipeCategories(new HPCategories.Chopping(guiHelper));
        registration.addRecipeCategories(new HPCategories.Press(guiHelper, false));
        registration.addRecipeCategories(new HPCategories.Press(guiHelper, true));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null)
            return;
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();

        if (Configs.recipes.useSeperateGrindstoneRecipes.get())
            registration.addRecipes(HPCategories.HAND_GRINDING, manager.getAllRecipesFor(Registration.HAND_GRINDING_TYPE.get()));
        registration.addRecipes(HPCategories.GRINDING, manager.getAllRecipesFor(Registration.GRINDING_TYPE.get()));
        if (Configs.general.enableHandChoppingBlock.get())
            registration.addRecipes(HPCategories.MANUAL_CHOPPING, HPCategories.manualChopping(manager.getAllRecipesFor(HPRecipes.choppingType(true))));
        registration.addRecipes(HPCategories.CHOPPING, manager.getAllRecipesFor(Registration.CHOPPING_TYPE.get()));

        List<PressRecipe> items = new ArrayList<>();
        List<PressRecipe> fluids = new ArrayList<>();
        for (PressRecipe recipe : manager.getAllRecipesFor(Registration.PRESSING_TYPE.get()))
            (recipe.isLiquidRecipe() ? fluids : items).add(recipe);
        registration.addRecipes(HPCategories.PRESS_ITEM, items);
        registration.addRecipes(HPCategories.PRESS_FLUID, fluids);

        registration.addIngredientInfo(new ItemStack(Registration.GRINDSTONE.get()), VanillaTypes.ITEM_STACK,
                Component.literal(Localization.translate("info.horsepower:grindstone.info1")),
                Component.literal(Localization.translate("info.horsepower:grindstone.info2")),
                Component.literal(Localization.translate("info.horsepower:grindstone.info3")));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Registration.HAND_GRINDSTONE.get()),
                Configs.recipes.useSeperateGrindstoneRecipes.get() ? HPCategories.HAND_GRINDING : HPCategories.GRINDING);
        if (Configs.general.enableHandChoppingBlock.get())
            registration.addRecipeCatalyst(HPCategories.choppingBlockIcon(new ItemStack(Registration.CHOPPING_BLOCK_ITEM.get())), HPCategories.MANUAL_CHOPPING);
        registration.addRecipeCatalyst(new ItemStack(Registration.GRINDSTONE.get()), HPCategories.GRINDING);
        registration.addRecipeCatalyst(HPCategories.choppingBlockIcon(new ItemStack(Registration.CHOPPER_ITEM.get())), HPCategories.CHOPPING);
        registration.addRecipeCatalyst(new ItemStack(Registration.PRESS.get()), HPCategories.PRESS_ITEM, HPCategories.PRESS_FLUID);
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addCategoryExtension(ChoppingCraftingRecipes.Shaped.class,
                recipe -> new ChoppingCraftingExtension(recipe, recipe.getTexture(), recipe.getWidth(), recipe.getHeight()));
        registration.getCraftingCategory().addCategoryExtension(ChoppingCraftingRecipes.Shapeless.class,
                recipe -> new ChoppingCraftingExtension(recipe, recipe.getTexture(), 0, 0));
    }

    /**
     * Port of {@code ShapedChoppingCraftingWrapper}/{@code ShapelessChoppingCraftingWrapper}: the output cycles
     * through every log texture together with the log slot.
     */
    private static class ChoppingCraftingExtension implements ICraftingCategoryExtension {
        private final CraftingRecipe recipe;
        private final Ingredient texture;
        private final int width;
        private final int height;

        ChoppingCraftingExtension(CraftingRecipe recipe, Ingredient texture, int width, int height) {
            this.recipe = recipe;
            this.texture = texture;
            this.width = width;
            this.height = height;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ICraftingGridHelper craftingGridHelper, IFocusGroup focuses) {
            ItemStack base = recipe.getResultItem(RegistryAccess.EMPTY);
            List<ItemStack> textures = new ArrayList<>();
            for (ItemStack stack : texture.getItems()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (key != null && ChoppingTexture.isDisplayLog(key, Configs.general.useDynamicDisplay.get()))
                    textures.add(stack);
            }
            List<ItemStack> outputs = new ArrayList<>();
            for (ItemStack stack : textures)
                outputs.add(ChoppingTexture.createItemStack(base.getItem(), base.getCount(), stack));

            List<List<ItemStack>> inputs = new ArrayList<>();
            int textureSlot = -1;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (textureSlot == -1 && !ingredient.isEmpty() && Arrays.equals(ingredient.getItems(), texture.getItems(), (a, b) -> a.getItem() == b.getItem() ? 0 : 1)) {
                    textureSlot = inputs.size();
                    inputs.add(textures);
                } else {
                    inputs.add(Arrays.asList(ingredient.getItems()));
                }
            }

            List<IRecipeSlotBuilder> inputSlots = craftingGridHelper.createAndSetInputs(builder, inputs, width, height);
            IRecipeSlotBuilder outputSlot = craftingGridHelper.createAndSetOutputs(builder, outputs);
            if (textureSlot >= 0 && !outputs.isEmpty()) {
                int slot = craftingIndex(textureSlot, inputs.size());
                if (slot < inputSlots.size())
                    builder.createFocusLink(inputSlots.get(slot), outputSlot);
            }
        }

        /**
         * Same mapping of ingredient index to 3x3 grid slot as JEI's crafting grid helper.
         */
        private int craftingIndex(int i, int inputCount) {
            int w = width, h = height;
            if (w <= 0 || h <= 0) {
                w = h = inputCount > 4 ? 3 : inputCount > 1 ? 2 : 1;
            }
            if (w == 1)
                return h >= 2 ? (i * 3) + 1 : 4;
            if (h == 1)
                return i + 3;
            if (w == 2) {
                int index = i;
                if (i > 1) {
                    index++;
                    if (i > 3)
                        index++;
                }
                return index;
            }
            if (h == 2)
                return i + 3;
            return i;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public ResourceLocation getRegistryName() {
            return recipe.getId();
        }
    }
}
