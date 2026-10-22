package se.gory_moon.horsepower.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of the 1.12.2 {@code Localization} helper. The lang files keep the original Horse Power placeholder syntax:
 * {@code [%1]} is replaced by the first argument and {@code [%2->lap|laps]} picks an option from a boolean argument.
 * Only used for client side text (tooltips, JEI, nameplates).
 */
public final class Localization {

    private Localization() {}

    public static String translate(String key, Object... vars) {
        String result = Language.getInstance().getOrDefault(key);

        for (int i = 0; i < vars.length; i++) {
            String optionCheck = "[%" + (i + 1) + "->";
            int pos = result.indexOf(optionCheck);

            if (pos != -1) {
                int endPos = result.indexOf("]", pos);
                if (endPos != -1) {
                    String[] options = result.substring(pos + optionCheck.length(), endPos).split("\\|");
                    int pickedOption = vars[i] instanceof Boolean b && b ? 1 : 0;
                    if (options.length > pickedOption) {
                        String opt = options[pickedOption];
                        result = result.substring(0, pos) + opt + result.substring(endPos + 1);
                        i--;
                    }
                }
            } else {
                result = result.replace("[%" + (i + 1) + "]", String.valueOf(vars[i]));
            }
        }

        return result;
    }

    public static Component component(String key, Object... vars) {
        return Component.literal(translate(key, vars));
    }

    /**
     * Translated text split on line breaks, one component per line.
     */
    public static List<Component> lines(String key, Object... vars) {
        List<Component> list = new ArrayList<>();
        for (String line : translate(key, vars).split("\n"))
            list.add(Component.literal(line));
        return list;
    }

    public static final String GRINDSTONE_INVALID = "info.horsepower:grindstone.invalid";
    public static final String CHOPPING_INVALID = "info.horsepower:chopping.invalid";
    public static final String PRESS_INVALID = "info.horsepower:press.invalid";
    public static final String ITEM_REVEAL = "info.horsepower:item.reveal";

    public static final String WAILA_GRINDSTONE_PROGRESS = "gui.horsepower.waila.grindstone.progress";
    public static final String WAILA_WINDUP_PROGRESS = "gui.horsepower.waila.windup.progress";
    public static final String WAILA_CHOPPING_PROGRESS = "gui.horsepower.waila.chopping.progress";
    public static final String WAILA_PRESS_PROGRESS = "gui.horsepower.waila.press.progress";

    public static final String TOP_GRINDSTONE_PROGRESS = "gui.horsepower.top.grindstone.progress";
    public static final String TOP_WINDUP_PROGRESS = "gui.horsepower.top.windup.progress";
    public static final String TOP_CHOPPING_PROGRESS = "gui.horsepower.top.chopping.progress";
    public static final String TOP_PRESS_PROGRESS = "gui.horsepower.top.press.progress";
}
