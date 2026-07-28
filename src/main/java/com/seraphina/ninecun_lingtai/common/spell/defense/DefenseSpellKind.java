package com.seraphina.ninecun_lingtai.common.spell.defense;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum DefenseSpellKind {
    METAL_BODY_SHIELD("metal_body_shield", "庚金护体罡", DefenseSpellShape.BODY_SHIELD, ElementType.METAL),
    WOOD_GUARDIAN("wood_guardian", "青木替命身", DefenseSpellShape.GUARDIAN, ElementType.WOOD),
    WATER_BODY_SHIELD("water_body_shield", "玄水流光衣", DefenseSpellShape.BODY_SHIELD, ElementType.WATER),
    FIRE_GUARDIAN("fire_guardian", "离火护法灯", DefenseSpellShape.GUARDIAN, ElementType.FIRE),
    EARTH_GUARDIAN("earth_guardian", "戊土镇岳傀", DefenseSpellShape.GUARDIAN, ElementType.EARTH),
    YIN_YANG_BODY_SHIELD("yin_yang_body_shield", "两仪衡界", DefenseSpellShape.BODY_SHIELD, ElementType.YIN_YANG),
    FIVE_ELEMENT_CYCLE("five_element_cycle", "五行轮转界", DefenseSpellShape.BODY_SHIELD, null),
    CHAOS_VOID("chaos_void", "混沌无相壁", DefenseSpellShape.BODY_SHIELD, ElementType.CHAOS),
    COMPOSITE_BODY_SHIELD("composite_body_shield", "合元灵幕", DefenseSpellShape.BODY_SHIELD, null),
    FOUR_PHASE_GUARDIAN("four_phase_guardian", "四象守御阵", DefenseSpellShape.GUARDIAN, null);

    private static final Map<String, DefenseSpellKind> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(DefenseSpellKind::getSerializedName, Function.identity()));

    private final String serializedName;
    private final String chineseName;
    private final DefenseSpellShape shape;
    private final ElementType fixedElement;

    DefenseSpellKind(
            String serializedName,
            String chineseName,
            DefenseSpellShape shape,
            ElementType fixedElement
    ) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
        this.shape = shape;
        this.fixedElement = fixedElement;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    public DefenseSpellShape getShape() {
        return this.shape;
    }

    public Optional<ElementType> getFixedElement() {
        return Optional.ofNullable(this.fixedElement);
    }

    /** 练气一层、圆满熟练度、九寸灵根时的基础防御点数。 */
    public double getBasePoints() {
        ForgeConfigSpec.DoubleValue value = switch (this) {
            case METAL_BODY_SHIELD -> CommonConfig.METAL_BODY_SHIELD_POINTS_PER_STRENGTH;
            case WOOD_GUARDIAN -> CommonConfig.WOOD_GUARDIAN_POINTS_PER_STRENGTH;
            case WATER_BODY_SHIELD -> CommonConfig.WATER_BODY_SHIELD_POINTS_PER_STRENGTH;
            case FIRE_GUARDIAN -> CommonConfig.FIRE_GUARDIAN_POINTS_PER_STRENGTH;
            case EARTH_GUARDIAN -> CommonConfig.EARTH_GUARDIAN_POINTS_PER_STRENGTH;
            case YIN_YANG_BODY_SHIELD -> CommonConfig.YIN_YANG_BODY_SHIELD_POINTS_PER_STRENGTH;
            case FIVE_ELEMENT_CYCLE -> CommonConfig.FIVE_ELEMENT_BODY_SHIELD_POINTS_PER_STRENGTH;
            case CHAOS_VOID -> CommonConfig.CHAOS_BODY_SHIELD_POINTS_PER_STRENGTH;
            case COMPOSITE_BODY_SHIELD -> CommonConfig.COMPOSITE_BODY_SHIELD_POINTS_PER_STRENGTH;
            case FOUR_PHASE_GUARDIAN -> CommonConfig.FOUR_PHASE_GUARDIAN_POINTS_PER_STRENGTH;
        };
        return value.get();
    }

    public double getTargetAttractionRadius() {
        return switch (this) {
            case WOOD_GUARDIAN -> 16.0D;
            case FIRE_GUARDIAN -> 20.0D;
            case EARTH_GUARDIAN -> 24.0D;
            case FOUR_PHASE_GUARDIAN -> 22.0D;
            default -> 0.0D;
        };
    }

    public static Optional<DefenseSpellKind> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
