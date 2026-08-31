package net.seraphina.ninecun_lingtai.common.formation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 阵法只读查询门面（给外部系统的集中接线点，M4 并发约束：不改
 * PlayerMeditationManager，由主控在其环境倍率计算处加一行乘入
 * {@link #getMeditationMultiplier}）。
 *
 * <p>说明：聚灵阵已经通过灵气账本（{@code SpiritualAuraManager.addReserve}）
 * 抬升阵眼灵域存量——打坐吸收系数随存量自然上浮；本乘区是"身处阵中"的
 * 直接加成，两者叠加即完整的位势体验。</p>
 */
public final class FormationApi {
    /** 建议键 formationMeditationBonusPerEfficiency：满效聚灵阵内打坐吸收 +30%。 */
    public static final double MEDITATION_BONUS_AT_FULL_EFFICIENCY = 0.3D;

    private FormationApi() {
    }

    /**
     * 某点位的打坐吸收乘区（≥1.0）：位于运转中的聚灵阵域内时
     * = 1 + 0.3 × 阵效率；多阵重叠取最高，不叠乘。
     */
    public static double getMeditationMultiplier(ServerLevel level, BlockPos position) {
        double best = 1.0D;
        long gameTime = level.getGameTime();
        Vec3 point = Vec3.atCenterOf(position);
        for (FormationInstance instance : FormationSavedData.get(level).getAllInstances()) {
            if (instance.isParalyzed(gameTime) || instance.paralyzedUntilGameTime() != 0L) {
                continue;
            }
            FormationDefinition definition =
                    FormationManager.INSTANCE.getDefinition(instance.formationId());
            if (definition == null
                    || definition.type() != FormationType.SPIRIT_GATHERING
                    || !FormationRuntime.isInsideDomain(instance, definition, point)) {
                continue;
            }
            best = Math.max(best,
                    1.0D + MEDITATION_BONUS_AT_FULL_EFFICIENCY * instance.efficiency());
        }
        return best;
    }

    /** 某点位是否处于运转中的护府阵护罩内（给其他系统预判用）。 */
    public static boolean isInsideActiveShield(ServerLevel level, Vec3 position) {
        long gameTime = level.getGameTime();
        for (FormationInstance instance : FormationSavedData.get(level).getAllInstances()) {
            if (instance.isParalyzed(gameTime) || instance.paralyzedUntilGameTime() != 0L) {
                continue;
            }
            FormationDefinition definition =
                    FormationManager.INSTANCE.getDefinition(instance.formationId());
            if (definition != null
                    && definition.type() == FormationType.SHIELD
                    && FormationRuntime.isInsideDomain(instance, definition, position)) {
                return true;
            }
        }
        return false;
    }
}
