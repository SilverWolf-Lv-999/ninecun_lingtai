package net.seraphina.ninecun_lingtai.client.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.player.ClientDaoTitleManager;
import net.seraphina.ninecun_lingtai.common.player.DaoTitleSnapshot;
import net.seraphina.ninecun_lingtai.utility.DaoTitleUtility;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DaoTitleClientEventHandler {
    private DaoTitleClientEventHandler() {
    }

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        DaoTitleSnapshot snapshot = ClientDaoTitleManager.get(entity.getUUID());
        if (snapshot == null || !snapshot.isUnlocked() || DaoTitleUtility.hasTitle(event.getContent())) {
            return;
        }
        String title = DaoTitleUtility.getDisplayTitle(snapshot);
        if (!title.isBlank()) {
            event.setContent(DaoTitleUtility.formatName(event.getContent(), snapshot));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDaoTitleManager.reset();
    }
}
