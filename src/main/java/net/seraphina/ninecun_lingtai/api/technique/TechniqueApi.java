package net.seraphina.ninecun_lingtai.api.technique;

import net.seraphina.ninecun_lingtai.common.ai.TechniqueComprehensionService;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import net.seraphina.ninecun_lingtai.utility.TechniqueUtility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    public static Optional<ItemStack> createTechniqueItem(ResourceLocation techniqueId) {
        return getTechnique(techniqueId).map(TechniqueItemUtility::createStack);
    }

    public static Set<ResourceLocation> getImprintedTechniques(Player player) {
        return PlayerImprintedTechniqueData.getTechniques(player);
    }

    public static boolean isImprinted(Player player, ResourceLocation techniqueId) {
        return PlayerImprintedTechniqueData.hasTechnique(player, techniqueId);
    }

    public static Map<ResourceLocation, TechniqueProgress> getLearnedTechniques(Player player) {
        return PlayerTechniqueData.getTechniques(player);
    }

    public static Optional<TechniqueProgress> getProgress(Player player, ResourceLocation techniqueId) {
        return PlayerTechniqueData.getTechnique(player, techniqueId);
    }

    public static long getProficiency(Player player, ResourceLocation techniqueId) {
        return PlayerTechniqueProficiencyData.get(player, techniqueId);
    }

    public static Map<ResourceLocation, Long> getProficiencies(Player player) {
        return PlayerTechniqueProficiencyData.getAll(player);
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
