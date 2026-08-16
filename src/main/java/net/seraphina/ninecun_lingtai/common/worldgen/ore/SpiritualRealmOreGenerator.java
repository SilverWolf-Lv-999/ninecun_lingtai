package net.seraphina.ninecun_lingtai.common.worldgen.ore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.Tags;
import net.seraphina.ninecun_lingtai.utility.SpiritualRealmUtility;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class SpiritualRealmOreGenerator {
    private static final int UPDATE_FLAGS = 2;
    private static final int ORE_REGION_SPACING_BLOCKS = 4_096;
    private static final int CUSTOM_ORE_REGION_SPACING_BLOCKS = 2_048;
    private static final double TWO_PI = Math.PI * 2.0D;
    private static final long CUSTOM_ORE_SALT = 0x7A11A11EL;
    private static final Set<Block> VANILLA_ORE_BLOCKS = Set.of(
            Blocks.COAL_ORE,
            Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE,
            Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE,
            Blocks.DEEPSLATE_COPPER_ORE,
            Blocks.GOLD_ORE,
            Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.REDSTONE_ORE,
            Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.LAPIS_ORE,
            Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.NETHER_QUARTZ_ORE,
            Blocks.NETHER_GOLD_ORE,
            Blocks.ANCIENT_DEBRIS
    );
    private static final List<OreRegionDefinition> VANILLA_ORE_REGIONS = List.of(
            new OreRegionDefinition(
                    Blocks.COAL_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(),
                    -24,
                    192,
                    2_800.0D,
                    7.0D,
                    4,
                    180.0D,
                    0xC0A1L
            ),
            new OreRegionDefinition(
                    Blocks.IRON_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(),
                    -56,
                    128,
                    2_700.0D,
                    7.0D,
                    4,
                    180.0D,
                    0x1A0FL
            ),
            new OreRegionDefinition(
                    Blocks.COPPER_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState(),
                    -16,
                    112,
                    2_600.0D,
                    7.5D,
                    4,
                    190.0D,
                    0xC077E2L
            ),
            new OreRegionDefinition(
                    Blocks.GOLD_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(),
                    -56,
                    48,
                    2_700.0D,
                    6.5D,
                    4,
                    175.0D,
                    0x601DL
            ),
            new OreRegionDefinition(
                    Blocks.REDSTONE_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState(),
                    -60,
                    16,
                    2_700.0D,
                    6.5D,
                    4,
                    175.0D,
                    0x2ED570EEL
            ),
            new OreRegionDefinition(
                    Blocks.LAPIS_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState(),
                    -48,
                    64,
                    2_500.0D,
                    6.0D,
                    4,
                    170.0D,
                    0x1A715L
            ),
            new OreRegionDefinition(
                    Blocks.DIAMOND_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(),
                    -60,
                    -4,
                    3_000.0D,
                    7.0D,
                    5,
                    185.0D,
                    0xD1A60D1L
            ),
            new OreRegionDefinition(
                    Blocks.EMERALD_ORE.defaultBlockState(),
                    Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState(),
                    -16,
                    160,
                    2_500.0D,
                    6.0D,
                    3,
                    200.0D,
                    0xE6E2A1DL
            ),
            new OreRegionDefinition(
                    Blocks.NETHER_QUARTZ_ORE.defaultBlockState(),
                    Blocks.NETHER_QUARTZ_ORE.defaultBlockState(),
                    -32,
                    112,
                    2_700.0D,
                    7.0D,
                    4,
                    180.0D,
                    0x0A12E2L
            ),
            new OreRegionDefinition(
                    Blocks.NETHER_GOLD_ORE.defaultBlockState(),
                    Blocks.NETHER_GOLD_ORE.defaultBlockState(),
                    -48,
                    80,
                    2_600.0D,
                    6.5D,
                    4,
                    180.0D,
                    0x6E7AE2L
            ),
            new OreRegionDefinition(
                    Blocks.ANCIENT_DEBRIS.defaultBlockState(),
                    Blocks.ANCIENT_DEBRIS.defaultBlockState(),
                    -56,
                    32,
                    3_000.0D,
                    7.0D,
                    5,
                    185.0D,
                    0xA6C1E67L
            )
    );

    private static volatile OrePaletteCache orePaletteCache = new OrePaletteCache(null, List.of());

    public static void generate(WorldGenLevel level, ChunkAccess chunk) {
        for (OreRegionDefinition regionDefinition : VANILLA_ORE_REGIONS) {
            generateOreRegions(level, chunk, regionDefinition, ORE_REGION_SPACING_BLOCKS);
        }
        generateCustomOreRegions(level, chunk);
    }

    private static void generateCustomOreRegions(WorldGenLevel level, ChunkAccess chunk) {
        List<BlockState> customOreStates = getCustomOreStates(level);
        if (customOreStates.isEmpty()) {
            return;
        }

        int minimumY = Math.max(level.getMinBuildHeight() + 8, -56);
        int maximumY = Math.min(level.getMaxBuildHeight() - 16, 128);
        if (minimumY >= maximumY) {
            return;
        }

        int maximumReach = getMaximumReach(1_600.0D, 5.0D, 140.0D);
        int minimumCellX = Math.floorDiv(chunk.getPos().getMinBlockX() - maximumReach, CUSTOM_ORE_REGION_SPACING_BLOCKS);
        int maximumCellX = Math.floorDiv(chunk.getPos().getMaxBlockX() + maximumReach, CUSTOM_ORE_REGION_SPACING_BLOCKS);
        int minimumCellZ = Math.floorDiv(chunk.getPos().getMinBlockZ() - maximumReach, CUSTOM_ORE_REGION_SPACING_BLOCKS);
        int maximumCellZ = Math.floorDiv(chunk.getPos().getMaxBlockZ() + maximumReach, CUSTOM_ORE_REGION_SPACING_BLOCKS);
        for (int cellX = minimumCellX; cellX <= maximumCellX; cellX++) {
            for (int cellZ = minimumCellZ; cellZ <= maximumCellZ; cellZ++) {
                long selectionHash = SpiritualRealmUtility.mixSeed(level.getSeed(), cellX, cellZ, CUSTOM_ORE_SALT);
                int oreIndex = (int) Math.floorMod(selectionHash, (long) customOreStates.size());
                OreRegionDefinition regionDefinition = new OreRegionDefinition(
                        customOreStates.get(oreIndex),
                        customOreStates.get(oreIndex),
                        minimumY,
                        maximumY,
                        1_600.0D,
                        5.0D,
                        3,
                        140.0D,
                        CUSTOM_ORE_SALT ^ oreIndex * 0x9E3779B9L
                );
                generateOreRegion(level, chunk, regionDefinition, CUSTOM_ORE_REGION_SPACING_BLOCKS, cellX, cellZ);
            }
        }
    }

    private static void generateOreRegions(
            WorldGenLevel level,
            ChunkAccess chunk,
            OreRegionDefinition regionDefinition,
            int regionSpacing
    ) {
        int maximumReach = getMaximumReach(
                regionDefinition.beltLengthBlocks(),
                regionDefinition.beltRadiusBlocks(),
                regionDefinition.beltSeparationBlocks()
        );
        int minimumCellX = Math.floorDiv(chunk.getPos().getMinBlockX() - maximumReach, regionSpacing);
        int maximumCellX = Math.floorDiv(chunk.getPos().getMaxBlockX() + maximumReach, regionSpacing);
        int minimumCellZ = Math.floorDiv(chunk.getPos().getMinBlockZ() - maximumReach, regionSpacing);
        int maximumCellZ = Math.floorDiv(chunk.getPos().getMaxBlockZ() + maximumReach, regionSpacing);
        for (int cellX = minimumCellX; cellX <= maximumCellX; cellX++) {
            for (int cellZ = minimumCellZ; cellZ <= maximumCellZ; cellZ++) {
                generateOreRegion(level, chunk, regionDefinition, regionSpacing, cellX, cellZ);
            }
        }
    }

    private static void generateOreRegion(
            WorldGenLevel level,
            ChunkAccess chunk,
            OreRegionDefinition regionDefinition,
            int regionSpacing,
            int cellX,
            int cellZ
    ) {
        int minimumY = Math.max(level.getMinBuildHeight() + 1, regionDefinition.minimumY());
        int maximumY = Math.min(level.getMaxBuildHeight() - 2, regionDefinition.maximumY());
        if (minimumY >= maximumY) {
            return;
        }

        long seed = level.getSeed();
        long regionSalt = regionDefinition.salt();
        double centerX = (long) cellX * regionSpacing + regionSpacing / 2.0D
                + signedUnit(seed, cellX, cellZ, regionSalt ^ 0x14A53L) * regionSpacing * 0.11D;
        double centerZ = (long) cellZ * regionSpacing + regionSpacing / 2.0D
                + signedUnit(seed, cellX, cellZ, regionSalt ^ 0x79B9L) * regionSpacing * 0.11D;
        double angle = SpiritualRealmUtility.toUnitDouble(SpiritualRealmUtility.mixSeed(
                seed,
                cellX,
                cellZ,
                regionSalt ^ 0xC6BC2L
        )) * TWO_PI;
        double directionX = Math.cos(angle);
        double directionZ = Math.sin(angle);
        double perpendicularX = -directionZ;
        double perpendicularZ = directionX;
        double beltCenter = (regionDefinition.beltCount() - 1) / 2.0D;

        for (int beltIndex = 0; beltIndex < regionDefinition.beltCount(); beltIndex++) {
            long beltSalt = regionSalt ^ (long) beltIndex * 0x9E3779B97F4A7C15L;
            double beltOffset = (beltIndex - beltCenter) * regionDefinition.beltSeparationBlocks()
                    + signedUnit(seed, cellX, cellZ, beltSalt ^ 0xA5A5L) * 48.0D;
            double beltLength = regionDefinition.beltLengthBlocks() * (
                    0.9D
                            + SpiritualRealmUtility.toUnitDouble(SpiritualRealmUtility.mixSeed(
                            seed,
                            cellX,
                            cellZ,
                            beltSalt ^ 0x5A17L
                    )) * 0.2D
            );
            double halfLength = beltLength / 2.0D;
            double centerY = minimumY + (maximumY - minimumY)
                    * SpiritualRealmUtility.toUnitDouble(SpiritualRealmUtility.mixSeed(
                    seed,
                    cellX,
                    cellZ,
                    beltSalt ^ 0xD31A5L
            ));
            double verticalRange = Math.min(36.0D, (maximumY - minimumY) * 0.32D);
            double verticalOffset = signedUnit(seed, cellX, cellZ, beltSalt ^ 0x2B992L) * verticalRange;
            double startY = clamp(centerY - verticalOffset, minimumY, maximumY);
            double endY = clamp(centerY + verticalOffset, minimumY, maximumY);
            double startX = centerX + perpendicularX * beltOffset - directionX * halfLength;
            double startZ = centerZ + perpendicularZ * beltOffset - directionZ * halfLength;
            double endX = centerX + perpendicularX * beltOffset + directionX * halfLength;
            double endZ = centerZ + perpendicularZ * beltOffset + directionZ * halfLength;
            double curvePhase = SpiritualRealmUtility.toUnitDouble(SpiritualRealmUtility.mixSeed(
                    seed,
                    cellX,
                    cellZ,
                    beltSalt ^ 0xBEEFL
            )) * TWO_PI;
            placeOreBelt(
                    level,
                    chunk,
                    regionDefinition,
                    startX,
                    startY,
                    startZ,
                    endX,
                    endY,
                    endZ,
                    curvePhase,
                    beltLength
            );
        }
    }

    private static void placeOreBelt(
            WorldGenLevel level,
            ChunkAccess chunk,
            OreRegionDefinition regionDefinition,
            double startX,
            double startY,
            double startZ,
            double endX,
            double endY,
            double endZ,
            double curvePhase,
            double beltLength
    ) {
        double curveAmplitude = 44.0D;
        double margin = regionDefinition.beltRadiusBlocks() + curveAmplitude + 8.0D;
        if (!canIntersectChunk(chunk, startX, startZ, endX, endZ, margin)) {
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(beltLength / 4.0D));
        double directionX = endX - startX;
        double directionZ = endZ - startZ;
        double horizontalLength = Math.max(1.0D, Math.hypot(directionX, directionZ));
        double perpendicularX = -directionZ / horizontalLength;
        double perpendicularZ = directionX / horizontalLength;
        for (int step = 0; step <= steps; step++) {
            double progress = (double) step / steps;
            double curveOffset = Math.sin(progress * TWO_PI * 2.0D + curvePhase) * curveAmplitude;
            double pointX = lerp(progress, startX, endX) + perpendicularX * curveOffset;
            double pointY = lerp(progress, startY, endY)
                    + Math.sin(progress * TWO_PI * 3.0D + curvePhase) * 3.0D;
            double pointZ = lerp(progress, startZ, endZ) + perpendicularZ * curveOffset;
            if (isNearChunk(chunk, pointX, pointZ, regionDefinition.beltRadiusBlocks())) {
                placeOreSphere(level, chunk, regionDefinition, pointX, pointY, pointZ);
            }
        }
    }

    private static void placeOreSphere(
            WorldGenLevel level,
            ChunkAccess chunk,
            OreRegionDefinition regionDefinition,
            double centerX,
            double centerY,
            double centerZ
    ) {
        int radius = (int) Math.ceil(regionDefinition.beltRadiusBlocks());
        int minimumX = Math.max(chunk.getPos().getMinBlockX(), (int) Math.floor(centerX) - radius);
        int maximumX = Math.min(chunk.getPos().getMaxBlockX(), (int) Math.floor(centerX) + radius);
        int minimumZ = Math.max(chunk.getPos().getMinBlockZ(), (int) Math.floor(centerZ) - radius);
        int maximumZ = Math.min(chunk.getPos().getMaxBlockZ(), (int) Math.floor(centerZ) + radius);
        int minimumY = Math.max(
                Math.max(level.getMinBuildHeight() + 1, regionDefinition.minimumY()),
                (int) Math.floor(centerY) - radius
        );
        int maximumY = Math.min(
                Math.min(level.getMaxBuildHeight() - 2, regionDefinition.maximumY()),
                (int) Math.floor(centerY) + radius
        );
        double radiusSquared = regionDefinition.beltRadiusBlocks() * regionDefinition.beltRadiusBlocks();
        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();
        for (int blockX = minimumX; blockX <= maximumX; blockX++) {
            double distanceX = blockX + 0.5D - centerX;
            for (int blockY = minimumY; blockY <= maximumY; blockY++) {
                double distanceY = blockY + 0.5D - centerY;
                for (int blockZ = minimumZ; blockZ <= maximumZ; blockZ++) {
                    double distanceZ = blockZ + 0.5D - centerZ;
                    if (distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ
                            > radiusSquared) {
                        continue;
                    }
                    if (Math.floorMod(SpiritualRealmUtility.mixSeed(
                            level.getSeed(),
                            blockX,
                            blockY,
                            blockZ,
                            regionDefinition.salt()
                    ), 11L) == 0L) {
                        continue;
                    }
                    mutablePosition.set(blockX, blockY, blockZ);
                    BlockState existingState = level.getBlockState(mutablePosition);
                    if (canReplaceWithOre(existingState)) {
                        level.setBlock(
                                mutablePosition,
                                regionDefinition.getOreState(blockY),
                                UPDATE_FLAGS
                        );
                    }
                }
            }
        }
    }

    private static List<BlockState> getCustomOreStates(WorldGenLevel level) {
        Registry<Block> blockRegistry = level.registryAccess().registryOrThrow(Registries.BLOCK);
        OrePaletteCache currentCache = orePaletteCache;
        if (currentCache.blockRegistry() == blockRegistry) {
            return currentCache.oreStates();
        }

        synchronized (SpiritualRealmOreGenerator.class) {
            currentCache = orePaletteCache;
            if (currentCache.blockRegistry() == blockRegistry) {
                return currentCache.oreStates();
            }
            List<BlockState> oreStates = blockRegistry.getTag(Tags.Blocks.ORES)
                    .stream()
                    .flatMap(HolderSet.Named::stream)
                    .map(holder -> holder.value().defaultBlockState())
                    .filter(blockState -> !blockState.isAir())
                    .filter(blockState -> !VANILLA_ORE_BLOCKS.contains(blockState.getBlock()))
                    .distinct()
                    .sorted(Comparator.comparing(blockState ->
                            blockRegistry.getKey(blockState.getBlock()).toString()
                    ))
                    .toList();
            orePaletteCache = new OrePaletteCache(blockRegistry, oreStates);
            return oreStates;
        }
    }

    private static boolean canReplaceWithOre(BlockState blockState) {
        return blockState.is(BlockTags.STONE_ORE_REPLACEABLES)
                || blockState.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || blockState.is(Tags.Blocks.ORE_BEARING_GROUND_STONE)
                || blockState.is(Tags.Blocks.ORE_BEARING_GROUND_DEEPSLATE);
    }

    private static int getMaximumReach(double length, double radius, double separation) {
        return (int) Math.ceil(length / 2.0D + separation * 3.0D + radius + 64.0D);
    }

    private static boolean canIntersectChunk(
            ChunkAccess chunk,
            double startX,
            double startZ,
            double endX,
            double endZ,
            double margin
    ) {
        double minimumX = Math.min(startX, endX) - margin;
        double maximumX = Math.max(startX, endX) + margin;
        double minimumZ = Math.min(startZ, endZ) - margin;
        double maximumZ = Math.max(startZ, endZ) + margin;
        return maximumX >= chunk.getPos().getMinBlockX()
                && minimumX <= chunk.getPos().getMaxBlockX()
                && maximumZ >= chunk.getPos().getMinBlockZ()
                && minimumZ <= chunk.getPos().getMaxBlockZ();
    }

    private static boolean isNearChunk(
            ChunkAccess chunk,
            double blockX,
            double blockZ,
            double radius
    ) {
        return blockX + radius >= chunk.getPos().getMinBlockX()
                && blockX - radius <= chunk.getPos().getMaxBlockX()
                && blockZ + radius >= chunk.getPos().getMinBlockZ()
                && blockZ - radius <= chunk.getPos().getMaxBlockZ();
    }

    private static double signedUnit(long seed, int cellX, int cellZ, long salt) {
        return SpiritualRealmUtility.toUnitDouble(SpiritualRealmUtility.mixSeed(seed, cellX, cellZ, salt))
                * 2.0D - 1.0D;
    }

    private static double lerp(double progress, double start, double end) {
        return start + (end - start) * progress;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record OrePaletteCache(
            Registry<Block> blockRegistry,
            List<BlockState> oreStates
    ) {
    }

    private record OreRegionDefinition(
            BlockState normalOreState,
            BlockState deepslateOreState,
            int minimumY,
            int maximumY,
            double beltLengthBlocks,
            double beltRadiusBlocks,
            int beltCount,
            double beltSeparationBlocks,
            long salt
    ) {
        private BlockState getOreState(int blockY) {
            return blockY < 0 ? this.deepslateOreState : this.normalOreState;
        }
    }
}
