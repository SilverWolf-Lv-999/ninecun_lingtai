package net.seraphina.ninecun_lingtai.client.utility;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Set;

public final class FiveElementWheelRenderUtility {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 72;
    private static final float CENTER_X = WIDTH * 0.5F;
    private static final float CENTER_Y = HEIGHT * 0.5F;
    private static final float FORMATION_RADIUS = 27.0F;
    private static final float ELEMENT_RADIUS = 24.0F;
    private static final float NODE_RADIUS = 5.0F;
    private static final float TAIJI_RADIUS = 5.0F;
    private static final float ELEMENT_TEXT_SCALE = 0.82F;

    public static int getWidth() {
        return WIDTH;
    }

    public static int getHeight() {
        return HEIGHT;
    }

    public static void render(
            GuiGraphics guiGraphics,
            Font font,
            int x,
            int y,
            Set<ElementType> selectedElements
    ) {
        long currentTime = Util.getMillis();
        float centerX = x + CENTER_X;
        float centerY = y + CENTER_Y;
        AptitudeFormationRenderUtility.drawFormation(
                guiGraphics,
                centerX,
                centerY,
                FORMATION_RADIUS,
                currentTime,
                0.86F
        );
        for (int index = 0; index < SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.size(); index++) {
            ElementType element = SpiritualRootUtility.FIVE_ELEMENT_DIAGRAM_ORDER.get(index);
            boolean selected = selectedElements.contains(element);
            int baseColor = SpiritualRootUtility.getElementColor(element);
            ScreenPoint position = getElementPosition(index, centerX, centerY);
            AptitudeFormationRenderUtility.drawNodeSeal(
                    guiGraphics,
                    position,
                    NODE_RADIUS,
                    element,
                    baseColor,
                    selected,
                    currentTime,
                    selected ? 0.9F : 0.58F
            );
            int textColor = selected
                    ? ColorUtility.brighten(baseColor, 0.28F)
                    : ColorUtility.darken(baseColor, 0.42F);
            AptitudeRenderUtility.drawCenteredText(
                    guiGraphics,
                    font,
                    Component.literal(SpiritualRootUtility.getElementName(element)),
                    position.x(),
                    position.y(),
                    ELEMENT_TEXT_SCALE,
                    ColorUtility.withAlpha(textColor, selected ? 1.0F : 0.82F),
                    0x070A10,
                    selected ? 0.76F : 0.6F
            );
        }
        AptitudeRenderUtility.drawTaiji(
                guiGraphics,
                centerX,
                centerY,
                TAIJI_RADIUS,
                currentTime * 0.012F,
                0.9F,
                0.9F
        );
    }

    private static ScreenPoint getElementPosition(int elementIndex, float centerX, float centerY) {
        float angle = (float) Math.toRadians(-90.0F + elementIndex * 72.0F);
        return new ScreenPoint(
                centerX + Mth.cos(angle) * ELEMENT_RADIUS,
                centerY + Mth.sin(angle) * ELEMENT_RADIUS
        );
    }
}
