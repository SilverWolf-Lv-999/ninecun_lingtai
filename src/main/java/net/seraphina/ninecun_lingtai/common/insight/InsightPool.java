package net.seraphina.ninecun_lingtai.common.insight;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenInsightSelectionPacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 悟道印记内置注册表 + 三选一抽取/选定（T14）。M1 首发 6 枚（金丹 3 + 元婴 3），
 * 代码内注册，datapack 化留 M5。
 *
 * <p>抽取加权：M1 只按灵根亲和元素加权（含亲和元素权重 ×2）；主修功法与行为账本
 * 加权留 TODO。选定走服务端校验：只接受当前未过期候选集内的 id（防伪造包）。</p>
 */
public final class InsightPool {

    private static final int CANDIDATE_COUNT = 3;
    private static final double AFFINITY_WEIGHT_MULTIPLIER = 2.0D;

    /**
     * M1 首发 6 枚，效果 hook 全部接线完毕：dao_clarity（BreakthroughChanceCalculator）、
     * enduring_breath（PlayerMeditationManager）、stable_as_bedrock（EntityDefenseResourceData）、
     * pill_fire_mastery（AlchemyFurnaceBlockEntity 火候评分）、sword_heart（AttackSpellUtility）、
     * soul_expanse（SoulSenseUtility.getRange）。
     */
    public static final List<InsightDefinition> BUILTIN = List.of(
            new InsightDefinition(
                    id("golden_core/stable_as_bedrock"),
                    CultivationRealm.GOLDEN_CORE,
                    InsightDefinition.Category.SURVIVAL,
                    InsightDefinition.Tier.ENTRY,
                    ElementType.EARTH,
                    // 已接线：EntityDefenseResourceData.getMaximum 护体上限乘区。
                    Map.of(InsightEffectUtility.EFFECT_DEFENSE_CAPACITY_BONUS, 0.10D)
            ),
            new InsightDefinition(
                    id("golden_core/pill_fire_mastery"),
                    CultivationRealm.GOLDEN_CORE,
                    InsightDefinition.Category.ALCHEMY,
                    InsightDefinition.Tier.ENTRY,
                    ElementType.FIRE,
                    // 已接线：AlchemyFurnaceBlockEntity.finishRefining 火候评分宽容乘区。
                    Map.of(InsightEffectUtility.EFFECT_ALCHEMY_QUALITY_BONUS, 0.05D)
            ),
            new InsightDefinition(
                    id("golden_core/dao_clarity"),
                    CultivationRealm.GOLDEN_CORE,
                    InsightDefinition.Category.SURVIVAL,
                    InsightDefinition.Tier.ENTRY,
                    null,
                    // 已接线：BreakthroughChanceCalculator 的 insight 加算项。
                    Map.of(InsightEffectUtility.EFFECT_BREAKTHROUGH_CHANCE_BONUS, 0.03D)
            ),
            new InsightDefinition(
                    id("nascent_soul/enduring_breath"),
                    CultivationRealm.NASCENT_SOUL,
                    InsightDefinition.Category.SURVIVAL,
                    InsightDefinition.Tier.ADEPT,
                    ElementType.WOOD,
                    // 已接线：PlayerMeditationManager 打坐吸收乘区。
                    Map.of(InsightEffectUtility.EFFECT_MEDITATION_ABSORPTION_BONUS, 0.05D)
            ),
            new InsightDefinition(
                    id("nascent_soul/sword_heart"),
                    CultivationRealm.NASCENT_SOUL,
                    InsightDefinition.Category.COMBAT,
                    InsightDefinition.Tier.ADEPT,
                    ElementType.METAL,
                    // 已接线：AttackSpellUtility.getScaledDamage 施法者重载（仅玩家生效）。
                    Map.of(InsightEffectUtility.EFFECT_ATTACK_SPELL_DAMAGE_BONUS, 0.05D)
            ),
            new InsightDefinition(
                    id("nascent_soul/soul_expanse"),
                    CultivationRealm.NASCENT_SOUL,
                    InsightDefinition.Category.SURVIVAL,
                    InsightDefinition.Tier.ADEPT,
                    ElementType.WATER,
                    // 已接线：SoulSenseUtility.getRange 尾乘区（与魂伤乘区同点）。
                    Map.of(InsightEffectUtility.EFFECT_SOUL_SENSE_RANGE_BONUS, 0.20D)
            )
    );

    private static final Map<ResourceLocation, InsightDefinition> BY_ID = BUILTIN.stream()
            .collect(Collectors.toUnmodifiableMap(InsightDefinition::id, Function.identity()));

    private InsightPool() {
    }

    public static Optional<InsightDefinition> get(ResourceLocation insightId) {
        return Optional.ofNullable(BY_ID.get(insightId));
    }

    /**
     * 破境成功后掉落三选一候选：抽取 → 写入待选 NBT → 发包打开三选一界面。
     */
    public static void offerCandidates(ServerPlayer player, CultivationRealm realm) {
        long gameTime = player.serverLevel().getGameTime();
        long expireTicks = CommonConfig.BREAKTHROUGH_INSIGHT_CHOICE_EXPIRE_TICKS.get();
        List<InsightDefinition> candidates = rollThree(player, player.serverLevel().random, realm);
        if (candidates.isEmpty()) {
            return;
        }
        PlayerInsightData.setPendingCandidates(
                player,
                candidates.stream().map(InsightDefinition::id).toList(),
                gameTime + expireTicks
        );
        ModMain.LOGGER.info(
                "[insight] offered {} candidates to {}: {}",
                candidates.size(),
                player.getName().getString(),
                candidates.stream().map(candidate -> candidate.id().toString()).toList()
        );
        ModNetwork.sendOpenInsightSelection(player, new OpenInsightSelectionPacket(
                candidates.stream().map(InsightDefinition::id).toList(),
                gameTime + expireTicks
        ));
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.insight_selection_title",
                        "悟道印记 · 三择其一，落子无悔"
                ),
                false
        );
    }

    /**
     * 按境界抽取至多 3 枚候选（排除已持有）。M1 加权：灵根含亲和元素 ×2；
     * TODO: 主修功法与行为账本类别加权（行为账本 M1 未上线）。
     */
    public static List<InsightDefinition> rollThree(
            ServerPlayer player,
            RandomSource random,
            CultivationRealm realm
    ) {
        Set<ElementType> rootElements = PlayerAptitudeData.getSpiritualRoot(player)
                .map(SpiritualRootUtility::getIndependentRootElements)
                .orElse(Set.of());
        List<InsightDefinition> available = new ArrayList<>();
        for (InsightDefinition definition : BUILTIN) {
            if (definition.realm() == realm && !PlayerInsightData.hasInsight(player, definition.id())) {
                available.add(definition);
            }
        }
        List<InsightDefinition> picked = new ArrayList<>(CANDIDATE_COUNT);
        while (picked.size() < CANDIDATE_COUNT && !available.isEmpty()) {
            picked.add(pickWeighted(available, rootElements, random));
        }
        return picked;
    }

    /**
     * 三选一落子（SelectInsightPacket 的服务端入口）：只接受未过期候选集内的 id。
     *
     * @return 是否选定成功
     */
    public static boolean select(ServerPlayer player, ResourceLocation insightId) {
        long gameTime = player.serverLevel().getGameTime();
        List<ResourceLocation> candidates = PlayerInsightData.getPendingCandidates(player, gameTime);
        if (!candidates.contains(insightId)) {
            ModMain.LOGGER.warn(
                    "[insight] {} tried to select {} outside candidate set {}",
                    player.getName().getString(),
                    insightId,
                    candidates
            );
            return false;
        }
        InsightDefinition definition = BY_ID.get(insightId);
        if (definition == null) {
            return false;
        }
        PlayerInsightData.addInsight(player, insightId, definition.tier(), gameTime);
        PlayerInsightData.clearPendingCandidates(player);
        player.displayClientMessage(
                Component.translatable(definition.getTranslationKeyBase() + ".name"),
                false
        );
        ModMain.LOGGER.info(
                "[insight] {} selected {}",
                player.getName().getString(),
                insightId
        );
        return true;
    }

    private static InsightDefinition pickWeighted(
            List<InsightDefinition> available,
            Set<ElementType> rootElements,
            RandomSource random
    ) {
        double totalWeight = 0.0D;
        double[] weights = new double[available.size()];
        for (int index = 0; index < available.size(); index++) {
            InsightDefinition definition = available.get(index);
            double weight = definition.affinityElement() != null
                    && rootElements.contains(definition.affinityElement())
                    ? AFFINITY_WEIGHT_MULTIPLIER
                    : 1.0D;
            weights[index] = weight;
            totalWeight += weight;
        }
        double rolled = random.nextDouble() * totalWeight;
        for (int index = 0; index < available.size(); index++) {
            rolled -= weights[index];
            if (rolled <= 0.0D) {
                return available.remove(index);
            }
        }
        return available.remove(available.size() - 1);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, path);
    }
}
