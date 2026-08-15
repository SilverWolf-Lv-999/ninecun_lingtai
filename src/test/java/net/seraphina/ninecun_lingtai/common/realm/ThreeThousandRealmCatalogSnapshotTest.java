package net.seraphina.ninecun_lingtai.common.realm;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThreeThousandRealmCatalogSnapshotTest {
    @Test
    void allowsPartialCatalogEntriesWhileLoading() {
        List<ThreeThousandRealmCatalogEntry> realms = List.of(
                new ThreeThousandRealmCatalogEntry(
                        ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "first_jie"),
                        "第一界"
                ),
                new ThreeThousandRealmCatalogEntry(
                        ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "second_jie"),
                        "第二界"
                )
        );

        assertDoesNotThrow(
                () -> new ThreeThousandRealmCatalogSnapshot(
                        ThreeThousandRealmCatalogStatus.LOADING,
                        2,
                        3,
                        realms
                )
        );
    }

    @Test
    void rejectsCatalogEntriesThatDoNotMatchLoadingProgress() {
        List<ThreeThousandRealmCatalogEntry> realms = List.of(
                new ThreeThousandRealmCatalogEntry(
                        ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "first_jie"),
                        "第一界"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ThreeThousandRealmCatalogSnapshot(
                        ThreeThousandRealmCatalogStatus.LOADING,
                        2,
                        3,
                        realms
                )
        );
    }
}
