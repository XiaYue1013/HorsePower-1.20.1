package se.gory_moon.horsepower.recipes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * JSON / network serializers of the machine recipes.
 * <p>
 * Formats:
 * <pre>
 * grinding / hand_grinding: { "ingredient": ingredient, "result": item, "time": int, ["secondary": item, "secondary_chance": 1-100] }
 * chopping / manual_chopping: { "ingredient": ingredient, "result": item, "time": int }
 * pressing: { "ingredient": ingredient, ["count": int], "result": item | "fluid": { "fluid": id, "amount": int, ["nbt": tag] } }
 * </pre>
 */
public final class HPRecipeSerializers {

    private HPRecipeSerializers() {}

    static Ingredient readIngredient(JsonObject json) {
        if (!json.has("ingredient"))
            throw new JsonParseException("Missing required \"ingredient\"");
        Ingredient ingredient = Ingredient.fromJson(json.get("ingredient"), false);
        if (ingredient.isEmpty())
            throw new JsonParseException("The \"ingredient\" doesn't match any item");
        return ingredient;
    }

    static ItemStack readItem(JsonObject json, String key) {
        ItemStack stack = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, key), true, true);
        if (stack.isEmpty())
            throw new JsonParseException("\"" + key + "\" can't be empty");
        return stack;
    }

    static int readTime(JsonObject json) {
        int time = GsonHelper.getAsInt(json, "time");
        if (time < 0)
            throw new JsonParseException("\"time\" can't be negative, was " + time);
        return time;
    }

    static FluidStack readFluid(JsonObject json) {
        ResourceLocation fluidId = new ResourceLocation(GsonHelper.getAsString(json, "fluid"));
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY)
            throw new JsonParseException("Unknown fluid '" + fluidId + "'");
        int amount = GsonHelper.getAsInt(json, "amount", 1000);
        if (amount <= 0)
            throw new JsonParseException("Fluid \"amount\" must be positive, was " + amount);
        FluidStack stack = new FluidStack(fluid, amount);
        if (json.has("nbt"))
            stack.setTag(CraftingHelper.getNBT(json.get("nbt")));
        return stack;
    }

    public static class Grinding implements RecipeSerializer<GrindstoneRecipe> {
        private final boolean hand;

        public Grinding(boolean hand) {
            this.hand = hand;
        }

        @Override
        public GrindstoneRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = readIngredient(json);
            ItemStack output = readItem(json, "result");
            int time = readTime(json);
            ItemStack secondary = ItemStack.EMPTY;
            int chance = 0;
            if (json.has("secondary")) {
                secondary = readItem(json, "secondary");
                chance = GsonHelper.getAsInt(json, "secondary_chance", 100);
                // Same rule as the 1.12.2 config parser: a secondary output with 0% chance is invalid
                if (chance <= 0 || chance > 100)
                    throw new JsonParseException("\"secondary_chance\" must be between 1 and 100, was " + chance);
            }
            return new GrindstoneRecipe(id, hand, input, output, secondary, chance, time);
        }

        @Override
        public GrindstoneRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            ItemStack secondary = buf.readItem();
            int chance = buf.readVarInt();
            int time = buf.readVarInt();
            return new GrindstoneRecipe(id, hand, input, output, secondary, chance, time);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, GrindstoneRecipe recipe) {
            recipe.getInput().toNetwork(buf);
            buf.writeItem(recipe.getOutput());
            buf.writeItem(recipe.getSecondary());
            buf.writeVarInt(recipe.getSecondaryChance());
            buf.writeVarInt(recipe.getTime());
        }
    }

    public static class Chopping implements RecipeSerializer<ChoppingRecipe> {
        private final boolean manual;

        public Chopping(boolean manual) {
            this.manual = manual;
        }

        @Override
        public ChoppingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ChoppingRecipe(id, manual, readIngredient(json), readItem(json, "result"), readTime(json));
        }

        @Override
        public ChoppingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            int time = buf.readVarInt();
            return new ChoppingRecipe(id, manual, input, output, time);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ChoppingRecipe recipe) {
            recipe.getInput().toNetwork(buf);
            buf.writeItem(recipe.getOutput());
            buf.writeVarInt(recipe.getTime());
        }
    }

    public static class Pressing implements RecipeSerializer<PressRecipe> {
        @Override
        public PressRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = readIngredient(json);
            int count = GsonHelper.getAsInt(json, "count", 1);
            if (count < 1 || count > 64)
                throw new JsonParseException("\"count\" must be between 1 and 64, was " + count);
            boolean hasItem = json.has("result");
            boolean hasFluid = json.has("fluid");
            if (hasItem == hasFluid)
                throw new JsonParseException("A pressing recipe needs exactly one of \"result\" or \"fluid\"");
            ItemStack output = hasItem ? readItem(json, "result") : ItemStack.EMPTY;
            FluidStack fluid = hasFluid ? readFluid(GsonHelper.getAsJsonObject(json, "fluid")) : FluidStack.EMPTY;
            return new PressRecipe(id, input, count, output, fluid);
        }

        @Override
        public PressRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int count = buf.readVarInt();
            ItemStack output = buf.readItem();
            FluidStack fluid = FluidStack.readFromPacket(buf);
            return new PressRecipe(id, input, count, output, fluid);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, PressRecipe recipe) {
            recipe.getInput().toNetwork(buf);
            buf.writeVarInt(recipe.getInputCount());
            buf.writeItem(recipe.getOutput());
            recipe.getOutputFluid().writeToPacket(buf);
        }
    }
}
