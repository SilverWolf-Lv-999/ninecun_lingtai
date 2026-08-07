package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import com.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.AlchemyFurnaceActionPacket;
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
    private static final int PANEL_COLOR = 0xF2181612;
    private static final int PANEL_BORDER_COLOR = 0xFF8E7645;
    private static final int DIAGRAM_COLOR = 0x705E4A28;
    private static final int SLOT_BORDER_COLOR = 0xFF776B56;
    private static final int OUTPUT_SLOT_BORDER_COLOR = 0xFFC5A35B;
    private static final int SLOT_FILL_COLOR = 0xEE0E0E0D;
    private static final int SLOT_INNER_FILL_COLOR = 0xFF171512;
    private static final int HEAT_TRACK_COLOR = 0x99251D16;
    private static final int HEAT_FILL_COLOR = 0xFFDD793A;
    private static final int START_BUTTON_WIDTH = 124;
    private static final int START_BUTTON_LEFT = (PREFERRED_WIDTH - START_BUTTON_WIDTH) / 2;
    private static final int START_BUTTON_TOP = 137;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private static final int HEAT_BAR_LEFT = 278;
    private static final int HEAT_BAR_TOP = 23;
    private static final int HEAT_BAR_WIDTH = 16;
    private static final int HEAT_BAR_HEIGHT = 112;
    private static final int SLOT_CENTER_OFFSET = 9;

    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PREFERRED_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 24, MINIMUM_WIDTH, PREFERRED_WIDTH);
        this.imageHeight = IMAGE_HEIGHT;
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int panelRight = this.leftPos + this.imageWidth;
        int panelBottom = this.topPos + this.imageHeight;
        guiGraphics.fill(this.leftPos, this.topPos, panelRight, panelBottom, PANEL_COLOR);
        guiGraphics.renderOutline(
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight,
                PANEL_BORDER_COLOR
        );
        guiGraphics.fill(
                this.leftPos + 8,
                this.topPos + 159,
                panelRight - 8,
                this.topPos + 160,
                PANEL_BORDER_COLOR
        );
        renderRefiningDiagram(guiGraphics);
        renderSlotFrames(guiGraphics);
        renderHeat(guiGraphics);
        renderActions(guiGraphics, mouseX, mouseY);
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
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (isInside(
                    mouseX,
                    mouseY,
                    this.leftPos + START_BUTTON_LEFT,
                    this.topPos + START_BUTTON_TOP,
                    START_BUTTON_WIDTH,
                    ACTION_BUTTON_HEIGHT
            )) {
                ModNetwork.sendToServer(new AlchemyFurnaceActionPacket(
                        this.menu.getBlockPosition(),
                        this.menu.isRefining()
                                ? AlchemyFurnaceActionPacket.Action.FINISH_REFINING
                                : AlchemyFurnaceActionPacket.Action.START_REFINING
                ));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderRefiningDiagram(GuiGraphics guiGraphics) {
        ScreenPoint main = slotCenter(AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT);
        ScreenPoint firstSecondary = slotCenter(AlchemyFurnaceMenu.FIRST_SECONDARY_INGREDIENT_SLOT);
        ScreenPoint secondSecondary = slotCenter(AlchemyFurnaceMenu.SECOND_SECONDARY_INGREDIENT_SLOT);
        ScreenPoint output = slotCenter(AlchemyFurnaceMenu.OUTPUT_SLOT);
        ScreenPoint firstAuxiliary = slotCenter(AlchemyFurnaceMenu.FIRST_AUXILIARY_INGREDIENT_SLOT);
        ScreenPoint secondAuxiliary = slotCenter(AlchemyFurnaceMenu.SECOND_AUXILIARY_INGREDIENT_SLOT);

        drawConnection(guiGraphics, main, firstSecondary, DIAGRAM_COLOR);
        drawConnection(guiGraphics, main, secondSecondary, DIAGRAM_COLOR);
        drawConnection(guiGraphics, firstSecondary, output, DIAGRAM_COLOR);
        drawConnection(guiGraphics, secondSecondary, output, DIAGRAM_COLOR);
        drawConnection(guiGraphics, firstAuxiliary, output, DIAGRAM_COLOR);
        drawConnection(guiGraphics, secondAuxiliary, output, DIAGRAM_COLOR);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
            int slotLeft = this.leftPos + this.menu.slots.get(slotIndex).x;
            int slotTop = this.topPos + this.menu.slots.get(slotIndex).y;
            int slotBorder = slotIndex == AlchemyFurnaceMenu.OUTPUT_SLOT
                    ? OUTPUT_SLOT_BORDER_COLOR
                    : SLOT_BORDER_COLOR;
            guiGraphics.fill(slotLeft, slotTop, slotLeft + 18, slotTop + 18, slotBorder);
            guiGraphics.fill(slotLeft + 1, slotTop + 1, slotLeft + 17, slotTop + 17, SLOT_FILL_COLOR);
            guiGraphics.fill(slotLeft + 2, slotTop + 2, slotLeft + 16, slotTop + 16, SLOT_INNER_FILL_COLOR);
        }
    }

    private void renderHeat(GuiGraphics guiGraphics) {
        int heatLeft = this.leftPos + HEAT_BAR_LEFT;
        int heatTop = this.topPos + HEAT_BAR_TOP;
        float heat = this.menu.getMaximumHeat() <= 0
                ? 0.0F
                : Mth.clamp(this.menu.getHeat() / (float) this.menu.getMaximumHeat(), 0.0F, 1.0F);
        guiGraphics.fill(
                heatLeft,
                heatTop,
                heatLeft + HEAT_BAR_WIDTH,
                heatTop + HEAT_BAR_HEIGHT,
                HEAT_TRACK_COLOR
        );
        int heatFillHeight = Math.round((HEAT_BAR_HEIGHT - 2) * heat);
        guiGraphics.fill(
                heatLeft + 1,
                heatTop + HEAT_BAR_HEIGHT - 1 - heatFillHeight,
                heatLeft + HEAT_BAR_WIDTH - 1,
                heatTop + HEAT_BAR_HEIGHT - 1,
                HEAT_FILL_COLOR
        );
        guiGraphics.renderOutline(
                heatLeft,
                heatTop,
                HEAT_BAR_WIDTH,
                HEAT_BAR_HEIGHT,
                SLOT_BORDER_COLOR
        );
    }

    private void renderActions(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean startHovered = isInside(
                mouseX,
                mouseY,
                this.leftPos + START_BUTTON_LEFT,
                this.topPos + START_BUTTON_TOP,
                START_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT
        );
        int buttonLeft = this.leftPos + START_BUTTON_LEFT;
        int buttonTop = this.topPos + START_BUTTON_TOP;
        guiGraphics.fill(
                buttonLeft,
                buttonTop,
                buttonLeft + START_BUTTON_WIDTH,
                buttonTop + ACTION_BUTTON_HEIGHT,
                startHovered ? 0xFF3C3120 : 0xFF29231A
        );
        guiGraphics.renderOutline(
                buttonLeft,
                buttonTop,
                START_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT,
                startHovered ? OUTPUT_SLOT_BORDER_COLOR : SLOT_BORDER_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(this.menu.isRefining()
                        ? "screen.ninecun_lingtai.alchemy.stop"
                        : "screen.ninecun_lingtai.alchemy.start"),
                buttonLeft + START_BUTTON_WIDTH / 2,
                buttonTop + 6,
                PRIMARY_TEXT_COLOR
        );
    }

    private ScreenPoint point(float x, float y) {
        return new ScreenPoint(this.leftPos + x, this.topPos + y);
    }

    private ScreenPoint slotCenter(int slotIndex) {
        net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
        return point(slot.x + SLOT_CENTER_OFFSET, slot.y + SLOT_CENTER_OFFSET);
    }

    private static void drawConnection(GuiGraphics guiGraphics, ScreenPoint start, ScreenPoint end, int color) {
        AptitudeRenderUtility.drawLine(guiGraphics, start, end, 0.65F, color);
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int left,
            int top,
            int width,
            int height
    ) {
        return mouseX >= left
                && mouseX < left + width
                && mouseY >= top
                && mouseY < top + height;
    }
}
