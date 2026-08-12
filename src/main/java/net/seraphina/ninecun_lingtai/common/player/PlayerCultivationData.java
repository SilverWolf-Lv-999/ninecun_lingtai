package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerCultivationData {
    private static final String CULTIVATION_KEY = ModMain.MOD_ID + ":cultivation";
    private static final String REALM_KEY = "realm";
    private static final String LAYER_KEY = "layer";

    public static Optional<CultivationStage> getStage(Player player) {
        CompoundTag persistentData = player.getPersistentData();
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

    public static void setStage(Player player, CultivationStage stage) {
        CompoundTag stageData = new CompoundTag();
        stageData.putString(REALM_KEY, stage.realm().getSerializedName());
        stageData.putInt(LAYER_KEY, stage.layer());
        player.getPersistentData().put(CULTIVATION_KEY, stageData);
    }

    public static void clearStage(Player player) {
        player.getPersistentData().remove(CULTIVATION_KEY);
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
