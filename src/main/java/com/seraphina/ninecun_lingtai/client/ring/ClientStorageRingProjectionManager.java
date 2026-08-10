package com.seraphina.ninecun_lingtai.client.ring;

import com.seraphina.ninecun_lingtai.client.render.StorageRingProjectionRenderMeshManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.StorageRingCameraPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncStorageRingCameraPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.SyncStorageRingProjectionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientStorageRingProjectionManager {
    private static final double FIELD_OF_VIEW = 55.0D;
    private static final double MAXIMUM_INTERACTION_DISTANCE = 8.0D;
    private static final double RAYCAST_EPSILON = 0.000001D;

    private static UUID thisRingId;
    private static Vec3 thisCameraPosition = Vec3.ZERO;
    private static float thisCameraYRot;
    private static float thisCameraXRot;
    private static Map<BlockPos, BlockState> thisBlockStates = Map.of();
    private static long thisSnapshotVersion;

    private ClientStorageRingProjectionManager() {
    }

    public static void acceptProjection(SyncStorageRingProjectionPacket packet) {
        if (!packet.active()) {
            reset();
            return;
        }

        Map<BlockPos, BlockState> blockStates = new HashMap<>();
        for (SyncStorageRingProjectionPacket.BlockSnapshot snapshot : packet.blocks()) {
            BlockState blockState = Block.stateById(snapshot.blockStateId());
            if (!blockState.isAir()) {
                blockStates.put(snapshot.localPosition().immutable(), blockState);
            }
        }
        thisRingId = packet.ringId();
        thisCameraPosition = packet.cameraPosition();
        thisCameraYRot = packet.cameraYRot();
        thisCameraXRot = packet.cameraXRot();
        thisBlockStates = Collections.unmodifiableMap(blockStates);
        thisSnapshotVersion++;
    }

    public static void acceptCamera(SyncStorageRingCameraPacket packet) {
        if (!isActiveFor(packet.ringId())) {
            return;
        }
        thisCameraPosition = packet.cameraPosition();
        thisCameraYRot = packet.cameraYRot();
        thisCameraXRot = packet.cameraXRot();
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
        ModNetwork.sendToServer(new StorageRingCameraPacket(
                leftImpulse,
                forwardImpulse,
                movingUp,
                movingDown,
                thisCameraYRot,
                thisCameraXRot
        ));
    }

    public static void rotate(UUID ringId, double horizontalDelta, double verticalDelta) {
        if (!isActiveFor(ringId)) {
            return;
        }
        thisCameraYRot = Mth.wrapDegrees(thisCameraYRot + (float) horizontalDelta * 0.15F);
        thisCameraXRot = Mth.clamp(
                thisCameraXRot + (float) verticalDelta * 0.15F,
                -75.0F,
                75.0F
        );
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
        return raycast(direction);
    }

    public static Vec3 getCameraPosition() {
        return thisCameraPosition;
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

    public static void reset() {
        thisRingId = null;
        thisCameraPosition = Vec3.ZERO;
        thisCameraYRot = 0.0F;
        thisCameraXRot = 0.0F;
        thisBlockStates = Map.of();
        thisSnapshotVersion++;
        StorageRingProjectionRenderMeshManager.clear();
    }

    private static ProjectionTarget raycast(Vec3 direction) {
        ProjectionTarget closestTarget = null;
        double closestDistance = MAXIMUM_INTERACTION_DISTANCE;
        for (Map.Entry<BlockPos, BlockState> entry : thisBlockStates.entrySet()) {
            if (entry.getValue().isAir()) {
                continue;
            }

            RayIntersection intersection = new RayIntersection(0.0D, closestDistance);
            BlockPos position = entry.getKey();
            if (!intersection.clip(
                    thisCameraPosition.x,
                    direction.x,
                    position.getX(),
                    position.getX() + 1.0D,
                    Direction.WEST,
                    Direction.EAST
            ) || !intersection.clip(
                    thisCameraPosition.y,
                    direction.y,
                    position.getY(),
                    position.getY() + 1.0D,
                    Direction.DOWN,
                    Direction.UP
            ) || !intersection.clip(
                    thisCameraPosition.z,
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
                    thisCameraPosition.add(direction.scale(intersection.thisEnterDistance))
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
