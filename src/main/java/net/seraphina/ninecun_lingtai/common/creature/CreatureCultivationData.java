package net.seraphina.ninecun_lingtai.common.creature;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * Cultivation stage and spiritual root storage that works for any living entity. Players delegate
 * to their dedicated data classes; other creatures use the same NBT keys and layout so the two
 * remain interchangeable when read generically.
 */
public final class CreatureCultivationData {
    private static final String CULTIVATION_KEY = ModMain.MOD_ID + ":cultivation";
    private static final String REALM_KEY = "realm";
    private static final String LAYER_KEY = "layer";
    private static final String SPIRITUAL_ROOT_KEY = ModMain.MOD_ID + ":spiritual_root";
    private static final String ELEMENT_MASK_KEY = "element_mask";
    private static final String LENGTH_INCHES_KEY = "length_inches";
    private static final String SPAWN_ROLL_DONE_KEY = ModMain.MOD_ID + ":cultivation_rolled";

    private CreatureCultivationData() {
    }

    public static Optional<CultivationStage> getStage(LivingEntity entity) {
        if (entity instanceof Player player) {
            return PlayerCultivationData.getStage(player);
        }
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

    public static void setStage(LivingEntity entity, CultivationStage stage) {
        if (entity instanceof Player player) {
            PlayerCultivationData.setStage(player, stage);
            return;
        }
        CompoundTag stageData = new CompoundTag();
        stageData.putString(REALM_KEY, stage.realm().getSerializedName());
        stageData.putInt(LAYER_KEY, stage.layer());
        entity.getPersistentData().put(CULTIVATION_KEY, stageData);
    }

    public static void clearStage(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerCultivationData.clearStage(player);
            return;
        }
        entity.getPersistentData().remove(CULTIVATION_KEY);
    }

    public static Optional<SpiritualRoot> getSpiritualRoot(LivingEntity entity) {
        if (entity instanceof Player player) {
            return PlayerAptitudeData.getSpiritualRoot(player);
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
        return SpiritualRoot.fromElementMask(
                spiritualRootData.getInt(ELEMENT_MASK_KEY),
                spiritualRootData.getFloat(LENGTH_INCHES_KEY)
        );
    }

    public static void setSpiritualRoot(LivingEntity entity, SpiritualRoot spiritualRoot) {
        if (entity instanceof Player player) {
            PlayerAptitudeData.setSpiritualRoot(player, spiritualRoot);
            return;
        }
        CompoundTag spiritualRootData = new CompoundTag();
        spiritualRootData.putInt(ELEMENT_MASK_KEY, spiritualRoot.getElementMask());
        spiritualRootData.putFloat(LENGTH_INCHES_KEY, spiritualRoot.lengthInches());
        entity.getPersistentData().put(SPIRITUAL_ROOT_KEY, spiritualRootData);
    }

    public static void clearSpiritualRoot(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerAptitudeData.clearSpiritualRoot(player);
            return;
        }
        entity.getPersistentData().remove(SPIRITUAL_ROOT_KEY);
    }

    public static boolean isSpawnRollDone(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(SPAWN_ROLL_DONE_KEY);
    }

    public static void markSpawnRollDone(LivingEntity entity) {
        entity.getPersistentData().putBoolean(SPAWN_ROLL_DONE_KEY, true);
    }
}
