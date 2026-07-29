package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.defense.EntityDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.player.DefenseResourceType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class CultivationDefenseUtility {
    private static final float PROTECTED_HEALTH = 20.0F;
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
    private static final float HIGH_DAMAGE_COMMAND_THRESHOLD = 1000.0F;
    private static final int GOLDEN_CORE_BODY_WIND_BASE = 500;
    private static final int GOLDEN_CORE_BODY_WIND_PER_LAYER = 100;
    private static final int NASCENT_SOUL_SPIRITUAL_POWER_BASE = 100;
    private static final int NASCENT_SOUL_SPIRITUAL_POWER_PER_LAYER = 300;
    private static final int DAO_BASE = 50;
    private static final int DAO_MINOR_LAYER_MULTIPLIER = 700;
    private static final int DAO_MAJOR_REALM_MULTIPLIER = 1000;
    private static final int GOLDEN_CORE_HIGH_DAMAGE_COST = 250;
    private static final int NASCENT_SOUL_HIGH_DAMAGE_COST = 10;
    private static final int SPIRIT_TRANSFORMATION_HIGH_DAMAGE_COST = 1;

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

    public static DefenseResourceType getDefenseResourceType(Optional<CultivationStage> stage) {
        return stage.map(CultivationDefenseUtility::getDefenseResourceType).orElse(DefenseResourceType.NONE);
    }

    public static DefenseResourceType getDefenseResourceType(CultivationStage stage) {
        if (stage.realm() == CultivationRealm.GOLDEN_CORE) {
            return DefenseResourceType.BODY_WIND;
        }
        if (stage.realm() == CultivationRealm.NASCENT_SOUL) {
            return DefenseResourceType.SPIRITUAL_POWER;
        }
        if (stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal()) {
            return DefenseResourceType.DAO;
        }
        return DefenseResourceType.NONE;
    }

    public static boolean isActiveDefense(
            Optional<CultivationStage> stage,
            DefenseResourceType resourceType,
            int currentResource
    ) {
        return stage.isPresent()
                && getDefenseResourceType(stage.get()) == resourceType
                && resourceType != DefenseResourceType.NONE
                && currentResource > 0;
    }

    public static boolean hasActiveDefense(LivingEntity entity) {
        Optional<CultivationStage> stage = CreatureCultivationData.getStage(entity);
        if (stage.isEmpty()) {
            return false;
        }
        DefenseResourceType resourceType = getDefenseResourceType(stage.get());
        return isActiveDefense(
                stage,
                resourceType,
                EntityDefenseResourceData.getCurrent(entity)
        );
    }

    public static boolean hasActivePlayerDefense(Player player) {
        return hasActiveDefense(player);
    }

    public static float getHealth(LivingEntity entity) {
        return getProtectedHealth(entity, entity.getHealth());
    }

    public static float getProtectedHealth(LivingEntity entity, float original) {
        return hasActiveDefense(entity)
                ? PROTECTED_HEALTH
                : original;
    }

    public static boolean isAlive(Entity entity) {
        return getProtectedAlive(entity, entity.isAlive());
    }

    public static boolean getProtectedAlive(Entity entity, boolean original) {
        return entity instanceof LivingEntity livingEntity && hasActiveDefense(livingEntity) || original;
    }

    public static boolean isDeadOrDying(LivingEntity entity) {
        return getProtectedDeadOrDying(entity, entity.isDeadOrDying());
    }

    public static boolean getProtectedDeadOrDying(LivingEntity entity, boolean original) {
        return !hasActiveDefense(entity) && original;
    }

    public static int getMaximumDefenseResource(Optional<CultivationStage> stage) {
        return stage.map(CultivationDefenseUtility::getMaximumDefenseResource).orElse(0);
    }

    public static int getMaximumDefenseResource(CultivationStage stage) {
        if (stage.realm() == CultivationRealm.GOLDEN_CORE) {
            return GOLDEN_CORE_BODY_WIND_BASE
                    + (stage.layer() - CultivationStage.MINIMUM_LAYER) * GOLDEN_CORE_BODY_WIND_PER_LAYER;
        }
        if (stage.realm() == CultivationRealm.NASCENT_SOUL) {
            return NASCENT_SOUL_SPIRITUAL_POWER_BASE
                    + (stage.layer() - CultivationStage.MINIMUM_LAYER) * NASCENT_SOUL_SPIRITUAL_POWER_PER_LAYER;
        }
        if (stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal()) {
            return getMaximumDaoResource(stage);
        }
        return 0;
    }

    public static int getDefenseResourceRepairAmount(
            Optional<CultivationStage> stage,
            double percentPerSecond
    ) {
        if (!Double.isFinite(percentPerSecond) || percentPerSecond <= 0.0D) {
            return 0;
        }
        int maximum = getMaximumDefenseResource(stage);
        if (maximum <= 0) {
            return 0;
        }
        return Math.min(
                maximum,
                Math.max(1, (int) Math.ceil(maximum * percentPerSecond / 100.0D))
        );
    }

    public static boolean isHighDamageCommandAttack(DamageSource damageSource, float amount) {
        return damageSource.is(DamageTypes.GENERIC_KILL) || amount >= HIGH_DAMAGE_COMMAND_THRESHOLD;
    }

    public static boolean isFullyImmuneToHighDamage(CultivationStage stage) {
        return stage.realm().ordinal() > CultivationRealm.SPIRIT_TRANSFORMATION.ordinal();
    }

    public static OptionalInt getHighDamageImmunityCost(CultivationStage stage) {
        if (stage.realm() == CultivationRealm.GOLDEN_CORE) {
            return OptionalInt.of(GOLDEN_CORE_HIGH_DAMAGE_COST);
        }
        if (stage.realm() == CultivationRealm.NASCENT_SOUL) {
            return OptionalInt.of(NASCENT_SOUL_HIGH_DAMAGE_COST);
        }
        if (stage.realm() == CultivationRealm.SPIRIT_TRANSFORMATION) {
            return OptionalInt.of(SPIRIT_TRANSFORMATION_HIGH_DAMAGE_COST);
        }
        return OptionalInt.empty();
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

    private static int getMaximumDaoResource(CultivationStage stage) {
        int spiritTransformationRealmIndex = CultivationRealm.SPIRIT_TRANSFORMATION.ordinal();
        int stageRealmIndex = stage.realm().ordinal();
        int maximum = DAO_BASE;
        for (int realmIndex = spiritTransformationRealmIndex; realmIndex <= stageRealmIndex; realmIndex++) {
            int realmNumber = realmIndex + 1;
            if (realmIndex > spiritTransformationRealmIndex) {
                maximum += DAO_MAJOR_REALM_MULTIPLIER * realmNumber;
            }
            int layersInRealm = realmIndex == stageRealmIndex ? stage.layer() : CultivationStage.MAXIMUM_LAYER;
            maximum += (layersInRealm - CultivationStage.MINIMUM_LAYER)
                    * DAO_MINOR_LAYER_MULTIPLIER
                    * realmNumber;
        }
        return maximum;
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
