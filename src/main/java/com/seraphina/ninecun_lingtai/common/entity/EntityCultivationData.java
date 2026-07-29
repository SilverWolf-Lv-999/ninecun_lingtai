package com.seraphina.ninecun_lingtai.common.entity;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.utility.LivingEntityDataUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The shared cultivation state for every living entity.
 *
 * <p>EntityData is the live, automatically synchronized representation. Legacy PersistentData is
 * only read once for migration; current saves are written by the LivingEntity Mixin.</p>
 */
public final class EntityCultivationData {
    private static final String CULTIVATION_KEY = ModMain.MOD_ID + ":cultivation";
    private static final String SPIRITUAL_ROOT_KEY = ModMain.MOD_ID + ":spiritual_root";
    private static final String SOUL_STRENGTH_KEY = ModMain.MOD_ID + ":soul_strength";
    private static final String SPIRITUAL_ENERGY_KEY = ModMain.MOD_ID + ":spiritual_energy";
    private static final String REALM_KEY = "realm";
    private static final String LAYER_KEY = "layer";
    private static final String ELEMENT_MASK_KEY = "element_mask";
    private static final String LENGTH_INCHES_KEY = "length_inches";
    private static final String ILLUMINATED_STARS_KEY = "illuminated_stars";

    private EntityCultivationData() {
    }

    public static Optional<CultivationStage> getStage(LivingEntity entity) {
        Optional<CultivationStage> syncedStage = getSyncedStage(entity);
        if (syncedStage.isPresent()) {
            return syncedStage;
        }

        Optional<CultivationStage> persistentStage = readPersistentStage(entity);
        persistentStage.ifPresent(stage -> setSyncedStage(entity, stage));
        return persistentStage;
    }

    public static void setStage(LivingEntity entity, CultivationStage stage) {
        setSyncedStage(entity, stage);
        entity.getPersistentData().remove(CULTIVATION_KEY);
    }

    public static void clearStage(LivingEntity entity) {
        clearSyncedStage(entity);
        entity.getPersistentData().remove(CULTIVATION_KEY);
    }

    public static Optional<SpiritualRoot> getSpiritualRoot(LivingEntity entity) {
        if (LivingEntityDataUtility.getSpiritualRootElementMask(entity) >= 0) {
            return SpiritualRoot.fromElementMask(
                    LivingEntityDataUtility.getSpiritualRootElementMask(entity),
                    LivingEntityDataUtility.getSpiritualRootLengthInches(entity)
            );
        }

        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(SPIRITUAL_ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag spiritualRootData = persistentData.getCompound(SPIRITUAL_ROOT_KEY);
        if (!spiritualRootData.contains(ELEMENT_MASK_KEY, Tag.TAG_ANY_NUMERIC)
                || !spiritualRootData.contains(LENGTH_INCHES_KEY, Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        Optional<SpiritualRoot> spiritualRoot = SpiritualRoot.fromElementMask(
                spiritualRootData.getInt(ELEMENT_MASK_KEY),
                spiritualRootData.getFloat(LENGTH_INCHES_KEY)
        );
        spiritualRoot.ifPresent(value -> setSyncedSpiritualRoot(entity, value));
        return spiritualRoot;
    }

    public static void setSpiritualRoot(LivingEntity entity, SpiritualRoot spiritualRoot) {
        setSyncedSpiritualRoot(entity, spiritualRoot);
        entity.getPersistentData().remove(SPIRITUAL_ROOT_KEY);
    }

    public static void clearSpiritualRoot(LivingEntity entity) {
        LivingEntityDataUtility.setSpiritualRoot(entity, -1, 0.0F);
        entity.getPersistentData().remove(SPIRITUAL_ROOT_KEY);
    }

    public static Optional<SoulStrength> getSoulStrength(LivingEntity entity) {
        if (LivingEntityDataUtility.getSoulStrengthIlluminatedStars(entity) >= 0) {
            return SoulStrength.fromIlluminatedStars(
                    LivingEntityDataUtility.getSoulStrengthIlluminatedStars(entity)
            );
        }

        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(SOUL_STRENGTH_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag soulStrengthData = persistentData.getCompound(SOUL_STRENGTH_KEY);
        if (!soulStrengthData.contains(ILLUMINATED_STARS_KEY, Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        Optional<SoulStrength> soulStrength = SoulStrength.fromIlluminatedStars(
                soulStrengthData.getInt(ILLUMINATED_STARS_KEY)
        );
        soulStrength.ifPresent(value -> setSyncedSoulStrength(entity, value));
        return soulStrength;
    }

    public static void setSoulStrength(LivingEntity entity, SoulStrength soulStrength) {
        setSyncedSoulStrength(entity, soulStrength);
        entity.getPersistentData().remove(SOUL_STRENGTH_KEY);
    }

    public static void clearSoulStrength(LivingEntity entity) {
        LivingEntityDataUtility.setSoulStrength(entity, -1);
        entity.getPersistentData().remove(SOUL_STRENGTH_KEY);
    }

    public static double getEnergy(LivingEntity entity, ElementType element) {
        CompoundTag spiritualEnergyData = getSpiritualEnergyData(entity);
        String elementKey = element.getSerializedName();
        if (!spiritualEnergyData.contains(elementKey, Tag.TAG_ANY_NUMERIC)) {
            return 0.0D;
        }
        double spiritualEnergy = spiritualEnergyData.getDouble(elementKey);
        return Double.isFinite(spiritualEnergy) && spiritualEnergy > 0.0D
                ? spiritualEnergy
                : 0.0D;
    }

    public static Map<ElementType, Double> getAllEnergy(LivingEntity entity) {
        EnumMap<ElementType, Double> energyValues = new EnumMap<>(ElementType.class);
        for (ElementType element : ElementType.values()) {
            double energy = getEnergy(entity, element);
            if (energy > 0.0D) {
                energyValues.put(element, energy);
            }
        }
        return Collections.unmodifiableMap(energyValues);
    }

    public static void addEnergy(LivingEntity entity, ElementType element, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        double updatedEnergy = getEnergy(entity, element) + amount;
        setEnergy(entity, element, Double.isFinite(updatedEnergy) ? updatedEnergy : Double.MAX_VALUE);
    }

    public static void setEnergy(LivingEntity entity, ElementType element, double amount) {
        if (!Double.isFinite(amount)) {
            return;
        }
        if (amount <= 0.0D) {
            clearEnergy(entity, element);
            return;
        }
        CompoundTag spiritualEnergyData = getSpiritualEnergyData(entity);
        spiritualEnergyData.putDouble(element.getSerializedName(), amount);
        setSpiritualEnergy(entity, spiritualEnergyData);
        entity.getPersistentData().remove(SPIRITUAL_ENERGY_KEY);
    }

    public static void removeEnergy(LivingEntity entity, ElementType element, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return;
        }
        setEnergy(entity, element, Math.max(0.0D, getEnergy(entity, element) - amount));
    }

    public static void clearEnergy(LivingEntity entity, ElementType element) {
        CompoundTag spiritualEnergyData = getSpiritualEnergyData(entity);
        spiritualEnergyData.remove(element.getSerializedName());
        setSpiritualEnergy(entity, spiritualEnergyData);
        entity.getPersistentData().remove(SPIRITUAL_ENERGY_KEY);
    }

    public static void clearAllEnergy(LivingEntity entity) {
        setSpiritualEnergy(entity, new CompoundTag());
        entity.getPersistentData().remove(SPIRITUAL_ENERGY_KEY);
    }

    public static void retainEnergyRatio(LivingEntity entity, double retainedRatio) {
        if (!Double.isFinite(retainedRatio)) {
            return;
        }
        if (retainedRatio <= 0.0D) {
            clearAllEnergy(entity);
            return;
        }
        if (retainedRatio >= 1.0D) {
            return;
        }
        for (ElementType element : ElementType.values()) {
            double energy = getEnergy(entity, element);
            if (energy > 0.0D) {
                setEnergy(entity, element, energy * retainedRatio);
            }
        }
    }

    public static boolean consumeEnergy(LivingEntity entity, ElementType element, double amount) {
        return consumeEnergy(entity, Set.of(element), amount);
    }

    public static Map<ElementType, Double> consumeEnergyAny(LivingEntity entity, double amount) {
        return consumeEnergyAny(entity, getAllEnergy(entity).keySet(), amount);
    }

    public static Map<ElementType, Double> consumeEnergyAny(
            LivingEntity entity,
            Set<ElementType> eligibleElements,
            double amount
    ) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return null;
        }
        Map<ElementType, Double> currentEnergy = getEligibleEnergy(entity, eligibleElements);
        double totalEnergy = currentEnergy.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalEnergy < amount) {
            return null;
        }
        EnumMap<ElementType, Double> consumed = new EnumMap<>(ElementType.class);
        double remaining = amount;
        while (remaining > 1.0E-9D) {
            ElementType richestElement = null;
            double richestEnergy = 0.0D;
            for (Map.Entry<ElementType, Double> entry : currentEnergy.entrySet()) {
                double available = entry.getValue() - consumed.getOrDefault(entry.getKey(), 0.0D);
                if (available > richestEnergy) {
                    richestEnergy = available;
                    richestElement = entry.getKey();
                }
            }
            if (richestElement == null) {
                return null;
            }
            double drained = Math.min(richestEnergy, remaining);
            consumed.merge(richestElement, drained, Double::sum);
            remaining -= drained;
        }
        for (Map.Entry<ElementType, Double> entry : consumed.entrySet()) {
            setEnergy(entity, entry.getKey(), currentEnergy.get(entry.getKey()) - entry.getValue());
        }
        return Collections.unmodifiableMap(consumed);
    }

    public static double consumeEnergyUpTo(
            LivingEntity entity,
            Set<ElementType> eligibleElements,
            double maximumAmount
    ) {
        if (!Double.isFinite(maximumAmount) || maximumAmount <= 0.0D) {
            return 0.0D;
        }
        Map<ElementType, Double> currentEnergy = getEligibleEnergy(entity, eligibleElements);
        double remaining = maximumAmount;
        double consumed = 0.0D;
        for (Map.Entry<ElementType, Double> entry : currentEnergy.entrySet()) {
            if (remaining <= 1.0E-9D) {
                break;
            }
            double drained = Math.min(entry.getValue(), remaining);
            setEnergy(entity, entry.getKey(), entry.getValue() - drained);
            remaining -= drained;
            consumed += drained;
        }
        return consumed;
    }

    public static boolean consumeEnergy(
            LivingEntity entity,
            Set<ElementType> elements,
            double amount
    ) {
        if (elements == null || elements.isEmpty() || !Double.isFinite(amount) || amount <= 0.0D) {
            return false;
        }
        EnumMap<ElementType, Double> currentEnergy = new EnumMap<>(ElementType.class);
        for (ElementType element : elements) {
            double elementEnergy = getEnergy(entity, element);
            if (elementEnergy < amount) {
                return false;
            }
            currentEnergy.put(element, elementEnergy);
        }
        for (Map.Entry<ElementType, Double> entry : currentEnergy.entrySet()) {
            setEnergy(entity, entry.getKey(), entry.getValue() - amount);
        }
        return true;
    }

    public static void copy(LivingEntity sourceEntity, LivingEntity targetEntity) {
        getStage(sourceEntity).ifPresentOrElse(
                stage -> setStage(targetEntity, stage),
                () -> clearStage(targetEntity)
        );
        getSpiritualRoot(sourceEntity).ifPresentOrElse(
                root -> setSpiritualRoot(targetEntity, root),
                () -> clearSpiritualRoot(targetEntity)
        );
        getSoulStrength(sourceEntity).ifPresentOrElse(
                strength -> setSoulStrength(targetEntity, strength),
                () -> clearSoulStrength(targetEntity)
        );
        clearAllEnergy(targetEntity);
        for (Map.Entry<ElementType, Double> entry : getAllEnergy(sourceEntity).entrySet()) {
            setEnergy(targetEntity, entry.getKey(), entry.getValue());
        }
    }

    public static void migratePersistentData(LivingEntity entity) {
        if (getStage(entity).isPresent()) {
            entity.getPersistentData().remove(CULTIVATION_KEY);
        }
        if (getSpiritualRoot(entity).isPresent()) {
            entity.getPersistentData().remove(SPIRITUAL_ROOT_KEY);
        }
        if (getSoulStrength(entity).isPresent()) {
            entity.getPersistentData().remove(SOUL_STRENGTH_KEY);
        }
        if (!getAllEnergy(entity).isEmpty()) {
            entity.getPersistentData().remove(SPIRITUAL_ENERGY_KEY);
        }
    }

    private static Optional<CultivationStage> getSyncedStage(LivingEntity entity) {
        int realmOrdinal = LivingEntityDataUtility.getCultivationRealm(entity);
        int layer = LivingEntityDataUtility.getCultivationLayer(entity);
        if (realmOrdinal < 0 || realmOrdinal >= CultivationRealm.values().length) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CultivationStage(CultivationRealm.values()[realmOrdinal], layer));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static Optional<CultivationStage> readPersistentStage(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(CULTIVATION_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag stageData = persistentData.getCompound(CULTIVATION_KEY);
        if (!stageData.contains(REALM_KEY, Tag.TAG_STRING)
                || !stageData.contains(LAYER_KEY, Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        Optional<CultivationRealm> realm = CultivationRealm.fromSerializedName(stageData.getString(REALM_KEY));
        if (realm.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new CultivationStage(realm.get(), stageData.getInt(LAYER_KEY)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static void setSyncedStage(LivingEntity entity, CultivationStage stage) {
        LivingEntityDataUtility.setCultivationStage(entity, stage.realm().ordinal(), stage.layer());
    }

    private static void clearSyncedStage(LivingEntity entity) {
        LivingEntityDataUtility.setCultivationStage(entity, -1, 0);
    }

    private static void setSyncedSpiritualRoot(LivingEntity entity, SpiritualRoot spiritualRoot) {
        LivingEntityDataUtility.setSpiritualRoot(
                entity,
                spiritualRoot.getElementMask(),
                spiritualRoot.lengthInches()
        );
    }

    private static void setSyncedSoulStrength(LivingEntity entity, SoulStrength soulStrength) {
        LivingEntityDataUtility.setSoulStrength(entity, soulStrength.illuminatedStars());
    }

    private static CompoundTag getSpiritualEnergyData(LivingEntity entity) {
        CompoundTag syncedData = LivingEntityDataUtility.getSpiritualEnergy(entity);
        if (!syncedData.isEmpty()) {
            return syncedData.copy();
        }
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(SPIRITUAL_ENERGY_KEY, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }
        CompoundTag persistentEnergy = persistentData.getCompound(SPIRITUAL_ENERGY_KEY).copy();
        LivingEntityDataUtility.setSpiritualEnergy(entity, persistentEnergy);
        return persistentEnergy;
    }

    private static void setSpiritualEnergy(LivingEntity entity, CompoundTag spiritualEnergyData) {
        LivingEntityDataUtility.setSpiritualEnergy(entity, spiritualEnergyData);
    }

    private static Map<ElementType, Double> getEligibleEnergy(
            LivingEntity entity,
            Set<ElementType> eligibleElements
    ) {
        EnumMap<ElementType, Double> energyValues = new EnumMap<>(ElementType.class);
        if (eligibleElements == null || eligibleElements.isEmpty()) {
            energyValues.putAll(getAllEnergy(entity));
            return energyValues;
        }
        for (ElementType element : eligibleElements) {
            double energy = getEnergy(entity, element);
            if (energy > 0.0D) {
                energyValues.put(element, energy);
            }
        }
        return energyValues;
    }
}
