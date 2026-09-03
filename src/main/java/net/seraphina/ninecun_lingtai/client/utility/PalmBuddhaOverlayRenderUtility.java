package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaMemberManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaOverlayManager;
import net.seraphina.ninecun_lingtai.client.render.PalmBuddhaOverlayShader;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPageSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberSnapshot;
import org.joml.Matrix4f;

/** Renders the first Palm Buddha member page with one shader-backed surface. */
public final class PalmBuddhaOverlayRenderUtility {
    private static final float MAXIMUM_PANEL_WIDTH = 560.0F;
    private static final float MAXIMUM_PANEL_HEIGHT = 360.0F;
    private static final float PANEL_MARGIN = 12.0F;
    private static final float HEADER_HEIGHT = 58.0F;
    private static final float FOOTER_HEIGHT = 42.0F;
    private static final float ROW_HEIGHT = 22.0F;
    private static final int INK_COLOR = 0xE51B1712;
    private static final int MUTED_INK_COLOR = 0xC86A5543;
    private static final int SEAL_COLOR = 0xE5A62B22;

    private PalmBuddhaOverlayRenderUtility() {
    }

    public static void render(GuiGraphics guiGraphics, float partialTicks) {
        if (!ClientPalmBuddhaOverlayManager.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        float panelWidth = Math.min(MAXIMUM_PANEL_WIDTH, Math.max(220.0F, screenWidth - PANEL_MARGIN * 2.0F));
        float panelHeight = Math.min(MAXIMUM_PANEL_HEIGHT, Math.max(240.0F, screenHeight - PANEL_MARGIN * 2.0F));
        float left = (screenWidth - panelWidth) * 0.5F;
        float top = (screenHeight - panelHeight) * 0.5F;

        drawShaderPanel(guiGraphics, left, top, panelWidth, panelHeight, partialTicks);
        drawMemberText(guiGraphics, minecraft.font, left, top, panelWidth, panelHeight);
    }

    private static void drawShaderPanel(
            GuiGraphics guiGraphics,
            float left,
            float top,
            float panelWidth,
            float panelHeight,
            float partialTicks
    ) {
        ShaderInstance shaderInstance = PalmBuddhaOverlayShader.getPalmBuddhaOverlayShader();
        if (shaderInstance == null) {
            return;
        }
        PalmBuddhaMemberPageSnapshot page = ClientPalmBuddhaMemberManager.getCurrentPage();
        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("PanelSize").set(panelWidth, panelHeight);
        shaderInstance.safeGetUniform("Time").set(
                (Minecraft.getInstance().level == null ? 0.0F : Minecraft.getInstance().level.getGameTime())
                        + partialTicks
        );
        shaderInstance.safeGetUniform("SelectedRow").set(-1.0F);
        shaderInstance.safeGetUniform("PageIndex").set((float) page.pageIndex());
        shaderInstance.safeGetUniform("PageCount").set((float) page.pageCount());

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        vertex(bufferBuilder, matrix, left, top, 0.0F, 0.0F);
        vertex(bufferBuilder, matrix, left, top + panelHeight, 0.0F, 1.0F);
        vertex(bufferBuilder, matrix, left + panelWidth, top + panelHeight, 1.0F, 1.0F);
        vertex(bufferBuilder, matrix, left + panelWidth, top, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawMemberText(
            GuiGraphics guiGraphics,
            Font font,
            float left,
            float top,
            float panelWidth,
            float panelHeight
    ) {
        PalmBuddhaMemberPageSnapshot page = ClientPalmBuddhaMemberManager.getCurrentPage();
        int textLeft = Mth.floor(left + 28.0F);
        int titleY = Mth.floor(top + 22.0F);
        guiGraphics.drawString(font, Component.literal("掌中佛国"), textLeft, titleY, INK_COLOR, false);
        guiGraphics.drawString(
                font,
                Component.literal("成员簿 · 真实维度"),
                textLeft,
                titleY + 18,
                MUTED_INK_COLOR,
                false
        );

        String pageLabel = page.pageCount() <= 0
                ? "空"
                : (page.pageIndex() + 1) + " / " + page.pageCount();
        int pageWidth = font.width(pageLabel);
        guiGraphics.drawString(
                font,
                Component.literal(pageLabel),
                Mth.floor(left + panelWidth - pageWidth - 28.0F),
                titleY + 9,
                MUTED_INK_COLOR,
                false
        );

        int rowsTop = Mth.floor(top + HEADER_HEIGHT);
        int rowsBottom = Mth.floor(top + panelHeight - FOOTER_HEIGHT);
        guiGraphics.enableScissor(
                Mth.floor(left + 20.0F),
                rowsTop,
                Mth.floor(left + panelWidth - 20.0F),
                rowsBottom
        );
        for (int index = 0; index < page.members().size(); index++) {
            PalmBuddhaMemberSnapshot member = page.members().get(index);
            int rowY = Mth.floor(rowsTop + index * ROW_HEIGHT + 6.0F);
            int statusColor = member.online() && member.inExpectedRealm() ? 0xE5637650 : MUTED_INK_COLOR;
            String status = !member.online() ? "离线" : member.inExpectedRealm() ? "在国" : "归位中";
            String realm = member.cultivationRealmName();
            String suffix = realm.isEmpty() ? status : status + " · " + realm;
            guiGraphics.drawString(font, Component.literal(member.displayName()), textLeft, rowY, INK_COLOR, false);
            int suffixWidth = font.width(suffix);
            guiGraphics.drawString(
                    font,
                    Component.literal(suffix),
                    Mth.floor(left + panelWidth - suffixWidth - 28.0F),
                    rowY,
                    statusColor,
                    false
            );
        }
        guiGraphics.disableScissor();

        int footerY = Mth.floor(top + panelHeight - 28.0F);
        String footer = "← / → 翻页    Esc 收卷";
        guiGraphics.drawString(
                font,
                Component.literal(footer),
                Mth.floor(left + 28.0F),
                footerY,
                MUTED_INK_COLOR,
                false
        );
        guiGraphics.drawString(
                font,
                Component.literal("掌印"),
                Mth.floor(left + panelWidth - 58.0F),
                footerY,
                SEAL_COLOR,
                false
        );
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            float textureU,
            float textureV
    ) {
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(textureU, textureV)
                .endVertex();
    }
}

