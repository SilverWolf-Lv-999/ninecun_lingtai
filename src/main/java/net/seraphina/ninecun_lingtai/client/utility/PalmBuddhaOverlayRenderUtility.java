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
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaCandidateManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaMemberManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaOverlayManager;
import net.seraphina.ninecun_lingtai.client.render.PalmBuddhaOverlayShader;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaCandidateSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPacketLimits;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPageSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberSnapshot;
import org.joml.Matrix4f;

/** Renders the first Palm Buddha member page with one shader-backed surface. */
public final class PalmBuddhaOverlayRenderUtility {
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
        PalmBuddhaOverlayGeometryUtility.PanelBounds panelBounds =
                PalmBuddhaOverlayGeometryUtility.calculatePanelBounds(
                        minecraft.getWindow().getGuiScaledWidth(),
                        minecraft.getWindow().getGuiScaledHeight()
                );

        drawShaderPanel(guiGraphics, panelBounds, partialTicks);
        drawMemberText(guiGraphics, minecraft.font, panelBounds);
    }

    private static void drawShaderPanel(
            GuiGraphics guiGraphics,
            PalmBuddhaOverlayGeometryUtility.PanelBounds panelBounds,
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
        shaderInstance.safeGetUniform("PanelSize").set(panelBounds.width(), panelBounds.height());
        shaderInstance.safeGetUniform("Time").set(
                (Minecraft.getInstance().level == null ? 0.0F : Minecraft.getInstance().level.getGameTime())
                        + partialTicks
        );
        shaderInstance.safeGetUniform("SelectedRow").set(ClientPalmBuddhaOverlayManager.getSelectedRow());
        shaderInstance.safeGetUniform("ReleaseReady").set(
                ClientPalmBuddhaOverlayManager.isSelectedMemberReleasable() ? 1.0F : 0.0F
        );
        shaderInstance.safeGetUniform("ReleasePending").set(
                ClientPalmBuddhaOverlayManager.isReleasePending() ? 1.0F : 0.0F
        );
        shaderInstance.safeGetUniform("FeedbackProgress").set(
                ClientPalmBuddhaOverlayManager.getReleaseFeedbackProgress(partialTicks)
        );
        shaderInstance.safeGetUniform("FeedbackType").set(
                ClientPalmBuddhaOverlayManager.getReleaseFeedbackType()
        );
        shaderInstance.safeGetUniform("PageIndex").set(
                ClientPalmBuddhaOverlayManager.isCandidateView()
                        ? (float) ClientPalmBuddhaOverlayManager.getCandidatePageIndex()
                        : (float) page.pageIndex()
        );
        shaderInstance.safeGetUniform("PageCount").set(
                ClientPalmBuddhaOverlayManager.isCandidateView()
                        ? (float) ClientPalmBuddhaOverlayManager.getCandidatePageCount()
                        : (float) page.pageCount()
        );
        shaderInstance.safeGetUniform("CandidateView").set(
                ClientPalmBuddhaOverlayManager.isCandidateView() ? 1.0F : 0.0F
        );
        shaderInstance.safeGetUniform("CandidateCount").set(
                (float) ClientPalmBuddhaCandidateManager.getCandidates().size()
        );
        shaderInstance.safeGetUniform("CandidatePending").set(
                ClientPalmBuddhaCandidateManager.isRequestPending() ? 1.0F : 0.0F
        );

        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        vertex(bufferBuilder, matrix, panelBounds.left(), panelBounds.top(), 0.0F, 0.0F);
        vertex(
                bufferBuilder,
                matrix,
                panelBounds.left(),
                panelBounds.bottom(),
                0.0F,
                1.0F
        );
        vertex(
                bufferBuilder,
                matrix,
                panelBounds.right(),
                panelBounds.bottom(),
                1.0F,
                1.0F
        );
        vertex(bufferBuilder, matrix, panelBounds.right(), panelBounds.top(), 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawMemberText(
            GuiGraphics guiGraphics,
            Font font,
            PalmBuddhaOverlayGeometryUtility.PanelBounds panelBounds
    ) {
        if (ClientPalmBuddhaOverlayManager.isCandidateView()) {
            drawCandidateText(guiGraphics, font, panelBounds);
            return;
        }
        PalmBuddhaMemberPageSnapshot page = ClientPalmBuddhaMemberManager.getCurrentPage();
        float left = panelBounds.left();
        float top = panelBounds.top();
        float panelWidth = panelBounds.width();
        float panelHeight = panelBounds.height();
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

        int rowsTop = Mth.floor(panelBounds.rowsTop());
        int rowsBottom = Mth.floor(panelBounds.rowsBottom());
        guiGraphics.enableScissor(
                Mth.floor(left + 20.0F),
                rowsTop,
                Mth.floor(left + panelWidth - 20.0F),
                rowsBottom
        );
        for (int index = 0; index < page.members().size(); index++) {
            PalmBuddhaMemberSnapshot member = page.members().get(index);
            int rowY = Mth.floor(PalmBuddhaOverlayGeometryUtility.rowTextTop(panelBounds, index));
            int statusColor = member.online() && member.inExpectedRealm() ? 0xE5637650 : MUTED_INK_COLOR;
            String status = !member.online()
                    ? (member.kind() == net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberKind.CREATURE
                            ? "未加载"
                            : "离线")
                    : member.inExpectedRealm() ? "在国" : "归位中";
            String realm = member.cultivationRealmName();
            String kindLabel = member.kind()
                    == net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberKind.CREATURE
                    ? "生灵 · "
                    : "";
            String suffix = kindLabel + (realm.isEmpty() ? status : status + " · " + realm);
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
        String footer = ClientPalmBuddhaOverlayManager.isReleasePending()
                ? "放生中 · 等待真实维度回执"
                : "← / → 翻页    左键选人    右键放出    Esc 收卷";
        guiGraphics.drawString(
                font,
                Component.literal(footer),
                Mth.floor(left + 28.0F),
                footerY,
                MUTED_INK_COLOR,
                false
        );
        ClientPalmBuddhaOverlayManager.getReleaseFeedbackMessage().ifPresent(message -> {
            String feedbackText = message.getString();
            int feedbackWidth = font.width(feedbackText);
            int feedbackColor = ClientPalmBuddhaOverlayManager.getReleaseFeedbackType() > 0.0F
                    ? 0xE5637650
                    : SEAL_COLOR;
            guiGraphics.drawString(
                    font,
                    message,
                    Mth.floor(left + (panelWidth - feedbackWidth) * 0.5F),
                    footerY - 17,
                    feedbackColor,
                    false
            );
        });
        guiGraphics.drawString(
                font,
                Component.literal("掌印"),
                Mth.floor(left + panelWidth - 58.0F),
                footerY,
                SEAL_COLOR,
                false
        );
    }

    private static void drawCandidateText(
            GuiGraphics guiGraphics,
            Font font,
            PalmBuddhaOverlayGeometryUtility.PanelBounds panelBounds
    ) {
        float left = panelBounds.left();
        float top = panelBounds.top();
        float panelWidth = panelBounds.width();
        float panelHeight = panelBounds.height();
        int textLeft = Mth.floor(left + 28.0F);
        int titleY = Mth.floor(top + 22.0F);
        int candidateCount = ClientPalmBuddhaCandidateManager.getCandidates().size();
        int candidatePageIndex = ClientPalmBuddhaOverlayManager.getCandidatePageIndex();
        int candidatePageCount = ClientPalmBuddhaOverlayManager.getCandidatePageCount();
        guiGraphics.drawString(font, Component.literal("掌中佛国"), textLeft, titleY, INK_COLOR, false);
        guiGraphics.drawString(
                font,
                Component.literal(
                        ClientPalmBuddhaCandidateManager.isRequestPending()
                                ? "周天墨影 · 观照中"
                                : "周天墨影 · 可摄众生"
                ),
                textLeft,
                titleY + 18,
                MUTED_INK_COLOR,
                false
        );
        String pageLabel = candidatePageCount == 0
                ? "空"
                : (candidatePageIndex + 1) + " / " + candidatePageCount;
        int pageWidth = font.width(pageLabel);
        guiGraphics.drawString(
                font,
                Component.literal(pageLabel),
                Mth.floor(left + panelWidth - pageWidth - 28.0F),
                titleY + 9,
                MUTED_INK_COLOR,
                false
        );

        int rowsTop = Mth.floor(panelBounds.rowsTop());
        int rowsBottom = Mth.floor(panelBounds.rowsBottom());
        guiGraphics.enableScissor(
                Mth.floor(left + 20.0F),
                rowsTop,
                Mth.floor(left + panelWidth - 20.0F),
                rowsBottom
        );
        int firstCandidateIndex = candidatePageIndex * PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE;
        int lastCandidateIndex = Math.min(
                firstCandidateIndex + PalmBuddhaMemberPacketLimits.MAXIMUM_PAGE_SIZE,
                candidateCount
        );
        for (int index = firstCandidateIndex; index < lastCandidateIndex; index++) {
            PalmBuddhaCandidateSnapshot candidate = ClientPalmBuddhaCandidateManager.getCandidates().get(index);
            int rowY = Mth.floor(
                    PalmBuddhaOverlayGeometryUtility.rowTextTop(
                            panelBounds,
                            index - firstCandidateIndex
                    )
            );
            String cultivation = candidate.cultivationStage()
                    .map(stage -> stage.realm().getChineseName() + " " + stage.layer() + "层")
                    .orElse("未显境界");
            String kind = candidate.kind()
                    == net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberKind.CREATURE
                    ? "生灵"
                    : "修士";
            String suffix = kind + " · " + cultivation + " · "
                    + Mth.floor(candidate.distance() + 0.5F) + "格";
            guiGraphics.drawString(font, Component.literal(candidate.displayName()), textLeft, rowY, INK_COLOR, false);
            int suffixWidth = font.width(suffix);
            guiGraphics.drawString(
                    font,
                    Component.literal(suffix),
                    Mth.floor(left + panelWidth - suffixWidth - 28.0F),
                    rowY,
                    0xE0526049,
                    false
            );
        }
        if (!ClientPalmBuddhaCandidateManager.isRequestPending() && candidateCount == 0) {
            guiGraphics.drawString(
                    font,
                    Component.literal("四野无可摄众生"),
                    textLeft,
                    Mth.floor(PalmBuddhaOverlayGeometryUtility.rowTextTop(panelBounds, 0)),
                    MUTED_INK_COLOR,
                    false
            );
        }
        guiGraphics.disableScissor();

        int footerY = Mth.floor(top + panelHeight - 28.0F);
        String footer = ClientPalmBuddhaCandidateManager.isRequestPending()
                ? "周天观照中 · 等待服务端候选集"
                : "候选 " + candidateCount + " · 换手键 收卷墨影    ← / → 翻页    Esc 收卷";
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
