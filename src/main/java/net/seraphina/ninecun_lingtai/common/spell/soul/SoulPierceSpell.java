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
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class SoulPierceSpell extends Spell {
    private static final double RANGE = 32.0D;
    private static final double BASE_DAMAGE_PER_STAR = 3.0D;
    private static final double REALM_DAMAGE_GROWTH = 0.5D;

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (SoulInjuryUtility.isSoulSpellForbidden(player)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_injury_spell_forbidden"),
                    true
            );
            return false;
        }
        Optional<LivingEntity> target = AttackSpellUtility.findBoltTarget(player, RANGE);
        if (target.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_pierce_no_target"),
                    true
            );
            return false;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        LivingEntity victim = target.get();
        int casterStars = SoulSpellUtility.getCasterStars(player);
        int targetStars = SoulSpellUtility.getTargetStars(victim);
        double realmMultiplier = 1.0D
                + REALM_DAMAGE_GROWTH * SoulSpellUtility.getMajorRealmIndex(context.cultivationStrength());
        double damage = BASE_DAMAGE_PER_STAR
                * casterStars
                * realmMultiplier
                * context.powerEfficiency();
        boolean resisted = targetStars >= casterStars;
        if (resisted) {
            damage *= 0.5D;
        }
        AttackSpellUtility.spawnBoltTrail(
                serverLevel,
                player.getEyePosition(),
                victim.getEyePosition(),
                net.seraphina.ninecun_lingtai.api.element.ElementType.YIN_YANG
        );
        victim.hurt(
                AttackSpellUtility.createDamageSource(player, ModDamageTypes.SOUL_PIERCE),
                (float) damage
        );
        if (victim instanceof ServerPlayer victimPlayer) {
            SoulInjuryUtility.addInjury(
                    victimPlayer,
                    CommonConfig.SOUL_INJURY_SOURCE_PIERCE.get(),
                    SoulInjuryUtility.InjurySource.SOUL_PIERCE_HIT
            );
        }
        serverLevel.sendParticles(
                ParticleTypes.SCULK_SOUL,
                victim.getX(),
                victim.getEyeY(),
                victim.getZ(),
                16,
                0.3D,
                0.3D,
                0.3D,
                0.05D
        );
        serverLevel.playSound(
                null,
                victim.blockPosition(),
                SoundEvents.WARDEN_ATTACK_IMPACT,
                SoundSource.PLAYERS,
                0.8F,
                1.6F
        );
        if (resisted) {
            SoulSpellUtility.applyResistedBacklash(player);
        }
        SoulSpellUtility.applyWeakSoulBacklash(player, context.powerEfficiency());
        return true;
    }
}
