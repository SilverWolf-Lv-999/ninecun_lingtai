package net.seraphina.ninecun_lingtai.client.utility;

import net.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import net.seraphina.ninecun_lingtai.client.player.ClientDefenseResourceManager;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;

public final class ClientCultivationDefenseUtility {

    public static boolean hasActiveDefense(LivingEntity entity) {
        return isLocalPlayer(entity) && hasActiveLocalDefense();
    }

    public static float getProtectedHealth(LivingEntity entity, float original) {
        return hasActiveDefense(entity) ? entity.getMaxHealth() : original;
    }

    public static boolean getProtectedAlive(Entity entity, boolean original) {
        return entity instanceof LivingEntity livingEntity
                && hasActiveDefense(livingEntity)
                || original;
    }

    public static boolean getProtectedDeadOrDying(LivingEntity entity, boolean original) {
        return hasActiveDefense(entity) ? false : original;
    }

    private static boolean isLocalPlayer(Entity entity) {
        return entity != null && entity == Minecraft.getInstance().player;
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
