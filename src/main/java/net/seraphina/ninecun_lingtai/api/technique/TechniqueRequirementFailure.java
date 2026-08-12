package net.seraphina.ninecun_lingtai.api.technique;

import java.util.Objects;

public record TechniqueRequirementFailure(TechniqueRequirementType type, String detail) {
    public TechniqueRequirementFailure {
        Objects.requireNonNull(type, "type");
        detail = Objects.requireNonNull(detail, "detail");
    }
}
