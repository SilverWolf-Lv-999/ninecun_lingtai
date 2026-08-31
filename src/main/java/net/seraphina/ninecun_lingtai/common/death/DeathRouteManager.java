package net.seraphina.ninecun_lingtai.common.death;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.ascension.AscensionManager;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.breakthrough.BreakthroughRitualManager;
import net.seraphina.ninecun_lingtai.common.causality.CausalityHistoryManager;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenDeathRouteScreenPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.risk.RiskContext;
import net.seraphina.ninecun_lingtai.common.risk.RiskFailureType;
import net.seraphina.ninecun_lingtai.common.risk.RiskSettlementService;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationManager;
import net.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 死亡路线状态机（灵气生态与死亡复活日志 §6.2）。
 * <b>唯一</b>的死亡路线 LivingDeathEvent 订阅入口在 DeathRouteEventHandler（HIGHEST），
 * 玩家分支 cancel 接管；真实死亡通过 bypass 标记重新触发，保证渡劫 fail/行为账本/
 * 大因果等既有订阅者在"尘埃落定"时正常收到事件。
 */
public final class DeathRouteManager {
    /** 重新触发真实死亡时置位：本类订阅者放行。 */
    private static final Set<UUID> BYPASS = new HashSet<>();
    /** 原始伤害源与凶手（会话存续期内存级；重启后按环境死亡结算）。 */
    private static final Map<UUID, DamageSource> ORIGINAL_SOURCES = new HashMap<>();
    private static final Map<UUID, UUID> KILLER_UUIDS = new HashMap<>();
    private static final Random NONCE_RANDOM = new Random();
    private static final double SOUL_RETURN_ARRIVE_DISTANCE_SQUARED = 16.0D;

    private DeathRouteManager() {
    }

    public static boolean isBypassing(UUID playerUuid) {
        return BYPASS.contains(playerUuid);
    }

    public static boolean hasSession(MinecraftServer server, UUID playerUuid) {
        return DeathChoiceSavedData.get(server).getSession(playerUuid) != null;
    }

    // ------------------------------------------------------------------
    // 接管
    // ------------------------------------------------------------------

    /**
     * @return true = 已接管（调用方 cancel 事件）；false = 放行 vanilla 死亡
     */
    public static boolean intercept(ServerPlayer player, DamageSource source) {
        MinecraftServer server = player.server;
        if (BYPASS.contains(player.getUUID())) {
            return false;
        }
        // 魂游态的"死亡"由魂游系统自己 cancel（血锁），不属于本状态机
        if (net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager
                .isSoulRoaming(player)) {
            return false;
        }
        // 凡人不入轮回抉择
        CultivationStage stage = PlayerCultivationData.getStage(player).orElse(null);
        if (stage == null) {
            return false;
        }
        DeathChoiceSavedData savedData = DeathChoiceSavedData.get(server);
        // 虚空坠落无处可归：直接按正常重生结算，不给选择界面（施工图坑 3）
        // 已有会话期间再次致死（理论上无敌挡不住的来源）同理兜底
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || savedData.getSession(player.getUUID()) != null) {
            savedData.removeSession(player.getUUID());
            settleRespawnPenalty(player, 0.0D);
            return false;
        }

        // 镜像既有订阅者的"死亡时刻"清理（事件被 cancel 后它们收不到；全部幂等，
        // 真实死亡重触发时会再走一遍原生订阅者，无害）
        mirrorDeathMomentCleanup(player);

        long now = MonotonicCalendarUtility.getCalendarTick(server);
        long nonce = NONCE_RANDOM.nextLong();
        DeathChoiceSession session = new DeathChoiceSession(
                player.getUUID(),
                player.level().dimension().location().toString(),
                player.blockPosition(),
                computeAvailableMask(player, stage),
                nonce,
                now + CommonConfig.DEATH_CHOOSE_TIMEOUT_SECONDS.get() * 20L
        );
        savedData.putSession(session);
        ORIGINAL_SOURCES.put(player.getUUID(), source);
        if (source.getEntity() instanceof ServerPlayer killer && killer != player) {
            KILLER_UUIDS.put(player.getUUID(), killer.getUUID());
        }
        // 血锁 1 + 无敌，掉落延迟到路线确定
        player.setHealth(1.0F);
        player.setInvulnerable(true);
        player.removeAllEffects();
        CausalityHistoryManager.record(player, "death",
                "濒死入轮回抉择；原因：" + source.getMsgId());
        sendOpenScreen(player, session);
        return true;
    }

    /** 事件被 cancel 后收不到 LivingDeathEvent 的既有订阅者，在此镜像其清理逻辑。 */
    private static void mirrorDeathMomentCleanup(ServerPlayer player) {
        TribulationManager.fail(player);
        AscensionManager.fail(player);
        if (PlayerMeditationManager.isMeditating(player)) {
            PlayerMeditationManager.stopMeditating(player);
        }
        BreakthroughRitualManager.interrupt(player, "death");
    }

    private static int computeAvailableMask(ServerPlayer player, CultivationStage stage) {
        int mask = 1 << DeathRoute.RESPAWN.ordinal();
        int realmOrdinal = stage.realm().ordinal();
        int stars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars).orElse(0);
        if (realmOrdinal >= CultivationRealm.FOUNDATION_ESTABLISHMENT.ordinal() && stars >= 1) {
            mask |= 1 << DeathRoute.SOUL_RETURN.ordinal();
        }
        if (realmOrdinal >= CultivationRealm.NASCENT_SOUL.ordinal()) {
            mask |= 1 << DeathRoute.BURN_NASCENT.ordinal();
        }
        // POSSESS_MOB：M4 属性移植方案落地前永远灰显
        return mask;
    }

    private static void sendOpenScreen(ServerPlayer player, DeathChoiceSession session) {
        long now = MonotonicCalendarUtility.getCalendarTick(player.server);
        int remainingSeconds = (int) Math.max(1L,
                (session.chooseDeadlineCalendarTick() - now) / 20L);
        List<String> reasons = new ArrayList<>();
        for (DeathRoute route : DeathRoute.values()) {
            if (session.isRouteAvailable(route)) {
                reasons.add("");
            } else {
                reasons.add(switch (route) {
                    case SOUL_RETURN -> "screen.ninecun_lingtai.death_route.locked.soul_return";
                    case BURN_NASCENT -> "screen.ninecun_lingtai.death_route.locked.burn_nascent";
                    case POSSESS_MOB -> "screen.ninecun_lingtai.death_route.locked.possess";
                    default -> "";
                });
            }
        }
        ModNetwork.sendOpenDeathRouteScreen(player, new OpenDeathRouteScreenPacket(
                session.nonce(),
                remainingSeconds,
                session.availableMask(),
                reasons
        ));
    }

    // ------------------------------------------------------------------
    // 路线选择
    // ------------------------------------------------------------------

    public static void handleChoice(ServerPlayer player, long nonce, DeathRoute route) {
        MinecraftServer server = player.server;
        DeathChoiceSavedData savedData = DeathChoiceSavedData.get(server);
        DeathChoiceSession session = savedData.getSession(player.getUUID());
        if (session == null || session.state() != DeathChoiceSession.State.CHOOSING
                || session.nonce() != nonce) {
            return;
        }
        if (!session.isRouteAvailable(route)) {
            ModMain.LOGGER.warn("[death] {} chose unavailable route {}",
                    player.getName().getString(), route);
            return;
        }
        switch (route) {
            case RESPAWN -> executeRespawnRoute(player, session, 0.0D);
            case SOUL_RETURN -> startSoulReturn(player, session);
            case BURN_NASCENT -> executeBurnNascent(player, session);
            case POSSESS_MOB -> {
                // 不可达（mask 恒未置位），防御性忽略
            }
        }
    }

    /** 正常重生：结算核心损失后重新触发真实死亡（bypass 放行 → 掉落 + 原生订阅者）。 */
    private static void executeRespawnRoute(
            ServerPlayer player,
            DeathChoiceSession session,
            double extraSoulInjury
    ) {
        MinecraftServer server = player.server;
        DeathChoiceSavedData.get(server).removeSession(player.getUUID());
        settleRespawnPenalty(player, extraSoulInjury);
        DamageSource source = ORIGINAL_SOURCES.remove(player.getUUID());
        KILLER_UUIDS.remove(player.getUUID());
        if (source == null) {
            source = player.damageSources().genericKill();
        }
        BYPASS.add(player.getUUID());
        try {
            player.setInvulnerable(false);
            player.invulnerableTime = 0;
            if (!player.hurt(source, Float.MAX_VALUE)) {
                player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
            }
        } finally {
            BYPASS.remove(player.getUUID());
        }
    }

    /**
     * 正常重生的核心损失（死亡路线口径：练气掉 1 小层；筑基+掉大境界进度 30% + 魂伤）。
     * 风险结算标记 coreLossHandled——掉层/掉进度是核心损失，结算服务只补伤痕。
     */
    private static void settleRespawnPenalty(ServerPlayer player, double extraSoulInjury) {
        CultivationStage stage = PlayerCultivationData.getStage(player).orElse(null);
        if (stage == null) {
            return;
        }
        if (stage.realm() == CultivationRealm.QI_REFINING) {
            int lostLayers = CommonConfig.DEATH_RESPAWN_QI_LAYER_LOSS.get();
            int newLayer = Math.max(CultivationStage.MINIMUM_LAYER, stage.layer() - lostLayers);
            if (newLayer != stage.layer()) {
                PlayerCultivationData.setStage(player,
                        new CultivationStage(stage.realm(), newLayer));
            }
        } else {
            PlayerSpiritualEnergyData.retainEnergyRatio(
                    player, 1.0D - CommonConfig.DEATH_RESPAWN_PROGRESS_LOSS_RATIO.get());
        }
        double injury = CommonConfig.DEATH_RESPAWN_SOUL_INJURY.get() + extraSoulInjury;
        SoulInjuryUtility.addInjury(player, injury, SoulInjuryUtility.InjurySource.DEATH);
        RiskSettlementService.settle(player, new RiskContext(
                RiskFailureType.DEATH, stage.realm(), 1.0D, "death_route_respawn", true));
        ModNetwork.sendCultivationInfo(player);
    }

    // ------------------------------------------------------------------
    // 魂魄归体
    // ------------------------------------------------------------------

    private static void startSoulReturn(ServerPlayer player, DeathChoiceSession session) {
        MinecraftServer server = player.server;
        int stars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars).orElse(1);
        long limitTicks = (long) stars
                * CommonConfig.DEATH_SOUL_RETURN_MINUTES_PER_STAR.get() * 60L * 20L;
        session.setState(DeathChoiceSession.State.SOUL_RETURNING);
        session.setReturnDeadlineCalendarTick(
                MonotonicCalendarUtility.getCalendarTick(server) + limitTicks);
        DeathChoiceSavedData savedData = DeathChoiceSavedData.get(server);
        savedData.putSession(session);
        teleportToRespawnPoint(player);
        applySoulForm(player);
        player.displayClientMessage(Component.translatable(
                "message.ninecun_lingtai.death.soul_return_started",
                limitTicks / 20L / 60L
        ), false);
    }

    /** 魂形态：隐身 + 缓落 + 可飞 + 无敌 + 血锁 1，飘回尸身即成。 */
    private static void applySoulForm(ServerPlayer player) {
        player.setHealth(1.0F);
        player.setInvulnerable(true);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, -1, 0, false, false));
    }

    private static void removeSoulForm(ServerPlayer player) {
        player.setInvulnerable(false);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.SLOW_FALLING);
    }

    private static void finishSoulReturn(ServerPlayer player, DeathChoiceSession session) {
        MinecraftServer server = player.server;
        DeathChoiceSavedData.get(server).removeSession(player.getUUID());
        ORIGINAL_SOURCES.remove(player.getUUID());
        removeSoulForm(player);
        player.setHealth(Math.max(1.0F, player.getMaxHealth() / 2.0F));
        SoulInjuryUtility.addInjury(
                player,
                CommonConfig.DEATH_SOUL_RETURN_SUCCESS_SOUL_INJURY.get(),
                SoulInjuryUtility.InjurySource.DEATH
        );
        creditKillerLedger(server, player);
        CausalityHistoryManager.record(player, "death", "魂魄归体成功，原地还阳");
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.death.soul_return_success"), false);
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0F, 1.4F);
    }

    private static void timeoutSoulReturn(ServerPlayer player, DeathChoiceSession session) {
        MinecraftServer server = player.server;
        removeSoulForm(player);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.death.soul_return_timeout"), false);
        double extra = CommonConfig.DEATH_SOUL_RETURN_TIMEOUT_EXTRA_SOUL_INJURY.get();
        if (CommonConfig.DEATH_PVE_MODE.get()) {
            // PVE 预设：转普通重生加重罚（不真死，人已在魂形位置，拉回重生点）
            DeathChoiceSavedData.get(server).removeSession(player.getUUID());
            ORIGINAL_SOURCES.remove(player.getUUID());
            KILLER_UUIDS.remove(player.getUUID());
            settleRespawnPenalty(player, extra);
            teleportToRespawnPoint(player);
            player.setHealth(Math.max(1.0F, player.getMaxHealth() / 2.0F));
        } else {
            executeRespawnRoute(player, session, extra);
        }
    }

    // ------------------------------------------------------------------
    // 燃元婴
    // ------------------------------------------------------------------

    private static void executeBurnNascent(ServerPlayer player, DeathChoiceSession session) {
        MinecraftServer server = player.server;
        CultivationStage stage = PlayerCultivationData.getStage(player).orElse(null);
        if (stage == null || stage.realm().ordinal() < CultivationRealm.NASCENT_SOUL.ordinal()) {
            return;
        }
        // 折寿基数取燃婴前的寿元上限（化神+现状无寿元，跌落后重新有——先记上限再改境界）
        int maximumYearsBefore = LifespanUtility.getMaximumLifespanYears(player);
        DeathChoiceSavedData.get(server).removeSession(player.getUUID());
        ORIGINAL_SOURCES.remove(player.getUUID());
        player.setInvulnerable(false);
        PlayerCultivationData.setStage(player, new CultivationStage(
                CultivationRealm.GOLDEN_CORE, CultivationStage.MAXIMUM_LAYER));
        int maximumYearsAfter = LifespanUtility.getMaximumLifespanYears(player);
        int burnBaseYears = maximumYearsBefore > 0 ? maximumYearsBefore : maximumYearsAfter;
        long burnedTicks = (long) (LifespanUtility.ticksForYears(burnBaseYears)
                * CommonConfig.DEATH_BURN_NASCENT_LIFESPAN_LOSS_RATIO.get());
        EntityLifespanData.addAgeTicks(player, burnedTicks);
        player.setHealth(Math.max(1.0F, player.getMaxHealth() / 2.0F));
        player.clearFire();
        SoulInjuryUtility.addInjury(
                player,
                CommonConfig.DEATH_RESPAWN_SOUL_INJURY.get(),
                SoulInjuryUtility.InjurySource.DEATH
        );
        RiskSettlementService.settle(player, new RiskContext(
                RiskFailureType.DEATH, stage.realm(), 1.0D, "death_route_burn_nascent", true));
        ModNetwork.sendCultivationInfo(player);
        creditKillerLedger(server, player);
        CausalityHistoryManager.record(player, "death", "燃元婴续命，跌落金丹");
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.death.burn_nascent_done"), false);
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.6F);
    }

    // ------------------------------------------------------------------
    // tick / 登录恢复 / 清理
    // ------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        DeathChoiceSavedData savedData = DeathChoiceSavedData.get(server);
        if (savedData.getAllSessions().isEmpty()) {
            return;
        }
        long now = MonotonicCalendarUtility.getCalendarTick(server);
        for (DeathChoiceSession session : List.copyOf(savedData.getAllSessions())) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerUuid());
            if (player == null) {
                continue; // 掉线：会话冻结在 SavedData，登录时恢复
            }
            if (session.state() == DeathChoiceSession.State.CHOOSING) {
                if (now > session.chooseDeadlineCalendarTick()) {
                    player.displayClientMessage(Component.translatable(
                            "message.ninecun_lingtai.death.choose_timeout"), false);
                    executeRespawnRoute(player, session, 0.0D);
                }
                continue;
            }
            // SOUL_RETURNING
            if (player.level().dimension().location().toString().equals(session.dimension())
                    && player.position().distanceToSqr(
                            Vec3.atCenterOf(session.deathPosition()))
                    <= SOUL_RETURN_ARRIVE_DISTANCE_SQUARED) {
                finishSoulReturn(player, session);
                continue;
            }
            if (now > session.returnDeadlineCalendarTick()) {
                timeoutSoulReturn(player, session);
            }
        }
    }

    /** 登录恢复：CHOOSING 重开界面（过期直接结算），SOUL_RETURNING 重挂魂形。 */
    public static void restoreOnLogin(ServerPlayer player) {
        MinecraftServer server = player.server;
        DeathChoiceSession session = DeathChoiceSavedData.get(server)
                .getSession(player.getUUID());
        if (session == null) {
            return;
        }
        long now = MonotonicCalendarUtility.getCalendarTick(server);
        if (session.state() == DeathChoiceSession.State.CHOOSING) {
            if (now > session.chooseDeadlineCalendarTick()) {
                executeRespawnRoute(player, session, 0.0D);
                return;
            }
            player.setHealth(1.0F);
            player.setInvulnerable(true);
            sendOpenScreen(player, session);
            return;
        }
        applySoulForm(player);
    }

    private static void creditKillerLedger(MinecraftServer server, ServerPlayer victim) {
        UUID killerUuid = KILLER_UUIDS.remove(victim.getUUID());
        if (killerUuid == null) {
            return;
        }
        ServerPlayer killer = server.getPlayerList().getPlayer(killerUuid);
        if (killer == null) {
            return;
        }
        // 受害者未走真实死亡，行为账本的击杀采样不会触发，在此镜像（口径同事件订阅者）
        PlayerBehaviorLedgerData.recordKill(
                killer,
                PlayerCultivationData.getStage(victim).isPresent()
                        ? PlayerBehaviorLedgerData.KillCategory.CULTIVATOR
                        : PlayerBehaviorLedgerData.KillCategory.ALLY
        );
    }

    private static void teleportToRespawnPoint(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel respawnLevel = server.getLevel(player.getRespawnDimension());
        if (respawnLevel == null) {
            respawnLevel = server.overworld();
        }
        BlockPos respawnPosition = player.getRespawnPosition();
        Vec3 target = null;
        if (respawnPosition != null) {
            target = Player.findRespawnPositionAndUseSpawnBlock(
                    respawnLevel, respawnPosition, player.getRespawnAngle(),
                    player.isRespawnForced(), true
            ).orElse(null);
        }
        if (target == null) {
            respawnLevel = server.overworld();
            BlockPos sharedSpawn = respawnLevel.getSharedSpawnPos();
            target = Vec3.atBottomCenterOf(sharedSpawn.above());
        }
        player.teleportTo(respawnLevel, target.x, target.y, target.z,
                player.getYRot(), player.getXRot());
    }

    public static void clear() {
        BYPASS.clear();
        ORIGINAL_SOURCES.clear();
        KILLER_UUIDS.clear();
    }
}
