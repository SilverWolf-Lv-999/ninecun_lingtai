package net.seraphina.ninecun_lingtai.common.data.migration;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * NBT schema 迁移注册与调度器。
 *
 * <p>用法：数据类在读取自己的根 CompoundTag 前调用
 * {@link #migrateToVersion(String, CompoundTag, int)}，调度器读取 tag 内的
 * {@link SchemaVersions#SCHEMA_VERSION_KEY}，按版本号从小到大依次应用已注册的
 * {@link DataMigrator}，每步成功后更新版本标记，直到到达目标版本。
 * 迁移链断档（缺某个版本的迁移器）时停在断档处并记录错误，不损坏数据。</p>
 *
 * <p>注册时机：迁移器随所属数据类的静态初始化注册即可（示例见
 * {@code common/status/PlayerScarData} 的 static 块），也可以在 Mod 构造阶段集中注册。</p>
 */
public final class DataMigrationRegistry {
    /** domain -> (fromVersion -> migrator)，按 fromVersion 升序。 */
    private static final Map<String, ConcurrentSkipListMap<Integer, DataMigrator>> MIGRATORS =
            new ConcurrentHashMap<>();

    /**
     * 注册一个迁移器。重复注册同一 (domain, fromVersion) 时保留先注册者并记录警告。
     */
    public static void register(DataMigrator migrator) {
        Objects.requireNonNull(migrator, "migrator");
        String domain = Objects.requireNonNull(migrator.domain(), "migrator domain");
        if (migrator.toVersion() != migrator.fromVersion() + 1) {
            throw new IllegalArgumentException(
                    "Data migrator for domain '" + domain + "' must advance exactly one version, got "
                            + migrator.fromVersion() + " -> " + migrator.toVersion()
            );
        }
        DataMigrator existing = MIGRATORS
                .computeIfAbsent(domain, unused -> new ConcurrentSkipListMap<>())
                .putIfAbsent(migrator.fromVersion(), migrator);
        if (existing != null && existing != migrator) {
            ModMain.LOGGER.warn(
                    "[schema] duplicate migrator for domain '{}' from version {}; keeping {}",
                    domain,
                    migrator.fromVersion(),
                    existing.getClass().getName()
            );
        }
    }

    /**
     * 检查 {@code data} 是否落后于 {@code targetVersion}（需要迁移）。
     */
    public static boolean needsMigration(CompoundTag data, int targetVersion) {
        return data != null && SchemaVersions.getVersion(data) < targetVersion;
    }

    /**
     * 将 {@code data} 原地迁移到 {@code targetVersion}。
     *
     * @return 迁移结束后 tag 的实际版本。等于 targetVersion 表示成功；小于 targetVersion
     *         表示迁移链断档（已记录错误日志）；大于 targetVersion 表示数据来自更新的
     *         模组版本，此时不做任何改动。
     */
    public static int migrateToVersion(String domain, CompoundTag data, int targetVersion) {
        Objects.requireNonNull(domain, "domain");
        if (data == null) {
            return targetVersion;
        }
        int version = SchemaVersions.getVersion(data);
        if (version > targetVersion) {
            ModMain.LOGGER.warn(
                    "[schema] domain '{}' data has version {} newer than supported {}; leaving untouched",
                    domain,
                    version,
                    targetVersion
            );
            return version;
        }
        if (version == targetVersion) {
            return version;
        }
        ConcurrentSkipListMap<Integer, DataMigrator> chain = MIGRATORS.get(domain);
        while (version < targetVersion) {
            DataMigrator migrator = chain != null ? chain.get(version) : null;
            if (migrator == null) {
                ModMain.LOGGER.error(
                        "[schema] no migrator registered for domain '{}' from version {}; "
                                + "data stays at version {} (target {})",
                        domain,
                        version,
                        version,
                        targetVersion
                );
                return version;
            }
            migrator.migrate(data);
            version = migrator.toVersion();
            SchemaVersions.setVersion(data, version);
            ModMain.LOGGER.info(
                    "[schema] migrated domain '{}' data {} -> {}",
                    domain,
                    migrator.fromVersion(),
                    version
            );
        }
        return version;
    }
}
