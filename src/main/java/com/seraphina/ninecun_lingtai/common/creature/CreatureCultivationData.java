package com.seraphina.ninecun_lingtai.common.creature;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.entity.EntityCultivationData;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class CreatureCultivationData {
    private static final String SPAWN_ROLL_DONE_KEY = "ninecun_lingtai:cultivation_rolled";

    private CreatureCultivationData() {
    }

    public static Optional<CultivationStage> getStage(LivingEntity entity) {
        return EntityCultivationData.getStage(entity);
    }

    public static void setStage(LivingEntity entity, CultivationStage stage) {
        EntityCultivationData.setStage(entity, stage);
    }

    public static void clearStage(LivingEntity entity) {
        EntityCultivationData.clearStage(entity);
    }

    public static Optional<SpiritualRoot> getSpiritualRoot(LivingEntity entity) {
        return EntityCultivationData.getSpiritualRoot(entity);
    }

    public static void setSpiritualRoot(LivingEntity entity, SpiritualRoot spiritualRoot) {
        EntityCultivationData.setSpiritualRoot(entity, spiritualRoot);
    }

    public static void clearSpiritualRoot(LivingEntity entity) {
        EntityCultivationData.clearSpiritualRoot(entity);
    }

    public static boolean isSpawnRollDone(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(SPAWN_ROLL_DONE_KEY);
    }

    public static void markSpawnRollDone(LivingEntity entity) {
        entity.getPersistentData().putBoolean(SPAWN_ROLL_DONE_KEY, true);
    }
}
