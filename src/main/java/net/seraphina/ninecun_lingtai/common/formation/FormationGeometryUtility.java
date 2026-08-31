package net.seraphina.ninecun_lingtai.common.formation;

import net.seraphina.ninecun_lingtai.api.element.ElementRelation;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.block.FormationFlagBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 布阵几何校验（任务口径：否决 0.5 格亚方块精度，按方位角/距离/高度做点位校验）。
 * <b>只在激活（与瘫痪期满复核）时调用一次</b>，运转期不重复扫方块（施工图风险 5）。
 *
 * <p>效率档：全旗位最大偏差 ≤1 格 → 100%；≤2 格 → 80%；超限或缺旗 → 失败反冲。
 * 五行旗序（沿阵图顺位环形相邻）：全部相生 +20%；出现相克 −30%。</p>
 */
public final class FormationGeometryUtility {

    private FormationGeometryUtility() {
    }

    /** 单旗位校验结果；actualPos 为空 = 搜索半径内没有材料匹配的旗。 */
    public record SlotResult(
            BlockPos expectedPos,
            @Nullable BlockPos actualPos,
            @Nullable ElementType element,
            double deviation
    ) {
        public boolean found() {
            return this.actualPos != null;
        }
    }

    public record ValidationResult(
            boolean success,
            double maxDeviation,
            double efficiency,
            boolean allGenerating,
            boolean anyOvercoming,
            List<SlotResult> slots
    ) {
    }

    /** 旗位期望点（方位角 0 = 北、顺时针；与 SealManager.computeOctant 同一罗盘口径）。 */
    public static Vec3 getExpectedPoint(BlockPos discPos, FormationDefinition.FlagSlot slot) {
        double radians = Math.toRadians(slot.angleDegrees());
        return new Vec3(
                discPos.getX() + 0.5D + Math.sin(radians) * slot.distance(),
                discPos.getY(),
                discPos.getZ() + 0.5D - Math.cos(radians) * slot.distance()
        );
    }

    public static ValidationResult validate(
            ServerLevel level,
            BlockPos discPos,
            FormationDefinition definition
    ) {
        List<SlotResult> slots = new ArrayList<>();
        Set<BlockPos> claimedFlags = new HashSet<>();
        double maxDeviation = 0.0D;
        boolean allFound = true;
        for (FormationDefinition.FlagSlot slot : definition.flagLayout()) {
            SlotResult result = locateFlag(level, discPos, slot, claimedFlags);
            slots.add(result);
            if (!result.found()) {
                allFound = false;
                continue;
            }
            claimedFlags.add(result.actualPos());
            maxDeviation = Math.max(maxDeviation, result.deviation());
        }

        boolean allGenerating = false;
        boolean anyOvercoming = false;
        if (allFound && slots.size() >= 2) {
            allGenerating = true;
            for (int index = 0; index < slots.size(); index++) {
                ElementType current = slots.get(index).element();
                ElementType next = slots.get((index + 1) % slots.size()).element();
                ElementRelation relation = current.getRelationTo(next);
                if (relation != ElementRelation.GENERATES) {
                    allGenerating = false;
                }
                if (relation == ElementRelation.OVERCOMES || relation == ElementRelation.OVERCOME_BY) {
                    anyOvercoming = true;
                }
            }
        }

        if (!allFound || maxDeviation > FormationTuning.DEVIATION_LIMIT) {
            return new ValidationResult(false, maxDeviation, 0.0D, allGenerating, anyOvercoming, slots);
        }
        double efficiency = maxDeviation <= FormationTuning.DEVIATION_GOOD
                ? FormationTuning.EFFICIENCY_GOOD
                : FormationTuning.EFFICIENCY_COARSE;
        if (allGenerating) {
            efficiency *= 1.0D + FormationTuning.FLAG_SEQUENCE_SHENG_BONUS;
        } else if (anyOvercoming) {
            efficiency *= 1.0D - FormationTuning.FLAG_SEQUENCE_KE_PENALTY;
        }
        return new ValidationResult(true, maxDeviation, efficiency, allGenerating, anyOvercoming, slots);
    }

    /**
     * 在期望点立方邻域内找最近的材料匹配阵旗（一旗不得同时充任两个旗位）。
     * 偏差 = 旗方块中柱点到期望点的三维距离（高度偏差同权计入）。
     */
    private static SlotResult locateFlag(
            ServerLevel level,
            BlockPos discPos,
            FormationDefinition.FlagSlot slot,
            Set<BlockPos> claimedFlags
    ) {
        Vec3 expectedPoint = getExpectedPoint(discPos, slot);
        BlockPos expectedPos = BlockPos.containing(expectedPoint.x, expectedPoint.y, expectedPoint.z);
        int searchRadius = FormationTuning.FLAG_SEARCH_RADIUS;
        BlockPos bestPos = null;
        ElementType bestElement = null;
        double bestDeviation = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    cursor.setWithOffset(expectedPos, dx, dy, dz);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof FormationFlagBlock flagBlock)
                            || !state.is(slot.materialTag())
                            || claimedFlags.contains(cursor)) {
                        continue;
                    }
                    double deviation = expectedPoint.distanceTo(new Vec3(
                            cursor.getX() + 0.5D, cursor.getY(), cursor.getZ() + 0.5D));
                    if (deviation < bestDeviation) {
                        bestDeviation = deviation;
                        bestPos = cursor.immutable();
                        bestElement = flagBlock.element();
                    }
                }
            }
        }
        if (bestPos == null) {
            return new SlotResult(expectedPos, null, null, Double.MAX_VALUE);
        }
        return new SlotResult(expectedPos, bestPos, bestElement, bestDeviation);
    }
}
