package net.seraphina.ninecun_lingtai.common.inner_demon;

import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.behavior.PlayerBehaviorLedgerData;
import net.seraphina.ninecun_lingtai.common.behavior.SpellUsageWindow;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.spell.attack.AttackSpellKind;
import net.seraphina.ninecun_lingtai.common.spell.attack.AttackSpellShape;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 不可变镜像快照（心魔即己 §6.1）。读锁定账本
 * （{@link PlayerBehaviorLedgerData#getLockedSnapshotOrLive}）生成，存进会话
 * SavedData——开战后临时改套路无效。
 *
 * <p>绝不复制物品/背包；外观只用 owner 皮肤 + 暗化（渲染层处理）。
 * 面板 = 玩家属性 × karmaScale，karmaScale = base + kills × perKill，
 * 带旧心障每条 +10%，整体 clamp ≤ cap（R9）。</p>
 */
public record MindDemonSnapshot(
        UUID ownerUuid,
        String ownerName,
        int schemaVersion,
        long seed,
        CultivationStage stage,
        int soulStars,
        ResourceLocation primarySpell,
        List<ResourceLocation> topSpells,
        CombatRole role,
        double karmaScale,
        List<MindDemonCounterMove> counterMoves,
        List<String> oldScarIds
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /** 带旧心障升级：心魔面板 +10%/条（§8），计入 cap（R9）。 */
    public static final double OLD_SCAR_SCALE_BONUS = 0.10D;

    /** mindDemon.karmaScaleBase（读配置，面板基准比例）。 */
    public static double karmaScaleBase() {
        return CommonConfig.MIND_DEMON_KARMA_SCALE_BASE.get();
    }

    /** mindDemon.karmaScaleCap（读配置，面板比例上限）。 */
    public static double karmaScaleCap() {
        return CommonConfig.MIND_DEMON_KARMA_SCALE_CAP.get();
    }

    private static final double DEFENSE_RATIO_THRESHOLD = 0.40D;
    private static final double MOBILITY_RATIO_THRESHOLD = 0.30D;
    private static final int SOUL_PILL_THRESHOLD = 5;
    private static final double GUARDIAN_RATIO_THRESHOLD = 0.15D;

    public enum CombatRole {
        RANGED,
        MELEE,
        SUMMONER,
        KITER
    }

    // ------------------------------------------------------------------ 生成

    /** 读玩家锁定账本生成快照（服务端，心魔开战时调用一次）。 */
    public static MindDemonSnapshot capture(ServerPlayer player) {
        CompoundTag ledger = PlayerBehaviorLedgerData.getLockedSnapshotOrLive(player);
        SpellUsageWindow window = PlayerBehaviorLedgerData.spellWindowFrom(ledger);
        List<ResourceLocation> topSpells = window.topSpells(3);
        List<String> oldScarIds = PlayerBehaviorLedgerData.activeScarIdsFrom(ledger).stream()
                .filter(id -> id.startsWith("mind_scar_"))
                .toList();

        int totalKills = PlayerBehaviorLedgerData.totalKillsFrom(ledger);
        double karmaScale = Mth.clamp(
                karmaScaleBase()
                        + totalKills * CommonConfig.MIND_DEMON_KARMA_SCALE_PER_KILL.get()
                        + oldScarIds.size() * OLD_SCAR_SCALE_BONUS,
                karmaScaleBase(),
                karmaScaleCap()
        );

        CultivationStage stage = PlayerCultivationData.getStage(player)
                .orElse(new CultivationStage(CultivationRealm.values()[0], 1));
        int soulStars = PlayerAptitudeData.getSoulStrength(player)
                .map(SoulStrength::illuminatedStars)
                .orElse(SoulStrength.MINIMUM_ILLUMINATED_STARS);

        return new MindDemonSnapshot(
                player.getUUID(),
                player.getGameProfile().getName(),
                CURRENT_SCHEMA_VERSION,
                player.getRandom().nextLong(),
                stage,
                soulStars,
                topSpells.isEmpty() ? null : topSpells.get(0),
                topSpells,
                inferRole(window, topSpells),
                karmaScale,
                selectCounterMoves(ledger, window, topSpells, oldScarIds),
                oldScarIds
        );
    }

    private static CombatRole inferRole(SpellUsageWindow window, List<ResourceLocation> topSpells) {
        if (window.usageRatio(MindDemonSnapshot::isMobilitySpell) > MOBILITY_RATIO_THRESHOLD) {
            return CombatRole.KITER;
        }
        if (window.usageRatio(MindDemonSnapshot::isGuardianSpell) > GUARDIAN_RATIO_THRESHOLD) {
            return CombatRole.SUMMONER;
        }
        if (!topSpells.isEmpty() && isStrikeSpell(topSpells.get(0))) {
            return CombatRole.MELEE;
        }
        return CombatRole.RANGED;
    }

    /** §6.3 触发条件表 + 带旧心障对应反制招必选；结果收拢到 2–3 个。 */
    private static List<MindDemonCounterMove> selectCounterMoves(
            CompoundTag ledger,
            SpellUsageWindow window,
            List<ResourceLocation> topSpells,
            List<String> oldScarIds
    ) {
        Set<MindDemonCounterMove> moves = new LinkedHashSet<>();
        for (String scarId : oldScarIds) {
            MindDemonCounterMove forced = forcedMoveForScar(scarId);
            if (forced != null) {
                moves.add(forced);
            }
        }
        if (window.usageRatio(MindDemonSnapshot::isDefenseSpell) > DEFENSE_RATIO_THRESHOLD) {
            moves.add(MindDemonCounterMove.SHIELD_BREAK);
        }
        if (window.usageRatio(MindDemonSnapshot::isMobilitySpell) > MOBILITY_RATIO_THRESHOLD) {
            moves.add(MindDemonCounterMove.AIR_LOCK);
        }
        if (PlayerBehaviorLedgerData.soulPillUsesFrom(ledger) > SOUL_PILL_THRESHOLD) {
            moves.add(MindDemonCounterMove.PILL_HAZE);
        }
        if (!topSpells.isEmpty() && isStrikeSpell(topSpells.get(0))) {
            moves.add(MindDemonCounterMove.THORN_MAIL);
        }
        if (window.usageRatio(MindDemonSnapshot::isGuardianSpell) > GUARDIAN_RATIO_THRESHOLD) {
            moves.add(MindDemonCounterMove.FOCUS_SUMMON);
        }
        // 不足 2 个时按默认序补齐；超过 3 个时裁掉后加入者。
        List<MindDemonCounterMove> selected = new ArrayList<>(moves);
        for (MindDemonCounterMove fallback : new MindDemonCounterMove[]{
                MindDemonCounterMove.THORN_MAIL,
                MindDemonCounterMove.SHIELD_BREAK
        }) {
            if (selected.size() >= 2) {
                break;
            }
            if (!selected.contains(fallback)) {
                selected.add(fallback);
            }
        }
        return List.copyOf(selected.size() > 3 ? selected.subList(0, 3) : selected);
    }

    private static MindDemonCounterMove forcedMoveForScar(String scarId) {
        return switch (scarId) {
            case PlayerBehaviorLedgerData.MindScarIds.THUNDER_FEAR -> MindDemonCounterMove.SHIELD_BREAK;
            case PlayerBehaviorLedgerData.MindScarIds.BLOODTHIRST -> MindDemonCounterMove.THORN_MAIL;
            case PlayerBehaviorLedgerData.MindScarIds.PILL_BOND -> MindDemonCounterMove.PILL_HAZE;
            case PlayerBehaviorLedgerData.MindScarIds.FLIGHT_GRIP -> MindDemonCounterMove.AIR_LOCK;
            default -> null;
        };
    }

    // ------------------------------------------------------------------ 法术分类（id 判断）

    public static boolean isDefenseSpell(ResourceLocation spellId) {
        String path = spellId.getPath();
        return path.endsWith("_body_shield")
                || path.equals("five_element_cycle")
                || path.equals("chaos_void")
                || path.equals("composite_body_shield")
                || isGuardianSpell(spellId);
    }

    public static boolean isGuardianSpell(ResourceLocation spellId) {
        return spellId.getPath().endsWith("_guardian");
    }

    public static boolean isMobilitySpell(ResourceLocation spellId) {
        String path = spellId.getPath();
        return path.equals("swift_wind")
                || path.equals("shrink_earth")
                || path.equals("sword_flight");
    }

    public static boolean isStrikeSpell(ResourceLocation spellId) {
        return AttackSpellKind.fromSerializedName(spellId.getPath())
                .map(kind -> kind.getShape() == AttackSpellShape.STRIKE)
                .orElse(false);
    }

    // ------------------------------------------------------------------ 序列化

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("OwnerUuid", this.ownerUuid);
        tag.putString("OwnerName", this.ownerName);
        tag.putInt("SchemaVersion", this.schemaVersion);
        tag.putLong("Seed", this.seed);
        tag.putString("Realm", this.stage.realm().getSerializedName());
        tag.putInt("Layer", this.stage.layer());
        tag.putInt("SoulStars", this.soulStars);
        if (this.primarySpell != null) {
            tag.putString("PrimarySpell", this.primarySpell.toString());
        }
        ListTag topSpellTags = new ListTag();
        this.topSpells.forEach(spellId -> topSpellTags.add(StringTag.valueOf(spellId.toString())));
        tag.put("TopSpells", topSpellTags);
        tag.putString("Role", this.role.name());
        tag.putDouble("KarmaScale", this.karmaScale);
        ListTag moveTags = new ListTag();
        this.counterMoves.forEach(move -> moveTags.add(StringTag.valueOf(move.name())));
        tag.put("CounterMoves", moveTags);
        ListTag scarTags = new ListTag();
        this.oldScarIds.forEach(scarId -> scarTags.add(StringTag.valueOf(scarId)));
        tag.put("OldScarIds", scarTags);
        return tag;
    }

    /** 非法数据返回 null（会话读取方按崩塌兜底处理）。 */
    public static MindDemonSnapshot fromNbt(CompoundTag tag) {
        if (!tag.hasUUID("OwnerUuid")) {
            return null;
        }
        CultivationRealm realm = CultivationRealm.fromSerializedName(tag.getString("Realm"))
                .orElse(null);
        if (realm == null) {
            return null;
        }
        int layer = Mth.clamp(
                tag.getInt("Layer"),
                CultivationStage.MINIMUM_LAYER,
                CultivationStage.MAXIMUM_LAYER
        );
        List<ResourceLocation> topSpells = new ArrayList<>();
        for (Tag spellTag : tag.getList("TopSpells", Tag.TAG_STRING)) {
            ResourceLocation spellId = ResourceLocation.tryParse(spellTag.getAsString());
            if (spellId != null) {
                topSpells.add(spellId);
            }
        }
        List<MindDemonCounterMove> counterMoves = new ArrayList<>();
        for (Tag moveTag : tag.getList("CounterMoves", Tag.TAG_STRING)) {
            MindDemonCounterMove move = MindDemonCounterMove.byName(moveTag.getAsString());
            if (move != null) {
                counterMoves.add(move);
            }
        }
        List<String> oldScarIds = new ArrayList<>();
        for (Tag scarTag : tag.getList("OldScarIds", Tag.TAG_STRING)) {
            oldScarIds.add(scarTag.getAsString());
        }
        CombatRole role;
        try {
            role = CombatRole.valueOf(tag.getString("Role"));
        } catch (IllegalArgumentException exception) {
            role = CombatRole.RANGED;
        }
        return new MindDemonSnapshot(
                tag.getUUID("OwnerUuid"),
                tag.getString("OwnerName"),
                tag.getInt("SchemaVersion"),
                tag.getLong("Seed"),
                new CultivationStage(realm, layer),
                tag.getInt("SoulStars"),
                ResourceLocation.tryParse(tag.getString("PrimarySpell")),
                List.copyOf(topSpells),
                role,
                Mth.clamp(tag.getDouble("KarmaScale"), karmaScaleBase(), karmaScaleCap()),
                List.copyOf(counterMoves),
                List.copyOf(oldScarIds)
        );
    }
}
