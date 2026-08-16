package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.ModMain;

public final class SpiritualRealmUtility {
    public static final ResourceLocation SPIRITUAL_REALM_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "spiritual_realm"
    );
    public static final int SETTLEMENT_REGION_SIZE_BLOCKS = 4_096;
    public static final int SETTLEMENT_REGION_SPACING_BLOCKS = 8_192;
    public static final int SETTLEMENT_ROAD_SPACING_BLOCKS = 32;
    public static final int SETTLEMENT_ROAD_HALF_WIDTH_BLOCKS = 2;
    public static final int UNDERGROUND_REGION_SIZE_BLOCKS = 4_096;
    public static final int UNDERGROUND_REGION_SPACING_BLOCKS = 8_192;

    private static final long HASH_MULTIPLIER_X = 0x9E3779B97F4A7C15L;
    private static final long HASH_MULTIPLIER_Y = 0xC2B2AE3D27D4EB4FL;
    private static final long HASH_MULTIPLIER_Z = 0x165667B19E3779F9L;

    public static boolean isSpiritualRealm(Level level) {
        return level.dimension().location().equals(SPIRITUAL_REALM_ID);
    }

    public static boolean isSettlementPosition(int blockX, int blockZ) {
        return isInsideGridRegion(
                blockX,
                blockZ,
                SETTLEMENT_REGION_SIZE_BLOCKS,
                SETTLEMENT_REGION_SPACING_BLOCKS,
                0,
                0
        );
    }

    public static boolean isMineshaftPosition(int blockX, int blockZ) {
        return isInsideGridRegion(
                blockX,
                blockZ,
                UNDERGROUND_REGION_SIZE_BLOCKS,
                UNDERGROUND_REGION_SPACING_BLOCKS,
                0,
                0
        );
    }

    public static boolean isDeepDarkPosition(int blockX, int blockZ) {
        return isInsideGridRegion(
                blockX,
                blockZ,
                UNDERGROUND_REGION_SIZE_BLOCKS,
                UNDERGROUND_REGION_SPACING_BLOCKS,
                UNDERGROUND_REGION_SPACING_BLOCKS / 2,
                UNDERGROUND_REGION_SPACING_BLOCKS / 2
        );
    }

    public static boolean isSettlementRoad(int blockX, int blockZ) {
        return distanceToRoadCenter(blockX) <= SETTLEMENT_ROAD_HALF_WIDTH_BLOCKS
                || distanceToRoadCenter(blockZ) <= SETTLEMENT_ROAD_HALF_WIDTH_BLOCKS;
    }

    public static boolean shouldGenerateSettlementHouse(long seed, int chunkX, int chunkZ) {
        return Math.floorMod(mixSeed(seed, chunkX, chunkZ, 0x51E771E5L), 2L) == 0L;
    }

    public static boolean shouldGenerateSettlementVillager(long seed, int chunkX, int chunkZ) {
        return Math.floorMod(mixSeed(seed, chunkX, chunkZ, 0x7711A6E2L), 16L) == 0L;
    }

    public static long mixSeed(long seed, long x, long z, long salt) {
        return avalanche(seed ^ x * HASH_MULTIPLIER_X ^ z * HASH_MULTIPLIER_Z ^ salt);
    }

    public static long mixSeed(long seed, int x, int y, int z, long salt) {
        return avalanche(
                seed
                        ^ (long) x * HASH_MULTIPLIER_X
                        ^ (long) y * HASH_MULTIPLIER_Y
                        ^ (long) z * HASH_MULTIPLIER_Z
                        ^ salt
        );
    }

    public static double toUnitDouble(long value) {
        return (double) (value >>> 11) * 0x1.0p-53;
    }

    private static boolean isInsideGridRegion(
            int blockX,
            int blockZ,
            int regionSize,
            int regionSpacing,
            int offsetX,
            int offsetZ
    ) {
        int centerX = getNearestGridCenter(blockX, regionSpacing, offsetX);
        int centerZ = getNearestGridCenter(blockZ, regionSpacing, offsetZ);
        int radius = regionSize / 2;
        return blockX >= centerX - radius
                && blockX < centerX + radius
                && blockZ >= centerZ - radius
                && blockZ < centerZ + radius;
    }

    private static int getNearestGridCenter(int coordinate, int spacing, int offset) {
        long shiftedCoordinate = (long) coordinate - offset + spacing / 2L;
        long region = Math.floorDiv(shiftedCoordinate, spacing);
        return Math.toIntExact(region * spacing + offset);
    }

    private static int distanceToRoadCenter(int coordinate) {
        int localCoordinate = Math.floorMod(coordinate, SETTLEMENT_ROAD_SPACING_BLOCKS);
        return Math.min(localCoordinate, SETTLEMENT_ROAD_SPACING_BLOCKS - localCoordinate);
    }

    private static long avalanche(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
