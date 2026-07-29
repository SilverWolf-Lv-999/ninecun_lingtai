package com.seraphina.ninecun_lingtai.mixin.common.entity;

import com.seraphina.ninecun_lingtai.common.entity.EntityCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlData;
import com.seraphina.ninecun_lingtai.utility.LivingEntityDataUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity {
    @Unique
    private final LivingEntity ninecunLingtai$livingEntity = (LivingEntity) (Object) this;

    public MixinLivingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "defineSynchedData", at = @At("HEAD"))
    private void defineSynchedData(CallbackInfo callbackInfo) {
        this.entityData.define(LivingEntityDataUtility.CULTIVATION_REALM, -1);
        this.entityData.define(LivingEntityDataUtility.CULTIVATION_LAYER, 0);
        this.entityData.define(LivingEntityDataUtility.SPIRITUAL_ROOT_ELEMENT_MASK, -1);
        this.entityData.define(LivingEntityDataUtility.SPIRITUAL_ROOT_LENGTH_INCHES, 0.0F);
        this.entityData.define(LivingEntityDataUtility.SOUL_STRENGTH_ILLUMINATED_STARS, -1);
        this.entityData.define(LivingEntityDataUtility.SPIRITUAL_ENERGY, new CompoundTag());
        this.entityData.define(LivingEntityDataUtility.VALUE_CONTROL, new CompoundTag());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readAdditionalSaveData(CompoundTag tag, CallbackInfo callbackInfo) {
        if (tag.contains(LivingEntityDataUtility.CULTIVATION_REALM_KEY)) {
            LivingEntityDataUtility.setCultivationStage(
                    ninecunLingtai$livingEntity,
                    tag.getInt(LivingEntityDataUtility.CULTIVATION_REALM_KEY),
                    tag.getInt(LivingEntityDataUtility.CULTIVATION_LAYER_KEY)
            );
        }
        if (tag.contains(LivingEntityDataUtility.SPIRITUAL_ROOT_ELEMENT_MASK_KEY)) {
            LivingEntityDataUtility.setSpiritualRoot(
                    ninecunLingtai$livingEntity,
                    tag.getInt(LivingEntityDataUtility.SPIRITUAL_ROOT_ELEMENT_MASK_KEY),
                    tag.getFloat(LivingEntityDataUtility.SPIRITUAL_ROOT_LENGTH_INCHES_KEY)
            );
        }
        if (tag.contains(LivingEntityDataUtility.SOUL_STRENGTH_ILLUMINATED_STARS_KEY)) {
            LivingEntityDataUtility.setSoulStrength(
                    ninecunLingtai$livingEntity,
                    tag.getInt(LivingEntityDataUtility.SOUL_STRENGTH_ILLUMINATED_STARS_KEY)
            );
        }
        if (tag.contains(LivingEntityDataUtility.SPIRITUAL_ENERGY_KEY)) {
            LivingEntityDataUtility.setSpiritualEnergy(
                    ninecunLingtai$livingEntity,
                    tag.getCompound(LivingEntityDataUtility.SPIRITUAL_ENERGY_KEY)
            );
        }
        if (tag.contains(LivingEntityDataUtility.VALUE_CONTROL_KEY)) {
            LivingEntityDataUtility.setValueControl(
                    ninecunLingtai$livingEntity,
                    tag.getCompound(LivingEntityDataUtility.VALUE_CONTROL_KEY)
            );
        }

        EntityCultivationData.migratePersistentData(ninecunLingtai$livingEntity);
        if (ninecunLingtai$livingEntity instanceof Player player) {
            PlayerValueControlData.migratePersistentData(player);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void addAdditionalSaveData(CompoundTag tag, CallbackInfo callbackInfo) {
        tag.putInt(
                LivingEntityDataUtility.CULTIVATION_REALM_KEY,
                LivingEntityDataUtility.getCultivationRealm(ninecunLingtai$livingEntity)
        );
        tag.putInt(
                LivingEntityDataUtility.CULTIVATION_LAYER_KEY,
                LivingEntityDataUtility.getCultivationLayer(ninecunLingtai$livingEntity)
        );
        tag.putInt(
                LivingEntityDataUtility.SPIRITUAL_ROOT_ELEMENT_MASK_KEY,
                LivingEntityDataUtility.getSpiritualRootElementMask(ninecunLingtai$livingEntity)
        );
        tag.putFloat(
                LivingEntityDataUtility.SPIRITUAL_ROOT_LENGTH_INCHES_KEY,
                LivingEntityDataUtility.getSpiritualRootLengthInches(ninecunLingtai$livingEntity)
        );
        tag.putInt(
                LivingEntityDataUtility.SOUL_STRENGTH_ILLUMINATED_STARS_KEY,
                LivingEntityDataUtility.getSoulStrengthIlluminatedStars(ninecunLingtai$livingEntity)
        );
        tag.put(
                LivingEntityDataUtility.SPIRITUAL_ENERGY_KEY,
                LivingEntityDataUtility.getSpiritualEnergy(ninecunLingtai$livingEntity).copy()
        );
        tag.put(
                LivingEntityDataUtility.VALUE_CONTROL_KEY,
                LivingEntityDataUtility.getValueControl(ninecunLingtai$livingEntity).copy()
        );
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void clinit(CallbackInfo callbackInfo) {
        LivingEntityDataUtility.CULTIVATION_REALM = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.INT
        );
        LivingEntityDataUtility.CULTIVATION_LAYER = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.INT
        );
        LivingEntityDataUtility.SPIRITUAL_ROOT_ELEMENT_MASK = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.INT
        );
        LivingEntityDataUtility.SPIRITUAL_ROOT_LENGTH_INCHES = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.FLOAT
        );
        LivingEntityDataUtility.SOUL_STRENGTH_ILLUMINATED_STARS = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.INT
        );
        LivingEntityDataUtility.SPIRITUAL_ENERGY = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.COMPOUND_TAG
        );
        LivingEntityDataUtility.VALUE_CONTROL = SynchedEntityData.defineId(
                LivingEntity.class,
                EntityDataSerializers.COMPOUND_TAG
        );
    }
}
