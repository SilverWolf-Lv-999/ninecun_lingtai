package net.seraphina.ninecun_lingtai.common.tomb;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 墓穴三态机的持久层（每维度一份，稀疏存储）。
 *
 * <p>只记录离开"沉睡"态的墓穴：{@code AWAKENED}（墓主已破棺，实体活在区块里）与
 * {@code DEFEATED}（墓主已陨落，永不复生）。未记录 = 沉睡（默认态），
 * 网格判定与墓穴参数全部由 {@link CultivatorTombUtility} 从种子推导，无需入档。</p>
 */
public final class CultivatorTombSavedData extends SavedData {
    private static final int CURRENT_VERSION = 1;
    private static final String DATA_NAME = "ninecun_lingtai_cultivator_tombs";
    private static final String VERSION_TAG = "Version";
    private static final String TOMBS_TAG = "Tombs";
    private static final String CELL_KEY_TAG = "Key";
    private static final String STATE_TAG = "State";

    public enum TombState {
        DORMANT,
        AWAKENED,
        DEFEATED
    }

    private final Map<Long, TombState> states = new HashMap<>();

    public static CultivatorTombSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                CultivatorTombSavedData::load,
                CultivatorTombSavedData::new,
                DATA_NAME
        );
    }

    public static CultivatorTombSavedData load(CompoundTag tag) {
        CultivatorTombSavedData savedData = new CultivatorTombSavedData();
        ListTag tombTags = tag.getList(TOMBS_TAG, Tag.TAG_COMPOUND);
        for (Tag tombTag : tombTags) {
            if (!(tombTag instanceof CompoundTag tombDataTag) || !tombDataTag.contains(CELL_KEY_TAG)) {
                continue;
            }
            int stateOrdinal = tombDataTag.getInt(STATE_TAG);
            if (stateOrdinal <= 0 || stateOrdinal >= TombState.values().length) {
                continue;
            }
            savedData.states.put(tombDataTag.getLong(CELL_KEY_TAG), TombState.values()[stateOrdinal]);
        }
        return savedData;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(VERSION_TAG, CURRENT_VERSION);
        ListTag tombTags = new ListTag();
        for (Map.Entry<Long, TombState> entry : this.states.entrySet()) {
            if (entry.getValue() == TombState.DORMANT) {
                continue;
            }
            CompoundTag tombDataTag = new CompoundTag();
            tombDataTag.putLong(CELL_KEY_TAG, entry.getKey());
            tombDataTag.putInt(STATE_TAG, entry.getValue().ordinal());
            tombTags.add(tombDataTag);
        }
        tag.put(TOMBS_TAG, tombTags);
        return tag;
    }

    public TombState getState(long cellKey) {
        return this.states.getOrDefault(cellKey, TombState.DORMANT);
    }

    public void setState(long cellKey, TombState state) {
        if (state == TombState.DORMANT) {
            this.states.remove(cellKey);
        } else {
            this.states.put(cellKey, state);
        }
        this.setDirty();
    }
}
