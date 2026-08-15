package net.seraphina.ninecun_lingtai.common.player;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class PlayerThreeThousandRealmReturnPosition {
    private final ResourceLocation thisRealmId;
    private final double thisX;
    private final double thisY;
    private final double thisZ;
    private final float thisYRot;
    private final float thisXRot;

    public PlayerThreeThousandRealmReturnPosition(
            ResourceLocation realmId,
            double x,
            double y,
            double z,
            float yRot,
            float xRot
    ) {
        this.thisRealmId = Objects.requireNonNull(realmId, "realmId");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Return coordinates must be finite");
        }
        if (!Float.isFinite(yRot) || !Float.isFinite(xRot)) {
            throw new IllegalArgumentException("Return rotations must be finite");
        }
        this.thisX = x;
        this.thisY = y;
        this.thisZ = z;
        this.thisYRot = yRot;
        this.thisXRot = xRot;
    }

    public ResourceLocation getRealmId() {
        return this.thisRealmId;
    }

    public double getX() {
        return this.thisX;
    }

    public double getY() {
        return this.thisY;
    }

    public double getZ() {
        return this.thisZ;
    }

    public float getYRot() {
        return this.thisYRot;
    }

    public float getXRot() {
        return this.thisXRot;
    }
}
