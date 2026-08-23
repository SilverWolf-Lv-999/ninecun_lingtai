package net.seraphina.ninecun_lingtai.common.entity.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

/**
 * A short-lived, server-synchronised sword-qi slash.
 *
 * <p>The entity owns the slash position, facing and lifetime. The renderer only draws its
 * local-space blade mesh, so the effect is never billboarded towards the camera.</p>
 */
public final class SwordQiEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final String DIRECTION_X_KEY = "DirectionX";
    private static final String DIRECTION_Y_KEY = "DirectionY";
    private static final String DIRECTION_Z_KEY = "DirectionZ";
    private static final String ARC_ANGLE_KEY = "ArcAngle";
    private static final String AGE_KEY = "Age";

    private static final int LIFETIME_TICKS = 18;
    private static final double SPEED = 0.72D;
    private static final EntityDataAccessor<Float> ARC_ANGLE = SynchedEntityData.defineId(
            SwordQiEntity.class,
            EntityDataSerializers.FLOAT
    );

    private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
    private int age;

    public SwordQiEntity(EntityType<? extends SwordQiEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public void initialize(Vec3 direction, float arcAngleDegrees) {
        this.direction = normalizeDirection(direction);
        this.entityData.set(ARC_ANGLE, clampArcAngle(arcAngleDegrees));
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(this.direction.scale(SPEED));
        updateRotation();
    }

    public Vec3 getForwardDirection() {
        return this.direction;
    }

    public float getArcAngleDegrees() {
        return this.entityData.get(ARC_ANGLE);
    }

    public float getProgress(float partialTick) {
        return Math.min(1.0F, (this.age + partialTick) / (float) LIFETIME_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(this.direction.scale(SPEED));
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.age++;
        if (this.age >= LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void defineSynchedData() {
        this.entityData.define(ARC_ANGLE, 60.0F);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        this.direction = normalizeDirection(new Vec3(
                compoundTag.getDouble(DIRECTION_X_KEY),
                compoundTag.getDouble(DIRECTION_Y_KEY),
                compoundTag.getDouble(DIRECTION_Z_KEY)
        ));
        this.entityData.set(ARC_ANGLE, clampArcAngle(compoundTag.getFloat(ARC_ANGLE_KEY)));
        this.age = Math.max(0, compoundTag.getInt(AGE_KEY));
        this.setDeltaMovement(this.direction.scale(SPEED));
        updateRotation();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putDouble(DIRECTION_X_KEY, this.direction.x);
        compoundTag.putDouble(DIRECTION_Y_KEY, this.direction.y);
        compoundTag.putDouble(DIRECTION_Z_KEY, this.direction.z);
        compoundTag.putFloat(ARC_ANGLE_KEY, this.getArcAngleDegrees());
        compoundTag.putInt(AGE_KEY, this.age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeDouble(this.direction.x);
        buffer.writeDouble(this.direction.y);
        buffer.writeDouble(this.direction.z);
        buffer.writeFloat(this.getArcAngleDegrees());
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.direction = normalizeDirection(new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        ));
        this.entityData.set(ARC_ANGLE, clampArcAngle(buffer.readFloat()));
        this.setDeltaMovement(this.direction.scale(SPEED));
        updateRotation();
    }

    private void updateRotation() {
        this.setYRot((float) Math.toDegrees(Math.atan2(-this.direction.x, this.direction.z)));
        this.setXRot((float) Math.toDegrees(Math.asin(-this.direction.y)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    private static Vec3 normalizeDirection(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-8D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize();
    }

    private static float clampArcAngle(float angleDegrees) {
        if (!Float.isFinite(angleDegrees)) {
            return 60.0F;
        }
        return Math.max(12.0F, Math.min(170.0F, Math.abs(angleDegrees)));
    }
}

