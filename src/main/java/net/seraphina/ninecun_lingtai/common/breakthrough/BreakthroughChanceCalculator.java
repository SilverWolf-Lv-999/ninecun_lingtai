package net.seraphina.ninecun_lingtai.common.breakthrough;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.insight.InsightEffectUtility;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughBonusData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughFailureData;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 大境界破境成功率公式（纯函数，服务端调用）：
 * {@code clamp(基准 × 质量 × 道心 × 功法 + 丹药加成 + 瓶颈补偿, 0.05, 0.95)}。
 *
 * <p>掷骰只在服务端；{@link ChanceBreakdown} 明细只进日志与遥测，客户端只收
 * 把握档位键与（魂强 ≥ 5 星时）最拖后腿一项的修正 id（设计文档 R6）。</p>
 *
 * <p>八档基准与夹逼边界读 {@code CommonConfig.breakthrough} 组
 * （baseRateFoundation…baseRateImmortal / chanceFloor / chanceCeiling），
 * 运行时 get()，不做 static 缓存。</p>
 */
public final class BreakthroughChanceCalculator {

    private static final String GRASP_BUCKET_KEY_PREFIX = "grasp_";

    private static QiQualityProvider qiQualityProvider = QiQualityProvider.CONSTANT;
    private static DaoHeartProvider daoHeartProvider = DaoHeartProvider.CONSTANT;

    private BreakthroughChanceCalculator() {
    }

    /**
     * 成功率明细快照。
     *
     * @param finalChance    夹逼后的最终成功率
     * @param baseRate       八档基准
     * @param modifiers      各修正项（乘区系数或加算加数）
     * @param graspBucketKey 把握档位键 "grasp_1".."grasp_10"，客户端拼
     *                       message.ninecun_lingtai.breakthrough_grasp_N 显示
     */
    public record ChanceBreakdown(
            double finalChance,
            double baseRate,
            List<BreakthroughModifier> modifiers,
            String graspBucketKey
    ) {
        public ChanceBreakdown {
            modifiers = List.copyOf(modifiers);
        }

        /** 最拖后腿的修正项 id（乘区取系数最小者优先，其次缺失的加算项），无则 empty。 */
        public Optional<String> worstDraggingModifierId() {
            return this.modifiers.stream()
                    .filter(BreakthroughModifier::dragging)
                    .min(Comparator
                            .comparing(BreakthroughModifier::multiplicative, Comparator.reverseOrder())
                            .thenComparingDouble(BreakthroughModifier::value))
                    .map(BreakthroughModifier::id);
        }

        /** 单行日志文本，供掷骰落日志与 P8 遥测复用。 */
        public String describe() {
            StringBuilder text = new StringBuilder()
                    .append("finalChance=").append(String.format("%.4f", this.finalChance))
                    .append(", base=").append(String.format("%.2f", this.baseRate));
            for (BreakthroughModifier modifier : this.modifiers) {
                text.append(", ").append(modifier.id())
                        .append(modifier.multiplicative() ? "×" : "+")
                        .append(String.format("%.3f", modifier.value()));
            }
            return text.toString();
        }
    }

    public static ChanceBreakdown calculate(ServerPlayer player, CultivationStage target) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");
        long gameTime = player.serverLevel().getGameTime();

        double base = getBaseRate(target.realm());
        double quality = qiQualityProvider.getCoefficient(player);
        double daoHeart = daoHeartProvider.getCoefficient(player);
        double technique = getTechniqueGradeCoefficient(player);
        double pillBonus = PlayerBreakthroughBonusData.getSuccessBonus(player, gameTime);
        double pity = PlayerBreakthroughFailureData.getPityBonus(player, target.realm());
        double insight = InsightEffectUtility.query(
                player,
                InsightEffectUtility.EFFECT_BREAKTHROUGH_CHANCE_BONUS
        );

        List<BreakthroughModifier> modifiers = new ArrayList<>(6);
        modifiers.add(BreakthroughModifier.factor(BreakthroughModifier.ID_QUALITY, quality));
        modifiers.add(BreakthroughModifier.factor(BreakthroughModifier.ID_DAO_HEART, daoHeart));
        modifiers.add(BreakthroughModifier.factor(BreakthroughModifier.ID_TECHNIQUE, technique));
        modifiers.add(BreakthroughModifier.addend(BreakthroughModifier.ID_PILL, pillBonus, true));
        modifiers.add(BreakthroughModifier.addend(BreakthroughModifier.ID_PITY, pity, false));
        modifiers.add(BreakthroughModifier.addend(BreakthroughModifier.ID_INSIGHT, insight, false));

        double finalChance = clamp(base * quality * daoHeart * technique + pillBonus + pity + insight);
        return new ChanceBreakdown(finalChance, base, modifiers, getGraspBucketKey(finalChance));
    }

    /** 八档基准（读配置）；练气目标（凡人引气入体）不走掷骰，防御性返回 1.0。 */
    public static double getBaseRate(CultivationRealm targetRealm) {
        return switch (targetRealm) {
            case QI_REFINING -> 1.0D;
            case FOUNDATION_ESTABLISHMENT -> CommonConfig.BREAKTHROUGH_BASE_RATE_FOUNDATION.get();
            case GOLDEN_CORE -> CommonConfig.BREAKTHROUGH_BASE_RATE_GOLDEN_CORE.get();
            case NASCENT_SOUL -> CommonConfig.BREAKTHROUGH_BASE_RATE_NASCENT_SOUL.get();
            case SPIRIT_TRANSFORMATION -> CommonConfig.BREAKTHROUGH_BASE_RATE_SPIRIT_TRANSFORMATION.get();
            case DAO_INTEGRATION -> CommonConfig.BREAKTHROUGH_BASE_RATE_DAO_INTEGRATION.get();
            case VOID_REFINEMENT -> CommonConfig.BREAKTHROUGH_BASE_RATE_VOID_REFINEMENT.get();
            case MAHAYANA -> CommonConfig.BREAKTHROUGH_BASE_RATE_MAHAYANA.get();
            case IMMORTAL -> CommonConfig.BREAKTHROUGH_BASE_RATE_IMMORTAL.get();
        };
    }

    /** 把握档位键："grasp_" + round(chance×10)（收拢到 1..10）。 */
    public static String getGraspBucketKey(double finalChance) {
        int bucket = Math.max(1, Math.min(10, (int) Math.round(finalChance * 10.0D)));
        return GRASP_BUCKET_KEY_PREFIX + bucket;
    }

    /** M2 换真实现时替换（灵气生态）；仅供主控接线调用。 */
    public static void setQiQualityProvider(QiQualityProvider provider) {
        qiQualityProvider = Objects.requireNonNull(provider, "provider");
    }

    /** 道心/杀孽系统就绪后替换；仅供主控接线调用。 */
    public static void setDaoHeartProvider(DaoHeartProvider provider) {
        daoHeartProvider = Objects.requireNonNull(provider, "provider");
    }

    /** 功法品阶系数：M5 功法品阶落地前恒 1.0，接口先留。 */
    private static double getTechniqueGradeCoefficient(ServerPlayer player) {
        return 1.0D;
    }

    private static double clamp(double chance) {
        return Math.max(
                CommonConfig.BREAKTHROUGH_CHANCE_FLOOR.get(),
                Math.min(CommonConfig.BREAKTHROUGH_CHANCE_CEILING.get(), chance)
        );
    }
}
