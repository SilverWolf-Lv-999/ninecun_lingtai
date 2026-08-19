package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class EntityUtility {
    public static void protectEntity(Object object) {
        if (!(object instanceof LivingEntity living)) return;
        living.canUpdate(true);
        living.setHealth(living.getMaxHealth());
        living.deathTime = -2;
        living.hurtTime = -2;
        living.fallDistance = 0;
        living.removalReason = null;
        living.canUpdate = true;
        living.isAddedToWorld = true;
        living.setAirSupply(300);
        living.clearFire();
        if (living instanceof Player player) {
            player.removeStingerTime = 0;
            player.removeArrowTime = 0;
            player.noJumpDelay = 0;
        }
    }
}
