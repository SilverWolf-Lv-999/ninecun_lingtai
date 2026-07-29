package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.entity.EntityCultivationData;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerCultivationData {
    private PlayerCultivationData() {
    }

    public static Optional<CultivationStage> getStage(Player player) {
        return EntityCultivationData.getStage(player);
    }

    public static void setStage(Player player, CultivationStage stage) {
        EntityCultivationData.setStage(player, stage);
    }

    public static void clearStage(Player player) {
        EntityCultivationData.clearStage(player);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        getStage(sourcePlayer).ifPresentOrElse(
                stage -> setStage(targetPlayer, stage),
                () -> clearStage(targetPlayer)
        );
    }
}
