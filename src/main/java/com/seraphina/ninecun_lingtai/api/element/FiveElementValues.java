package com.seraphina.ninecun_lingtai.api.element;

import java.util.Comparator;

public record FiveElementValues(float metal, float wood, float water, float fire, float earth) {
    public static final FiveElementValues EMPTY = new FiveElementValues(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    public static final FiveElementValues NEUTRAL = new FiveElementValues(1.0F, 1.0F, 1.0F, 1.0F, 1.0F);

    public FiveElementValues {
        validateValue(metal, ElementType.METAL);
        validateValue(wood, ElementType.WOOD);
        validateValue(water, ElementType.WATER);
        validateValue(fire, ElementType.FIRE);
        validateValue(earth, ElementType.EARTH);
    }

    public float get(ElementType element) {
        return switch (element) {
            case METAL -> this.metal;
            case WOOD -> this.wood;
            case WATER -> this.water;
            case FIRE -> this.fire;
            case EARTH -> this.earth;
            case YIN_YANG, THUNDER, CHAOS -> throw new IllegalArgumentException(
                    "Only five-element values are available: " + element
            );
        };
    }

    public float maximum() {
        return Math.max(
                Math.max(this.metal, this.wood),
                Math.max(this.water, Math.max(this.fire, this.earth))
        );
    }

    public ElementType dominantElement() {
        return ElementType.FIVE_ELEMENTS.stream()
                .max(Comparator.comparingDouble(this::get))
                .orElse(ElementType.METAL);
    }

    private static void validateValue(float value, ElementType element) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(element + " value must be finite and non-negative");
        }
    }
}
