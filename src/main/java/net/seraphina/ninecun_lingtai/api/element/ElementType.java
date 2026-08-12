package net.seraphina.ninecun_lingtai.api.element;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ElementType {
    METAL("metal"),
    WOOD("wood"),
    WATER("water"),
    FIRE("fire"),
    EARTH("earth"),
    YIN_YANG("yin_yang"),
    THUNDER("thunder"),
    CHAOS("chaos");

    public static final List<ElementType> FIVE_ELEMENTS = List.of(METAL, WOOD, WATER, FIRE, EARTH);

    private static final Map<String, ElementType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ElementType::getSerializedName, Function.identity()));
    private static final Map<ElementType, ElementType> GENERATION_RELATIONS = Map.of(
            WOOD, FIRE,
            FIRE, EARTH,
            EARTH, METAL,
            METAL, WATER,
            WATER, WOOD
    );
    private static final Map<ElementType, ElementType> RESTRAINT_RELATIONS = Map.of(
            WOOD, EARTH,
            EARTH, WATER,
            WATER, FIRE,
            FIRE, METAL,
            METAL, WOOD
    );

    private final String serializedName;

    ElementType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public boolean isFiveElement() {
        return GENERATION_RELATIONS.containsKey(this);
    }

    public ElementRelation getRelationTo(ElementType targetElement) {
        if (this == targetElement) {
            return ElementRelation.SAME;
        }
        if (this == CHAOS) {
            return ElementRelation.SUPPRESSES;
        }
        if (targetElement == CHAOS) {
            return ElementRelation.SUPPRESSED_BY;
        }
        if (GENERATION_RELATIONS.get(this) == targetElement) {
            return ElementRelation.GENERATES;
        }
        if (GENERATION_RELATIONS.get(targetElement) == this) {
            return ElementRelation.GENERATED_BY;
        }
        if (RESTRAINT_RELATIONS.get(this) == targetElement) {
            return ElementRelation.OVERCOMES;
        }
        if (RESTRAINT_RELATIONS.get(targetElement) == this) {
            return ElementRelation.OVERCOME_BY;
        }
        return ElementRelation.NEUTRAL;
    }

    public static Optional<ElementType> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
