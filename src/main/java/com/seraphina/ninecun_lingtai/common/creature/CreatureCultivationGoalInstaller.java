package com.seraphina.ninecun_lingtai.common.creature;

import com.mojang.logging.LogUtils;
import com.seraphina.ninecun_lingtai.utility.CreatureCultivationUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Equips a cultivating creature with the behaviours its cultivation implies: peaceful animals
 * retaliate and hunt down attackers instead of panicking, and innate fears (creeper vs cat,
 * skeleton vs wolf, ...) invert into aggression when the feared creature is the weaker cultivator.
 */
public final class CreatureCultivationGoalInstaller {
    private static final Logger LOGGER = LogUtils.getLogger();
    // Priority zero outranks PanicGoal so an attacked cultivator fights instead of fleeing.
    private static final int RETALIATION_ATTACK_PRIORITY = 0;
    private static final int RETALIATION_TARGET_PRIORITY = 1;
    private static final int FEAR_HUNT_TARGET_PRIORITY = 3;
    private static final int FEAR_HUNT_SCAN_INTERVAL = 10;
    private static final double RETALIATION_SPEED_MODIFIER = 1.25D;
    private static final double FOLLOW_RANGE_BONUS = 32.0D;
    private static final UUID FOLLOW_RANGE_MODIFIER_ID = UUID.fromString(
            "5f28e0bb-58cf-465b-b6da-c19c6a1f55f0"
    );
    private static final Field AVOID_CLASS_FIELD;
    private static final Field MAX_DIST_FIELD;
    private static final Field WALK_SPEED_FIELD;
    private static final Field SPRINT_SPEED_FIELD;
    private static final Field AVOID_PREDICATE_FIELD;
    private static final Field ON_AVOID_PREDICATE_FIELD;

    static {
        // Located by type rather than name so the lookup survives obfuscated field names.
        Field avoidClassField = null;
        Field maxDistField = null;
        List<Field> doubleFields = new ArrayList<>();
        List<Field> predicateFields = new ArrayList<>();
        for (Field field : AvoidEntityGoal.class.getDeclaredFields()) {
            if (field.getType() == Class.class) {
                avoidClassField = field;
            } else if (field.getType() == float.class) {
                maxDistField = field;
            } else if (field.getType() == double.class) {
                doubleFields.add(field);
            } else if (field.getType() == Predicate.class) {
                predicateFields.add(field);
            }
        }
        if (avoidClassField == null
                || maxDistField == null
                || doubleFields.size() != 2
                || predicateFields.size() != 2) {
            LOGGER.warn("Unexpected AvoidEntityGoal layout; cultivator fear inversion is disabled");
            AVOID_CLASS_FIELD = null;
            MAX_DIST_FIELD = null;
            WALK_SPEED_FIELD = null;
            SPRINT_SPEED_FIELD = null;
            AVOID_PREDICATE_FIELD = null;
            ON_AVOID_PREDICATE_FIELD = null;
        } else {
            avoidClassField.setAccessible(true);
            maxDistField.setAccessible(true);
            doubleFields.forEach(field -> field.setAccessible(true));
            predicateFields.forEach(field -> field.setAccessible(true));
            AVOID_CLASS_FIELD = avoidClassField;
            MAX_DIST_FIELD = maxDistField;
            WALK_SPEED_FIELD = doubleFields.get(0);
            SPRINT_SPEED_FIELD = doubleFields.get(1);
            AVOID_PREDICATE_FIELD = predicateFields.get(0);
            ON_AVOID_PREDICATE_FIELD = predicateFields.get(1);
        }
    }

    private CreatureCultivationGoalInstaller() {
    }

    public static void install(Mob mob) {
        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return;
        }
        invertFearGoals(pathfinderMob);
        installRetaliationGoals(pathfinderMob);
        extendFollowRange(pathfinderMob);
    }

    private static void invertFearGoals(PathfinderMob mob) {
        if (AVOID_CLASS_FIELD == null) {
            return;
        }
        // Only plain AvoidEntityGoal instances are rewritten; specialised subclasses (for example
        // the tamed-cat variant) carry extra conditions that a rebuilt goal would lose.
        List<WrappedGoal> avoidGoals = mob.goalSelector.getAvailableGoals().stream()
                .filter(wrappedGoal -> wrappedGoal.getGoal().getClass() == AvoidEntityGoal.class)
                .toList();
        for (WrappedGoal wrappedGoal : avoidGoals) {
            replaceAvoidGoal(mob, wrappedGoal.getPriority(), (AvoidEntityGoal<?>) wrappedGoal.getGoal());
        }
    }

    private static <T extends LivingEntity> void replaceAvoidGoal(
            PathfinderMob mob,
            int priority,
            AvoidEntityGoal<T> originalGoal
    ) {
        Class<T> avoidClass;
        float maxDist;
        double walkSpeedModifier;
        double sprintSpeedModifier;
        Predicate<LivingEntity> avoidPredicate;
        Predicate<LivingEntity> predicateOnAvoidEntity;
        try {
            @SuppressWarnings("unchecked")
            Class<T> extractedAvoidClass = (Class<T>) AVOID_CLASS_FIELD.get(originalGoal);
            @SuppressWarnings("unchecked")
            Predicate<LivingEntity> extractedAvoidPredicate =
                    (Predicate<LivingEntity>) AVOID_PREDICATE_FIELD.get(originalGoal);
            @SuppressWarnings("unchecked")
            Predicate<LivingEntity> extractedOnAvoidPredicate =
                    (Predicate<LivingEntity>) ON_AVOID_PREDICATE_FIELD.get(originalGoal);
            avoidClass = extractedAvoidClass;
            avoidPredicate = extractedAvoidPredicate;
            predicateOnAvoidEntity = extractedOnAvoidPredicate;
            maxDist = MAX_DIST_FIELD.getFloat(originalGoal);
            walkSpeedModifier = WALK_SPEED_FIELD.getDouble(originalGoal);
            sprintSpeedModifier = SPRINT_SPEED_FIELD.getDouble(originalGoal);
        } catch (ReflectiveOperationException | ClassCastException exception) {
            LOGGER.warn("Failed to read an avoid goal of {}; leaving it untouched", mob.getType(), exception);
            return;
        }

        mob.goalSelector.removeGoal(originalGoal);
        mob.goalSelector.addGoal(priority, new CultivationAwareAvoidGoal<>(
                mob,
                avoidClass,
                avoidPredicate,
                maxDist,
                walkSpeedModifier,
                sprintSpeedModifier,
                predicateOnAvoidEntity.and(target -> !CreatureCultivationUtility.isStrictlyWeaker(target, mob))
        ));
        mob.targetSelector.addGoal(FEAR_HUNT_TARGET_PRIORITY, new NearestAttackableTargetGoal<>(
                mob,
                avoidClass,
                FEAR_HUNT_SCAN_INTERVAL,
                true,
                false,
                target -> CreatureCultivationUtility.isStrictlyWeaker(target, mob)
        ));
    }

    private static void installRetaliationGoals(PathfinderMob mob) {
        if (mob instanceof Enemy) {
            return;
        }
        boolean hasRetaliationTarget = mob.targetSelector.getAvailableGoals().stream()
                .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof HurtByTargetGoal);
        if (!hasRetaliationTarget) {
            mob.targetSelector.addGoal(RETALIATION_TARGET_PRIORITY, new CultivatorHurtByTargetGoal(mob));
        }
        boolean hasMeleeAttack = mob.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrappedGoal -> wrappedGoal.getGoal() instanceof MeleeAttackGoal);
        if (!hasMeleeAttack) {
            mob.goalSelector.addGoal(
                    RETALIATION_ATTACK_PRIORITY,
                    new CultivatorMeleeAttackGoal(mob, RETALIATION_SPEED_MODIFIER)
            );
        }
    }

    private static void extendFollowRange(PathfinderMob mob) {
        AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange == null || followRange.getModifier(FOLLOW_RANGE_MODIFIER_ID) != null) {
            return;
        }
        followRange.addPermanentModifier(new AttributeModifier(
                FOLLOW_RANGE_MODIFIER_ID,
                "Cultivator follow range",
                FOLLOW_RANGE_BONUS,
                AttributeModifier.Operation.ADDITION
        ));
    }
}
