package com.seraphina.ninecun_lingtai.common.entity.tribulation;

import com.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import com.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import com.seraphina.ninecun_lingtai.utility.TribulationUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import java.util.Optional;
import java.util.UUID;

/**
 * A damage-aware tribulation strike that is also a vanilla LightningBolt.
 * The inherited client renderer is intentionally used without a custom visual renderer.
 */
public final class ThunderTribulationEntity extends LightningBolt {
    private static final String OWNER_UUID_KEY = "OwnerUuid";
    private static final String STRIKE_INDEX_KEY = "StrikeIndex";
    private static final String DAMAGE_KEY = "Damage";
    private static final String INTENSITY_KEY = "Intensity";
    private static final String REMAINING_TICKS_KEY = "RemainingTicks";

    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
            ThunderTribulationEntity.class,
            EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Integer> STRIKE_INDEX = SynchedEntityData.defineId(
            ThunderTribulationEntity.class,
            EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(
            ThunderTribulationEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Float> INTENSITY = SynchedEntityData.defineId(
            ThunderTribulationEntity.class,
            EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<Integer> REMAINING_TICKS = SynchedEntityData.defineId(
            ThunderTribulationEntity.class,
            EntityDataSerializers.INT
    );

    public ThunderTribulationEntity(EntityType<? extends ThunderTribulationEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public void initialize(
            ServerPlayer owner,
            int strikeIndex,
            float damage,
            float intensity
    ) {
        this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
        this.entityData.set(STRIKE_INDEX, strikeIndex);
        this.entityData.set(DAMAGE, damage);
        this.entityData.set(INTENSITY, intensity);
        this.entityData.set(REMAINING_TICKS, CommonConfig.TRIBULATION_LIGHTNING_LIFETIME_TICKS.get());
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(OWNER_UUID);
    }

    public int getStrikeIndex() {
        return this.entityData.get(STRIKE_INDEX);
    }

    public float getDamage() {
        return this.entityData.get(DAMAGE);
    }

    public float getIntensity() {
        return this.entityData.get(INTENSITY);
    }

    public int getRemainingTicks() {
        return this.entityData.get(REMAINING_TICKS);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(STRIKE_INDEX, 1);
        this.entityData.define(DAMAGE, 1.0F);
        this.entityData.define(INTENSITY, 1.0F);
        this.entityData.define(REMAINING_TICKS, 1);
    }

    @Override
    public void tick() {
        // LightningBolt.tick() applies vanilla fire and thunder-hit behavior. The tribulation
        // owns its damage and lifetime, so run the common entity tick only and let the vanilla
        // LightningBoltRenderer handle the client appearance.
        this.baseTick();
        this.setNoGravity(true);
        this.noPhysics = true;
        if (this.level().isClientSide) {
            return;
        }
        int remainingTicks = this.getRemainingTicks() - 1;
        if (remainingTicks <= 0) {
            this.discard();
            return;
        }
        this.entityData.set(REMAINING_TICKS, remainingTicks);
        if (this.level() instanceof ServerLevel serverLevel) {
            this.damageNearbyLivingEntities(serverLevel);
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
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.hasUUID(OWNER_UUID_KEY)) {
            this.entityData.set(OWNER_UUID, Optional.of(compoundTag.getUUID(OWNER_UUID_KEY)));
        }
        this.entityData.set(STRIKE_INDEX, Math.max(1, compoundTag.getInt(STRIKE_INDEX_KEY)));
        this.entityData.set(DAMAGE, Math.max(0.0F, compoundTag.getFloat(DAMAGE_KEY)));
        this.entityData.set(INTENSITY, Math.max(0.8F, compoundTag.getFloat(INTENSITY_KEY)));
        this.entityData.set(REMAINING_TICKS, Math.max(1, compoundTag.getInt(REMAINING_TICKS_KEY)));
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        this.getOwnerUuid().ifPresent(ownerUuid -> compoundTag.putUUID(OWNER_UUID_KEY, ownerUuid));
        compoundTag.putInt(STRIKE_INDEX_KEY, this.getStrikeIndex());
        compoundTag.putFloat(DAMAGE_KEY, this.getDamage());
        compoundTag.putFloat(INTENSITY_KEY, this.getIntensity());
        compoundTag.putInt(REMAINING_TICKS_KEY, this.getRemainingTicks());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void damageNearbyLivingEntities(ServerLevel serverLevel) {
        double damageRadius = 2.25D + this.getIntensity() * 0.45D;
        AABB damageBox = this.getBoundingBox().inflate(damageRadius, 3.0D, damageRadius);
        DamageSource damageSource = new DamageSource(
                serverLevel.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageTypes.REALM_EXECUTION),
                this,
                this
        );
        float pulseDamage = this.getDamage();
        ServerPlayer owner = this.getOwnerUuid()
                .map(serverLevel::getPlayerByUUID)
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .orElse(null);
        if (owner != null && owner.isAlive()) {
            hurtLivingEntity(owner, damageSource, pulseDamage);
        }
        for (LivingEntity livingEntity : serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                damageBox,
                livingEntity -> livingEntity.isAlive()
                        && !(livingEntity instanceof DefenseSpellGuardianEntity)
                        && livingEntity != owner
        )) {
            hurtLivingEntity(livingEntity, damageSource, pulseDamage);
        }
    }

    private static void hurtLivingEntity(
            LivingEntity livingEntity,
            DamageSource damageSource,
            float damage
    ) {
        if (livingEntity instanceof ServerPlayer player) {
            DefenseSpellUtility.getGuardian(player).ifPresentOrElse(
                    guardian -> {
                        TribulationUtility.noHurtDelay(guardian);
                        guardian.hurt(damageSource, damage);
                    },
                    () -> {
                        TribulationUtility.noHurtDelay(player);
                        player.hurt(damageSource, damage);
                    }
            );
            return;
        }
        TribulationUtility.noHurtDelay(livingEntity);
        livingEntity.hurt(damageSource, damage);
    }
}
