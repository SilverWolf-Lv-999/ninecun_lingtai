package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.api.technique.TechniqueAssessment;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueLearnability;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueLearningResult;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueLearningStatus;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.utility.TechniqueUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

public final class PlayerTechniqueManager {
    private PlayerTechniqueManager() {
    }

    /**
     * Adds training proficiency to a learned technique and advances its mastery stage when a
     * threshold is crossed, announcing the advancement and re-syncing the technique library.
     * Returns the total proficiency after the addition.
     */
    public static long addProficiency(ServerPlayer player, ResourceLocation techniqueId, long gain) {
        long total = PlayerTechniqueProficiencyData.add(player, techniqueId, gain);
        advanceMasteryStage(player, techniqueId, total);
        return total;
    }

    private static void advanceMasteryStage(
            ServerPlayer player,
            ResourceLocation techniqueId,
            long totalProficiency
    ) {
        Optional<TechniqueProgress> progress = PlayerTechniqueData.getTechnique(player, techniqueId);
        if (progress.isEmpty()) {
            return;
        }
        TechniqueMasteryStage targetStage = TechniqueMasteryStage.getStageForProficiency(totalProficiency);
        int currentRank = TechniqueMasteryStage.fromMastery(progress.get().mastery())
                .map(Enum::ordinal)
                .orElse(TechniqueMasteryStage.ENTRY.ordinal());
        if (targetStage.ordinal() <= currentRank) {
            return;
        }
        PlayerTechniqueData.setTechnique(player, techniqueId, new TechniqueProgress(
                progress.get().comprehensionScore(),
                targetStage.getMasteryId(),
                progress.get().feedback()
        ));
        player.displayClientMessage(
                Component.translatable(
                        "message.ninecun_lingtai.technique_stage_advanced",
                        getTechniqueName(techniqueId),
                        targetStage.getChineseName()
                ),
                false
        );
        ModNetwork.sendTechniqueLibrary(player);
    }

    private static String getTechniqueName(ResourceLocation techniqueId) {
        return TechniqueManager.INSTANCE.getTechnique(techniqueId)
                .map(TechniqueDefinition::name)
                .orElse(techniqueId.toString());
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

        // A stage already reached through cultivation is never lost to a new assessment.
        ResourceLocation resultMastery = existingProgress
                .map(progress -> TechniqueMasteryStage.highest(progress.mastery(), mastery.get()))
                .orElse(mastery.get());
        TechniqueProgress progress = new TechniqueProgress(
                assessment.score(),
                resultMastery,
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
