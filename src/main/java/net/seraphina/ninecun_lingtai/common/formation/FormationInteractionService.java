package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.aura.SpiritualAuraManager;
import net.seraphina.ninecun_lingtai.common.block.entity.FormationDiscBlockEntity;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.seraphina.ninecun_lingtai.common.seal.SealAnchorUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 阵盘交互路由（服务端）。潜行右键 = 慢操作（换阵图 / 点睛 / 驱阵）；
 * 普通右键 = 激活（一次性几何校验）/ 停转；持灵石右键 = 充能。
 * 全部文案 translatableWithFallback，无 lang 可跑（M4 约束）。
 */
public final class FormationInteractionService {

    /** 点睛二次确认：玩家 → (阵盘坐标, 截止 gameTime)。 */
    private static final Map<UUID, PendingEnlighten> PENDING_ENLIGHTEN = new HashMap<>();

    private record PendingEnlighten(BlockPos discPos, long deadlineGameTime) {
    }

    private FormationInteractionService() {
    }

    public static InteractionResult handleDiscUse(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            InteractionHand hand
    ) {
        if (!(level.getBlockEntity(pos) instanceof FormationDiscBlockEntity disc)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.SPIRITUAL_STONE.get())) {
            chargeWithSpiritStone(level, pos, player, held, disc);
            return InteractionResult.CONSUME;
        }
        FormationInstance instance = FormationSavedData.get(level).getInstance(pos);
        if (player.isShiftKeyDown()) {
            if (instance == null) {
                cycleFormation(player, disc);
            } else if (!instance.ownerUuid().equals(player.getUUID())) {
                actionbar(player, "message.ninecun_lingtai.formation.not_master",
                        "此阵非你所布，阵盘灵机对你毫无回应。");
            } else if (!instance.isEnlightened()) {
                tryEnlighten(level, pos, player, instance);
            } else {
                trySurge(level, pos, player, disc, instance);
            }
            return InteractionResult.CONSUME;
        }
        if (instance == null) {
            tryActivate(level, pos, player, disc);
        } else if (instance.ownerUuid().equals(player.getUUID())) {
            FormationRuntime.deactivate(level, instance, null);
            actionbar(player, "message.ninecun_lingtai.formation.deactivated",
                    "阵法缓缓停转，灵机归于沉寂。");
        } else {
            actionbar(player, "message.ninecun_lingtai.formation.not_master_stop",
                    "阵主方能收阵。要破此阵，去找它的旗。");
        }
        return InteractionResult.CONSUME;
    }

    // ==================== 充能 ====================

    private static void chargeWithSpiritStone(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            ItemStack held,
            FormationDiscBlockEntity disc
    ) {
        double accepted = disc.addEnergy(FormationTuning.SPIRIT_STONE_ENERGY_UNITS);
        if (accepted <= 0.0D) {
            actionbar(player, "message.ninecun_lingtai.formation.energy_full", "阵盘储灵已满。");
            return;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.2F);
        player.displayClientMessage(Component.translatableWithFallback(
                "message.ninecun_lingtai.formation.energy_charged",
                "灵石化作流光没入阵盘（储能 %s / %s）。",
                (int) disc.getStoredEnergy(),
                (int) FormationTuning.DISC_ENERGY_CAPACITY
        ), true);
    }

    // ==================== 换阵图 ====================

    private static void cycleFormation(ServerPlayer player, FormationDiscBlockEntity disc) {
        var next = FormationManager.INSTANCE.cycleNext(disc.getSelectedFormationId());
        if (next == null) {
            actionbar(player, "message.ninecun_lingtai.formation.no_definitions",
                    "识海中没有任何阵图（formations 数据包为空）。");
            return;
        }
        disc.setSelectedFormationId(next);
        FormationDefinition definition = FormationManager.INSTANCE.getDefinition(next);
        if (definition != null) {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.selected",
                    "阵盘载入阵图：%s（%s阶·%s旗）",
                    definition.name(),
                    definition.grade(),
                    definition.flagLayout().size()
            ), true);
        }
    }

    // ==================== 激活（一次性几何校验，施工图风险 5） ====================

    private static void tryActivate(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            FormationDiscBlockEntity disc
    ) {
        FormationDefinition definition =
                FormationManager.INSTANCE.getDefinition(disc.getSelectedFormationId());
        if (definition == null) {
            actionbar(player, "message.ninecun_lingtai.formation.no_selection",
                    "阵盘未载阵图——潜行右键择阵。");
            return;
        }
        if (!definition.type().isImplemented()) {
            actionbar(player, "message.ninecun_lingtai.formation.type_unimplemented",
                    "此类阵图的阵理尚未参透（后续版本开放）。");
            return;
        }
        if (definition.energyMode() == FormationEnergyMode.SIPHON) {
            actionbar(player, "message.ninecun_lingtai.formation.siphon_unavailable",
                    "汲灵式阵法尚未开放，请改用储能式阵图。");
            return;
        }
        CultivationStage stage = PlayerCultivationData.getStage(player).orElse(null);
        if (stage == null || stage.realm().ordinal() < definition.gradeRealm().ordinal()) {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.realm_too_low",
                    "境界不足以驾驭%s阶阵图（需%s及以上）。",
                    definition.grade(),
                    definition.gradeRealm().getChineseName()
            ), true);
            return;
        }
        CultivationStage minimum = definition.minimumCultivation().orElse(null);
        if (minimum != null && (stage.realm().ordinal() < minimum.realm().ordinal()
                || (stage.realm() == minimum.realm() && stage.layer() < minimum.layer()))) {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.cultivation_too_low",
                    "修为未至此阵门槛（需%s%s层）。",
                    minimum.realm().getChineseName(),
                    minimum.layer()
            ), true);
            return;
        }
        if (disc.getStoredEnergy() <= 0.0D) {
            actionbar(player, "message.ninecun_lingtai.formation.no_energy",
                    "阵盘空空如也——先以灵石充能。");
            return;
        }
        FormationGeometryUtility.ValidationResult result =
                FormationGeometryUtility.validate(level, pos, definition);
        if (!result.success()) {
            applyActivationBacklash(level, pos, player, definition, result);
            return;
        }
        FormationInstance instance = new FormationInstance(
                pos,
                definition.id(),
                player.getUUID(),
                player.getGameProfile().getName()
        );
        instance.setEfficiency(result.efficiency());
        for (FormationGeometryUtility.SlotResult slot : result.slots()) {
            instance.flagPositions().add(slot.actualPos());
            instance.flagElements().add(slot.element());
        }
        // 生门旗：合规旗位中随机指定，阵主自己也不知道（阵禁篇 7.2 天机不可尽算）
        instance.setLifeGateIndex(level.random.nextInt(instance.flagPositions().size()));
        FormationSavedData.get(level).putInstance(instance);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.3F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, 40, definition.radius() * 0.4D, 0.5D, definition.radius() * 0.4D, 0.02D);
        String sequenceNote = result.allGenerating() ? "，五行顺生 +20%"
                : result.anyOvercoming() ? "，旗序相克 −30%" : "";
        player.displayClientMessage(Component.translatableWithFallback(
                "message.ninecun_lingtai.formation.activated",
                "%s成阵！几何精度档 %s%%%s——合计效率 %s%%。",
                definition.name(),
                result.maxDeviation() <= FormationTuning.DEVIATION_GOOD ? 100 : 80,
                sequenceNote,
                Math.round(result.efficiency() * 100.0D)
        ), false);
    }

    /** 布阵失败反冲：旗位不合即灵力乱窜（M3 锁纹同款直接结算，风险服务无此类型不硬造）。 */
    private static void applyActivationBacklash(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            FormationDefinition definition,
            FormationGeometryUtility.ValidationResult result
    ) {
        PlayerSpiritualEnergyData.consumeEnergyUpTo(
                player,
                PlayerSpiritualEnergyData.getAllEnergy(player).keySet(),
                FormationTuning.ACTIVATION_BACKLASH_ENERGY_PER_GRADE * definition.grade()
        );
        player.hurt(level.damageSources().magic(), FormationTuning.ACTIVATION_BACKLASH_DAMAGE);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                SoundSource.BLOCKS, 1.0F, 0.6F);
        boolean missingFlag = result.slots().stream()
                .anyMatch(slot -> !slot.found());
        if (missingFlag) {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.activate_missing_flag",
                    "旗位残缺，灵机接不成环——反冲入体！按幻影指引补齐材料相合的阵旗。"
            ), false);
        } else {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.activate_backlash",
                    "旗位偏差 %s 格，超出两格容限——灵力反冲入体！",
                    String.format("%.1f", result.maxDeviation())
            ), false);
        }
    }

    // ==================== 点睛（地禁，占锚位，二次确认） ====================

    private static void tryEnlighten(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            FormationInstance instance
    ) {
        FormationDefinition definition =
                FormationManager.INSTANCE.getDefinition(instance.formationId());
        if (definition == null) {
            return;
        }
        int anchorCost = FormationAnchorLedger.getEnlightenAnchorCost(definition.grade());
        int used = SealAnchorUtility.getUsedAnchors(player.server, player.getUUID());
        int available = SealAnchorUtility.getAvailableAnchors(player);
        if (used + anchorCost > available) {
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.enlighten_no_anchor",
                    "神念锚位不足：点睛需 %s 枚（已用 %s / 可用 %s）。",
                    anchorCost, used, available
            ), true);
            return;
        }
        long gameTime = level.getGameTime();
        PendingEnlighten pending = PENDING_ENLIGHTEN.get(player.getUUID());
        if (pending == null || !pending.discPos().equals(pos)
                || gameTime > pending.deadlineGameTime()) {
            PENDING_ENLIGHTEN.put(player.getUUID(), new PendingEnlighten(
                    pos, gameTime + FormationTuning.ENLIGHTEN_CONFIRM_WINDOW_TICKS));
            player.displayClientMessage(Component.translatableWithFallback(
                    "message.ninecun_lingtai.formation.enlighten_confirm",
                    "点睛将长期占用 %s 枚神念锚位，明阵升为灵阵（白名单+警报）。五秒内再次潜行右键确认。",
                    anchorCost
            ), false);
            return;
        }
        PENDING_ENLIGHTEN.remove(player.getUUID());
        instance.setEnlightened(true);
        FormationSavedData.get(level).setDirty();
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatableWithFallback(
                "message.ninecun_lingtai.formation.enlightened",
                "一缕神念没入阵眼，明阵升为灵阵——外人踏入即有警觉。潜行右键域内道友可授白名单。"
        ), false);
    }

    /** 白名单增删：阵主在自家灵阵域内潜行空手右键目标玩家（FormationEventHandler 转入）。 */
    public static boolean toggleWhitelist(ServerPlayer owner, ServerPlayer target) {
        ServerLevel level = owner.serverLevel();
        for (FormationInstance instance : FormationSavedData.get(level).getAllInstances()) {
            if (!instance.ownerUuid().equals(owner.getUUID()) || !instance.isEnlightened()) {
                continue;
            }
            FormationDefinition definition =
                    FormationManager.INSTANCE.getDefinition(instance.formationId());
            if (definition == null
                    || !FormationRuntime.isInsideDomain(instance, definition, owner.position())
                    || !FormationRuntime.isInsideDomain(instance, definition, target.position())) {
                continue;
            }
            boolean added;
            if (instance.whitelist().remove(target.getUUID())) {
                added = false;
            } else {
                instance.whitelist().add(target.getUUID());
                added = true;
            }
            FormationSavedData.get(level).setDirty();
            owner.displayClientMessage(Component.translatableWithFallback(
                    added ? "message.ninecun_lingtai.formation.whitelist_added"
                            : "message.ninecun_lingtai.formation.whitelist_removed",
                    added ? "已授 %s 出入之权。" : "已收回 %s 的出入之权。",
                    target.getName().getString()
            ), true);
            target.displayClientMessage(Component.translatableWithFallback(
                    added ? "message.ninecun_lingtai.formation.whitelist_granted"
                            : "message.ninecun_lingtai.formation.whitelist_revoked",
                    added ? "%s 的大阵对你敞开了门户。" : "%s 的大阵收回了你的通行之权。",
                    owner.getName().getString()
            ), true);
            return true;
        }
        return false;
    }

    // ==================== 驱阵（阵禁篇 6.3：人在阵在） ====================

    private static void trySurge(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            FormationDiscBlockEntity disc,
            FormationInstance instance
    ) {
        FormationDefinition definition =
                FormationManager.INSTANCE.getDefinition(instance.formationId());
        if (definition == null) {
            return;
        }
        long gameTime = level.getGameTime();
        if (instance.isParalyzed(gameTime)) {
            actionbar(player, "message.ninecun_lingtai.formation.surge_paralyzed",
                    "阵法灵机紊乱，催动不得——先重整旗位。");
            return;
        }
        if (!FormationRuntime.isInsideDomain(instance, definition, player.position())) {
            actionbar(player, "message.ninecun_lingtai.formation.surge_outside",
                    "驱阵须身处阵域之内。");
            return;
        }
        if (instance.isSurgeOnCooldown(gameTime)) {
            actionbar(player, "message.ninecun_lingtai.formation.surge_cooldown",
                    "阵法灵机未复，稍候再催。");
            return;
        }
        if (PlayerSpiritualEnergyData.consumeEnergyAny(
                player, FormationTuning.SURGE_PLAYER_ENERGY_COST) == null) {
            actionbar(player, "message.ninecun_lingtai.formation.surge_no_energy",
                    "自身灵力不足，催不动这座阵。");
            return;
        }
        instance.setSurgeCooldownUntilGameTime(gameTime + FormationTuning.SURGE_COOLDOWN_TICKS);
        FormationSavedData.get(level).setDirty();
        switch (definition.type()) {
            case SPIRIT_GATHERING -> {
                SpiritualAuraManager.addReserve(level, pos,
                        FormationTuning.SURGE_GATHER_RESERVE_UNITS * instance.efficiency());
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D,
                        pos.getY() + 1.0D, pos.getZ() + 0.5D, 60,
                        definition.radius() * 0.5D, 1.0D, definition.radius() * 0.5D, 0.0D);
                actionbar(player, "message.ninecun_lingtai.formation.surge_gather",
                        "阵主亲催！四方灵气顺势灌入阵眼，短时暴涨。");
            }
            case SHIELD -> {
                disc.addEnergy(FormationTuning.SURGE_SHIELD_ENERGY_UNITS * instance.efficiency());
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5D,
                        pos.getY() + 1.0D, pos.getZ() + 0.5D, 80,
                        definition.radius() * 0.6D, definition.radius() * 0.4D,
                        definition.radius() * 0.6D, 0.01D);
                actionbar(player, "message.ninecun_lingtai.formation.surge_shield",
                        "阵主亲催！护罩灵光骤盛，凭空补上一层。");
            }
            default -> {
            }
        }
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.4F);
    }

    public static void onPlayerLoggedOut(ServerPlayer player) {
        PENDING_ENLIGHTEN.remove(player.getUUID());
    }

    public static void clear() {
        PENDING_ENLIGHTEN.clear();
    }

    private static void actionbar(ServerPlayer player, String key, String fallback) {
        player.displayClientMessage(Component.translatableWithFallback(key, fallback), true);
    }
}
