package net.seraphina.ninecun_lingtai.api.technique;

import java.util.List;

public record TechniqueLearnability(List<TechniqueRequirementFailure> failures) {
    public TechniqueLearnability {
        failures = List.copyOf(failures);
    }

    public boolean canLearn() {
        return this.failures.isEmpty();
    }
}
