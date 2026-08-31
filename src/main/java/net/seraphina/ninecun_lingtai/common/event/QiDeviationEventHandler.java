package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.qi_deviation.QiDeviationManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 走火入魔的事件接线（M4 切片）。
 *
 * <p>打坐相关触发点全部在本处理器内以只读方式观测（{@code PlayerMeditationManager.isMeditating}
 * / {@code PlayerPillToxinData.getToxin}），不改动 {@code PlayerMeditationManager} 本体
 * （并发约束：该文件属共享禁改清单）。</p>
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QiDeviationEventHandler {
    private static final int TICK_INTERVAL = 20;
    private static final int MINUTE_TICKS = 1200;

    /** 触发点 ①：打坐中被重创。 */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            QiDeviationManager.onMeditatingPlayerHurt(player, event.getAmount());
        }
    }

    /** 逐秒发作效果 + 每分钟打坐风险掷点（丹毒 / 心障与劫后虚弱窗口）。 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % TICK_INTERVAL != 0) {
            return;
        }
        if (player.tickCount % MINUTE_TICKS == 0) {
            QiDeviationManager.rollMeditationRisks(player);
        }
        QiDeviationManager.tickPlayer(player, player.level().getGameTime());
    }

    /** 化解点 ②：他人潜行右键护法。 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer guardian)
                || !guardian.isShiftKeyDown()
                || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }
        if (QiDeviationManager.tryStartGuard(guardian, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QiDeviationManager.clearAll();
    }

    private QiDeviationEventHandler() {
    }
}
