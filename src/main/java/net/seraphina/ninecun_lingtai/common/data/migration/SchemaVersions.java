package net.seraphina.ninecun_lingtai.common.data.migration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * 各数据域的 NBT schema 版本常量与版本标记读写工具。
 *
 * <p>约定：每个需要迁移能力的数据域，在其根 CompoundTag 内写入 {@value #SCHEMA_VERSION_KEY}
 * （int）。缺失该键视为 {@value #UNVERSIONED}（历史数据）。新增数据域时在此追加
 * {@code XXX_DOMAIN} 与 {@code XXX_CURRENT} 两个常量，并为每次格式变更注册一个
 * {@link DataMigrator}。</p>
 */
public final class SchemaVersions {
    /** 根 CompoundTag 内保存 schema 版本的键名。 */
    public static final String SCHEMA_VERSION_KEY = "SchemaVersion";

    /** 缺失版本标记的历史数据统一视为版本 0。 */
    public static final int UNVERSIONED = 0;

    /** 玩家伤痕数据域（见 common/status/PlayerScarData）。 */
    public static final String PLAYER_SCARS_DOMAIN = "player_scars";
    public static final int PLAYER_SCARS_CURRENT = 1;

    /** 禁制权威记录数据域（见 common/seal/SealRecordSavedData）。 */
    public static final String SEAL_RECORD_DOMAIN = "seal_record";
    public static final int SEAL_RECORD_CURRENT = 1;

    /** 死亡路线会话数据域（见 common/death/DeathChoiceSavedData）。 */
    public static final String DEATH_CHOICE_DOMAIN = "death_choice";
    public static final int DEATH_CHOICE_CURRENT = 1;

    public static int getVersion(CompoundTag data) {
        if (data == null || !data.contains(SCHEMA_VERSION_KEY, Tag.TAG_ANY_NUMERIC)) {
            return UNVERSIONED;
        }
        return data.getInt(SCHEMA_VERSION_KEY);
    }

    public static void setVersion(CompoundTag data, int version) {
        data.putInt(SCHEMA_VERSION_KEY, version);
    }
}
