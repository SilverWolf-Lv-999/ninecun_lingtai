package net.seraphina.ninecun_lingtai.client.tooltip;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.client.utility.FiveElementWheelRenderUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.Set;

public final class FiveElementWheelClientTooltipComponent implements ClientTooltipComponent {
    private final Set<ElementType> selectedElements;

    public FiveElementWheelClientTooltipComponent(FiveElementWheelTooltipComponent tooltipComponent) {
        this.selectedElements = tooltipComponent.getSelectedElements();
    }

    @Override
    public int getHeight() {
        return FiveElementWheelRenderUtility.getHeight();
    }

    @Override
    public int getWidth(Font font) {
        return FiveElementWheelRenderUtility.getWidth();
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        FiveElementWheelRenderUtility.render(guiGraphics, font, x, y, this.selectedElements);
    }
}
