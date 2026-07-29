package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

public final class LivingEntityDataUtility {
    public static EntityDataAccessor<Integer> CULTIVATION_REALM;
    public static EntityDataAccessor<Integer> CULTIVATION_LAYER;
    public static EntityDataAccessor<Integer> SPIRITUAL_ROOT_ELEMENT_MASK;
    public static EntityDataAccessor<Float> SPIRITUAL_ROOT_LENGTH_INCHES;
    public static EntityDataAccessor<Integer> SOUL_STRENGTH_ILLUMINATED_STARS;
    public static EntityDataAccessor<CompoundTag> SPIRITUAL_ENERGY;
    public static EntityDataAccessor<CompoundTag> VALUE_CONTROL;

    public static final String CULTIVATION_REALM_KEY = ModMain.MOD_ID + ":cultivation_realm";
    public static final String CULTIVATION_LAYER_KEY = ModMain.MOD_ID + ":cultivation_layer";
    public static final String SPIRITUAL_ROOT_ELEMENT_MASK_KEY =
            ModMain.MOD_ID + ":spiritual_root_element_mask";
    public static final String SPIRITUAL_ROOT_LENGTH_INCHES_KEY =
            ModMain.MOD_ID + ":spiritual_root_length_inches";
    public static final String SOUL_STRENGTH_ILLUMINATED_STARS_KEY =
            ModMain.MOD_ID + ":soul_strength_illuminated_stars";
    public static final String SPIRITUAL_ENERGY_KEY = ModMain.MOD_ID + ":spiritual_energy";
    public static final String VALUE_CONTROL_KEY = ModMain.MOD_ID + ":value_control";

    private LivingEntityDataUtility() {
        throw new RuntimeException("哥们要是我发现有人针对我这个mod的话那我就要以雷霆手段出击了");
    }

    public static int getCultivationRealm(LivingEntity entity) {
        return entity.getEntityData().get(CULTIVATION_REALM);
    }

    public static int getCultivationLayer(LivingEntity entity) {
        return entity.getEntityData().get(CULTIVATION_LAYER);
    }

    public static void setCultivationStage(LivingEntity entity, int realmOrdinal, int layer) {
        entity.getEntityData().set(CULTIVATION_REALM, realmOrdinal);
        entity.getEntityData().set(CULTIVATION_LAYER, layer);
    }

    public static int getSpiritualRootElementMask(LivingEntity entity) {
        return entity.getEntityData().get(SPIRITUAL_ROOT_ELEMENT_MASK);
    }

    public static float getSpiritualRootLengthInches(LivingEntity entity) {
        return entity.getEntityData().get(SPIRITUAL_ROOT_LENGTH_INCHES);
    }

    public static void setSpiritualRoot(LivingEntity entity, int elementMask, float lengthInches) {
        entity.getEntityData().set(SPIRITUAL_ROOT_ELEMENT_MASK, elementMask);
        entity.getEntityData().set(SPIRITUAL_ROOT_LENGTH_INCHES, lengthInches);
    }

    public static int getSoulStrengthIlluminatedStars(LivingEntity entity) {
        return entity.getEntityData().get(SOUL_STRENGTH_ILLUMINATED_STARS);
    }

    public static void setSoulStrength(LivingEntity entity, int illuminatedStars) {
        entity.getEntityData().set(SOUL_STRENGTH_ILLUMINATED_STARS, illuminatedStars);
    }

    public static CompoundTag getSpiritualEnergy(LivingEntity entity) {
        return entity.getEntityData().get(SPIRITUAL_ENERGY);
    }

    public static void setSpiritualEnergy(LivingEntity entity, CompoundTag spiritualEnergy) {
        entity.getEntityData().set(SPIRITUAL_ENERGY, spiritualEnergy.copy());
    }

    public static CompoundTag getValueControl(LivingEntity entity) {
        return entity.getEntityData().get(VALUE_CONTROL);
    }

    public static void setValueControl(LivingEntity entity, CompoundTag valueControl) {
        entity.getEntityData().set(VALUE_CONTROL, valueControl.copy());
    }
}
