package com.seraphina.ninecun_lingtai.integration.jade;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import com.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public final class LifespanJadeProvider implements
        IEntityComponentProvider,
        IServerDataProvider<EntityAccessor> {
    public static final LifespanJadeProvider INSTANCE = new LifespanJadeProvider();

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "lifespan"
    );
    private static final String AVAILABLE_KEY = "ninecunLingtaiLifespanAvailable";
    private static final String AGE_TICKS_KEY = "ninecunLingtaiAgeTicks";
    private static final String MAXIMUM_YEARS_KEY = "ninecunLingtaiMaximumLifespanYears";
    private static final String IMMORTAL_KEY = "ninecunLingtaiImmortal";

    private LifespanJadeProvider() {
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!(accessor.getEntity() instanceof LivingEntity entity)
                || !LifespanUtility.hasLifespan(entity)) {
            return;
        }
        LifespanSnapshot lifespan = EntityLifespanData.capture(entity);
        data.putBoolean(AVAILABLE_KEY, true);
        data.putLong(AGE_TICKS_KEY, lifespan.ageTicks());
        data.putInt(MAXIMUM_YEARS_KEY, lifespan.maximumLifespanYears());
        data.putBoolean(IMMORTAL_KEY, lifespan.immortal());
    }

    @Override
    public void appendTooltip(
            ITooltip tooltip,
            EntityAccessor accessor,
            IPluginConfig config
    ) {
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean(AVAILABLE_KEY)) {
            return;
        }
        LifespanSnapshot lifespan = new LifespanSnapshot(
                data.getLong(AGE_TICKS_KEY),
                data.getInt(MAXIMUM_YEARS_KEY),
                data.getBoolean(IMMORTAL_KEY)
        );
        Component age = formatDuration(
                lifespan.ageTicks(),
                "jade.ninecun_lingtai.age_duration"
        );
        Component remaining = lifespan.immortal()
                ? Component.translatable("jade.ninecun_lingtai.immortal")
                : formatDuration(
                        lifespan.remainingTicks(),
                        "jade.ninecun_lingtai.remaining_duration"
                );
        tooltip.add(
                Component.translatable(
                                "jade.ninecun_lingtai.lifespan",
                                age,
                                remaining
                        )
                        .withStyle(ChatFormatting.GRAY),
                UID
        );
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static Component formatDuration(long ticks, String translationKey) {
        long totalDays = Math.max(0L, ticks) / LifespanUtility.TICKS_PER_DAY;
        long years = totalDays / LifespanUtility.DAYS_PER_YEAR;
        long days = totalDays % LifespanUtility.DAYS_PER_YEAR;
        return Component.translatable(translationKey, years, days);
    }
}
