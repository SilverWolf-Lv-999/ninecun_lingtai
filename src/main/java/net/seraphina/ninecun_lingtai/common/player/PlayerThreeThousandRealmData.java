package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.ModMain;

import java.util.Optional;

public final class PlayerThreeThousandRealmData {
    private static final String INITIAL_REALM_PLACEMENT_KEY =
            ModMain.MOD_ID + ":initial_three_thousand_realm_placement";
    private static final String DEATH_REALM_ID_KEY =
            ModMain.MOD_ID + ":three_thousand_realm_death_realm_id";

    private PlayerThreeThousandRealmData() {
    }

    public static boolean hasReceivedInitialRealmPlacement(Player player) {
        return player.getPersistentData().getBoolean(INITIAL_REALM_PLACEMENT_KEY);
    }

    public static void markInitialRealmPlacementReceived(Player player) {
        player.getPersistentData().putBoolean(INITIAL_REALM_PLACEMENT_KEY, true);
    }

    public static void setDeathRealmId(Player player, String realmId) {
        player.getPersistentData().putString(DEATH_REALM_ID_KEY, realmId);
    }

    public static Optional<String> getDeathRealmId(Player player) {
        String realmId = player.getPersistentData().getString(DEATH_REALM_ID_KEY);
        return realmId.isEmpty() ? Optional.empty() : Optional.of(realmId);
    }

    public static void clearDeathRealmId(Player player) {
        player.getPersistentData().remove(DEATH_REALM_ID_KEY);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        if (hasReceivedInitialRealmPlacement(sourcePlayer)) {
            markInitialRealmPlacementReceived(targetPlayer);
        }
        clearDeathRealmId(targetPlayer);
    }
}
