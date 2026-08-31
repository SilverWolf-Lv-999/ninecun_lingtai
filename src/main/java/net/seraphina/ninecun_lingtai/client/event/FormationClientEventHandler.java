package net.seraphina.ninecun_lingtai.client.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.player.ClientFormationOverlayData;
import net.seraphina.ninecun_lingtai.client.utility.FormationRenderUtility;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 阵法客户端事件（独立订阅类，不动共享的 ClientForgeEventHandler——M4 并发约束）。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FormationClientEventHandler {

    private FormationClientEventHandler() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        FormationRenderUtility.render(event);
    }

    @SubscribeEvent
    public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientFormationOverlayData.clear();
    }
}
