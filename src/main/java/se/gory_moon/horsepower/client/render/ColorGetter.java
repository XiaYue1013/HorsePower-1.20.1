package se.gory_moon.horsepower.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.data.ModelData;

import java.util.HashMap;
import java.util.Map;

/**
 * Replacement of the 1.12.2 {@code ColorGetter}/{@code ColorThief}: finds the dominant color of an item's texture.
 * It is used to tint the ground layer in the horse grindstone with the color of what is being ground.
 * Pixels are bucketed with 4 bits per channel, the most common bucket is averaged.
 */
public final class ColorGetter {

    private ColorGetter() {}

    /**
     * @return the dominant RGB color, or -1 if it can't be determined
     */
    public static int getColor(ItemStack stack) {
        if (stack.isEmpty())
            return -1;
        try {
            Minecraft mc = Minecraft.getInstance();
            BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);
            TextureAtlasSprite sprite = model.getParticleIcon(ModelData.EMPTY);
            int dominant = dominantColor(sprite.contents().getOriginalImage());
            if (dominant == -1)
                return -1;
            int tint = mc.getItemColors().getColor(stack, 0);
            if (tint != -1) {
                int r = ((dominant >> 16 & 0xFF) * (tint >> 16 & 0xFF)) / 255;
                int g = ((dominant >> 8 & 0xFF) * (tint >> 8 & 0xFF)) / 255;
                int b = ((dominant & 0xFF) * (tint & 0xFF)) / 255;
                dominant = r << 16 | g << 8 | b;
            }
            return dominant;
        } catch (RuntimeException | LinkageError e) {
            return -1;
        }
    }

    private static int dominantColor(NativeImage image) {
        Map<Integer, long[]> buckets = new HashMap<>();
        int best = -1;
        long bestCount = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = abgr >>> 24;
                if (a < 128)
                    continue;
                int r = abgr & 0xFF, g = abgr >> 8 & 0xFF, b = abgr >> 16 & 0xFF;
                // Skip almost white/black pixels like ColorThief does
                if (r > 250 && g > 250 && b > 250)
                    continue;
                int key = (r >> 4) << 8 | (g >> 4) << 4 | (b >> 4);
                long[] sum = buckets.computeIfAbsent(key, k -> new long[4]);
                sum[0] += r;
                sum[1] += g;
                sum[2] += b;
                sum[3]++;
                if (sum[3] > bestCount) {
                    bestCount = sum[3];
                    best = key;
                }
            }
        }
        if (best == -1)
            return -1;
        long[] sum = buckets.get(best);
        return (int) (sum[0] / sum[3]) << 16 | (int) (sum[1] / sum[3]) << 8 | (int) (sum[2] / sum[3]);
    }
}
