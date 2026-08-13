package net.seraphina.ninecun_lingtai.api.alchemy;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record AlchemyRecipeDefinition(
        ResourceLocation id,
        String name,
        String description,
        String content,
        AlchemyGrade grade,
        Set<CultivationRealm> allowedRealms,
        CultivationRealm minimumRefiningRealm,
        AlchemyIngredient primaryIngredient,
        List<AlchemyIngredient> secondaryIngredients,
        List<AlchemyIngredient> auxiliaryIngredients,
        ResourceLocation outputItem,
        int outputCount,
        int brewingDuration,
        @Nullable ElementType primaryElement,
        double baseSuccessRate,
        List<HeatStage> heatStages,
        List<AlchemyFeedStep> feedPlan,
        double pillToxin,
        double failureExplosionRadius,
        int failureDregCount,
        List<AlchemyPillEffectData> effects
) {
    public static final double DEFAULT_BASE_SUCCESS_RATE = 0.90D;
    public static final double DEFAULT_EXPLOSION_RADIUS = 1.5D;
    public static final int DEFAULT_DREG_COUNT = 2;

    public AlchemyRecipeDefinition {
        Objects.requireNonNull(id, "id");
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        content = requireText(content, "content");
        Objects.requireNonNull(grade, "grade");
        allowedRealms = Set.copyOf(allowedRealms);
        if (allowedRealms.isEmpty()) {
            throw new IllegalArgumentException("Alchemy recipe must allow at least one cultivation realm");
        }
        Objects.requireNonNull(minimumRefiningRealm, "minimumRefiningRealm");
        Objects.requireNonNull(primaryIngredient, "primaryIngredient");
        secondaryIngredients = List.copyOf(secondaryIngredients);
        if (secondaryIngredients.size() != 2) {
            throw new IllegalArgumentException("Alchemy recipe must define exactly two secondary ingredients");
        }
        auxiliaryIngredients = List.copyOf(auxiliaryIngredients);
        if (auxiliaryIngredients.size() != 2) {
            throw new IllegalArgumentException("Alchemy recipe must define exactly two auxiliary ingredients");
        }
        Objects.requireNonNull(outputItem, "outputItem");
        if (outputCount < 1 || outputCount > 64) {
            throw new IllegalArgumentException("Alchemy output count must be between one and 64");
        }
        if (brewingDuration < 20 || brewingDuration > 72000) {
            throw new IllegalArgumentException("Alchemy brewing duration must be between 20 and 72000 ticks");
        }
        if (baseSuccessRate <= 0.0D || baseSuccessRate > 1.0D) {
            throw new IllegalArgumentException("Alchemy base success rate must be within (0, 1]");
        }
        heatStages = List.copyOf(heatStages);
        feedPlan = List.copyOf(feedPlan);
        validateFeedPlan(feedPlan, heatStages);
        if (pillToxin < 0.0D || !Double.isFinite(pillToxin)) {
            throw new IllegalArgumentException("Alchemy pill toxin must be a non-negative finite number");
        }
        if (failureExplosionRadius < 0.0D || failureExplosionRadius > 16.0D) {
            throw new IllegalArgumentException("Alchemy failure explosion radius must be between zero and 16");
        }
        if (failureDregCount < 0 || failureDregCount > 64) {
            throw new IllegalArgumentException("Alchemy failure dreg count must be between zero and 64");
        }
        effects = List.copyOf(effects);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    public Optional<ElementType> getPrimaryElement() {
        return Optional.ofNullable(this.primaryElement);
    }

    public Optional<AlchemyFeedStep> getFeedStep(AlchemyIngredientSlot slot) {
        return this.feedPlan.stream()
                .filter(step -> step.slot() == slot)
                .findFirst();
    }

    public boolean isDelayedSlot(int slotIndex) {
        return getFeedStep(AlchemyIngredientSlot.fromSlotIndex(slotIndex)).isPresent();
    }

    public AlchemyIngredient getIngredient(AlchemyIngredientSlot slot) {
        return switch (slot) {
            case PRIMARY -> this.primaryIngredient;
            case SECONDARY_1 -> this.secondaryIngredients.get(0);
            case SECONDARY_2 -> this.secondaryIngredients.get(1);
            case AUXILIARY_1 -> this.auxiliaryIngredients.get(0);
            case AUXILIARY_2 -> this.auxiliaryIngredients.get(1);
        };
    }

    public String getAllowedRealmNames() {
        return this.allowedRealms.stream()
                .sorted()
                .map(CultivationRealm::getChineseName)
                .collect(Collectors.joining("、"));
    }

    public String getMinimumRefiningRealmName() {
        return this.minimumRefiningRealm.getChineseName();
    }

    private static void validateFeedPlan(
            List<AlchemyFeedStep> feedPlan,
            List<HeatStage> heatStages
    ) {
        if (feedPlan.isEmpty()) {
            return;
        }
        if (heatStages.isEmpty()) {
            throw new IllegalArgumentException(
                    "Alchemy recipes with a feed plan must define heat stages"
            );
        }
        Set<AlchemyIngredientSlot> seenSlots = new HashSet<>();
        for (AlchemyFeedStep step : feedPlan) {
            Objects.requireNonNull(step, "feedPlan step");
            if (!seenSlots.add(step.slot())) {
                throw new IllegalArgumentException(
                        "Alchemy feed plan defines slot more than once: "
                                + step.slot().getSerializedName()
                );
            }
            if (step.stageIndex() >= heatStages.size()) {
                throw new IllegalArgumentException(
                        "Alchemy feed stage " + (step.stageIndex() + 1)
                                + " exceeds heat stage count " + heatStages.size()
                );
            }
        }
    }
}
