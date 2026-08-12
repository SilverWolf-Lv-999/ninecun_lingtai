package net.seraphina.ninecun_lingtai.utility;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class MovementUtility {

    /**
     * Matches Entity#moveRelative's input-vector transform.
     */
    public static Vec3 getRelativeMovement(
            float leftImpulse,
            float forwardImpulse,
            float verticalImpulse,
            float speed,
            float yRot
    ) {
        Vec3 input = new Vec3(leftImpulse, verticalImpulse, forwardImpulse);
        double inputLengthSquared = input.lengthSqr();
        if (inputLengthSquared < 0.0000001D) {
            return Vec3.ZERO;
        }

        Vec3 normalizedInput = (inputLengthSquared > 1.0D ? input.normalize() : input).scale(speed);
        float sinYRot = Mth.sin(yRot * ((float) Math.PI / 180.0F));
        float cosYRot = Mth.cos(yRot * ((float) Math.PI / 180.0F));
        return new Vec3(
                normalizedInput.x * cosYRot - normalizedInput.z * sinYRot,
                normalizedInput.y,
                normalizedInput.z * cosYRot + normalizedInput.x * sinYRot
        );
    }
}
