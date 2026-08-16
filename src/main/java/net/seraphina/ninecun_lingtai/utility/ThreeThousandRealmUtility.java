package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;

import java.util.Objects;
import java.util.UUID;

public final class ThreeThousandRealmUtility {
    private static final String REALM_ID_UUID_SEPARATOR = "_jie_";

    private ThreeThousandRealmUtility() {
    }

    public static boolean isPortalCompatibleRealm(Level level) {
        Objects.requireNonNull(level, "level");
        if (level.isClientSide()) {
            return isGeneratedThreeThousandRealmId(level.dimension().location());
        }
        if (level instanceof ServerLevel serverLevel) {
            return ThreeThousandRealmManager.get(serverLevel.getServer())
                    .isThreeThousandRealm(serverLevel.dimension());
        }
        return false;
    }

    public static boolean isGeneratedThreeThousandRealmId(ResourceLocation realmId) {
        Objects.requireNonNull(realmId, "realmId");
        if (!ModMain.MOD_ID.equals(realmId.getNamespace())) {
            return false;
        }

        String path = realmId.getPath();
        int separatorIndex = path.lastIndexOf(REALM_ID_UUID_SEPARATOR);
        if (separatorIndex < 1) {
            return false;
        }

        String uuidString = path.substring(separatorIndex + REALM_ID_UUID_SEPARATOR.length());
        try {
            return UUID.fromString(uuidString).toString().equals(uuidString);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
