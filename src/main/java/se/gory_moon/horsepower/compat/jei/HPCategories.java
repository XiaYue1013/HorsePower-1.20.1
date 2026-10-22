package se.gory_moon.horsepower.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.recipes.ChoppingRecipe;
import se.gory_moon.horsepower.recipes.GrindstoneRecipe;
import se.gory_moon.horsepower.recipes.HPRecipes;
import se.gory_moon.horsepower.recipes.PressRecipe;
import se.gory_moon.horsepower.util.ChoppingAxes;
import se.gory_moon.horsepower.util.ChoppingTexture;
import se.gory_moon.horsepower.util.Localization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ports of the 1.12.2 JEI categories. Layout (slot positions, arrows, lap counters, backgrounds) follows 1.12.2.
 */
public final class HPCategories {

    public static final RecipeType<GrindstoneRecipe> GRINDING = RecipeType.create(HorsePower.MODID, "grinding", GrindstoneRecipe.class);
    public static final RecipeType<GrindstoneRecipe> HAND_GRINDING = RecipeType.create(HorsePower.MODID, "hand_grinding", GrindstoneRecipe.class);
    public static final RecipeType<ChoppingRecipe> CHOPPING = RecipeType.create(HorsePower.MODID, "chopping", ChoppingRecipe.class);
    public static final RecipeType<ManualChopping> MANUAL_CHOPPING = RecipeType.create(HorsePower.MODID, "manual_chopping", ManualChopping.class);
    public static final RecipeType<PressRecipe> PRESS_ITEM = RecipeType.create(HorsePower.MODID, "press", PressRecipe.class);
    public static final RecipeType<PressRecipe> PRESS_FLUID = RecipeType.create(HorsePower.MODID, "press_fluid", PressRecipe.class);

    private static final ResourceLocation JEI = new ResourceLocation(HorsePower.MODID, "textures/gui/jei.png");
    private static final ResourceLocation JEI_FLUID = new ResourceLocation(HorsePower.MODID, "textures/gui/jei_fluid.png");
    private static final ResourceLocation JEI_GRINDSTONE = new ResourceLocation(HorsePower.MODID, "textures/gui/jei_grindstone.png");
    private static final ResourceLocation JEI_MANUAL_CHOPPING = new ResourceLocation(HorsePower.MODID, "textures/gui/jei_manual_chopping.png");

    private HPCategories() {}

    private static String format(double laps) {
        return laps == Math.floor(laps) ? String.valueOf((int) laps) + ".0" : String.valueOf(laps);
    }

    /**
     * A manual chopping recipe with the amount of chops it needs (recipe time x "choppMultiplier" when not separated).
     */
    public record ManualChopping(ChoppingRecipe recipe, int chops) {}

    public static ItemStack choppingBlockIcon(ItemStack block) {
        return ChoppingTexture.createItemStack(block.getItem(), 1, new ItemStack(net.minecraft.world.item.Items.OAK_LOG));
    }

    // ---------------------------------------------------------------- grinding

    public static class Grinding extends HorsePowerCategory<GrindstoneRecipe> {
        private final boolean hand;
        private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

        public Grinding(IGuiHelper guiHelper, boolean hand) {
            super(guiHelper, guiHelper.createDrawableItemStack(new ItemStack(hand ? Registration.HAND_GRINDSTONE.get() : Registration.GRINDSTONE.get())),
                    true, 146, 85, JEI_GRINDSTONE);
            this.hand = hand;
        }

        @Override
        public RecipeType<GrindstoneRecipe> getRecipeType() {
            return hand ? HAND_GRINDING : GRINDING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable(hand ? "gui.horsepower.jei.category.hand.grinding" : "gui.horsepower.jei.category.grinding");
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, GrindstoneRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 35, 28).addIngredients(recipe.getInput());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 28).addItemStack(recipe.getOutput());
            IRecipeSlotBuilder secondary = builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 51);
            if (!recipe.getSecondary().isEmpty()) {
                secondary.addItemStack(recipe.getSecondary());
                secondary.addTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(
                        Localization.translate("gui.horsepower.jei.tooltip.grinding.chance", recipe.getSecondaryChance())).withStyle(ChatFormatting.GRAY)));
            }
            openRecipe();
        }

        @Override
        public void draw(GrindstoneRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            super.draw(recipe, slots, graphics, mouseX, mouseY);
            arrows.computeIfAbsent(recipe.getTime(), t -> arrow((int) ((t / 8D) * 100))).draw(graphics, 57, 27);
            graphics.drawString(Minecraft.getInstance().font, "x" + format(laps(recipe.getTime())), 33, 48, 0xFFFFFF, true);
            if (recipe.getSecondaryChance() > 0)
                graphics.drawString(Minecraft.getInstance().font, recipe.getSecondaryChance() + "%", 65, 58, 0x808080, false);
        }

        @Override
        public List<Component> getTooltipStrings(GrindstoneRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            if (mouseX >= 55 && mouseY >= 21 && mouseX < 80 && mouseY < 45) {
                double laps = laps(recipe.getTime());
                tooltip.add(Localization.component("gui.horsepower.jei.tooltip.grinding", format(laps), laps >= 2D));
            }
            return tooltip;
        }
    }

    // ---------------------------------------------------------------- horse chopping

    public static class Chopping extends HorsePowerCategory<ChoppingRecipe> {
        private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

        public Chopping(IGuiHelper guiHelper) {
            super(guiHelper, guiHelper.createDrawableItemStack(choppingBlockIcon(new ItemStack(Registration.CHOPPER_ITEM.get()))), false, 146, 74, JEI);
        }

        private static double points(ChoppingRecipe recipe) {
            return recipe.getTime() * (double) Math.max(Configs.general.pointsForWindup.get(), 1);
        }

        @Override
        public RecipeType<ChoppingRecipe> getRecipeType() {
            return CHOPPING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.horsepower.jei.category.chopping");
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ChoppingRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 35, 33).addIngredients(recipe.getInput());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 33).addItemStack(recipe.getOutput());
            openRecipe();
        }

        @Override
        public void draw(ChoppingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            super.draw(recipe, slots, graphics, mouseX, mouseY);
            double points = points(recipe);
            arrows.computeIfAbsent((int) points, p -> arrow((int) ((p / 8D) * 100))).draw(graphics, 57, 32);
            graphics.drawString(Minecraft.getInstance().font, "x" + format(laps(points)), 58, 23, 0xFFFFFF, true);
        }

        @Override
        public List<Component> getTooltipStrings(ChoppingRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            if (mouseX >= 55 && mouseY >= 21 && mouseX < 80 && mouseY < 33) {
                double laps = laps(points(recipe));
                tooltip.add(Localization.component("gui.horsepower.jei.tooltip.chopping", format(laps), laps >= 2D));
            }
            return tooltip;
        }
    }

    // ---------------------------------------------------------------- press

    public static class Press extends HorsePowerCategory<PressRecipe> {
        private final boolean fluid;
        private final IDrawableAnimated arrow;

        public Press(IGuiHelper guiHelper, boolean fluid) {
            super(guiHelper, guiHelper.createDrawableItemStack(new ItemStack(Registration.PRESS.get())), false, 146, 74, fluid ? JEI_FLUID : JEI);
            this.fluid = fluid;
            this.arrow = arrow((int) ((points() / 8D) * 100));
        }

        private static double points() {
            return Math.max(Configs.general.pointsForPress.get(), 1);
        }

        @Override
        public RecipeType<PressRecipe> getRecipeType() {
            return fluid ? PRESS_FLUID : PRESS_ITEM;
        }

        @Override
        public Component getTitle() {
            return Component.translatable(fluid ? "gui.horsepower.jei.category.press.fluid" : "gui.horsepower.jei.category.press.item");
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, PressRecipe recipe, IFocusGroup focuses) {
            List<ItemStack> inputs = new ArrayList<>();
            for (ItemStack stack : recipe.getInput().getItems())
                inputs.add(stack.copyWithCount(recipe.getInputCount()));
            builder.addSlot(RecipeIngredientRole.INPUT, 35, 33).addItemStacks(inputs);
            if (fluid) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 23)
                        .setFluidRenderer(Configs.general.pressFluidTankSize.get(), true, 16, 27)
                        .addFluidStack(recipe.getOutputFluid().getFluid(), recipe.getOutputFluid().getAmount());
            } else {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 33).addItemStack(recipe.getOutput());
            }
            openRecipe();
        }

        @Override
        public void draw(PressRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            super.draw(recipe, slots, graphics, mouseX, mouseY);
            arrow.draw(graphics, fluid ? 61 : 57, 32);
            graphics.drawString(Minecraft.getInstance().font, "x" + format(laps(points())), 58, 23, 0xFFFFFF, true);
        }

        @Override
        public List<Component> getTooltipStrings(PressRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            if (mouseX >= 55 && mouseY >= 21 && mouseX < 80 && mouseY < 33) {
                double laps = laps(points());
                tooltip.add(Localization.component("gui.horsepower.jei.tooltip.pressing", format(laps), laps >= 2D));
            }
            return tooltip;
        }
    }

    // ---------------------------------------------------------------- manual chopping

    public static class ManualChoppingCategory implements IRecipeCategory<ManualChopping> {
        private final IDrawable background;
        private final IDrawable icon;
        private final IDrawableAnimated arrow;
        private final List<ItemStack> axes;

        public ManualChoppingCategory(IGuiHelper guiHelper, List<ItemStack> axes) {
            this.background = guiHelper.createDrawable(JEI_MANUAL_CHOPPING, 0, 0, 78, 44);
            this.icon = guiHelper.createDrawableItemStack(choppingBlockIcon(new ItemStack(Registration.CHOPPING_BLOCK_ITEM.get())));
            this.arrow = guiHelper.createAnimatedDrawable(guiHelper.createDrawable(HorsePowerCategory.COMPONENTS, 60, 0, 24, 17), 50, IDrawableAnimated.StartDirection.LEFT, false);
            this.axes = axes;
        }

        @Override
        public RecipeType<ManualChopping> getRecipeType() {
            return MANUAL_CHOPPING;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("gui.horsepower.jei.category.manual.chopping");
        }

        @Override
        @SuppressWarnings("removal")
        public IDrawable getBackground() {
            return background;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, ManualChopping recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, 23).addIngredients(recipe.recipe().getInput());
            List<ItemStack> outputs = new ArrayList<>(axes.size());
            for (ItemStack axe : axes) {
                ItemStack result = recipe.recipe().getOutput().copy();
                double base = ChoppingAxes.getBaseAmount(axe) / 100D;
                result.setCount(Math.max(1, (int) Math.ceil((double) result.getCount() * base)));
                outputs.add(result);
            }
            IRecipeSlotBuilder axeSlot = builder.addSlot(RecipeIngredientRole.CATALYST, 27, 1).addItemStacks(axes)
                    .addTooltipCallback((view, tooltip) -> view.getDisplayedItemStack().ifPresent(axe -> {
                        tooltip.add(Component.literal(Localization.translate("gui.horsepower.jei.tooltip.manual.chopping.desc.1", ChatFormatting.WHITE.toString() + ChoppingAxes.getBaseAmount(axe))).withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.literal(Localization.translate("gui.horsepower.jei.tooltip.manual.chopping.desc.2", ChatFormatting.WHITE.toString() + ChoppingAxes.getChance(axe))).withStyle(ChatFormatting.GRAY));
                        tooltip.add(Component.literal(Localization.translate("gui.horsepower.jei.tooltip.manual.chopping.desc.3", ChatFormatting.WHITE, ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
                    }));
            IRecipeSlotBuilder outputSlot = builder.addSlot(RecipeIngredientRole.OUTPUT, 57, 23).addItemStacks(outputs);
            if (!axes.isEmpty())
                builder.createFocusLink(axeSlot, outputSlot);
        }

        @Override
        public void draw(ManualChopping recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            arrow.draw(graphics, 23, 22);
        }

        @Override
        public List<Component> getTooltipStrings(ManualChopping recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
            List<Component> tooltip = new ArrayList<>();
            if (mouseX >= 23 && mouseY >= 22 && mouseX < 47 && mouseY < 39)
                tooltip.add(Localization.component("gui.horsepower.jei.tooltip.manual.chopping", recipe.chops()));
            return tooltip;
        }
    }

    public static List<ManualChopping> manualChopping(List<ChoppingRecipe> recipes) {
        List<ManualChopping> list = new ArrayList<>();
        int mult = HPRecipes.getChoppingMultiplier(true);
        for (ChoppingRecipe recipe : recipes)
            list.add(new ManualChopping(recipe, recipe.getTime() * mult));
        return list;
    }
}
