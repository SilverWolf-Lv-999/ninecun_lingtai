package net.seraphina.ninecun_lingtai.client.utility;

import net.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import net.seraphina.ninecun_lingtai.client.player.ClientDefenseResourceManager;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ClientCultivationDefenseUtility {

    public static float getProtectedHealth(LivingEntity entity, float original) {
        return hasActiveLocalDefense() ? 20.0F : original;
    }

    public static boolean getProtectedAlive(Entity entity, boolean original) {
        return hasActiveLocalDefense() || original;
    }

    public static boolean getProtectedDeadOrDying(LivingEntity entity, boolean original) {
        return hasActiveLocalDefense() ? false : original;
    }

    private static boolean hasActiveLocalDefense() {
        return ClientDefenseResourceManager.getSnapshot()
                .map(snapshot -> CultivationDefenseUtility.isActiveDefense(
                        ClientCultivationInfoManager.getCultivationStage(),
                        snapshot.type(),
                        snapshot.current()
                ))
                .orElse(false);
    }
}
