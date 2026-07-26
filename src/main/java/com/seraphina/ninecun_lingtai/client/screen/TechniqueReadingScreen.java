package com.seraphina.ninecun_lingtai.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.client.utility.VerticalTextUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.ImprintTechniquePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class TechniqueReadingScreen extends Screen {
    private static final int PANEL_COLOR = 0xF2A8CFBA;
    private static final int PANEL_BORDER_COLOR = 0xFF315A48;
    private static final int INNER_BORDER_COLOR = 0xFF6E9F87;
    private static final int JADE_HIGHLIGHT_COLOR = 0x557CE0B5;
    private static final int JADE_SHADOW_COLOR = 0x55345949;
    private static final int TEXT_COLOR = 0xFF17372A;
    private static final int MUTED_TEXT_COLOR = 0xFF49695A;
    private static final int BUTTON_COLOR = 0xFFC1E0CF;
    private static final int BUTTON_HOVER_COLOR = 0xFFD5EFDF;
    private static final int BUTTON_DISABLED_COLOR = 0xFF89AA99;
    private static final int LINE_HEIGHT = 14;
    private static final int COLUMN_GAP = 18;
    private static final int ACTION_BUTTON_WIDTH = 92;
    private static final int ACTION_BUTTON_HEIGHT = 20;

    private final TechniqueClientData technique;
    private final Screen parentScreen;
    private final boolean allowImprint;

    private List<List<String>> columns = List.of();
    private int columnOffset;
    private int maximumColumnOffset;
    private int panelLeft;
    private int panelTop;
    private int panelRight;
    private int panelBottom;
    private int actionButtonLeft;
    private int actionButtonTop;
    private int previousButtonLeft;
    private int nextButtonLeft;
    private boolean awaitingImprint;
    private int imprintRequestRevision;

    public TechniqueReadingScreen(
            TechniqueClientData technique,
            Screen parentScreen,
            boolean allowImprint
    ) {
        super(Component.literal(technique.name()));
        this.technique = technique;
        this.parentScreen = parentScreen;
        this.allowImprint = allowImprint;
    }

    @Override
    protected void init() {
        int panelWidth = Mth.clamp(this.width - 36, 280, 520);
        int panelHeight = Mth.clamp(this.height - 30, 210, 360);
        this.panelLeft = (this.width - panelWidth) / 2;
        this.panelTop = (this.height - panelHeight) / 2;
        this.panelRight = this.panelLeft + panelWidth;
        this.panelBottom = this.panelTop + panelHeight;
        this.actionButtonLeft = (this.width - ACTION_BUTTON_WIDTH) / 2;
        this.actionButtonTop = this.panelBottom - 30;
        this.previousButtonLeft = this.panelLeft + 18;
        this.nextButtonLeft = this.panelRight - 18 - 54;

        int contentTop = this.panelTop + 54;
        int contentBottom = this.actionButtonTop - 14;
        int maximumRows = Math.max(1, (contentBottom - contentTop) / LINE_HEIGHT);
        this.columns = VerticalTextUtility.splitIntoColumns(this.technique.content(), maximumRows);
        int visibleColumns = Math.max(1, (panelWidth - 58) / COLUMN_GAP);
        this.maximumColumnOffset = Math.max(0, this.columns.size() - visibleColumns);
        this.columnOffset = Mth.clamp(this.columnOffset, 0, this.maximumColumnOffset);
    }

    @Override
    public void tick() {
        if (this.awaitingImprint
                && ClientTechniqueManager.getRevision() > this.imprintRequestRevision) {
            this.awaitingImprint = false;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        renderJadeSurface(guiGraphics);

        drawScaledCenteredText(
                guiGraphics,
                displayFontText(this.technique.name()),
                this.width / 2,
                this.panelTop + 17,
                1.6F,
                this.panelRight - this.panelLeft - 76,
                TEXT_COLOR
        );
        guiGraphics.fill(
                this.panelLeft + 22,
                this.panelTop + 43,
                this.panelRight - 22,
                this.panelTop + 44,
                INNER_BORDER_COLOR
        );

        renderVerticalContent(guiGraphics);
        renderPageControls(guiGraphics, mouseX, mouseY);
        renderActionButton(guiGraphics, mouseX, mouseY);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (isInside(mouseX, mouseY, this.previousButtonLeft, this.actionButtonTop, 54, 20)
                && this.columnOffset > 0) {
            this.columnOffset = Math.max(0, this.columnOffset - getVisibleColumnCount());
            return true;
        }
        if (isInside(mouseX, mouseY, this.nextButtonLeft, this.actionButtonTop, 54, 20)
                && this.columnOffset < this.maximumColumnOffset) {
            this.columnOffset = Math.min(
                    this.maximumColumnOffset,
                    this.columnOffset + getVisibleColumnCount()
            );
            return true;
        }
        if (!isInside(
                mouseX,
                mouseY,
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT
        )) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (!this.allowImprint) {
            this.onClose();
            return true;
        }
        if (!ClientTechniqueManager.isImprinted(this.technique.id()) && !this.awaitingImprint) {
            this.awaitingImprint = true;
            this.imprintRequestRevision = ClientTechniqueManager.getRevision();
            ModNetwork.sendToServer(new ImprintTechniquePacket(this.technique.id()));
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.maximumColumnOffset <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        this.columnOffset = Mth.clamp(
                this.columnOffset - (int) Math.signum(delta),
                0,
                this.maximumColumnOffset
        );
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && this.columnOffset > 0) {
            this.columnOffset--;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && this.columnOffset < this.maximumColumnOffset) {
            this.columnOffset++;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (this.parentScreen != null && this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
            return;
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderVerticalContent(GuiGraphics guiGraphics) {
        int contentTop = this.panelTop + 54;
        int contentRight = this.panelRight - 28;
        int visibleColumns = getVisibleColumnCount();
        int renderedColumns = Math.min(
                visibleColumns,
                this.columns.size() - this.columnOffset
        );
        for (int visibleColumnIndex = 0; visibleColumnIndex < renderedColumns; visibleColumnIndex++) {
            List<String> column = this.columns.get(this.columnOffset + visibleColumnIndex);
            int columnX = contentRight - visibleColumnIndex * COLUMN_GAP;
            for (int rowIndex = 0; rowIndex < column.size(); rowIndex++) {
                Component symbol = customFontText(column.get(rowIndex));
                int symbolX = columnX - this.font.width(symbol) / 2;
                guiGraphics.drawString(
                        this.font,
                        symbol,
                        symbolX,
                        contentTop + rowIndex * LINE_HEIGHT,
                        TEXT_COLOR,
                        false
                );
            }
        }
    }

    private void renderPageControls(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawButton(
                guiGraphics,
                this.previousButtonLeft,
                this.actionButtonTop,
                54,
                20,
                customFontText("前卷"),
                this.columnOffset > 0,
                isInside(mouseX, mouseY, this.previousButtonLeft, this.actionButtonTop, 54, 20)
        );
        drawButton(
                guiGraphics,
                this.nextButtonLeft,
                this.actionButtonTop,
                54,
                20,
                customFontText("后卷"),
                this.columnOffset < this.maximumColumnOffset,
                isInside(mouseX, mouseY, this.nextButtonLeft, this.actionButtonTop, 54, 20)
        );

        int visibleColumns = Math.min(
                getVisibleColumnCount(),
                this.columns.size() - this.columnOffset
        );
        String pageText = (this.columnOffset + 1)
                + "-"
                + (this.columnOffset + Math.max(1, visibleColumns))
                + " / "
                + this.columns.size();
        guiGraphics.drawCenteredString(
                this.font,
                customFontText(pageText),
                this.width / 2,
                this.actionButtonTop - 11,
                MUTED_TEXT_COLOR
        );
    }

    private void renderActionButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean imprinted = ClientTechniqueManager.isImprinted(this.technique.id());
        boolean active = !this.allowImprint || (!imprinted && !this.awaitingImprint);
        String buttonText;
        if (!this.allowImprint) {
            buttonText = "退出";
        } else if (imprinted) {
            buttonText = "已烙印";
        } else if (this.awaitingImprint) {
            buttonText = "烙印中……";
        } else {
            buttonText = "烙印";
        }
        drawButton(
                guiGraphics,
                this.actionButtonLeft,
                this.actionButtonTop,
                ACTION_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT,
                displayFontText(buttonText),
                active,
                isInside(
                        mouseX,
                        mouseY,
                        this.actionButtonLeft,
                        this.actionButtonTop,
                        ACTION_BUTTON_WIDTH,
                        ACTION_BUTTON_HEIGHT
                )
        );
    }

    private void drawButton(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int width,
            int height,
            Component text,
            boolean active,
            boolean hovered
    ) {
        int color = active
                ? (hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR)
                : BUTTON_DISABLED_COLOR;
        guiGraphics.fill(left, top, left + width, top + height, color);
        guiGraphics.renderOutline(left, top, width, height, PANEL_BORDER_COLOR);
        guiGraphics.drawCenteredString(
                this.font,
                text,
                left + width / 2,
                top + (height - this.font.lineHeight) / 2,
                active ? TEXT_COLOR : MUTED_TEXT_COLOR
        );
    }

    private int getVisibleColumnCount() {
        return Math.max(1, (this.panelRight - this.panelLeft - 58) / COLUMN_GAP);
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int left,
            int top,
            int width,
            int height
    ) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private void drawScaledCenteredText(
            GuiGraphics guiGraphics,
            Component text,
            int centerX,
            int top,
            float preferredScale,
            int maximumWidth,
            int color
    ) {
        float scale = Math.min(
                preferredScale,
                maximumWidth / (float) Math.max(1, this.font.width(text))
        );
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, top, 0.0F);
        poseStack.scale(scale, scale, 1.0F);
        guiGraphics.drawString(
                this.font,
                text,
                -this.font.width(text) / 2,
                0,
                color,
                false
        );
        poseStack.popPose();
    }

    private void renderJadeSurface(GuiGraphics guiGraphics) {
        guiGraphics.fill(
                this.panelLeft,
                this.panelTop,
                this.panelRight,
                this.panelBottom,
                PANEL_COLOR
        );
        guiGraphics.renderOutline(
                this.panelLeft,
                this.panelTop,
                this.panelRight - this.panelLeft,
                this.panelBottom - this.panelTop,
                PANEL_BORDER_COLOR
        );
        guiGraphics.renderOutline(
                this.panelLeft + 5,
                this.panelTop + 5,
                this.panelRight - this.panelLeft - 10,
                this.panelBottom - this.panelTop - 10,
                INNER_BORDER_COLOR
        );

        guiGraphics.fill(
                this.panelLeft + 9,
                this.panelTop + 9,
                this.panelLeft + 11,
                this.panelBottom - 9,
                JADE_HIGHLIGHT_COLOR
        );
        guiGraphics.fill(
                this.panelRight - 12,
                this.panelTop + 9,
                this.panelRight - 10,
                this.panelBottom - 9,
                JADE_SHADOW_COLOR
        );
        guiGraphics.fill(
                this.panelLeft + 18,
                this.panelTop + 31,
                this.panelRight - 38,
                this.panelTop + 32,
                JADE_HIGHLIGHT_COLOR
        );
        guiGraphics.fill(
                this.panelLeft + 42,
                this.panelBottom - 43,
                this.panelRight - 18,
                this.panelBottom - 42,
                JADE_SHADOW_COLOR
        );
    }

    private static Component customFontText(String text) {
        return Component.literal(text).withStyle(
                style -> style.withFont(CustomFontConstants.SHAN_BEI_FONT_ID)
        );
    }

    private static Component displayFontText(String text) {
        return Component.literal(text).withStyle(
                style -> style.withFont(CustomFontConstants.SHAN_BEI_DISPLAY_FONT_ID)
        );
    }
}
