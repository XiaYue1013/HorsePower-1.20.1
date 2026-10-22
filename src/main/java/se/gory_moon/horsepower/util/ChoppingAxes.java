package se.gory_moon.horsepower.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Pair;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of the axe handling of the manual chopping block ({@code HPEventHandler.choppingAxes/harvestPercentages},
 * {@code TileEntityManualChopper.getBaseAmount/getChance} and {@code BlockChoppingBlock.isValidChoppingTool}).
 * <p>
 * 1.12.2 used the "axe" harvest level; in 1.20.1 an axe is anything that can perform {@link ToolActions#AXE_DIG}
 * and its harvest level is the tier level of the item.
 */
public final class ChoppingAxes {

    public static final TagKey<Item> CHOPPING_AXES_TAG = TagKey.create(Registries.ITEM, new ResourceLocation(HorsePower.MODID, "chopping_axes"));

    private static List<? extends String> cachedAxesConfig;
    private static List<? extends String> cachedHarvestConfig;
    private static final Map<Item, Pair<Integer, Integer>> choppingAxes = new HashMap<>();
    private static final Map<Integer, Pair<Integer, Integer>> harvestPercentages = new HashMap<>();

    private ChoppingAxes() {}

    private static void refresh() {
        List<? extends String> axes = Configs.general.choppingBlockAxes.get();
        if (axes != cachedAxesConfig) {
            choppingAxes.clear();
            for (String s : axes) {
                String[] data = s.split("=");
                if (data.length != 2) {
                    HorsePower.LOGGER.warn("Parse error with item for custom axes for the chopping block ({})", s);
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(data[0].trim());
                Item item = id != null && ForgeRegistries.ITEMS.containsKey(id) ? ForgeRegistries.ITEMS.getValue(id) : null;
                Pair<Integer, Integer> values = parseValues(data[1], s);
                if (item == null)
                    HorsePower.LOGGER.warn("Parse error with item for custom axes for the chopping block, unknown item ({})", s);
                else if (values != null)
                    choppingAxes.put(item, values);
            }
            cachedAxesConfig = axes;
        }
        List<? extends String> harvest = Configs.general.harvestable_percentage.get();
        if (harvest != cachedHarvestConfig) {
            harvestPercentages.clear();
            for (String s : harvest) {
                String[] data = s.split("=");
                try {
                    Pair<Integer, Integer> values = data.length == 2 ? parseValues(data[1], s) : null;
                    if (values != null)
                        harvestPercentages.put(Integer.parseInt(data[0].trim()), values);
                    else
                        HorsePower.LOGGER.warn("HarvestLevel config is malformed ({})", s);
                } catch (NumberFormatException e) {
                    HorsePower.LOGGER.warn("HarvestLevel config is malformed, make sure only numbers are used as values, ({})", s);
                }
            }
            cachedHarvestConfig = harvest;
        }
    }

    private static Pair<Integer, Integer> parseValues(String in, String line) {
        String[] parts = in.split("-");
        try {
            return Pair.of(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            HorsePower.LOGGER.warn("Base amount or chance for chopping axe is malformed, ({})", line);
            return null;
        }
    }

    /**
     * The "axe harvest level" of a stack, -1 if it isn't an axe.
     */
    public static int getHarvestLevel(ItemStack stack) {
        if (stack.isEmpty() || !stack.canPerformAction(ToolActions.AXE_DIG))
            return -1;
        if (stack.getItem() instanceof TieredItem tiered)
            return tiered.getTier().getLevel();
        return 0;
    }

    public static boolean isValidChoppingTool(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        refresh();
        return getHarvestLevel(stack) > -1 || stack.is(CHOPPING_AXES_TAG) || choppingAxes.containsKey(stack.getItem());
    }

    public static int getBaseAmount(ItemStack axe) {
        refresh();
        Pair<Integer, Integer> custom = choppingAxes.get(axe.getItem());
        if (custom != null)
            return custom.getLeft();
        int harvestLevel = getHarvestLevel(axe);
        if (harvestLevel > -1 && harvestPercentages.get(harvestLevel) != null)
            return harvestPercentages.get(harvestLevel).getLeft();
        return 100;
    }

    public static int getChance(ItemStack axe) {
        refresh();
        Pair<Integer, Integer> custom = choppingAxes.get(axe.getItem());
        if (custom != null)
            return custom.getRight();
        int harvestLevel = getHarvestLevel(axe);
        if (harvestLevel > -1 && harvestPercentages.get(harvestLevel) != null)
            return harvestPercentages.get(harvestLevel).getRight();
        return 0;
    }
}
