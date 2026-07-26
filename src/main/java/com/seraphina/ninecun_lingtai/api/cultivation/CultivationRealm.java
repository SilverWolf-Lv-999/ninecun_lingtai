package com.seraphina.ninecun_lingtai.api.cultivation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CultivationRealm {
    QI_REFINING("qi_refining", "练气"),
    FOUNDATION_ESTABLISHMENT("foundation_establishment", "筑基"),
    GOLDEN_CORE("golden_core", "金丹"),
    NASCENT_SOUL("nascent_soul", "元婴"),
    SPIRIT_TRANSFORMATION("spirit_transformation", "化神"),
    DAO_INTEGRATION("dao_integration", "合道"),
    VOID_REFINEMENT("void_refinement", "炼虚"),
    MAHAYANA("mahayana", "大乘"),
    IMMORTAL("immortal", "仙");

    private static final Map<String, CultivationRealm> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(CultivationRealm::getSerializedName, Function.identity()));

    private final String serializedName;
    private final String chineseName;

    CultivationRealm(String serializedName, String chineseName) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    public boolean usesHeavenlyLayerName() {
        return this.ordinal() >= SPIRIT_TRANSFORMATION.ordinal();
    }

    public Optional<CultivationRealm> next() {
        int nextRealmIndex = this.ordinal() + 1;
        return nextRealmIndex < values().length
                ? Optional.of(values()[nextRealmIndex])
                : Optional.empty();
    }

    public static Optional<CultivationRealm> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
