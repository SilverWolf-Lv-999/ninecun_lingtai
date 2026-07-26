package com.seraphina.ninecun_lingtai.common.creature;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;

import java.util.function.Predicate;

/**
 * Replacement for a vanilla {@link AvoidEntityGoal} that keeps the original avoidance behaviour
 * but stops fearing targets whose cultivation is strictly weaker than the owner's. The distinct
 * type also marks the goal selector as already processed by the fear-inversion pass.
 */
public final class CultivationAwareAvoidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
    public CultivationAwareAvoidGoal(
            PathfinderMob mob,
            Class<T> avoidClass,
            Predicate<LivingEntity> avoidPredicate,
            float maxDist,
            double walkSpeedModifier,
            double sprintSpeedModifier,
            Predicate<LivingEntity> predicateOnAvoidEntity
    ) {
        super(
                mob,
                avoidClass,
                avoidPredicate,
                maxDist,
                walkSpeedModifier,
                sprintSpeedModifier,
                predicateOnAvoidEntity
        );
    }
}
