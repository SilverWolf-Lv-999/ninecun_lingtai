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

    private static final Map<ElementType, String> ELEMENT_NAMES = Map.ofEntries(
            Map.entry(ElementType.METAL, "金"),
            Map.entry(ElementType.WOOD, "木"),
            Map.entry(ElementType.WATER, "水"),
            Map.entry(ElementType.FIRE, "火"),
            Map.entry(ElementType.EARTH, "土"),
            Map.entry(ElementType.YIN_YANG, "阴阳"),
            Map.entry(ElementType.THUNDER, "雷"),
            Map.entry(ElementType.CHAOS, "混沌")
    );
    private static final Map<ElementType, Integer> ELEMENT_COLORS = Map.ofEntries(
            Map.entry(ElementType.WATER, 0x328CFF),
            Map.entry(ElementType.FIRE, 0xFF413A),
            Map.entry(ElementType.EARTH, 0xC98A4A),
            Map.entry(ElementType.WOOD, 0x67B85F),
            Map.entry(ElementType.METAL, 0xE6C45C),
            Map.entry(ElementType.YIN_YANG, 0xC8C8D0),
            Map.entry(ElementType.THUNDER, 0xA67CFF),
            Map.entry(ElementType.CHAOS, 0xC46CFF)
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
        if (isFiveElementRoot(elements)) {
            return "五行灵根";
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

    public static int getSpiritualRootGlowColor(Set<ElementType> elements) {
        return isChaos(elements)
                ? getElementColor(ElementType.CHAOS)
                : getSpiritualRootColor(elements);
    }

    public static boolean isChaos(Set<ElementType> elements) {
        return elements.size() == SpiritualRoot.SELECTABLE_ELEMENTS.size()
                && elements.containsAll(SpiritualRoot.SELECTABLE_ELEMENTS);
    }

    private static boolean isFiveElementRoot(Set<ElementType> elements) {
        return elements.size() == ElementType.FIVE_ELEMENTS.size()
                && elements.containsAll(ElementType.FIVE_ELEMENTS);
    }

    public static Set<ElementType> getIndependentRootElements(SpiritualRoot spiritualRoot) {
        return spiritualRoot.isChaos()
                ? Set.of(ElementType.CHAOS)
                : spiritualRoot.elements();
    }
}
