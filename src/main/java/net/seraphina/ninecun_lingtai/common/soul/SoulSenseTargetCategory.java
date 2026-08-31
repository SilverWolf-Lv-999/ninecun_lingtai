package net.seraphina.ninecun_lingtai.common.soul;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SoulSenseTargetCategory {
    HOSTILE("hostile", 0xFFE04040, true),
    ANIMAL("animal", 0xFF58C858, true),
    VILLAGER("villager", 0xFFE8C84A, true),
    PLAYER("player", 0xFF4A9AE8, true),
    CULTIVATOR("cultivator", 0xFFB05AE8, true),
    ORE("ore", 0xFFE8904A, false),
    SPIRIT_STONE("spirit_stone", 0xFF6AE8D8, false),
    CONTAINER("container", 0xFF8AE84A, false),
    // 道心杀孽 §6.3：高杀孽玩家的血色标记。getMaskBit 用 ordinal，必须追加在
    // 末尾且永不重排，否则老玩家的过滤器 NBT 掩码错位。
    BLOOD_MARKED("blood_marked", 0xFF8B0000, true);

    private static final Map<String, SoulSenseTargetCategory> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(
                    SoulSenseTargetCategory::getSerializedName,
                    Function.identity()
            ));

    private final String serializedName;
    private final int color;
    private final boolean entityCategory;

    SoulSenseTargetCategory(String serializedName, int color, boolean entityCategory) {
        this.serializedName = serializedName;
        this.color = color;
        this.entityCategory = entityCategory;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public int getColor() {
        return this.color;
    }

    public boolean isEntityCategory() {
        return this.entityCategory;
    }

    public int getMaskBit() {
        return 1 << ordinal();
    }

    public static int getFullMask() {
        return (1 << values().length) - 1;
    }

    public static Optional<SoulSenseTargetCategory> fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length
                ? Optional.of(values()[ordinal])
                : Optional.empty();
    }

    public static Optional<SoulSenseTargetCategory> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
