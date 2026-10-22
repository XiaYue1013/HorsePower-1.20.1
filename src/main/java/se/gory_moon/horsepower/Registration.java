package se.gory_moon.horsepower;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import se.gory_moon.horsepower.blockentity.ChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.FillerBlockEntity;
import se.gory_moon.horsepower.blockentity.GrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.HandGrindstoneBlockEntity;
import se.gory_moon.horsepower.blockentity.ManualChopperBlockEntity;
import se.gory_moon.horsepower.blockentity.PressBlockEntity;
import se.gory_moon.horsepower.blocks.ChopperBlock;
import se.gory_moon.horsepower.blocks.ChoppingBlockBlock;
import se.gory_moon.horsepower.blocks.FillerBlock;
import se.gory_moon.horsepower.blocks.GrindstoneBlock;
import se.gory_moon.horsepower.blocks.HandGrindstoneBlock;
import se.gory_moon.horsepower.blocks.PressBlock;
import se.gory_moon.horsepower.items.DoubleBlockItem;
import se.gory_moon.horsepower.recipes.ChoppingCraftingRecipes;
import se.gory_moon.horsepower.recipes.ChoppingRecipe;
import se.gory_moon.horsepower.recipes.GrindstoneRecipe;
import se.gory_moon.horsepower.recipes.HPRecipeSerializers;
import se.gory_moon.horsepower.recipes.PressRecipe;
import se.gory_moon.horsepower.util.ChoppingTexture;

/**
 * All registrations. The registry names are the 1.12.2 ones ({@code horsepower:hand_grindstone}, {@code grindstone},
 * {@code chopping_block}, {@code chopper}, {@code chopper_filler}, {@code press}, {@code press_filler}, {@code flour},
 * {@code dough}).
 */
public final class Registration {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, HorsePower.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HorsePower.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, HorsePower.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, HorsePower.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, HorsePower.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HorsePower.MODID);

    // Blocks
    public static final RegistryObject<HandGrindstoneBlock> HAND_GRINDSTONE = BLOCKS.register("hand_grindstone", () -> new HandGrindstoneBlock(stone()));
    public static final RegistryObject<GrindstoneBlock> GRINDSTONE = BLOCKS.register("grindstone", () -> new GrindstoneBlock(stone()));
    public static final RegistryObject<ChoppingBlockBlock> CHOPPING_BLOCK = BLOCKS.register("chopping_block", () -> new ChoppingBlockBlock(wood(2.0F, 5.0F)));
    public static final RegistryObject<ChopperBlock> CHOPPER = BLOCKS.register("chopper", () -> new ChopperBlock(wood(5.0F, 5.0F)));
    public static final RegistryObject<FillerBlock> CHOPPER_FILLER = BLOCKS.register("chopper_filler", () -> new FillerBlock(wood(5.0F, 5.0F).noLootTable()));
    public static final RegistryObject<PressBlock> PRESS = BLOCKS.register("press", () -> new PressBlock(wood(5.0F, 5.0F)));
    public static final RegistryObject<FillerBlock> PRESS_FILLER = BLOCKS.register("press_filler", () -> new FillerBlock(wood(5.0F, 5.0F).noLootTable()));

    // Items
    public static final RegistryObject<Item> FLOUR = ITEMS.register("flour", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DOUGH = ITEMS.register("dough", () -> new Item(new Item.Properties()));
    public static final RegistryObject<BlockItem> HAND_GRINDSTONE_ITEM = ITEMS.register("hand_grindstone", () -> new BlockItem(HAND_GRINDSTONE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> GRINDSTONE_ITEM = ITEMS.register("grindstone", () -> new BlockItem(GRINDSTONE.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> CHOPPING_BLOCK_ITEM = ITEMS.register("chopping_block", () -> new BlockItem(CHOPPING_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockItem> CHOPPER_ITEM = ITEMS.register("chopper", () -> new DoubleBlockItem(CHOPPER.get(), CHOPPER_FILLER, new Item.Properties()));
    public static final RegistryObject<BlockItem> PRESS_ITEM = ITEMS.register("press", () -> new DoubleBlockItem(PRESS.get(), PRESS_FILLER, new Item.Properties()));

    // Block entities (1.12.2 used horsepower:HandGrindstone etc., upper case is not valid anymore)
    public static final RegistryObject<BlockEntityType<HandGrindstoneBlockEntity>> HAND_GRINDSTONE_BE = BLOCK_ENTITIES.register("hand_grindstone",
            () -> BlockEntityType.Builder.of(HandGrindstoneBlockEntity::new, HAND_GRINDSTONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<GrindstoneBlockEntity>> GRINDSTONE_BE = BLOCK_ENTITIES.register("grindstone",
            () -> BlockEntityType.Builder.of(GrindstoneBlockEntity::new, GRINDSTONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<ManualChopperBlockEntity>> MANUAL_CHOPPER_BE = BLOCK_ENTITIES.register("manual_chopper",
            () -> BlockEntityType.Builder.of(ManualChopperBlockEntity::new, CHOPPING_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<ChopperBlockEntity>> CHOPPER_BE = BLOCK_ENTITIES.register("chopper",
            () -> BlockEntityType.Builder.of(ChopperBlockEntity::new, CHOPPER.get()).build(null));
    public static final RegistryObject<BlockEntityType<FillerBlockEntity>> FILLER_BE = BLOCK_ENTITIES.register("filler",
            () -> BlockEntityType.Builder.of(FillerBlockEntity::new, CHOPPER_FILLER.get(), PRESS_FILLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<PressBlockEntity>> PRESS_BE = BLOCK_ENTITIES.register("press",
            () -> BlockEntityType.Builder.of(PressBlockEntity::new, PRESS.get()).build(null));

    // Recipes
    public static final RegistryObject<RecipeType<GrindstoneRecipe>> GRINDING_TYPE = recipeType("grinding");
    public static final RegistryObject<RecipeType<GrindstoneRecipe>> HAND_GRINDING_TYPE = recipeType("hand_grinding");
    public static final RegistryObject<RecipeType<ChoppingRecipe>> CHOPPING_TYPE = recipeType("chopping");
    public static final RegistryObject<RecipeType<ChoppingRecipe>> MANUAL_CHOPPING_TYPE = recipeType("manual_chopping");
    public static final RegistryObject<RecipeType<PressRecipe>> PRESSING_TYPE = recipeType("pressing");

    public static final RegistryObject<RecipeSerializer<GrindstoneRecipe>> GRINDING_SERIALIZER = RECIPE_SERIALIZERS.register("grinding", () -> new HPRecipeSerializers.Grinding(false));
    public static final RegistryObject<RecipeSerializer<GrindstoneRecipe>> HAND_GRINDING_SERIALIZER = RECIPE_SERIALIZERS.register("hand_grinding", () -> new HPRecipeSerializers.Grinding(true));
    public static final RegistryObject<RecipeSerializer<ChoppingRecipe>> CHOPPING_SERIALIZER = RECIPE_SERIALIZERS.register("chopping", () -> new HPRecipeSerializers.Chopping(false));
    public static final RegistryObject<RecipeSerializer<ChoppingRecipe>> MANUAL_CHOPPING_SERIALIZER = RECIPE_SERIALIZERS.register("manual_chopping", () -> new HPRecipeSerializers.Chopping(true));
    public static final RegistryObject<RecipeSerializer<PressRecipe>> PRESSING_SERIALIZER = RECIPE_SERIALIZERS.register("pressing", HPRecipeSerializers.Pressing::new);
    public static final RegistryObject<RecipeSerializer<ChoppingCraftingRecipes.Shaped>> SHAPED_CHOPPING_SERIALIZER = RECIPE_SERIALIZERS.register("shaped_chopping", ChoppingCraftingRecipes.ShapedSerializer::new);
    public static final RegistryObject<RecipeSerializer<ChoppingCraftingRecipes.Shapeless>> SHAPELESS_CHOPPING_SERIALIZER = RECIPE_SERIALIZERS.register("shapeless_chopping", ChoppingCraftingRecipes.ShapelessSerializer::new);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_TABS.register("horsepower", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.horsepower"))
            .icon(() -> new ItemStack(HAND_GRINDSTONE.get()))
            .displayItems((params, output) -> {
                output.accept(HAND_GRINDSTONE_ITEM.get());
                output.accept(GRINDSTONE_ITEM.get());
                // One chopping block per log type, as getSubBlocks did in 1.12.2
                params.holders().lookup(Registries.ITEM).flatMap(items -> items.get(ItemTags.LOGS)).ifPresent(logs -> logs.forEach(log -> {
                    ResourceLocation key = log.unwrapKey().map(k -> k.location()).orElse(null);
                    if (key == null || !ChoppingTexture.isDisplayLog(key, Configs.general.useDynamicDisplay.get()))
                        return;
                    ItemStack logStack = new ItemStack(log.value());
                    if (Configs.general.enableHandChoppingBlock.get())
                        output.accept(ChoppingTexture.createItemStack(CHOPPING_BLOCK_ITEM.get(), 1, logStack));
                    output.accept(ChoppingTexture.createItemStack(CHOPPER_ITEM.get(), 1, logStack));
                }));
                output.accept(PRESS_ITEM.get());
                if (Configs.general.enableFlour.get())
                    output.accept(FLOUR.get());
                if (Configs.general.enableDough.get())
                    output.accept(DOUGH.get());
            })
            .build());

    private Registration() {}

    private static BlockBehaviour.Properties stone() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 10.0F).requiresCorrectToolForDrops().sound(SoundType.STONE).noOcclusion();
    }

    private static BlockBehaviour.Properties wood(float hardness, float resistance) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(hardness, resistance).sound(SoundType.WOOD).noOcclusion();
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RegistryObject<RecipeType<T>> recipeType(String name) {
        return RECIPE_TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(HorsePower.MODID, name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        CREATIVE_TABS.register(modBus);
    }
}
