package net.seraphina.ninecun_lingtai.client.animation;

import dev.kosmx.playerAnim.api.layered.PlayerAnimationFrame;
import dev.kosmx.playerAnim.core.util.Vec3f;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

final class AirWalkingLegAnimation extends PlayerAnimationFrame {
    private static final float MAX_LEG_ROTATION_DEGREES = 10.0F;
    private static final float MAX_SIDE_LEG_ROTATION_DEGREES = 5.0F;
    private static final float ROTATION_RESPONSE = 0.35F;
    private static final double FLYING_SPEED_MULTIPLIER = 2.0D;
    private static final float MINIMUM_FLYING_SPEED = 0.05F;

    private final Player player;
    private boolean active;
    private float previousForwardRotation;
    private float forwardRotation;
    private float previousSideRotation;
    private float sideRotation;

    AirWalkingLegAnimation(Player player) {
        this.player = player;
    }

    void setActive(boolean active) {
        this.active = active;
        if (!active) {
            this.previousForwardRotation = 0.0F;
            this.forwardRotation = 0.0F;
            this.previousSideRotation = 0.0F;
            this.sideRotation = 0.0F;
            clearPose();
        }
    }

    @Override
    public void tick() {
        if (!this.active) {
            return;
        }
        this.previousForwardRotation = this.forwardRotation;
        this.forwardRotation = Mth.lerp(
                ROTATION_RESPONSE,
                this.forwardRotation,
                getTargetForwardRotation()
        );
        this.previousSideRotation = this.sideRotation;
        this.sideRotation = Mth.lerp(
                ROTATION_RESPONSE,
                this.sideRotation,
                getTargetSideRotation()
        );
    }

    @Override
    public void setupAnim(float tickDelta) {
        if (!this.active) {
            clearPose();
            return;
        }
        float interpolatedForwardRotation = Mth.lerp(
                tickDelta,
                this.previousForwardRotation,
                this.forwardRotation
        );
        float interpolatedSideRotation = Mth.lerp(
                tickDelta,
                this.previousSideRotation,
                this.sideRotation
        );
        this.rightLeg.rot = new Vec3f(
                interpolatedForwardRotation * Mth.DEG_TO_RAD,
                0.0F,
                interpolatedSideRotation * Mth.DEG_TO_RAD
        );
        this.leftLeg.rot = new Vec3f(
                interpolatedForwardRotation * Mth.DEG_TO_RAD,
                0.0F,
                interpolatedSideRotation * Mth.DEG_TO_RAD
        );
    }

    private float getTargetForwardRotation() {
        return getHorizontalSpeedRatio(true) * MAX_LEG_ROTATION_DEGREES;
    }

    private float getTargetSideRotation() {
        return getHorizontalSpeedRatio(false) * MAX_SIDE_LEG_ROTATION_DEGREES;
    }

    private float getHorizontalSpeedRatio(boolean forward) {
        double flyingSpeed = Math.max(
                this.player.getAbilities().getFlyingSpeed(),
                MINIMUM_FLYING_SPEED
        ) * FLYING_SPEED_MULTIPLIER;
        Vec3 lookAngle = this.player.getLookAngle();
        double horizontalLookLength = Math.sqrt(
                lookAngle.x * lookAngle.x + lookAngle.z * lookAngle.z
        );
        if (horizontalLookLength < 1.0E-6D) {
            return 0.0F;
        }
        Vec3 horizontalLook = new Vec3(
                lookAngle.x / horizontalLookLength,
                0.0D,
                lookAngle.z / horizontalLookLength
        );
        Vec3 horizontalMovement = new Vec3(
                this.player.getDeltaMovement().x,
                0.0D,
                this.player.getDeltaMovement().z
        );
        Vec3 direction = forward
                ? horizontalLook
                : new Vec3(horizontalLook.z, 0.0D, -horizontalLook.x);
        float speedRatio = (float) Mth.clamp(
                horizontalMovement.dot(direction) / flyingSpeed,
                -1.0D,
                1.0D
        );
        return speedRatio;
    }

    private void clearPose() {
        this.rightLeg.rot = null;
        this.leftLeg.rot = null;
    }
}
