package se.gory_moon.horsepower.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import se.gory_moon.horsepower.HorsePower;

import java.util.UUID;

/**
 * Port of the 1.12.2 JEI {@code HorsePowerCategory}: shared background, icon and the walking horse.
 */
public abstract class HorsePowerCategory<T> implements IRecipeCategory<T> {

    public static final ResourceLocation COMPONENTS = new ResourceLocation(HorsePower.MODID, "textures/gui/components.png");
    private static final UUID DARKOSTO = UUID.fromString("10755ea6-9721-467a-8b5c-92adf689072c");

    protected final IGuiHelper guiHelper;
    private final IDrawable background;
    private final IDrawable icon;
    private final HorseDrawable horse;
    private final HorseDrawable hedgehog;
    private final HorseDrawable character;
    private HorseDrawable currentDrawable;

    protected HorsePowerCategory(IGuiHelper guiHelper, IDrawable icon, boolean grinding, int width, int height, ResourceLocation background) {
        this.guiHelper = guiHelper;
        this.icon = icon;
        this.background = guiHelper.createDrawable(background, 0, 0, width, height);
        this.horse = new HorseDrawable(guiHelper, 0, 20, 100, grinding);
        this.hedgehog = new HorseDrawable(guiHelper, 40, 5, 35, grinding);
        this.character = new HorseDrawable(guiHelper, 80, 10, 100, grinding);
        this.currentDrawable = horse;
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

    protected IDrawableAnimated arrow(int ticks) {
        IDrawableStatic arrowDrawable = guiHelper.createDrawable(COMPONENTS, 60, 0, 24, 17);
        return guiHelper.createAnimatedDrawable(arrowDrawable, Math.max(ticks, 1), IDrawableAnimated.StartDirection.LEFT, false);
    }

    /**
     * The 1.12.2 easter eggs: a hedgehog with Animania installed and a mini Darkosto (who came up with the mod).
     */
    protected void openRecipe() {
        currentDrawable = horse;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null)
            return;
        if (mc.level.random.nextInt(100) <= 10 && DARKOSTO.equals(mc.player.getGameProfile().getId())) {
            currentDrawable = character;
        } else if (mc.level.random.nextInt(3000) <= 50 && ModList.get().isLoaded("animania")) {
            currentDrawable = hedgehog;
        }
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        currentDrawable.draw(graphics, 2, 0);
    }

    protected static double laps(double points) {
        return (double) Math.round((points / 8D) * 100.0D) / 100.0D;
    }
}
