package net.seraphina.ninecun_lingtai.common.player;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public record PossessionSyncData(
        boolean active,
        UUID possessorUuid,
        GameProfile possessedProfile
) {
    public static PossessionSyncData inactive(UUID possessorUuid) {
        return new PossessionSyncData(false, possessorUuid, null);
    }
}
