package net.seraphina.ninecun_lingtai.api.spell;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 功能类法术的子类型：战斗类主攻伐，实用类主行游济事。 */
public enum SpellCategory {
    COMBAT("combat", "战斗"),
    UTILITY("utility", "实用");

    private static final Map<String, SpellCategory> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SpellCategory::getSerializedName, Function.identity()));

    private final String serializedName;
    private final String chineseName;

    SpellCategory(String serializedName, String chineseName) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    public static Optional<SpellCategory> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
