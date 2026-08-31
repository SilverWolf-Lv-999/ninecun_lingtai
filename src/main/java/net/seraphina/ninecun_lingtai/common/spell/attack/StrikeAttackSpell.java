package net.seraphina.ninecun_lingtai.common.spell.attack;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.utility.AttackSpellUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class StrikeAttackSpell extends Spell {
    private final AttackSpellKind kind;

    public StrikeAttackSpell(AttackSpellKind kind) {
        this.kind = kind;
    }

    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Vec3 strikePoint = AttackSpellUtility.findStrikePoint(player, this.kind.getRange());
        List<LivingEntity> victims = AttackSpellUtility.findBurstTargets(
                player,
                strikePoint,
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
            if (this.kind == AttackSpellKind.EARTH_SPIKE) {
                victim.setDeltaMovement(victim.getDeltaMovement().add(
                        0.0D,
                        0.45D + 0.35D * context.powerEfficiency(),
                        0.0D
                ));
                victim.hurtMarked = true;
            }
        }
        playImpact(serverLevel, player, strikePoint);
        return true;
    }

    private void playImpact(ServerLevel serverLevel, ServerPlayer player, Vec3 strikePoint) {
        switch (this.kind) {
            case EARTH_SPIKE -> {
                AttackSpellUtility.spawnBurstParticles(
                        serverLevel,
                        strikePoint,
                        this.kind.getRadius(),
                        this.kind.getElement()
                );
                serverLevel.playSound(
                        null,
                        net.minecraft.core.BlockPos.containing(strikePoint),
                        SoundEvents.ROOTED_DIRT_BREAK,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.7F
                );
            }
            case PALM_THUNDER -> spawnVisualLightning(serverLevel, player, strikePoint, 1);
            case THUNDER_FALL -> spawnVisualLightning(serverLevel, player, strikePoint, 3);
            default -> AttackSpellUtility.spawnBurstParticles(
                    serverLevel,
                    strikePoint,
                    Math.max(1.0D, this.kind.getRadius()),
                    this.kind.getElement()
            );
        }
    }

    private static void spawnVisualLightning(
            ServerLevel serverLevel,
            ServerPlayer player,
            Vec3 strikePoint,
            int boltCount
    ) {
        for (int index = 0; index < boltCount; index++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
            if (bolt == null) {
                continue;
            }
            double offsetX = index == 0 ? 0.0D : (serverLevel.random.nextDouble() - 0.5D) * 4.0D;
            double offsetZ = index == 0 ? 0.0D : (serverLevel.random.nextDouble() - 0.5D) * 4.0D;
            bolt.moveTo(strikePoint.x + offsetX, strikePoint.y, strikePoint.z + offsetZ);
            bolt.setVisualOnly(true);
            bolt.setCause(player);
            serverLevel.addFreshEntity(bolt);
        }
    }
}
