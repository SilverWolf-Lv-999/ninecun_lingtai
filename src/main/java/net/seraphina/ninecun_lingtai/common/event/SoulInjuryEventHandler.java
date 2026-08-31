package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulInjuryData;
import net.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 魂伤周期性结算与档位体感：
 * 每修真日自然恢复（7 星翻倍）、PERIL 档每日折寿、PERIL 常驻耳鸣/暗影脉冲、SHAKEN 档夜间惊醒。
 * 日界检测用 lastNaturalHealTick 双向 clamp（设计文档 R2）：时间倒拨不结算，
 * 正向跳日（/time set）单次也只结一天并重定基准，防一次性折掉多年寿元。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulInjuryEventHandler {
    private static final int CHECK_INTERVAL_TICKS = 100;
    /** PERIL 常驻体感的暗影脉冲时长（tick）。 */
    private static final int PERIL_DARKNESS_PULSE_TICKS = 40;
    /** 夜间惊醒提示的最小间隔（tick），SleepingTimeCheckEvent 每 tick 触发需节流。 */
    private static final int NIGHT_TERROR_MESSAGE_COOLDOWN_TICKS = 100;

    /** 会话级：玩家 UUID -> 上次夜间惊醒提示的 gameTime。 */
    private static final Map<UUID, Long> LAST_NIGHT_TERROR_MESSAGE_TICKS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        settleCultivationDay(player);
        applyPerilAmbience(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulInjuryUtility.syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulInjuryUtility.syncToClient(player);
        }
    }

    /** SHAKEN 档起夜间惊醒：睡眠判定直接否决，玩家无法入眠/被惊醒。 */
    @SubscribeEvent
    public static void onSleepingTimeCheck(SleepingTimeCheckEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || SoulInjuryUtility.getTier(player).ordinal()
                        < SoulInjuryUtility.InjuryTier.SHAKEN.ordinal()) {
            return;
        }
        event.setResult(Event.Result.DENY);
        long gameTime = player.serverLevel().getGameTime();
        Long lastMessageTick = LAST_NIGHT_TERROR_MESSAGE_TICKS.get(player.getUUID());
        if (lastMessageTick == null
                || gameTime < lastMessageTick
                || gameTime - lastMessageTick >= NIGHT_TERROR_MESSAGE_COOLDOWN_TICKS) {
            LAST_NIGHT_TERROR_MESSAGE_TICKS.put(player.getUUID(), gameTime);
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_injury_night_terror"),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_NIGHT_TERROR_MESSAGE_TICKS.remove(player.getUUID());
            SoulInjuryUtility.removeSyncState(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_NIGHT_TERROR_MESSAGE_TICKS.clear();
        SoulInjuryUtility.clearSyncState();
    }

    /**
     * 修真日结算（lazy：登录/日界补算由常规 tick 兜住）。
     * 折寿先于疗伤结算，避免当日自然恢复把玩家从 PERIL 档"治"出去后又免掉折寿。
     */
    private static void settleCultivationDay(ServerPlayer player) {
        long calendarTick = player.serverLevel().getServer().overworld().getDayTime();
        long lastSettledTick = PlayerSoulInjuryData.getLastNaturalHealTick(player);
        if (lastSettledTick <= 0L || calendarTick < lastSettledTick) {
            // 首次结算或时间倒拨：只重定基准，不结算（R2 负差值 clamp 为 0）
            PlayerSoulInjuryData.setLastNaturalHealTick(player, calendarTick);
            return;
        }
        if (calendarTick - lastSettledTick < LifespanUtility.TICKS_PER_DAY) {
            return;
        }
        // 正向 clamp：无论跳过多少天，单次只结一天并重定基准到当前
        if (SoulInjuryUtility.getTier(player) == SoulInjuryUtility.InjuryTier.PERIL) {
            EntityLifespanData.addAgeTicks(
                    player,
                    LifespanUtility.ticksForYears(
                            CommonConfig.SOUL_INJURY_PERIL_LIFESPAN_YEARS_PER_DAY.get()
                    )
            );
        }
        int stars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars)
                .orElse(SoulStrength.MINIMUM_ILLUMINATED_STARS);
        double healPerDay = CommonConfig.SOUL_INJURY_NATURAL_HEAL_PER_DAY.get()
                * (stars >= SoulStrength.MAXIMUM_ILLUMINATED_STARS
                        ? CommonConfig.SOUL_INJURY_SEVEN_STAR_HEAL_MULTIPLIER.get()
                        : 1.0D);
        SoulInjuryUtility.heal(player, healPerDay, SoulInjuryUtility.HealSource.NATURAL);
        PlayerSoulInjuryData.setLastNaturalHealTick(player, calendarTick);
    }

    /** PERIL 常驻体感：低音量耳鸣心跳 + 短暗影脉冲（服务端驱动，无需客户端包）。 */
    private static void applyPerilAmbience(ServerPlayer player) {
        if (SoulInjuryUtility.getTier(player) != SoulInjuryUtility.InjuryTier.PERIL) {
            return;
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                PERIL_DARKNESS_PULSE_TICKS,
                0
        ));
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.WARDEN_HEARTBEAT,
                SoundSource.PLAYERS,
                0.3F,
                0.8F
        );
    }
}
