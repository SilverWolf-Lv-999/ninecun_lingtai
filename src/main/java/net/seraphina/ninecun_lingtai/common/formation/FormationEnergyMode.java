package net.seraphina.ninecun_lingtai.common.formation;

import java.util.Optional;

/**
 * 阵法灵力来源（阵禁篇 6.3）。SIPHON（汲灵式）依赖灵气侧提供
 * "按区域抽取存量"的写 API，本期解析但拒绝激活（施工图风险 7 口径）。
 */
public enum FormationEnergyMode {
    STORED("stored"),
    SIPHON("siphon");

    private final String serializedName;

    FormationEnergyMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static Optional<FormationEnergyMode> fromSerializedName(String serializedName) {
        for (FormationEnergyMode mode : values()) {
            if (mode.serializedName.equals(serializedName)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
