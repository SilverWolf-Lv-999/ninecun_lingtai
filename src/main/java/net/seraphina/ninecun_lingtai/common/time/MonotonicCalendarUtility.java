package net.seraphina.ninecun_lingtai.common.time;

import net.minecraft.server.MinecraftServer;

/**
 * 修真历公共门面：全服唯一、只增不减的时间读 API。
 *
 * <p>用途：寿元、药龄、死契、耐药窗口等一切"跨重启累计流逝时间"的场景。
 * 新代码一律禁用裸 {@code getDayTime()}（可被 {@code /time set} 回拨）；
 * {@code getGameTime()} 仅限"运行期短时窗口"（如缓存过期）继续使用。
 *
 * <p>本类只读，不承载迁移；现有系统（EntityLifespanData、PlayerPillToxinData 等）
 * 的迁移方案见 docs/开发日志/基建加固_修真历与会话持久化.md。
 */
public final class MonotonicCalendarUtility {
    /** 一修真日 = 24000 tick，与 vanilla 昼夜同刻度，方便策划换算。 */
    public static final long TICKS_PER_DAY = 24000L;

    private MonotonicCalendarUtility() {
    }

    /** 当前修真历刻（自世界创建以来累计的服务端 tick，只增不减）。 */
    public static long getCalendarTick(MinecraftServer server) {
        return MonotonicCalendarSavedData.get(server).getElapsedTicks();
    }

    /** 当前修真历日（{@link #getCalendarTick} / {@value #TICKS_PER_DAY}）。 */
    public static long getCalendarDay(MinecraftServer server) {
        return getCalendarTick(server) / TICKS_PER_DAY;
    }

    /**
     * 两个修真历刻之间流逝的 tick 数；{@code to} 早于 {@code from} 时返回 0
     * （正常运行不会发生，防御旧存档脏数据）。
     */
    public static long elapsedTicksBetween(long fromCalendarTick, long toCalendarTick) {
        return Math.max(0L, toCalendarTick - fromCalendarTick);
    }

    /** 两个修真历刻之间流逝的整日数（向下取整）。 */
    public static long elapsedDaysBetween(long fromCalendarTick, long toCalendarTick) {
        return elapsedTicksBetween(fromCalendarTick, toCalendarTick) / TICKS_PER_DAY;
    }
}
