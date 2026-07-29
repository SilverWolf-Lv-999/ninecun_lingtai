package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

public final class PushingMountainsUtility {
    public static final CultivationStage MINIMUM_STAGE = new CultivationStage(CultivationRealm.GOLDEN_CORE, 1);

    private PushingMountainsUtility() {
    }

    public static boolean canUse(Optional<CultivationStage> stage) {
        return stage.map(PushingMountainsUtility::canUse).orElse(false);
    }

    public static boolean canUse(CultivationStage stage) {
        return getProgressionIndex(stage) >= getProgressionIndex(MINIMUM_STAGE);
    }

    public static double getMaximumSelectionDistance(CultivationStage stage) {
        int realmGrowth = Math.max(0, stage.realm().ordinal() - CultivationRealm.GOLDEN_CORE.ordinal());
        double exponentialDistance = CommonConfig.PUSHING_MOUNTAINS_BASE_SELECTION_DISTANCE.get()
                * Math.pow(2.0D, realmGrowth)
                * (1.0D + (stage.layer() - 1) * 0.12D);
        return Mth.clamp(
                exponentialDistance,
                1.0D,
                CommonConfig.PUSHING_MOUNTAINS_MAX_SELECTION_DISTANCE.get()
        );
    }

    public static long getProgressionIndex(CultivationStage stage) {
        return (long) stage.realm().ordinal() * CultivationStage.MAXIMUM_LAYER + stage.layer();
    }

    public static AABB createSelectionBox(BlockPos first, BlockPos second) {
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX()) + 1;
        int maxY = Math.max(first.getY(), second.getY()) + 1;
        int maxZ = Math.max(first.getZ(), second.getZ()) + 1;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static long getSelectionVolume(BlockPos first, BlockPos second) {
        long width = Math.abs((long) first.getX() - second.getX()) + 1L;
        long height = Math.abs((long) first.getY() - second.getY()) + 1L;
        long depth = Math.abs((long) first.getZ() - second.getZ()) + 1L;
        return width * height * depth;
    }
}
