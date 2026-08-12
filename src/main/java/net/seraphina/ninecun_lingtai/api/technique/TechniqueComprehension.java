package net.seraphina.ninecun_lingtai.api.technique;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record TechniqueComprehension(
        int minimumLearningScore,
        ResourceLocation perfectScoreMastery,
        List<String> keyConcepts,
        List<String> gradingCriteria,
        List<TechniqueMasteryThreshold> masteryThresholds
) {
    public static final int MINIMUM_SCORE = 0;
    public static final int MAXIMUM_SCORE = 100;

    public TechniqueComprehension {
        Objects.requireNonNull(perfectScoreMastery, "perfectScoreMastery");
        keyConcepts = List.copyOf(keyConcepts);
        gradingCriteria = List.copyOf(gradingCriteria);
        masteryThresholds = masteryThresholds.stream()
                .sorted(Comparator.comparingInt(TechniqueMasteryThreshold::minimumScore))
                .toList();
        if (minimumLearningScore < MINIMUM_SCORE || minimumLearningScore > MAXIMUM_SCORE) {
            throw new IllegalArgumentException("Minimum learning score must be between zero and one hundred");
        }
        if (masteryThresholds.isEmpty()) {
            throw new IllegalArgumentException("A technique must define at least one mastery threshold");
        }

        Set<ResourceLocation> masteryIds = new HashSet<>();
        int previousScore = -1;
        for (TechniqueMasteryThreshold masteryThreshold : masteryThresholds) {
            if (masteryThreshold.minimumScore() < minimumLearningScore) {
                throw new IllegalArgumentException("Mastery thresholds cannot be below the learning score");
            }
            if (masteryThreshold.minimumScore() == previousScore) {
                throw new IllegalArgumentException("Mastery thresholds must use distinct scores");
            }
            if (!masteryIds.add(masteryThreshold.mastery())) {
                throw new IllegalArgumentException("Mastery ids must be unique");
            }
            previousScore = masteryThreshold.minimumScore();
        }
        if (masteryThresholds.get(0).minimumScore() != minimumLearningScore) {
            throw new IllegalArgumentException("The first mastery threshold must equal the learning score");
        }
    }

    public Optional<ResourceLocation> getMastery(int score) {
        if (score < minimumLearningScore || score > MAXIMUM_SCORE) {
            return Optional.empty();
        }
        if (score == MAXIMUM_SCORE) {
            return Optional.of(this.perfectScoreMastery);
        }

        ResourceLocation mastery = null;
        for (TechniqueMasteryThreshold masteryThreshold : this.masteryThresholds) {
            if (score < masteryThreshold.minimumScore()) {
                break;
            }
            mastery = masteryThreshold.mastery();
        }
        return Optional.ofNullable(mastery);
    }
}
