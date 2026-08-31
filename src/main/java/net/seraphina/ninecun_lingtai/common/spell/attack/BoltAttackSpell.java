package net.seraphina.ninecun_lingtai.common.spell.attack;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.utility.AttackSpellUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class BoltAttackSpell extends Spell {
    private final AttackSpellKind kind;

    public BoltAttackSpell(AttackSpellKind kind) {
        this.kind = kind;
    }

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        Optional<LivingEntity> target = AttackSpellUtility.findBoltTarget(player, this.kind.getRange());
        if (target.isEmpty()) {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "目力所及无可袭之敌，《" + this.kind.getChineseName() + "》未出手"
                    ),
                    true
            );
            return false;
        }
        LivingEntity victim = target.get();
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        float damage = AttackSpellUtility.getScaledDamage(
                player,
                this.kind.getBaseDamage(),
                context.cultivationStrength(),
                context.powerEfficiency()
        );
        AttackSpellUtility.spawnBoltTrail(
                serverLevel,
                player.getEyePosition(),
                victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D),
                this.kind.getElement()
        );
        victim.hurt(AttackSpellUtility.createDamageSource(player, this.kind.getDamageTypeKey()), damage);
        applyRider(victim, context.powerEfficiency());
        serverLevel.sendParticles(
                AttackSpellUtility.getTrailParticle(this.kind.getElement()),
                victim.getX(),
                victim.getY(0.5D),
                victim.getZ(),
                12,
                victim.getBbWidth() * 0.5D,
                victim.getBbHeight() * 0.35D,
                victim.getBbWidth() * 0.5D,
                0.05D
        );
        player.level().playSound(
                null,
                player.blockPosition(),
                getCastSound(),
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        );
        return true;
    }

    private void applyRider(LivingEntity victim, double powerEfficiency) {
        switch (this.kind) {
            case WOOD_BIND -> {
                victim.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        scaleDuration(100, powerEfficiency),
                        4
                ));
                victim.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN,
                        scaleDuration(100, powerEfficiency),
                        2
                ));
            }
            case ICE_SPIKE -> victim.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    scaleDuration(80, powerEfficiency),
                    2
            ));
            case YIN_FINGER -> victim.addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    scaleDuration(120, powerEfficiency),
                    1
            ));
            default -> {
            }
        }
    }

    private SoundEvent getCastSound() {
        return switch (this.kind) {
            case METAL_BLADE -> SoundEvents.TRIDENT_THROW;
            case WOOD_BIND -> SoundEvents.CAVE_VINES_PLACE;
            case ICE_SPIKE -> SoundEvents.GLASS_BREAK;
            case YIN_FINGER -> SoundEvents.SOUL_ESCAPE;
            default -> SoundEvents.EVOKER_CAST_SPELL;
        };
    }

    private static int scaleDuration(int baseTicks, double powerEfficiency) {
        return Math.max(20, (int) Math.round(baseTicks * powerEfficiency));
    }
}
