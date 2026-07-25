package com.seraphina.ninecun_lingtai.api.aptitude;

import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SpiritualRootApi {
    private SpiritualRootApi() {
    }

    public static Optional<SpiritualRoot> getSpiritualRoot(Player player) {
        return PlayerAptitudeData.getSpiritualRoot(player);
    }

    public static boolean hasSpiritualRoot(Player player) {
        return getSpiritualRoot(player).isPresent();
    }
}
