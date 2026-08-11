package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

import java.util.List;

public final class CultivationRenderUtility {
    private static final float CUSTOM_FONT_RENDER_HEIGHT = 48.0F;

    /**
     * Renders the player front-facing in the current pose. Unlike the inventory model this
     * never follows the mouse cursor: body, head, and pitch are pinned towards the viewer.
     */
    public static void renderFrontFacingPlayer(
            GuiGraphics guiGraphics,
            int centerX,
            int bottomY,
            int scale,
            LivingEntity entity
    ) {
        float previousYBodyRot = entity.yBodyRot;
        float previousYBodyRotO = entity.yBodyRotO;
        float previousYRot = entity.getYRot();
        float previousYRotO = entity.yRotO;
        float previousXRot = entity.getXRot();
        float previousXRotO = entity.xRotO;
        float previousYHeadRot = entity.yHeadRot;
        float previousYHeadRotO = entity.yHeadRotO;

        entity.yBodyRot = 180.0F;
        entity.yBodyRotO = 180.0F;
        entity.setYRot(180.0F);
        entity.yRotO = 180.0F;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;
        entity.yHeadRot = 180.0F;
        entity.yHeadRotO = 180.0F;

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraOrientation = new Quaternionf();
        InventoryScreen.renderEntityInInventory(
                guiGraphics,
                centerX,
                bottomY,
                scale,
                pose,
                cameraOrientation,
                entity
        );

        entity.yBodyRot = previousYBodyRot;
        entity.yBodyRotO = previousYBodyRotO;
        entity.setYRot(previousYRot);
        entity.yRotO = previousYRotO;
        entity.setXRot(previousXRot);
        entity.xRotO = previousXRotO;
        entity.yHeadRot = previousYHeadRot;
        entity.yHeadRotO = previousYHeadRotO;
    }

    public static void drawMeditationAura(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        float pulse = 0.82F + Mth.sin(currentTime * 0.0021F) * 0.18F;
        int ringColor = ColorUtility.withAlpha(
                TechniqueImprintRenderUtility.GOLD_COLOR,
                alpha * 0.34F * pulse
        );
        int brightColor = ColorUtility.withAlpha(
                TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR,
                alpha * 0.5F * pulse
        );
        AptitudeRenderUtility.drawRing(guiGraphics, centerX, centerY, radius, 0.9F, ringColor);
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                centerX,
                centerY,
                radius * 0.82F,
                0.6F,
                ColorUtility.withAlpha(TechniqueImprintRenderUtility.GOLD_COLOR, alpha * 0.18F)
        );
        float rotation = currentTime * 0.008F;
        for (int arcIndex = 0; arcIndex < 4; arcIndex++) {
            float start = rotation + arcIndex * 90.0F;
            AptitudeRenderUtility.drawArc(
                    guiGraphics,
                    centerX,
                    centerY,
                    radius * 1.12F,
                    start,
                    start + 34.0F,
                    0.8F,
                    ringColor
            );
        }
        for (int starIndex = 0; starIndex < 3; starIndex++) {
            float orbit = -currentTime * 0.0006F + starIndex * ((float) Math.PI * 2.0F / 3.0F);
            float starX = centerX + Mth.cos(orbit) * radius * 1.12F;
            float starY = centerY + Mth.sin(orbit) * radius * 1.12F;
            float starPulse = 0.7F + Mth.sin(currentTime * 0.003F + starIndex * 2.1F) * 0.3F;
            drawSmallStar(guiGraphics, starX, starY, 2.0F + starPulse, brightColor);
        }
    }

    public static void drawInputBoxFrame(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int width,
            int height,
            boolean focused,
            long currentTime,
            float alpha
    ) {
        if (alpha <= 0.0F) {
            return;
        }
        float pulse = focused
                ? 0.82F + Mth.sin(currentTime * 0.006F) * 0.18F
                : 0.68F;
        int lineColor = ColorUtility.withAlpha(
                focused
                        ? TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
                        : TechniqueImprintRenderUtility.GOLD_COLOR,
                alpha * pulse
        );
        int softColor = ColorUtility.withAlpha(
                TechniqueImprintRenderUtility.GOLD_COLOR,
                alpha * 0.30F
        );
        guiGraphics.fill(
                left,
                top,
                left + width,
                top + height,
                ColorUtility.withAlpha(TechniqueImprintRenderUtility.GOLD_COLOR, alpha * 0.07F)
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(left + 2.0F, top + height),
                new ScreenPoint(left + width - 2.0F, top + height),
                focused ? 1.0F : 0.8F,
                lineColor
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(left + 6.0F, top),
                new ScreenPoint(left + width - 6.0F, top),
                0.55F,
                softColor
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(left, top + 3.0F),
                new ScreenPoint(left, top + height - 1.0F),
                0.55F,
                softColor
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(left + width, top + 3.0F),
                new ScreenPoint(left + width, top + height - 1.0F),
                0.55F,
                softColor
        );
        TechniqueImprintRenderUtility.drawSeal(
                guiGraphics,
                left - 9.0F,
                top + height * 0.5F,
                3.2F,
                focused,
                false,
                currentTime,
                alpha * 0.9F
        );
        TechniqueImprintRenderUtility.drawSeal(
                guiGraphics,
                left + width + 9.0F,
                top + height * 0.5F,
                3.2F,
                focused,
                false,
                currentTime,
                alpha * 0.9F
        );
    }

    public static void drawWrappedText(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            float centerX,
            float top,
            int maximumWidth,
            float scale,
            int color,
            float alpha,
            int maximumLines
    ) {
        if (alpha <= 0.0F || maximumLines <= 0) {
            return;
        }
        int alphaColor = ColorUtility.withAlpha(color, alpha);
        List<FormattedCharSequence> lines = font.split(
                text,
                Math.max(20, (int) (maximumWidth / scale))
        );
        int lineCount = Math.min(lines.size(), maximumLines);
        float lineSpacing = (CUSTOM_FONT_RENDER_HEIGHT + 10.0F) * scale;
        PoseStack poseStack = guiGraphics.pose();
        for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
            FormattedCharSequence line = lines.get(lineIndex);
            float lineWidth = font.width(line) * scale;
            poseStack.pushPose();
            poseStack.translate(
                    centerX - lineWidth * 0.5F,
                    top + lineIndex * lineSpacing,
                    220.0F
            );
            poseStack.scale(scale, scale, 1.0F);
            guiGraphics.drawString(font, line, 0, 0, alphaColor, false);
            poseStack.popPose();
        }
    }

    private static void drawSmallStar(
            GuiGraphics guiGraphics,
            float centerX,
            float centerY,
            float radius,
            int color
    ) {
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(centerX - radius, centerY),
                new ScreenPoint(centerX + radius, centerY),
                0.6F,
                color
        );
        AptitudeRenderUtility.drawLine(
                guiGraphics,
                new ScreenPoint(centerX, centerY - radius),
                new ScreenPoint(centerX, centerY + radius),
                0.6F,
                color
        );
    }
}
