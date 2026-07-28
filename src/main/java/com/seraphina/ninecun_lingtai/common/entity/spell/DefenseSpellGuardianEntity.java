package com.seraphina.ninecun_lingtai.common.entity.spell;

import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseSpellData;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellShape;
import com.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

public final class DefenseSpellGuardianEntity extends PathfinderMob {
    private static final String OWNER_UUID_KEY = "OwnerUuid";
    private static final String KIND_KEY = "Kind";
    private static final String CURRENT_POINTS_KEY = "CurrentPoints";
    private static final String MAXIMUM_POINTS_KEY = "MaximumPoints";
    private static final String ELEMENT_MASK_KEY = "ElementMask";
    private static final String REMAINING_TICKS_KEY = "RemainingTicks";
    private static final String LAST_HURT_TICK_KEY = "LastHurtTick";
    private static final String RECOVERY_DISABLED_UNTIL_KEY = "RecoveryDisabledUntil";

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
            DefenseSpellGuardianEntity.class,
            EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(
            DefenseSpellGuardianEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Float> CURRENT_POINTS = SynchedEntityData.defineId(
            DefenseSpellGuardianEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> MAXIMUM_POINTS = SynchedEntityData.defineId(
            DefenseSpellGuardianEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Integer> ELEMENT_MASK = SynchedEntityData.defineId(
            DefenseSpellGuardianEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Integer> REMAINING_TICKS = SynchedEntityData.defineId(
            DefenseSpellGuardianEntity.class,
            EntityDataSerializers.INT
    );

    private int lastHurtTick;
    private int recoveryDisabledUntil;

    public DefenseSpellGuardianEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public void initialize(
            ServerPlayer owner,
            DefenseSpellKind kind,
            double maximumPoints,
            int elementMask,
            int durationTicks
    ) {
        if (kind.getShape() != DefenseSpellShape.GUARDIAN) {
            throw new IllegalArgumentException("Defense spell guardian requires a guardian kind");
        }
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        this.entityData.set(KIND, kind.ordinal());
        this.entityData.set(MAXIMUM_POINTS, (float) maximumPoints);
        this.entityData.set(CURRENT_POINTS, (float) maximumPoints);
        this.entityData.set(ELEMENT_MASK, elementMask);
        this.entityData.set(REMAINING_TICKS, durationTicks);
        this.lastHurtTick = this.tickCount;
        updateMovementMode(kind);
        this.setCustomName(Component.literal(kind.getChineseName()));
        this.setCustomNameVisible(false);
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(OWNER_UUID);
    }

    public Optional<ServerPlayer> getOwnerPlayer() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        return getOwnerUuid()
                .map(serverLevel::getPlayerByUUID)
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast);
    }

    public DefenseSpellKind getSpellKind() {
        int kindIndex = this.entityData.get(KIND);
        DefenseSpellKind[] kinds = DefenseSpellKind.values();
        return kindIndex >= 0 && kindIndex < kinds.length ? kinds[kindIndex] : DefenseSpellKind.WOOD_GUARDIAN;
    }

    public double getCurrentPoints() {
        return this.entityData.get(CURRENT_POINTS);
    }

    public double getMaximumPoints() {
        return this.entityData.get(MAXIMUM_POINTS);
    }

    public int getElementMask() {
        return this.entityData.get(ELEMENT_MASK);
    }

    public int getRemainingTicks() {
        return this.entityData.get(REMAINING_TICKS);
    }

    public DefenseSpellSnapshot createSnapshot() {
        return new DefenseSpellSnapshot(
                getSpellKind(),
                getCurrentPoints(),
                getMaximumPoints(),
                getElementMask(),
                getRemainingTicks()
        );
    }

    public double consumePoints(double amount) {
        if (amount <= 0.0D) {
            return 0.0D;
        }
        double consumed = Math.min(getCurrentPoints(), amount);
        setCurrentPoints(getCurrentPoints() - consumed);
        return consumed;
    }

    public void recordHit(Optional<ElementType> attackElement) {
        this.lastHurtTick = this.tickCount;
        if (attackElement.filter(element -> element == ElementType.CHAOS).isPresent()) {
            this.recoveryDisabledUntil = this.tickCount + 100;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(KIND, DefenseSpellKind.WOOD_GUARDIAN.ordinal());
        this.entityData.define(CURRENT_POINTS, 0.0F);
        this.entityData.define(MAXIMUM_POINTS, 1.0F);
        this.entityData.define(ELEMENT_MASK, 0);
        this.entityData.define(REMAINING_TICKS, 1);
    }

    @Override
    public void tick() {
        super.tick();
        DefenseSpellKind kind = getSpellKind();
        updateMovementMode(kind);
        if (this.level().isClientSide) {
            return;
        }
        Optional<ServerPlayer> ownerOptional = getOwnerPlayer();
        if (ownerOptional.isEmpty() || !ownerOptional.get().isAlive()) {
            this.discard();
            return;
        }
        ServerPlayer owner = ownerOptional.get();
        int remainingTicks = getRemainingTicks() - 1;
        if (remainingTicks <= 0) {
            this.discard();
            return;
        }
        this.entityData.set(REMAINING_TICKS, remainingTicks);
        followOwner(owner, kind);
        if (this.tickCount % 20 == 0) {
            repairWithSpiritualEnergy(owner, kind);
            ModNetwork.sendDefenseSpellState(owner);
        }
    }

    @Override
    public boolean isAlliedTo(Entity entity) {
        if (entity instanceof Player player && getOwnerUuid().filter(player.getUUID()::equals).isPresent()) {
            return true;
        }
        return getOwnerPlayer().map(owner -> owner.isAlliedTo(entity)).orElse(super.isAlliedTo(entity));
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
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            getOwnerPlayer().ifPresent(owner -> {
                PlayerDefenseSpellData.clearGuardian(owner, this.getUUID());
                ModNetwork.sendDefenseSpellState(owner);
            });
        }
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        getOwnerUuid().ifPresent(ownerUuid -> compoundTag.putUUID(OWNER_UUID_KEY, ownerUuid));
        compoundTag.putString(KIND_KEY, getSpellKind().getSerializedName());
        compoundTag.putDouble(CURRENT_POINTS_KEY, getCurrentPoints());
        compoundTag.putDouble(MAXIMUM_POINTS_KEY, getMaximumPoints());
        compoundTag.putInt(ELEMENT_MASK_KEY, getElementMask());
        compoundTag.putInt(REMAINING_TICKS_KEY, getRemainingTicks());
        compoundTag.putInt(LAST_HURT_TICK_KEY, this.lastHurtTick);
        compoundTag.putInt(RECOVERY_DISABLED_UNTIL_KEY, this.recoveryDisabledUntil);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(OWNER_UUID_KEY, Tag.TAG_INT_ARRAY)) {
            this.entityData.set(OWNER_UUID, Optional.of(compoundTag.getUUID(OWNER_UUID_KEY)));
        }
        DefenseSpellKind.fromSerializedName(compoundTag.getString(KIND_KEY))
                .filter(kind -> kind.getShape() == DefenseSpellShape.GUARDIAN)
                .ifPresent(kind -> this.entityData.set(KIND, kind.ordinal()));
        double maximum = Math.max(1.0D, compoundTag.getDouble(MAXIMUM_POINTS_KEY));
        this.entityData.set(MAXIMUM_POINTS, (float) maximum);
        this.entityData.set(CURRENT_POINTS, (float) Mth.clamp(
                compoundTag.getDouble(CURRENT_POINTS_KEY),
                0.0D,
                maximum
        ));
        this.entityData.set(ELEMENT_MASK, compoundTag.getInt(ELEMENT_MASK_KEY));
        this.entityData.set(REMAINING_TICKS, Math.max(1, compoundTag.getInt(REMAINING_TICKS_KEY)));
        this.lastHurtTick = compoundTag.getInt(LAST_HURT_TICK_KEY);
        this.recoveryDisabledUntil = compoundTag.getInt(RECOVERY_DISABLED_UNTIL_KEY);
        this.setPersistenceRequired();
        updateMovementMode(getSpellKind());
    }

    private void setCurrentPoints(double currentPoints) {
        this.entityData.set(CURRENT_POINTS, (float) Math.max(0.0D, currentPoints));
    }

    private void updateMovementMode(DefenseSpellKind kind) {
        boolean flying = kind == DefenseSpellKind.FIRE_GUARDIAN
                || kind == DefenseSpellKind.FOUR_PHASE_GUARDIAN;
        this.setNoGravity(flying);
        this.noPhysics = false;
    }

    private void followOwner(ServerPlayer owner, DefenseSpellKind kind) {
        Vec3 look = owner.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() < 1.0E-6D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            look = look.normalize();
        }
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        Vec3 desired = switch (kind) {
            case WOOD_GUARDIAN -> owner.position().add(side.scale(1.3D));
            case FIRE_GUARDIAN -> owner.position().add(look.scale(1.2D)).add(0.0D, 1.5D, 0.0D);
            case EARTH_GUARDIAN -> owner.position().add(look.scale(1.8D));
            case FOUR_PHASE_GUARDIAN -> owner.position().add(side.scale(-1.6D)).add(0.0D, 0.4D, 0.0D);
            default -> owner.position();
        };
        double distanceSquared = this.distanceToSqr(desired);
        if (distanceSquared > 144.0D) {
            this.teleportTo(desired.x, desired.y, desired.z);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (kind == DefenseSpellKind.FIRE_GUARDIAN || kind == DefenseSpellKind.FOUR_PHASE_GUARDIAN) {
            this.setDeltaMovement(desired.subtract(this.position()).scale(0.18D));
            this.hasImpulse = true;
        } else if (distanceSquared > 4.0D) {
            this.getNavigation().moveTo(desired.x, desired.y, desired.z, kind == DefenseSpellKind.EARTH_GUARDIAN ? 0.75D : 1.0D);
        }
    }

    private void repairWithSpiritualEnergy(ServerPlayer owner, DefenseSpellKind kind) {
        if (this.tickCount < this.recoveryDisabledUntil
                || this.tickCount - this.lastHurtTick < 20
                || getCurrentPoints() >= getMaximumPoints()) {
            return;
        }
        double repaired = DefenseSpellUtility.repairWithSpiritualEnergy(
                owner,
                kind,
                getElementMask(),
                getCurrentPoints(),
                getMaximumPoints()
        );
        if (repaired <= 0.0D) {
            return;
        }
        setCurrentPoints(Math.min(getMaximumPoints(), getCurrentPoints() + repaired));
        ModNetwork.sendCultivationInfo(owner);
    }
}
