package net.seraphina.ninecun_lingtai.common.worldgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.seraphina.ninecun_lingtai.utility.SpiritualRealmUtility;

public final class SpiritualRealmUndergroundStructureGenerator {
    private static final int UPDATE_FLAGS = 2;
    private static final int MINE_TUNNEL_SPACING_BLOCKS = 48;
    private static final int MINE_TUNNEL_HALF_WIDTH_BLOCKS = 2;
    private static final int MINE_SUPPORT_SPACING_BLOCKS = 16;
    private static final int MINE_TUNNEL_HEIGHT_BLOCKS = 5;
    private static final int[] MINE_FLOOR_Y_LEVELS = {-48, -20, 8};
    private static final int DEEP_DARK_FLOOR_Y = -54;
    private static final int DEEP_DARK_CEILING_HEIGHT_BLOCKS = 18;
    private static final int DEEP_DARK_AVENUE_SPACING_BLOCKS = 96;
    private static final int DEEP_DARK_AVENUE_HALF_WIDTH_BLOCKS = 12;
    private static final long COBWEB_SALT = 0xC0B0EEL;
    private static final long SCULK_SALT = 0x5C01C5L;

    public static void generate(WorldGenLevel level, ChunkAccess chunk) {
        int centerX = chunk.getPos().getMiddleBlockX();
        int centerZ = chunk.getPos().getMiddleBlockZ();
        if (SpiritualRealmUtility.isMineshaftPosition(centerX, centerZ)) {
            generateMineshaft(level, chunk);
        }
        if (SpiritualRealmUtility.isDeepDarkPosition(centerX, centerZ)) {
            generateDeepDark(level, chunk);
        }
    }

    private static void generateMineshaft(WorldGenLevel level, ChunkAccess chunk) {
        int minimumX = chunk.getPos().getMinBlockX();
        int minimumZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();

        for (int floorY : MINE_FLOOR_Y_LEVELS) {
            if (floorY <= level.getMinBuildHeight()
                    || floorY + MINE_TUNNEL_HEIGHT_BLOCKS >= level.getMaxBuildHeight()) {
                continue;
            }

            for (int localX = 0; localX < 16; localX++) {
                int blockX = minimumX + localX;
                boolean tunnelAlongZ = distanceToGridLine(blockX, MINE_TUNNEL_SPACING_BLOCKS)
                        <= MINE_TUNNEL_HALF_WIDTH_BLOCKS;
                for (int localZ = 0; localZ < 16; localZ++) {
                    int blockZ = minimumZ + localZ;
                    boolean tunnelAlongX = distanceToGridLine(blockZ, MINE_TUNNEL_SPACING_BLOCKS)
                            <= MINE_TUNNEL_HALF_WIDTH_BLOCKS;
                    if (!tunnelAlongX && !tunnelAlongZ) {
                        continue;
                    }

                    carveMineshaftColumn(level, mutablePosition, blockX, floorY, blockZ);
                    placeMineshaftFloor(level, mutablePosition, blockX, floorY, blockZ, tunnelAlongX, tunnelAlongZ);
                    placeMineshaftSupport(
                            level,
                            mutablePosition,
                            blockX,
                            floorY,
                            blockZ,
                            tunnelAlongX,
                            tunnelAlongZ
                    );
                    placeMineshaftCobweb(level, mutablePosition, blockX, floorY, blockZ);
                }
            }
        }
    }

    private static void carveMineshaftColumn(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int floorY,
            int blockZ
    ) {
        for (int offsetY = 1; offsetY <= MINE_TUNNEL_HEIGHT_BLOCKS; offsetY++) {
            mutablePosition.set(blockX, floorY + offsetY, blockZ);
            level.setBlock(mutablePosition, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    private static void placeMineshaftFloor(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int floorY,
            int blockZ,
            boolean tunnelAlongX,
            boolean tunnelAlongZ
    ) {
        mutablePosition.set(blockX, floorY, blockZ);
        level.setBlock(mutablePosition, Blocks.OAK_PLANKS.defaultBlockState(), UPDATE_FLAGS);
        if (!tunnelAlongX || !tunnelAlongZ) {
            mutablePosition.setY(floorY + 1);
            level.setBlock(mutablePosition, Blocks.RAIL.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    private static void placeMineshaftSupport(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int floorY,
            int blockZ,
            boolean tunnelAlongX,
            boolean tunnelAlongZ
    ) {
        boolean xSupport = tunnelAlongX
                && Math.floorMod(blockX, MINE_SUPPORT_SPACING_BLOCKS) == 0;
        boolean zSupport = tunnelAlongZ
                && Math.floorMod(blockZ, MINE_SUPPORT_SPACING_BLOCKS) == 0;
        if (!xSupport && !zSupport) {
            return;
        }

        boolean supportEdge = xSupport && distanceToGridLine(blockZ, MINE_TUNNEL_SPACING_BLOCKS)
                == MINE_TUNNEL_HALF_WIDTH_BLOCKS;
        supportEdge |= zSupport && distanceToGridLine(blockX, MINE_TUNNEL_SPACING_BLOCKS)
                == MINE_TUNNEL_HALF_WIDTH_BLOCKS;
        if (supportEdge) {
            for (int offsetY = 1; offsetY < MINE_TUNNEL_HEIGHT_BLOCKS; offsetY++) {
                mutablePosition.set(blockX, floorY + offsetY, blockZ);
                level.setBlock(mutablePosition, Blocks.OAK_LOG.defaultBlockState(), UPDATE_FLAGS);
            }
        }
        if (xSupport || zSupport) {
            mutablePosition.set(blockX, floorY + MINE_TUNNEL_HEIGHT_BLOCKS, blockZ);
            level.setBlock(mutablePosition, Blocks.OAK_PLANKS.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    private static void placeMineshaftCobweb(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int floorY,
            int blockZ
    ) {
        if (Math.floorMod(SpiritualRealmUtility.mixSeed(
                level.getSeed(),
                blockX,
                floorY,
                blockZ,
                COBWEB_SALT
        ), 97L) != 0L) {
            return;
        }
        mutablePosition.set(blockX, floorY + MINE_TUNNEL_HEIGHT_BLOCKS - 1, blockZ);
        level.setBlock(mutablePosition, Blocks.COBWEB.defaultBlockState(), UPDATE_FLAGS);
    }

    private static void generateDeepDark(WorldGenLevel level, ChunkAccess chunk) {
        if (DEEP_DARK_FLOOR_Y <= level.getMinBuildHeight()
                || DEEP_DARK_FLOOR_Y + DEEP_DARK_CEILING_HEIGHT_BLOCKS >= level.getMaxBuildHeight()) {
            return;
        }

        int minimumX = chunk.getPos().getMinBlockX();
        int minimumZ = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();
        for (int localX = 0; localX < 16; localX++) {
            int blockX = minimumX + localX;
            boolean avenueAlongZ = distanceToGridLine(blockX, DEEP_DARK_AVENUE_SPACING_BLOCKS)
                    <= DEEP_DARK_AVENUE_HALF_WIDTH_BLOCKS;
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockZ = minimumZ + localZ;
                boolean avenueAlongX = distanceToGridLine(blockZ, DEEP_DARK_AVENUE_SPACING_BLOCKS)
                        <= DEEP_DARK_AVENUE_HALF_WIDTH_BLOCKS;
                if (!avenueAlongX && !avenueAlongZ) {
                    continue;
                }

                carveDeepDarkAvenue(level, mutablePosition, blockX, blockZ);
                placeDeepDarkFloor(level, mutablePosition, blockX, blockZ, avenueAlongX, avenueAlongZ);
                placeDeepDarkPillar(level, mutablePosition, blockX, blockZ, avenueAlongX, avenueAlongZ);
                placeDeepDarkDecoration(level, mutablePosition, blockX, blockZ);
            }
        }
    }

    private static void carveDeepDarkAvenue(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int blockZ
    ) {
        for (int offsetY = 1; offsetY <= DEEP_DARK_CEILING_HEIGHT_BLOCKS; offsetY++) {
            mutablePosition.set(blockX, DEEP_DARK_FLOOR_Y + offsetY, blockZ);
            level.setBlock(mutablePosition, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    private static void placeDeepDarkFloor(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int blockZ,
            boolean avenueAlongX,
            boolean avenueAlongZ
    ) {
        boolean avenueCenter = distanceToGridLine(blockX, DEEP_DARK_AVENUE_SPACING_BLOCKS) <= 2
                || distanceToGridLine(blockZ, DEEP_DARK_AVENUE_SPACING_BLOCKS) <= 2;
        BlockState floorState = avenueCenter && (avenueAlongX || avenueAlongZ)
                ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                : Blocks.SCULK.defaultBlockState();
        mutablePosition.set(blockX, DEEP_DARK_FLOOR_Y, blockZ);
        level.setBlock(mutablePosition, floorState, UPDATE_FLAGS);
    }

    private static void placeDeepDarkPillar(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int blockZ,
            boolean avenueAlongX,
            boolean avenueAlongZ
    ) {
        if (!avenueAlongX
                || !avenueAlongZ
                || distanceToGridLine(blockX, DEEP_DARK_AVENUE_SPACING_BLOCKS) > 1
                || distanceToGridLine(blockZ, DEEP_DARK_AVENUE_SPACING_BLOCKS) > 1) {
            return;
        }
        for (int offsetY = 1; offsetY < DEEP_DARK_CEILING_HEIGHT_BLOCKS; offsetY++) {
            mutablePosition.set(blockX, DEEP_DARK_FLOOR_Y + offsetY, blockZ);
            level.setBlock(mutablePosition, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    private static void placeDeepDarkDecoration(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int blockX,
            int blockZ
    ) {
        long decorationHash = SpiritualRealmUtility.mixSeed(
                level.getSeed(),
                blockX,
                DEEP_DARK_FLOOR_Y,
                blockZ,
                SCULK_SALT
        );
        int selection = (int) Math.floorMod(decorationHash, 521L);
        if (selection != 0 && selection != 1 && selection != 2) {
            return;
        }

        mutablePosition.set(blockX, DEEP_DARK_FLOOR_Y + 1, blockZ);
        BlockState decorationState = selection == 0
                ? Blocks.SCULK_CATALYST.defaultBlockState()
                : selection == 1 ? Blocks.SCULK_SENSOR.defaultBlockState() : Blocks.SCULK_SHRIEKER.defaultBlockState();
        level.setBlock(mutablePosition, decorationState, UPDATE_FLAGS);
    }

    private static int distanceToGridLine(int coordinate, int spacing) {
        int localCoordinate = Math.floorMod(coordinate, spacing);
        return Math.min(localCoordinate, spacing - localCoordinate);
    }
}
