package net.seraphina.ninecun_lingtai.common.worldgen.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.seraphina.ninecun_lingtai.utility.SpiritualRealmUtility;

public final class SpiritualRealmSettlementGenerator {
    private static final int UPDATE_FLAGS = 2;
    private static final int HOUSE_RADIUS = 4;
    private static final int HOUSE_WALL_HEIGHT = 4;
    private static final int CLEARANCE_HEIGHT = 8;
    private static final int MAXIMUM_FOUNDATION_DEPTH = 16;

    public static void generate(WorldGenLevel level, ChunkAccess chunk) {
        int minimumX = chunk.getPos().getMinBlockX();
        int minimumZ = chunk.getPos().getMinBlockZ();
        generateRoads(level, minimumX, minimumZ);

        int centerX = minimumX + 8;
        int centerZ = minimumZ + 8;
        if (!SpiritualRealmUtility.isSettlementPosition(centerX, centerZ)
                || !SpiritualRealmUtility.shouldGenerateSettlementHouse(
                level.getSeed(),
                chunk.getPos().x,
                chunk.getPos().z
        )) {
            return;
        }
        generateHouse(level, centerX, centerZ);
    }

    private static void generateRoads(WorldGenLevel level, int minimumX, int minimumZ) {
        for (int localX = 0; localX < 16; localX++) {
            int blockX = minimumX + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockZ = minimumZ + localZ;
                if (!SpiritualRealmUtility.isSettlementPosition(blockX, blockZ)
                        || !SpiritualRealmUtility.isSettlementRoad(blockX, blockZ)) {
                    continue;
                }
                generateRoadColumn(level, blockX, blockZ);
            }
        }
    }

    private static void generateRoadColumn(WorldGenLevel level, int blockX, int blockZ) {
        int roadY = findSurfaceY(level, blockX, blockZ);
        if (roadY <= level.getMinBuildHeight() || roadY >= level.getMaxBuildHeight() - 1) {
            return;
        }

        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos(blockX, roadY, blockZ);
        clearAbove(level, mutablePosition, CLEARANCE_HEIGHT);
        level.setBlock(mutablePosition, Blocks.STONE_BRICKS.defaultBlockState(), UPDATE_FLAGS);
        buildFoundation(level, mutablePosition, Blocks.COBBLESTONE.defaultBlockState());
    }

    private static void generateHouse(WorldGenLevel level, int centerX, int centerZ) {
        int floorY = findSurfaceY(level, centerX, centerZ);
        if (floorY <= level.getMinBuildHeight()
                || floorY + HOUSE_WALL_HEIGHT + 3 >= level.getMaxBuildHeight()) {
            return;
        }

        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();
        for (int offsetX = -HOUSE_RADIUS; offsetX <= HOUSE_RADIUS; offsetX++) {
            for (int offsetZ = -HOUSE_RADIUS; offsetZ <= HOUSE_RADIUS; offsetZ++) {
                mutablePosition.set(centerX + offsetX, floorY, centerZ + offsetZ);
                level.setBlock(mutablePosition, Blocks.OAK_PLANKS.defaultBlockState(), UPDATE_FLAGS);
                buildFoundation(level, mutablePosition, Blocks.COBBLESTONE.defaultBlockState());
                clearAbove(level, mutablePosition, CLEARANCE_HEIGHT);
            }
        }

        generateWalls(level, centerX, floorY, centerZ);
        generateRoof(level, centerX, floorY, centerZ);
        generateInterior(level, centerX, floorY, centerZ);
        if (SpiritualRealmUtility.shouldGenerateSettlementVillager(
                level.getSeed(),
                Math.floorDiv(centerX, 16),
                Math.floorDiv(centerZ, 16)
        )) {
            generateVillager(level, centerX, floorY, centerZ);
        }
    }

    private static void generateWalls(WorldGenLevel level, int centerX, int floorY, int centerZ) {
        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();
        for (int wallY = 1; wallY <= HOUSE_WALL_HEIGHT; wallY++) {
            for (int offsetX = -HOUSE_RADIUS; offsetX <= HOUSE_RADIUS; offsetX++) {
                for (int offsetZ = -HOUSE_RADIUS; offsetZ <= HOUSE_RADIUS; offsetZ++) {
                    boolean edgeX = Math.abs(offsetX) == HOUSE_RADIUS;
                    boolean edgeZ = Math.abs(offsetZ) == HOUSE_RADIUS;
                    if (!edgeX && !edgeZ) {
                        continue;
                    }

                    mutablePosition.set(centerX + offsetX, floorY + wallY, centerZ + offsetZ);
                    boolean corner = edgeX && edgeZ;
                    boolean window = wallY == 2
                            && ((edgeX && offsetZ == 0) || (edgeZ && Math.abs(offsetX) == 2));
                    BlockState wallState = corner
                            ? Blocks.OAK_LOG.defaultBlockState()
                            : window ? Blocks.GLASS.defaultBlockState() : Blocks.OAK_PLANKS.defaultBlockState();
                    level.setBlock(mutablePosition, wallState, UPDATE_FLAGS);
                }
            }
        }

        placeDoor(level, centerX, floorY + 1, centerZ + HOUSE_RADIUS);
    }

    private static void generateRoof(WorldGenLevel level, int centerX, int floorY, int centerZ) {
        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();
        int lowerRoofY = floorY + HOUSE_WALL_HEIGHT + 1;
        for (int offsetX = -HOUSE_RADIUS - 1; offsetX <= HOUSE_RADIUS + 1; offsetX++) {
            for (int offsetZ = -HOUSE_RADIUS - 1; offsetZ <= HOUSE_RADIUS + 1; offsetZ++) {
                mutablePosition.set(centerX + offsetX, lowerRoofY, centerZ + offsetZ);
                level.setBlock(mutablePosition, Blocks.OAK_SLAB.defaultBlockState(), UPDATE_FLAGS);
            }
        }
        for (int offsetX = -HOUSE_RADIUS + 1; offsetX <= HOUSE_RADIUS - 1; offsetX++) {
            for (int offsetZ = -HOUSE_RADIUS + 1; offsetZ <= HOUSE_RADIUS - 1; offsetZ++) {
                mutablePosition.set(centerX + offsetX, lowerRoofY + 1, centerZ + offsetZ);
                level.setBlock(mutablePosition, Blocks.OAK_SLAB.defaultBlockState(), UPDATE_FLAGS);
            }
        }
    }

    private static void generateInterior(WorldGenLevel level, int centerX, int floorY, int centerZ) {
        level.setBlock(
                new BlockPos(centerX - 2, floorY + 1, centerZ - 2),
                Blocks.CRAFTING_TABLE.defaultBlockState(),
                UPDATE_FLAGS
        );
        level.setBlock(
                new BlockPos(centerX + 2, floorY + 1, centerZ - 2),
                Blocks.COMPOSTER.defaultBlockState(),
                UPDATE_FLAGS
        );
        level.setBlock(
                new BlockPos(centerX - 2, floorY + 1, centerZ + 1),
                Blocks.TORCH.defaultBlockState(),
                UPDATE_FLAGS
        );
        level.setBlock(
                new BlockPos(centerX + 2, floorY + 1, centerZ + 1),
                Blocks.TORCH.defaultBlockState(),
                UPDATE_FLAGS
        );

        BlockState bedFoot = Blocks.WHITE_BED.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.BED_PART, BedPart.FOOT);
        BlockState bedHead = bedFoot.setValue(BlockStateProperties.BED_PART, BedPart.HEAD);
        level.setBlock(new BlockPos(centerX, floorY + 1, centerZ), bedFoot, UPDATE_FLAGS);
        level.setBlock(new BlockPos(centerX, floorY + 1, centerZ - 1), bedHead, UPDATE_FLAGS);
    }

    private static void placeDoor(WorldGenLevel level, int blockX, int blockY, int blockZ) {
        BlockState lowerDoor = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER);
        BlockState upperDoor = lowerDoor.setValue(
                BlockStateProperties.DOUBLE_BLOCK_HALF,
                DoubleBlockHalf.UPPER
        );
        level.setBlock(new BlockPos(blockX, blockY, blockZ), lowerDoor, UPDATE_FLAGS);
        level.setBlock(new BlockPos(blockX, blockY + 1, blockZ), upperDoor, UPDATE_FLAGS);
    }

    private static void generateVillager(WorldGenLevel level, int centerX, int floorY, int centerZ) {
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) {
            return;
        }

        BlockPos spawnPosition = new BlockPos(centerX, floorY + 1, centerZ + 2);
        float yaw = (float) (
                SpiritualRealmUtility.toUnitDouble(SpiritualRealmUtility.mixSeed(
                        level.getSeed(),
                        centerX,
                        centerZ,
                        0x7111A6E2L
                )) * 360.0D
        );
        villager.moveTo(
                spawnPosition.getX() + 0.5D,
                spawnPosition.getY(),
                spawnPosition.getZ() + 0.5D,
                yaw,
                0.0F
        );
        villager.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(spawnPosition),
                MobSpawnType.STRUCTURE,
                null,
                null
        );
        villager.setPersistenceRequired();
        level.addFreshEntity(villager);
    }

    private static int findSurfaceY(WorldGenLevel level, int blockX, int blockZ) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ) - 1;
        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos(blockX, surfaceY, blockZ);
        while (surfaceY > level.getMinBuildHeight()) {
            BlockState blockState = level.getBlockState(mutablePosition);
            if (!blockState.is(BlockTags.LOGS) && !blockState.is(BlockTags.LEAVES)) {
                return surfaceY;
            }
            surfaceY--;
            mutablePosition.setY(surfaceY);
        }
        return surfaceY;
    }

    private static void clearAbove(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            int height
    ) {
        int originalY = mutablePosition.getY();
        for (int offsetY = 1; offsetY <= height; offsetY++) {
            int blockY = originalY + offsetY;
            if (blockY >= level.getMaxBuildHeight()) {
                break;
            }
            mutablePosition.setY(blockY);
            level.setBlock(mutablePosition, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        mutablePosition.setY(originalY);
    }

    private static void buildFoundation(
            WorldGenLevel level,
            BlockPos.MutableBlockPos mutablePosition,
            BlockState foundationState
    ) {
        int originalY = mutablePosition.getY();
        for (int depth = 1; depth <= MAXIMUM_FOUNDATION_DEPTH; depth++) {
            int blockY = originalY - depth;
            if (blockY <= level.getMinBuildHeight()) {
                break;
            }
            mutablePosition.setY(blockY);
            BlockState existingState = level.getBlockState(mutablePosition);
            if (existingState.isSolidRender(level, mutablePosition)
                    && existingState.getFluidState().isEmpty()) {
                break;
            }
            level.setBlock(mutablePosition, foundationState, UPDATE_FLAGS);
        }
        mutablePosition.setY(originalY);
    }
}
