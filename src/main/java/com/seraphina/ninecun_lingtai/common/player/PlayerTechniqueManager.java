package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueLearnability;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueLearningResult;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueLearningStatus;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.utility.TechniqueUtility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

public final class PlayerTechniqueManager {
    private PlayerTechniqueManager() {
    }

    public static TechniqueLearningResult applyAssessment(
            Player player,
            ResourceLocation techniqueId,
            TechniqueAssessment assessment
    ) {
        Optional<TechniqueDefinition> technique = TechniqueManager.INSTANCE.getTechnique(techniqueId);
        if (technique.isEmpty()) {
            return new TechniqueLearningResult(
                    TechniqueLearningStatus.TECHNIQUE_NOT_FOUND,
                    Optional.empty(),
                    List.of()
            );
        }

        TechniqueLearnability learnability = TechniqueUtility.getLearnability(player, technique.get());
        if (!learnability.canLearn()) {
            return new TechniqueLearningResult(
                    TechniqueLearningStatus.REQUIREMENTS_NOT_MET,
                    PlayerTechniqueData.getTechnique(player, techniqueId),
                    learnability.failures()
            );
        }

        Optional<ResourceLocation> mastery = technique.get().comprehension().getMastery(assessment.score());
        if (mastery.isEmpty()) {
            return new TechniqueLearningResult(
                    TechniqueLearningStatus.SCORE_TOO_LOW,
                    PlayerTechniqueData.getTechnique(player, techniqueId),
                    List.of()
            );
        }

        Optional<TechniqueProgress> existingProgress = PlayerTechniqueData.getTechnique(player, techniqueId);
        if (existingProgress.isPresent()
                && existingProgress.get().comprehensionScore() >= assessment.score()) {
            return new TechniqueLearningResult(
                    TechniqueLearningStatus.UNCHANGED,
                    existingProgress,
                    List.of()
            );
        }

        TechniqueProgress progress = new TechniqueProgress(
                assessment.score(),
                mastery.get(),
                assessment.feedback()
        );
        PlayerTechniqueData.setTechnique(player, techniqueId, progress);
        return new TechniqueLearningResult(
                existingProgress.isPresent()
                        ? TechniqueLearningStatus.IMPROVED
                        : TechniqueLearningStatus.LEARNED,
                Optional.of(progress),
                List.of()
        );
    }
}
