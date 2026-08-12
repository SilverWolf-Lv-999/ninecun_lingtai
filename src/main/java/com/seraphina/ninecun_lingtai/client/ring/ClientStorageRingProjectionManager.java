package com.seraphina.ninecun_lingtai.client.ring;

import com.seraphina.ninecun_lingtai.client.render.StorageRingProjectionRenderMeshManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.StorageRingCameraPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncStorageRingProjectionPacket;
import com.seraphina.ninecun_lingtai.utility.MovementUtility;
import com.seraphina.ninecun_lingtai.utility.RingStorageUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ClientStorageRingProjectionManager {
    private static final double FIELD_OF_VIEW = 55.0D;
    private static final double MAXIMUM_INTERACTION_DISTANCE = 8.0D;
    private static final double RAYCAST_EPSILON = 0.000001D;
    private static final float MIDDLE_DRAG_ROTATION_SENSITIVITY = 0.60F;

    private static UUID thisRingId;
    private static Vec3 thisPreviousCameraPosition = Vec3.ZERO;
    private static Vec3 thisCameraPosition = Vec3.ZERO;
    private static float thisCameraYRot;
    private static float thisCameraXRot;
    private static Map<BlockPos, BlockState> thisBlockStates = Map.of();
    private static long thisSnapshotVersion;
    private static boolean thisClientControlsCamera;
    private static float thisLeftImpulse;
    private static float thisForwardImpulse;
    private static boolean thisMovingUp;
    private static boolean thisMovingDown;

    private ClientStorageRingProjectionManager() {
    }

    public static void acceptProjection(SyncStorageRingProjectionPacket packet) {
        if (!packet.active()) {
            reset();
            return;
        }

        boolean resetCamera = !isActiveFor(packet.ringId()) || !thisClientControlsCamera;
        Map<BlockPos, BlockState> blockStates = resetCamera
                ? createInitialBlockStates()
                : new HashMap<>(thisBlockStates);
        Set<BlockPos> receivedBlockPositions = new HashSet<>(packet.blocks().size());
        boolean blockStatesChanged = resetCamera;
        for (SyncStorageRingProjectionPacket.BlockSnapshot snapshot : packet.blocks()) {
            BlockState blockState = Block.stateById(snapshot.blockStateId());
            if (!blockState.isAir()) {
                BlockPos localPosition = snapshot.localPosition().immutable();
                receivedBlockPositions.add(localPosition);
                BlockState previousBlockState = blockStates.put(localPosition, blockState);
                if (!blockState.equals(previousBlockState)) {
                    blockStatesChanged = true;
                }
            }
        }
        if (removeMissingFloorBlocks(blockStates, receivedBlockPositions, packet.cameraPosition())) {
            blockStatesChanged = true;
        }
        thisRingId = packet.ringId();
        if (resetCamera) {
            thisCameraPosition = packet.cameraPosition();
            thisPreviousCameraPosition = thisCameraPosition;
            thisCameraYRot = packet.cameraYRot();
            thisCameraXRot = packet.cameraXRot();
            thisClientControlsCamera = false;
            clearCameraInput();
        }
        if (blockStatesChanged) {
            thisBlockStates = Collections.unmodifiableMap(blockStates);
            thisSnapshotVersion++;
        }
    }

    public static boolean isActiveFor(UUID ringId) {
        return thisRingId != null && thisRingId.equals(ringId);
    }

    public static void sendCameraInput(
            UUID ringId,
            float leftImpulse,
            float forwardImpulse,
            boolean movingUp,
            boolean movingDown
    ) {
        if (!isActiveFor(ringId)) {
            return;
        }
        thisLeftImpulse = Mth.clamp(leftImpulse, -1.0F, 1.0F);
        thisForwardImpulse = Mth.clamp(forwardImpulse, -1.0F, 1.0F);
        thisMovingUp = movingUp;
        thisMovingDown = movingDown;
        thisClientControlsCamera = true;
        ModNetwork.sendToServer(new StorageRingCameraPacket(
                thisLeftImpulse,
                thisForwardImpulse,
                thisMovingUp,
                thisMovingDown,
                thisCameraYRot,
                thisCameraXRot
        ));
    }

    public static void rotate(UUID ringId, double horizontalDelta, double verticalDelta) {
        rotate(ringId, horizontalDelta, verticalDelta, MIDDLE_DRAG_ROTATION_SENSITIVITY);
    }

    public static void rotate(
            UUID ringId,
            double horizontalDelta,
            double verticalDelta,
            float sensitivity
    ) {
        if (!isActiveFor(ringId)) {
            return;
        }
        thisCameraYRot = Mth.wrapDegrees(
                thisCameraYRot + (float) horizontalDelta * sensitivity
        );
        thisCameraXRot = Mth.clamp(
                thisCameraXRot + (float) verticalDelta * sensitivity,
                -75.0F,
                75.0F
        );
        thisClientControlsCamera = true;
    }

    public static void tickCameraMotion() {
        if (thisRingId == null) {
            return;
        }

        thisPreviousCameraPosition = thisCameraPosition;
        moveCamera();
    }

    public static ProjectionTarget pick(
            UUID ringId,
            int viewportLeft,
            int viewportTop,
            int viewportWidth,
            int viewportHeight,
            double mouseX,
            double mouseY
    ) {
        if (!isActiveFor(ringId)) {
            return null;
        }

        double screenX = ((mouseX - viewportLeft) / viewportWidth) * 2.0D - 1.0D;
        double screenY = 1.0D - ((mouseY - viewportTop) / viewportHeight) * 2.0D;
        double tangent = Math.tan(Math.toRadians(FIELD_OF_VIEW * 0.5D));
        double aspectRatio = (double) viewportWidth / viewportHeight;

        Vec3 forward = Vec3.directionFromRotation(thisCameraXRot, thisCameraYRot).normalize();
        Vec3 right = forward.cross(new Vec3(0.0D, 1.0D, 0.0D)).normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 direction = forward
                .add(right.scale(screenX * aspectRatio * tangent))
                .add(up.scale(screenY * tangent))
                .normalize();
        return raycast(getCameraPosition(), direction);
    }

    public static Vec3 getCameraPosition() {
        return thisCameraPosition;
    }

    public static Vec3 getInterpolatedCameraPosition(float partialTick) {
        return thisPreviousCameraPosition.lerp(
                thisCameraPosition,
                Mth.clamp(partialTick, 0.0F, 1.0F)
        );
    }

    public static float getCameraYRot() {
        return thisCameraYRot;
    }

    public static float getCameraXRot() {
        return thisCameraXRot;
    }

    public static Map<BlockPos, BlockState> getBlockStates() {
        return thisBlockStates;
    }

    public static long getSnapshotVersion() {
        return thisSnapshotVersion;
    }

    public static void removeBlock(UUID ringId, BlockPos localPosition) {
        if (!isActiveFor(ringId) || !thisBlockStates.containsKey(localPosition)) {
            return;
        }

        Map<BlockPos, BlockState> blockStates = new HashMap<>(thisBlockStates);
        blockStates.remove(localPosition);
        thisBlockStates = Collections.unmodifiableMap(blockStates);
        thisSnapshotVersion++;
    }

    public static void reset() {
        ClientStorageRingImmersionManager.reset();
        thisRingId = null;
        thisPreviousCameraPosition = Vec3.ZERO;
        thisCameraPosition = Vec3.ZERO;
        thisCameraYRot = 0.0F;
        thisCameraXRot = 0.0F;
        thisBlockStates = Map.of();
        thisSnapshotVersion++;
        thisClientControlsCamera = false;
        clearCameraInput();
        StorageRingProjectionRenderMeshManager.clear();
    }

    private static void moveCamera() {
        if (thisLeftImpulse == 0.0F
                && thisForwardImpulse == 0.0F
                && !thisMovingUp
                && !thisMovingDown) {
            return;
        }

        float verticalImpulse = (thisMovingUp ? 1.0F : 0.0F) - (thisMovingDown ? 1.0F : 0.0F);
        Vec3 movement = MovementUtility.getRelativeMovement(
                thisLeftImpulse,
                thisForwardImpulse,
                verticalImpulse,
                (float) RingStorageUtility.CAMERA_SPEED_PER_TICK,
                thisCameraYRot
        );
        Vec3 nextPosition = clampCameraPosition(thisCameraPosition.add(movement));
        if (thisCameraPosition.distanceToSqr(nextPosition) > RAYCAST_EPSILON) {
            thisCameraPosition = nextPosition;
        }
    }

    private static Map<BlockPos, BlockState> createInitialBlockStates() {
        Map<BlockPos, BlockState> blockStates = new HashMap<>(
                RingStorageUtility.POCKET_SIZE * RingStorageUtility.POCKET_SIZE
        );
        BlockState floorBlockState = Blocks.SMOOTH_STONE.defaultBlockState();
        for (int x = 0; x < RingStorageUtility.POCKET_SIZE; x++) {
            for (int z = 0; z < RingStorageUtility.POCKET_SIZE; z++) {
                blockStates.put(new BlockPos(x, 0, z), floorBlockState);
            }
        }
        return blockStates;
    }

    private static boolean removeMissingFloorBlocks(
            Map<BlockPos, BlockState> blockStates,
            Set<BlockPos> receivedBlockPositions,
            Vec3 snapshotCameraPosition
    ) {
        int minimumX = Math.max(
                0,
                Mth.floor(snapshotCameraPosition.x) - RingStorageUtility.PROJECTION_HORIZONTAL_RADIUS
        );
        int maximumX = Math.min(
                RingStorageUtility.POCKET_SIZE - 1,
                Mth.floor(snapshotCameraPosition.x) + RingStorageUtility.PROJECTION_HORIZONTAL_RADIUS
        );
        int minimumZ = Math.max(
                0,
                Mth.floor(snapshotCameraPosition.z) - RingStorageUtility.PROJECTION_HORIZONTAL_RADIUS
        );
        int maximumZ = Math.min(
                RingStorageUtility.POCKET_SIZE - 1,
                Mth.floor(snapshotCameraPosition.z) + RingStorageUtility.PROJECTION_HORIZONTAL_RADIUS
        );
        boolean changed = false;
        for (int x = minimumX; x <= maximumX; x++) {
            for (int z = minimumZ; z <= maximumZ; z++) {
                BlockPos floorPosition = new BlockPos(x, 0, z);
                if (!receivedBlockPositions.contains(floorPosition)
                        && blockStates.get(floorPosition) != null) {
                    blockStates.remove(floorPosition);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static void clearCameraInput() {
        thisLeftImpulse = 0.0F;
        thisForwardImpulse = 0.0F;
        thisMovingUp = false;
        thisMovingDown = false;
    }

    private static Vec3 clampCameraPosition(Vec3 cameraPosition) {
        return new Vec3(
                Mth.clamp(cameraPosition.x, 0.5D, RingStorageUtility.POCKET_SIZE - 0.5D),
                Mth.clamp(
                        cameraPosition.y,
                        RingStorageUtility.MINIMUM_CAMERA_Y - RingStorageUtility.FLOOR_Y,
                        RingStorageUtility.MAXIMUM_CAMERA_Y - RingStorageUtility.FLOOR_Y
                ),
                Mth.clamp(cameraPosition.z, 0.5D, RingStorageUtility.POCKET_SIZE - 0.5D)
        );
    }

    private static ProjectionTarget raycast(Vec3 cameraPosition, Vec3 direction) {
        ProjectionTarget closestTarget = null;
        double closestDistance = MAXIMUM_INTERACTION_DISTANCE;
        for (Map.Entry<BlockPos, BlockState> entry : thisBlockStates.entrySet()) {
            if (entry.getValue().isAir()) {
                continue;
            }

            RayIntersection intersection = new RayIntersection(0.0D, closestDistance);
            BlockPos position = entry.getKey();
            if (!intersection.clip(
                    cameraPosition.x,
                    direction.x,
                    position.getX(),
                    position.getX() + 1.0D,
                    Direction.WEST,
                    Direction.EAST
            ) || !intersection.clip(
                    cameraPosition.y,
                    direction.y,
                    position.getY(),
                    position.getY() + 1.0D,
                    Direction.DOWN,
                    Direction.UP
            ) || !intersection.clip(
                    cameraPosition.z,
                    direction.z,
                    position.getZ(),
                    position.getZ() + 1.0D,
                    Direction.NORTH,
                    Direction.SOUTH
            ) || intersection.thisEnterDirection == null) {
                continue;
            }

            closestDistance = intersection.thisEnterDistance;
            closestTarget = new ProjectionTarget(
                    position,
                    intersection.thisEnterDirection,
                    cameraPosition.add(direction.scale(intersection.thisEnterDistance))
            );
        }
        return closestTarget;
    }

    private static final class RayIntersection {
        private double thisEnterDistance;
        private double thisExitDistance;
        private Direction thisEnterDirection;

        private RayIntersection(double enterDistance, double exitDistance) {
            this.thisEnterDistance = enterDistance;
            this.thisExitDistance = exitDistance;
        }

        private boolean clip(
                double origin,
                double direction,
                double minimum,
                double maximum,
                Direction minimumDirection,
                Direction maximumDirection
        ) {
            if (Math.abs(direction) < RAYCAST_EPSILON) {
                return origin >= minimum && origin <= maximum;
            }

            double nearDistance = (minimum - origin) / direction;
            double farDistance = (maximum - origin) / direction;
            Direction nearDirection = minimumDirection;
            Direction farDirection = maximumDirection;
            if (nearDistance > farDistance) {
                double temporaryDistance = nearDistance;
                nearDistance = farDistance;
                farDistance = temporaryDistance;

                Direction temporaryDirection = nearDirection;
                nearDirection = farDirection;
                farDirection = temporaryDirection;
            }

            if (nearDistance > this.thisEnterDistance) {
                this.thisEnterDistance = nearDistance;
                this.thisEnterDirection = nearDirection;
            }
            this.thisExitDistance = Math.min(this.thisExitDistance, farDistance);
            return this.thisEnterDistance <= this.thisExitDistance;
        }
    }

    public record ProjectionTarget(
            BlockPos localPosition,
            Direction direction,
            Vec3 localHitLocation
    ) {
    }
}
