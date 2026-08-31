package net.seraphina.ninecun_lingtai.common.insight;

import net.minecraft.world.entity.player.Player;

/**
 * 悟道印记效果的单点查询门面（R9）：各系统 hook 处一律经
 * {@code InsightEffectUtility.query(player, effectKey)} 读取，避免 if(hasInsight)
 * 散落全库。返回玩家所有已持有印记该效果键数值之和（无印记时 0）。
 */
public final class InsightEffectUtility {

    /** 破境成功率加算项（已接线：BreakthroughChanceCalculator）。 */
    public static final String EFFECT_BREAKTHROUGH_CHANCE_BONUS = "breakthrough_chance_bonus";
    /** 打坐吸收乘区加成（已接线：PlayerMeditationManager）。 */
    public static final String EFFECT_MEDITATION_ABSORPTION_BONUS = "meditation_absorption_bonus";
    /** 护体上限加成（已接线：EntityDefenseResourceData.getMaximum）。 */
    public static final String EFFECT_DEFENSE_CAPACITY_BONUS = "defense_capacity_bonus";
    /** 炼丹火候评分宽容加成（已接线：AlchemyFurnaceBlockEntity.finishRefining）。 */
    public static final String EFFECT_ALCHEMY_QUALITY_BONUS = "alchemy_quality_bonus";
    /** 攻击法术伤害加成（已接线：AttackSpellUtility.getScaledDamage 施法者重载）。 */
    public static final String EFFECT_ATTACK_SPELL_DAMAGE_BONUS = "attack_spell_damage_bonus";
    /** 神识范围加成（已接线：SoulSenseUtility.getRange）。 */
    public static final String EFFECT_SOUL_SENSE_RANGE_BONUS = "soul_sense_range_bonus";

    private InsightEffectUtility() {
    }

    /** 玩家全部已持有印记中 effectKey 的数值之和；无印记/无该效果返回 0。 */
    public static double query(Player player, String effectKey) {
        double total = 0.0D;
        for (var insightId : PlayerInsightData.getInsightIds(player)) {
            total += InsightPool.get(insightId)
                    .map(definition -> definition.getEffect(effectKey))
                    .orElse(0.0D);
        }
        return total;
    }
}
