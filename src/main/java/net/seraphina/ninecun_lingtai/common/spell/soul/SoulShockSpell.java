package net.seraphina.ninecun_lingtai.common.spell.soul;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import net.seraphina.ninecun_lingtai.utility.AttackSpellUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class SoulShockSpell extends Spell {
    private static final double RADIUS = 12.0D;
    private static final double BASE_DAMAGE_PER_STAR_GAP = 2.0D;
    private static final double REALM_DAMAGE_GROWTH = 0.3D;
    private static final int STUN_TICKS_PER_STAR_GAP = 40;

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (SoulInjuryUtility.isSoulSpellForbidden(player)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_injury_spell_forbidden"),
                    true
            );
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        int casterStars = SoulSpellUtility.getCasterStars(player);
        List<LivingEntity> victims = AttackSpellUtility.findBurstTargets(
                player,
                player.position(),
                RADIUS
        );
        DamageSource damageSource = AttackSpellUtility.createDamageSource(
                player,
                ModDamageTypes.SOUL_PIERCE
        );
        double realmMultiplier = 1.0D
                + REALM_DAMAGE_GROWTH * SoulSpellUtility.getMajorRealmIndex(context.cultivationStrength());
        boolean higherSoulPresent = false;
        for (LivingEntity victim : victims) {
            int starGap = casterStars - SoulSpellUtility.getTargetStars(victim);
            if (starGap <= 0) {
                higherSoulPresent = true;
                continue;
            }
            int stunTicks = Math.max(
                    20,
                    (int) Math.round(starGap * STUN_TICKS_PER_STAR_GAP * context.powerEfficiency())
            );
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stunTicks, 4));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stunTicks, 2));
            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, stunTicks, 0));
            double damage = BASE_DAMAGE_PER_STAR_GAP
                    * starGap
                    * realmMultiplier
                    * context.powerEfficiency();
            victim.hurt(damageSource, (float) damage);
            if (victim instanceof ServerPlayer victimPlayer) {
                SoulInjuryUtility.addInjury(
                        victimPlayer,
                        CommonConfig.SOUL_INJURY_SOURCE_SHOCK.get(),
                        SoulInjuryUtility.InjurySource.SOUL_SHOCK_HIT
                );
            }
        }
        serverLevel.sendParticles(
                ParticleTypes.SONIC_BOOM,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
        serverLevel.sendParticles(
                ParticleTypes.SCULK_SOUL,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                48,
                RADIUS * 0.4D,
                0.8D,
                RADIUS * 0.4D,
                0.1D
        );
        serverLevel.playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS,
                1.2F,
                1.3F
        );
        if (higherSoulPresent) {
            SoulSpellUtility.applyResistedBacklash(player);
        }
        SoulSpellUtility.applyWeakSoulBacklash(player, context.powerEfficiency());
        return true;
    }
}
