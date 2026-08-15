package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.ModMain;

public final class PlayerThreeThousandRealmData {
    private static final String INITIAL_REALM_PLACEMENT_KEY =
            ModMain.MOD_ID + ":initial_three_thousand_realm_placement";

    private PlayerThreeThousandRealmData() {
    }

    public static boolean hasReceivedInitialRealmPlacement(Player player) {
        return player.getPersistentData().getBoolean(INITIAL_REALM_PLACEMENT_KEY);
    }

    public static void markInitialRealmPlacementReceived(Player player) {
        player.getPersistentData().putBoolean(INITIAL_REALM_PLACEMENT_KEY, true);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        if (hasReceivedInitialRealmPlacement(sourcePlayer)) {
            markInitialRealmPlacementReceived(targetPlayer);
        }
    }
}
