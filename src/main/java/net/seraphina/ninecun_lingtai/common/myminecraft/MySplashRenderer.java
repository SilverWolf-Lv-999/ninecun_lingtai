package net.seraphina.ninecun_lingtai.common.myminecraft;

import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.util.Mth;

import java.util.Random;

public class MySplashRenderer extends SplashRenderer {
    private static final long POSITION_CHANGE_INTERVAL_MILLIS = 350L;
    private static final float MAX_HORIZONTAL_OFFSET = 100.0F;
    private static final float MAX_VERTICAL_OFFSET = 50.0F;
    private static final float ROTATION_DEGREES_PER_SECOND = 180.0F;

    private final Random random = new Random();
    private long lastPositionChangeTime;
    private float currentOffsetX;
    private float currentOffsetY;
    private float targetOffsetX;
    private float targetOffsetY;

    public MySplashRenderer(String pSplash) {
        super(pSplash);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pScreenWidth, Font pFont, int pColor) {
        long currentTime = Util.getMillis();
        this.updateTargetPosition(currentTime);
        float positionProgress = Mth.clamp(
                (float)(currentTime - this.lastPositionChangeTime) / POSITION_CHANGE_INTERVAL_MILLIS,
                0.0F,
                1.0F
        );
        float offsetX = Mth.lerp(positionProgress, this.currentOffsetX, this.targetOffsetX);
        float offsetY = Mth.lerp(positionProgress, this.currentOffsetY, this.targetOffsetY);
        float rotationDegrees = currentTime / 1000.0F * ROTATION_DEGREES_PER_SECOND;

        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate((float)pScreenWidth / 2.0F + 123.0F + offsetX, 69.0F + offsetY, 0.0F);
        pGuiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationDegrees));
        float scale = 1.8F - Mth.abs(Mth.sin((float)(currentTime % 1000L) / 1000.0F * ((float)Math.PI * 2F)) * 0.1F);
        scale = scale * 100.0F / (float)(pFont.width("I Love You") + 32);
        pGuiGraphics.pose().scale(scale, scale, scale);
        pGuiGraphics.drawCenteredString(pFont, "I Love You", 0, -8, 16776960 | pColor);
        pGuiGraphics.pose().popPose();
    }

    private void updateTargetPosition(long currentTime) {
        if (currentTime - this.lastPositionChangeTime < POSITION_CHANGE_INTERVAL_MILLIS) {
            return;
        }

        this.currentOffsetX = this.targetOffsetX;
        this.currentOffsetY = this.targetOffsetY;
        this.targetOffsetX = this.getRandomOffset(MAX_HORIZONTAL_OFFSET);
        this.targetOffsetY = this.getRandomOffset(MAX_VERTICAL_OFFSET);
        this.lastPositionChangeTime = currentTime;
    }

    private float getRandomOffset(float maxOffset) {
        return (this.random.nextFloat() * 2.0F - 1.0F) * maxOffset;
    }
}
