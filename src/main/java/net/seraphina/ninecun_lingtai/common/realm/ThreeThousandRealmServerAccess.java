package net.seraphina.ninecun_lingtai.common.realm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public interface ThreeThousandRealmServerAccess {
    ServerLevel ninecunLingtai$loadRegisteredRealm(ResourceKey<Level> levelKey, long realmSeed);
}
