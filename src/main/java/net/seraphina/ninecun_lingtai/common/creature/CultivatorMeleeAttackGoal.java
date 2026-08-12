package net.seraphina.ninecun_lingtai.common.creature;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

/**
 * Melee goal usable by creatures without an attack damage attribute (cows, cats, ...). The base
 * hit is deliberately small; the realm bonus damage handler layers the cultivation damage on top.
 */
public final class CultivatorMeleeAttackGoal extends MeleeAttackGoal {
    private static final float BASE_ATTACK_DAMAGE = 2.0F;

    public CultivatorMeleeAttackGoal(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier, true);
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distanceToTargetSquared) {
        if (distanceToTargetSquared > this.getAttackReachSqr(target) || !this.isTimeToAttack()) {
            return;
        }
        this.resetAttackCooldown();
        this.mob.swing(InteractionHand.MAIN_HAND);
        if (this.mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            this.mob.doHurtTarget(target);
            return;
        }
        if (target.hurt(this.mob.damageSources().mobAttack(this.mob), BASE_ATTACK_DAMAGE)) {
            this.mob.setLastHurtMob(target);
        }
    }
}
