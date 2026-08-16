package net.seraphina.ninecun_lingtai.client.tooltip;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.EnumSet;
import java.util.Set;

public final class FiveElementWheelTooltipComponent implements TooltipComponent {
    private final Set<ElementType> selectedElements;

    public FiveElementWheelTooltipComponent(Set<ElementType> selectedElements) {
        EnumSet<ElementType> fiveElements = EnumSet.noneOf(ElementType.class);
        for (ElementType element : selectedElements) {
            if (element.isFiveElement()) {
                fiveElements.add(element);
            }
        }
        this.selectedElements = Set.copyOf(fiveElements);
    }

    public Set<ElementType> getSelectedElements() {
        return this.selectedElements;
    }
}
