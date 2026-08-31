package net.seraphina.ninecun_lingtai.common.spell.attack;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.utility.AttackSpellUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class BurstAttackSpell extends Spell {
    private final AttackSpellKind kind;

    public BurstAttackSpell(AttackSpellKind kind) {
        this.kind = kind;
    }

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Vec3 center = player.position();
        List<LivingEntity> victims = AttackSpellUtility.findBurstTargets(
                player,
                center,
                this.kind.getRadius()
        );
        float damage = AttackSpellUtility.getScaledDamage(
                player,
                this.kind.getBaseDamage(),
                context.cultivationStrength(),
                context.powerEfficiency()
        );
        DamageSource damageSource = AttackSpellUtility.createDamageSource(
                player,
                this.kind.getDamageTypeKey()
        );
        for (LivingEntity victim : victims) {
            victim.hurt(damageSource, damage);
            applyRider(player, victim, context.powerEfficiency());
        }
        AttackSpellUtility.spawnBurstParticles(
                serverLevel,
                center,
                this.kind.getRadius(),
                this.kind.getElement()
        );
        player.level().playSound(
                null,
                player.blockPosition(),
                getCastSound(),
                SoundSource.PLAYERS,
                1.0F,
                0.9F
        );
        return true;
    }

    private void applyRider(ServerPlayer player, LivingEntity victim, double powerEfficiency) {
        switch (this.kind) {
            case FLAME_SEA -> victim.setSecondsOnFire(
                    Math.max(2, (int) Math.round(5.0D * powerEfficiency))
            );
            case FROST_DOMAIN -> {
                victim.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        Math.max(20, (int) Math.round(160.0D * powerEfficiency)),
                        3
                ));
                victim.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN,
                        Math.max(20, (int) Math.round(160.0D * powerEfficiency)),
                        1
                ));
            }
            case CHAOS_REND -> {
                Vec3 push = victim.position()
                        .subtract(player.position())
                        .normalize()
                        .scale(0.8D + 0.6D * powerEfficiency);
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.3D, push.z));
                victim.hurtMarked = true;
            }
            default -> {
            }
        }
    }

    private SoundEvent getCastSound() {
        return switch (this.kind) {
            case FLAME_SEA -> SoundEvents.BLAZE_SHOOT;
            case FROST_DOMAIN -> SoundEvents.PLAYER_HURT_FREEZE;
            case CHAOS_REND -> SoundEvents.WARDEN_SONIC_BOOM;
            default -> SoundEvents.EVOKER_CAST_SPELL;
        };
    }
}
