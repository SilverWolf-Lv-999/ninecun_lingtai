package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * MOD 总线钩子（注解自动发现）：双端在 CommonSetup 各自完成阵法专用
 * 网络通道注册——不依赖 ModMain 接线也能收发（M4 并发约束）。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FormationModEventHandler {

    private FormationModEventHandler() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(FormationNetwork::init);
    }
}
