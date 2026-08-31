package net.seraphina.ninecun_lingtai.common.time;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 修真历的唯一推进者：每个服务端 tick 的 END 相位把
 * {@link MonotonicCalendarSavedData} +1。
 *
 * <p>置脏节奏：每 {@value #MARK_DIRTY_INTERVAL_TICKS} tick 置脏一次，避免每
 * tick 都触发 SavedData 序列化；世界周期存盘与关服存盘会把脏数据落盘。
 * 崩服最多丢失一个置脏周期（10 秒）的历法进度，单调性不受影响。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CalendarEventHandler {
    private static final int MARK_DIRTY_INTERVAL_TICKS = 200;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MonotonicCalendarSavedData calendar = MonotonicCalendarSavedData.get(event.getServer());
        if (calendar.advance() % MARK_DIRTY_INTERVAL_TICKS == 0L) {
            calendar.setDirty();
        }
    }
}
