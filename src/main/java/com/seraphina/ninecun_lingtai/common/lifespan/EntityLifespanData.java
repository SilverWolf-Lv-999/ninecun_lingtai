package com.seraphina.ninecun_lingtai.common.lifespan;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class EntityLifespanData {
    private static final String LIFESPAN_KEY = ModMain.MOD_ID + ":lifespan";
    private static final String AGE_TICKS_KEY = "ageTicks";
    private static final String LAST_CALENDAR_TICK_KEY = "lastCalendarTick";
    private static final String EXPIRATION_HANDLED_KEY = "expirationHandled";

    private EntityLifespanData() {
    }

    public static LifespanSnapshot capture(LivingEntity entity) {
        if (!LifespanUtility.hasLifespan(entity)) {
            return LifespanSnapshot.empty();
        }
        if (!entity.level().isClientSide) {
            updateAge(entity);
        }
        return new LifespanSnapshot(
                getAgeTicks(entity),
                LifespanUtility.getMaximumLifespanYears(entity),
                LifespanUtility.isImmortal(entity)
        );
    }

    public static void copy(LivingEntity sourceEntity, LivingEntity targetEntity) {
        CompoundTag sourceData = sourceEntity.getPersistentData();
        if (sourceData.contains(LIFESPAN_KEY, Tag.TAG_COMPOUND)) {
            targetEntity.getPersistentData().put(
                    LIFESPAN_KEY,
                    sourceData.getCompound(LIFESPAN_KEY).copy()
            );
        }
    }

    public static boolean isExpirationHandled(LivingEntity entity) {
        return getLifespanData(entity).getBoolean(EXPIRATION_HANDLED_KEY);
    }

    public static void markExpirationHandled(LivingEntity entity) {
        CompoundTag lifespanData = getLifespanData(entity);
        lifespanData.putBoolean(EXPIRATION_HANDLED_KEY, true);
        saveLifespanData(entity, lifespanData);
    }

    private static void updateAge(LivingEntity entity) {
        long calendarTick = getCalendarTick(entity);
        CompoundTag lifespanData = getLifespanData(entity);
        if (lifespanData.contains(LAST_CALENDAR_TICK_KEY, Tag.TAG_LONG)) {
            long lastCalendarTick = lifespanData.getLong(LAST_CALENDAR_TICK_KEY);
            if (calendarTick > lastCalendarTick) {
                long elapsedTicks = calendarTick - lastCalendarTick;
                lifespanData.putLong(
                        AGE_TICKS_KEY,
                        saturatingAdd(Math.max(0L, lifespanData.getLong(AGE_TICKS_KEY)), elapsedTicks)
                );
            }
        }
        lifespanData.putLong(LAST_CALENDAR_TICK_KEY, calendarTick);
        saveLifespanData(entity, lifespanData);
    }

    private static long getAgeTicks(LivingEntity entity) {
        return Math.max(0L, getLifespanData(entity).getLong(AGE_TICKS_KEY));
    }

    private static long getCalendarTick(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().overworld().getDayTime();
        }
        return entity.level().getDayTime();
    }

    private static CompoundTag getLifespanData(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (persistentData.contains(LIFESPAN_KEY, Tag.TAG_COMPOUND)) {
            return persistentData.getCompound(LIFESPAN_KEY);
        }
        return new CompoundTag();
    }

    private static void saveLifespanData(LivingEntity entity, CompoundTag lifespanData) {
        entity.getPersistentData().put(LIFESPAN_KEY, lifespanData);
    }

    private static long saturatingAdd(long left, long right) {
        if (right > Long.MAX_VALUE - left) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
