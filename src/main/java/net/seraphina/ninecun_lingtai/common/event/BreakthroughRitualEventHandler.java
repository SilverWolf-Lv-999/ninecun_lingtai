package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.breakthrough.BreakthroughRitualManager;
import net.seraphina.ninecun_lingtai.common.insight.PlayerInsightData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 引灵仪式的事件驱动层：tick 推进、受击中断（R3：仅实体来源实伤打断）、
 * 登出/换维度/死亡结算——登出不再是免罚出口（§5.5）。
 * 服务器重启的恢复结算不在此处：由 SavedData 的 restoredFromDisk 标记在首个 tick 处理。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BreakthroughRitualEventHandler {

    /** 悟道印记候选过期的低频检查间隔（tick）。 */
    private static final int INSIGHT_EXPIRY_CHECK_INTERVAL_TICKS = 100;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            BreakthroughRitualManager.tick(player);
            // 印记候选过期提醒：过期即清除并提示 insight_expired。
            if (player.tickCount % INSIGHT_EXPIRY_CHECK_INTERVAL_TICKS == 0) {
                PlayerInsightData.tickExpiry(player, player.serverLevel().getGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getAmount() > 0.0F
                && BreakthroughRitualManager.isInRitual(player)) {
            BreakthroughRitualManager.onHurt(player, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BreakthroughRitualManager.interrupt(player, "death");
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BreakthroughRitualManager.interrupt(player, "logout");
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BreakthroughRitualManager.interrupt(player, "changed_dimension");
        }
    }
}
