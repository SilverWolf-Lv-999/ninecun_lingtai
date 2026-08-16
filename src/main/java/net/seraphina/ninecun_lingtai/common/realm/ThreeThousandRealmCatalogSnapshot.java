package net.seraphina.ninecun_lingtai.common.realm;

import java.util.List;
import java.util.Objects;

public final class ThreeThousandRealmCatalogSnapshot {
    private final ThreeThousandRealmCatalogStatus thisStatus;
    private final int thisCompletedRealmCount;
    private final int thisTargetRealmCount;
    private final List<ThreeThousandRealmCatalogEntry> thisRealms;

    public ThreeThousandRealmCatalogSnapshot(
            ThreeThousandRealmCatalogStatus status,
            int completedRealmCount,
            int targetRealmCount,
            List<ThreeThousandRealmCatalogEntry> realms
    ) {
        this.thisStatus = Objects.requireNonNull(status, "status");
        if (completedRealmCount < 0) {
            throw new IllegalArgumentException("completedRealmCount must not be negative");
        }
        if (targetRealmCount < 1 || completedRealmCount > targetRealmCount) {
            throw new IllegalArgumentException("Invalid three-thousand-realm catalog progress");
        }

        this.thisRealms = List.copyOf(Objects.requireNonNull(realms, "realms"));
        if (status != ThreeThousandRealmCatalogStatus.FAILED
                && this.thisRealms.size() != completedRealmCount) {
            throw new IllegalArgumentException(
                    "Catalog entries must match the completed realm count while loading or ready"
            );
        }
        if (status == ThreeThousandRealmCatalogStatus.FAILED && !this.thisRealms.isEmpty()) {
            throw new IllegalArgumentException("A failed catalog must not contain realm entries");
        }
        if (status == ThreeThousandRealmCatalogStatus.READY
                && completedRealmCount != targetRealmCount) {
            throw new IllegalArgumentException("A ready catalog must contain every configured realm");
        }
        this.thisCompletedRealmCount = completedRealmCount;
        this.thisTargetRealmCount = targetRealmCount;
    }

    public ThreeThousandRealmCatalogStatus getStatus() {
        return this.thisStatus;
    }

    public int getCompletedRealmCount() {
        return this.thisCompletedRealmCount;
    }

    public int getTargetRealmCount() {
        return this.thisTargetRealmCount;
    }

    public List<ThreeThousandRealmCatalogEntry> getRealms() {
        return this.thisRealms;
    }
}
