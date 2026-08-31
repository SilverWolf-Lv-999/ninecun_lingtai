package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.common.aura.SpiritualAuraManager;
import net.seraphina.ninecun_lingtai.common.block.entity.FormationDiscBlockEntity;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.registry.ModFormationContent;
import net.seraphina.ninecun_lingtai.common.seal.SealManager;
import net.seraphina.ninecun_lingtai.common.soul.SoulSenseManager;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阵法运转层（服务端 tick 驱动，施工图 6.4）：储能维持、聚灵注账、护罩吸伤、
 * 灵阵警报、生门瘫痪与复稳、幻影指引/灵力流下发。
 * 几何校验只在激活与瘫痪期满复核时发生，运转期靠方块移除事件驱动失效（风险 5）。
 */
public final class FormationRuntime {
    private static final String[] DIRECTION_FALLBACKS =
            {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};

    /** 已加载阵盘索引（BE onLoad/setRemoved 维护），给幻影指引做近盘查找。 */
    private static final Map<ResourceKey<Level>, Set<Long>> TRACKED_DISCS = new ConcurrentHashMap<>();
    /** 灵阵警报的域内滞留者（瞬态，不落盘）。 */
    private static final Map<ResourceKey<Level>, Map<Long, Set<UUID>>> INTRUDERS = new HashMap<>();

    private FormationRuntime() {
    }

    // ==================== 阵盘索引 ====================

    public static void trackDisc(ServerLevel level, BlockPos pos) {
        TRACKED_DISCS.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet())
                .add(pos.asLong());
    }

    public static void untrackDisc(ServerLevel level, BlockPos pos) {
        Set<Long> discs = TRACKED_DISCS.get(level.dimension());
        if (discs != null) {
            discs.remove(pos.asLong());
        }
    }

    public static void clear() {
        TRACKED_DISCS.clear();
        INTRUDERS.clear();
    }

    // ==================== 主 tick ====================

    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
        if (server.overworld().getGameTime() % FormationTuning.GUIDE_SEND_INTERVAL_TICKS == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                sendGuideIfHoldingDisc(player);
            }
        }
    }

    private static void tickLevel(ServerLevel level) {
        FormationSavedData savedData = FormationSavedData.get(level);
        if (savedData.getAllInstances().isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        List<FormationInstance> snapshot = new ArrayList<>(savedData.getAllInstances());
        for (FormationInstance instance : snapshot) {
            if (!level.isLoaded(instance.discPos())) {
                continue;
            }
            FormationDefinition definition =
                    FormationManager.INSTANCE.getDefinition(instance.formationId());
            if (definition == null) {
                // 数据包 /reload 后阵图被移除：实例安然散去，不崩（验收 §11）
                deactivate(level, instance, "message.ninecun_lingtai.formation.definition_gone");
                continue;
            }
            FormationDiscBlockEntity disc = getDisc(level, instance.discPos());
            if (disc == null) {
                savedData.removeInstance(instance.discPos());
                continue;
            }
            if (instance.isParalyzed(gameTime)) {
                continue;
            }
            if (instance.paralyzedUntilGameTime() != 0L) {
                // 瘫痪期满：静默复核几何——旗齐则复稳，缺旗则散
                recoverFromParalysis(level, instance, definition);
                continue;
            }
            if (gameTime % 20L == 0L && !payUpkeep(level, instance, definition, disc)) {
                continue;
            }
            if (definition.type() == FormationType.SPIRIT_GATHERING
                    && gameTime % FormationTuning.GATHER_INTERVAL_TICKS == 0L) {
                SpiritualAuraManager.addReserve(
                        level,
                        instance.discPos(),
                        FormationTuning.GATHER_RESERVE_PER_SECOND_PER_GRADE
                                * definition.grade()
                                * instance.efficiency()
                                * definition.getEffect("gather_rate", 1.0D)
                );
            }
            if (gameTime % FormationTuning.ALARM_SCAN_INTERVAL_TICKS == 0L) {
                scanIntruders(level, instance, definition);
            }
            if (gameTime % 40L == 0L) {
                emitAmbientParticles(level, instance, definition);
            }
            if (gameTime % FormationTuning.FLOW_SEND_INTERVAL_TICKS == 0L) {
                sendFlowFrames(level, instance, definition);
            }
        }
    }

    /** 维持耗能（聚灵含注灵附加）；储能见底 → 全阵崩碎。@return false = 已崩。 */
    private static boolean payUpkeep(
            ServerLevel level,
            FormationInstance instance,
            FormationDefinition definition,
            FormationDiscBlockEntity disc
    ) {
        double cost = FormationTuning.UPKEEP_PER_SECOND_PER_GRADE * definition.grade();
        if (definition.type() == FormationType.SPIRIT_GATHERING) {
            cost += FormationTuning.GATHER_ENERGY_PER_SECOND_PER_GRADE * definition.grade();
        }
        if (disc.consumeEnergy(cost)) {
            return true;
        }
        collapse(level, instance, "message.ninecun_lingtai.formation.energy_depleted",
                "阵盘储能见底，%s轰然崩碎。");
        return false;
    }

    private static void recoverFromParalysis(
            ServerLevel level,
            FormationInstance instance,
            FormationDefinition definition
    ) {
        FormationGeometryUtility.ValidationResult result =
                FormationGeometryUtility.validate(level, instance.discPos(), definition);
        if (result.success()) {
            instance.setParalyzedUntilGameTime(0L);
            instance.setEfficiency(result.efficiency());
            instance.flagPositions().clear();
            instance.flagElements().clear();
            for (FormationGeometryUtility.SlotResult slot : result.slots()) {
                instance.flagPositions().add(slot.actualPos());
                instance.flagElements().add(slot.element());
            }
            instance.setLifeGateIndex(level.random.nextInt(instance.flagPositions().size()));
            FormationSavedData.get(level).setDirty();
            notifyOwner(level.getServer(), instance, "message.ninecun_lingtai.formation.recovered",
                    "旗位重整，%s复稳运转。", definition.name());
            level.playSound(null, instance.discPos(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.BLOCKS, 0.8F, 1.5F);
        } else {
            deactivate(level, instance, "message.ninecun_lingtai.formation.paralysis_collapse");
        }
    }

    // ==================== 护府阵吸伤（LivingHurtEvent HIGH，先于防御法术） ====================

    public static void absorbDamage(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || event.getAmount() <= 0.0F) {
            return;
        }
        long gameTime = level.getGameTime();
        for (FormationInstance instance : FormationSavedData.get(level).getAllInstances()) {
            FormationDefinition definition =
                    FormationManager.INSTANCE.getDefinition(instance.formationId());
            if (definition == null
                    || definition.type() != FormationType.SHIELD
                    || instance.isParalyzed(gameTime)
                    || instance.paralyzedUntilGameTime() != 0L
                    || !isInsideDomain(instance, definition, entity.position())) {
                continue;
            }
            // 明阵粗放：域内众生皆护；灵阵有规则：只护阵主与白名单（阵禁篇 6.5）
            if (FormationAnchorLedger.isEnlightenmentActive(level.getServer(), instance)
                    && (!(entity instanceof Player player)
                            || !instance.isWhitelisted(player.getUUID()))) {
                continue;
            }
            FormationDiscBlockEntity disc = getDisc(level, instance.discPos());
            if (disc == null) {
                continue;
            }
            double energyPerDamage = FormationTuning.SHIELD_ENERGY_PER_DAMAGE
                    / Math.max(0.1D, instance.efficiency()
                            * definition.getEffect("shield_efficiency", 1.0D));
            double needed = event.getAmount() * energyPerDamage;
            double drained = disc.drainEnergyUpTo(needed);
            if (drained <= 0.0D) {
                collapse(level, instance, "message.ninecun_lingtai.formation.energy_depleted",
                        "阵盘储能见底，%s轰然崩碎。");
                return;
            }
            float absorbed = (float) (drained / energyPerDamage);
            float remainder = event.getAmount() - absorbed;
            level.playSound(null, entity.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.BLOCKS, 0.7F, 1.5F);
            level.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 1.0D, entity.getZ(), 8, 0.4D, 0.6D, 0.4D, 0.05D);
            if (remainder <= 0.001F) {
                event.setCanceled(true);
            } else {
                event.setAmount(remainder);
            }
            if (disc.getStoredEnergy() <= 0.0D) {
                collapse(level, instance, "message.ninecun_lingtai.formation.energy_depleted",
                        "阵盘储能见底，%s轰然崩碎。");
            }
            return;
        }
    }

    // ==================== 灵阵警报（点睛后的规则层） ====================

    private static void scanIntruders(
            ServerLevel level,
            FormationInstance instance,
            FormationDefinition definition
    ) {
        Map<Long, Set<UUID>> levelIntruders =
                INTRUDERS.computeIfAbsent(level.dimension(), key -> new HashMap<>());
        Set<UUID> inside = levelIntruders
                .computeIfAbsent(instance.discPos().asLong(), key -> new HashSet<>());
        if (!FormationAnchorLedger.isEnlightenmentActive(level.getServer(), instance)) {
            inside.clear();
            return;
        }
        Set<UUID> stillInside = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()
                    || instance.isWhitelisted(player.getUUID())
                    || !isInsideDomain(instance, definition, player.position())) {
                continue;
            }
            stillInside.add(player.getUUID());
            if (inside.add(player.getUUID())) {
                alarmOwner(level, instance, definition, player);
            }
        }
        inside.retainAll(stillInside);
    }

    /** 心头一悸（阵法版）：方位 + 阵名，不给坐标；直发消息，无 lang 也可跑。 */
    private static void alarmOwner(
            ServerLevel level,
            FormationInstance instance,
            FormationDefinition definition,
            ServerPlayer intruder
    ) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(instance.ownerUuid());
        if (owner == null) {
            return;
        }
        Component message;
        if (owner.level().dimension() == level.dimension()) {
            int octant = SealManager.computeOctant(owner.position(), intruder.position());
            message = Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.alarm_intrusion",
                    "心头一悸——%s的阵域有人踏入（方位：%s）。",
                    definition.name(),
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.formation.direction." + octant,
                            DIRECTION_FALLBACKS[octant])
            );
        } else {
            message = Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.alarm_intrusion_far",
                    "心头一悸——远方%s的阵域有人踏入。",
                    definition.name()
            );
        }
        owner.displayClientMessage(message, false);
        owner.displayClientMessage(message, true);
        owner.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.75F, 1.0F);
    }

    // ==================== 旗与盘的移除（事件驱动失效，风险 5） ====================

    /** 阵旗真被移除后（BreakEvent 未取消的场合 + 爆炸/活塞）由方块 onRemove 转入。 */
    public static void onFlagRemoved(ServerLevel level, BlockPos flagPos) {
        FormationInstance instance = findInstanceByFlag(level, flagPos);
        if (instance == null) {
            return;
        }
        long gameTime = level.getGameTime();
        if (instance.isParalyzed(gameTime)) {
            return; // 瘫痪期内的进一步拆旗，期满复核时自然散阵
        }
        FormationDefinition definition =
                FormationManager.INSTANCE.getDefinition(instance.formationId());
        int flagIndex = instance.flagPositions().indexOf(flagPos);
        if (flagIndex == instance.lifeGateIndex()) {
            // 生门被拔：全阵紊乱瘫痪，阵主剧烈警报（阵禁篇 7.2）
            instance.setParalyzedUntilGameTime(
                    gameTime + FormationTuning.LIFE_GATE_PARALYSIS_TICKS);
            FormationSavedData.get(level).setDirty();
            level.playSound(null, instance.discPos(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 1.2F, 0.5F);
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(instance.ownerUuid());
            if (owner != null) {
                owner.displayClientMessage(Component.translatableWithFallback(
                        "message.ninecun_lingtai.formation.life_gate_pulled",
                        "剧烈的悸动撕过识海——%s的生门旗被拔，全阵瘫痪！速去重整旗位！",
                        definition == null ? "" : definition.name()
                ), false);
                owner.playNotifySound(SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
        } else {
            deactivate(level, instance, "message.ninecun_lingtai.formation.flag_destroyed");
        }
    }

    public static void onDiscRemoved(ServerLevel level, BlockPos discPos) {
        FormationInstance instance = FormationSavedData.get(level).getInstance(discPos);
        if (instance != null) {
            deactivate(level, instance, "message.ninecun_lingtai.formation.disc_destroyed");
        }
        untrackDisc(level, discPos);
    }

    @Nullable
    public static FormationInstance findInstanceByFlag(ServerLevel level, BlockPos flagPos) {
        for (FormationInstance instance : FormationSavedData.get(level).getAllInstances()) {
            if (instance.flagPositions().contains(flagPos)) {
                return instance;
            }
        }
        return null;
    }

    /** 停转/散阵：移除实例并（可选）通知阵主。 */
    public static void deactivate(
            ServerLevel level,
            FormationInstance instance,
            @Nullable String reasonKey
    ) {
        FormationSavedData.get(level).removeInstance(instance.discPos());
        Map<Long, Set<UUID>> levelIntruders = INTRUDERS.get(level.dimension());
        if (levelIntruders != null) {
            levelIntruders.remove(instance.discPos().asLong());
        }
        if (reasonKey != null) {
            notifyOwner(level.getServer(), instance, reasonKey, defaultReasonFallback(reasonKey));
        }
    }

    private static String defaultReasonFallback(String reasonKey) {
        return switch (reasonKey) {
            case "message.ninecun_lingtai.formation.flag_destroyed" -> "一面阵旗被毁，%s应声而散。";
            case "message.ninecun_lingtai.formation.disc_destroyed" -> "阵盘被毁，%s应声而散。";
            case "message.ninecun_lingtai.formation.paralysis_collapse" -> "瘫痪期满旗位未复，%s散去。";
            case "message.ninecun_lingtai.formation.definition_gone" -> "阵图传承断绝，%s静静散去。";
            default -> "%s已散。";
        };
    }

    private static void collapse(
            ServerLevel level,
            FormationInstance instance,
            String reasonKey,
            String fallback
    ) {
        level.playSound(null, instance.discPos(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.BLOCKS, 1.2F, 0.5F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                instance.discPos().getX() + 0.5D, instance.discPos().getY() + 0.5D,
                instance.discPos().getZ() + 0.5D, 30, 1.5D, 0.5D, 1.5D, 0.01D);
        FormationSavedData.get(level).removeInstance(instance.discPos());
        notifyOwner(level.getServer(), instance, reasonKey, fallback);
    }

    private static void notifyOwner(
            MinecraftServer server,
            FormationInstance instance,
            String key,
            String fallback,
            Object... extraArgs
    ) {
        ServerPlayer owner = server.getPlayerList().getPlayer(instance.ownerUuid());
        if (owner == null) {
            return;
        }
        Object[] args;
        if (extraArgs.length > 0) {
            args = extraArgs;
        } else {
            FormationDefinition definition =
                    FormationManager.INSTANCE.getDefinition(instance.formationId());
            args = new Object[]{definition == null ? "阵法" : definition.name()};
        }
        owner.displayClientMessage(
                Component.translatableWithFallback(key, fallback, args), false);
    }

    /** 已加载区块内取阵盘 BE；未加载/被替换返回 null（不强制加载区块）。 */
    @Nullable
    private static FormationDiscBlockEntity getDisc(ServerLevel level, BlockPos discPos) {
        if (!level.isLoaded(discPos)) {
            return null;
        }
        return level.getBlockEntity(discPos) instanceof FormationDiscBlockEntity disc ? disc : null;
    }

    // ==================== 域判定与演出 ====================

    /** 半球护罩/阵域：以阵盘为心的上半球（略容脚下一格）。 */
    public static boolean isInsideDomain(
            FormationInstance instance,
            FormationDefinition definition,
            Vec3 position
    ) {
        Vec3 center = Vec3.atCenterOf(instance.discPos());
        if (position.y < center.y - 1.5D) {
            return false;
        }
        return position.distanceTo(center) <= definition.radius();
    }

    private static void emitAmbientParticles(
            ServerLevel level,
            FormationInstance instance,
            FormationDefinition definition
    ) {
        BlockPos pos = instance.discPos();
        if (definition.type() == FormationType.SHIELD) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    6, definition.radius() * 0.6D, definition.radius() * 0.3D,
                    definition.radius() * 0.6D, 0.0D);
        } else {
            level.sendParticles(ParticleTypes.ENCHANT,
                    pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                    10, definition.radius() * 0.4D, 0.8D, definition.radius() * 0.4D, 0.0D);
        }
    }

    // ==================== 幻影指引（手持阵盘潜行，阵禁篇 6.2） ====================

    private static void sendGuideIfHoldingDisc(ServerPlayer player) {
        if (!player.isShiftKeyDown()) {
            return;
        }
        boolean holdingDisc =
                player.getMainHandItem().is(ModFormationContent.FORMATION_DISC_ITEM.get())
                        || player.getOffhandItem().is(ModFormationContent.FORMATION_DISC_ITEM.get());
        if (!holdingDisc) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Set<Long> discs = TRACKED_DISCS.get(level.dimension());
        if (discs == null || discs.isEmpty()) {
            return;
        }
        FormationSavedData savedData = FormationSavedData.get(level);
        BlockPos bestPos = null;
        double bestDistance = FormationTuning.GUIDE_MAX_DISTANCE;
        for (long packed : discs) {
            BlockPos discPos = BlockPos.of(packed);
            if (savedData.getInstance(discPos) != null) {
                continue; // 运转中的阵不再出指引
            }
            double distance = Math.sqrt(discPos.distToCenterSqr(player.position()));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = discPos;
            }
        }
        if (bestPos == null || !(level.getBlockEntity(bestPos) instanceof FormationDiscBlockEntity disc)) {
            return;
        }
        FormationDefinition definition =
                FormationManager.INSTANCE.getDefinition(disc.getSelectedFormationId());
        if (definition == null) {
            return;
        }
        boolean senseOpen = SoulSenseManager.isActive(player);
        FormationGeometryUtility.ValidationResult result =
                FormationGeometryUtility.validate(level, bestPos, definition);
        List<FormationGuidePacket.Entry> entries = new ArrayList<>();
        for (int index = 0; index < definition.flagLayout().size(); index++) {
            FormationDefinition.FlagSlot slot = definition.flagLayout().get(index);
            FormationGeometryUtility.SlotResult slotResult = result.slots().get(index);
            boolean aligned = slotResult.found()
                    && slotResult.deviation() <= FormationTuning.GUIDE_ALIGNED_DISTANCE;
            entries.add(new FormationGuidePacket.Entry(
                    slotResult.expectedPos(),
                    elementIndexFromTag(slot),
                    aligned,
                    // 精确偏差连线只在神识开启时给（布阵也吃魂强，阵禁篇 6.2）
                    senseOpen && slotResult.found() && !aligned ? slotResult.actualPos() : null
            ));
        }
        FormationNetwork.sendGuide(player, new FormationGuidePacket(bestPos, entries));
    }

    private static byte elementIndexFromTag(FormationDefinition.FlagSlot slot) {
        String path = slot.materialTag().location().getPath();
        int slash = path.lastIndexOf('/');
        String suffix = slash < 0 ? path : path.substring(slash + 1);
        return switch (suffix) {
            case "metal" -> 0;
            case "wood" -> 1;
            case "water" -> 2;
            case "fire" -> 3;
            case "earth" -> 4;
            default -> -1;
        };
    }

    // ==================== 灵力流（生门观察，神识开启才下发） ====================

    private static void sendFlowFrames(
            ServerLevel level,
            FormationInstance instance,
            FormationDefinition definition
    ) {
        if (instance.flagPositions().size() < 2
                || instance.isFlowHidden(
                        MonotonicCalendarUtility.getCalendarTick(level.getServer()))) {
            return;
        }
        double viewRange = definition.radius() + FormationTuning.FLOW_EXTRA_VIEW_DISTANCE;
        Vec3 center = Vec3.atCenterOf(instance.discPos());
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (SoulSenseManager.isActive(player)
                    && player.position().distanceTo(center) <= viewRange) {
                viewers.add(player);
            }
        }
        if (viewers.isEmpty()) {
            return;
        }
        List<FormationFlowPacket.Segment> plainSegments =
                buildFlowSegments(instance, false);
        List<FormationFlowPacket.Segment> revealedSegments =
                buildFlowSegments(instance, true);
        for (ServerPlayer viewer : viewers) {
            int stars = PlayerAptitudeData.getSoulStrength(viewer)
                    .map(SoulStrength::illuminatedStars).orElse(0);
            // 魂强不足者服务端直接同质化（不发逆流位），不给客户端破解面
            boolean revealed = stars >= FormationTuning.LIFE_GATE_MIN_SOUL_STARS;
            FormationNetwork.sendFlow(viewer, new FormationFlowPacket(
                    instance.discPos(),
                    revealed ? revealedSegments : plainSegments,
                    FormationTuning.FLOW_SEND_INTERVAL_TICKS + 10
            ));
        }
    }

    private static List<FormationFlowPacket.Segment> buildFlowSegments(
            FormationInstance instance,
            boolean revealLifeGate
    ) {
        List<FormationFlowPacket.Segment> segments = new ArrayList<>();
        int flagCount = instance.flagPositions().size();
        for (int index = 0; index < flagCount; index++) {
            BlockPos from = instance.flagPositions().get(index);
            BlockPos to = instance.flagPositions().get((index + 1) % flagCount);
            boolean reversed = revealLifeGate && index == instance.lifeGateIndex();
            segments.add(new FormationFlowPacket.Segment(
                    from.getX() + 0.5F, from.getY() + 0.9F, from.getZ() + 0.5F,
                    to.getX() + 0.5F, to.getY() + 0.9F, to.getZ() + 0.5F,
                    reversed
            ));
        }
        return segments;
    }
}
