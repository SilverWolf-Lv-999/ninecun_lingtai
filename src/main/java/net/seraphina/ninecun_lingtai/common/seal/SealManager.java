package net.seraphina.ninecun_lingtai.common.seal;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenSealCarveScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SealAlertPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.ring.RingStorageManager;
import net.seraphina.ninecun_lingtai.common.seal.lockgrid.LockGridSessionManager;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.seraphina.ninecun_lingtai.utility.SoulTemperingUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 器禁业务唯一入口（服务端）。数据权威在 {@link SealRecordSavedData}；
 * 本类持有的仅是运行期仪式会话（刻印/废除/力破），掉线即中断、无需持久化。
 */
public final class SealManager {
    /** 触碰告警的节流（每记录），防止右键连点刷屏主人。 */
    private static final int TOUCH_ALERT_COOLDOWN_TICKS = 200;
    /** 反噬禁魂伤 = 该值 × 层品阶（阵禁篇数值总表；建议键 sealBacklashInjuryPerGrade）。 */
    private static final double BACKLASH_INJURY_PER_GRADE = 15.0D;
    /** 遗念禁遗言长度上限（字符）。 */
    public static final int LEGACY_MESSAGE_MAX_LENGTH = 120;
    /** 锚位休眠钝痛检查间隔。 */
    private static final int DORMANCY_CHECK_INTERVAL_TICKS = 200;
    /** 距离档阈值（格）：< 64 咫尺、< 512 百里、其余千里之外。 */
    private static final double DISTANCE_TIER_NEAR = 64.0D;
    private static final double DISTANCE_TIER_FAR = 512.0D;

    private static final Map<UUID, RitualSession> RITUAL_SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_TOUCH_ALERT_TICK = new HashMap<>();

    private SealManager() {
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    public static Optional<SealRecord> getRecord(MinecraftServer server, ItemStack itemStack) {
        return SealItemUtility.getSealId(itemStack)
                .map(sealId -> SealRecordSavedData.get(server).getRecord(sealId));
    }

    /** 是否存在未休眠的某类禁制层。 */
    public static boolean hasActiveLayer(MinecraftServer server, SealRecord record, SealKind kind) {
        for (SealLayer layer : record.layers()) {
            if (layer.kind() == kind
                    && !SealAnchorUtility.isLayerDormant(server, record.ownerUuid(), layer)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 认主禁拦截：非主人使用/打开被封物品时返回 true（调用方取消行为）。
     * 同时触发传讯禁"被触碰"告警与 lastSeen 更新。
     */
    public static boolean blocksUse(ServerPlayer user, ItemStack itemStack) {
        MinecraftServer server = user.server;
        SealRecord record = getRecord(server, itemStack).orElse(null);
        if (record == null || record.layers().isEmpty() || record.ownerUuid().equals(user.getUUID())) {
            return false;
        }
        record.updateLastSeen(user.level().dimension().location(), user.blockPosition());
        SealRecordSavedData.get(server).setDirty();
        if (!hasActiveLayer(server, record, SealKind.OWNERSHIP)) {
            return false;
        }
        notifyTouchAlert(server, record, user.serverLevel(), user.position(),
                "message.ninecun_lingtai.seal.alert.touched");
        user.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.seal.blocked_by_owner"), true);
        return true;
    }

    // ------------------------------------------------------------------
    // 潜行右键路由（SealEventHandler 调用）
    // ------------------------------------------------------------------

    /** @return true = 已消费该次交互 */
    public static boolean handleSneakUse(ServerPlayer player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.isEmpty()) {
            return false;
        }
        MinecraftServer server = player.server;
        SealRecord record = getRecord(server, itemStack).orElse(null);
        if (record != null && record.isBrokenResidue()) {
            if (record.ownerUuid().equals(player.getUUID())) {
                readFingerprints(player, itemStack, record);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.ninecun_lingtai.seal.residue_not_yours"), true);
            }
            return true;
        }
        if (record != null && !record.layers().isEmpty()
                && !record.ownerUuid().equals(player.getUUID())) {
            LockGridSessionManager.startCrack(player, hand, record);
            return true;
        }
        // 自己的物品（或未刻印物品）：开刻印/管理界面
        openCarveScreen(player, hand, record);
        return true;
    }

    private static void openCarveScreen(
            ServerPlayer player,
            InteractionHand hand,
            @Nullable SealRecord record
    ) {
        List<Byte> ownLayerKinds = new ArrayList<>();
        if (record != null) {
            for (SealLayer layer : record.layers()) {
                ownLayerKinds.add((byte) layer.kind().ordinal());
            }
        }
        ModNetwork.sendOpenSealCarveScreen(player, new OpenSealCarveScreenPacket(
                hand == InteractionHand.MAIN_HAND,
                SealAnchorUtility.getUsedAnchors(player.server, player.getUUID()),
                SealAnchorUtility.getTotalAnchors(player),
                SealAnchorUtility.getAvailableAnchors(player),
                SealAnchorUtility.getLayerCap(player),
                ownLayerKinds
        ));
    }

    // ------------------------------------------------------------------
    // 刻印 / 废除仪式
    // ------------------------------------------------------------------

    public static void startCarve(
            ServerPlayer player,
            InteractionHand hand,
            SealKind kind,
            double detonateThreshold,
            String legacyMessage
    ) {
        if (!kind.isImplemented()) {
            return;
        }
        String trimmedLegacy = legacyMessage == null ? "" : legacyMessage.trim();
        if (trimmedLegacy.length() > LEGACY_MESSAGE_MAX_LENGTH) {
            trimmedLegacy = trimmedLegacy.substring(0, LEGACY_MESSAGE_MAX_LENGTH);
        }
        if (kind == SealKind.LEGACY && trimmedLegacy.isEmpty()) {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.seal.legacy_message_required",
                    "遗念禁须先录下一句遗言，方能起笔。"), true);
            return;
        }
        if (RITUAL_SESSIONS.containsKey(player.getUUID())
                || LockGridSessionManager.hasSession(player)) {
            return;
        }
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.isEmpty()) {
            return;
        }
        MinecraftServer server = player.server;
        SealRecord existing = getRecord(server, itemStack).orElse(null);
        if (existing != null && !existing.ownerUuid().equals(player.getUUID())
                && !existing.layers().isEmpty()) {
            return;
        }
        Optional<SoulStrength> soulStrength = PlayerAptitudeData.getSoulStrength(player);
        Optional<CultivationStage> stage = PlayerCultivationData.getStage(player);
        if (soulStrength.isEmpty() || stage.isEmpty()) {
            feedback(player, "message.ninecun_lingtai.seal.carve_no_cultivation");
            return;
        }
        int used = SealAnchorUtility.getUsedAnchors(server, player.getUUID());
        if (used + 1 > SealAnchorUtility.getAvailableAnchors(player)) {
            feedback(player, "message.ninecun_lingtai.seal.carve_no_anchor");
            return;
        }
        int existingLayers = existing == null ? 0 : existing.layers().size();
        if (existingLayers + 1 > SealAnchorUtility.getLayerCap(player)) {
            feedback(player, "message.ninecun_lingtai.seal.carve_layer_cap");
            return;
        }
        double energyCost = CommonConfig.SEAL_CARVE_ENERGY_PER_GRADE.get() * kind.getGrade();
        Map<ElementType, Double> consumedEnergy =
                PlayerSpiritualEnergyData.consumeEnergyAny(player, energyCost);
        if (consumedEnergy == null) {
            feedback(player, "message.ninecun_lingtai.seal.carve_no_energy");
            return;
        }
        double temperingCost = CommonConfig.SEAL_CARVE_TEMPERING_COST_PER_GRADE.get() * kind.getGrade();
        if (!SoulTemperingUtility.tryReduceProgress(player, temperingCost)) {
            // 灵力已扣，温养不足则全额退灵力并拒绝
            refundEnergy(player, consumedEnergy, 1.0D);
            feedback(player, "message.ninecun_lingtai.seal.carve_no_tempering");
            return;
        }
        UUID sealId = SealItemUtility.getOrCreateSealId(itemStack);
        RitualSession session = new RitualSession(
                RitualType.CARVE,
                hand,
                sealId,
                kind,
                Math.max(0.5D, Math.min(0.9D, detonateThreshold)),
                CommonConfig.SEAL_CARVE_RITUAL_TICKS.get(),
                player.position(),
                consumedEnergy,
                trimmedLegacy
        );
        RITUAL_SESSIONS.put(player.getUUID(), session);
        feedback(player, "message.ninecun_lingtai.seal.carve_started");
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    public static void startRevoke(ServerPlayer player, InteractionHand hand) {
        if (RITUAL_SESSIONS.containsKey(player.getUUID())) {
            return;
        }
        ItemStack itemStack = player.getItemInHand(hand);
        MinecraftServer server = player.server;
        SealRecord record = getRecord(server, itemStack).orElse(null);
        if (record == null || record.layers().isEmpty()
                || !record.ownerUuid().equals(player.getUUID())) {
            return;
        }
        RITUAL_SESSIONS.put(player.getUUID(), new RitualSession(
                RitualType.REVOKE,
                hand,
                record.sealId(),
                null,
                0.0D,
                CommonConfig.SEAL_REVOKE_TICKS.get(),
                player.position(),
                Map.of(),
                ""
        ));
        feedback(player, "message.ninecun_lingtai.seal.revoke_started");
    }

    /** 锁纹界面"力破"入口（LockGridSessionManager 转入）。 */
    public static void startForceBreak(ServerPlayer player, InteractionHand hand, SealRecord record) {
        if (RITUAL_SESSIONS.containsKey(player.getUUID())) {
            return;
        }
        MinecraftServer server = player.server;
        long now = MonotonicCalendarUtility.getCalendarTick(server);
        double power = getBreakPower(player);
        double strength = record.totalStrength(record.isDeathPactActive(now))
                * (record.isDeathPactActive(now) ? 1.0D : 1.0D);
        double crushLine = strength * CommonConfig.SEAL_FORCE_BREAK_CRUSH_MULTIPLIER.get();
        notifyTouchAlert(server, record, player.serverLevel(), player.position(),
                "message.ninecun_lingtai.seal.alert.force_break");
        if (power >= crushLine) {
            // 一念破尽（仍必触发自毁判定）
            finishForceBreak(player, hand, record);
            return;
        }
        int tier = (int) Math.ceil(Math.log(crushLine / Math.max(0.01D, power)) / Math.log(2.0D));
        tier = Math.max(1, Math.min(4, tier));
        int durationTicks = CommonConfig.SEAL_FORCE_BREAK_BASE_SECONDS.get() * 20 * (1 << tier);
        RITUAL_SESSIONS.put(player.getUUID(), new RitualSession(
                RitualType.FORCE_BREAK,
                hand,
                record.sealId(),
                null,
                0.0D,
                durationTicks,
                player.position(),
                Map.of(),
                ""
        ));
        player.displayClientMessage(Component.translatable(
                "message.ninecun_lingtai.seal.force_break_started",
                durationTicks / 20
        ), false);
    }

    /** 破禁力 = 魂强星数 × (1 + 境界序号 × 0.5)。 */
    public static double getBreakPower(ServerPlayer player) {
        int stars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars).orElse(0);
        int realmOrdinal = PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm().ordinal()).orElse(0);
        return stars * (1.0D + realmOrdinal * 0.5D);
    }

    /** 每 tick 由 SealEventHandler 驱动：仪式姿态校验与倒计时。 */
    public static void tickRituals(MinecraftServer server) {
        Iterator<Map.Entry<UUID, RitualSession>> iterator = RITUAL_SESSIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RitualSession> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            RitualSession session = entry.getValue();
            if (player == null) {
                iterator.remove();
                continue;
            }
            ItemStack held = player.getItemInHand(session.hand);
            boolean holdingSameItem = SealItemUtility.getSealId(held)
                    .map(session.sealId::equals).orElse(false);
            boolean posture = player.isShiftKeyDown()
                    && player.position().distanceToSqr(session.anchorPosition) < 1.0D;
            if (!holdingSameItem || !posture || !player.isAlive()) {
                iterator.remove();
                interruptRitual(player, session);
                continue;
            }
            // 力破：灵力狂泄，扣不动即中断；嗡鸣渐强全场可闻
            if (session.type == RitualType.FORCE_BREAK) {
                if (session.remainingTicks % 20 == 0) {
                    double drain = CommonConfig.SEAL_FORCE_BREAK_ENERGY_PER_SECOND.get();
                    if (PlayerSpiritualEnergyData.consumeEnergyAny(player, drain) == null) {
                        iterator.remove();
                        interruptRitual(player, session);
                        continue;
                    }
                }
                if (session.remainingTicks % 30 == 0) {
                    float pitch = 0.6F + 1.2F
                            * (1.0F - (float) session.remainingTicks / session.totalTicks);
                    player.serverLevel().playSound(null, player.blockPosition(),
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.2F, pitch);
                }
            } else if (session.remainingTicks % 40 == 0) {
                player.serverLevel().playSound(null, player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.5F, 1.4F);
            }
            if (--session.remainingTicks > 0) {
                continue;
            }
            iterator.remove();
            completeRitual(player, session);
        }
    }

    public static void onPlayerHurt(ServerPlayer player) {
        RitualSession session = RITUAL_SESSIONS.remove(player.getUUID());
        if (session != null) {
            interruptRitual(player, session);
        }
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        RITUAL_SESSIONS.remove(player.getUUID());
    }

    public static void clear() {
        RITUAL_SESSIONS.clear();
        LAST_TOUCH_ALERT_TICK.clear();
    }

    private static void interruptRitual(ServerPlayer player, RitualSession session) {
        switch (session.type) {
            case CARVE -> {
                // 中断退还灵力 50%，温养不退（有痛感）
                refundEnergy(player, session.consumedEnergy, 0.5D);
                feedback(player, "message.ninecun_lingtai.seal.carve_interrupted");
            }
            case REVOKE -> feedback(player, "message.ninecun_lingtai.seal.revoke_interrupted");
            case FORCE_BREAK -> feedback(player, "message.ninecun_lingtai.seal.force_break_interrupted");
        }
    }

    private static void completeRitual(ServerPlayer player, RitualSession session) {
        MinecraftServer server = player.server;
        ItemStack itemStack = player.getItemInHand(session.hand);
        switch (session.type) {
            case CARVE -> completeCarve(player, itemStack, session);
            case REVOKE -> completeRevoke(player, itemStack, session);
            case FORCE_BREAK -> {
                SealRecord record = SealRecordSavedData.get(server).getRecord(session.sealId);
                if (record != null && !record.layers().isEmpty()) {
                    finishForceBreak(player, session.hand, record);
                }
            }
        }
    }

    private static void completeCarve(ServerPlayer player, ItemStack itemStack, RitualSession session) {
        MinecraftServer server = player.server;
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        SealRecord record = savedData.getOrCreateRecord(session.sealId, player.getUUID());
        if (!record.ownerUuid().equals(player.getUUID())) {
            return;
        }
        int stars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars).orElse(1);
        int realmOrdinal = PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm().ordinal()).orElse(0);
        double strength = stars
                * (1.0D + realmOrdinal * 0.5D)
                * (1.0D + 0.1D * record.layers().size());
        ElementType element = PlayerAptitudeData.getSpiritualRoot(player)
                .flatMap(root -> root.elements().stream()
                        .filter(ElementType::isFiveElement).findFirst())
                .orElse(ElementType.CHAOS);
        SealLayer layer = new SealLayer(
                session.kind,
                strength,
                stars,
                realmOrdinal,
                element,
                session.kind == SealKind.DETONATE ? session.detonateThreshold : 0.0D,
                savedData.nextCarveSequence(),
                session.kind == SealKind.LEGACY ? session.legacyMessage : null
        );
        record.layers().add(0, layer);
        record.updateLastSeen(player.level().dimension().location(), player.blockPosition());
        savedData.setDirty();
        updateDisplaySnapshot(server, itemStack, record);
        feedback(player, "message.ninecun_lingtai.seal.carve_finished");
        player.serverLevel().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.8F);
    }

    private static void completeRevoke(ServerPlayer player, ItemStack itemStack, RitualSession session) {
        MinecraftServer server = player.server;
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        SealRecord record = savedData.getRecord(session.sealId);
        if (record == null || record.layers().isEmpty()
                || !record.ownerUuid().equals(player.getUUID())) {
            return;
        }
        record.layers().remove(0);
        if (record.layers().isEmpty() && record.fingerprints().isEmpty()) {
            savedData.removeRecord(record.sealId());
            SealItemUtility.clearSealTags(itemStack);
        } else {
            updateDisplaySnapshot(server, itemStack, record);
        }
        savedData.setDirty();
        feedback(player, "message.ninecun_lingtai.seal.revoke_finished");
    }

    // ------------------------------------------------------------------
    // 破解结算（锁纹/力破共用）
    // ------------------------------------------------------------------

    /** 力破完成：必触发反噬与自毁（阵禁篇 5.1"蛮力是最贵的路"）→ 未自毁则层尽去、留指纹。 */
    private static void finishForceBreak(ServerPlayer breaker, InteractionHand hand, SealRecord record) {
        MinecraftServer server = breaker.server;
        for (SealLayer layer : record.layers()) {
            if (layer.kind() == SealKind.BACKLASH
                    && !SealAnchorUtility.isLayerDormant(server, record.ownerUuid(), layer)) {
                applyBacklashInjury(breaker, layer);
            }
        }
        boolean hadDetonate = record.layers().stream()
                .anyMatch(layer -> layer.kind() == SealKind.DETONATE);
        if (hadDetonate) {
            detonate(breaker, hand, record);
            return;
        }
        completeBreak(breaker, hand, record, true);
        breaker.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.seal.force_break_finished"), false);
    }

    /** 反噬禁：破解失败/力破时反击破解者魂伤 +15 × 层品阶（走魂伤唯一入口）。 */
    public static void applyBacklashInjury(ServerPlayer breaker, SealLayer layer) {
        SoulInjuryUtility.addInjury(
                breaker,
                BACKLASH_INJURY_PER_GRADE * layer.kind().getGrade(),
                SoulInjuryUtility.InjurySource.SEAL_BACKLASH
        );
        breaker.displayClientMessage(Component.translatableWithFallback(
                "message.ninecun_lingtai.seal.backlash_struck",
                "禁制深处一根倒刺猛然扎进识海——反噬禁！魂魄剧痛。"), false);
    }

    /**
     * 单层被巧破/剥除时的层规则结算（LockGridSessionManager 逐层调用）：
     * 遗念禁——以主人口吻把遗言送进破解者识海，并即时落一枚神念指纹
     * （中途收手也带不走清白，阵禁篇 4.1"复仇线索"）。
     */
    public static void onLayerBroken(ServerPlayer breaker, SealRecord record, SealLayer layer) {
        if (layer.kind() == SealKind.LEGACY) {
            deliverLegacyMessage(breaker, record, layer);
            // 中层遗念即时落指纹（收手也留痕）；最外层剥尽时由 completeBreak 统一落，避免重复
            if (!record.layers().isEmpty()) {
                appendFingerprint(breaker.server, record, breaker, true);
            }
        }
    }

    private static void deliverLegacyMessage(ServerPlayer breaker, SealRecord record, SealLayer layer) {
        if (layer.legacyMessage() == null || layer.legacyMessage().isEmpty()) {
            return;
        }
        String ownerName = breaker.server.getProfileCache() == null ? null
                : breaker.server.getProfileCache().get(record.ownerUuid())
                        .map(com.mojang.authlib.GameProfile::getName)
                        .orElse(null);
        breaker.displayClientMessage(Component.translatableWithFallback(
                "message.ninecun_lingtai.seal.legacy_delivered",
                "一道残存的神念拂过识海，%s的声音在你耳畔响起：“%s”",
                ownerName == null || ownerName.isEmpty() ? "故人" : ownerName,
                layer.legacyMessage()
        ), false);
        breaker.serverLevel().playSound(null, breaker.blockPosition(),
                SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    /** 全层破尽：留指纹、层清空、记录转残骸、物品展示解封。 */
    public static void completeBreak(
            ServerPlayer breaker,
            InteractionHand hand,
            SealRecord record,
            boolean successful
    ) {
        MinecraftServer server = breaker.server;
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        // 力破整摞剥尽时，途中每一枚遗念都会响起（逐层巧破则由 onLayerBroken 送达）
        for (SealLayer layer : record.layers()) {
            if (layer.kind() == SealKind.LEGACY) {
                deliverLegacyMessage(breaker, record, layer);
            }
        }
        appendFingerprint(server, record, breaker, successful);
        record.layers().clear();
        record.updateLastSeen(breaker.level().dimension().location(), breaker.blockPosition());
        savedData.setDirty();
        ItemStack held = breaker.getItemInHand(hand);
        if (SealItemUtility.getSealId(held).map(record.sealId()::equals).orElse(false)) {
            SealItemUtility.writeDisplaySnapshot(held, false, 0, false);
        }
        notifyOwner(server, record, "message.ninecun_lingtai.seal.owner_seal_broken");
        breaker.serverLevel().playSound(null, breaker.blockPosition(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** 自毁禁触发：戒指清空储物空间、普通物品湮灭；层保留（禁制赢了）。 */
    public static void detonate(ServerPlayer breaker, InteractionHand hand, SealRecord record) {
        MinecraftServer server = breaker.server;
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        appendFingerprint(server, record, breaker, false);
        ItemStack held = breaker.getItemInHand(hand);
        boolean isCarrier = SealItemUtility.getSealId(held)
                .map(record.sealId()::equals).orElse(false);
        boolean ringCleared = false;
        if (isCarrier) {
            ringCleared = RingStorageManager.annihilateRingContents(server, held);
            if (!ringCleared) {
                held.shrink(held.getCount());
                savedData.removeRecord(record.sealId());
            }
        }
        savedData.setDirty();
        breaker.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.seal.detonated"), false);
        notifyOwner(server, record, "message.ninecun_lingtai.seal.owner_seal_detonated");
        if (ringCleared) {
            // 储物戒指化为飞灰的全服播报（不点名坐标）
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.ninecun_lingtai.seal.detonate_broadcast"), false);
        }
        breaker.serverLevel().playSound(null, breaker.blockPosition(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    public static void appendFingerprint(
            MinecraftServer server,
            SealRecord record,
            ServerPlayer breaker,
            boolean successful
    ) {
        int stars = PlayerAptitudeData.getSoulStrength(breaker)
                .map(SoulStrength::illuminatedStars).orElse(0);
        int realmOrdinal = PlayerCultivationData.getStage(breaker)
                .map(stage -> stage.realm().ordinal()).orElse(-1);
        ElementType element = PlayerAptitudeData.getSpiritualRoot(breaker)
                .flatMap(root -> root.elements().stream()
                        .filter(ElementType::isFiveElement).findFirst())
                .orElse(ElementType.CHAOS);
        record.fingerprints().add(new SealFingerprint(
                stars,
                realmOrdinal,
                element,
                MonotonicCalendarUtility.getCalendarTick(server),
                successful
        ));
        SealRecordSavedData.get(server).setDirty();
    }

    /** 主人触碰残骸读取神念指纹（精度随主人魂强，阵禁篇 5.4 表）。读毕回收记录。 */
    private static void readFingerprints(ServerPlayer owner, ItemStack itemStack, SealRecord record) {
        MinecraftServer server = owner.server;
        int ownerStars = PlayerAptitudeData.getSoulStrength(owner)
                .map(SoulStrength::illuminatedStars).orElse(1);
        owner.displayClientMessage(Component.translatable(
                "message.ninecun_lingtai.seal.fingerprint_header", record.fingerprints().size()), false);
        if (ownerStars >= 3) {
            for (SealFingerprint fingerprint : record.fingerprints()) {
                Component line;
                String realmName = fingerprint.realmOrdinal() >= 0
                        && fingerprint.realmOrdinal() < net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm.values().length
                        ? net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm
                                .values()[fingerprint.realmOrdinal()].getChineseName()
                        : "?";
                if (ownerStars >= 5) {
                    line = Component.translatable(
                            "message.ninecun_lingtai.seal.fingerprint_entry_element",
                            fingerprint.soulStars(),
                            realmName,
                            Component.translatable("element.ninecun_lingtai."
                                    + fingerprint.element().getSerializedName())
                    );
                } else {
                    line = Component.translatable(
                            "message.ninecun_lingtai.seal.fingerprint_entry",
                            fingerprint.soulStars(),
                            realmName
                    );
                }
                owner.displayClientMessage(line, false);
            }
        }
        if (ownerStars >= 7 && record.lastSeenDimension() != null) {
            // 7 星：最后一次感知方位（一次性，读后即散）
            BlockPos lastSeen = record.lastSeenPosition();
            int octant = computeOctant(owner.position(), Vec3.atCenterOf(lastSeen));
            owner.displayClientMessage(Component.translatable(
                    "message.ninecun_lingtai.seal.fingerprint_direction",
                    Component.translatable("message.ninecun_lingtai.seal.alert.direction." + octant)
            ), false);
        }
        SealRecordSavedData.get(server).removeRecord(record.sealId());
        SealItemUtility.clearSealTags(itemStack);
    }

    // ------------------------------------------------------------------
    // 传讯禁 / 死契
    // ------------------------------------------------------------------

    /** 传讯禁告警（带节流）：仅当记录存在未休眠传讯层时打给在线主人。 */
    public static void notifyTouchAlert(
            MinecraftServer server,
            SealRecord record,
            ServerLevel sourceLevel,
            Vec3 sourcePosition,
            String reasonKey
    ) {
        if (!hasActiveLayer(server, record, SealKind.MESSAGE)) {
            return;
        }
        long gameTime = server.overworld().getGameTime();
        Long lastAlert = LAST_TOUCH_ALERT_TICK.get(record.sealId());
        if (lastAlert != null && gameTime - lastAlert < TOUCH_ALERT_COOLDOWN_TICKS) {
            return;
        }
        LAST_TOUCH_ALERT_TICK.put(record.sealId(), gameTime);
        sendAlert(server, record, sourceLevel, sourcePosition, reasonKey);
    }

    private static void sendAlert(
            MinecraftServer server,
            SealRecord record,
            @Nullable ServerLevel sourceLevel,
            @Nullable Vec3 sourcePosition,
            String reasonKey
    ) {
        ServerPlayer owner = server.getPlayerList().getPlayer(record.ownerUuid());
        if (owner == null) {
            return;
        }
        byte octant = -1;
        byte distanceTier = 2;
        if (sourceLevel != null && sourcePosition != null
                && owner.level().dimension() == sourceLevel.dimension()) {
            double distance = owner.position().distanceTo(sourcePosition);
            distanceTier = distance < DISTANCE_TIER_NEAR ? (byte) 0
                    : distance < DISTANCE_TIER_FAR ? (byte) 1 : (byte) 2;
            octant = (byte) computeOctant(owner.position(), sourcePosition);
        }
        ModNetwork.sendSealAlert(owner, new SealAlertPacket(octant, distanceTier, reasonKey));
    }

    private static void notifyOwner(MinecraftServer server, SealRecord record, String messageKey) {
        ServerPlayer owner = server.getPlayerList().getPlayer(record.ownerUuid());
        if (owner != null) {
            owner.displayClientMessage(Component.translatable(messageKey), false);
        }
    }

    /** 主人死亡：其掉落物上的禁制进入死契（强度 ×1.5、持续 N 修真日）。 */
    public static void applyDeathPact(ServerPlayer owner, Iterable<ItemStack> droppedStacks) {
        MinecraftServer server = owner.server;
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        long until = MonotonicCalendarUtility.getCalendarTick(server)
                + CommonConfig.SEAL_DEATH_PACT_DAYS.get() * MonotonicCalendarUtility.TICKS_PER_DAY;
        for (ItemStack itemStack : droppedStacks) {
            UUID sealId = SealItemUtility.getSealId(itemStack).orElse(null);
            if (sealId == null) {
                continue;
            }
            SealRecord record = savedData.getRecord(sealId);
            if (record == null || record.layers().isEmpty()
                    || !record.ownerUuid().equals(owner.getUUID())) {
                continue;
            }
            record.setDeathPactUntilCalendarTick(until);
            record.updateLastSeen(owner.level().dimension().location(), owner.blockPosition());
            updateDisplaySnapshot(server, itemStack, record);
            savedData.setDirty();
        }
    }

    /** 死契期方位脉冲 + 锚位休眠钝痛（低频，由 SealEventHandler 的 ServerTick 驱动）。 */
    public static void tickSlowScan(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        long now = MonotonicCalendarUtility.getCalendarTick(server);
        SealRecordSavedData savedData = SealRecordSavedData.get(server);
        if (gameTime % CommonConfig.SEAL_DEATH_PACT_ALERT_INTERVAL_TICKS.get() == 0) {
            for (SealRecord record : savedData.getAllRecords()) {
                if (!record.isDeathPactActive(now) || record.lastSeenDimension() == null
                        || !hasActiveLayer(server, record, SealKind.MESSAGE)) {
                    continue;
                }
                ServerLevel lastSeenLevel = null;
                for (ServerLevel level : server.getAllLevels()) {
                    if (level.dimension().location().toString().equals(record.lastSeenDimension())) {
                        lastSeenLevel = level;
                        break;
                    }
                }
                sendAlert(server, record, lastSeenLevel,
                        Vec3.atCenterOf(record.lastSeenPosition()),
                        "message.ninecun_lingtai.seal.alert.death_pact");
            }
        }
        if (gameTime % DORMANCY_CHECK_INTERVAL_TICKS == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (savedData.getUsedAnchors(player.getUUID()) == 0) {
                    continue;
                }
                int dormant = SealAnchorUtility.getDormantCount(player);
                int cached = savedData.getCachedDormantCount(player.getUUID());
                if (dormant != cached) {
                    savedData.setCachedDormantCount(player.getUUID(), dormant);
                    if (dormant > cached) {
                        player.displayClientMessage(Component.translatable(
                                "message.ninecun_lingtai.seal.anchors_dormant", dormant), false);
                    } else if (dormant == 0) {
                        player.displayClientMessage(Component.translatable(
                                "message.ninecun_lingtai.seal.anchors_awake"), false);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    public static void updateDisplaySnapshot(
            MinecraftServer server,
            ItemStack itemStack,
            SealRecord record
    ) {
        // 隐匿禁生效时展示快照抹平：tooltip 与常物无异（阵禁篇 4.2 隐士流），
        // 服务端权威判定不受影响——只是不给神识与肉眼任何线索
        if (hasActiveLayer(server, record, SealKind.CONCEAL) && !record.layers().isEmpty()) {
            SealItemUtility.writeDisplaySnapshot(itemStack, false, 0, false);
            return;
        }
        int publicGrade = record.layers().stream()
                .mapToInt(layer -> layer.kind().getGrade()).max().orElse(0);
        SealItemUtility.writeDisplaySnapshot(
                itemStack,
                !record.layers().isEmpty(),
                publicGrade,
                record.isDeathPactActive(MonotonicCalendarUtility.getCalendarTick(server))
        );
    }

    /** 0=北 顺时针 7=西北。 */
    public static int computeOctant(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        int octant = (int) Math.floorMod(Math.round(angle / 45.0D), 8L);
        return octant;
    }

    private static void refundEnergy(
            ServerPlayer player,
            Map<ElementType, Double> consumedEnergy,
            double ratio
    ) {
        for (Map.Entry<ElementType, Double> entry : consumedEnergy.entrySet()) {
            PlayerSpiritualEnergyData.addEnergy(player, entry.getKey(), entry.getValue() * ratio);
        }
    }

    private static void feedback(ServerPlayer player, String messageKey) {
        player.displayClientMessage(Component.translatable(messageKey), true);
    }

    private enum RitualType {
        CARVE,
        REVOKE,
        FORCE_BREAK
    }

    private static final class RitualSession {
        private final RitualType type;
        private final InteractionHand hand;
        private final UUID sealId;
        @Nullable
        private final SealKind kind;
        private final double detonateThreshold;
        private final int totalTicks;
        private final Vec3 anchorPosition;
        private final Map<ElementType, Double> consumedEnergy;
        private final String legacyMessage;
        private int remainingTicks;

        private RitualSession(
                RitualType type,
                InteractionHand hand,
                UUID sealId,
                @Nullable SealKind kind,
                double detonateThreshold,
                int totalTicks,
                Vec3 anchorPosition,
                Map<ElementType, Double> consumedEnergy,
                String legacyMessage
        ) {
            this.type = type;
            this.hand = hand;
            this.sealId = sealId;
            this.kind = kind;
            this.detonateThreshold = detonateThreshold;
            this.totalTicks = totalTicks;
            this.remainingTicks = totalTicks;
            this.anchorPosition = anchorPosition;
            this.consumedEnergy = consumedEnergy;
            this.legacyMessage = legacyMessage;
        }
    }
}
