package net.seraphina.ninecun_lingtai.common.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.daotitle.DaoTitleManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerDaoTitleData;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DaoTitleEventHandler {
    private DaoTitleEventHandler() {
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setDisplayname(DaoTitleManager.formatName(player, event.getUsername()));
        }
    }

    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            event.setDisplayName(DaoTitleManager.formatName(player, player.getName()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (ServerPlayer subject : player.server.getPlayerList().getPlayers()) {
            DaoTitleManager.syncTo(player, subject);
            if (subject != player) {
                DaoTitleManager.syncTo(subject, player);
            }
        }
        DaoTitleManager.syncToAll(player);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer recipient
                && event.getTarget() instanceof ServerPlayer subject) {
            DaoTitleManager.syncTo(recipient, subject);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerDaoTitleData.copy(event.getOriginal(), event.getEntity());
    }
}
