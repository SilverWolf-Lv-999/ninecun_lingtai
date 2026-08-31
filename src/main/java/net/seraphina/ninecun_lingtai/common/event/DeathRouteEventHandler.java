package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.death.DeathRouteManager;
import net.seraphina.ninecun_lingtai.common.seal.SealManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * 死亡路线事件入口——<b>唯一</b>以 HIGHEST 优先级接管玩家死亡的订阅者。
 * cancel 后其余 LivingDeathEvent 订阅者收不到本次事件：清理类逻辑由
 * DeathRouteManager.mirrorDeathMomentCleanup 镜像；记录类（大因果/行为账本）
 * 在真实死亡重触发或路线结算时补记。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathRouteEventHandler {

    private DeathRouteEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()) {
            return;
        }
        if (DeathRouteManager.intercept(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    /** 死亡瞬间掉落物上的禁制自动进死契（真实死亡路径，含 bypass 重触发）。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<net.minecraft.world.item.ItemStack> droppedStacks = new ArrayList<>();
        for (ItemEntity itemEntity : event.getDrops()) {
            droppedStacks.add(itemEntity.getItem());
        }
        SealManager.applyDeathPact(player, droppedStacks);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 10 == 0) {
            DeathRouteManager.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DeathRouteManager.restoreOnLogin(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DeathRouteManager.clear();
    }
}
