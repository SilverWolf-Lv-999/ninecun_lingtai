package com.seraphina.ninecun_lingtai.common.player;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SoulRoamingSyncData(
        boolean active,
        ResourceKey<Level> bodyDimension,
        Vec3 bodyPosition,
        int soulEntityId,
        boolean bodyExists
) {
    public static SoulRoamingSyncData inactive() {
        return new SoulRoamingSyncData(
                false,
                Level.OVERWORLD,
                Vec3.ZERO,
                -1,
                false
        );
    }
}
