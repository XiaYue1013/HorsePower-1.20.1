package se.gory_moon.horsepower.compat.jei;

import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Port of the 1.12.2 JEI {@code HorseDrawable}: a little horse walking around the recipe.
 */
public class HorseDrawable {

    private final IDrawableStatic horse1;
    private final IDrawableStatic horse2;
    private final IDrawableStatic horse3;
    private final IDrawableStatic horse4;
    private final ITickTimer animTimer;
    private final ITickTimer pathTimer;
    private final boolean grinding;

    private boolean reverse;
    private int x;
    private int y;

    public HorseDrawable(IGuiHelper guiHelper, int v, int animCycle, int pathCycle, boolean grinding) {
        this.horse1 = guiHelper.createDrawable(HorsePowerCategory.COMPONENTS, 0, v, 30, 20);
        this.horse2 = guiHelper.createDrawable(HorsePowerCategory.COMPONENTS, 0, v + 20, 30, 20);
        this.horse3 = guiHelper.createDrawable(HorsePowerCategory.COMPONENTS, 30, v, 30, 20);
        this.horse4 = guiHelper.createDrawable(HorsePowerCategory.COMPONENTS, 30, v + 20, 30, 20);
        this.animTimer = guiHelper.createTickTimer(animCycle, 1, false);
        this.pathTimer = guiHelper.createTickTimer(pathCycle, grinding ? 352 : 324, false);
        this.grinding = grinding;
    }

    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        reverse = false;
        setXYPos();
        IDrawableStatic draw;
        if (animTimer.getValue() == 0)
            draw = reverse ? horse3 : horse1;
        else
            draw = reverse ? horse4 : horse2;
        draw.draw(graphics, xOffset + x, yOffset + y);
    }

    private void setXYPos() {
        int location = pathTimer.getValue();
        if (grinding) {
            if (location <= 112) {
                x = location;
                y = 0;
            } else if (location <= 176) {
                x = 112;
                y = location - 112;
                reverse = true;
            } else if (location <= 288) {
                x = 112 - (location - 174);
                y = 64;
                reverse = true;
            } else {
                x = 0;
                y = 64 - (location - 288);
            }
        } else {
            if (location <= 112) {
                x = location;
                y = 0;
            } else if (location <= 162) {
                x = 112;
                y = location - 112;
                reverse = true;
            } else if (location <= 274) {
                x = 112 - (location - 160);
                y = 50;
                reverse = true;
            } else {
                x = 0;
                y = 50 - (location - 274);
            }
        }
    }
}
