package net.seraphina.ninecun_lingtai.common.spell;

import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

public final class FireballSpell extends Spell {
    @Override
    public boolean cast(ServerPlayer player, SpellCastContext context) {
        Vec3 lookDirection = player.getLookAngle();
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
