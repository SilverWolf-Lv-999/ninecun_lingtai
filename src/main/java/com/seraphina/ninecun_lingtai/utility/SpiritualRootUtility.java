package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpiritualRootUtility {
    public static final List<ElementType> FIVE_ELEMENT_DIAGRAM_ORDER = List.of(
            ElementType.WOOD,
            ElementType.FIRE,
            ElementType.EARTH,
            ElementType.METAL,
            ElementType.WATER
    );
    public static final List<ElementType> FIVE_ELEMENT_NAME_ORDER = List.of(
            ElementType.WATER,
            ElementType.WOOD,
            ElementType.FIRE,
            ElementType.EARTH,
            ElementType.METAL
    );

    private static final Map<ElementType, String> ELEMENT_NAMES = Map.of(
            ElementType.METAL, "金",
            ElementType.WOOD, "木",
            ElementType.WATER, "水",
            ElementType.FIRE, "火",
            ElementType.EARTH, "土",
            ElementType.YIN_YANG, "阴阳"
    );
    private static final Map<ElementType, Integer> ELEMENT_COLORS = Map.of(
            ElementType.WATER, 0x328CFF,
            ElementType.FIRE, 0xFF413A,
            ElementType.EARTH, 0xE58A2B,
            ElementType.WOOD, 0x8B5A2B,
            ElementType.METAL, 0xFFD24A,
            ElementType.YIN_YANG, 0xC8C8D0
    );

    private SpiritualRootUtility() {
    }

    public static String getElementName(ElementType element) {
        String elementName = ELEMENT_NAMES.get(element);
        if (elementName == null) {
            throw new IllegalArgumentException("Unsupported spiritual root element: " + element);
        }
        return elementName;
    }

    public static int getElementColor(ElementType element) {
        Integer elementColor = ELEMENT_COLORS.get(element);
        if (elementColor == null) {
            throw new IllegalArgumentException("Unsupported spiritual root element: " + element);
        }
        return elementColor;
    }

    public static String getSpiritualRootName(Set<ElementType> elements) {
        if (isChaos(elements)) {
            return "混沌灵根";
        }
        StringBuilder spiritualRootName = new StringBuilder();
        if (elements.contains(ElementType.YIN_YANG)) {
            spiritualRootName.append(getElementName(ElementType.YIN_YANG));
            if (elements.size() > 1) {
                spiritualRootName.append('·');
            }
        }
        for (ElementType element : FIVE_ELEMENT_NAME_ORDER) {
            if (elements.contains(element)) {
                spiritualRootName.append(getElementName(element));
            }
        }
        return spiritualRootName.append("灵根").toString();
    }

    public static int getSpiritualRootColor(Set<ElementType> elements) {
        if (isChaos(elements)) {
            return 0x000000;
        }
        List<Integer> elementColors = new ArrayList<>(elements.size());
        for (ElementType element : elements) {
            elementColors.add(getElementColor(element));
        }
        return ColorUtility.mix(elementColors);
    }

    public static boolean isChaos(Set<ElementType> elements) {
        return elements.size() == SpiritualRoot.SELECTABLE_ELEMENTS.size()
                && elements.containsAll(SpiritualRoot.SELECTABLE_ELEMENTS);
    }
}
