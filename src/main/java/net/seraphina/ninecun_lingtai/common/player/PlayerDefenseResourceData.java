package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.common.defense.EntityDefenseResourceData;
import net.minecraft.world.entity.player.Player;

/** Player-facing compatibility facade for the shared living-entity defense resource. */
public final class PlayerDefenseResourceData {

    public static DefenseResourceSnapshot capture(Player player) {
        return EntityDefenseResourceData.capture(player);
    }

    public static void clampToStage(Player player) {
        EntityDefenseResourceData.clampToStage(player);
    }

    public static int getCurrent(Player player) {
        return EntityDefenseResourceData.getCurrent(player);
    }

    public static int repair(Player player, int amount) {
        return EntityDefenseResourceData.repair(player, amount);
    }

    public static void refillToMaximum(Player player) {
        EntityDefenseResourceData.refillToMaximum(player);
    }

    public static boolean consume(Player player, int amount) {
        return EntityDefenseResourceData.consume(player, amount);
    }

    public static void deplete(Player player) {
        EntityDefenseResourceData.deplete(player);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        EntityDefenseResourceData.copy(sourcePlayer, targetPlayer);
    }

    public static void clear(Player player) {
        EntityDefenseResourceData.clear(player);
    }
}
