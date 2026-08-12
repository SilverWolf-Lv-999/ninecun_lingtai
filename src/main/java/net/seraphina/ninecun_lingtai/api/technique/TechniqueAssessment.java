package net.seraphina.ninecun_lingtai.api.technique;

import java.util.Objects;

public record TechniqueAssessment(int score, String feedback) {
    public TechniqueAssessment {
        if (score < TechniqueComprehension.MINIMUM_SCORE || score > TechniqueComprehension.MAXIMUM_SCORE) {
            throw new IllegalArgumentException("Technique comprehension score must be between zero and one hundred");
        }
        feedback = Objects.requireNonNull(feedback, "feedback");
    }
}
