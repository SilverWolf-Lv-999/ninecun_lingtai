package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.common.entity.EntityCultivationData;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerAptitudeData {

    public static Optional<SpiritualRoot> getSpiritualRoot(Player player) {
        return EntityCultivationData.getSpiritualRoot(player);
    }

    public static void setSpiritualRoot(Player player, SpiritualRoot spiritualRoot) {
        EntityCultivationData.setSpiritualRoot(player, spiritualRoot);
    }

    public static Optional<SoulStrength> getSoulStrength(Player player) {
        return EntityCultivationData.getSoulStrength(player);
    }

    public static void setSoulStrength(Player player, SoulStrength soulStrength) {
        EntityCultivationData.setSoulStrength(player, soulStrength);
    }

    public static void setAptitude(Player player, SpiritualRoot spiritualRoot, SoulStrength soulStrength) {
        setSpiritualRoot(player, spiritualRoot);
        setSoulStrength(player, soulStrength);
    }

    public static boolean hasCompleteAptitude(Player player) {
        return getSpiritualRoot(player).isPresent() && getSoulStrength(player).isPresent();
    }

    public static void clearSpiritualRoot(Player player) {
        EntityCultivationData.clearSpiritualRoot(player);
    }

    public static void clearSoulStrength(Player player) {
        EntityCultivationData.clearSoulStrength(player);
    }

    public static void clearAptitude(Player player) {
        clearSpiritualRoot(player);
        clearSoulStrength(player);
    }

    public static void copySpiritualRoot(Player sourcePlayer, Player targetPlayer) {
        getSpiritualRoot(sourcePlayer).ifPresentOrElse(
                root -> setSpiritualRoot(targetPlayer, root),
                () -> clearSpiritualRoot(targetPlayer)
        );
    }

    public static void copyAptitude(Player sourcePlayer, Player targetPlayer) {
        copySpiritualRoot(sourcePlayer, targetPlayer);
        getSoulStrength(sourcePlayer).ifPresentOrElse(
                strength -> setSoulStrength(targetPlayer, strength),
                () -> clearSoulStrength(targetPlayer)
        );
    }
}
