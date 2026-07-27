package com.seraphina.ninecun_lingtai.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.UUID;

public final class SoulEntity extends PathfinderMob {
    private static final String OWNER_UUID_KEY = "OwnerUuid";
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
            SoulEntity.class,
            EntityDataSerializers.OPTIONAL_UUID
    );

    public SoulEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(OWNER_UUID);
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.entityData.set(OWNER_UUID, Optional.of(ownerUuid));
    }

    public boolean isOwnedBy(Player player) {
        return getOwnerUuid()
                .map(ownerUuid -> ownerUuid.equals(player.getUUID()))
                .orElse(false);
    }

    public void moveSoul(float leftImpulse, float forwardImpulse, float verticalImpulse, float yRot, float xRot) {
        yRot = Mth.wrapDegrees(yRot);
        xRot = Mth.clamp(xRot, -90.0F, 90.0F);
        this.setYRot(yRot);
        this.setXRot(xRot);
        this.setYHeadRot(yRot);
        this.setYBodyRot(yRot);
        this.yRotO = yRot;
        this.xRotO = xRot;
        this.yHeadRotO = yRot;
        this.yBodyRotO = yRot;

        float horizontalLength = (float) Math.sqrt(
                leftImpulse * leftImpulse + forwardImpulse * forwardImpulse
        );
        if (horizontalLength > 1.0F) {
            leftImpulse /= horizontalLength;
            forwardImpulse /= horizontalLength;
        }

        double yawRadians = Math.toRadians(yRot);
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0.0D, Math.cos(yawRadians));
        Vec3 left = new Vec3(Math.cos(yawRadians), 0.0D, Math.sin(yawRadians));
        Vec3 movement = forward.scale(forwardImpulse)
                .add(left.scale(leftImpulse))
                .add(0.0D, verticalImpulse, 0.0D)
                .scale(0.35D);
        this.setDeltaMovement(movement);
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    protected void dropEquipment() {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        getOwnerUuid().ifPresent(ownerUuid -> compoundTag.putUUID(OWNER_UUID_KEY, ownerUuid));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(OWNER_UUID_KEY, Tag.TAG_INT_ARRAY)) {
            setOwnerUuid(compoundTag.getUUID(OWNER_UUID_KEY));
        }
        this.setNoAi(true);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }
}
