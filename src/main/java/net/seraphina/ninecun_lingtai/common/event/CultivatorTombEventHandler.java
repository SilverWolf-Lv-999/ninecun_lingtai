package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.tomb.CultivatorTombManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 修士墓穴三态机的事件接线（M4 墓穴切片）。
 * 逻辑全部在 {@link CultivatorTombManager}，本类只做事件转发。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivatorTombEventHandler {
    private static final int TICK_INTERVAL = 10;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && event.getServer().getTickCount() % TICK_INTERVAL == 0) {
            CultivatorTombManager.tick(event.getServer());
        }
    }

    /** "动棺椁"唤醒：破坏沉睡墓主室的任何方块即视为扰墓。 */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && event.getPlayer() instanceof ServerPlayer player
                && !player.isCreative()) {
            CultivatorTombManager.onChamberBlockBroken(level, event.getPos(), player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob
                && mob.level() instanceof ServerLevel level
                && CultivatorTombManager.isTombMaster(mob)) {
            CultivatorTombManager.onMasterDeath(level, mob);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        CultivatorTombManager.clearAll();
    }

    private CultivatorTombEventHandler() {
    }
}
