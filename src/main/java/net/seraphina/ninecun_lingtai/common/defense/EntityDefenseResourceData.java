package net.seraphina.ninecun_lingtai.common.defense;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.seraphina.ninecun_lingtai.common.insight.InsightEffectUtility;
import net.seraphina.ninecun_lingtai.common.player.DefenseResourceSnapshot;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/** Stores the realm defense resource shared by players and cultivating creatures. */
public final class EntityDefenseResourceData {
    private static final String DEFENSE_RESOURCE_KEY = ModMain.MOD_ID + ":defense_resource";
    private static final String CURRENT_KEY = "current";

    public static DefenseResourceSnapshot capture(LivingEntity entity) {
        Optional<CultivationStage> stage = CreatureCultivationData.getStage(entity);
        int elementMask = CreatureCultivationData.getSpiritualRoot(entity)
                .map(SpiritualRoot::getElementMask)
                .orElse(0);
        int maximum = getMaximum(entity);
        if (maximum <= 0) {
            clear(entity);
            return DefenseResourceSnapshot.empty(elementMask);
        }
        return new DefenseResourceSnapshot(
                CultivationDefenseUtility.getDefenseResourceType(stage),
                getOrInitializeCurrent(entity, maximum),
                maximum,
                elementMask
        );
    }

    public static void clampToStage(LivingEntity entity) {
        int maximum = getMaximum(entity);
        if (maximum <= 0) {
            clear(entity);
            return;
        }
        getOrInitializeCurrent(entity, maximum);
    }

    public static int getCurrent(LivingEntity entity) {
        int maximum = getMaximum(entity);
        if (maximum <= 0) {
            clear(entity);
            return 0;
        }
        return getOrInitializeCurrent(entity, maximum);
    }

    public static void setCurrent(LivingEntity entity, int current) {
        setCurrentInternal(entity, current);
    }

    public static int repair(LivingEntity entity, int amount) {
        if (amount <= 0) {
            return 0;
        }
        int maximum = getMaximum(entity);
        if (maximum <= 0) {
            clear(entity);
            return 0;
        }
        int current = getOrInitializeCurrent(entity, maximum);
        int repaired = (int) Math.min((long) maximum - current, (long) amount);
        if (repaired <= 0) {
            return 0;
        }
        setCurrentInternal(entity, current + repaired);
        return repaired;
    }

    public static void refillToMaximum(LivingEntity entity) {
        int maximum = getMaximum(entity);
        if (maximum <= 0) {
            clear(entity);
            return;
        }
        setCurrentInternal(entity, maximum);
    }

    public static boolean consume(LivingEntity entity, int amount) {
        if (amount <= 0) {
            return true;
        }
        int maximum = getMaximum(entity);
        if (maximum <= 0) {
            clear(entity);
            return false;
        }
        int current = getOrInitializeCurrent(entity, maximum);
        if (current < amount) {
            return false;
        }
        setCurrentInternal(entity, current - amount);
        return true;
    }

    public static void deplete(LivingEntity entity) {
        if (getMaximum(entity) <= 0) {
            clear(entity);
            return;
        }
        setCurrentInternal(entity, 0);
    }

    public static void copy(LivingEntity sourceEntity, LivingEntity targetEntity) {
        CompoundTag sourceData = sourceEntity.getPersistentData();
        if (sourceData.contains(DEFENSE_RESOURCE_KEY, Tag.TAG_COMPOUND)) {
            targetEntity.getPersistentData().put(
                    DEFENSE_RESOURCE_KEY,
                    sourceData.getCompound(DEFENSE_RESOURCE_KEY).copy()
            );
        } else {
            clear(targetEntity);
        }
    }

    public static void clear(LivingEntity entity) {
        entity.getPersistentData().remove(DEFENSE_RESOURCE_KEY);
    }

    /**
     * 护体上限：境界基准 ×（1 + 悟道印记"稳如磐石"加成）。印记只属于玩家，
     * 生物走境界基准；乘区经 {@link InsightEffectUtility} 单点读取（R9）。
     */
    private static int getMaximum(LivingEntity entity) {
        int base = CultivationDefenseUtility.getMaximumDefenseResource(
                CreatureCultivationData.getStage(entity)
        );
        if (base <= 0 || !(entity instanceof Player player)) {
            return base;
        }
        double insightBonus = InsightEffectUtility.query(
                player,
                InsightEffectUtility.EFFECT_DEFENSE_CAPACITY_BONUS
        );
        return insightBonus <= 0.0D
                ? base
                : (int) Math.round(base * (1.0D + insightBonus));
    }

    private static int getOrInitializeCurrent(LivingEntity entity, int maximum) {
        CompoundTag resourceData = getOrCreateResourceData(entity);
        if (!resourceData.contains(CURRENT_KEY, Tag.TAG_ANY_NUMERIC)) {
            resourceData.putInt(CURRENT_KEY, maximum);
            return maximum;
        }
        int storedCurrent = resourceData.getInt(CURRENT_KEY);
        int current = Math.max(0, Math.min(storedCurrent, maximum));
        if (current != storedCurrent) {
            resourceData.putInt(CURRENT_KEY, current);
        }
        return current;
    }

    private static void setCurrentInternal(LivingEntity entity, int current) {
        int maximum = getMaximum(entity);
        if (maximum <= 0) {
            clear(entity);
            return;
        }
        getOrCreateResourceData(entity).putInt(CURRENT_KEY, Math.max(0, Math.min(current, maximum)));
    }

    private static CompoundTag getOrCreateResourceData(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        if (!persistentData.contains(DEFENSE_RESOURCE_KEY, Tag.TAG_COMPOUND)) {
            persistentData.put(DEFENSE_RESOURCE_KEY, new CompoundTag());
        }
        return persistentData.getCompound(DEFENSE_RESOURCE_KEY);
    }
}
