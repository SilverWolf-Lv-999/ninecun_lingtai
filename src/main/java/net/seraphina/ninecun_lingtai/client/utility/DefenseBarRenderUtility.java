package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.client.player.ClientDefenseResourceManager;
import net.seraphina.ninecun_lingtai.client.render.DefenseBarShader;
import net.seraphina.ninecun_lingtai.common.player.DefenseResourceSnapshot;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.Set;

public final class DefenseBarRenderUtility {
    private static final int HOTBAR_WIDTH = 182;
    private static final int BAR_WIDTH = 10;
    private static final int BAR_HEIGHT = 58;
    private static final int HOTBAR_RIGHT_GAP = 7;
    private static final int SCREEN_MARGIN = 4;
    private static final int FRAME_COLOR = 0xB0151820;
    private static final int INNER_FRAME_COLOR = 0xC02C3038;
    private static final int TRACK_COLOR = 0x9004080D;
    private static final int HIGHLIGHT_COLOR = 0x36FFFFFF;

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
            return;
        }
        Optional<DefenseResourceSnapshot> snapshot = ClientDefenseResourceManager.getSnapshot();
        if (snapshot.isEmpty()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        int left = Math.min(
                screenWidth - BAR_WIDTH - SCREEN_MARGIN,
                screenWidth / 2 + HOTBAR_WIDTH / 2 + HOTBAR_RIGHT_GAP
        );
        int bottom = Math.max(BAR_HEIGHT + SCREEN_MARGIN, screenHeight - SCREEN_MARGIN);
        int top = bottom - BAR_HEIGHT;
        RootVisual rootVisual = getRootVisual(snapshot.get().elementMask());
        float coverage = Mth.clamp(snapshot.get().coverage(), 0.0F, 1.0F);

        drawFrame(guiGraphics, left, top, bottom);
        if (coverage > 0.0F) {
            drawFill(guiGraphics, left, top, bottom, coverage, rootVisual, getTime(minecraft, event.getPartialTick()));
        }
        guiGraphics.fill(left + 1, top + 1, left + 2, bottom - 1, HIGHLIGHT_COLOR);
    }

    private static void drawFrame(GuiGraphics guiGraphics, int left, int top, int bottom) {
        guiGraphics.fill(left - 2, top - 2, left + BAR_WIDTH + 2, bottom + 2, FRAME_COLOR);
        guiGraphics.fill(left - 1, top - 1, left + BAR_WIDTH + 1, bottom + 1, INNER_FRAME_COLOR);
        guiGraphics.fill(left, top, left + BAR_WIDTH, bottom, TRACK_COLOR);
    }

    private static void drawFill(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int bottom,
            float coverage,
            RootVisual rootVisual,
            float time
    ) {
        ShaderInstance shaderInstance = DefenseBarShader.getDefenseBarShader();
        if (shaderInstance == null) {
            drawFallbackFill(guiGraphics, left, top, bottom, coverage, rootVisual);
            return;
        }

        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("Time").set(time);
        shaderInstance.safeGetUniform("Coverage").set(coverage);
        shaderInstance.safeGetUniform("ElementMode").set((float) rootVisual.elementMode());
        setColorUniform(shaderInstance, "PrimaryColor", rootVisual.primaryColor());
        setColorUniform(shaderInstance, "SecondaryColor", rootVisual.secondaryColor());

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int color = ColorUtility.withAlpha(rootVisual.primaryColor(), 0.94F);
        vertex(bufferBuilder, matrix, left, top, color, 0.0F, 0.0F);
        vertex(bufferBuilder, matrix, left, bottom, color, 0.0F, 1.0F);
        vertex(bufferBuilder, matrix, left + BAR_WIDTH, bottom, color, 1.0F, 1.0F);
        vertex(bufferBuilder, matrix, left + BAR_WIDTH, top, color, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawFallbackFill(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int bottom,
            float coverage,
            RootVisual rootVisual
    ) {
        int fillTop = bottom - Math.max(1, Math.round((bottom - top) * coverage));
        guiGraphics.fill(left, fillTop, left + BAR_WIDTH, bottom, ColorUtility.withAlpha(rootVisual.primaryColor(), 0.82F));
        guiGraphics.fill(left + BAR_WIDTH - 2, fillTop, left + BAR_WIDTH - 1, bottom, HIGHLIGHT_COLOR);
    }

    private static RootVisual getRootVisual(int elementMask) {
        Optional<SpiritualRoot> spiritualRoot = SpiritualRoot.fromElementMask(
                elementMask,
                SpiritualRoot.MAXIMUM_LENGTH_INCHES
        );
        if (spiritualRoot.isEmpty()) {
            return new RootVisual(0, 0xC8D1DC, 0xF4F7FF);
        }
        Set<ElementType> elements = spiritualRoot.get().elements();
        if (spiritualRoot.get().isChaos()) {
            return new RootVisual(6, 0xB961FF, 0x3B1E78);
        }
        if (elements.contains(ElementType.YIN_YANG)) {
            return new RootVisual(5, 0xF4F6FA, 0x10131A);
        }
        for (ElementType element : SpiritualRootUtility.FIVE_ELEMENT_NAME_ORDER) {
            if (elements.contains(element)) {
                return getElementVisual(element);
            }
        }
        return new RootVisual(0, 0xC8D1DC, 0xF4F7FF);
    }

    private static RootVisual getElementVisual(ElementType element) {
        return switch (element) {
            case METAL -> new RootVisual(0, 0xE9CF78, 0xEEF6FF);
            case WOOD -> new RootVisual(1, 0x55B75D, 0xA6E875);
            case WATER -> new RootVisual(2, 0x2E96FF, 0xB7F1FF);
            case FIRE -> new RootVisual(3, 0xFF4B36, 0xFFD45A);
            case EARTH -> new RootVisual(4, 0xB98049, 0x6E5238);
            case YIN_YANG -> new RootVisual(5, 0xF4F6FA, 0x10131A);
            case CHAOS, THUNDER -> new RootVisual(6, 0xB961FF, 0x3B1E78);
        };
    }

    private static float getTime(Minecraft minecraft, float partialTick) {
        return (minecraft.level.getGameTime() + partialTick) / 20.0F;
    }

    private static void setColorUniform(ShaderInstance shaderInstance, String uniformName, int color) {
        shaderInstance.safeGetUniform(uniformName).set(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        );
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

    private record RootVisual(int elementMode, int primaryColor, int secondaryColor) {
    }
}
