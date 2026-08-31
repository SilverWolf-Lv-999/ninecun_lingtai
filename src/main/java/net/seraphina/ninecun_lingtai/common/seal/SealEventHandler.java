package net.seraphina.ninecun_lingtai.common.seal;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.seal.lockgrid.LockGridSessionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 阵禁事件挂钩：
 * 潜行右键持物 = 禁制入口（自己的物品开刻印界面 / 他人的封印物开锁纹 / 残骸读指纹）；
 * 非潜行使用被认主禁拦截；受击打断仪式与锁纹会话（施工图风险 3：界面不暂停，
 * 玩家被打必须服务端强制弹出，否则会话泄漏）。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SealEventHandler {

    private SealEventHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (SealManager.handleSneakUse(player, event.getHand())) {
                event.setCanceled(true);
            }
            return;
        }
        if (SealManager.blocksUse(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !event.getItemStack().isEmpty()
                && SealManager.blocksUse(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0.0F) {
            SealManager.onPlayerHurt(player);
            LockGridSessionManager.onPlayerHurt(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        SealManager.tickRituals(event.getServer());
        LockGridSessionManager.tick(event.getServer());
        SealManager.tickSlowScan(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SealManager.onPlayerLoggedOut(player);
            LockGridSessionManager.onPlayerLoggedOut(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SealManager.clear();
        LockGridSessionManager.clear();
    }
}
