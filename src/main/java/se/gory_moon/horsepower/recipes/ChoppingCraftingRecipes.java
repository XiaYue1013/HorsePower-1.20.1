package se.gory_moon.horsepower.recipes;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.registries.ForgeRegistries;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.Registration;
import se.gory_moon.horsepower.blocks.HPChoppingBaseBlock;
import se.gory_moon.horsepower.util.ChoppingTexture;

/**
 * Ports of the 1.12.2 {@code ShapedChoppingRecipe} and {@code ShapelessChoppingRecipe}.
 * The crafted chopping block remembers which log (the "texture" ingredient) was used and renders with its textures.
 * A chopping block used as ingredient passes its texture on.
 */
public final class ChoppingCraftingRecipes {

    private ChoppingCraftingRecipes() {}

    static ItemStack assembleTextured(CraftingContainer container, Ingredient texture, ItemStack result) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem))
                continue;
            boolean isChopping = blockItem.getBlock() instanceof HPChoppingBaseBlock;
            if (texture.test(stack) || isChopping) {
                if (!isChopping && !Configs.general.useDynamicCrafting.get()) {
                    ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (key == null || !"minecraft".equals(key.getNamespace()))
                        return ChoppingTexture.createItemStack(result.getItem(), result.getCount(), new ItemStack(Items.OAK_LOG));
                }
                return ChoppingTexture.createItemStack(result.getItem(), result.getCount(), stack);
            }
        }
        return result.copy();
    }

    static ItemStack displayResult(Ingredient texture, ItemStack result) {
        ItemStack[] items = texture.getItems();
        if (items.length > 0)
            return ChoppingTexture.createItemStack(result.getItem(), result.getCount(), items[0]);
        return result.copy();
    }

    public static class Shaped extends ShapedRecipe {
        private final Ingredient texture;
        private final ItemStack baseResult;

        public Shaped(ShapedRecipe base, Ingredient texture) {
            super(base.getId(), base.getGroup(), base.category(), base.getWidth(), base.getHeight(), base.getIngredients(), base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
            this.texture = texture;
            this.baseResult = base.getResultItem(RegistryAccess.EMPTY);
        }

        public Ingredient getTexture() {
            return texture;
        }

        @Override
        public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
            return assembleTextured(container, texture, baseResult);
        }

        @Override
        public ItemStack getResultItem(RegistryAccess access) {
            return displayResult(texture, baseResult);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return Registration.SHAPED_CHOPPING_SERIALIZER.get();
        }
    }

    public static class Shapeless extends ShapelessRecipe {
        private final Ingredient texture;
        private final ItemStack baseResult;

        public Shapeless(ShapelessRecipe base, Ingredient texture) {
            super(base.getId(), base.getGroup(), base.category(), base.getResultItem(RegistryAccess.EMPTY), base.getIngredients());
            this.texture = texture;
            this.baseResult = base.getResultItem(RegistryAccess.EMPTY);
        }

        public Ingredient getTexture() {
            return texture;
        }

        @Override
        public ItemStack assemble(CraftingContainer container, RegistryAccess access) {
            return assembleTextured(container, texture, baseResult);
        }

        @Override
        public ItemStack getResultItem(RegistryAccess access) {
            return displayResult(texture, baseResult);
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return Registration.SHAPELESS_CHOPPING_SERIALIZER.get();
        }
    }

    public static class ShapedSerializer implements RecipeSerializer<Shaped> {
        @Override
        public Shaped fromJson(ResourceLocation id, JsonObject json) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
            return new Shaped(base, Ingredient.fromJson(GsonHelper.getNonNull(json, "texture"), false));
        }

        @Override
        public Shaped fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapedRecipe base = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
            return new Shaped(base, Ingredient.fromNetwork(buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, Shaped recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
            recipe.getTexture().toNetwork(buf);
        }
    }

    public static class ShapelessSerializer implements RecipeSerializer<Shapeless> {
        @Override
        public Shapeless fromJson(ResourceLocation id, JsonObject json) {
            ShapelessRecipe base = RecipeSerializer.SHAPELESS_RECIPE.fromJson(id, json);
            return new Shapeless(base, Ingredient.fromJson(GsonHelper.getNonNull(json, "texture"), false));
        }

        @Override
        public Shapeless fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            ShapelessRecipe base = RecipeSerializer.SHAPELESS_RECIPE.fromNetwork(id, buf);
            return new Shapeless(base, Ingredient.fromNetwork(buf));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, Shapeless recipe) {
            RecipeSerializer.SHAPELESS_RECIPE.toNetwork(buf, recipe);
            recipe.getTexture().toNetwork(buf);
        }
    }
}
