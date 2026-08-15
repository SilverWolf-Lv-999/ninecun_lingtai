package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerThreeThousandRealmReturnStateTest {
    @TempDir
    private Path thisTemporaryDirectory;

    @Test
    void persistsAndRemovesPlayerRealmReturnPositions() {
        Path filePath = this.thisTemporaryDirectory.resolve("player_realm_returns.json");
        UUID playerId = UUID.fromString("0b2c15d0-c1bc-456f-bce5-13c0ded99502");
        PlayerThreeThousandRealmReturnPosition returnPosition =
                new PlayerThreeThousandRealmReturnPosition(
                        ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "qixing_jie_test"),
                        123.25D,
                        70.5D,
                        -456.75D,
                        180.0F,
                        23.5F
                );

        PlayerThreeThousandRealmReturnState state = PlayerThreeThousandRealmReturnState.load(filePath);
        assertTrue(state.putReturnPosition(playerId, returnPosition));

        PlayerThreeThousandRealmReturnPosition reloadedPosition =
                PlayerThreeThousandRealmReturnState.load(filePath)
                        .getReturnPosition(playerId)
                        .orElseThrow();
        assertEquals(returnPosition.getRealmId(), reloadedPosition.getRealmId());
        assertEquals(returnPosition.getX(), reloadedPosition.getX());
        assertEquals(returnPosition.getY(), reloadedPosition.getY());
        assertEquals(returnPosition.getZ(), reloadedPosition.getZ());
        assertEquals(returnPosition.getYRot(), reloadedPosition.getYRot());
        assertEquals(returnPosition.getXRot(), reloadedPosition.getXRot());

        assertTrue(state.removeReturnPosition(playerId));
        assertFalse(
                PlayerThreeThousandRealmReturnState.load(filePath)
                        .getReturnPosition(playerId)
                        .isPresent()
        );
    }

    @Test
    void ignoresMalformedAndForeignReturnPositions() throws IOException {
        Path filePath = this.thisTemporaryDirectory.resolve("player_realm_returns.json");
        Files.writeString(
                filePath,
                """
                        {
                          "formatVersion": 1,
                          "returnPositions": [
                            {
                              "playerId": "0b2c15d0-c1bc-456f-bce5-13c0ded99502",
                              "realmId": "ninecun_lingtai:qixing_jie_test",
                              "x": 1.5,
                              "y": 64.0,
                              "z": -3.25,
                              "yRot": 90.0,
                              "xRot": 0.0
                            },
                            {
                              "playerId": "not-a-uuid",
                              "realmId": "ninecun_lingtai:bad_realm",
                              "x": 0.0,
                              "y": 64.0,
                              "z": 0.0,
                              "yRot": 0.0,
                              "xRot": 0.0
                            },
                            {
                              "playerId": "b3730318-a544-47ac-8a75-37bfe6e65a38",
                              "realmId": "minecraft:overworld",
                              "x": 0.0,
                              "y": 64.0,
                              "z": 0.0,
                              "yRot": 0.0,
                              "xRot": 0.0
                            }
                          ]
                        }
                        """,
                StandardCharsets.UTF_8
        );

        PlayerThreeThousandRealmReturnState state = PlayerThreeThousandRealmReturnState.load(filePath);
        assertTrue(
                state.getReturnPosition(UUID.fromString("0b2c15d0-c1bc-456f-bce5-13c0ded99502"))
                        .isPresent()
        );
        assertFalse(
                state.getReturnPosition(UUID.fromString("b3730318-a544-47ac-8a75-37bfe6e65a38"))
                        .isPresent()
        );
    }

    @Test
    void rejectsNonFiniteReturnPositionValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PlayerThreeThousandRealmReturnPosition(
                        ResourceLocation.fromNamespaceAndPath("ninecun_lingtai", "qixing_jie_test"),
                        Double.NaN,
                        64.0D,
                        0.0D,
                        0.0F,
                        0.0F
                )
        );
    }
}
