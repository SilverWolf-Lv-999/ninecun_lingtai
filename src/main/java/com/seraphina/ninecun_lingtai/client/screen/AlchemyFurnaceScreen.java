package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.AlchemyFurnaceActionPacket;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public final class AlchemyFurnaceScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {
    private static final int PREFERRED_WIDTH = 324;
    private static final int MINIMUM_WIDTH = PREFERRED_WIDTH;
    private static final int IMAGE_HEIGHT = 250;
    private static final int PRIMARY_TEXT_COLOR = 0xFFE6D5A8;
    private static final int SECONDARY_TEXT_COLOR = 0xFFC1A96D;
    private static final int MUTED_TEXT_COLOR = 0xFF7D745E;
    private static final int HEAT_TRACK_COLOR = 0x99251D16;
    private static final int HEAT_FILL_COLOR = 0xFFDD793A;
    private static final int PROGRESS_TRACK_COLOR = 0x9928241E;
    private static final int PROGRESS_FILL_COLOR = 0xFFB8954A;
    private static final int START_BUTTON_LEFT = 112;
    private static final int START_BUTTON_TOP = 137;
    private static final int START_BUTTON_WIDTH = 100;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private static final int IGNITE_BUTTON_LEFT = 250;
    private static final int IGNITE_BUTTON_TOP = 137;
    private static final int IGNITE_BUTTON_WIDTH = 52;

    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PREFERRED_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.inventoryLabelY = 155;
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 24, MINIMUM_WIDTH, PREFERRED_WIDTH);
        this.imageHeight = IMAGE_HEIGHT;
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long currentTime = Util.getMillis();
        TechniqueImprintRenderUtility.drawCloudVeil(guiGraphics, this.width, this.height, currentTime, 1.0F);
        TechniqueImprintRenderUtility.drawConstellationField(guiGraphics, this.width, this.height, currentTime, 0.42F);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        long currentTime = Util.getMillis();
        int panelRight = this.leftPos + this.imageWidth;
        int panelBottom = this.topPos + this.imageHeight;
        int panelColor = ColorUtility.withAlpha(0x1A1612, 0.92F);
        guiGraphics.fill(this.leftPos, this.topPos, panelRight, panelBottom, panelColor);
        guiGraphics.renderOutline(
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight,
                ColorUtility.withAlpha(TechniqueImprintRenderUtility.GOLD_COLOR, 0.62F)
        );

        renderRefiningDiagram(guiGraphics, currentTime);
        renderSlotLabels(guiGraphics);
        renderProgress(guiGraphics);
        renderActions(guiGraphics, mouseX, mouseY, currentTime);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.imageWidth / 2,
                7,
                PRIMARY_TEXT_COLOR
        );
        guiGraphics.drawString(
                this.font,
                this.playerInventoryTitle,
                8,
                this.inventoryLabelY,
                SECONDARY_TEXT_COLOR,
                false
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (TechniqueImprintRenderUtility.isInside(
                    mouseX,
                    mouseY,
                    this.leftPos + START_BUTTON_LEFT,
                    this.topPos + START_BUTTON_TOP,
                    START_BUTTON_WIDTH,
                    ACTION_BUTTON_HEIGHT
            )) {
                ModNetwork.sendToServer(new AlchemyFurnaceActionPacket(
                        this.menu.getBlockPosition(),
                        AlchemyFurnaceActionPacket.Action.TOGGLE_REFINING
                ));
                return true;
            }
            if (TechniqueImprintRenderUtility.isInside(
                    mouseX,
                    mouseY,
                    this.leftPos + IGNITE_BUTTON_LEFT,
                    this.topPos + IGNITE_BUTTON_TOP,
                    IGNITE_BUTTON_WIDTH,
                    ACTION_BUTTON_HEIGHT
            )) {
                ModNetwork.sendToServer(new AlchemyFurnaceActionPacket(
                        this.menu.getBlockPosition(),
                        AlchemyFurnaceActionPacket.Action.IGNITE
                ));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderRefiningDiagram(GuiGraphics guiGraphics, long currentTime) {
        float alpha = 0.78F;
        ScreenPoint main = point(153.0F, 31.0F);
        ScreenPoint firstSecondary = point(89.0F, 75.0F);
        ScreenPoint secondSecondary = point(217.0F, 75.0F);
        ScreenPoint output = point(153.0F, 96.0F);
        ScreenPoint firstAuxiliary = point(89.0F, 126.0F);
        ScreenPoint secondAuxiliary = point(217.0F, 126.0F);
        int connectionColor = ColorUtility.withAlpha(
                TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                alpha * 0.48F
        );

        drawConnection(guiGraphics, main, firstSecondary, connectionColor);
        drawConnection(guiGraphics, main, secondSecondary, connectionColor);
        drawConnection(guiGraphics, firstSecondary, output, connectionColor);
        drawConnection(guiGraphics, secondSecondary, output, connectionColor);
        drawConnection(guiGraphics, firstAuxiliary, output, connectionColor);
        drawConnection(guiGraphics, secondAuxiliary, output, connectionColor);

        float outputPulse = this.menu.isRefining()
                ? 1.0F + Mth.sin(currentTime * 0.008F) * 0.08F
                : 1.0F;
        AptitudeRenderUtility.drawRing(
                guiGraphics,
                output.x(),
                output.y(),
                16.0F * outputPulse,
                this.menu.isRefining() ? 1.1F : 0.7F,
                ColorUtility.withAlpha(
                        this.menu.isRefining()
                                ? TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
                                : TechniqueImprintRenderUtility.GOLD_COLOR,
                        this.menu.isRefining() ? 0.86F : 0.5F
                )
        );
        TechniqueImprintRenderUtility.drawSeal(
                guiGraphics,
                output.x(),
                output.y(),
                10.5F * outputPulse,
                this.menu.isRefining(),
                false,
                currentTime,
                alpha
        );
    }

    private void renderSlotLabels(GuiGraphics guiGraphics) {
        drawSlotLabel(guiGraphics, Component.translatable("screen.ninecun_lingtai.alchemy.main"), 153, 12);
        drawSlotLabel(guiGraphics, Component.translatable("screen.ninecun_lingtai.alchemy.secondary"), 58, 54);
        drawSlotLabel(guiGraphics, Component.translatable("screen.ninecun_lingtai.alchemy.secondary"), 230, 54);
        drawSlotLabel(guiGraphics, Component.translatable("screen.ninecun_lingtai.alchemy.auxiliary"), 58, 106);
        drawSlotLabel(guiGraphics, Component.translatable("screen.ninecun_lingtai.alchemy.auxiliary"), 230, 106);
        drawSlotLabel(guiGraphics, Component.translatable("screen.ninecun_lingtai.alchemy.output"), 153, 74);
    }

    private void renderProgress(GuiGraphics guiGraphics) {
        int progressLeft = this.leftPos + 18;
        int progressTop = this.topPos + 28;
        int progressWidth = 38;
        int progressHeight = 6;
        float brewProgress = this.menu.getBrewDuration() <= 0
                ? 0.0F
                : Mth.clamp(
                        this.menu.getBrewProgress() / (float) this.menu.getBrewDuration(),
                        0.0F,
                        1.0F
                );
        guiGraphics.fill(progressLeft, progressTop, progressLeft + progressWidth, progressTop + progressHeight, PROGRESS_TRACK_COLOR);
        guiGraphics.fill(
                progressLeft,
                progressTop,
                progressLeft + Math.round(progressWidth * brewProgress),
                progressTop + progressHeight,
                PROGRESS_FILL_COLOR
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.alchemy.progress"),
                progressLeft,
                progressTop - 11,
                MUTED_TEXT_COLOR,
                false
        );

        int heatLeft = this.leftPos + 279;
        int heatTop = this.topPos + 36;
        int heatHeight = 88;
        float heat = this.menu.getMaximumHeat() <= 0
                ? 0.0F
                : Mth.clamp(this.menu.getHeat() / (float) this.menu.getMaximumHeat(), 0.0F, 1.0F);
        guiGraphics.fill(heatLeft, heatTop, heatLeft + 7, heatTop + heatHeight, HEAT_TRACK_COLOR);
        int heatFillHeight = Math.round(heatHeight * heat);
        guiGraphics.fill(
                heatLeft,
                heatTop + heatHeight - heatFillHeight,
                heatLeft + 7,
                heatTop + heatHeight,
                HEAT_FILL_COLOR
        );
        guiGraphics.renderOutline(heatLeft, heatTop, 7, heatHeight, MUTED_TEXT_COLOR);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.alchemy.heat"),
                heatLeft - 8,
                heatTop - 11,
                MUTED_TEXT_COLOR,
                false
        );
    }

    private void renderActions(GuiGraphics guiGraphics, int mouseX, int mouseY, long currentTime) {
        boolean startHovered = TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.leftPos + START_BUTTON_LEFT,
                this.topPos + START_BUTTON_TOP,
                START_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                Component.translatable(this.menu.isRefining()
                        ? "screen.ninecun_lingtai.alchemy.stop"
                        : "screen.ninecun_lingtai.alchemy.start"),
                this.leftPos + START_BUTTON_LEFT,
                this.topPos + START_BUTTON_TOP,
                START_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT,
                true,
                startHovered,
                currentTime,
                0.95F
        );
        boolean igniteHovered = TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.leftPos + IGNITE_BUTTON_LEFT,
                this.topPos + IGNITE_BUTTON_TOP,
                IGNITE_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT
        );
        TechniqueImprintRenderUtility.drawStarButton(
                guiGraphics,
                this.font,
                Component.translatable("screen.ninecun_lingtai.alchemy.ignite"),
                this.leftPos + IGNITE_BUTTON_LEFT,
                this.topPos + IGNITE_BUTTON_TOP,
                IGNITE_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT,
                true,
                igniteHovered,
                currentTime,
                0.95F
        );
    }

    private void drawSlotLabel(GuiGraphics guiGraphics, Component label, int centerX, int y) {
        guiGraphics.drawCenteredString(this.font, label, centerX, y, MUTED_TEXT_COLOR);
    }

    private ScreenPoint point(float x, float y) {
        return new ScreenPoint(this.leftPos + x, this.topPos + y);
    }

    private static void drawConnection(GuiGraphics guiGraphics, ScreenPoint start, ScreenPoint end, int color) {
        AptitudeRenderUtility.drawLine(guiGraphics, start, end, 0.65F, color);
    }
}
