package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThreeThousandRealmEventHandler {
    private ThreeThousandRealmEventHandler() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ThreeThousandRealmManager.initialize(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ThreeThousandRealmManager.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ThreeThousandRealmManager.clear(event.getServer());
    }
}
