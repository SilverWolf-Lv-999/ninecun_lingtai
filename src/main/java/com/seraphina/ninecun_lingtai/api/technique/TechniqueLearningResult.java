package com.seraphina.ninecun_lingtai.api.technique;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record TechniqueLearningResult(
        TechniqueLearningStatus status,
        Optional<TechniqueProgress> progress,
        List<TechniqueRequirementFailure> requirementFailures
) {
    public TechniqueLearningResult {
        Objects.requireNonNull(status, "status");
        progress = Objects.requireNonNull(progress, "progress");
        requirementFailures = List.copyOf(requirementFailures);
    }
}
