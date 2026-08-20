package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;

public final class DimensionNameUtility {
    public static String getDisplayName(MinecraftServer server, ResourceLocation dimensionId) {
        if (dimensionId == null) {
            return "unknown";
        }
        if (SpiritualRealmUtility.SPIRITUAL_REALM_ID.equals(dimensionId)) {
            return "灵界";
        }
        if (server != null) {
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            String realmName = ThreeThousandRealmManager.get(server)
                    .getRealmDefinition(dimensionKey)
                    .map(realm -> realm.getDisplayName())
                    .orElse(null);
            if (realmName != null) {
                return realmName;
            }
        }
        return dimensionId.toString();
    }

    public static String getDisplayName(MinecraftServer server, String dimensionName) {
        if (dimensionName == null || dimensionName.isBlank()) {
            return "unknown";
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(dimensionName);
        return dimensionId == null
                ? dimensionName
                : getDisplayName(server, dimensionId);
    }
}
