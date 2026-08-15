package net.seraphina.ninecun_lingtai.common.realm;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class ThreeThousandRealmCatalogEntry {
    private final ResourceLocation thisRealmId;
    private final String thisDisplayName;

    public ThreeThousandRealmCatalogEntry(ResourceLocation realmId, String displayName) {
        this.thisRealmId = Objects.requireNonNull(realmId, "realmId");
        this.thisDisplayName = Objects.requireNonNull(displayName, "displayName").strip();
        if (this.thisDisplayName.isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }

    public ResourceLocation getRealmId() {
        return this.thisRealmId;
    }

    public String getDisplayName() {
        return this.thisDisplayName;
    }
}
