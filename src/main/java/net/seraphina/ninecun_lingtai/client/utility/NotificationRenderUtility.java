package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.client.notification.ClientNotificationManager;
import net.seraphina.ninecun_lingtai.client.notification.ClientNotificationManager.NotificationView;
import net.seraphina.ninecun_lingtai.client.render.NotificationScrollShader;
import net.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NotificationRenderUtility {
    private static final int PANEL_HEIGHT = 112;
    private static final int ROLLER_WIDTH = 24;
    private static final int SCREEN_MARGIN = 8;
    private static final int MINIMUM_SLAT_COUNT = 4;
    private static final int DEFAULT_MAXIMUM_ROWS = 6;
    private static final float CUSTOM_FONT_RENDER_HEIGHT = 48.0F;
    private static final float DEFAULT_FONT_SCALE = 0.23F;
    private static final float DEFAULT_SLAT_WIDTH = 16.0F;
    private static final float CHARACTER_ADVANCE = 53.0F;
    private static final float VIEW_ANGLE_DEGREES = 80.0F;
    private static final int INK_COLOR = 0x3A291A;

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
            return;
        }
        Optional<NotificationView> current = ClientNotificationManager.getCurrent(event.getPartialTick());
        if (current.isEmpty()) {
            return;
        }

        NotificationView notification = current.get();
        float opacity = NotificationAnimationUtility.getOpacity(
                notification.ageTicks(),
                notification.durationTicks()
        );
        if (opacity <= 0.001F) {
            return;
        }

        TextLayout textLayout = createTextLayout(notification, event.getWindow().getGuiScaledWidth());
        float unrollProgress = NotificationAnimationUtility.getUnrollProgress(
                notification.ageTicks(),
                notification.durationTicks()
        );
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        float centerX = screenWidth * 0.5F;
        float top = Math.max(10.0F, screenHeight * 0.19F - PANEL_HEIGHT * 0.5F);
        float panelWidth = textLayout.paperWidth() + ROLLER_WIDTH * 2.0F;

        drawScroll(
                event.getGuiGraphics(),
                centerX,
                top,
                panelWidth,
                textLayout.slatCount(),
                unrollProgress,
                opacity
        );
        drawText(
                event.getGuiGraphics(),
                minecraft.font,
                notification,
                textLayout,
                centerX,
                top,
                unrollProgress,
                opacity
        );
    }

    private static TextLayout createTextLayout(NotificationView notification, int screenWidth) {
        List<TextColumn> titleColumns = VerticalTextUtility
                .splitIntoColumns(notification.title().getString(), DEFAULT_MAXIMUM_ROWS)
                .stream()
                .map(symbols -> new TextColumn(symbols, true))
                .toList();
        List<TextColumn> contentColumns = VerticalTextUtility
                .splitIntoColumns(notification.content().getString(), DEFAULT_MAXIMUM_ROWS)
                .stream()
                .map(symbols -> new TextColumn(symbols, false))
                .toList();
        List<TextColumn> columns = new ArrayList<>(titleColumns.size() + contentColumns.size());
        columns.addAll(titleColumns);
        columns.addAll(contentColumns);

        int slatCount = Math.max(MINIMUM_SLAT_COUNT, columns.size() + 1);
        float requiredPaperWidth = slatCount * DEFAULT_SLAT_WIDTH;
        float maximumPaperWidth = Math.max(
                MINIMUM_SLAT_COUNT * DEFAULT_SLAT_WIDTH,
                screenWidth - SCREEN_MARGIN * 2.0F - ROLLER_WIDTH * 2.0F
        );
        float paperWidth = Math.min(requiredPaperWidth, maximumPaperWidth);
        float slatWidth = paperWidth / slatCount;
        float layoutScale = slatWidth / DEFAULT_SLAT_WIDTH;
        int glyphCount = columns.stream().mapToInt(column -> column.symbols().size()).sum();
        return new TextLayout(
                List.copyOf(columns),
                paperWidth,
                slatCount,
                slatWidth,
                layoutScale,
                glyphCount
        );
    }

    private static void drawScroll(
            GuiGraphics guiGraphics,
            float centerX,
            float top,
            float panelWidth,
            int slatCount,
            float unrollProgress,
            float opacity
    ) {
        ShaderInstance shaderInstance = NotificationScrollShader.getNotificationScrollShader();
        if (shaderInstance == null) {
            drawFallbackScroll(guiGraphics, centerX, top, panelWidth, unrollProgress, opacity);
            return;
        }

        float left = centerX - panelWidth * 0.5F;
        float right = centerX + panelWidth * 0.5F;
        float bottom = top + PANEL_HEIGHT;
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("PanelSize").set(panelWidth, (float) PANEL_HEIGHT);
        shaderInstance.safeGetUniform("SlatCount").set((float) slatCount);
        shaderInstance.safeGetUniform("ViewAngleDegrees").set(VIEW_ANGLE_DEGREES);
        shaderInstance.safeGetUniform("UnrollProgress").set(unrollProgress);
        shaderInstance.safeGetUniform("Opacity").set(opacity);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int color = 0xFFFFFFFF;
        vertex(bufferBuilder, matrix, left, top, color, 0.0F, 0.0F);
        vertex(bufferBuilder, matrix, left, bottom, color, 0.0F, 1.0F);
        vertex(bufferBuilder, matrix, right, bottom, color, 1.0F, 1.0F);
        vertex(bufferBuilder, matrix, right, top, color, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawFallbackScroll(
            GuiGraphics guiGraphics,
            float centerX,
            float top,
            float panelWidth,
            float unrollProgress,
            float opacity
    ) {
        float paperWidth = Math.max(0.0F, panelWidth - ROLLER_WIDTH * 2.0F) * unrollProgress;
        int paperLeft = Math.round(centerX - paperWidth * 0.5F);
        int paperRight = Math.round(centerX + paperWidth * 0.5F);
        int topEdge = Math.round(top + 9.0F);
        int bottomEdge = Math.round(top + PANEL_HEIGHT - 9.0F);
        int paperColor = ColorUtility.withAlpha(0xF2E7C5, opacity);
        int edgeColor = ColorUtility.withAlpha(0xB9955E, opacity);
        guiGraphics.fill(paperLeft, topEdge, paperRight, bottomEdge, paperColor);
        guiGraphics.fill(paperLeft, topEdge, paperRight, topEdge + 2, edgeColor);
        guiGraphics.fill(paperLeft, bottomEdge - 2, paperRight, bottomEdge, edgeColor);
        int rollerHalf = ROLLER_WIDTH / 2;
        int leftRollerCenter = Math.round(centerX - paperWidth * 0.5F - rollerHalf + 3.0F);
        int rightRollerCenter = Math.round(centerX + paperWidth * 0.5F + rollerHalf - 3.0F);
        guiGraphics.fill(
                leftRollerCenter - rollerHalf,
                Math.round(top + 2.0F),
                leftRollerCenter + rollerHalf,
                Math.round(top + PANEL_HEIGHT - 2.0F),
                edgeColor
        );
        guiGraphics.fill(
                rightRollerCenter - rollerHalf,
                Math.round(top + 2.0F),
                rightRollerCenter + rollerHalf,
                Math.round(top + PANEL_HEIGHT - 2.0F),
                edgeColor
        );
    }

    private static void drawText(
            GuiGraphics guiGraphics,
            Font font,
            NotificationView notification,
            TextLayout textLayout,
            float centerX,
            float top,
            float unrollProgress,
            float opacity
    ) {
        if (unrollProgress <= 0.001F || textLayout.glyphCount() <= 0) {
            return;
        }
        float visiblePaperWidth = textLayout.paperWidth() * unrollProgress;
        float topPerspectiveScale = getPerspectiveScale(0.0F);
        int scissorLeft = Math.round(centerX - visiblePaperWidth * topPerspectiveScale * 0.5F + 2.0F);
        int scissorRight = Math.round(centerX + visiblePaperWidth * topPerspectiveScale * 0.5F - 2.0F);
        if (scissorRight <= scissorLeft) {
            return;
        }
        guiGraphics.enableScissor(
                scissorLeft,
                Math.round(top + 10.0F),
                scissorRight,
                Math.round(top + PANEL_HEIGHT - 10.0F)
        );

        float revealProgress = NotificationAnimationUtility.getTextRevealProgress(notification.ageTicks());
        float revealedGlyphs = revealProgress * textLayout.glyphCount();
        float fontScale = DEFAULT_FONT_SCALE * textLayout.layoutScale();
        float paperRight = centerX + textLayout.paperWidth() * 0.5F;
        float characterTop = top + (PANEL_HEIGHT - DEFAULT_MAXIMUM_ROWS * CHARACTER_ADVANCE * fontScale) * 0.5F;
        int glyphIndex = 0;
        int columnIndex = 0;
        for (TextColumn column : textLayout.columns()) {
            float flatColumnX = paperRight - (columnIndex + 0.5F) * textLayout.slatWidth();
            for (int rowIndex = 0; rowIndex < column.symbols().size(); rowIndex++) {
                float glyphReveal = Mth.clamp(revealedGlyphs - glyphIndex, 0.0F, 1.0F);
                if (glyphReveal > 0.001F) {
                    float glyphY = characterTop + rowIndex * CHARACTER_ADVANCE * fontScale;
                    float verticalProgress = Mth.clamp((glyphY - top) / PANEL_HEIGHT, 0.0F, 1.0F);
                    float perspectiveScale = getPerspectiveScale(verticalProgress);
                    float columnX = centerX + (flatColumnX - centerX) * perspectiveScale;
                    Component glyph = Component.literal(column.symbols().get(rowIndex))
                            .withStyle(style -> style
                                    .withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
                                    .withBold(column.title()));
                    drawGlyph(
                            guiGraphics,
                            font,
                            glyph,
                            columnX,
                            glyphY,
                            fontScale,
                            perspectiveScale,
                            opacity * glyphReveal
                    );
                }
                glyphIndex++;
            }
            columnIndex++;
        }
        guiGraphics.disableScissor();
    }

    private static void drawGlyph(
            GuiGraphics guiGraphics,
            Font font,
            Component glyph,
            float x,
            float y,
            float scale,
            float perspectiveScale,
            float opacity
    ) {
        int color = ColorUtility.withAlpha(INK_COLOR, opacity);
        float horizontalScale = scale * perspectiveScale;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x - font.width(glyph) * horizontalScale * 0.5F, y, 1.0F);
        guiGraphics.pose().scale(horizontalScale, scale, 1.0F);
        guiGraphics.drawString(font, glyph, 0, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private static float getPerspectiveScale(float verticalProgress) {
        float tilt = Mth.cos(VIEW_ANGLE_DEGREES * Mth.DEG_TO_RAD);
        float topScale = 1.0F - tilt * 0.28F;
        return Mth.lerp(Mth.clamp(verticalProgress, 0.0F, 1.0F), topScale, 1.0F);
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            int color,
            float textureU,
            float textureV
    ) {
        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .endVertex();
    }

    private record TextColumn(List<String> symbols, boolean title) {
    }

    private record TextLayout(
            List<TextColumn> columns,
            float paperWidth,
            int slatCount,
            float slatWidth,
            float layoutScale,
            int glyphCount
    ) {
    }
}
