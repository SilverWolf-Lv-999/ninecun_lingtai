package net.seraphina.ninecun_lingtai.common.creature;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

import javax.annotation.Nullable;

/**
 * Retaliation targeting for peaceful creatures that carry cultivation. The distinct type lets the
 * installer detect that a creature has already been equipped.
 */
public final class CultivatorHurtByTargetGoal extends HurtByTargetGoal {
    public CultivatorHurtByTargetGoal(PathfinderMob mob) {
        super(mob);
    }

    @Override
    protected boolean canAttack(@Nullable LivingEntity target, TargetingConditions conditions) {
        if (this.mob instanceof TamableAnimal tamableAnimal && tamableAnimal.isOwnedBy(target)) {
            return false;
        }
        return super.canAttack(target, conditions);
    }
}
