package net.seraphina.ninecun_lingtai.common.realm;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class RealmDefinition {
    private final ResourceLocation thisId;
    private final String thisDisplayName;
    private final long thisSeed;
    private final long thisCreatedAtEpochMillis;

    public RealmDefinition(
            ResourceLocation id,
            String displayName,
            long seed,
            long createdAtEpochMillis
    ) {
        this.thisId = Objects.requireNonNull(id, "id");
        this.thisDisplayName = Objects.requireNonNull(displayName, "displayName").strip();
        if (this.thisDisplayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (createdAtEpochMillis < 0L) {
            throw new IllegalArgumentException("createdAtEpochMillis must not be negative");
        }
        this.thisSeed = seed;
        this.thisCreatedAtEpochMillis = createdAtEpochMillis;
    }

    public ResourceLocation getId() {
        return this.thisId;
    }

    public String getDisplayName() {
        return this.thisDisplayName;
    }

    public long getSeed() {
        return this.thisSeed;
    }

    public long getCreatedAtEpochMillis() {
        return this.thisCreatedAtEpochMillis;
    }
}
