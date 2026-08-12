package net.seraphina.ninecun_lingtai.client.player;

import com.mojang.authlib.GameProfile;
import net.seraphina.ninecun_lingtai.common.player.PossessionSyncData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientPossessionManager {
    private static final Map<UUID, GameProfile> POSSESSED_PROFILES = new HashMap<>();

    private ClientPossessionManager() {
    }

    public static void accept(PossessionSyncData syncData) {
        if (!syncData.active() || syncData.possessedProfile() == null) {
            POSSESSED_PROFILES.remove(syncData.possessorUuid());
            return;
        }
        POSSESSED_PROFILES.put(syncData.possessorUuid(), syncData.possessedProfile());
    }

    public static GameProfile getPossessedProfile(UUID possessorUuid) {
        return POSSESSED_PROFILES.get(possessorUuid);
    }

    public static void reset() {
        POSSESSED_PROFILES.clear();
    }
}
