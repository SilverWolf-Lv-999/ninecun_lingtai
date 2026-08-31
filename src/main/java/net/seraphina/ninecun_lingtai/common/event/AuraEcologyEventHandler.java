package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.aura.AuraDebugCommand;
import net.seraphina.ninecun_lingtai.common.aura.SpiritualAuraManager;
import net.seraphina.ninecun_lingtai.common.aura.SpiritualQiQualityProvider;
import net.seraphina.ninecun_lingtai.common.breakthrough.BreakthroughChanceCalculator;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 灵气生态（M2 切片）的接线事件处理器：
 * <ul>
 *   <li>服务器启动前经公开 setter 注册破境灵气质量 Provider（不改 breakthrough 包）；</li>
 *   <li>注册 /lingtai_aura 调试命令（独立根，不动现有命令树）；</li>
 *   <li>服务器停机清空质量派生缓存，防跨存档串档。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AuraEcologyEventHandler {

    private AuraEcologyEventHandler() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        BreakthroughChanceCalculator.setQiQualityProvider(new SpiritualQiQualityProvider());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AuraDebugCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SpiritualAuraManager.clearQualityCache();
    }
}
