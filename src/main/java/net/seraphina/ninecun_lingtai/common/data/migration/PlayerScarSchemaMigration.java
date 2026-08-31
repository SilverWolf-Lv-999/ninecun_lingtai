package net.seraphina.ninecun_lingtai.common.data.migration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 样例迁移器：玩家伤痕数据域 0 -> 1，把缺省键补齐。
 *
 * <p>版本 0 指没有版本标记的历史根 tag；版本 1 的格式约定根 tag 内必有
 * {@code "Scars"}（ListTag&lt;CompoundTag&gt;）。本迁移器只补齐缺失的空列表，
 * 不改动任何已有数据，同时演示迁移器的标准写法：</p>
 * <ul>
 *   <li>键名一律用字符串字面量（历史格式是固定的，不引用业务类常量）；</li>
 *   <li>只原地改写传入的 tag；</li>
 *   <li>版本标记由 {@link DataMigrationRegistry} 统一写入。</li>
 * </ul>
 *
 * <p>将来真实的格式变更（重命名键、拆合字段等）同样写成一个 from N 到 N+1 的迁移器，
 * 例如把旧键搬到新键：{@code data.put("NewKey", data.get("OldKey")); data.remove("OldKey");}</p>
 */
public final class PlayerScarSchemaMigration implements DataMigrator {

    @Override
    public String domain() {
        return SchemaVersions.PLAYER_SCARS_DOMAIN;
    }

    @Override
    public int fromVersion() {
        return SchemaVersions.UNVERSIONED;
    }

    @Override
    public int toVersion() {
        return SchemaVersions.PLAYER_SCARS_CURRENT;
    }

    @Override
    public void migrate(CompoundTag data) {
        if (!data.contains("Scars", Tag.TAG_LIST)) {
            data.put("Scars", new ListTag());
        }
    }
}
