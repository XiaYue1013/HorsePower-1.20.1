package se.gory_moon.horsepower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;
import se.gory_moon.horsepower.Configs;
import se.gory_moon.horsepower.HorsePower;
import se.gory_moon.horsepower.blocks.ChopperBlock;
import se.gory_moon.horsepower.blocks.GrindstoneBlock;
import se.gory_moon.horsepower.blocks.HandGrindstoneBlock;
import se.gory_moon.horsepower.blocks.PressBlock;
import se.gory_moon.horsepower.client.render.HPRenderHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Client only game events: port of the 1.12.2 {@code ClientHandler}, {@code HorsePowerCommand} and the tooltip
 * part of {@code HPEventHandler}.
 */
@Mod.EventBusSubscriber(modid = HorsePower.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {}

    private static boolean isHorseBlock(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item))
            return false;
        Block block = item.getBlock();
        return block instanceof GrindstoneBlock || block instanceof ChopperBlock || block instanceof PressBlock;
    }

    /**
     * Shows the needed 7x7x2 area while holding a horse powered block (1.12.2 "Show Obstructed Area").
     */
    @SubscribeEvent
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !Configs.client.showObstructedPlace.get())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;
        ItemStack held = ItemStack.EMPTY;
        for (ItemStack stack : mc.player.getHandSlots()) {
            if (isHorseBlock(stack)) {
                held = stack;
                break;
            }
        }
        if (held.isEmpty() || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return;

        int offset = ((BlockItem) held.getItem()).getBlock() instanceof GrindstoneBlock ? -1 : 0;
        BlockPos pos = hit.getBlockPos();
        if (!mc.level.getBlockState(pos).canBeReplaced())
            pos = pos.relative(hit.getDirection());
        if (offset == 0 && !mc.level.getBlockState(pos.above()).canBeReplaced())
            pos = pos.below();

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        HPRenderHelper.renderUsedArea(mc.level, pos, offset, 0.15F, 0.05F, pose, buffers);
        pose.popPose();
        buffers.endBatch();
    }

    /**
     * The hand grindstone has no selection box (1.12.2 returned an empty selected bounding box).
     */
    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockState(event.getTarget().getBlockPos()).getBlock() instanceof HandGrindstoneBlock)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> entity = Commands.literal("entity").executes(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                ctx.getSource().sendFailure(Component.translatable("commands.horsepower.entity.invalid"));
                return 0;
            }
            if (mc.hitResult instanceof EntityHitResult hit) {
                Entity target = hit.getEntity();
                ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
                String name = String.valueOf(id);
                mc.keyboardHandler.setClipboard(name);
                ctx.getSource().sendSuccess(() -> Component.translatable("commands.horsepower.entity.has", name), false);
                return Command.SINGLE_SUCCESS;
            }
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.horsepower.entity.no"), false);
            return 0;
        });
        LiteralArgumentBuilder<CommandSourceStack> reload = Commands.literal("reload").executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.horsepower.reload").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), false);
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.horsepower.reload.noerror").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
            return Command.SINGLE_SUCCESS;
        });
        event.getDispatcher().register(Commands.literal("horsepower").then(entity).then(reload));
        event.getDispatcher().register(Commands.literal("hp").then(entity).then(reload));
    }

    private static ToolAction harvestAction(String type) {
        return switch (type) {
            case "pickaxe" -> ToolActions.PICKAXE_DIG;
            case "shovel" -> ToolActions.SHOVEL_DIG;
            case "hoe" -> ToolActions.HOE_DIG;
            case "sword" -> ToolActions.SWORD_DIG;
            default -> ToolActions.AXE_DIG;
        };
    }

    /**
     * The debug tooltips of the "misc" config: item tags (the 1.12.2 ore dictionary) and harvest levels.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onToolTip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty())
            return;
        List<Component> tooltip = event.getToolTip();
        String part = "";
        if (Configs.misc.showOreDictionaries.get()) {
            String tags = stack.getTags().map(TagKey::location).map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "));
            if (!tags.isEmpty()) {
                tooltip.add(Component.literal("Tags: ").withStyle(ChatFormatting.GRAY).append(Component.literal(tags).withStyle(ChatFormatting.GOLD)));
                part = "Tags";
            }
        }
        if (Configs.misc.showHarvestLevel.get()) {
            boolean added = false;
            for (String type : Configs.misc.harvestTypes.get()) {
                if (stack.canPerformAction(harvestAction(type))) {
                    int level = stack.getItem() instanceof TieredItem tiered ? tiered.getTier().getLevel() : 0;
                    tooltip.add(Component.literal("HarvestLevel: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(StringUtils.capitalize(type) + " (" + level + ")").withStyle(ChatFormatting.GOLD)));
                    if (!added) {
                        part += (!part.isEmpty() ? " and " : "") + "HarvestLevel";
                        added = true;
                    }
                }
            }
        }
        if (!part.isEmpty())
            tooltip.add(Component.literal("The " + part + " tooltip was added by HorsePower, to disabled check the config.").withStyle(ChatFormatting.GRAY));
    }
}
