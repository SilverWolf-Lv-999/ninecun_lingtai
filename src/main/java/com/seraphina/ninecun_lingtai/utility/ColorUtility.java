package com.seraphina.ninecun_lingtai.utility;

import net.minecraft.util.Mth;

import java.util.Collection;

public final class ColorUtility {

    public static int withAlpha(int color, float alpha) {
        int alphaChannel = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        return alphaChannel << 24 | color & 0x00FFFFFF;
    }

    public static int brighten(int color, float amount) {
        return blend(color, 0xFFFFFF, amount);
    }

    public static int darken(int color, float amount) {
        return blend(color, 0x000000, amount);
    }

    public static int blend(int firstColor, int secondColor, float secondWeight) {
        float clampedWeight = Mth.clamp(secondWeight, 0.0F, 1.0F);
        float firstWeight = 1.0F - clampedWeight;
        int red = Math.round(((firstColor >> 16) & 0xFF) * firstWeight
                + ((secondColor >> 16) & 0xFF) * clampedWeight);
        int green = Math.round(((firstColor >> 8) & 0xFF) * firstWeight
                + ((secondColor >> 8) & 0xFF) * clampedWeight);
        int blue = Math.round((firstColor & 0xFF) * firstWeight
                + (secondColor & 0xFF) * clampedWeight);
        return red << 16 | green << 8 | blue;
    }

    public static int mix(Collection<Integer> colors) {
        if (colors.isEmpty()) {
            throw new IllegalArgumentException("At least one color is required");
        }
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        for (int color : colors) {
            red += color >> 16 & 0xFF;
            green += color >> 8 & 0xFF;
            blue += color & 0xFF;
        }
        int colorCount = colors.size();
        return (int) (red / colorCount) << 16
                | (int) (green / colorCount) << 8
                | (int) (blue / colorCount);
    }
}
