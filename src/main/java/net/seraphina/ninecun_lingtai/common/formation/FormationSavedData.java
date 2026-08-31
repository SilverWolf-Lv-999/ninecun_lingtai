package net.seraphina.ninecun_lingtai.common.formation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 每维度一份的阵法实例权威存储（阵禁系统日志 4.2）。
 * 首版 schema 常量内聚本类（M4 并发约束：不动 SchemaVersions 共享文件，
 * 后续统一迁移到 DataMigrator 时再挪）。
 */
public final class FormationSavedData extends SavedData {
    private static final String NAME = "ninecun_lingtai_formations";
    private static final String VERSION_TAG = "Version";
    private static final String INSTANCES_TAG = "Instances";
    /** 建议迁移目标：SchemaVersions.FORMATION_CURRENT。 */
    public static final int SCHEMA_VERSION = 1;

    /** 阵盘坐标 long → 实例（一盘一阵）。 */
    private final Map<Long, FormationInstance> instances = new HashMap<>();

    public static FormationSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FormationSavedData::load,
                FormationSavedData::new,
                NAME
        );
    }

    @Nullable
    public FormationInstance getInstance(BlockPos discPos) {
        return this.instances.get(discPos.asLong());
    }

    public Collection<FormationInstance> getAllInstances() {
        return this.instances.values();
    }

    public void putInstance(FormationInstance instance) {
        this.instances.put(instance.discPos().asLong(), instance);
        setDirty();
    }

    @Nullable
    public FormationInstance removeInstance(BlockPos discPos) {
        FormationInstance removed = this.instances.remove(discPos.asLong());
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(VERSION_TAG, SCHEMA_VERSION);
        ListTag instanceTags = new ListTag();
        for (FormationInstance instance : this.instances.values()) {
            instanceTags.add(instance.toNbt());
        }
        tag.put(INSTANCES_TAG, instanceTags);
        return tag;
    }

    private static FormationSavedData load(CompoundTag tag) {
        FormationSavedData savedData = new FormationSavedData();
        ListTag instanceTags = tag.getList(INSTANCES_TAG, Tag.TAG_COMPOUND);
        for (Tag instanceTag : instanceTags) {
            FormationInstance instance = FormationInstance.fromNbt((CompoundTag) instanceTag);
            if (instance != null) {
                savedData.instances.put(instance.discPos().asLong(), instance);
            }
        }
        return savedData;
    }
}
