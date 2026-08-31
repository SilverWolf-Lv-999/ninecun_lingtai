package net.seraphina.ninecun_lingtai.common.time;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 修真历：全服唯一、只增不减的单调时间账本。
 *
 * <p>与 {@code overworld().getDayTime()}（可被 {@code /time set} 回拨）和
 * {@code getGameTime()}（各维度口径不一）不同，本账本由
 * {@link CalendarEventHandler} 在每个服务端 tick 的 END 相位无条件 +1，
 * 存于 overworld 的 DataStorage，因此跨重启只增不减、不受任何时间指令干扰。
 *
 * <p>外部系统请勿直接持有本类，一律通过 {@link MonotonicCalendarUtility} 读取。
 */
public final class MonotonicCalendarSavedData extends SavedData {
    private static final String DATA_NAME = "ninecun_lingtai_monotonic_calendar";
    private static final String ELAPSED_TICKS_TAG = "ElapsedTicks";

    private long elapsedTicks;

    public static MonotonicCalendarSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                MonotonicCalendarSavedData::load,
                MonotonicCalendarSavedData::new,
                DATA_NAME
        );
    }

    public static MonotonicCalendarSavedData load(CompoundTag tag) {
        MonotonicCalendarSavedData savedData = new MonotonicCalendarSavedData();
        savedData.elapsedTicks = Math.max(0L, tag.getLong(ELAPSED_TICKS_TAG));
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong(ELAPSED_TICKS_TAG, this.elapsedTicks);
        return tag;
    }

    public long getElapsedTicks() {
        return this.elapsedTicks;
    }

    /**
     * 推进一个 tick 并返回新值。只允许 {@link CalendarEventHandler} 调用；
     * 落盘节奏（周期 setDirty）由调用方控制，这里不置脏。
     */
    long advance() {
        return ++this.elapsedTicks;
    }
}
