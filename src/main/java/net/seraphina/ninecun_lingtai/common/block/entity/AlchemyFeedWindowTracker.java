package net.seraphina.ninecun_lingtai.common.block.entity;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyFeedStep;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.utility.AlchemyProcessUtility;

/**
 * 分段投药窗口状态（自 {@link AlchemyFurnaceBlockEntity} 机械拆出，零行为变更）：
 * 持有五个 feed mask 与投药准头累计，窗口推进/投药记账/评分口径与拆分前逐位一致。
 * NBT 持久化仍走 {@link AlchemyFurnaceSessionState}（键名不变），本类只在内存态。
 */
final class AlchemyFeedWindowTracker {
    private int openMask;
    private int fedMask;
    private int missedMask;
    private int lateMask;
    private int plannedMask;
    private double scoreTotal;
    private int scoreCount;

    int getOpenMask() {
        return this.openMask;
    }

    int getFedMask() {
        return this.fedMask;
    }

    int getMissedMask() {
        return this.missedMask;
    }

    int getLateMask() {
        return this.lateMask;
    }

    int getPlannedMask() {
        return this.plannedMask;
    }

    double getScoreTotal() {
        return this.scoreTotal;
    }

    int getScoreCount() {
        return this.scoreCount;
    }

    // ---- ContainerData / NBT 回填口（客户端镜像与存档恢复用）----

    void setOpenMask(int openMask) {
        this.openMask = openMask;
    }

    void setFedMask(int fedMask) {
        this.fedMask = fedMask;
    }

    void setMissedMask(int missedMask) {
        this.missedMask = missedMask;
    }

    void setLateMask(int lateMask) {
        this.lateMask = lateMask;
    }

    void setPlannedMask(int plannedMask) {
        this.plannedMask = plannedMask;
    }

    void restoreScores(double scoreTotal, int scoreCount) {
        this.scoreTotal = scoreTotal;
        this.scoreCount = scoreCount;
    }

    void reset() {
        this.openMask = 0;
        this.fedMask = 0;
        this.missedMask = 0;
        this.lateMask = 0;
        this.scoreTotal = 0.0D;
        this.scoreCount = 0;
    }

    /** 每 tick 推进窗口：过期未投记 missed，开窗集合剔除已错过位。 */
    void updateWindowState(AlchemyRecipeDefinition recipe, int refiningTicks, int effectiveDuration) {
        this.plannedMask = AlchemyProcessUtility.getRequiredFeedMask(recipe.feedPlan());
        AlchemyProcessUtility.StageProgress stageProgress = AlchemyProcessUtility.locateStage(
                recipe.heatStages(),
                refiningTicks / (double) Math.max(1, effectiveDuration)
        );
        AlchemyProcessUtility.FeedWindowState windowState = AlchemyProcessUtility.getFeedWindowState(
                recipe.feedPlan(),
                this.fedMask,
                stageProgress
        );
        this.missedMask |= windowState.missedMask();
        this.openMask = windowState.openMask() & ~this.missedMask;
    }

    /** 该槽当前是否可投（窗口开着、未投过、未错过）。 */
    boolean canFeed(int slotMask) {
        return (this.openMask & slotMask) != 0
                && (this.fedMask & slotMask) == 0
                && (this.missedMask & slotMask) == 0;
    }

    /** 投药记账：准头计分，窗口尾段之后投的记迟投。 */
    void recordFeed(int slotMask, double accuracy, AlchemyFeedStep step, AlchemyProcessUtility.StageProgress stageProgress) {
        this.scoreTotal += accuracy;
        this.scoreCount++;
        this.fedMask |= slotMask;
        this.openMask &= ~slotMask;
        if (stageProgress.progressWithinStage() > step.windowEnd()) {
            this.lateMask |= slotMask;
        }
    }

    /** 投药评分：无投药丹方恒 1；有漏投归零；否则平均准头 clamp 到 [0,1]。 */
    double getFeedScore(AlchemyRecipeDefinition recipe) {
        if (recipe.feedPlan().isEmpty()) {
            return 1.0D;
        }
        if (hasMissingFeed(recipe)) {
            return 0.0D;
        }
        return Math.max(
                0.0D,
                Math.min(1.0D, this.scoreTotal / Math.max(1, recipe.feedPlan().size()))
        );
    }

    boolean hasMissingFeed(AlchemyRecipeDefinition recipe) {
        int requiredMask = AlchemyProcessUtility.getRequiredFeedMask(recipe.feedPlan());
        return (this.fedMask & requiredMask) != requiredMask
                || (this.missedMask & requiredMask) != 0;
    }
}
