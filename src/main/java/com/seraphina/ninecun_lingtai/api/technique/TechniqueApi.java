package com.seraphina.ninecun_lingtai.api.technique;

import com.seraphina.ninecun_lingtai.common.ai.TechniqueComprehensionService;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import com.seraphina.ninecun_lingtai.utility.TechniqueUtility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class TechniqueApi {
    private TechniqueApi() {
    }

    public static Optional<TechniqueDefinition> getTechnique(ResourceLocation techniqueId) {
        return TechniqueManager.INSTANCE.getTechnique(techniqueId);
    }

    public static List<TechniqueDefinition> getTechniques() {
        return TechniqueManager.INSTANCE.getTechniques();
    }

    public static Map<ResourceLocation, TechniqueProgress> getLearnedTechniques(Player player) {
        return PlayerTechniqueData.getTechniques(player);
    }

    public static Optional<TechniqueProgress> getProgress(Player player, ResourceLocation techniqueId) {
        return PlayerTechniqueData.getTechnique(player, techniqueId);
    }

    public static TechniqueLearnability getLearnability(Player player, TechniqueDefinition technique) {
        return TechniqueUtility.getLearnability(player, technique);
    }

    public static CompletableFuture<TechniqueAssessment> evaluateUnderstanding(
            TechniqueDefinition technique,
            String playerUnderstanding
    ) {
        return TechniqueComprehensionService.evaluate(technique, playerUnderstanding);
    }

    public static TechniqueLearningResult applyAssessment(
            Player player,
            ResourceLocation techniqueId,
            TechniqueAssessment assessment
    ) {
        return PlayerTechniqueManager.applyAssessment(player, techniqueId, assessment);
    }
}
