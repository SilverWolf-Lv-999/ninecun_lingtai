package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerCultivationData {
    private static final String CULTIVATION_KEY = ModMain.MOD_ID + ":cultivation";
    private static final String REALM_KEY = "realm";
    private static final String LAYER_KEY = "layer";

    private PlayerCultivationData() {
    }

    public static Optional<CultivationStage> getStage(Player player, ElementType spiritualRootElement) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(CULTIVATION_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag cultivationData = persistentData.getCompound(CULTIVATION_KEY);
        String spiritualRootKey = spiritualRootElement.getSerializedName();
        if (!cultivationData.contains(spiritualRootKey, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag stageData = cultivationData.getCompound(spiritualRootKey);
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

    public static Map<ElementType, CultivationStage> getAllStages(Player player) {
        EnumMap<ElementType, CultivationStage> stages = new EnumMap<>(ElementType.class);
        for (ElementType element : ElementType.values()) {
            getStage(player, element).ifPresent(stage -> stages.put(element, stage));
        }
        return Collections.unmodifiableMap(stages);
    }

    public static void setStage(Player player, ElementType spiritualRootElement, CultivationStage stage) {
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag cultivationData = persistentData.contains(CULTIVATION_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(CULTIVATION_KEY)
                : new CompoundTag();
        CompoundTag stageData = new CompoundTag();
        stageData.putString(REALM_KEY, stage.realm().getSerializedName());
        stageData.putInt(LAYER_KEY, stage.layer());
        cultivationData.put(spiritualRootElement.getSerializedName(), stageData);
        persistentData.put(CULTIVATION_KEY, cultivationData);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(CULTIVATION_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    CULTIVATION_KEY,
                    sourceData.getCompound(CULTIVATION_KEY).copy()
            );
        }
    }
}
