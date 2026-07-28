package com.seraphina.ninecun_lingtai.common.spell;

import com.seraphina.ninecun_lingtai.api.spell.Spell;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

/** 火球术的神通实现；名称、品级与消耗见 data/ninecun_lingtai/techniques/fireball_spell.json。 */
public final class FireballSpell extends Spell {
    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        Vec3 lookDirection = player.getLookAngle();
        // 熟练度越深、灵根资质越好，火球去势越疾。
        Vec3 acceleration = lookDirection.scale(0.5D + 0.5D * context.powerEfficiency());
        SmallFireball fireball = new SmallFireball(
                player.level(),
                player,
                acceleration.x,
                acceleration.y,
                acceleration.z
        );
        fireball.setPos(
                player.getX() + lookDirection.x,
                player.getEyeY() - 0.1D + lookDirection.y,
                player.getZ() + lookDirection.z
        );
        player.level().addFreshEntity(fireball);
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS,
                0.8F,
                1.1F
        );
        return true;
    }
}
