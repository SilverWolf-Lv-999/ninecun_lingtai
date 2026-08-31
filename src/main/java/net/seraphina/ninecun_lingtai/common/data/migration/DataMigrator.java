package net.seraphina.ninecun_lingtai.common.data.migration;

import net.minecraft.nbt.CompoundTag;

/**
 * 单步 NBT schema 迁移器：把某个数据域的根 CompoundTag 从 {@link #fromVersion()}
 * 原地改写为 {@link #toVersion()} 的格式。
 *
 * <p>实现约束：</p>
 * <ul>
 *   <li>{@code toVersion()} 必须等于 {@code fromVersion() + 1}，调度器按版本号逐步串联；</li>
 *   <li>{@link #migrate(CompoundTag)} 只允许改写传入 tag 的内容，不要在其中读写玩家实体等外部状态；</li>
 *   <li>迁移器内部应使用字符串字面量而非引用业务类里的键名常量——业务常量会随版本演进，
 *       而迁移器操作的是历史格式，必须固定不变；</li>
 *   <li>版本标记（{@link SchemaVersions#SCHEMA_VERSION_KEY}）由调度器统一写入，实现类不必处理。</li>
 * </ul>
 */
public interface DataMigrator {
    /** 所属数据域，见 {@link SchemaVersions} 中的 {@code XXX_DOMAIN} 常量。 */
    String domain();

    /** 本迁移器能处理的源版本。 */
    int fromVersion();

    /** 迁移完成后的目标版本，必须为 {@code fromVersion() + 1}。 */
    int toVersion();

    /** 将 {@code data} 从 fromVersion 格式原地改写为 toVersion 格式。 */
    void migrate(CompoundTag data);
}
