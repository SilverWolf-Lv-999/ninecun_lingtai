package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.Util;
import net.minecraft.network.chat.Style;

public final class DaoTitleGradientUtility {
    public static int getColor(Style style) {
        String insertion = style.getInsertion();
        if (insertion == null) {
            return -1;
        }
        String prefix = DaoTitleUtility.DAO_TITLE_GRADIENT_PREFIX;
        if (!insertion.startsWith(prefix)) {
            return -1;
        }
        String[] values = insertion.substring(prefix.length()).split(":");
        if (values.length != 3) {
            return -1;
        }
        try {
            int firstColor = Integer.parseInt(values[0]);
            int secondColor = Integer.parseInt(values[1]);
            int characterIndex = Integer.parseInt(values[2]);
            float phase = (Util.getMillis() % 1800L) / 1800.0F + characterIndex * 0.075F;
            float progress = 0.5F + 0.5F * (float) Math.sin(phase * (float) (Math.PI * 2.0D));
            return mix(firstColor, secondColor, progress);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static int mix(int firstColor, int secondColor, float progress) {
        int red = Math.round((firstColor >> 16 & 255) + ((secondColor >> 16 & 255) - (firstColor >> 16 & 255)) * progress);
        int green = Math.round((firstColor >> 8 & 255) + ((secondColor >> 8 & 255) - (firstColor >> 8 & 255)) * progress);
        int blue = Math.round((firstColor & 255) + ((secondColor & 255) - (firstColor & 255)) * progress);
        return red << 16 | green << 8 | blue;
    }
}
