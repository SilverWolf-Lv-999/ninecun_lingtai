package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class CultivationDefenseUtility {
    private static final UUID MAX_HEALTH_MODIFIER_ID = UUID.fromString("2b467160-8bb5-43f2-9a76-78dcde68bcb5");
    private static final String MAX_HEALTH_MODIFIER_NAME = "Ninecun Lingtai cultivation health";
    private static final double QI_REFINING_HEALTH_PER_LAYER = 1.0D;
    private static final double FOUNDATION_HEALTH_PER_LAYER = 3.0D;
    private static final double BASE_REFLECTION_DAMAGE = 2.0D;
    private static final double REFLECTION_DAMAGE_PER_DEFENDER_STRENGTH = 0.5D;
    private static final double REFLECTION_DAMAGE_PER_STRENGTH_GAP = 0.25D;
    private static final double BASE_HORIZONTAL_KNOCKBACK = 0.55D;
    private static final double HORIZONTAL_KNOCKBACK_PER_DEFENDER_STRENGTH = 0.025D;
    private static final double HORIZONTAL_KNOCKBACK_PER_STRENGTH_GAP = 0.035D;
    private static final double MAXIMUM_HORIZONTAL_KNOCKBACK = 3.2D;
    private static final double BASE_VERTICAL_KNOCKBACK = 0.18D;
    private static final double VERTICAL_KNOCKBACK_PER_DEFENDER_REALM = 0.06D;
    private static final double VERTICAL_KNOCKBACK_PER_STRENGTH_GAP = 0.01D;
    private static final double MAXIMUM_VERTICAL_KNOCKBACK = 0.9D;

    private CultivationDefenseUtility() {
    }

    public static double getMaxHealthBonus(Optional<CultivationStage> stage) {
        return stage.map(CultivationDefenseUtility::getMaxHealthBonus).orElse(0.0D);
    }

    public static double getMaxHealthBonus(CultivationStage stage) {
        if (stage.realm() == CultivationRealm.QI_REFINING) {
            return stage.layer() * QI_REFINING_HEALTH_PER_LAYER;
        }
        double qiRefiningHealth = CultivationStage.MAXIMUM_LAYER * QI_REFINING_HEALTH_PER_LAYER;
        if (stage.realm() == CultivationRealm.FOUNDATION_ESTABLISHMENT) {
            return qiRefiningHealth + stage.layer() * FOUNDATION_HEALTH_PER_LAYER;
        }
        if (stage.realm().ordinal() >= CultivationRealm.GOLDEN_CORE.ordinal()) {
            return qiRefiningHealth + CultivationStage.MAXIMUM_LAYER * FOUNDATION_HEALTH_PER_LAYER;
        }
        return 0.0D;
    }

    public static void syncMaxHealthModifier(LivingEntity entity) {
        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }

        double healthBonus = getMaxHealthBonus(CreatureCultivationData.getStage(entity));
        AttributeModifier existingModifier = maxHealth.getModifier(MAX_HEALTH_MODIFIER_ID);
        if (existingModifier != null && Math.abs(existingModifier.getAmount() - healthBonus) <= 0.0001D) {
            clampHealthToMaximum(entity);
            return;
        }

        if (existingModifier != null) {
            maxHealth.removeModifier(MAX_HEALTH_MODIFIER_ID);
        }
        if (healthBonus > 0.0D) {
            maxHealth.addTransientModifier(new AttributeModifier(
                    MAX_HEALTH_MODIFIER_ID,
                    MAX_HEALTH_MODIFIER_NAME,
                    healthBonus,
                    AttributeModifier.Operation.ADDITION
            ));
        }
        clampHealthToMaximum(entity);
    }

    public static Optional<DefensiveReaction> getDefensiveReaction(
            CultivationStage defenderStage,
            Optional<CultivationStage> attackerStage,
            double incomingDamage
    ) {
        if (defenderStage.realm() == CultivationRealm.GOLDEN_CORE
                && isAttackerCultivationLower(defenderStage, attackerStage)) {
            return Optional.of(createDefensiveReaction(defenderStage, attackerStage, incomingDamage, true));
        }
        if (defenderStage.realm().ordinal() >= CultivationRealm.NASCENT_SOUL.ordinal()
                && isAttackerAtLeastOneMajorRealmLower(defenderStage, attackerStage)) {
            return Optional.of(createDefensiveReaction(defenderStage, attackerStage, incomingDamage, false));
        }
        return Optional.empty();
    }

    public static void applyKnockback(LivingEntity defender, LivingEntity attacker, DefensiveReaction reaction) {
        Vec3 direction = attacker.position().subtract(defender.position()).multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.0001D) {
            direction = defender.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (direction.lengthSqr() < 0.0001D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        direction = direction.normalize();
        attacker.push(
                direction.x * reaction.horizontalKnockback(),
                reaction.verticalKnockback(),
                direction.z * reaction.horizontalKnockback()
        );
    }

    public static int getCultivationStrength(Optional<CultivationStage> stage) {
        return stage.map(CultivationDefenseUtility::getCultivationStrength).orElse(0);
    }

    public static int getCultivationStrength(CultivationStage stage) {
        return CultivationUtility.getProgressionIndex(stage) + 1;
    }

    private static DefensiveReaction createDefensiveReaction(
            CultivationStage defenderStage,
            Optional<CultivationStage> attackerStage,
            double incomingDamage,
            boolean showsBodyShield
    ) {
        int defenderStrength = getCultivationStrength(defenderStage);
        int strengthGap = Math.max(1, defenderStrength - getCultivationStrength(attackerStage));
        double reflectedDamage = Math.max(
                incomingDamage,
                BASE_REFLECTION_DAMAGE
                        + defenderStrength * REFLECTION_DAMAGE_PER_DEFENDER_STRENGTH
                        + strengthGap * REFLECTION_DAMAGE_PER_STRENGTH_GAP
        );
        double horizontalKnockback = Math.min(
                MAXIMUM_HORIZONTAL_KNOCKBACK,
                BASE_HORIZONTAL_KNOCKBACK
                        + defenderStrength * HORIZONTAL_KNOCKBACK_PER_DEFENDER_STRENGTH
                        + strengthGap * HORIZONTAL_KNOCKBACK_PER_STRENGTH_GAP
        );
        double verticalKnockback = Math.min(
                MAXIMUM_VERTICAL_KNOCKBACK,
                BASE_VERTICAL_KNOCKBACK
                        + defenderStage.realm().ordinal() * VERTICAL_KNOCKBACK_PER_DEFENDER_REALM
                        + strengthGap * VERTICAL_KNOCKBACK_PER_STRENGTH_GAP
        );
        return new DefensiveReaction(reflectedDamage, horizontalKnockback, verticalKnockback, showsBodyShield);
    }

    private static boolean isAttackerCultivationLower(
            CultivationStage defenderStage,
            Optional<CultivationStage> attackerStage
    ) {
        return getCultivationStrength(attackerStage) < getCultivationStrength(defenderStage);
    }

    private static boolean isAttackerAtLeastOneMajorRealmLower(
            CultivationStage defenderStage,
            Optional<CultivationStage> attackerStage
    ) {
        int attackerRealm = attackerStage.map(stage -> stage.realm().ordinal()).orElse(-1);
        return attackerRealm <= defenderStage.realm().ordinal() - 1;
    }

    private static void clampHealthToMaximum(LivingEntity entity) {
        if (entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    public record DefensiveReaction(
            double reflectedDamage,
            double horizontalKnockback,
            double verticalKnockback,
            boolean showsBodyShield
    ) {
    }
}
