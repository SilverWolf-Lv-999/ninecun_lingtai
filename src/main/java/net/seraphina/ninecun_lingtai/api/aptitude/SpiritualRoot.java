package net.seraphina.ninecun_lingtai.api.aptitude;

import net.seraphina.ninecun_lingtai.api.element.ElementType;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public record SpiritualRoot(Set<ElementType> elements, float lengthInches) {
    public static final float MINIMUM_LENGTH_INCHES = 1.0F;
    public static final float MAXIMUM_LENGTH_INCHES = 9.0F;
    public static final Set<ElementType> SELECTABLE_ELEMENTS = Set.copyOf(EnumSet.of(
            ElementType.METAL,
            ElementType.WOOD,
            ElementType.WATER,
            ElementType.FIRE,
            ElementType.EARTH,
            ElementType.YIN_YANG
    ));

    public SpiritualRoot {
        elements = Set.copyOf(elements);
        if (elements.isEmpty() || !SELECTABLE_ELEMENTS.containsAll(elements)) {
            throw new IllegalArgumentException("A spiritual root must contain selectable elements");
        }
        if (!Float.isFinite(lengthInches)
                || lengthInches < MINIMUM_LENGTH_INCHES
                || lengthInches > MAXIMUM_LENGTH_INCHES) {
            throw new IllegalArgumentException("Spiritual root length must be between one and nine inches");
        }
    }

    public boolean isChaos() {
        return this.elements.size() == SELECTABLE_ELEMENTS.size()
                && this.elements.containsAll(SELECTABLE_ELEMENTS);
    }

    public float quality() {
        return this.lengthInches / MAXIMUM_LENGTH_INCHES;
    }

    public int getElementMask() {
        return createElementMask(this.elements);
    }

    public static int createElementMask(Set<ElementType> elements) {
        int elementMask = 0;
        for (ElementType element : elements) {
            if (!SELECTABLE_ELEMENTS.contains(element)) {
                throw new IllegalArgumentException("Unselectable spiritual root element: " + element);
            }
            elementMask |= 1 << element.ordinal();
        }
        return elementMask;
    }

    public static Optional<SpiritualRoot> fromElementMask(int elementMask, float lengthInches) {
        EnumSet<ElementType> elements = EnumSet.noneOf(ElementType.class);
        int remainingMask = elementMask;
        for (ElementType element : SELECTABLE_ELEMENTS) {
            int elementBit = 1 << element.ordinal();
            if ((elementMask & elementBit) != 0) {
                elements.add(element);
                remainingMask &= ~elementBit;
            }
        }
        if (remainingMask != 0 || elements.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SpiritualRoot(elements, lengthInches));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
