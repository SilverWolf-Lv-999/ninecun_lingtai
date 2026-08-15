package net.seraphina.ninecun_lingtai.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogEntry;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogSnapshot;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmCatalogStatus;

import java.util.ArrayList;
import java.util.List;

public final class ThreeThousandRealmPacketUtility {
    private static final int MAXIMUM_REALM_COUNT = 3000;
    private static final int MAXIMUM_DISPLAY_NAME_LENGTH = 128;

    private ThreeThousandRealmPacketUtility() {
    }

    public static void writeCatalogSnapshot(
            FriendlyByteBuf buffer,
            ThreeThousandRealmCatalogSnapshot snapshot
    ) {
        buffer.writeVarInt(snapshot.getStatus().ordinal());
        buffer.writeVarInt(snapshot.getCompletedRealmCount());
        buffer.writeVarInt(snapshot.getTargetRealmCount());
        List<ThreeThousandRealmCatalogEntry> realms = snapshot.getRealms();
        if (realms.size() > MAXIMUM_REALM_COUNT) {
            throw new IllegalArgumentException("Too many three-thousand-realm catalog entries");
        }
        buffer.writeVarInt(realms.size());
        for (ThreeThousandRealmCatalogEntry realm : realms) {
            buffer.writeResourceLocation(realm.getRealmId());
            buffer.writeUtf(realm.getDisplayName(), MAXIMUM_DISPLAY_NAME_LENGTH);
        }
    }

    public static ThreeThousandRealmCatalogSnapshot readCatalogSnapshot(FriendlyByteBuf buffer) {
        int statusOrdinal = buffer.readVarInt();
        ThreeThousandRealmCatalogStatus[] statuses = ThreeThousandRealmCatalogStatus.values();
        if (statusOrdinal < 0 || statusOrdinal >= statuses.length) {
            throw new IllegalArgumentException("Invalid three-thousand-realm catalog status");
        }

        int completedRealmCount = buffer.readVarInt();
        int targetRealmCount = buffer.readVarInt();
        int realmCount = buffer.readVarInt();
        if (realmCount < 0 || realmCount > MAXIMUM_REALM_COUNT) {
            throw new IllegalArgumentException("Invalid three-thousand-realm catalog entry count");
        }

        List<ThreeThousandRealmCatalogEntry> realms = new ArrayList<>(realmCount);
        for (int index = 0; index < realmCount; index++) {
            ResourceLocation realmId = buffer.readResourceLocation();
            String displayName = buffer.readUtf(MAXIMUM_DISPLAY_NAME_LENGTH);
            realms.add(new ThreeThousandRealmCatalogEntry(realmId, displayName));
        }
        return new ThreeThousandRealmCatalogSnapshot(
                statuses[statusOrdinal],
                completedRealmCount,
                targetRealmCount,
                realms
        );
    }
}
