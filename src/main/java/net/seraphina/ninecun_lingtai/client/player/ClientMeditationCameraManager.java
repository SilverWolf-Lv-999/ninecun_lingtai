package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ClientMeditationCameraManager {
    private static UUID controlledPlayerId;
    private static float lockedYRot;
    private static float lockedXRot;
    private static float lockedYHeadRot;
    private static float lockedYBodyRot;
    private static float cameraYRot;
    private static float cameraXRot;

    private ClientMeditationCameraManager() {
    }

    public static void start(LocalPlayer player) {
        controlledPlayerId = player.getUUID();
        lockedYRot = player.getYRot();
        lockedXRot = player.getXRot();
        lockedYHeadRot = player.yHeadRot;
        lockedYBodyRot = player.yBodyRot;
        cameraYRot = player.getViewYRot(1.0F);
        cameraXRot = player.getViewXRot(1.0F);
        player.stopUsingItem();
        lockPlayerRotation(player);
    }

    public static void stop(LocalPlayer player) {
        if (!isControlling(player)) {
            return;
        }
        float releasedYRot = cameraYRot;
        float releasedXRot = cameraXRot;
        reset();
        applyPlayerRotation(player, releasedYRot, releasedXRot, releasedYRot, releasedYRot);
    }

    public static void reset() {
        controlledPlayerId = null;
    }

    public static void lockPlayerRotation(LocalPlayer player) {
        if (isControlling(player)) {
            applyPlayerRotation(player, lockedYRot, lockedXRot, lockedYHeadRot, lockedYBodyRot);
        }
    }

    public static boolean rotateCamera(LocalPlayer player, double yRotDelta, double xRotDelta) {
        if (!isControlling(player)) {
            return false;
        }
        cameraYRot += (float) yRotDelta * 0.15F;
        cameraXRot = Mth.clamp(cameraXRot + (float) xRotDelta * 0.15F, -90.0F, 90.0F);
        return true;
    }

    public static float getCameraYRot(Entity cameraEntity, float partialTick) {
        return isControlling(cameraEntity) ? cameraYRot : cameraEntity.getViewYRot(partialTick);
    }

    public static float getCameraXRot(Entity cameraEntity, float partialTick) {
        return isControlling(cameraEntity) ? cameraXRot : cameraEntity.getViewXRot(partialTick);
    }

    public static Vec3 getCameraViewVector(Entity cameraEntity, float partialTick) {
        return isControlling(cameraEntity)
                ? Vec3.directionFromRotation(cameraXRot, cameraYRot)
                : cameraEntity.getViewVector(partialTick);
    }

    public static HitResult pick(Entity cameraEntity, double hitDistance, float partialTick, boolean hitFluids) {
        if (!isControlling(cameraEntity)) {
            return cameraEntity.pick(hitDistance, partialTick, hitFluids);
        }
        Vec3 eyePosition = cameraEntity.getEyePosition(partialTick);
        Vec3 viewVector = getCameraViewVector(cameraEntity, partialTick);
        Vec3 targetPosition = eyePosition.add(viewVector.scale(hitDistance));
        return cameraEntity.level().clip(new ClipContext(
                eyePosition,
                targetPosition,
                ClipContext.Block.OUTLINE,
                hitFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
                cameraEntity
        ));
    }

    private static boolean isControlling(Entity entity) {
        return controlledPlayerId != null
                && entity instanceof LocalPlayer
                && controlledPlayerId.equals(entity.getUUID());
    }

    private static void applyPlayerRotation(
            LocalPlayer player,
            float yRot,
            float xRot,
            float yHeadRot,
            float yBodyRot
    ) {
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.setYHeadRot(yHeadRot);
        player.setYBodyRot(yBodyRot);
        player.yRotO = yRot;
        player.xRotO = xRot;
        player.yHeadRotO = yHeadRot;
        player.yBodyRotO = yBodyRot;
    }
}
