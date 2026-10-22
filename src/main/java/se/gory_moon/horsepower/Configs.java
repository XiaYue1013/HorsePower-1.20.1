package se.gory_moon.horsepower;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

import java.util.List;

/**
 * Port of the 1.12.2 {@code @Config} class. Option names, comments and defaults are kept from Horse Power 2.6.4.
 * The string based recipe lists from 1.12.2 were replaced by data pack recipes (see {@code data/horsepower/recipes}).
 */
public class Configs {

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec COMMON_SPEC;

    public static final Client client;
    public static final General general;
    public static final Recipes recipes;
    public static final Misc misc;

    static {
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        client = new Client(clientBuilder);
        CLIENT_SPEC = clientBuilder.build();

        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        general = new General(commonBuilder);
        recipes = new Recipes(commonBuilder);
        misc = new Misc(commonBuilder);
        COMMON_SPEC = commonBuilder.build();
    }

    public static class Client {
        public final BooleanValue renderItemAmount;
        public final BooleanValue mustLookAtBlock;
        public final BooleanValue showObstructedPlace;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("Client only configs").push("client");
            renderItemAmount = builder.comment("If the amount text on how many items is in a stack in a grindstone should render")
                    .define("renderItemAmount", true);
            mustLookAtBlock = builder.comment("Must look at the block to show the amount in it")
                    .define("mustLookAtBlock", true);
            showObstructedPlace = builder.comment("If true will show the area needed when placing a HP block")
                    .define("showObstructedPlace", true);
            builder.pop();
        }
    }

    public static class Recipes {
        public final BooleanValue useSeperateGrindstoneRecipes;
        public final BooleanValue useSeperateChoppingRecipes;

        Recipes(ForgeConfigSpec.Builder builder) {
            builder.comment("Recipe behaviour. The recipes themselves are data pack recipes:",
                    "  horsepower:grinding, horsepower:hand_grinding, horsepower:chopping, horsepower:manual_chopping, horsepower:pressing",
                    "Use a data pack (or CraftTweaker) to add, change or remove them, then /reload.").push("recipes");
            useSeperateGrindstoneRecipes = builder.comment("If the separate list of recipes (horsepower:hand_grinding) should be used for the hand grindstone")
                    .define("useSeperateGrindstoneRecipes", false);
            useSeperateChoppingRecipes = builder.comment("If the separate list of recipes (horsepower:manual_chopping) should be used for the chopping block")
                    .define("useSeperateChoppingRecipes", false);
            builder.pop();
        }
    }

    public static class General {
        public final BooleanValue enableFlour;
        public final BooleanValue enableDough;
        public final BooleanValue enableHandChoppingBlock;
        public final BooleanValue removeVanillaRecipes;
        public final BooleanValue useHorseInterface;
        public final BooleanValue shouldDamageAxe;
        public final ConfigValue<List<? extends String>> choppingBlockAxes;
        public final ConfigValue<List<? extends String>> harvestable_percentage;
        public final BooleanValue choppingBlockDrop;
        public final IntValue pointsForWindup;
        public final IntValue pointsPerRotation;
        public final DoubleValue grindstoneExhaustion;
        public final DoubleValue choppingblockExhaustion;
        public final IntValue choppMultiplier;
        public final ConfigValue<List<? extends String>> grindstoneMobList;
        public final IntValue pointsForPress;
        public final IntValue pressFluidTankSize;
        public final BooleanValue useDynamicCrafting;
        public final BooleanValue useDynamicDisplay;

        General(ForgeConfigSpec.Builder builder) {
            builder.comment("General configs").push("general");
            enableFlour = builder.comment("Enables the flour item", "If disabled all related recipes will be disabled and the item is hidden", "Requires a world reload (/reload) to update recipes")
                    .define("enableFlour", true);
            enableDough = builder.comment("Enables the dough item", "If disabled all related recipes will be disabled and the item is hidden", "Requires a world reload (/reload) to update recipes")
                    .define("enableDough", true);
            enableHandChoppingBlock = builder.comment("Enables the manual chopping block", "Requires a world reload (/reload) to update recipes")
                    .define("enableHandChoppingBlock", true);
            removeVanillaRecipes = builder.comment("Removes the vanilla crafting recipes that grinds or uses grinded resources",
                            "Removes Sugar Cane -> Sugar, Bone -> Bonemeal, Wheat -> Bread, Flowers -> Dye")
                    .define("removeVanillaRecipes", false);
            useHorseInterface = builder.comment("Use the base definition of a horse, in vanilla it includes Horse, Donkey, Mule, Llama and more",
                            "If false only entries in the mob list (and the horsepower:workers entity tag) are valid")
                    .define("useHorseInterface", true);
            shouldDamageAxe = builder.comment("If the item used as an axe for the manual chopping block should be damaged")
                    .define("shouldDamageAxe", true);
            choppingBlockAxes = builder.comment("The items to use with the manual chopping block, syntax is: ",
                            "modid:item=baseAmount-chance",
                            "The baseAmount is the percentage of returned items, the chance is for getting one more output",
                            "Items in the item tag horsepower:chopping_axes are also accepted as axes")
                    .defineListAllowEmpty(List.of("choppingBlockAxes"), List::of, o -> o instanceof String);
            harvestable_percentage = builder.comment("The percentage amount for the different materials", "The syntax is harvestLevel=baseAmount-chance",
                            "The harvest level is the tier level of the axe (wood/gold 0, stone 1, iron 2, diamond 3, netherite 4)",
                            "The baseAmount is the percentage of returned items, the chance is for getting one more output")
                    .defineList("harvestable_percentage", List.of("0=25-25", "1=50-25", "2=75-25", "3=100-25", "4=125-50"), o -> o instanceof String);
            choppingBlockDrop = builder.comment("If true the manual chopping block will drop the result items",
                            "If false the manual chopping block will put the result items in it's internal inventory")
                    .define("choppingBlockDrop", true);
            pointsForWindup = builder.comment("That amount of \"points\" for the chopper to do windup and do a chop", "One lap around the chopping block is 8 \"points\"")
                    .defineInRange("pointsForWindup", 8, 1, Integer.MAX_VALUE);
            pointsPerRotation = builder.comment("The amount of points per rotation with a hand grindstone", "The points correspond to the recipes requirement of time")
                    .defineInRange("pointsPerRotation", 2, 1, Integer.MAX_VALUE);
            grindstoneExhaustion = builder.comment("The exhaustion amount that will be added to the player when using the grindstone", "If set to 0 this is disabled")
                    .defineInRange("grindstoneExhaustion", 0.1D, 0D, Double.MAX_VALUE);
            choppingblockExhaustion = builder.comment("The exhaustion amount that will be added to the player when using the chopping block", "If set to 0 this is disabled")
                    .defineInRange("choppingblockExhaustion", 0.1D, 0D, Double.MAX_VALUE);
            choppMultiplier = builder.comment("The amount of chopps the time value in the horse chopping recipes should be multiplied with if recipes isn't separated",
                            "If the recipes are separate this isn't used, instead the recipe value is used")
                    .defineInRange("choppMultiplier", 4, 1, Integer.MAX_VALUE);
            grindstoneMobList = builder.comment("Add mobs that can use the horse powered blocks", "Only mobs that can be leashed and path find are valid",
                            "Use the entity id, e.g. minecraft:cow. It can be found with /horsepower entity while looking at the mob",
                            "Entities in the entity type tag horsepower:workers are also valid")
                    .defineListAllowEmpty(List.of("grindstoneMobList"), List::of, o -> o instanceof String);
            pointsForPress = builder.comment("The amount of points that is needed for a full press")
                    .defineInRange("pointsForPress", 16, 1, Integer.MAX_VALUE);
            pressFluidTankSize = builder.comment("The tank size of the press in mb, 1000mb = 1 bucket", "Only applies to presses loaded after the change")
                    .defineInRange("pressFluidTankSize", 3000, 1, Integer.MAX_VALUE);
            useDynamicCrafting = builder.comment("If true the chopping blocks will use all logs types in the game when crafted",
                            "If false the chopping blocks will only use the vanilla logs", "If only vanilla logs are used other logs in recipe will yield oak texture")
                    .define("useDynamicCrafting", true);
            useDynamicDisplay = builder.comment("If true it will show all chopping block types in the creative tab and JEI", "If false only the vanilla wood variants will show")
                    .define("useDynamicDisplay", true);
            builder.pop();
        }
    }

    public static class Misc {
        public final BooleanValue showOreDictionaries;
        public final BooleanValue showHarvestLevel;
        public final ConfigValue<List<? extends String>> harvestTypes;

        Misc(ForgeConfigSpec.Builder builder) {
            builder.comment("Contains misc configs, mostly for debugging and dev").push("misc");
            showOreDictionaries = builder.comment("Will show all item tags of an item in the tooltip (the 1.12 ore dictionary equivalent)")
                    .define("showOreDictionaries", false);
            showHarvestLevel = builder.comment("Will show the harvest level of items in the tooltip")
                    .define("showHarvestLevel", false);
            harvestTypes = builder.comment("What harvest types to show the harvest level for (axe, pickaxe, shovel, hoe, sword)")
                    .defineList("harvestTypes", List.of("axe"), o -> o instanceof String);
            builder.pop();
        }
    }
}
