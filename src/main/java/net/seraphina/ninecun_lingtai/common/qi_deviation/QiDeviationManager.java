package net.seraphina.ninecun_lingtai.common.qi_deviation;

import net.seraphina.ninecun_lingtai.common.ascension.AscensionManager;
import net.seraphina.ninecun_lingtai.common.breakthrough.BreakthroughRitualManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerPillToxinData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.status.ScarManager;
import net.seraphina.ninecun_lingtai.common.status.ScarType;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 走火入魔（施工图 §6.4，M4 切片）：触发判定、发作期效果与化解。
 *
 * <p>发作以 {@code ScarType.QI_DEVIATION} 伤痕承载（60 秒限时施加），到期清理由
 * Scar 框架（{@code ScarEventHandler}）负责，本类只管施加与化解两端：</p>
 *
 * <p><b>触发（均要求打坐中，渡劫/飞升/仪式盘坐豁免——对应施工图 FORCED 不判）：</b>
 * ①单次受伤超过最大生命 25%（概率一次性判定）；②丹毒 ≥60 强行打坐（每分钟判定）；
 * ③心魔劫失败留下的心障 / 劫后虚弱未消时打坐（每分钟判定——"数日内"以劫后虚弱
 * 与心障两种伤痕的存续期为窗口，贴合 Scar 框架而不引入自立计时）。</p>
 *
 * <p><b>发作（60 秒）：</b>每 5 秒一跳真伤（魔法伤，随严重度加深）、灵力持续散逸
 * （逐秒衰减，硬扛到期恰余两成）、不可施法（{@code SpellCastManager} 入口拦截）、
 * 屏幕扭曲（反胃药水效果复用原版后处理，零客户端代码）。</p>
 *
 * <p><b>化解：</b>静心丹立服立解（丹效 {@code resolve_qi_deviation}，PillEffectApplier
 * 的 case 见集中接线清单）；他人潜行右键持续护法 5 秒解除；硬扛到期自然结束。</p>
 */
public final class QiDeviationManager {
    // ==== 触发参数（建议配置键 qiDeviation.*，见集中接线清单） ====
    /** 单次受伤触发线（最大生命占比）。建议键 qiDeviation.damageTriggerHealthFraction。 */
    private static final double DAMAGE_TRIGGER_HEALTH_FRACTION = 0.25D;
    /** 受伤触发概率。建议键 qiDeviation.damageTriggerChance。 */
    private static final double DAMAGE_TRIGGER_CHANCE = 0.15D;
    /** 丹毒触发阈值。建议键 qiDeviation.toxinTriggerThreshold。 */
    private static final double TOXIN_TRIGGER_THRESHOLD = 60.0D;
    /** 丹毒打坐每分钟触发概率。建议键 qiDeviation.toxinTriggerChancePerMinute。 */
    private static final double TOXIN_TRIGGER_CHANCE_PER_MINUTE = 0.10D;
    /** 心障/劫后虚弱打坐每分钟触发概率。建议键 qiDeviation.scarTriggerChancePerMinute。 */
    private static final double SCAR_TRIGGER_CHANCE_PER_MINUTE = 0.08D;

    // ==== 发作参数 ====
    /** 发作时长（tick）。建议键 qiDeviation.outbreakDurationTicks。 */
    private static final long OUTBREAK_DURATION_TICKS = 1200L;
    /** 真伤脉冲间隔（tick）。建议键 qiDeviation.pulseIntervalTicks。 */
    private static final int PULSE_INTERVAL_TICKS = 100;
    /** 真伤基础值与严重度加成。建议键 qiDeviation.pulseDamageBase / pulseDamagePerSeverity。 */
    private static final float PULSE_DAMAGE_BASE = 1.0F;
    private static final float PULSE_DAMAGE_PER_SEVERITY = 0.5F;
    /** 硬扛到期的灵力保留比。建议键 qiDeviation.energyRetentionAtEnd。 */
    private static final double ENERGY_RETENTION_AT_END = 0.2D;
    /** 逐秒散逸系数：60 次方后恰为保留比。 */
    private static final double ENERGY_RETENTION_PER_SECOND = Math.pow(
            ENERGY_RETENTION_AT_END,
            20.0D / OUTBREAK_DURATION_TICKS
    );

    // ==== 护法参数 ====
    /** 护法所需持续时长（tick）。建议键 qiDeviation.guardDurationTicks。 */
    private static final long GUARD_DURATION_TICKS = 100L;
    /** 护法最大间距。建议键 qiDeviation.guardMaxDistance。 */
    private static final double GUARD_MAX_DISTANCE = 3.0D;

    /** 发作者 → 护法会话。 */
    private static final Map<UUID, GuardSession> GUARD_SESSIONS = new HashMap<>();
    /** 已观测到发作中的玩家（用于收尾播报与外部施加的兜底提示）。 */
    private static final Set<UUID> ACTIVE_OUTBREAKS = new HashSet<>();

    private record GuardSession(UUID guardianId, long finishGameTime) {
    }

    private QiDeviationManager() {
    }

    // ==== 触发 ====

    /** 触发点 ①：打坐中单次受伤超过最大生命 25%（LivingHurtEvent 接线）。 */
    public static void onMeditatingPlayerHurt(ServerPlayer player, float amount) {
        if (!canTrigger(player) || player.getMaxHealth() <= 0.0F) {
            return;
        }
        double healthFraction = amount / player.getMaxHealth();
        if (healthFraction <= DAMAGE_TRIGGER_HEALTH_FRACTION) {
            return;
        }
        if (player.getRandom().nextDouble() >= DAMAGE_TRIGGER_CHANCE) {
            return;
        }
        int severity = (int) Math.min(
                3L,
                1L + (long) Math.floor((healthFraction - DAMAGE_TRIGGER_HEALTH_FRACTION) / 0.25D)
        );
        startOutbreak(player, severity);
    }

    /** 触发点 ②③：每分钟一次的打坐风险判定（玩家 tick 接线，tickCount 对齐 1200）。 */
    public static void rollMeditationRisks(ServerPlayer player) {
        if (!canTrigger(player)) {
            return;
        }
        double toxin = PlayerPillToxinData.getToxin(player);
        if (toxin >= TOXIN_TRIGGER_THRESHOLD
                && player.getRandom().nextDouble() < TOXIN_TRIGGER_CHANCE_PER_MINUTE) {
            int severity = 1
                    + (toxin >= PlayerPillToxinData.EROSION_THRESHOLD ? 1 : 0)
                    + (toxin >= PlayerPillToxinData.MARROW_THRESHOLD ? 1 : 0);
            startOutbreak(player, severity);
            return;
        }
        boolean hauntedByDemon = ScarManager.hasScar(player, ScarType.HEART_DEMON);
        boolean tribulationWeakened = ScarManager.hasScar(player, ScarType.POST_TRIBULATION_WEAKNESS);
        if ((hauntedByDemon || tribulationWeakened)
                && player.getRandom().nextDouble() < SCAR_TRIGGER_CHANCE_PER_MINUTE) {
            int severity = Math.max(1, Math.min(3, ScarManager.getSeverity(player, ScarType.HEART_DEMON)));
            startOutbreak(player, severity);
        }
    }

    /** 触发总闸：打坐中、未在发作、且不处于渡劫/飞升/破境仪式的系统盘坐（FORCED 不判）。 */
    private static boolean canTrigger(ServerPlayer player) {
        return PlayerMeditationManager.isMeditating(player)
                && !ScarManager.hasScar(player, ScarType.QI_DEVIATION)
                && !TribulationManager.isInTribulation(player)
                && !AscensionManager.isInAscension(player)
                && !BreakthroughRitualManager.isInRitual(player);
    }

    // ==== 发作 ====

    private static void startOutbreak(ServerPlayer player, int severity) {
        PlayerMeditationManager.stopMeditating(player);
        ScarManager.applyScar(player, ScarType.QI_DEVIATION, severity, OUTBREAK_DURATION_TICKS);
        ACTIVE_OUTBREAKS.add(player.getUUID());
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.qi_deviation_start",
                        "灵力逆行！丹田如遭火焚，狂暴气机在经脉中横冲直撞——速服静心丹，或请道友潜行贴身护法！"
                ),
                false
        );
        player.playNotifySound(SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.8F, 0.5F);
    }

    /** 发作期逐秒效果（玩家 tick 接线，每 20 tick 一次）。 */
    public static void tickPlayer(ServerPlayer player, long gameTime) {
        boolean inOutbreak = ScarManager.hasScar(player, ScarType.QI_DEVIATION);
        if (!inOutbreak) {
            if (ACTIVE_OUTBREAKS.remove(player.getUUID())) {
                GUARD_SESSIONS.remove(player.getUUID());
                player.displayClientMessage(
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.qi_deviation_survived",
                                "狂乱的气机终于平息，真元十不存三——侥幸未殒，已是万幸"
                        ),
                        false
                );
            }
            return;
        }
        if (ACTIVE_OUTBREAKS.add(player.getUUID())) {
            // 兜底：伤痕由外部系统（风险结算等）施加时补一次发作提示。
            player.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.qi_deviation_start",
                            "灵力逆行！丹田如遭火焚，狂暴气机在经脉中横冲直撞——速服静心丹，或请道友潜行贴身护法！"
                    ),
                    false
            );
        }
        // 灵力持续散逸：逐秒衰减，硬扛整个发作期恰余两成。
        PlayerSpiritualEnergyData.retainEnergyRatio(player, ENERGY_RETENTION_PER_SECOND);
        // 屏幕扭曲体感：反胃复用原版后处理，零客户端代码。
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
        // 每 5 秒一跳真伤（魔法伤绕过护甲），随伤痕严重度加深。
        if (player.tickCount % PULSE_INTERVAL_TICKS == 0) {
            int severity = ScarManager.getSeverity(player, ScarType.QI_DEVIATION);
            float damage = PULSE_DAMAGE_BASE + PULSE_DAMAGE_PER_SEVERITY * Math.max(1, severity);
            player.hurt(player.damageSources().magic(), damage);
        }
        tickGuardSession(player, gameTime);
    }

    // ==== 化解 ====

    /** 化解点 ①：静心丹（PillEffectApplier 的 resolve_qi_deviation case 调用，见接线清单）。 */
    public static boolean resolveByPill(ServerPlayer player) {
        if (!ScarManager.resolveScar(player, ScarType.QI_DEVIATION)) {
            return false;
        }
        ACTIVE_OUTBREAKS.remove(player.getUUID());
        GUARD_SESSIONS.remove(player.getUUID());
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.qi_deviation_pill_resolved",
                        "丹力入腹化作一泓清凉，狂乱灵力如汤沃雪，霎时平复"
                ),
                false
        );
        return true;
    }

    /** 化解点 ②：他人潜行右键发起护法（须贴身保持潜行 5 秒）。 */
    public static boolean tryStartGuard(ServerPlayer guardian, ServerPlayer target) {
        if (guardian == target || !ScarManager.hasScar(target, ScarType.QI_DEVIATION)) {
            return false;
        }
        if (GUARD_SESSIONS.containsKey(target.getUUID())) {
            guardian.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.qi_deviation_guard_busy",
                            "已有道友在为其护法，切勿灵力相冲"
                    ),
                    true
            );
            return true;
        }
        GUARD_SESSIONS.put(
                target.getUUID(),
                new GuardSession(guardian.getUUID(), guardian.level().getGameTime() + GUARD_DURATION_TICKS)
        );
        guardian.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.qi_deviation_guard_start",
                        "你渡出一缕平和灵力护持道友心脉——保持潜行贴身五秒，不可离开"
                ),
                false
        );
        target.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.qi_deviation_guard_incoming",
                        "%s 正以自身灵力为你护法，压制逆行气机",
                        guardian.getName().getString()
                ),
                false
        );
        return true;
    }

    private static void tickGuardSession(ServerPlayer target, long gameTime) {
        GuardSession session = GUARD_SESSIONS.get(target.getUUID());
        if (session == null) {
            return;
        }
        ServerPlayer guardian = target.getServer() == null
                ? null
                : target.getServer().getPlayerList().getPlayer(session.guardianId());
        boolean valid = guardian != null
                && guardian.isAlive()
                && guardian.isShiftKeyDown()
                && guardian.level() == target.level()
                && guardian.distanceTo(target) <= GUARD_MAX_DISTANCE;
        if (!valid) {
            GUARD_SESSIONS.remove(target.getUUID());
            if (guardian != null) {
                guardian.displayClientMessage(
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.qi_deviation_guard_broken",
                                "护法中断——灵力反哺一空，道友仍在走火"
                        ),
                        true
                );
            }
            return;
        }
        if (gameTime < session.finishGameTime()) {
            return;
        }
        GUARD_SESSIONS.remove(target.getUUID());
        if (ScarManager.resolveScar(target, ScarType.QI_DEVIATION)) {
            ACTIVE_OUTBREAKS.remove(target.getUUID());
            guardian.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.qi_deviation_guard_success",
                            "你稳稳托住了道友的心脉，逆行的气机被缓缓抚平"
                    ),
                    false
            );
            target.displayClientMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.qi_deviation_guard_resolved",
                            "一缕温润灵力渡入丹田，狂暴的气机顺服下来——道友之恩没齿难忘"
                    ),
                    false
            );
        }
    }

    /** 服务器停止时清理会话内存。 */
    public static void clearAll() {
        GUARD_SESSIONS.clear();
        ACTIVE_OUTBREAKS.clear();
    }
}
