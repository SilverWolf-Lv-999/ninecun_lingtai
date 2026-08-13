package net.seraphina.ninecun_lingtai.api.alchemy;

import java.util.List;
import java.util.Objects;

public record HeatStage(HeatBand band, double portion) {
    public HeatStage {
        Objects.requireNonNull(band, "band");
        if (portion <= 0.0D || !Double.isFinite(portion)) {
            throw new IllegalArgumentException("Heat stage portion must be a positive finite number");
        }
    }

    public static HeatBand getTargetBand(List<HeatStage> stages, double progress) {
        if (stages.isEmpty()) {
            return HeatBand.MEDIUM;
        }
        double totalPortion = stages.stream().mapToDouble(HeatStage::portion).sum();
        double clampedProgress = Math.max(0.0D, Math.min(1.0D, progress));
        double accumulated = 0.0D;
        for (HeatStage stage : stages) {
            accumulated += stage.portion() / totalPortion;
            if (clampedProgress <= accumulated) {
                return stage.band();
            }
        }
        return stages.get(stages.size() - 1).band();
    }

    public static int getStageIndex(List<HeatStage> stages, double progress) {
        if (stages.isEmpty()) {
            return 0;
        }
        double totalPortion = stages.stream().mapToDouble(HeatStage::portion).sum();
        double clampedProgress = Math.max(0.0D, Math.min(1.0D, progress));
        double accumulated = 0.0D;
        for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
            accumulated += stages.get(stageIndex).portion() / totalPortion;
            if (clampedProgress <= accumulated) {
                return stageIndex;
            }
        }
        return stages.size() - 1;
    }
}
