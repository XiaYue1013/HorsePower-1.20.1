package se.gory_moon.horsepower.recipes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;

/**
 * Port of {@code Configs.ConfigFactory}, the 1.12.2 "horsepower:config" recipe condition.
 * Usage: {@code {"type": "horsepower:config", "option": "flour" | "dough" | "hand_chopping_block"}}
 */
public record ConfigCondition(String option) implements ICondition {

    public static final ResourceLocation ID = new ResourceLocation(HorsePower.MODID, "config");

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return switch (option) {
            case "flour" -> Configs.general.enableFlour.get();
            case "dough" -> Configs.general.enableDough.get();
            case "hand_chopping_block" -> Configs.general.enableHandChoppingBlock.get();
            default -> false;
        };
    }

    public static class Serializer implements IConditionSerializer<ConfigCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, ConfigCondition value) {
            json.addProperty("option", value.option());
        }

        @Override
        public ConfigCondition read(JsonObject json) {
            String option = GsonHelper.getAsString(json, "option", GsonHelper.getAsString(json, "enabled", ""));
            if (!option.equals("flour") && !option.equals("dough") && !option.equals("hand_chopping_block"))
                throw new JsonParseException("Unknown horsepower:config option '" + option + "'");
            return new ConfigCondition(option);
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
