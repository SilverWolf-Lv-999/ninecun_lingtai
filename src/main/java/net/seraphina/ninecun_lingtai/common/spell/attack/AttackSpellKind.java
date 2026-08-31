package net.seraphina.ninecun_lingtai.common.spell.attack;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum AttackSpellKind {
    METAL_BLADE("metal_blade", "庚金刃", AttackSpellShape.BOLT, ElementType.METAL,
            ModDamageTypes.SPELL_METAL, 3.0D, 24.0D, 0.0D),
    WOOD_BIND("wood_bind", "缚灵青藤", AttackSpellShape.BOLT, ElementType.WOOD,
            ModDamageTypes.SPELL_WOOD, 1.5D, 20.0D, 0.0D),
    ICE_SPIKE("ice_spike", "寒冰锥", AttackSpellShape.BOLT, ElementType.WATER,
            ModDamageTypes.SPELL_WATER, 2.5D, 24.0D, 0.0D),
    YIN_FINGER("yin_finger", "阴煞指", AttackSpellShape.BOLT, ElementType.YIN_YANG,
            ModDamageTypes.SPELL_YIN_YANG, 2.0D, 20.0D, 0.0D),
    EARTH_SPIKE("earth_spike", "崩地刺", AttackSpellShape.STRIKE, ElementType.EARTH,
            ModDamageTypes.SPELL_EARTH, 2.0D, 20.0D, 2.5D),
    PALM_THUNDER("palm_thunder", "掌心雷", AttackSpellShape.STRIKE, ElementType.THUNDER,
            ModDamageTypes.SPELL_THUNDER, 5.0D, 28.0D, 2.0D),
    THUNDER_FALL("thunder_fall", "九霄神雷", AttackSpellShape.STRIKE, ElementType.THUNDER,
            ModDamageTypes.SPELL_THUNDER, 8.0D, 40.0D, 4.0D),
    FLAME_SEA("flame_sea", "烈焰焚海", AttackSpellShape.BURST, ElementType.FIRE,
            ModDamageTypes.SPELL_FIRE, 3.0D, 0.0D, 6.0D),
    FROST_DOMAIN("frost_domain", "冰封领域", AttackSpellShape.BURST, ElementType.WATER,
            ModDamageTypes.SPELL_WATER, 1.5D, 0.0D, 8.0D),
    CHAOS_REND("chaos_rend", "混沌裂灭", AttackSpellShape.BURST, ElementType.CHAOS,
            ModDamageTypes.SPELL_CHAOS, 10.0D, 0.0D, 6.0D);

    private static final Map<String, AttackSpellKind> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(AttackSpellKind::getSerializedName, Function.identity()));

    private final String serializedName;
    private final String chineseName;
    private final AttackSpellShape shape;
    private final ElementType element;
    private final ResourceKey<DamageType> damageTypeKey;
    private final double baseDamage;
    private final double range;
    private final double radius;

    AttackSpellKind(
            String serializedName,
            String chineseName,
            AttackSpellShape shape,
            ElementType element,
            ResourceKey<DamageType> damageTypeKey,
            double baseDamage,
            double range,
            double radius
    ) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
        this.shape = shape;
        this.element = element;
        this.damageTypeKey = damageTypeKey;
        this.baseDamage = baseDamage;
        this.range = range;
        this.radius = radius;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    public AttackSpellShape getShape() {
        return this.shape;
    }

    public ElementType getElement() {
        return this.element;
    }

    public ResourceKey<DamageType> getDamageTypeKey() {
        return this.damageTypeKey;
    }

    public double getBaseDamage() {
        return this.baseDamage;
    }

    public double getRange() {
        return this.range;
    }

    public double getRadius() {
        return this.radius;
    }

    public static Optional<AttackSpellKind> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
