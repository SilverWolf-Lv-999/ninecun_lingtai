package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record PalmBuddhaCandidateSet(List<PalmBuddhaCandidateSnapshot> candidates) {
    public PalmBuddhaCandidateSet {
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        if (candidates.size() > PalmBuddhaMemberPacketLimits.MAXIMUM_CANDIDATE_COUNT) {
            throw new IllegalArgumentException("Too many palm-buddha candidates");
        }
        Set<UUID> candidateIds = new HashSet<>();
        for (PalmBuddhaCandidateSnapshot candidate : candidates) {
            if (!candidateIds.add(candidate.memberId())) {
                throw new IllegalArgumentException("Duplicate palm-buddha candidate");
            }
        }
    }

    public static PalmBuddhaCandidateSet empty() {
        return new PalmBuddhaCandidateSet(List.of());
    }
}
