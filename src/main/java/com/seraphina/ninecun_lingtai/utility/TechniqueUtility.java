package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueLearnability;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueRequirementFailure;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueRequirementType;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueRequirements;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TechniqueUtility {
    private TechniqueUtility() {
    }

    public static TechniqueLearnability getLearnability(Player player, TechniqueDefinition technique) {
        List<TechniqueRequirementFailure> failures = new ArrayList<>();
        TechniqueRequirements requirements = technique.requirements();
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        boolean requiresSpiritualRoot = !technique.compatibleElements().isEmpty()
                || !requirements.requiredElements().isEmpty();
        if (requiresSpiritualRoot && spiritualRoot.isEmpty()) {
            failures.add(new TechniqueRequirementFailure(
                    TechniqueRequirementType.SPIRITUAL_ROOT,
                    "missing"
            ));
        } else if (spiritualRoot.isPresent() && !spiritualRoot.get().isChaos()) {
            checkCompatibleElements(spiritualRoot.get(), technique, failures);
            checkRequiredElements(spiritualRoot.get(), requirements, failures);
        }

        requirements.minimumCultivation().ifPresent(minimumStage -> {
            Optional<CultivationStage> currentStage = PlayerCultivationData.getStage(player);
            if (currentStage.isEmpty()
                    || CultivationUtility.getProgressionIndex(currentStage.get())
                    < CultivationUtility.getProgressionIndex(minimumStage)) {
                failures.add(new TechniqueRequirementFailure(
                        TechniqueRequirementType.CULTIVATION_STAGE,
                        minimumStage.realm().getSerializedName() + ":" + minimumStage.layer()
                ));
            }
        });

        if (requirements.minimumSoulStrength() > 0
                && PlayerAptitudeData.getSoulStrength(player)
                .map(soulStrength -> soulStrength.illuminatedStars() < requirements.minimumSoulStrength())
                .orElse(true)) {
            failures.add(new TechniqueRequirementFailure(
                    TechniqueRequirementType.SOUL_STRENGTH,
                    Integer.toString(requirements.minimumSoulStrength())
            ));
        }

        for (ResourceLocation requiredTechnique : requirements.requiredTechniques()) {
            if (!PlayerTechniqueData.hasTechnique(player, requiredTechnique)) {
                failures.add(new TechniqueRequirementFailure(
                        TechniqueRequirementType.PREREQUISITE_TECHNIQUE,
                        requiredTechnique.toString()
                ));
            }
        }
        return new TechniqueLearnability(failures);
    }

    public static List<TechniqueDefinition> getImprintableTechniques(Player player) {
        Set<ResourceLocation> techniqueIds = new HashSet<>(
                PlayerTechniqueData.getTechniques(player).keySet()
        );
        techniqueIds.addAll(PlayerImprintedTechniqueData.getTechniques(player));
        return techniqueIds.stream()
                .map(TechniqueManager.INSTANCE::getTechnique)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(TechniqueDefinition::name)
                        .thenComparing(technique -> technique.id().toString()))
                .toList();
    }

    public static boolean canImprintToJadeSlip(Player player, ResourceLocation techniqueId) {
        return PlayerTechniqueData.hasTechnique(player, techniqueId)
                || PlayerImprintedTechniqueData.hasTechnique(player, techniqueId);
    }

    private static void checkCompatibleElements(
            SpiritualRoot spiritualRoot,
            TechniqueDefinition technique,
            List<TechniqueRequirementFailure> failures
    ) {
        if (technique.compatibleElements().isEmpty()) {
            return;
        }
        boolean hasCompatibleElement = spiritualRoot.elements().stream()
                .anyMatch(technique.compatibleElements()::contains);
        if (!hasCompatibleElement) {
            failures.add(new TechniqueRequirementFailure(
                    TechniqueRequirementType.COMPATIBLE_ELEMENT,
                    joinElements(technique.compatibleElements())
            ));
        }
    }

    private static void checkRequiredElements(
            SpiritualRoot spiritualRoot,
            TechniqueRequirements requirements,
            List<TechniqueRequirementFailure> failures
    ) {
        for (ElementType requiredElement : requirements.requiredElements()) {
            if (!spiritualRoot.elements().contains(requiredElement)) {
                failures.add(new TechniqueRequirementFailure(
                        TechniqueRequirementType.REQUIRED_ELEMENT,
                        requiredElement.getSerializedName()
                ));
            }
        }
    }

    private static String joinElements(Iterable<ElementType> elements) {
        List<String> elementNames = new ArrayList<>();
        for (ElementType element : elements) {
            elementNames.add(element.getSerializedName());
        }
        elementNames.sort(String::compareTo);
        return String.join(",", elementNames);
    }
}
