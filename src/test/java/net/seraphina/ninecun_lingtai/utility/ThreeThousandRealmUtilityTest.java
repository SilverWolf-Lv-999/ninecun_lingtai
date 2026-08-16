package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreeThousandRealmUtilityTest {
    @Test
    void recognizesGeneratedThreeThousandRealmIds() {
        ResourceLocation realmId = ResourceLocation.fromNamespaceAndPath(
                "ninecun_lingtai",
                "qixing_jie_123e4567-e89b-12d3-a456-426614174000"
        );

        assertTrue(ThreeThousandRealmUtility.isGeneratedThreeThousandRealmId(realmId));
    }

    @Test
    void rejectsNonRealmAndMalformedRealmIds() {
        assertFalse(ThreeThousandRealmUtility.isGeneratedThreeThousandRealmId(
                ResourceLocation.fromNamespaceAndPath("example_mod", "qixing_jie_123e4567-e89b-12d3-a456-426614174000")
        ));
        assertFalse(ThreeThousandRealmUtility.isGeneratedThreeThousandRealmId(
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "ring_storage")
        ));
        assertFalse(ThreeThousandRealmUtility.isGeneratedThreeThousandRealmId(
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "qixing_jie_not-a-uuid")
        ));
        assertFalse(ThreeThousandRealmUtility.isGeneratedThreeThousandRealmId(
                ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "qixing_jie_1-1-1-1-1")
        ));
    }
}
