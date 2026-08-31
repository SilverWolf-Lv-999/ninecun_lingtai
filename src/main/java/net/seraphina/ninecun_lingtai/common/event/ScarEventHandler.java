package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.status.Scar;
import net.seraphina.ninecun_lingtai.common.status.ScarManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 伤痕（Scar）到期清理：每秒检查一次玩家身上的限时伤痕，到期移除并提示本人。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScarEventHandler {
    private static final int CHECK_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        List<Scar> expired = ScarManager.tickExpire(player);
        for (Scar scar : expired) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.ninecun_lingtai.scar_expired",
                            scar.type().getDisplayName()
                    ),
                    true
            );
        }
    }

    private ScarEventHandler() {
    }
}
